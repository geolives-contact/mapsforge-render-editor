/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor;

import com.geolives.mapsforge.render.editor.forms.JMainFrame;
import com.geolives.mapsforge.render.editor.util.Constants;
import com.geolives.mapsforge.render.editor.util.ResourceUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 *
 * @author Christophe
 */
public class MainClass {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        // Open or create Properties
        try
        {
            File f = new File("properties.props");
            if (!f.exists())
                f.createNewFile();

            Properties props = new Properties();
            props.load(new FileInputStream(f));
            Constants.MAP_FILE = props.getProperty("mapFile", "default.map");
            if (props.getProperty("mapFile") == null)
                props.put("mapFile", "default.map");
            props.save(new FileOutputStream(f), "");
        }
        catch (Exception e)
        {
            
        }
        
        JMainFrame mainFrame = new JMainFrame();
        mainFrame.setTitle("MapsForge - Render Editor");
        ImageIcon ic = ResourceUtils.getImageResource("icon.png");
        mainFrame.setIconImage(ic.getImage());
        mainFrame.setLocationRelativeTo(null);
        mainFrame.show();
    }
    
}
