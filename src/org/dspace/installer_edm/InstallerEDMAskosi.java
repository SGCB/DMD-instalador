package org.dspace.installer_edm;

import org.dspace.core.ConfigurationManager;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @class InstallerEDMAskosi
 *
 * Clase para instalar Askosi en el servidor de aplicaciones (Tomcat).
 * Se copian al directorio de Tomcat de librerías unos archivos jar necesarios que vienen
 * con el instalador en el directorio packages. Así mismo se copia el servicio
 * Askosi en webapps.
 *
 */
public class InstallerEDMAskosi extends InstallerEDMBase
{
    /**
     * Directorio de datos de Askosi
     */
    private File finalAskosiDataDestDirFile = null;


    /**
     * Constructor con el paso actual.
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerEDMAskosi(int currentStepGlobal)
    {
        super(currentStepGlobal);
    }

    /**
     *  Copia los ficheros del jar del directorio packages del instalador al directorio de librerías de tomcat.
     *  Copia los ficheros jar de los driver de bbdd del directorio lib del instalador al directorio de librerías de tomcat.
     *  Pide el directorio de datos para Askosi
     *  Descomprime el fichero del servicio web de Askosi del directorio packages del instalador y lo copia a webapps
     *  Descomprime el fichero del plugin para jspui de Askosi del directorio packages del instalador y lo copia al webapps de jspui
     *  Descomprime el fichero de repositorios de Askosi del directorio packages del instalador y lo copia al directorio de datos de Askosi
     *
     *
     * @param dirPackages directorio packages del instalador
     * @return si ha sido correcta la copia
     */
    public boolean installPackages(File dirPackages)
    {
        installerEDMDisplay.showLn();
        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.title");

        // Copia los ficheros del jar del directorio packages al directorio de librerías de tomcat.
        if (copyJarsShared(dirPackages)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.jarok");

            // Copia los ficheros jar de los driver de bbdd del directorio packages al directorio de librerías de tomcat.
            installerEDMDisplay.showLn();
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.db.title");
            if (!copyDatabaseDrivers()) return false;

            // Pide el directorio de datos para Askosi
            if (!getAskosiDataDir()) return false;
            int currentStep = 0;
            Iterator<File> fileIterPackZip = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"zip"}, false);
            while (fileIterPackZip.hasNext()) {
                File sourcePackageFile = fileIterPackZip.next();

                // Descomprime el fichero del servicio web de Askosi y lo copia a webapps
                if (sourcePackageFile.getName().equals("askosiWebapp.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.deploy.title");
                    if (copyAskosiWebApp(sourcePackageFile)) currentStep++;

                // Descomprime el fichero del plugin para jspui de Askosi y lo copia al webapps de jspui
                } else if (sourcePackageFile.getName().equals("classes.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.deploy.plugin.title");
                    if (copyAskosiPlugJspui(sourcePackageFile)) currentStep++;

                // Descomprime el fichero de repositorios de Askosi y lo copia al directorio de datos de Askosi
                } else if (sourcePackageFile.getName().equals("exampleAskosiData.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "installPackages.datadir.title");
                    if (copyAskosiDataDir(sourcePackageFile)) currentStep++;
                }
            }
            if (currentStep == 3) {
                return true;
            }
        }
        return false;
    }


    /**
     * Copia las librerías jar de packages al directorio lib de Tomcat
     * Se busca el directorio shared/lib, si no existe el directorio lib. Ha de tener permisos de escritura.
     * Esta paso ha de lanzarse con un usuario que pueda escribir en el directorio de lib de Tomcat.
     *
     * @param dirPackages directorio packages del instalador
     * @return si la copia ha ido bien
     */
    private boolean copyJarsShared(File dirPackages)
    {
        // directorio Tomcat existe y es leíble
        TomcatBaseFile = new File(TomcatBase);
        if (!TomcatBaseFile.exists() || !TomcatBaseFile.canRead()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.tomcatbasedir.notexist", new String[]{TomcatBase});
            return false;
        }

        // se busca el directorio shared/lib
        String TomcatBaseShared = new StringBuilder().append(TomcatBase).append("shared").append(fileSeparator).append("lib").append(fileSeparator).toString();
        File dirInstallJar = new File(TomcatBaseShared);

        // si no existe el directorio lib
        if (!dirInstallJar.exists()) {
            TomcatBaseShared = new StringBuilder().append(TomcatBase).append("lib").append(fileSeparator).toString();
            dirInstallJar = new File(TomcatBaseShared);
        }

        // se ha de poder escribir en él
        if (!dirInstallJar.canWrite()) {
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.tomcatbaseshareddir.notwritable", new String[]{TomcatBaseShared});
            TomcatBaseShared = null;
        }

        // si no se ha encontrado, se pregunta la ruta del directorio
        dirInstallJar = null;
        while (true) {
            if (TomcatBaseShared != null) installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.copyjars", new String[]{TomcatBaseShared});
            else installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dirnew.copyjars");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            } catch (Exception e) {
                showException(e);
            }
            if ((response != null) && (response.length() > 0)) {
                response = response.trim();
                dirInstallJar = new File(response);
            } else if (TomcatBaseShared != null) {
                response = TomcatBaseShared;
                dirInstallJar = new File(TomcatBaseShared);
            }
            if (dirInstallJar != null && dirInstallJar.exists() && dirInstallJar.canWrite()) {
                if (!response.equals(TomcatBaseShared)) TomcatBaseShared = response;
                break;
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }

        // se copian los archivos jar a Tomcat
        Iterator<File> fileIterPack = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"jar"}, false);
        return copyPackagesFiles(fileIterPack, TomcatBaseShared);
    }


    /**
     * Se copian los archivos jar con los drivers de la bbdd del directorio lib del instalador  a Tomcat
     * Se copian a common/lib, si no existe a lib de Tomcat. Se ha de poder escribir en ellos
     *
     * @return el éxito de la copia
     */
    private boolean copyDatabaseDrivers()
    {
        // directorio lib del instalador
        String databaseDriverSourceDir = myInstallerDirPath + fileSeparator + "lib" + fileSeparator;
        File databaseDriverSourceDirFile = new File(databaseDriverSourceDir);
        if (databaseDriverSourceDirFile.exists() && databaseDriverSourceDirFile.canRead()) {

            // ficheros con nombre con patrón jdbc y extensión jar
            FilenameFilter selectJdbc = new FileListFilter("jdbc", "jar");
            File[] filesJdbc = databaseDriverSourceDirFile.listFiles(selectJdbc);
            if (filesJdbc.length > 0) {

                // directorio common/lib de Tomcat
                String TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("common").append(fileSeparator).append("lib").append(fileSeparator).toString();
                File dirInstallJar = new File(TomcatBaseCommon);

                // directorio lib de Tomcat
                if (!dirInstallJar.exists()) {
                    TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("lib").append(fileSeparator).toString();
                    dirInstallJar = new File(TomcatBaseCommon);
                }
                if (!dirInstallJar.canWrite()) {
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.tomcatbaseshareddir.notwritable", new String[]{TomcatBaseCommon});
                    TomcatBaseCommon = null;
                }

                // si no existe se pide la ruta del directorio
                dirInstallJar = null;
                while (true) {
                    if (TomcatBaseCommon != null) installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.dir.copyjars", new String[]{new File(TomcatBaseCommon).getAbsolutePath()});
                    else installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.dirnew.copyjars");
                    String response = null;
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        showException(e);
                        return false;
                    }
                    if ((response != null) && (response.length() > 0)) {
                        response = response.trim();
                        dirInstallJar = new File(response);
                    } else if (TomcatBaseCommon != null) {
                        response = TomcatBaseCommon;
                        dirInstallJar = new File(TomcatBaseCommon);
                    }
                    if (dirInstallJar != null && dirInstallJar.exists() && dirInstallJar.canWrite()) {
                        if (!response.equals(TomcatBaseCommon)) TomcatBaseCommon = response;
                        break;
                    } else {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.notwritable", new String[]{response});
                    }
                }

                // se copian los archivos jar
                for (File fileJdbc : filesJdbc) {
                    if (dbName != null && !dbName.isEmpty()) {
                        if (dbName.equalsIgnoreCase("postgres") && !fileJdbc.getName().matches("(?i)postgres.+\\.jar")) continue;
                        else if (dbName.equalsIgnoreCase("oracle") && !fileJdbc.getName().matches("(?i)ojdbc.+\\.jar")) continue;
                    }
                    if (!copyPackageFile(fileJdbc, TomcatBaseCommon)) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.copy.fail", new String[]{fileJdbc.getAbsolutePath()});
                        return false;
                    }
                }
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.copy.ok");
                return true;
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.dir.nojar", new String[]{databaseDriverSourceDir});
            }
        } else {
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyDatabaseDrivers.dir.noread", new String[]{databaseDriverSourceDir});
        }
        return false;
    }


    /**
     * Descomprime el archivo zip del servicio de Askosi y lo copia al directorio webapps de Tomcat. Si no existe se pida la ruta donde se instalará,
     * Se modifica el archivo web.xml de Askosi para configurarlo al directorio de datos de Askosi y al dspace desplegado.
     *
     * @param sourcePackageFile archivo zip con el servicio Askosi
     * @return éxito de la copia
     */
    private boolean copyAskosiWebApp(File sourcePackageFile)
    {
        // directorio webapps de Tomcat
        String askosiWebAppDestDir = TomcatBase + fileSeparator + "webapps" + fileSeparator;
        File askosiWebAppDestDirFile = new File(askosiWebAppDestDir);
        String message;
        if (!askosiWebAppDestDirFile.exists() || !askosiWebAppDestDirFile.canWrite()) {
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiWebApp.askosiwebappdestdir.notexist", new String[]{askosiWebAppDestDir});
            message = installerEDMDisplay.getQuestion(currentStepGlobal, "copyAskosiWebApp.askosiwebappdestdir");
        } else {
            message = installerEDMDisplay.getQuestion(currentStepGlobal, "copyAskosiWebApp.askosiwebappdestdir.new") + askosiWebAppDestDirFile.getAbsolutePath();
        }

        // si no existe se pide la ruta
        File finalAskosiWebAppDestDirFile = null;
        while (true) {
            installerEDMDisplay.showMessage(message);
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = response.trim();
                finalAskosiWebAppDestDirFile = new File(response);
            } else {
                response = askosiWebAppDestDir;
                finalAskosiWebAppDestDirFile = new File(askosiWebAppDestDir);
            }
            if (finalAskosiWebAppDestDirFile != null && finalAskosiWebAppDestDirFile.exists() && finalAskosiWebAppDestDirFile.canWrite()) {
                break;
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }

        // se descomprime, copia y se modica el web.xml de Askosi
        if (copyPackageZipFile(sourcePackageFile, finalAskosiWebAppDestDirFile.getAbsolutePath() + fileSeparator)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiWebApp.ok");
            File webXmlFile = new File(new StringBuilder(finalAskosiWebAppDestDirFile.getAbsolutePath()).append
                    (fileSeparator).append("askosi").append(fileSeparator).append("WEB-INF").append(fileSeparator).append("web.xml").toString());
            if (webXmlFile.exists() && webXmlFile.canWrite()) {
                changeWebXml(webXmlFile);
            } else installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiWebApp.nowebxml", new String[]{webXmlFile.getAbsolutePath()});
            return true;
        } else {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiWebApp.fail");
        }
        return false;
    }


    /**
     * Modifica el archivo web.xml de Askosi para indicarle el directorio de datos de Askosi
     *
     * @param webXmlFile archivo web.xml de Askosi
     */
    private void changeWebXml(File webXmlFile)
    {
        try {
            // se abre con DOM el archivo web.xml
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(webXmlFile);
            XPath xpathInputForms = XPathFactory.newInstance().newXPath();

            // se busca el elemento SKOSdirectory
            NodeList results = (NodeList)xpathInputForms.evaluate("//*[contains(*,\"SKOSdirectory\")]", doc, XPathConstants.NODESET);
            if (results.getLength()> 0) {
                Element contextParam = (Element) results.item(0);

                // se busca el elemento context-param como hijo del anterior
                if (contextParam.getTagName().equals("context-param")) {

                    // se busca el elemento param-value como hijo del anterior
                    NodeList resultsParamValue = contextParam.getElementsByTagName("param-value");
                    if (resultsParamValue.getLength() > 0) {

                        // se modifica con la ruta del directorio de datos de Askosi
                        Element valueParam = (Element) resultsParamValue.item(0);
                        Text text = doc.createTextNode(finalAskosiDataDestDirFile.getAbsolutePath());
                        valueParam.replaceChild(text, valueParam.getFirstChild());

                        // se guarda
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                        //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                        DocumentType docType = doc.getDoctype();
                        if (docType != null) {
                            if (docType.getPublicId() != null) transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
                            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
                        }
                        DOMSource source = new DOMSource(doc);
                        StreamResult result = new StreamResult(webXmlFile);
                        transformer.transform(source, result);
                    }
                }
            } else installerEDMDisplay.showQuestion(currentStepGlobal, "changeWebXml.noSKOSdirectory", new String[]{webXmlFile.getAbsolutePath()});
        } catch (ParserConfigurationException e) {
            showException(e);
        } catch (SAXException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (TransformerConfigurationException e) {
            showException(e);
        } catch (TransformerException e) {
            showException(e);
        }

    }


    /**
     * Descomprime y copia el plugin de Askosi para jspui en el directorio WEB-INF/classes de donde está desplegado el jspui
     *
     * @param sourcePackageFile archivo con el zip del plugin
     * @return éxito de la copia
     */
    private boolean copyAskosiPlugJspui(File sourcePackageFile)
    {
        // se pide en directorio donde está deplegado jspui y se comprueba que exista WEB-INF/classes
        File finalAskosiPlugJspDestDirFile = null;
        while (true) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiPlugJspui.dir", new String[]{TomcatBaseFile.getAbsolutePath()});
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(fileSeparator).append("WEB-INF").append(fileSeparator).append("classes").append(fileSeparator).toString();
                finalAskosiPlugJspDestDirFile = new File(response);
            }
            if (finalAskosiPlugJspDestDirFile != null && finalAskosiPlugJspDestDirFile.exists() && finalAskosiPlugJspDestDirFile.canWrite()) {
                break;
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }
        // se descomprime y copia
        if (copyPackageZipFile(sourcePackageFile, finalAskosiPlugJspDestDirFile.getAbsolutePath() + fileSeparator)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiPlugJspui.ok");
            return true;
        } else {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiPlugJspui.fail");
        }
        return false;
    }


    /**
     * Pide el directorio de datos de Askosi o se da la posibilidad de crearlo
     *
     * @return si existe o se ha creado bien
     */
    private boolean getAskosiDataDir()
    {
        finalAskosiDataDestDirFile = null;
        while (true) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiDataDir");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(fileSeparator).toString();
                finalAskosiDataDestDirFile = new File(response);
            }
            if (finalAskosiDataDestDirFile != null) {
                if (finalAskosiDataDestDirFile.exists()) {
                    if (finalAskosiDataDestDirFile.canWrite())
                        break;
                    else
                        installerEDMDisplay.showQuestion(currentStepGlobal, "copyJarsShared.dir.notwritable", new String[]{response});
                } else {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiDataDir.notexist", new String[]{response});
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        showException(e);
                        return false;
                    }
                    if (response == null || response.length() == 0 || response.trim().equals(answerYes)) {
                        if (!finalAskosiDataDestDirFile.mkdir()) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiDataDir.failcreate", new String[]{finalAskosiDataDestDirFile.getAbsolutePath()});
                        } else break;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Descomprime y copia el zip con los repositorios ejemplo de Askosi
     *
     * @param sourcePackageFile archivo zip con los repositorios de Askosi
     * @return  éxito de la copia
     */
    private boolean copyAskosiDataDir(File sourcePackageFile)
    {
        installerEDMDisplay.showLn();
        if (copyPackageZipFile(sourcePackageFile, finalAskosiDataDestDirFile.getAbsolutePath() + fileSeparator)) {
            setAskosiDataDir(finalAskosiDataDestDirFile.getAbsolutePath());
            changeLogFilePath();
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiDataDir.ok");
            return true;
        } else installerEDMDisplay.showQuestion(currentStepGlobal, "copyAskosiDataDir.fail");
        return false;
    }


    /**
     * Cambia la ruta de los logs del servicio Askosi
     */
    private void changeLogFilePath()
    {
        File logFile = new File(finalAskosiDataDestDirFile.getAbsolutePath() + fileSeparator + "log4j.properties");
        if (logFile.exists() && logFile.canWrite()) {
            InputStream is = null;
            try {
                Properties properties = new Properties();
                URL url = logFile.toURI().toURL();
                is = url.openStream();
                properties.load(is);
                String askosiLog = finalAskosiDataDestDirFile.getAbsolutePath() + fileSeparator + "askosi.log";
                if (properties.containsKey("log4j.appender.A1.File") && !properties.getProperty("log4j.appender.A1.File").equals(askosiLog)) {
                    String content = org.apache.commons.io.FileUtils.readFileToString(logFile);
                    content = content.replaceFirst("log4j\\.appender\\.A1\\.File\\s*=\\s*.+",
                            "log4j.appender.A1.File=" + askosiLog);
                    org.apache.commons.io.FileUtils.writeStringToFile(logFile, content, false);
                }
            } catch (MalformedURLException e) {
                showException(e);
            } catch (IOException e) {
                showException(e);
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException e) {
                    showException(e);
                }
            }
        }
    }


    /**
     * Descomprime el zip y copia los archivos a un directorio
     *
     * @param sourcePackageFile archivo zip
     * @param destDir directorio destino de la copia
     * @return   éxito de la copia
     */
    private boolean copyPackageZipFile(File sourcePackageFile, String destDir)
    {
        try {
            // lectura de los archivos que componen el zip
            ZipFile zf = new ZipFile(sourcePackageFile.getAbsolutePath());
            Enumeration entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if (entry.isDirectory()) {
                    File dirAux = new File(destDir + entry.getName());
                    if (!dirAux.exists() && !dirAux.mkdir()) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageZipFile.failcreate", new String[]{dirAux.getAbsolutePath()});
                    }
                } else {
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageZipFile.extract", new String[]{entry.getName()});
                    int index = entry.getName().lastIndexOf(47);
                    if (index == -1) {
                        index = entry.getName().lastIndexOf(92);
                    }
                    if (index > 0) {
                        File dir = new File(destDir + entry.getName().substring(0, index));
                        if (!dir.exists() && !dir.mkdirs()) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageZipFile.failcreate.dir", new String[]{dir.getAbsolutePath()});
                        }
                    }
                    // lectura del archivo interno y posterior escritura al directorio final
                    byte[] buffer = new byte[1024];

                    InputStream in = zf.getInputStream(entry);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destDir + entry.getName()));
                    int len;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
            return true;
        } catch (IOException e) {
            showException(e);
        }
        return false;
    }


    /**
     * Copia un conjunto de archivos a un directorio
     *
     * @param fileIterPack puntero a la lista de archivos
     * @param TomcatBaseShared directorio destino
     * @return  éxito de la copia
     */
    private boolean copyPackagesFiles(Iterator<File> fileIterPack, String TomcatBaseShared)
    {
        while (fileIterPack.hasNext()) {
            File sourcePackageFile = fileIterPack.next();
            if (!copyPackageFile(sourcePackageFile, TomcatBaseShared)) return false;
        }
        return true;
    }


    /**
     * Copia un archivo a un directorio
     *
     * @param sourcePackageFile archivo a copiar
     * @param TomcatBaseShared directorio destino
     * @return éxito de la copia
     */
    private boolean copyPackageFile (File sourcePackageFile, String TomcatBaseShared)
    {
        String nameSourcePackage = sourcePackageFile.getName();
        String nameDestPackage = TomcatBaseShared + fileSeparator + nameSourcePackage;
        File desPackageFile = new File(nameDestPackage);
        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageFile.copyfile", new String[]{sourcePackageFile.getAbsolutePath(), desPackageFile.getParent()});
        if (desPackageFile.exists()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageFile.fileexists", new String[]{desPackageFile.getAbsolutePath()});
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if (response.trim().equalsIgnoreCase(answerYes)) {
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "copyPackageFile.orverwritingfile");
                try {
                    org.apache.commons.io.FileUtils.copyFile(sourcePackageFile, desPackageFile);
                } catch (IOException e) {
                    showException(e);
                    return false;
                }
            }
        } else {
            try {
                org.apache.commons.io.FileUtils.copyFile(sourcePackageFile, desPackageFile);
            } catch (IOException e) {
                showException(e);
                return false;
            }
        }
        return true;
    }


    /**
     * Calcula los md5 de los archivos en el directorio packages del instalador y los comprueba con el array packagesMD5 en {@link InstallerEDMBase}
     *
     * @return el directorio de packages o null si no existe packages
     */
    public File checkPackages()
    {
        String myDirPackages = myInstallerDirPath + fileSeparator + "packages";
        File dirPackages = new File(myDirPackages);
        if (dirPackages.exists() && dirPackages.isDirectory() && dirPackages.canRead()) {
            Iterator<File> fileIter = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, null, false);
            while (fileIter.hasNext()) {
                File packageFile = fileIter.next();
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.file", new String[]{packageFile.getAbsolutePath()});
                if (!packageFile.canRead()) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.file.notreadable", new String[]{packageFile.getAbsolutePath()});
                    return null;
                }
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(packageFile);
                    String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                    if (verbose) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.file.md5", new String[]{md5});
                    }
                    for (int i=0; i < packages.length; i++) {
                        if (packages[i].equals(packageFile.getName())) {
                            if (!md5.equals(packagesMD5[i])) {
                                installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.file.md5.nok", new String[]{packageFile.getAbsolutePath()});
                                return null;
                            } else if (verbose) {
                                installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.file.ok");
                            }
                            break;
                        }
                    }
                } catch (FileNotFoundException e) {
                    showException(e);
                    return null;
                } catch (IOException e) {
                    showException(e);
                    return null;
                }
            }
            return dirPackages;
        } else {
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkPackages.directory.notaccessible", new String[]{myDirPackages});
        }
        return null;
    }

}


/**
 * @class FileListFilter
 *
 * Clase para filtrar por nombre y extensión, y recoger archivos de un directorio
 *
 */
class FileListFilter implements FilenameFilter
{
    /**
     * nombre por el que filtrar
     */
    private String inName;

    /**
     * extensión por la que filtrar
     */
    private String extension;

    /**
     * Construtor para asignar nombre y extensión
     *
     * @param inName nombre por el que filtrar
     * @param extension extensión por la que filtrar
     */
    public FileListFilter(String inName, String extension)
    {
        this.inName = inName;
        this.extension = extension;
    }

    /**
     * Comprueba si el archivo actual coincide con los criterios especificados
     *
     * @param directory directorio del que buscar los archivos
     * @param filename nombre del archivo actualmente procesado
     * @return éxito de la búsqueda
     */
    public boolean accept(File directory, String filename)
    {
        boolean fileOK = true;

        if (inName != null) {
            fileOK &= filename.indexOf(inName) > 0;
        }
        if (extension != null) {
            fileOK &= filename.endsWith('.' + extension);
        }
        return fileOK;
    }
}

