package org.dspace.installer_edm;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 11/02/13
 * Time: 13:07
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMCrosswalkXSL extends InstallerEDMBase
{
    private File dim2EdmNewFile;
    private File dspaceDirConfNewFile;
    private File dspaceConfNewFile;
    private File oaicatNewFile;
    private Document docDim2EdmXsl;

    private static final String XSL_PATH = "http://www.w3.org/1999/XSL/Transform";
    private static final String EDM_PATH = "http://www.europeana.eu/schemas/edm/";
    private static final String ORE_PATH = "http://www.openarchives.org/ore/terms/";
    private static final String RDF_PATH = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    // //*[namespace-uri() = 'http://www.w3.org/1999/XSL/Transform' and local-name() = 'variable'][@name='handle']
    private static final String xpathDim2EdmXslVariableHandleTemplate = "//xsl:variable[@name='handle']";

    // //*[namespace-uri() = 'http://www.w3.org/1999/XSL/Transform' and local-name() = 'variable'][@name='ident_uri_orig']
    private static final String xpathDim2EdmXslVariableIdentUriOrigTemplate = "//xsl:variable[@name='ident_uri_orig']";

    // //*[namespace-uri() = 'http://www.openarchives.org/ore/terms/' and local-name() = 'Aggregation']/*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'dataProvider']
    private static final String xpathDim2EdmXslAggregationDataProviderTemplate = "//ore:Aggregation/edm:dataProvider";

    // //*[namespace-uri() = 'http://www.openarchives.org/ore/terms/' and local-name() = 'Aggregation']/*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'provider']
    private static final String xpathDim2EdmXslAggregationProviderTemplate = "//ore:Aggregation/edm:provider";

    // //*[namespace-uri() = 'http://www.openarchives.org/ore/terms/' and local-name() = 'Aggregation']/*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'rights']
    private static final String xpathDim2EdmXslRightsTemplate = "//ore:Aggregation/edm:rights";

    // //*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'ProvidedCHO']/*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'type']
    private static final String xpathDim2EdmXslProviderCHOTypeTemplate = "//edm:ProvidedCHO/edm:type";

    // //*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'ProvidedCHO']/*[namespace-uri() = 'http://www.europeana.eu/schemas/edm/' and local-name() = 'language']
    private static final String xpathDim2EdmXslProviderCHOLanguageTemplate = "//edm:ProvidedCHO/edm:language";

    private NamespaceContext ctx;


    public InstallerEDMCrosswalkXSL(int currentStepGlobal)
    {
        super(currentStepGlobal);

        ctx = new NamespaceContext()
        {
            public String getNamespaceURI(String prefix)
            {
                String uri;
                if (prefix.equals("xsl"))
                    uri = XSL_PATH;
                else if (prefix.equals("edm"))
                    uri = EDM_PATH;
                else if (prefix.equals("ore"))
                    uri = ORE_PATH;
                else
                    uri = null;
                return uri;
            }

            public String getPrefix(String namespaceURI)
            {
                return null;
            }

            public Iterator getPrefixes(String namespaceURI)
            {
                return null;
            }
        };
    }


    public void configure()
    {
        String dspaceConfName = DspaceDir + "config" + fileSeparator + "dspace.cfg";
        File dspaceConfFile = new File(dspaceConfName);
        String dspaceOaiCatName = DspaceDir + "config" + fileSeparator + "oaicat.properties";
        File dspaceOaiCatFile = new File(dspaceOaiCatName);
        File dspaceDirConfNewFile = new File(myInstallerWorkDirPath);
        if (dspaceConfFile.exists() && dspaceConfFile.canRead() && dspaceOaiCatFile.exists() && dspaceOaiCatFile.canRead() && dspaceDirConfNewFile.canWrite()) {
            try {
                dspaceConfNewFile = new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "dspace.cfg");
                copyDspaceFile2Work(dspaceConfFile, dspaceConfNewFile, "configure.dspacecfg");
                oaicatNewFile = new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "oaicat.properties");
                copyDspaceFile2Work(dspaceOaiCatFile, oaicatNewFile, "configure.oaicat");
                if (!checkOldEDMCrosswalk()) {
                    File dim2EdmFile = new File(myInstallerDirPath + fileSeparator + "packages" + fileSeparator + "DIM2EDM.xsl");
                    dim2EdmNewFile = new File(myInstallerWorkDirPath + fileSeparator + dim2EdmFile.getName());
                    copyDspaceFile2Work(dim2EdmFile, dim2EdmNewFile, "configure.dim2edm");
                    if (!checkOldOaiProperties()) {
                        InputSource inputSourceDim2EdmXsl = new InputSource(dim2EdmNewFile.getAbsolutePath());
                        docDim2EdmXsl = getDocumentFromInputSource(inputSourceDim2EdmXsl);
                        configureDim2EdmXsl();
                        writeDim2EdmXsl();
                        writeDspaceCfg();
                        writeOaiCat();
                        installerEDMDisplay.showLn();
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.ok");
                        installerEDMDisplay.showLn();
                    }
                }
            } catch (IOException e) {
                showException(e);
            } catch (ParserConfigurationException e) {
                showException(e);
            } catch (SAXException e) {
                showException(e);
            } catch (TransformerException e) {
                showException(e);
            }
        } else installerEDMDisplay.showQuestion(currentStepGlobal, "configure.dspacedirconf.notexist", new String [] {dspaceConfName, dspaceOaiCatName, dspaceDirConfNewFile.getAbsolutePath()});
    }


    public void writeOaiCat() throws IOException
    {
        Writer out = null;
        String edm = "##" +  getTime() + " Appended by installerEDM\nCrosswalks.edm=org.dspace.app.oai.PluginCrosswalk\n";
        try {
            out = new OutputStreamWriter(new FileOutputStream(oaicatNewFile, true));
            out.write(edm);
        } catch (IOException e) {
            showException(e);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }


    public void writeDspaceCfg() throws IOException
    {
        Writer out = null;
        StringBuilder edmSB = new StringBuilder("##\n").append("## Configure XSLT-driven submission crosswalk for Partial EDM\n").append("## ").append(getTime()).append(" Appended by installerEDM\n").append("##\n");
        edmSB.append("crosswalk.dissemination.edm.stylesheet = crosswalks/DIM2EDM.xsl\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.dcterms=http://purl.org/dc/terms/\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.edm=http://www.europeana.eu/schemas/edm/\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.enrichment=http://www.europeana.eu/schemas/edm/enrichment/\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.owl=http://www.w3.org/2002/07/owl#\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.wgs84=http://www.w3.org/2003/01/geo/wgs84_pos#\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.skos=http://www.w3.org/2004/02/skos/core#\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.oai=http://www.openarchives.org/OAI/2.0/\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.ore=http://www.openarchives.org/ore/terms/\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.rdf=http://www.w3.org/1999/02/22-rdf-syntax-ns#\n");
        edmSB.append("crosswalk.dissemination.edm.namespace.dc=http://purl.org/dc/elements/1.1/\n");
        edmSB.append("crosswalk.dissemination.edm.schemaLocation=http://www.w3.org/1999/02/22-rdf-syntax-ns# EDM.xsd\n");
        edmSB.append("crosswalk.dissemination.edm.preferList = true\n");
        String edm = edmSB.toString();
        try {
            out = new OutputStreamWriter(new FileOutputStream(dspaceConfNewFile, true));
            out.write(edm);
        } catch (IOException e) {
            showException(e);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }


    public void writeDim2EdmXsl() throws IOException, TransformerException
    {
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(dim2EdmNewFile, false));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DocumentType docType = docDim2EdmXsl.getDoctype();
            if (docType != null) {
                if (docType.getPublicId() != null) transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
            }
            transformer.transform(new DOMSource(docDim2EdmXsl), new StreamResult(out));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }


    private void configureDim2EdmXsl()
    {
        configureDim2EdmXslVariableHandle();
        configureDim2EdmXslVariableIdentUriOrig();
        configureDim2EdmXslAggregationDataProvider();
        configureDim2EdmXslAggregationProvider();
        configureDim2EdmXslRights();
        configureDim2EdmXslProviderCHOType();
        configureDim2EdmXslProviderCHOLanguage();
    }


    private void configureDim2EdmXslVariableHandle()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslVariableHandleTemplate);
            if (results.getLength() > 0) {
                Element variableHandleElement = (Element) results.item(0);
                String currentValue = (variableHandleElement.hasAttribute("select"))?variableHandleElement.getAttribute("select"):"";
                if (!currentValue.isEmpty()) {
                    Pattern patternUri = Pattern.compile("^substring-after\\s*\\(\\s*\\$ident_uri\\s*,\\s*'([^']+)'\\s*\\)$", Pattern.CASE_INSENSITIVE);
                    Matcher matcherUri = patternUri.matcher(currentValue);
                    if (matcherUri.find()) {
                        currentValue = (String) matcherUri.group(1);
                    }
                }
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslVariableHandle", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty()) {
                        if (isValidURI(response)) break;
                    } else if (isValidURI(currentValue)) {
                        response = currentValue;
                        break;
                    }
                } while (true);
                if (!variableHandleElement.hasAttribute("select")) {
                    Attr newValueAttr = docDim2EdmXsl.createAttribute("select");
                    newValueAttr.setValue("substring-after($ident_uri, '" + response + "')");
                    variableHandleElement.setAttributeNode(newValueAttr);
                }
                else variableHandleElement.getAttributeNode("select").setValue("substring-after($ident_uri, '" + response + "')");
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }

    private void configureDim2EdmXslVariableIdentUriOrig()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslVariableIdentUriOrigTemplate);
            if (results.getLength() > 0) {
                Element variableIdentUriOrigElement = (Element) results.item(0);
                String currentValue = (variableIdentUriOrigElement.hasChildNodes())?variableIdentUriOrigElement.getFirstChild().getNodeValue():"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslVariableIdentUriOrig", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty()) {
                        if (isValidURI(response)) break;
                    } else if (isValidURI(currentValue)) {
                        response = currentValue;
                        break;
                    }
                } while (true);
                Text newValueElement = docDim2EdmXsl.createTextNode(response);
                if (variableIdentUriOrigElement.hasChildNodes()) variableIdentUriOrigElement.replaceChild(newValueElement, variableIdentUriOrigElement.getFirstChild());
                else variableIdentUriOrigElement.appendChild(newValueElement);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }

    private void configureDim2EdmXslAggregationDataProvider()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslAggregationDataProviderTemplate);
            if (results.getLength() > 0) {
                Element variableAggregationDataProviderElement = (Element) results.item(0);
                String currentValue = (variableAggregationDataProviderElement.hasChildNodes())?variableAggregationDataProviderElement.getFirstChild().getNodeValue():"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslAggregationDataProvider", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty() || !currentValue.isEmpty()) {
                        if (response.isEmpty()) response = currentValue;
                        break;
                    }
                } while (true);
                Text newValueElement = docDim2EdmXsl.createTextNode(response);
                if (variableAggregationDataProviderElement.hasChildNodes()) variableAggregationDataProviderElement.replaceChild(newValueElement, variableAggregationDataProviderElement.getFirstChild());
                else variableAggregationDataProviderElement.appendChild(newValueElement);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }

    private void configureDim2EdmXslAggregationProvider()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslAggregationProviderTemplate);
            if (results.getLength() > 0) {
                Element variableAggregationProviderElement = (Element) results.item(0);
                String currentValue = (variableAggregationProviderElement.hasChildNodes())?variableAggregationProviderElement.getFirstChild().getNodeValue():"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslAggregationProvider", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty() || !currentValue.isEmpty()) {
                        if (response.isEmpty()) response = currentValue;
                        break;
                    }
                } while (true);
                Text newValueElement = docDim2EdmXsl.createTextNode(response);
                if (variableAggregationProviderElement.hasChildNodes()) variableAggregationProviderElement.replaceChild(newValueElement, variableAggregationProviderElement.getFirstChild());
                else variableAggregationProviderElement.appendChild(newValueElement);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }

    private void configureDim2EdmXslRights()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslRightsTemplate);
            if (results.getLength() > 0) {
                Element variableHandleElement = (Element) results.item(0);
                String currentValue = (variableHandleElement.hasAttributeNS(RDF_PATH, "resource"))?variableHandleElement.getAttributeNS(RDF_PATH, "resource"):"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslRights", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty()) {
                        if (isValidURI(response)) break;
                    } else if (isValidURI(currentValue)) {
                        response = currentValue;
                        break;
                    }
                } while (true);
                if (!variableHandleElement.hasAttributeNS(RDF_PATH, "resource")) {
                    Attr newValueAttr = docDim2EdmXsl.createAttributeNS(RDF_PATH, "resource");
                    newValueAttr.setValue(response);
                    variableHandleElement.setAttributeNode(newValueAttr);
                }
                else variableHandleElement.getAttributeNodeNS(RDF_PATH, "resource").setValue(response);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }


    private void configureDim2EdmXslProviderCHOType()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslProviderCHOTypeTemplate);
            if (results.getLength() > 0) {
                Element variableProviderCHOTypeElement = (Element) results.item(0);
                String currentValue = (variableProviderCHOTypeElement.hasChildNodes())?variableProviderCHOTypeElement.getFirstChild().getNodeValue():"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslProviderCHOType", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty() || !currentValue.isEmpty()) {
                        if (response.isEmpty()) response = currentValue;
                        break;
                    }
                } while (true);
                Text newValueElement = docDim2EdmXsl.createTextNode(response);
                if (variableProviderCHOTypeElement.hasChildNodes()) variableProviderCHOTypeElement.replaceChild(newValueElement, variableProviderCHOTypeElement.getFirstChild());
                else variableProviderCHOTypeElement.appendChild(newValueElement);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }


    private void configureDim2EdmXslProviderCHOLanguage()
    {
        try {
            NodeList results = searchXpath(xpathDim2EdmXslProviderCHOLanguageTemplate);
            if (results.getLength() > 0) {
                Element variableProviderCHOLanguageElement = (Element) results.item(0);
                String currentValue = (variableProviderCHOLanguageElement.hasChildNodes())?variableProviderCHOLanguageElement.getFirstChild().getNodeValue():"";
                String response = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDim2EdmXslProviderCHOLanguage", new String[]{currentValue});
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty() || !currentValue.isEmpty()) {
                        if (response.isEmpty()) response = currentValue;
                        break;
                    }
                } while (true);
                Text newValueElement = docDim2EdmXsl.createTextNode(response);
                if (variableProviderCHOLanguageElement.hasChildNodes()) variableProviderCHOLanguageElement.replaceChild(newValueElement, variableProviderCHOLanguageElement.getFirstChild());
                else variableProviderCHOLanguageElement.appendChild(newValueElement);
            }
        } catch (XPathExpressionException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
    }


    private NodeList searchXpath(String expression) throws XPathExpressionException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(ctx);
        NodeList results = (NodeList)xpath.evaluate(expression, docDim2EdmXsl, XPathConstants.NODESET);
        return results;
    }


    private boolean checkOldOaiProperties() throws IOException
    {
        InputStream is = null;
        try {
            Properties properties = new Properties();
            URL url = oaicatNewFile.toURI().toURL();
            is = url.openStream();
            properties.load(is);
            String plugin;
            if (properties.containsKey("Crosswalks.edm")) {
                plugin = properties.getProperty("Crosswalks.edm");
                if (plugin.equals("org.dspace.app.oai.PluginCrosswalk")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "checkOldOaiProperties.crosswalk.xsl.exist", new String[]{oaicatNewFile.getAbsolutePath()});
                } else if (plugin.equals("org.dspace.app.oai.EDMCrosswalk")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "checkOldOaiProperties.crosswalk.java.exist", new String[]{oaicatNewFile.getAbsolutePath()});
                } else {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "checkOldOaiProperties.crosswalk.unknown.exist", new String[]{oaicatNewFile.getAbsolutePath()});
                }
                return true;
            }
        } finally {
            if (is != null) is.close();
        }
        return false;
    }

    private boolean checkOldEDMCrosswalk() throws IOException
    {
        InputStream is = null;
        try {
            Properties properties = new Properties();
            URL url = dspaceConfNewFile.toURI().toURL();
            is = url.openStream();
            properties.load(is);
            if (properties.containsKey("crosswalk.dissemination.edm.stylesheet")) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "checkOldEDMCrosswalk.crosswalk.xsl.exist", new String[]{dspaceConfNewFile.getAbsolutePath(),
                        (String) properties.get("crosswalk.dissemination.edm.stylesheet")});
                return true;
            }
        } finally {
            if (is != null) is.close();
        }
        return false;
    }

}
