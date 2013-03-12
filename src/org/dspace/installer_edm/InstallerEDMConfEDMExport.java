package org.dspace.installer_edm;

import org.apache.commons.io.filefilter.RegexFileFilter;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * @class InstallerEDMConfEDMExport
 *
 * Clase para configurar el servicio EDMExport según el dspace instalado.
 * Se copia el archivo EDMExport de packages al directorio de trabajo del instalador para
 * poder modificarlo.
 * Se modifica web.xml dentro de EDMExport.war para que apunte al archivo dspace.cfg de dspace.
 * Se cambian archivos jar de la api de dspace y de lucene para adaptarlos a la versión de lucen del dspace desplegado
 *
 */
public class InstallerEDMConfEDMExport extends InstallerEDMBase
{

    /**
     * Ruta del archivo EDMExport.war
     */
    private String eDMExportWar;

    /**
     * Archivo EDMExport.war
     */
    private File eDMExportWarFile;

    /**
     * Archivo EDMExport.war en el directorio de trabajo del instalador
     */
    private File eDMExportWarWorkFile;

    /**
     * Archivo de tipo jar para leer EDMExport.war en el directorio de trabajo del instalador
     */
    private JarFile eDMExportWarJarFile;

    /**
     * Documento jdom para leer el archivo web.xml
     */
    private Document eDMExportDocument;

    /**
     * Archivo con la api de dspace
     */
    private File dspaceApi = null;

    /**
     * Lista de jar con las librerías de lucene
     */
    private ArrayList<File> luceneLibs = null;

    /**
     * Consulta xpath para buscar la propiedad de dspace-config en web.xml
     */
    private static final String xpathDspaceConfigTemplate = "//*[contains(*,\"dspace-config\")]";

    /**
     *
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param EDMExportWar ruta del archivo EDMExport.war
     */
    public InstallerEDMConfEDMExport(int currentStepGlobal, String EDMExportWar)
    {
        super(currentStepGlobal);
        this.eDMExportWar = EDMExportWar;
        this.eDMExportWarFile = new File(EDMExportWar);
    }

