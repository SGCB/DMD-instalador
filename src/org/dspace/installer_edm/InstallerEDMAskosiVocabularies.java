package org.dspace.installer_edm;

import org.dspace.core.ConfigurationManager;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;

/**
 * @class InstallerEDMAskosiVocabularies
 *
 * Clase para crear y configurar los vocabularios de Askosi en el directorio de datos de Askosi.
 * Para cada elemento de autoridad se crar dos archivos, un ode conexión a la base de datos de dspace
 * y el otro con la consulta a la colección donde está la autoridad para recogerlas todas.
 * Extiende de {@link InstallerEDMBase}
 */
public class InstallerEDMAskosiVocabularies extends InstallerEDMBase
{
    /**
     * Directorio de datos de Askosi
     */
    private File askosiDataDirFile;

    /**
     * Url de la conexión con la base de datos de dspace
     */
    private String dbUrl;

    /**
     * Driver a usar en la conexión con la base de datos
     */
    private String dbDriver;

    /**
     * Usuario para la conexión con la bbdd
     */
    private String dbUserName;

    /**
     * Clave del usuario para la conexión con la bbdd
     */
    private String dbPasword;

    /**
     * Propiedades a crear en el archivo de la conexión a la base de datos
     */
    private static final String [] propertiesVocabularyPoolCfg = new String[] {"validation", "url", "driver", "username", "password"};

    /**
     * Propiedades a crear en el archivo de la consulta a la base de datos
     */
    private static final String [] propertiesVocabularyCfg = new String[] {"type", "pool", "labels"};


    /**
     * Constructor, paso actual y directorio de datos de Askosi
     * Se recogen los datos de la conexión del archivo de configuración de dspace
     *
     * @param currentStepGlobal paso actual
     * @param askosiDataDirFile directorio de datos de Askosi
     */
    public InstallerEDMAskosiVocabularies(int currentStepGlobal, File askosiDataDirFile)
    {
        super(currentStepGlobal);
        dbUrl = ConfigurationManager.getProperty("db.url");
        dbDriver = ConfigurationManager.getProperty("db.driver");
        dbUserName = ConfigurationManager.getProperty("db.username");
        dbPasword = ConfigurationManager.getProperty("db.password");
        this.askosiDataDirFile = askosiDataDirFile;
    }

    /**
     * Crea los archivos para cada una de las autoridades
     * Los archivos se crean con el nombre de la colección más un punto más el nombre del elemento de la autoridad más la extensión adecuada
     *
     * @throws IOException
     */
    public void processAskosiVocabularies() throws IOException
    {
        for (Map.Entry<String, InstallerEDMAuthBO> entry : authBOHashMap.entrySet()) {
            String handle = entry.getValue().getCollection().getHandle();
            String name = removeAccents(entry.getValue().getCollection().getName().toLowerCase());
            String vocabulary = name.replaceAll("\\.", "_") + "_" + entry.getKey().replaceAll("\\.", "_");
            String vocabularyCfg = askosiDataDirFile.getAbsolutePath() + fileSeparator + vocabulary + ".cfg";
            String vocabularyPoolCfg = askosiDataDirFile.getAbsolutePath() + fileSeparator + vocabulary + "-pool.cfg";
            installerEDMDisplay.showQuestion(currentStepGlobal, "processAskosiVocabularies.create", new String[] {vocabularyPoolCfg, vocabulary});
            processVocabularyPoolCfg(vocabularyPoolCfg);
            installerEDMDisplay.showQuestion(currentStepGlobal, "processAskosiVocabularies.create", new String[] {vocabularyCfg, vocabulary});
            processVocabularyCfg(entry, vocabulary, handle, vocabularyCfg);
        }
    }


