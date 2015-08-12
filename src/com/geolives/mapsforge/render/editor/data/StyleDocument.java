/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.geolives.mapsforge.render.editor.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Christophe
 */
public class StyleDocument {
    
    private boolean _isSaved;
    private String _path;
    private String _content;
    
    public StyleDocument()
    {
        _isSaved = false;
        _content = "";
    }
    
    public static StyleDocument loadStyle(String path)
    {
        StyleDocument sd = new StyleDocument();
        sd._isSaved = true;
        sd._path = path;
        
        String s = "";
        try {
            s = new String(Files.readAllBytes(Paths.get(path,"")));
        } catch (Exception e) { e.printStackTrace(); }
        sd._content = s;
        
        return sd;
    }
    
    public void closeStyle()
    {
        _isSaved = false;
    }
    
    public void saveStyle()
    {
        _isSaved = true;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(_path)));
            writer.write(_content);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void saveStyle(String filePath)
    {
        _isSaved = true;
        _path = filePath;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));
            writer.write(_content);
            writer.flush();
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public boolean isSaved()
    {
        return _isSaved;
    }
    
    public String getContent()
    {
        return _content;
    }
    
    public void putContents(String content)
    {
        _content = content;
    }
    
    public String getPath()
    {
        return _path;
    }
}
