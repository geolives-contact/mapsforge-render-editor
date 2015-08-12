/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor.data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;

/**
 *
 * @author Christophe
 */
public class MapsForgeStringXmlRenderTheme implements XmlRenderTheme {
    
    private String _content;
    private String _fileLocation;
    
    public MapsForgeStringXmlRenderTheme(String content, String fileLocation)
    {
        _content = content;
        _fileLocation = fileLocation;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getRelativePathPrefix() {
        return _fileLocation;
    }

    @Override
    public InputStream getRenderThemeAsStream() throws FileNotFoundException {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(_content.getBytes());
        return bais;
    }
    
}