    /**
     * Crea el archivo de conexión a la base de datos
     *
     * @param vocabularyPoolCfg nombre del archivo
     * @throws IOException
     */
    private void processVocabularyPoolCfg(String vocabularyPoolCfg) throws IOException
    {
        boolean modify = false;
        String textToSave = null;
        File vocabularyPoolCfgFile = new File(vocabularyPoolCfg);
        // se crea o mofifica el archivo
        if (vocabularyPoolCfgFile.exists()) {
            Properties properties = new Properties();
            URL url = vocabularyPoolCfgFile.toURI().toURL();
            InputStream is = url.openStream();
            properties.load(is);
            try {
                for (String property : propertiesVocabularyPoolCfg) {
                    String value = properties.getProperty(property);
                    if (value == null || value.isEmpty()) {
                        modify = true;
                    } else {
                        if (property.equals("validation") && !value.equalsIgnoreCase("SELECT 1")) {
                            modify = true;
                        } else if (property.equals("url") && !value.equals(dbUrl)) {
                            modify = true;
                        } else if (property.equals("driver") && !value.equals(dbDriver)) {
                            modify = true;
                        } else if (property.equals("username") && !value.equals(dbUserName)) {
                            modify = true;
                        } else if (property.equals("password") && !value.equals(dbPasword)) {
                            modify = true;
                        }
                    }
                    if (modify) break;
                }
            } finally {
                if (is != null) is.close();
            }

        } else modify = true;
        if (modify) {
            StringBuilder text = new StringBuilder();
            String validation = (dbName.equalsIgnoreCase("oracle"))?"SELECT 1 FROM DUAL":"SELECT 1";
            text.append("validation=").append(validation).append("\n");
            text.append("url=").append(dbUrl).append("\n");
            text.append("driver=").append(dbDriver).append("\n");
            text.append("username=").append(dbUserName).append("\n");
            text.append("password=").append(dbPasword).append("\n");
            textToSave = text.toString();
            FileChannel rwChannel = new RandomAccessFile(vocabularyPoolCfgFile.getAbsolutePath(), "rw").getChannel();
            try {
                ByteBuffer wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, textToSave.length());
                wrBuf.put(textToSave.getBytes());
            } finally {
                if (rwChannel != null) rwChannel.close();
            }
        }
    }

    /**
     * Crea el archivo con la consulta a la base de datos de dspace
     * Se consulta la colección asociada al elemento de la autoridad para recoger todos las autoridades con ese elemento
     * Se recoge el handle como about; text_lang como lang; text_value como label
     *
     * @param entry POJO de la autoridad {@link InstallerEDMAuthBO}
     * @param vocabulary nombre del vocabulario: se crea con el nombre de la colección más un punto más el nombre del elemento de la autoridad
     * @param handle handle de la autoridad
     * @param vocabularyCfg nombre del archivo
     * @throws IOException
     */
    private void processVocabularyCfg(Map.Entry<String, InstallerEDMAuthBO> entry, String vocabulary, String handle, String vocabularyCfg) throws IOException
    {
        boolean modify = false;
        File vocabularyCfgFile = new File(vocabularyCfg);
        Properties properties = null;

        // consulta sql
        StringBuilder sql = new StringBuilder();
        String sqlSubstring = (dbName.equalsIgnoreCase("oracle"))?"substr(m.text_lang, 1, 2)":"substring(m.text_lang from 1 for 2)";
        sql.append("SELECT hi.handle AS about, lower(").append(sqlSubstring).append(") AS lang, m.text_value AS label FROM metadatavalue m, item i, collection2item c, handle h, handle hi WHERE h.handle='");
        sql.append(handle);
        sql.append("' AND c.collection_id=h.resource_id AND i.item_id=c.item_id AND m.item_id=i.item_id AND hi.resource_type_id=2 AND hi.resource_id=i.item_id AND m.metadata_field_id=(SELECT f.metadata_field_id FROM metadatafieldregistry f WHERE f.metadata_schema_id=1 AND f.element='");
        sql.append(entry.getValue().getMetadataField().getElement());
        if (entry.getValue().getMetadataField().getQualifier() == null)
            sql.append("' AND f.qualifier IS NULL");
        else {
            sql.append("' AND f.qualifier='");
            sql.append(entry.getValue().getMetadataField().getQualifier());
            sql.append("'");
        }
        String sqlOrder = (dbName.equalsIgnoreCase("oracle"))?"to_char(m.text_value)":"m.text_value";
        sql.append(") ORDER BY ").append(sqlOrder);
        String query = sql.toString();
        // se crea o mofifica el archivo
        if (vocabularyCfgFile.exists()) {
            properties = new Properties();
            URL url = vocabularyCfgFile.toURI().toURL();
            InputStream is = url.openStream();
            properties.load(is);
            try {
                for (String property : propertiesVocabularyCfg) {
                    String value = properties.getProperty(property);
                    if (value == null || value.isEmpty()) {
                        modify = true;
                    } else {
                        if (property.equals("type") && !value.equalsIgnoreCase("DYNSQL")) {
                            modify = true;
                        } else if (property.equals("pool") && !value.equals(vocabulary)) {
                            modify = true;
                        } else if (property.equals("title-en") && !value.equals(vocabulary)) {
                            modify = true;
                        } else if (property.equals("title-es") && !value.equals(vocabulary)) {
                            modify = true;
                        } else if (property.equals("labels")) {

                            if (!value.equalsIgnoreCase(query)) modify = true;
                        }
                    }
                    if (modify) break;
                }
            } finally {
                if (is != null) is.close();
            }
        } else modify = true;

        if (modify) {
            if (properties == null) properties = new Properties();
            properties.setProperty("type", "DYNSQL");
            properties.setProperty("pool", vocabulary);
            properties.setProperty("title-en", vocabulary);
            properties.setProperty("title-es", vocabulary);
            properties.setProperty("labels", query);
            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(new FileOutputStream(vocabularyCfgFile), "UTF-8");
                properties.store(out, "");
            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        }
    }
}
