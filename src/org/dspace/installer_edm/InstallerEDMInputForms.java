package org.dspace.installer_edm;


import org.dspace.handle.HandleManager;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 28/01/13
 * Time: 10:56
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMInputForms extends InstallerEDMBase
{

    private static final String xpathInputFormsTemplate = "//input-forms";
    private static final String xpathFormMapTemplate = "//form-map";
    private static final String xpathFormMapNameTemplate = "//form-map/name-map[@collection-handle='%s']";
    private static final String xpathFormMapNamesTemplate = "//form-map/name-map[@form-name='%s']";
    private static final String xpathFormDefinitionsTemplate = "//form-definitions";
    private static final String xpathFormDefinitionsFormsTemplate = "//form-definitions/form";
    private static final String xpathFormNameTemplate = "//form[@name='%s']";
    private static final String xpathFormNamePageTemplate = "//form[@name='%s']/page[@number='%s']";

    private String dspaceInputFormsNewFile;
    private InputSource inputFormsIS;
    private Document docInputForms;
    private Writer out = null;
    private HashMap<String, ArrayList<String>> formsColections;


    public InstallerEDMInputForms(int currentStepGlobal, String dspaceInputFormsNewFile) throws IOException, SAXException, ParserConfigurationException
    {
        super(currentStepGlobal);
        if (formsColections == null)
            formsColections = new HashMap<String, ArrayList<String>>();
        else formsColections.clear();
        this.dspaceInputFormsNewFile = dspaceInputFormsNewFile;
        inputFormsIS = new InputSource(dspaceInputFormsNewFile);
        docInputForms = getDocumentFromInputSource(inputFormsIS);
    }


    public boolean processInputForms() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, TransformerException, SQLException
    {
        boolean modify = false;
        try {
            readForms();
            modify = readInputFormsDspace();
            if (modify) {
                out = new OutputStreamWriter(new FileOutputStream(dspaceInputFormsNewFile, false));
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DocumentType docType = docInputForms.getDoctype();
                if (docType != null) {
                    if (docType.getPublicId() != null) transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
                }
                transformer.transform(new DOMSource(docInputForms), new StreamResult(out));
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return modify;
    }


    private void readForms() throws XPathExpressionException
    {
        XPath xpathInputForms = XPathFactory.newInstance().newXPath();
        NodeList resultsInputForms = (NodeList)xpathInputForms.evaluate(xpathFormDefinitionsFormsTemplate, docInputForms, XPathConstants.NODESET);
        for (int i=0; i < resultsInputForms.getLength(); i++) {
            Element elem = (Element) resultsInputForms.item(i);
            String name = elem.getAttribute("name").trim();
            XPath xpathInputFormsNames = XPathFactory.newInstance().newXPath();
            String xpathFormMapNameExpression = String.format(xpathFormMapNamesTemplate, new Object[] { name });
            NodeList resultsInputFormsNames = (NodeList)xpathInputFormsNames.evaluate(xpathFormMapNameExpression, docInputForms, XPathConstants.NODESET);
            for (int j=0; j < resultsInputFormsNames.getLength(); j++) {
                Element elementColl = (Element) resultsInputFormsNames.item(j);
                String handle = elementColl.getAttribute("collection-handle").trim();
                if (handle != null) {
                    if (!formsColections.containsKey(handle)) formsColections.put(handle, new ArrayList<String>());
                    formsColections.get(handle).add(name);
                }
            }
        }
    }


    private boolean readInputFormsDspace() throws IndexOutOfBoundsException, IOException, NullPointerException, XPathExpressionException, ParserConfigurationException, SAXException, SQLException
    {
        boolean modified = false;

        Element inputFormsElement;
        XPath xpathInputForms = XPathFactory.newInstance().newXPath();
        NodeList resultsInputForms = (NodeList)xpathInputForms.evaluate(xpathInputFormsTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsInputForms.getLength() == 0) {
            System.out.println("No " + xpathInputFormsTemplate);
            return false;
        } else inputFormsElement = (Element) resultsInputForms.item(0);

        Element formMapElement;
        XPath xpathFormMap = XPathFactory.newInstance().newXPath();
        NodeList resultsFormMap = (NodeList)xpathFormMap.evaluate(xpathFormMapTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsFormMap.getLength() == 0) {
            System.out.println("No " + xpathFormMapTemplate);
            formMapElement = docInputForms.createElement("form-map");
            inputFormsElement.appendChild(formMapElement);
        } else formMapElement = (Element) resultsFormMap.item(0);

        Element formDefinitionsElement;
        XPath xpathFormDefinitions = XPathFactory.newInstance().newXPath();
        NodeList resultsFormDefinitions = (NodeList)xpathFormDefinitions.evaluate(xpathFormDefinitionsTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsFormDefinitions.getLength() == 0) {
            System.out.println("No " + xpathFormDefinitionsTemplate);
            Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form definitions");
            formDefinitionsElement = docInputForms.createElement("form-definitions");
            inputFormsElement.appendChild(simpleComment);
            inputFormsElement.appendChild(formDefinitionsElement);
            modified = true;
        } else formDefinitionsElement = (Element) resultsFormDefinitions.item(0);

        for (Map.Entry<String, InstallerEDMAuthBO> entry : authBOHashMap.entrySet()) {

            XPath xpathFormMapName = XPathFactory.newInstance().newXPath();
            String handle = entry.getValue().getCollection().getHandle();
            String name = removeAccents(entry.getValue().getCollection().getName().toLowerCase());
            String xpathFormMapNameExpression = String.format(xpathFormMapNameTemplate, new Object[] { handle });
            NodeList resultsFormMapName = (NodeList)xpathFormMap.evaluate(xpathFormMapNameExpression, docInputForms, XPathConstants.NODESET);
            if (resultsFormMapName.getLength() == 0) {
                System.out.println("No " + xpathFormMapNameExpression);
                Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form map " + handle + ":" + name);
                Element elem = addFormMap(handle, name);
                formMapElement.appendChild(simpleComment);
                formMapElement.appendChild(elem);
                modified = true;
            }

            installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.element.page", new String[] {entry.getKey(), name});
            String pageResp = null;
            do {
                pageResp = br.readLine();
                if (pageResp == null) continue;
                pageResp = pageResp.trim();
                if (!pageResp.isEmpty() && pageResp.matches("^\\d+$")) {
                    break;
                }
            } while (true);
            Element formNameElement;
            Element formNamePageElement;
            String xpathFormNameExpression = String.format(xpathFormNameTemplate, new Object[] { name });
            NodeList resultsFormName = (NodeList)xpathFormMap.evaluate(xpathFormNameExpression, docInputForms, XPathConstants.NODESET);
            if (resultsFormName.getLength() == 0) {
                System.out.println("No " + xpathFormNameExpression);
                Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form " + name);
                formNameElement = addFormName(name);
                formDefinitionsElement.appendChild(simpleComment);
                formDefinitionsElement.appendChild(formNameElement);

                formNamePageElement = addFormNamePage(pageResp);
                formNameElement.appendChild(formNamePageElement);

                Element elemFormNamePageField = addFieldForm(entry.getValue(), "name");
                formNamePageElement.appendChild(elemFormNamePageField);
                modified = true;
            } else {
                formNameElement = (Element) resultsFormName.item(0);
                XPath xpathFormNamePage = XPathFactory.newInstance().newXPath();
                String xpathFormNamePageExpression = String.format(xpathFormNamePageTemplate, new Object[] { name, pageResp });
                NodeList resultsFormNamePage = (NodeList)xpathFormNamePage.evaluate(xpathFormNamePageExpression, docInputForms, XPathConstants.NODESET);
                if (resultsFormNamePage.getLength() > 0) {
                    formNamePageElement = (Element) resultsFormNamePage.item(0);
                    if (searchFormNamePageField(formNamePageElement, entry.getValue()) == null) {
                        Element elemFormNamePageField = addFieldForm(entry.getValue(), "name");
                        formNamePageElement.appendChild(elemFormNamePageField);
                        modified = true;
                    }
                } else {
                    formNamePageElement = addFormNamePage(pageResp);
                    formDefinitionsElement.appendChild(formNamePageElement);
                    formNameElement.appendChild(formNamePageElement);
                    Element elemFormNamePageField = addFieldForm(entry.getValue(), "name");
                    formNamePageElement.appendChild(elemFormNamePageField);
                    modified = true;
                }
            }

            installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.element.form", new String[] {entry.getKey(), name});
            String formResp = null;
            HashSet<String> forms = null;
            do {
                formResp = br.readLine();
                if (formResp == null) continue;
                formResp = formResp.trim();
                if (!formResp.isEmpty()) {
                    forms = new HashSet<String>();
                    String status = checkCollHandles(formResp, forms);
                    if (status == null) break;
                    else {
                        installerEDMDisplay.showMessage(status);
                        installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.element.form", new String[] {entry.getKey(), name});
                    }
                }
            } while (true);
            if (forms != null) addElements2Forms(entry, forms, name);
        }
        return modified;
    }


    private void addElements2Forms(Map.Entry<String, InstallerEDMAuthBO> entry, HashSet<String> forms, String name) throws XPathExpressionException, IOException
    {
        for (String form : forms) {
            XPath xpathFormName = XPathFactory.newInstance().newXPath();
            String xpathFormNameExpression = String.format(xpathFormNameTemplate, new Object[] { form });
            NodeList resultsFormName = (NodeList)xpathFormName.evaluate(xpathFormNameExpression, docInputForms, XPathConstants.NODESET);
            if (resultsFormName.getLength() > 0) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.element.page", new String[] {entry.getKey(), form});
                String pageResp = null;
                do {
                    pageResp = br.readLine();
                    if (pageResp == null) continue;
                    pageResp = pageResp.trim();
                    if (!pageResp.isEmpty() && pageResp.matches("^\\d+$")) {
                        break;
                    }
                } while (true);
                XPath xpathFormNamePage = XPathFactory.newInstance().newXPath();
                String xpathFormNamePageExpression = String.format(xpathFormNamePageTemplate, new Object[] { form, pageResp });
                NodeList resultsFormNamePage = (NodeList)xpathFormNamePage.evaluate(xpathFormNamePageExpression, docInputForms, XPathConstants.NODESET);
                if (resultsFormNamePage.getLength() > 0) {
                    Element formNamePageElement = (Element) resultsFormNamePage.item(0);
                    Element element = searchFormNamePageField(formNamePageElement, entry.getValue());
                    String vocabulary = name + "." + entry.getKey();
                    if (element == null) {
                        element = createElementVocabulary(entry.getValue(), vocabulary);
                        formNamePageElement.appendChild(element);
                    } else {
                        updateElementVocabulary(entry, form, element, vocabulary);
                    }

                }
            }
        }
    }


    private void updateElementVocabulary(Map.Entry<String, InstallerEDMAuthBO> entry, String form, Element field, String vocabulary) throws IOException
    {
        NodeList vocabularyList = field.getElementsByTagName("vocabulary");
        if (vocabularyList.getLength() == 0) {
            Element elementVocabulary = docInputForms.createElement("vocabulary");
            Text text = docInputForms.createTextNode(vocabulary);
            elementVocabulary.appendChild(text);
            field.appendChild(elementVocabulary);
        } else {
            Element elementVocabulary = (Element) vocabularyList.item(0);
            String currentVocabulary = elementVocabulary.getFirstChild().getNodeValue();
            if (!currentVocabulary.equals(vocabulary)) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "updateElementvocabulary", new String[] {currentVocabulary, entry.getKey(), form, vocabulary});
                String response = null;
                do {
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    if (!response.isEmpty()) {
                        break;
                    }
                } while (true);
                if (response.equals("y")) {
                    Text text = docInputForms.createTextNode(vocabulary);
                    elementVocabulary.replaceChild(text, elementVocabulary.getFirstChild());
                }
            }
        }
        NodeList inputTypeList = field.getElementsByTagName("input-type");
        if (inputTypeList.getLength() == 0) {
            Element elementInputType = docInputForms.createElement("input-type");
            Text text = docInputForms.createTextNode("onebox");
            elementInputType.appendChild(text);
            field.appendChild(elementInputType);
        } else {
            Text text = docInputForms.createTextNode("onebox");
            inputTypeList.item(0).replaceChild(text, inputTypeList.item(0).getFirstChild());
        }
    }

    private Element createElementVocabulary(InstallerEDMAuthBO entry, String vocabulary) throws IOException
    {
        Element field = addFieldForm(entry, "onebox");
        Element elementVocabulary = docInputForms.createElement("vocabulary");
        Text text = docInputForms.createTextNode(vocabulary);
        elementVocabulary.appendChild(text);
        field.appendChild(elementVocabulary);
        return field;
    }


    private String checkCollHandles(String formResp, HashSet<String> forms) throws SQLException
    {
        String status = null;
        StringBuilder statusB = new StringBuilder();
        String[] handlesA = formResp.split("\\s*,\\s*");
        for (String handle : handlesA) {
            handle = handle.trim();
            if (!handle.equals("default") && HandleManager.resolveToObject(context, handle) == null) {
                statusB.append("Collection with handle \"").append(handle).append("\" does not exist in the database\n");
            }
            if (formsColections.containsKey(handle)) {
                if (handle.equals("default")) {
                    forms.add(formsColections.get(handle).get(0));
                } else {
                    for (String form : formsColections.get(handle)) {
                        forms.add(form);
                    }
                }
            }
        }
        if (statusB.length() > 0) status = statusB.toString();

        return status;
    }


    private Element searchFormNamePageField(Element formNamePageElement, InstallerEDMAuthBO value)
    {
        NodeList resultsFormNamePageFields = (NodeList) formNamePageElement.getChildNodes();
        for (int i=0; i < resultsFormNamePageFields.getLength(); i++) {
            if (resultsFormNamePageFields.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            int mask = 0;
            Element elementField = (Element) resultsFormNamePageFields.item(i);
            if (elementField.getNodeName().equals("field")) {
                NodeList dces = elementField.getElementsByTagName("dc-element");
                for (int j=0; j < dces.getLength(); j++) {
                    if (dces.item(j).getFirstChild().getNodeValue().toLowerCase().equals(value.getMetadataField().getElement().toLowerCase())) {
                        mask++;
                        break;
                    }
                }
                if (mask == 0) return null;
                if (value.getMetadataField().getQualifier() == null) mask++;
                else {
                    NodeList dcqs = elementField.getElementsByTagName("dc-qualifier");
                    for (int j=0; j < dcqs.getLength(); j++) {
                        if (dcqs.item(j).getFirstChild().getNodeValue().toLowerCase().equals(value.getMetadataField().getQualifier().toLowerCase())) {
                            mask++;
                            break;
                        }
                    }
                }
                if (mask == 2) return elementField;
            }
        }
        return null;
    }


    private Element addFormMap(String handle, String name)
    {
        Element elem = docInputForms.createElement("name-map");
        Attr handleAttr = docInputForms.createAttribute("collection-handle");
        handleAttr.setValue(handle);
        elem.setAttributeNode(handleAttr);
        Attr nameAttr = docInputForms.createAttribute("form-name");
        nameAttr.setValue(name);
        elem.setAttributeNode(nameAttr);
        return elem;
    }

    private Element addFormName(String name)
    {
        Element elem = docInputForms.createElement("form");
        Attr nameAttr = docInputForms.createAttribute("name");
        nameAttr.setValue(name);
        elem.setAttributeNode(nameAttr);
        return elem;
    }

    private Element addFormNamePage(String page)
    {
        Element elem = docInputForms.createElement("page");
        Attr numberAttr = docInputForms.createAttribute("number");
        numberAttr.setValue(page);
        elem.setAttributeNode(numberAttr);
        return elem;
    }

    private Element addFieldForm(InstallerEDMAuthBO value, String inputType) throws IOException
    {
        Element elem = docInputForms.createElement("field");
        Element elemDCSchema = docInputForms.createElement("dc-schema");
        Text text = docInputForms.createTextNode("dc");
        elemDCSchema.appendChild(text);
        elem.appendChild(elemDCSchema);

        Element elemDCElement = docInputForms.createElement("dc-element");
        text = docInputForms.createTextNode(value.getMetadataField().getElement().toLowerCase());
        elemDCElement.appendChild(text);
        elem.appendChild(elemDCElement);

        if (value.getMetadataField().getQualifier() != null) {
            Element elemDCQualifier = docInputForms.createElement("dc-qualifier");
            text = docInputForms.createTextNode(value.getMetadataField().getQualifier().toLowerCase());
            elemDCQualifier.appendChild(text);
            elem.appendChild(elemDCQualifier);
        }

        Element elemRepeatable = docInputForms.createElement("repeatable");
        text = docInputForms.createTextNode("true");
        elemRepeatable.appendChild(text);
        elem.appendChild(elemRepeatable);

        Element elemInputType = docInputForms.createElement("input-type");
        text = docInputForms.createTextNode(inputType);
        elemInputType.appendChild(text);
        elem.appendChild(elemInputType);

        Element elemRequired = docInputForms.createElement("required");
        elem.appendChild(elemRequired);

        installerEDMDisplay.showQuestion(currentStepGlobal, "addFieldForm.label", new String[]{value.getMetadataField().getElement() + "." + value.getMetadataField().getQualifier()});
        String label = null;
        do {
            label = br.readLine();
            if (label == null) continue;
            label = label.trim();
            if (label.length() > 0) {
                break;
            }
        } while (true);
        Element elemLabel = docInputForms.createElement("label");
        text = docInputForms.createTextNode(label);
        elemLabel.appendChild(text);
        elem.appendChild(elemLabel);

        installerEDMDisplay.showQuestion(currentStepGlobal, "addFieldForm.hint", new String[]{value.getMetadataField().getElement() + "." + value.getMetadataField().getQualifier()});
        String hint = null;
        do {
            hint = br.readLine();
            if (hint == null) continue;
            hint = hint.trim();
            if (hint.length() > 0) {
                break;
            }
        } while (true);
        Element elemHint = docInputForms.createElement("hint");
        text = docInputForms.createTextNode(hint);
        elemHint.appendChild(text);
        elem.appendChild(elemHint);
        return elem;
    }


    private Document getDocumentFromInputSource(InputSource IS) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(IS);
    }
}
