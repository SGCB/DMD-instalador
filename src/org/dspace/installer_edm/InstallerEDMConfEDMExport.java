package org.dspace.installer_edm;

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
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 1/02/13
 * Time: 9:38
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMConfEDMExport extends InstallerEDMBase
{

    private String eDMExportWar;
    private File eDMExportWarFile;
    private File eDMExportWarWorkFile;
    private JarFile eDMExportWarJarFile;
    private Document eDMExportDocument;

    private static final String xpathDspaceConfigTemplate = "//*[contains(*,\"dspace-config\")]";

    public InstallerEDMConfEDMExport(int currentStepGlobal, String EDMExportWar)
    {
        super(currentStepGlobal);
        this.eDMExportWar = EDMExportWar;
        this.eDMExportWarFile = new File(EDMExportWar);
    }

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


    public void configure()
    {
        try {
            if (checkEDMExporWar()) {
                eDMExportWarWorkFile = new File(myInstallerWorkDirPath + fileSeparator + eDMExportWarFile.getName());
                org.apache.commons.io.FileUtils.copyFile(eDMExportWarFile, eDMExportWarWorkFile);

                eDMExportWarJarFile = new JarFile(eDMExportWarWorkFile);
                ZipEntry edmExportWebZipentry = eDMExportWarJarFile.getEntry("WEB-INF/web.xml");
                if (edmExportWebZipentry == null) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.notwebxml");
                else {
                    InputStream is = eDMExportWarJarFile.getInputStream(edmExportWebZipentry);
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    eDMExportDocument = builder.parse(is);
                    XPath xpathInputForms = XPathFactory.newInstance().newXPath();
                    NodeList resultsDspaceConfig = (NodeList)xpathInputForms.evaluate(xpathDspaceConfigTemplate, eDMExportDocument, XPathConstants.NODESET);
                    if (resultsDspaceConfig.getLength() == 0) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.nopath");
                    } else {
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
            e.printStackTrace();
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (SAXException e) {
            e.printStackTrace();
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        } catch (TransformerException e) {
            e.printStackTrace();
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacecfg.nok");
        }
    }


    private void writeNewJar() throws IOException, TransformerException
    {
        final int BUFFER_SIZE = 1024;
        File jarDir = new File(this.eDMExportWarJarFile.getName()).getParentFile();
        File newJarFile = File.createTempFile("EDMExport", ".jar", jarDir);
        newJarFile.deleteOnExit();
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newJarFile));

        try {
            Enumeration<JarEntry> entries = eDMExportWarJarFile.entries();
            while (entries.hasMoreElements()) {
                installerEDMDisplay.showProgress('.');
                JarEntry entry = entries.nextElement();
                if (!entry.getName().equals("WEB-INF/web.xml")) {
                    JarEntry entryOld = new JarEntry(entry);
                    entryOld.setCompressedSize(-1);
                    jarOutputStream.putNextEntry(entryOld);
                    InputStream intputStream = eDMExportWarJarFile.getInputStream(entry);
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    while ((count = intputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                        jarOutputStream.write(data, 0, count);
                    }
                    intputStream.close();
                }
            }
            installerEDMDisplay.showLn();
            addNewWebXml(jarOutputStream);
            eDMExportWarJarFile.close();
            eDMExportWarWorkFile.delete();
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
