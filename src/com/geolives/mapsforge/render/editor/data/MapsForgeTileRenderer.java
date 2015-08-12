/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.rule.RenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

/**
 *
 * @author cbrasseur
 */
public class MapsForgeTileRenderer {
    
    private String mMapFilePath;
    private MapFile mMapFile;
    private TileCache mTileCache;
    private DisplayModel mDisplayModel;
    private RenderThemeFuture mRenderTheme;
    private DatabaseRenderer mDatabaseRenderer;
    private BoundingBox mBBOX;
    private int mZoomBounds = -1;
    
    private int mWestTileBounds;
    private int mEastTileBounds;
    private int mSouthTileBounds;
    private int mNorthTileBounds;
    
    public MapsForgeTileRenderer(String mapFile, String styleFile)
    {
        mMapFilePath = mapFile;
        mTileCache = new InMemoryTileCache(50);

        GraphicFactory awtFactory = AwtGraphicFactory.INSTANCE;

        mMapFile = new MapFile(new File(mMapFilePath));
        mDisplayModel = new DisplayModel();
        mRenderTheme = new RenderThemeFuture(awtFactory, new MapsForgeXmlRenderTheme(styleFile), mDisplayModel);
        new Thread(mRenderTheme).start();       // We run the render theme analyzer to make it available when drawing tiles.

        mDatabaseRenderer = new DatabaseRenderer(mMapFile, awtFactory, mTileCache);

        mBBOX = mMapFile.getMapFileInfo().boundingBox;
        mZoomBounds = -1;
    }
    
    public byte[] getTile(int x, int y, int z)
    {
        if(tileOutOfBounds(x, y, z))
            return null;

        final Tile tile = new Tile(x, y, (byte)z, 216);
        RendererJob job = new RendererJob(tile, mMapFile, mRenderTheme, mDisplayModel, 1f, false, false);

        TileBitmap tileBitmap = mDatabaseRenderer.executeJob(job);
        mTileCache.put(job, tileBitmap);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            tileBitmap.compress(baos);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        byte[] bitmapdata = baos.toByteArray();
        return bitmapdata;
    }
    
    private boolean tileOutOfBounds(int tileX, int tileY, int zoom)
    {
        if(zoom != mZoomBounds)
            recalculateTileBounds(zoom);

        final boolean oob =  (tileX < mWestTileBounds) || (tileX > mEastTileBounds) ||
                (tileY < mNorthTileBounds) || (tileY > mSouthTileBounds);
        return oob;
    } // tileOutOfBounds

    /* convert lon/lat to tile x,y from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames */
    private void recalculateTileBounds(final int zoom)
    {
        mZoomBounds = zoom;
        mWestTileBounds = lon2XTile(mBBOX.minLongitude, mZoomBounds);
        mEastTileBounds = lon2XTile(mBBOX.maxLongitude, mZoomBounds);
        mSouthTileBounds = lat2YTile(mBBOX.minLatitude, mZoomBounds);
        mNorthTileBounds = lat2YTile(mBBOX.maxLatitude, mZoomBounds);
    } // recalculateTileBounds

    static private int lon2XTile(final double lon, final int zoom)
    {
        return (int)Math.floor((lon + 180) / 360 * (1<<zoom)) ;
    } // lon2XTile

    static private int lat2YTile(final double lat, final int zoom)
    {
        return (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom)) ;
    } // lat2YTile
    
    public MapFileInfo getVectorDataInfo()
    {
        return mMapFile.getMapFileInfo();
    }
    
    // ==============
    
    private static class MapsForgeXmlRenderTheme implements XmlRenderTheme
    {
        private String mPath;

        public MapsForgeXmlRenderTheme(String path)
        {
            mPath = path;
        }

        @Override
        public XmlRenderThemeMenuCallback getMenuCallback() {
            return null;
        }

        @Override
        public String getRelativePathPrefix() {
            return null;
        }

        @Override
        public InputStream getRenderThemeAsStream() throws FileNotFoundException {

            FileInputStream fis = new FileInputStream(new File(mPath));
            return fis;
        }
    }
    
}
