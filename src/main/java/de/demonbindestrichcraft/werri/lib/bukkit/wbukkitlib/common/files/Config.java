/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.files;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ABC
 */
public final class Config implements Serializable {

    private Map<String, String> properties;
    File propertiesFile;

    public Config() {
        properties = new HashMap<String, String>();
        propertiesFile = null;
    }

    public Config(File file) {
        this();
        init(file);
    }

    public Config(File file, Map<String, String> properties) {
        this(file);
        update(properties);
    }

    public void init(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        propertiesFile = file;
    }

    public void updateIncorrectValueMap(Map properties) {
        Map<String, String> temp = new HashMap<String, String>();
        Set keys = new HashSet();
        keys.addAll(properties.keySet());
        for (Object key : keys) {
            temp.put(key.toString(), properties.get(key).toString());
        }
        update(temp);
    }

    public void update(Map<String, String> properties) {
        if(properties == null)
            return;
        if(properties.isEmpty())
            return;
        this.properties.putAll(properties);
    }
    
    public void update()
    {
    }
    
    public File getFile()
    {
        return propertiesFile;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getCopyOfProperties() {
        return new HashMap<String, String>(properties);
    }

    public String getString(String key) throws NullPointerException, ClassCastException {
        return properties.get(key);
    }

    public int getInt(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Integer.parseInt(properties.get(key));
    }

    public float getFloat(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Float.parseFloat(properties.get(key));
    }

    public double getDouble(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Double.parseDouble(properties.get(key));
    }

    public long getLong(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Long.parseLong(properties.get(key));
    }

    public short getShort(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Short.parseShort(properties.get(key));
    }

    public boolean getBoolean(String key) throws NumberFormatException, NullPointerException, ClassCastException {
        return Boolean.parseBoolean(properties.get(key));
    }

    public void save(String splitchar) {
        GenerallyFileManager.FileWrite(properties, propertiesFile, splitchar);
    }
    
    public void load(String splitchar)
    {
        load(this.propertiesFile, splitchar);
    }

    public void load(File file, String splitchar) {
        try {
            init(file);
            Map<String, String> content = GenerallyFileManager.AllgemeinFileReader(file, splitchar);
            if (content == null) {
                System.out.println("blablablga");
                return;
            }
            update(content);
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
