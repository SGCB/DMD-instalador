/**
 *  Copyright 2013 Spanish Minister of Education, Culture and Sport
 *
 *  written by MasMedios
 *
 *  Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work  except in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://ec.europa.eu/idabc/servlets/Docbb6d.pdf?id=31979
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis,
 *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and limitations under the License.
 */

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
 * @class LoadFileMessages
 *
 * Clase que lee y carga en UTF8 en memoria los archivos messages con los mensajes localizados que muestra el instalador
 *
 */
public class LoadFileMessages extends ResourceBundle
{
    /**
     * Archivo de mensajes a leer
     */
    String fileName;

    /**
     * El juego de caracteres en UTF8
     */
    protected static final Control UTF8_CONTROL = new UTF8Control();

    /**
     * Localiza los archivos
     */
    private Locale locale;


    /**
     * Constructor para identificar el locale con el idioma y para cargar el archivo
     *
     * @param fileName nombre del archivo messages
     * @param language idioma del messages
     */
    public LoadFileMessages(String fileName, String language)
    {
        if (language != null && !language.isEmpty()) {
            if (language.indexOf("_") > 0) {
                String[] arrLang = language.split("_");
                locale = new Locale(arrLang[0], arrLang[1]);
            } else locale = new Locale(language);
        }
        this.fileName = fileName;
        if (locale == null && Locale.getDefault() == null) Locale.setDefault(Locale.ENGLISH);
        try {
            if (locale != null) setParent(ResourceBundle.getBundle(fileName, locale, UTF8_CONTROL));
            else setParent(ResourceBundle.getBundle(fileName, Locale.getDefault(), UTF8_CONTROL));
        } catch (java.util.MissingResourceException e) {
            setParent(ResourceBundle.getBundle(fileName, Locale.ENGLISH, UTF8_CONTROL));
        }
    }


    /**
     * Cargar nuevo archivo
     *
     * @param fileName nombre del archivo messages
     */
    public LoadFileMessages(String fileName)
    {
        this(fileName, null);
    }

    /**
     * Método sobrecargado
     *
     * @param key
     * @return object
     */
    @Override
    protected Object handleGetObject(String key)
    {
        return parent.getObject(key);
    }

    /**
     * Método sobrecargado
     *
     * @return object
     */
    @Override
    public Enumeration<String> getKeys()
    {
        return parent.getKeys();
    }


    /**
     * @class UTF8Control
     *
     * Clase que carga el archivo messages como propiedades en UTF8
     *
     */
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
