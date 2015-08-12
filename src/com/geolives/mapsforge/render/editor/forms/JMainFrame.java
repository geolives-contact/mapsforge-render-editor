/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor.forms;

import com.geolives.mapsforge.render.editor.data.MapsForgeFileXmlRenderTheme;
import com.geolives.mapsforge.render.editor.data.MapsForgeStringXmlRenderTheme;
import com.geolives.mapsforge.render.editor.data.StyleDocument;
import com.geolives.mapsforge.render.editor.util.Constants;
import com.geolives.mapsforge.render.editor.util.ResourceUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.CodeEditorPane;
import javax.swing.DefaultSyntaxColorizer;
import javax.swing.DefaultSyntaxColorizer.RegExpHashMap;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JXTextPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.SyntaxColorizer;
import javax.swing.SyntaxHTMLWriter;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.mapsforge.map.swing.controller.MapViewComponentListener;
import org.mapsforge.map.swing.controller.MouseEventListener;
import org.mapsforge.map.swing.view.MapView;

/**
 *
 * @author Christophe
 */
public class JMainFrame extends javax.swing.JFrame {

    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    
    private JLabel lblError;
    private MapView _mapView;
    private CodeEditorPane _editorPane;
    private CaretListener _caretListener = new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                
                if (_editorPane.getText().hashCode() != _openedDocument.getContent().hashCode())
                    refreshUI();
            }
        };
    
    private StyleDocument _openedDocument = null;
    
    /**
     * Creates new form JMainFrame
     */
    public JMainFrame() {
        initComponents();
        
        splMain.setDividerLocation(0.5f);
        splMain.setResizeWeight(0.5f);
        
        _mapView = createMapView();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.add(_mapView, JLayeredPane.DEFAULT_LAYER);
        
        lblError = new JLabel();
        lblError.setForeground(Color.RED);
        lblError.setFont(lblError.getFont().deriveFont(Font.BOLD));
        layeredPane.add(lblError, JLayeredPane.POPUP_LAYER);
        lblError.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        lblError.setText("");
        
        _editorPane = createCodeTextPane();
        pnlCode.setLayout(new BorderLayout());
        pnlCode.add(_editorPane.getContainerWithLines(), BorderLayout.CENTER);
        
        refreshUI();
        
        // 50.636311, 5.570565
        MapFile mf = new MapFile(Constants.MAP_FILE);
        double lat = mf.boundingBox().getCenterPoint().latitude;
        double lng = mf.boundingBox().getCenterPoint().longitude;
        _mapView.getModel().mapViewPosition.setCenter(new LatLong(lat, lng));
        _mapView.getModel().mapViewPosition.setZoomLevel((byte)15);
        
    }
    
    private MapView createMapView()
    {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));

        MouseEventListener mouseEventListener = new MouseEventListener(mapView.getModel());
        mapView.addMouseListener(mouseEventListener);
        mapView.addMouseMotionListener(mouseEventListener);
        mapView.addMouseWheelListener(mouseEventListener);

        return mapView;
    }
    
    private CodeEditorPane createCodeTextPane()
    {
        CodeEditorPane pane = new CodeEditorPane();

        pane.setStyledDocument(new DefaultStyledDocument());
        HashMap<String, Color> keywords = new RegExpHashMap();
        //keywords.put("<", Color.blue);
        keywords.put("rule", Color.blue);
        keywords.put("line", Color.blue);
        keywords.put("pathText", Color.blue);
        keywords.put("area", Color.blue);
        keywords.put("rendertheme", Color.blue);
        pane.setKeywordColor(keywords);
        pane.setVerticalLineAtPos(80);
        
        /*HashMap<String, String> helps = new HashMap<>();
        helps.put("rule", "Hello World Rule !");
        helps.put("line", "Hello World Line !");
        pane.setKeywordHelp(helps);*/
        
        return pane;
    }
    
    private TileCache createTileCache(int index) {
        TileCache firstLevelTileCache = new InMemoryTileCache(128);
        File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge" + index);
        TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }
    
    private TileRendererLayer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent, boolean renderLabels, String mapFile, String rendererFile)
    {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), mapViewPosition, isTransparent,
                        renderLabels, GRAPHIC_FACTORY);
        tileRendererLayer.setXmlRenderTheme(new MapsForgeFileXmlRenderTheme(rendererFile));
        return tileRendererLayer;
    }
    
    private TileRendererLayer createTileRendererLayerWithString(TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent, boolean renderLabels, String mapFile, String xml, String fileLocation)
    {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), mapViewPosition, isTransparent,
                        renderLabels, GRAPHIC_FACTORY);
        tileRendererLayer.setXmlRenderTheme(new MapsForgeStringXmlRenderTheme(xml, fileLocation));
        return tileRendererLayer;
    }
    
    private String openBaseFileText()
    {
        return ResourceUtils.getTextResource("BaseFile.txt");
    }
    
    private void changeMap()
    {
        try
        {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                File f = fileChooser.getSelectedFile();
                if (!f.exists())
                    throw new Exception("File doesn't exists!");

                String path = f.getAbsolutePath();
                //String path = Constants.ROOT_MAPSFORGE_DIR + "\\styles\\default.xml";

                Constants.MAP_FILE = path;
                
                File fProperties = new File("properties.props");

                Properties props = new Properties();
                props.load(new FileInputStream(fProperties));
                props.put("mapFile", Constants.MAP_FILE);
                props.save(new FileOutputStream(fProperties), "");
                
                MapFile mf = new MapFile(f);
                double lat = mf.boundingBox().getCenterPoint().latitude;
                double lng = mf.boundingBox().getCenterPoint().longitude;
                _mapView.getModel().mapViewPosition.setCenter(new LatLong(lat, lng));
                
                refreshUI();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "Error while changing map file", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createStyle()
    {
        try
        {
            if (_openedDocument != null)
                closeStyle();
            
            refreshUI();
            
            _openedDocument = new StyleDocument();
            String baseFileText = openBaseFileText();
            _openedDocument.putContents(baseFileText);
            _editorPane.setText(_openedDocument.getContent());
            
            refreshUI();
            _editorPane.addCaretListener(_caretListener);
            
        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "Error while creating style", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openStyle()
    {
        try
        {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                File f = fileChooser.getSelectedFile();
                if (!f.exists())
                    throw new Exception("File doesn't exists!");
                
                String path = f.getAbsolutePath();
                //String path = Constants.ROOT_MAPSFORGE_DIR + "\\styles\\default.xml";
                
                if (_openedDocument != null)
                    closeStyle();
                
                refreshUI();
                
                _openedDocument = StyleDocument.loadStyle(path);
                _editorPane.setText(_openedDocument.getContent());
                
                refreshUI();
                _editorPane.addCaretListener(_caretListener);
            }
            
        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "Error while opening style", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void closeStyle()
    {
        if (_openedDocument == null)
            return;
        
        try
        {
            _openedDocument.closeStyle();
            _openedDocument = null;
            refreshUI();
            
            _editorPane.removeCaretListener(_caretListener);
            
        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "Error while closing style", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveStyle()
    {
        if (_openedDocument == null)
            return;
        
        _openedDocument.putContents(_editorPane.getText());
                
        try
        {
            if (!_openedDocument.isSaved())
            {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File f = fileChooser.getSelectedFile();
                    String path = f.getAbsolutePath();

                    _openedDocument.saveStyle(path);
                    refreshUI();
                }
            }
            else
            {
                _openedDocument.saveStyle();
                refreshUI();
            }
            
        } catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, e.toString(), "Error while saving style", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveAsStyle()
    {
        if (_openedDocument == null)
            return;
        
        _openedDocument.putContents(_editorPane.getText());
        
        try
        {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                File f = fileChooser.getSelectedFile();
                String path = f.getAbsolutePath();

                _openedDocument.saveStyle(path);
                refreshUI();
            }
            
        } catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, e.toString(), "Error while saving style", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshUI()
    {
        if (_openedDocument != null)
        {
            //_editorPane.setText(_openedDocument.getContent());
        
            String path = _openedDocument.getPath();
            Layers layers = _mapView.getLayerManager().getLayers();
            
            layers.clear();
            TileCache tc = createTileCache(0);
            
            System.out.println("RefreshUI()");

            String filePath = Constants.MAP_FILE;
            String rendererPath = _openedDocument.getPath();
            String fileLocation = "";
            if (rendererPath != null)
                fileLocation = new File(rendererPath).getParentFile().getAbsolutePath();
            
            TileRendererLayer layer = createTileRendererLayerWithString(tc, _mapView.getModel().mapViewPosition, false, true, filePath, 
                    _editorPane.getText(), fileLocation);
            layers.add(layer);
            
            _openedDocument.putContents(_editorPane.getText());
            
            // TODO
            final RenderThemeFuture rtf = new RenderThemeFuture(GRAPHIC_FACTORY, new MapsForgeStringXmlRenderTheme(_editorPane.getText(), fileLocation), new DisplayModel());
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    
                    rtf.run();
                    try {
                        rtf.get();
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            
                            @Override
                            public void run() {
                                System.out.println("Dismiss the error!");
                                //_mapView.setVisible(true);
                                //lblError.setVisible(false);
                                lblError.setText("");
                            }
                        });
                        
                    } catch (final Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                System.out.println("Showing an error!");
                                //_mapView.setVisible(false);
                                //lblError.setVisible(true);
                                lblError.setText("<html>" + e.toString() + "</html>");
                            }
                        });
                            
                    }
                }
            });
            t.start();
            
            
        }
        else
        {
            Layers layers = _mapView.getLayerManager().getLayers();
            layers.clear();
        }
        
        
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem8 = new javax.swing.JMenuItem();
        splMain = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        layeredPane = new javax.swing.JLayeredPane();
        jPanel3 = new javax.swing.JPanel();
        pnlCodeEditor = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pnlCode = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu2 = new javax.swing.JMenu();
        tsmiCreateStyle = new javax.swing.JMenuItem();
        tsmiOpenStyle = new javax.swing.JMenuItem();
        tsmiSaveStyle = new javax.swing.JMenuItem();
        tsmiSaveStyleAs = new javax.swing.JMenuItem();
        tsmiCloseStyle = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        tsmiChangeMapFile = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        tsmiExit = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItem14 = new javax.swing.JMenuItem();
        tsmiGoTo = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();

        jMenuItem8.setText("jMenuItem8");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        splMain.setDividerLocation(350);
        splMain.setDividerSize(2);

        layeredPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout layeredPaneLayout = new javax.swing.GroupLayout(layeredPane);
        layeredPane.setLayout(layeredPaneLayout);
        layeredPaneLayout.setHorizontalGroup(
            layeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 352, Short.MAX_VALUE)
        );
        layeredPaneLayout.setVerticalGroup(
            layeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 537, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(layeredPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(layeredPane)
                .addGap(2, 2, 2))
        );

        splMain.setRightComponent(jPanel2);

        javax.swing.GroupLayout pnlCodeLayout = new javax.swing.GroupLayout(pnlCode);
        pnlCode.setLayout(pnlCodeLayout);
        pnlCodeLayout.setHorizontalGroup(
            pnlCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 344, Short.MAX_VALUE)
        );
        pnlCodeLayout.setVerticalGroup(
            pnlCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 517, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("XML Editor", pnlCode);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 344, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 517, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("UI Editor", jPanel4);

        javax.swing.GroupLayout pnlCodeEditorLayout = new javax.swing.GroupLayout(pnlCodeEditor);
        pnlCodeEditor.setLayout(pnlCodeEditorLayout);
        pnlCodeEditorLayout.setHorizontalGroup(
            pnlCodeEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        pnlCodeEditorLayout.setVerticalGroup(
            pnlCodeEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlCodeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlCodeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        splMain.setLeftComponent(jPanel3);

        jMenu2.setText("File");

        tsmiCreateStyle.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        tsmiCreateStyle.setText("Create Style");
        tsmiCreateStyle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiCreateStyleActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiCreateStyle);

        tsmiOpenStyle.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        tsmiOpenStyle.setText("Open Style...");
        tsmiOpenStyle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiOpenStyleActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiOpenStyle);

        tsmiSaveStyle.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        tsmiSaveStyle.setText("Save Style");
        tsmiSaveStyle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiSaveStyleActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiSaveStyle);

        tsmiSaveStyleAs.setText("Save Style As...");
        tsmiSaveStyleAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiSaveStyleAsActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiSaveStyleAs);

        tsmiCloseStyle.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        tsmiCloseStyle.setText("Close Style");
        tsmiCloseStyle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiCloseStyleActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiCloseStyle);
        jMenu2.add(jSeparator2);

        tsmiChangeMapFile.setText("Change Map file...");
        tsmiChangeMapFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiChangeMapFileActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiChangeMapFile);
        jMenu2.add(jSeparator3);

        tsmiExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        tsmiExit.setText("Exit");
        tsmiExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tsmiExitActionPerformed(evt);
            }
        });
        jMenu2.add(tsmiExit);

        jMenuBar1.add(jMenu2);

        jMenu1.setText("Edit");

        jMenuItem9.setText("Undo");
        jMenu1.add(jMenuItem9);

        jMenuItem10.setText("Redo");
        jMenu1.add(jMenuItem10);
        jMenu1.add(jSeparator4);

        jMenuItem11.setText("Cut");
        jMenu1.add(jMenuItem11);

        jMenuItem12.setText("Copy");
        jMenu1.add(jMenuItem12);

        jMenuItem13.setText("Paste");
        jMenu1.add(jMenuItem13);
        jMenu1.add(jSeparator5);

        jMenuItem14.setText("Find...");
        jMenu1.add(jMenuItem14);

        jMenuBar1.add(jMenu1);

        tsmiGoTo.setText("Map");

        jMenuItem1.setText("Search Location...");
        tsmiGoTo.add(jMenuItem1);

        jMenuItem2.setText("Go To...");
        tsmiGoTo.add(jMenuItem2);

        jMenuBar1.add(tsmiGoTo);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splMain)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splMain)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tsmiChangeMapFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiChangeMapFileActionPerformed
        changeMap();
    }//GEN-LAST:event_tsmiChangeMapFileActionPerformed

    private void tsmiExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiExitActionPerformed
        this.hide();
        System.exit(0);
    }//GEN-LAST:event_tsmiExitActionPerformed

    private void tsmiCreateStyleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiCreateStyleActionPerformed
        createStyle();
    }//GEN-LAST:event_tsmiCreateStyleActionPerformed

    private void tsmiOpenStyleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiOpenStyleActionPerformed
        openStyle();
    }//GEN-LAST:event_tsmiOpenStyleActionPerformed

    private void tsmiSaveStyleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiSaveStyleActionPerformed
        saveStyle();
    }//GEN-LAST:event_tsmiSaveStyleActionPerformed

    private void tsmiSaveStyleAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiSaveStyleAsActionPerformed
        saveAsStyle();
    }//GEN-LAST:event_tsmiSaveStyleAsActionPerformed

    private void tsmiCloseStyleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tsmiCloseStyleActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tsmiCloseStyleActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLayeredPane layeredPane;
    private javax.swing.JPanel pnlCode;
    private javax.swing.JPanel pnlCodeEditor;
    private javax.swing.JSplitPane splMain;
    private javax.swing.JMenuItem tsmiChangeMapFile;
    private javax.swing.JMenuItem tsmiCloseStyle;
    private javax.swing.JMenuItem tsmiCreateStyle;
    private javax.swing.JMenuItem tsmiExit;
    private javax.swing.JMenu tsmiGoTo;
    private javax.swing.JMenuItem tsmiOpenStyle;
    private javax.swing.JMenuItem tsmiSaveStyle;
    private javax.swing.JMenuItem tsmiSaveStyleAs;
    // End of variables declaration//GEN-END:variables
}
