package org.dspace.installer_edm;


import org.dspace.handle.HandleManager;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
 * @class InstallerEDMInputForms
 *
 * Clase para copiar el archivo input-forms.xml de dspace al directorio de trabajo del instalador para que cuando se catalogue
 * se puedan buscar valores de los repositorios de Askosi para los elementos dc asociados a autoridades.
 *
 * Se modificar para añadir los nuevos formularios de los nuevos vocabularios, uno por cada elemento dc de autoridad.
 * Se pide en qué formularios existentes se añade el vocabulario y en qué página del proceso de catalogación.
 *
 */
public class InstallerEDMInputForms extends InstallerEDMBase
{

    /**
     * Consulta xpath para buscar el elemento raíz del archivo input-forms.xml
     */
    private static final String xpathInputFormsTemplate = "//input-forms";

    /**
     * Consulta xpath para buscar el mapeo de formularios con handles de colecciones
     */
    private static final String xpathFormMapTemplate = "//form-map";

    /**
     * Consulta xpath para buscar el mapeo de un formulario a una colección determinada
     */
    private static final String xpathFormMapNameTemplate = "//form-map/name-map[@collection-handle='%s']";

    /**
     * Consulta xpath para buscar el mapeo de una colección con un formulario determinado
     */
    private static final String xpathFormMapNamesTemplate = "//form-map/name-map[@form-name='%s']";

    /**
     * Consulta xpath para buscar el elemento raíz de las configuraciones de los formularios
     */
    private static final String xpathFormDefinitionsTemplate = "//form-definitions";

    /**
     * Consulta xpath para buscar una lista de las configuraciones de los formularios
     */
    private static final String xpathFormDefinitionsFormsTemplate = "//form-definitions/form";

    /**
     * Consulta xpath para buscar la configuración de un formulario determinado
     */
    private static final String xpathFormNameTemplate = "//form[@name='%s']";

    /**
     * Consulta xpath para buscar la configuración de un formulario determinado en una página determinada
     */
    private static final String xpathFormNamePageTemplate = "//form[@name='%s']/page[@number='%s']";

    /**
     * ruta del archivo input-forms.xml en el directorio de trabajo del instalador
     */
    private String dspaceInputFormsNewFile;

    /**
     * Flujo de entrada de datps del archivo input-forms.xml
     */
    private InputSource inputFormsIS;

    /**
     * documento jdom para el archivo xml
     */
    private Document docInputForms;

    /**
     * flujo de escritura de datos para el xml
     */
    private Writer out = null;

    /**
     * hash con asociación handle de la colección y lista de formularios (cada uno perteneciente a un elemento dc de autoridad)
     */
    private HashMap<String, ArrayList<String>> formsColections;


    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param dspaceInputFormsNewFile ruta del archivo input-forms.xml en el directorio de trabajo del instalador
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
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


