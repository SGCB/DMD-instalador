package org.dspace.installer_edm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 16/01/13
 * Time: 10:09
 * To change this template use File | Settings | File Templates.
 */
public class LoadFileMessages extends ResourceBundle
{
    String fileName;
    protected static final Control UTF8_CONTROL = new UTF8Control();

    public LoadFileMessages(String fileName)
    {
        this.fileName = fileName;
        if (Locale.getDefault() == null) Locale.setDefault(Locale.ENGLISH);
        try {
        setParent(ResourceBundle.getBundle(fileName, Locale.getDefault(), UTF8_CONTROL));
        } catch (java.util.MissingResourceException e) {
            setParent(ResourceBundle.getBundle(fileName, Locale.ENGLISH, UTF8_CONTROL));
        }
    }


    @Override
    protected Object handleGetObject(String key)
    {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys()
    {
        return parent.getKeys();
    }


    protected static class UTF8Control extends Control
    {
        public ResourceBundle newBundle
                (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException
        {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
