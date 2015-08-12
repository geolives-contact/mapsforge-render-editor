/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor.util;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;

/**
 *
 * @author Christophe
 */
public class ResourceUtils {
    
    public static ImageIcon getImageResource(String name)
    {
        URL url = ResourceUtils.class.getResource("../res/" + name);
        if (url == null)
            return null;
        
        ImageIcon ic = new ImageIcon(url);
        return ic;
    }
    
    public static String getTextResource(String name)
    {
        URL url = ResourceUtils.class.getResource("../res/" + name);
        
        try {
            
         URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                               new InputStreamReader(conn.getInputStream()));

            String inputLine;
            String completeText = "";
            while ((inputLine = br.readLine()) != null) {
                completeText += inputLine + "\n";
            }
            br.close();
            
            return completeText;
            
        } catch (IOException ioe) { ioe.printStackTrace(); }
        
        return "";
    }
    
    public static URL getURLResource(String name)
    {
        URL url = ResourceUtils.class.getResource("../res/" + name);
        if (url == null)
            return null;
        
        return url;
    }
}