    /**
     * lee el archivo input-forms.xml para recoger asociación de colecciones y formularios,
     * peticiona si se quieren añadir o modificar vocabularios y modifica el archivo
     * para escribirlo en disco
     *
     * @return si se ha modificado el archivo input-forms.xml
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws IOException
     * @throws TransformerException
     * @throws SQLException
     */
    public boolean processInputForms() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, TransformerException, SQLException
    {
        boolean modify = false;
        try {
            // lee xml para recoger asociación de colecciones y formularios
            readForms();
            // pide nuevos vocabularios a añadir o modificar
            modify = readInputFormsDspace();
            // escribe el documento jdom a disco
            if (modify) {
                out = new OutputStreamWriter(new FileOutputStream(dspaceInputFormsNewFile, false), "UTF-8");
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
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.processInputForms.end", null);
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return modify;
    }


    /**
     * Busca todos los formularios (desde las definiciones y buscando luego el handle de la colección en el mapeo) en el
     * documento xml para añadirlos al hash de asociaciones de colecciones con formularios
     *
     * @throws XPathExpressionException
     */
    private void readForms() throws XPathExpressionException
    {
        // lanza consulta xpath para buscar todos los formularios
        XPath xpathInputForms = XPathFactory.newInstance().newXPath();
        NodeList resultsInputForms = (NodeList)xpathInputForms.evaluate(xpathFormDefinitionsFormsTemplate, docInputForms, XPathConstants.NODESET);
        for (int i=0; i < resultsInputForms.getLength(); i++) {
            Element elem = (Element) resultsInputForms.item(i);
            // lee el nombre del formulario
            String name = elem.getAttribute("name").trim();
            // lanza consulta de buscar el formulario con un nombre en el mapeo de handle de colección con formulario
            XPath xpathInputFormsNames = XPathFactory.newInstance().newXPath();
            String xpathFormMapNameExpression = String.format(xpathFormMapNamesTemplate, new Object[] { name });
            NodeList resultsInputFormsNames = (NodeList)xpathInputFormsNames.evaluate(xpathFormMapNameExpression, docInputForms, XPathConstants.NODESET);
            // se añade en el hash de colecciones-formularios el binomio
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


    /**
     * Lanza consultas al documento jdom para comprobar que la estructura es correcta.
     *
     * Crea un formulario por cada elemento dc de autoridad nuevo:
     *  Recorre los elementos dc que son autoridades para añadir los formularios-handle de colección.
     *  Si no encuentra un handle de colección en el mapeo, lo añade con el nombre de la colección.
     *  Pregunta si se configura el elemento dc de autoridad en el formulario.
     *  Pregunta a qué pagina del formulario irá el vocabulario.
     *  Crea formulario en las definiciones si no existe si no busca la página en el formulario o la crea.
     *  Crea el elemento dc en el formulario.
     *
     * Pregunta a qué colecciones se asociarán cada uno de los formularios (vocabularios) creados.
     * Recoge los formularios asociados con los handle de las colecciones suministradas y añade los vocabularios
     * preguntando a qué página dentro del formulario se insertará
     *
     * @return si se ha modificado el documento jdom del archivo input-forms.xml
     * @throws IndexOutOfBoundsException
     * @throws IOException
     * @throws NullPointerException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws SQLException
     */
    private boolean readInputFormsDspace() throws IndexOutOfBoundsException, IOException, NullPointerException, XPathExpressionException, ParserConfigurationException, SAXException, SQLException
    {
        boolean modified = false;

        // busca elemento raíz del documento
        Element inputFormsElement;
        XPath xpathInputForms = XPathFactory.newInstance().newXPath();
        NodeList resultsInputForms = (NodeList)xpathInputForms.evaluate(xpathInputFormsTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsInputForms.getLength() == 0) {
            if (debug) System.out.println("No " + xpathInputFormsTemplate);
            return false;
        } else inputFormsElement = (Element) resultsInputForms.item(0);

        // buscar elemento de mapeos de colecciones con formularios
        Element formMapElement;
        XPath xpathFormMap = XPathFactory.newInstance().newXPath();
        NodeList resultsFormMap = (NodeList)xpathFormMap.evaluate(xpathFormMapTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsFormMap.getLength() == 0) {
            if (debug) System.out.println("No " + xpathFormMapTemplate);
            formMapElement = docInputForms.createElement("form-map");
            inputFormsElement.appendChild(formMapElement);
        } else formMapElement = (Element) resultsFormMap.item(0);

        // busca elemento raíz de las definiciones de los formularios
        Element formDefinitionsElement;
        XPath xpathFormDefinitions = XPathFactory.newInstance().newXPath();
        NodeList resultsFormDefinitions = (NodeList)xpathFormDefinitions.evaluate(xpathFormDefinitionsTemplate, docInputForms, XPathConstants.NODESET);
        if (resultsFormDefinitions.getLength() == 0) {
            if (debug) System.out.println("No " + xpathFormDefinitionsTemplate);
            Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form definitions");
            formDefinitionsElement = docInputForms.createElement("form-definitions");
            inputFormsElement.appendChild(simpleComment);
            inputFormsElement.appendChild(formDefinitionsElement);
            modified = true;
        } else formDefinitionsElement = (Element) resultsFormDefinitions.item(0);

        // recorre los elementos dc que son autoridades
        for (Map.Entry<String, InstallerEDMAuthBO> entry : authBOHashMap.entrySet()) {

            // busca formulario en el mapeo con el handle, si no lo encuentra lo añade
            XPath xpathFormMapName = XPathFactory.newInstance().newXPath();
            String handle = entry.getValue().getCollection().getHandle();
            String name = removeAccents(entry.getValue().getCollection().getName().toLowerCase());
            String xpathFormMapNameExpression = String.format(xpathFormMapNameTemplate, new Object[] { handle });
            NodeList resultsFormMapName = (NodeList)xpathFormMapName.evaluate(xpathFormMapNameExpression, docInputForms, XPathConstants.NODESET);
            if (resultsFormMapName.getLength() == 0) {
                if (debug) System.out.println("No " + xpathFormMapNameExpression);
                Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form map " + handle + ":" + name);
                Element elem = addFormMap(handle, name);
                formMapElement.appendChild(simpleComment);
                formMapElement.appendChild(elem);
                modified = true;
            }

            // preguntar si se configura el elemento dc de autoridad en el formulario
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.configure.element.form", new String[] {entry.getKey(), name});
            String response = null;
            boolean configureForm = false;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (!response.isEmpty() && response.equalsIgnoreCase(answerYes)) {
                    configureForm = true;
                }
                break;
            } while (true);
            if (!configureForm) continue;

            // preguntar a qué pagina del formulario irá el vocabulario
            installerEDMDisplay.showLn();
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

            // buscar formulario en las definiciones
            Element formNameElement;
            Element formNamePageElement;
            String xpathFormNameExpression = String.format(xpathFormNameTemplate, new Object[] { name });
            NodeList resultsFormName = (NodeList)xpathFormMap.evaluate(xpathFormNameExpression, docInputForms, XPathConstants.NODESET);
            // crearlo si no existe junto con la página
            if (resultsFormName.getLength() == 0) {
                if (debug) System.out.println("No " + xpathFormNameExpression);
                Comment simpleComment = docInputForms.createComment(getTime() + " Appended by installerEDM to add the form " + name);
                formNameElement = addFormName(name);
                formDefinitionsElement.appendChild(simpleComment);
                formDefinitionsElement.appendChild(formNameElement);

                formNamePageElement = addFormNamePage(pageResp);
                formNameElement.appendChild(formNamePageElement);

                Element elemFormNamePageField = addFieldForm(entry.getValue(), "onebox");
                formNamePageElement.appendChild(elemFormNamePageField);
                modified = true;
            } else {
                // buscar página en el formulario o la crea, añade el elemento dc en el formulario
                formNameElement = (Element) resultsFormName.item(0);
                XPath xpathFormNamePage = XPathFactory.newInstance().newXPath();
                String xpathFormNamePageExpression = String.format(xpathFormNamePageTemplate, new Object[] { name, pageResp });
                NodeList resultsFormNamePage = (NodeList)xpathFormNamePage.evaluate(xpathFormNamePageExpression, docInputForms, XPathConstants.NODESET);
                if (resultsFormNamePage.getLength() > 0) {
                    formNamePageElement = (Element) resultsFormNamePage.item(0);
                    if (searchFormNamePageField(formNamePageElement, entry.getValue()) == null) {
                        Element elemFormNamePageField = addFieldForm(entry.getValue(), "onebox");
                        formNamePageElement.appendChild(elemFormNamePageField);
                        modified = true;
                    }
                } else {
                    formNamePageElement = addFormNamePage(pageResp);
                    formDefinitionsElement.appendChild(formNamePageElement);
                    formNameElement.appendChild(formNamePageElement);
                    Element elemFormNamePageField = addFieldForm(entry.getValue(), "onebox");
                    formNamePageElement.appendChild(elemFormNamePageField);
                    modified = true;
                }
            }

            // pregunta a qué colecciones el nuevo formulario debe ser asociado
            installerEDMDisplay.showQuestion(currentStepGlobal, "readInputFormsDspace.element.form", new String[] {entry.getKey(), name});
            String formResp = null;
            HashSet<String> forms = null;
            do {
                formResp = br.readLine();
                if (formResp == null) continue;
                formResp = formResp.trim();
                if (!formResp.isEmpty()) {
                    forms = new HashSet<String>();
                    // recoge los formularios asociados con los handle de las colecciones suministradas
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


    /**
     * Añade a cada uno de los formularios suministrados el vocabulario del elemento dc correspondiente.
     * Pregunta a qué página dentro del formulario se insertará el vocabulario, que tendrá como nombre
     * el nombre de la colección + "." + elemento dc
     *
     * @param entry entrada del elemento dc con el objeto de autoridad {@link InstallerEDMAuthBO}
     * @param forms conjunto de formularios a los que añadir el vocabulario
     * @param name nombre del formulario del vocabulario
     * @throws XPathExpressionException
     * @throws IOException
     */
    private void addElements2Forms(Map.Entry<String, InstallerEDMAuthBO> entry, HashSet<String> forms, String name) throws XPathExpressionException, IOException
    {
        // recorre los formularios
        for (String form : forms) {
            // busca formulario en las definiciones
            XPath xpathFormName = XPathFactory.newInstance().newXPath();
            String xpathFormNameExpression = String.format(xpathFormNameTemplate, new Object[] { form });
            NodeList resultsFormName = (NodeList)xpathFormName.evaluate(xpathFormNameExpression, docInputForms, XPathConstants.NODESET);
            // existe
            if (resultsFormName.getLength() > 0) {
                // pregunta a qué paǵina se añadirá el vocabulario
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
                // busca la página
                XPath xpathFormNamePage = XPathFactory.newInstance().newXPath();
                String xpathFormNamePageExpression = String.format(xpathFormNamePageTemplate, new Object[] { form, pageResp });
                NodeList resultsFormNamePage = (NodeList)xpathFormNamePage.evaluate(xpathFormNamePageExpression, docInputForms, XPathConstants.NODESET);
                if (resultsFormNamePage.getLength() > 0) {
                    // añade vocabulario o lo actualiza
                    Element formNamePageElement = (Element) resultsFormNamePage.item(0);
                    // busca el elemento dc en la página
                    Element element = searchFormNamePageField(formNamePageElement, entry.getValue());
                    String vocabulary = name.replaceAll("\\.", "") + "" + entry.getKey().replaceAll("\\.", "");
                    // lo crea
                    if (element == null) {
                        element = createElementVocabulary(entry.getValue(), vocabulary);
                        formNamePageElement.appendChild(element);
                    // lo mofifica
                    } else {
                        updateElementVocabulary(entry, form, element, vocabulary);
                    }

                }
            }
        }
    }


    /**
     * Actualiza el vocabulario en un formulario para un elemento dc con los datos actuales
     * El elemento input-type tiene el valor onebox
     *
     * @param entry entrada del elemento dc con el objeto de autoridad {@link InstallerEDMAuthBO}
     * @param form nombre del formulario a modificar
     * @param field elemento jdom con el elemento dc dentro del formulario y la página
     * @param vocabulary nombre del vocabulario
     * @throws IOException
     */
    private void updateElementVocabulary(Map.Entry<String, InstallerEDMAuthBO> entry, String form, Element field, String vocabulary) throws IOException
    {
        // busca vocabularios
        NodeList vocabularyList = field.getElementsByTagName("vocabulary");
        // crea uno si no existe
        if (vocabularyList.getLength() == 0) {
            Element elementVocabulary = docInputForms.createElement("vocabulary");
            Text text = docInputForms.createTextNode(vocabulary);
            elementVocabulary.appendChild(text);
            field.appendChild(elementVocabulary);
        } else {
            // se recorre los que hay y pregunta si se quiere actualizar con los datos actuales
            Element elementVocabulary = (Element) vocabularyList.item(0);
            String currentVocabulary = elementVocabulary.getFirstChild().getNodeValue();
            if (!currentVocabulary.equals(vocabulary)) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "updateElementvocabulary", new String[] {currentVocabulary, entry.getKey(), form, vocabulary});
                String response = null;
                do {
                    response = br.readLine();
                    if (response == null) continue;
                    response = response.trim();
                    break;
                } while (true);
                if (response.equals(answerYes)) {
                    Text text = docInputForms.createTextNode(vocabulary);
                    elementVocabulary.replaceChild(text, elementVocabulary.getFirstChild());
                }
            }
        }
        // cambiar o crear el elemento input-type con valor onebox
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

    /**
     * Crea un element jdom para el vocabulario listo para añadir a un formulario
     *
     * @param entry entrada del elemento dc con el objeto de autoridad {@link InstallerEDMAuthBO}
     * @param vocabulary nombre del vocabulario
     * @return elemento jdom creado para el vocabulario
     * @throws IOException
     */
    private Element createElementVocabulary(InstallerEDMAuthBO entry, String vocabulary) throws IOException
    {
        Element field = addFieldForm(entry, "onebox");
        Element elementVocabulary = docInputForms.createElement("vocabulary");
        Text text = docInputForms.createTextNode(vocabulary);
        elementVocabulary.appendChild(text);
        Attr closedAttr = docInputForms.createAttribute("closed");
        closedAttr.setValue("false");
        elementVocabulary.setAttributeNode(closedAttr);
        field.appendChild(elementVocabulary);
        return field;
    }


    /**
     * Comprueba que los handle de las colecciones estén en dspace
     *
     * @param formResp cadena con los handle de las colecciones
     * @param forms hash con los handles de las colecciones de la lista
     * @return cadena con los handle incorrectos
     * @throws SQLException
     */
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


    /**
     * Busca en la página de un formulario el elemento dc requerido
     *
     * @param formNamePageElement elemento jdom con la página en el formulario
     * @param value elemento dc de la autoridad {@link InstallerEDMAuthBO}
     * @return elemento jdom del element dc en el formulario
     */
    private Element searchFormNamePageField(Element formNamePageElement, InstallerEDMAuthBO value)
    {
        String nameElement = value.getMetadataField().getElement().toLowerCase();
        String qualifierElement = value.getMetadataField().getQualifier();

        // recorrer todos los nodos de la página
        NodeList resultsFormNamePageFields = (NodeList) formNamePageElement.getChildNodes();
        for (int i=0; i < resultsFormNamePageFields.getLength(); i++) {
            if (resultsFormNamePageFields.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            int mask = 0;
            Element elementField = (Element) resultsFormNamePageFields.item(i);
            // nodo field
            if (elementField.getNodeName().equals("field")) {
                // nodo dc-element con nuestro elemento dc
                NodeList dces = elementField.getElementsByTagName("dc-element");
                for (int j=0; j < dces.getLength(); j++) {
                    if (dces.item(j).getFirstChild().getNodeValue().toLowerCase().equals(nameElement)) {
                        mask++;
                        break;
                    }
                }
                if (mask == 0) return null;
                if (qualifierElement == null) mask++;
                else {
                    // con nuestro qualifier
                    NodeList dcqs = elementField.getElementsByTagName("dc-qualifier");
                    for (int j=0; j < dcqs.getLength(); j++) {
                        if (dcqs.item(j).getFirstChild().getNodeValue().toLowerCase().equals(qualifierElement.toLowerCase())) {
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


    /**
     * Crea un elemento jdom con el mapeo del handle de la colección y el nombre de formulario
     *
     * @param handle cadena con el handle
     * @param name cadena con el nombre del formulario
     * @return elemento jdom con el mapeo de handle-formulario
     */
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

    /**
     * Crea un elemento jdom para una definición de formulario
     *
     * @param name cadena con el nombre del formulario
     * @return elemento jdom con el formulario
     */
    private Element addFormName(String name)
    {
        Element elem = docInputForms.createElement("form");
        Attr nameAttr = docInputForms.createAttribute("name");
        nameAttr.setValue(name);
        elem.setAttributeNode(nameAttr);
        return elem;
    }

    /**
     * Crea un elemento jdom para una página de formulario
     *
     * @param page cadena con la página para el formulario
     * @return elemento jdom con la página para el formulario
     */
    private Element addFormNamePage(String page)
    {
        Element elem = docInputForms.createElement("page");
        Attr numberAttr = docInputForms.createAttribute("number");
        numberAttr.setValue(page);
        elem.setAttributeNode(numberAttr);
        return elem;
    }

    /**
     * Crea un elemento jdom para un elemento dc para una página de formulario, es repetible y requerido
     *
     * @param value elemento dc de la autoridad {@link InstallerEDMAuthBO}
     * @param inputType tipo de input-type (lo normal es onebox)
     * @return elemento jdom con el elemento dc para el formulario
     * @throws IOException
     */
    private Element addFieldForm(InstallerEDMAuthBO value, String inputType) throws IOException
    {
        // crear nodo field
        Element elem = docInputForms.createElement("field");

        // crear nodo dc-schema
        Element elemDCSchema = docInputForms.createElement("dc-schema");
        Text text = docInputForms.createTextNode("dc");
        elemDCSchema.appendChild(text);
        elem.appendChild(elemDCSchema);

        // crear nodo dc-element
        Element elemDCElement = docInputForms.createElement("dc-element");
        text = docInputForms.createTextNode(value.getMetadataField().getElement().toLowerCase());
        elemDCElement.appendChild(text);
        elem.appendChild(elemDCElement);

        // crear nodo dc-qualifier
        if (value.getMetadataField().getQualifier() != null) {
            Element elemDCQualifier = docInputForms.createElement("dc-qualifier");
            text = docInputForms.createTextNode(value.getMetadataField().getQualifier().toLowerCase());
            elemDCQualifier.appendChild(text);
            elem.appendChild(elemDCQualifier);
        }

        // crear nodo repeatable
        Element elemRepeatable = docInputForms.createElement("repeatable");
        text = docInputForms.createTextNode("true");
        elemRepeatable.appendChild(text);
        elem.appendChild(elemRepeatable);

        // crear nodo input-type
        Element elemInputType = docInputForms.createElement("input-type");
        text = docInputForms.createTextNode(inputType);
        elemInputType.appendChild(text);
        elem.appendChild(elemInputType);

        // crear nodo required
        Element elemRequired = docInputForms.createElement("required");
        elem.appendChild(elemRequired);

        // crear etiqueta
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

        // crear descripción
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

}