    /**
     * Se comprueba que el archivo EDMExport.war exista y se pueda modificar o se pide su ruta
     *
     * @return si existe y se puede modificar
     * @throws IOException
     */
    private boolean checkEDMExporWar() throws IOException
    {
        if (!eDMExportWarFile.exists() || !eDMExportWarFile.isFile() || !eDMExportWarFile.canWrite()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkEDMExporWar.notexists", new String[]{eDMExportWar});
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkEDMExporWar.newwar");
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (!response.isEmpty()) {
                    eDMExportWarFile = new File(response);
                    return checkEDMExporWar();
                }
                else return false;
            } while (true);
        }
        return true;
    }


    /**
     * Abre el archivo EDMExport.war y se recorre la lista de archivos que lo componen.
     * En web.xml lo abre con jdom y pide la ruta de dspace.cfg para modificarlo
     * Para los jar con la api de dspace y de lucene muestra cuál hay en el war y cuál en dspace y pregunta si se cambia
     */
    public void configure()
    {
        try {
            // comprobar validez del war
            if (checkEDMExporWar()) {
                // copiar al directorio de trabajo
                eDMExportWarWorkFile = new File(myInstallerWorkDirPath + fileSeparator + eDMExportWarFile.getName());
                copyDspaceFile2Work(eDMExportWarFile, eDMExportWarWorkFile, "configure.edmexport");

                // abrir el war
                eDMExportWarJarFile = new JarFile(eDMExportWarWorkFile);
                // buscar web.xml
                ZipEntry edmExportWebZipentry = eDMExportWarJarFile.getEntry("WEB-INF/web.xml");
                if (edmExportWebZipentry == null) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.notwebxml");
                else {
                    // crear dom de web.xml
                    InputStream is = eDMExportWarJarFile.getInputStream(edmExportWebZipentry);
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    eDMExportDocument = builder.parse(is);
                    // buscar dspace-config
                    XPath xpathInputForms = XPathFactory.newInstance().newXPath();
                    NodeList resultsDspaceConfig = (NodeList)xpathInputForms.evaluate(xpathDspaceConfigTemplate, eDMExportDocument, XPathConstants.NODESET);
                    if (resultsDspaceConfig.getLength() == 0) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.nopath");
                    } else {
                        // preguntar ruta de dspace.cfg y configurar los jar
                        Element contextParam = (Element) resultsDspaceConfig.item(0);
                        if (contextParam.getTagName().equals("context-param")) {
                            NodeList resultsParamValue = contextParam.getElementsByTagName("param-value");
                            if (resultsParamValue.getLength() > 0) {
                                Element valueParam = (Element) resultsParamValue.item(0);
                                String dspaceCfg = DspaceDir + "config" + fileSeparator + "dspace.cfg";
                                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg", new String[] {dspaceCfg});
                                File dspaceCfgFile = new File(dspaceCfg);
                                String response = null;
                                do {
                                    response = br.readLine();
                                    if (response == null) continue;
                                    response = response.trim();
                                    if (response.isEmpty()) {
                                        break;
                                    } else {
                                        dspaceCfgFile = new File(response);
                                        if (dspaceCfgFile.exists()) break;
                                    }
                                } while (true);
                                Text text = eDMExportDocument.createTextNode(dspaceCfgFile.getAbsolutePath());
                                valueParam.replaceChild(text, valueParam.getFirstChild());
                                // jar con la api de dspace
                                findDspaceApi();
                                // jars con lucene
                                findLuceneLib();
                                // escribir el nuevo war con las modificaciones
                                writeNewJar();
                                eDMExportWarJarFile = new JarFile(eDMExportWarWorkFile);
                                installerEDMDisplay.showLn();
                                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.ok", new String[] {eDMExportWarWorkFile.getAbsolutePath()});
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (ParserConfigurationException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (SAXException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (XPathExpressionException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (TransformerException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        }
    }


    /**
     * Busca el jar con la api de dspace en el directorio lib de dspace
     */
    private void findDspaceApi()
    {
        File dspaceLib = new File(DspaceDir + "lib");
        if (dspaceLib.isDirectory() && dspaceLib.canRead()) {
            FileFilter fileFilter = new RegexFileFilter("^dspace-api-\\d+.+\\.jar$");
            File[] files = dspaceLib.listFiles(fileFilter);
            if (files.length > 0) {
                dspaceApi = files[0];
                if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "replaceDspaceApi.dspaceApi", new String[]{dspaceApi.getAbsolutePath()});
            } else installerEDMDisplay.showQuestion(currentStepGlobal, "replaceDspaceApi.notmatch", new String[]{dspaceLib.getAbsolutePath(), "dspace-api-\\d+.+\\.jar"});
        } else installerEDMDisplay.showQuestion(currentStepGlobal, "replaceDspaceApi.notdir", new String[]{DspaceDir + "lib"});
    }


    /**
     * Busca los jars con las librerías de lucene en el directorio lib de dspace
     */
    private void findLuceneLib()
    {
        File dspaceLib = new File(DspaceDir + "lib");
        if (dspaceLib.isDirectory() && dspaceLib.canRead()) {
            FileFilter fileFilter = new RegexFileFilter("^lucene-.+?-\\d+.+\\.jar$");
            File[] files = dspaceLib.listFiles(fileFilter);
            if (files.length > 0) {
                luceneLibs = new ArrayList<File>();
                for (File fileLucene : files) {
                    luceneLibs.add(fileLucene);
                    if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "replaceLuceneCoreLib.luceneCoreLib", new String[]{fileLucene.getAbsolutePath()});
                }
            } else installerEDMDisplay.showQuestion(currentStepGlobal, "replaceLuceneCoreLib.notmatch", new String[]{dspaceLib.getAbsolutePath(), "lucene-.+?-\\d+.+\\.jar"});
        } else installerEDMDisplay.showQuestion(currentStepGlobal, "replaceLuceneCoreLib.notdir", new String[]{DspaceDir + "lib"});
    }


    /**
     * Recorre el war para escribir los archivos web.xml, la api de dspace y los jar de lucene
     *
     * @throws IOException
     * @throws TransformerException
     */
    private void writeNewJar() throws IOException, TransformerException
    {
        final int BUFFER_SIZE = 1024;
        // directorio de trabajo
        File jarDir = new File(this.eDMExportWarJarFile.getName()).getParentFile();
        // archivo temporal del nuevo war
        File newJarFile = File.createTempFile("EDMExport", ".jar", jarDir);
        newJarFile.deleteOnExit();
        // flujo de escritura para el nuevo war
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newJarFile));

        try {
            // recorrer los archivos del war
            Enumeration<JarEntry> entries = eDMExportWarJarFile.entries();
            // librerías de lucene
            Pattern luceneLibPattern = Pattern.compile("^WEB-INF/lib/(lucene-.+?)-\\d+.+\\.jar$");
            boolean newApiCopied = false;
            if (dspaceApi == null) newApiCopied = true;
            boolean replace = false;
            // recorrer
            while (entries.hasMoreElements()) {
                replace = false;
                installerEDMDisplay.showProgress('.');
                JarEntry entry = entries.nextElement();
                InputStream intputStream = null;
                // todos menos web.xml
                if (!entry.getName().equals("WEB-INF/web.xml")) {
                    // api de dspace, se muestra la actual y la de dspace para pedir si se copia
                    if (!newApiCopied && entry.getName().matches("^WEB-INF/lib/dspace-api-\\d+.+\\.jar$")) {
                        String response = null;
                        do {
                            installerEDMDisplay.showLn();
                            installerEDMDisplay.showQuestion(currentStepGlobal, "writeNewJar.replace.question", new String[]{entry.getName(), "WEB-INF/lib/" + dspaceApi.getName(), dspaceApi.getAbsolutePath()});
                            response = br.readLine();
                            if (response == null) continue;
                            response = response.trim();
                            if (response.isEmpty() || response.equalsIgnoreCase(answerYes)) {
                                replace = true;
                                break;
                            } else if (response.equalsIgnoreCase("n")) {
                                break;
                            }
                        } while (true);
                        // se reemplaza por la de dspace
                        if (replace) {
                            JarEntry newJarEntry = new JarEntry("WEB-INF/lib/" + dspaceApi.getName());
                            newJarEntry.setCompressedSize(-1);
                            jarOutputStream.putNextEntry(newJarEntry);
                            intputStream = new FileInputStream(dspaceApi);
                            newApiCopied = true;
                            if (debug) {
                                installerEDMDisplay.showLn();
                                installerEDMDisplay.showQuestion(currentStepGlobal, "writeNewJar.replace", new String[]{entry.getName(), "WEB-INF/lib/" + dspaceApi.getName(), dspaceApi.getAbsolutePath()});
                                installerEDMDisplay.showLn();
                            }
                        }
                    } else {
                        // librerías de lucene
                        Matcher luceneLibMatcher = luceneLibPattern.matcher(entry.getName());
                        if (luceneLibMatcher.find()) {
                            String prefixLuceneLib = luceneLibMatcher.group(1);
                            File luceneLibFile = null;
                            String patternFile = prefixLuceneLib + "-\\d+.+\\.jar";
                            for (File file : luceneLibs) {
                                if (file.getName().matches(patternFile)) {
                                    luceneLibFile = file;
                                    break;
                                }
                            }
                            if (luceneLibFile != null) {
                                String response = null;
                                do {
                                    installerEDMDisplay.showLn();
                                    installerEDMDisplay.showQuestion(currentStepGlobal, "writeNewJar.replace.question", new String[]{entry.getName(), "WEB-INF/lib/" + luceneLibFile.getName(), luceneLibFile.getAbsolutePath()});
                                    response = br.readLine();
                                    if (response == null) continue;
                                    response = response.trim();
                                    if (response.isEmpty() || response.equalsIgnoreCase(answerYes)) {
                                        replace = true;
                                        break;
                                    } else if (response.equalsIgnoreCase("n")) {
                                        break;
                                    }
                                } while (true);
                                // se reemplaza por la de dspace
                                if (replace) {
                                    JarEntry newJarEntry = new JarEntry("WEB-INF/lib/" + luceneLibFile.getName());
                                    newJarEntry.setCompressedSize(-1);
                                    jarOutputStream.putNextEntry(newJarEntry);
                                    intputStream = new FileInputStream(luceneLibFile);
                                    if (debug) {
                                        installerEDMDisplay.showLn();
                                        installerEDMDisplay.showQuestion(currentStepGlobal, "writeNewJar.replace", new String[]{entry.getName(), "WEB-INF/lib/" + luceneLibFile.getName(), luceneLibFile.getAbsolutePath()});
                                        installerEDMDisplay.showLn();
                                    }
                                }
                            }
                        // si no era la api de dspace o las librerías de lucene se copia tal cual
                        } else if (!replace) {
                            JarEntry entryOld = new JarEntry(entry);
                            entryOld.setCompressedSize(-1);
                            jarOutputStream.putNextEntry(entryOld);
                            intputStream = eDMExportWarJarFile.getInputStream(entry);
                        }
                    }
                    if (intputStream == null) {
                        if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "writeNewJar.notIS", new String[]{entry.getName()});
                        continue;
                    }
                    // se lee el archivo y se copia al flujo de escritura del war
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    while ((count = intputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                        jarOutputStream.write(data, 0, count);
                    }
                    intputStream.close();
                }
            }
            installerEDMDisplay.showLn();
            // se añade web.xml al war
            addNewWebXml(jarOutputStream);
            // cerramos el archivo jar y borramos el war
            eDMExportWarJarFile.close();
            eDMExportWarWorkFile.delete();
            // sustituimos el viejo por el temporal
            if (newJarFile.renameTo(eDMExportWarWorkFile) && eDMExportWarWorkFile.setExecutable(true, true)) {
                eDMExportWarWorkFile = new File(myInstallerWorkDirPath + fileSeparator + eDMExportWarFile.getName());
            } else {
                throw new IOException();
            }
        } finally {
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
        }
    }


    /**
     * Añadimos el contenido del documento jdom como archivo web.xml al flujo de escritura del war
     *
     * @param jarOutputStream flujo de escritura del war
     * @throws TransformerException
     * @throws IOException
     */
    private void addNewWebXml(JarOutputStream jarOutputStream) throws TransformerException, IOException
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DocumentType docType = eDMExportDocument.getDoctype();
        if (docType != null) {
            if (docType.getPublicId() != null) transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
        }
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(eDMExportDocument);
        transformer.transform(source, result);
        String xmlString = sw.toString();
        jarOutputStream.putNextEntry(new JarEntry("WEB-INF/web.xml"));
        jarOutputStream.write(xmlString.getBytes());
        jarOutputStream.closeEntry();
    }

}
