package org.dspace.installer_edm;

import org.dspace.core.ConfigurationManager;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 28/01/13
 * Time: 17:01
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMAskosiVocabularies extends InstallerEDMBase
{
    private File askosiDataDirFile;
    private String dbUrl;
    private String dbDriver;
    private String dbUserName;
    private String dbPasword;

    private static final String [] propertiesVocabularyPoolCfg = new String[] {"validation", "url", "driver", "username", "password"};
    private static final String [] propertiesVocabularyCfg = new String[] {"type", "pool", "labels"};


    public InstallerEDMAskosiVocabularies(File askosiDataDirFile)
    {
        super();
        dbUrl = ConfigurationManager.getProperty("db.url");
        dbDriver = ConfigurationManager.getProperty("db.driver");
        dbUserName = ConfigurationManager.getProperty("db.username");
        dbPasword = ConfigurationManager.getProperty("db.password");
        this.askosiDataDirFile = askosiDataDirFile;
    }

    public void processAskosiVocabularies() throws IOException
    {
        for (Map.Entry<String, InstallerEDMAuthBO> entry : authBOHashMap.entrySet()) {
            String handle = entry.getValue().getCollection().getHandle();
            String name = removeAccents(entry.getValue().getCollection().getName().toLowerCase());
            String vocabulary = name + "." + entry.getKey();
            String vocabularyCfg = askosiDataDirFile.getAbsolutePath() + System.getProperty("file.separator") + vocabulary + ".cfg";
            String vocabularyPoolCfg = askosiDataDirFile.getAbsolutePath() + System.getProperty("file.separator") + vocabulary + "-pool.cfg";
            installerEDMDisplay.showQuestion(3, "processAskosiVocabularies.create", new String[] {vocabularyPoolCfg, vocabulary});
            processVocabularyPoolCfg(vocabularyPoolCfg);
            installerEDMDisplay.showQuestion(3, "processAskosiVocabularies.create", new String[] {vocabularyCfg, vocabulary});
            processVocabularyCfg(entry, vocabulary, handle, vocabularyCfg);
        }
    }


    private void processVocabularyPoolCfg(String vocabularyPoolCfg) throws IOException
    {
        boolean modify = false;
        String textToSave = null;
        File vocabularyPoolCfgFile = new File(vocabularyPoolCfg);
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
            text.append("validation=SELECT 1\n");
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

    private void processVocabularyCfg(Map.Entry<String, InstallerEDMAuthBO> entry, String vocabulary, String handle, String vocabularyCfg) throws IOException
    {
        boolean modify = false;
        File vocabularyCfgFile = new File(vocabularyCfg);
        Properties properties = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT hi.handle AS about, lower(substring(m.text_lang from 1 for 2)) AS lang, m.text_value AS label FROM metadatavalue m, item i, collection2item c, handle h, handle hi WHERE h.handle='");
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
        sql.append(") ORDER BY m.text_value");
        String query = sql.toString();
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
                        if (property.equals("type") && !value.equalsIgnoreCase("SQL")) {
                            modify = true;
                        } else if (property.equals("pool") && !value.equals(vocabulary)) {
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
            properties.setProperty("type", "SQL");
            properties.setProperty("pool", vocabulary);
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
