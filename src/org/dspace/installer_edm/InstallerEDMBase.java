package org.dspace.installer_edm;

import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @class InstallerEDMBase
 *
 * Clase base con variables estáticas y métodos que se usan en todas las clases que ejecutan pasos
 *
 */
public abstract class InstallerEDMBase implements Observer
{
    /**
     * clase que lanza el instalador
     */
    protected static InstallerEDM installerEDM;

    /**
     * ruta del directorio de dspace
     */
    protected static String DspaceDir = null;

    /**
     * ruta del directorio base de tomcat
     */
    protected static String TomcatBase = null;

    /**
     * directorio base de tomcat
     */
    protected static File TomcatBaseFile = null;

    /**
     * ruta del directorio datos de askosi
     */
    protected static String AskosiDataDir;

    /**
     * verbosidad
     */
    protected static boolean verbose = false;

    /**
     * debug para mostrar más mensajes
     */
    protected static boolean debug = false;

    /**
     * contexto de dspace
     */
    protected static Context context = null;

    /**
     * idioma paa los mensajes y los elementos dc
     */
    protected static String language = null;

    /**
     * conjunto de pasos del instalador
     */
    protected static Set<Integer> stepsSet = new HashSet<Integer>();

    /**
     * clase para mostrar los mensajes
     */
    protected static InstallerEDMDisplay installerEDMDisplay = null;

    /**
     * ruta del directorio del instalador
     */
    protected static String myInstallerDirPath = null;

    /**
     * ruta del directorio de trabajo del instalador
     */
    protected static String myInstallerWorkDirPath = null;

    /**
     * separador del sistema de ficheros del sistema
     */
    protected static String fileSeparator = System.getProperty("file.separator");

    /**
     * flujo de datos de entrada para las preguntas
     */
    protected static InputStreamReader isr = null;

    /**
     * buffer para el flujo de entrada
     */
    protected static BufferedReader br = null;

    /**
     * uri del esquema DC
     */
    protected final String DCSCHEMA = "http://dublincore.org/documents/dcmi-terms/";

    /**
     * objeto esquema de dspace {@link MetadataSchema}
     */
    protected MetadataSchema dcSchema = null;

    /**
     * array con los metadatos de elementos dc
     */
    protected static ArrayList<MetadataField> metadataFields;

    /**
     * conjunto de elementos dc que no son autoridades
     */
    protected static Set<String> elementsNotAuthSet = new HashSet<String>();

    /**
     * paso actual
     */
    protected int currentStepGlobal;

    /**
     * objeto usuario de dspace {@link EPerson}
     */
    protected static EPerson eperson;

    /**
     * nombre del usuario
     */
    protected static String user;

    /**
     * clave del usuario
     */
    protected static String password;

    /**
     * palabra de respuesta afirmativa
     */
    protected static String answerYes;

    /**
     * nombre de la base de datos
     */
    protected static String dbName = null;

    /**
     * administrador de autoridades de dspace {@link MetadataAuthorityManager}
     */
    protected static MetadataAuthorityManager metadataAuthorityManager = null;

    /**
     * conjunto de elementos con autoridad de dspace.cfg
     */
    protected static Set<String> elementsAuthDspaceCfg = null;


    /**
     * tabla hash de elementos dc como clave y el POJO de la autoridad {@link InstallerEDMAuthBO} de valor
     */
    protected static HashMap<String, InstallerEDMAuthBO> authBOHashMap;

    /**
     * elementos dc que no pueden ser autoridad
     */
    protected static final String[] elementsNotAuth = {"identifier.uri", "date.accessioned", "date.available", "date.issued", "description.provenance", "type"};

    /**
     * archivos en el directorio packages para validar por firma md5
     */
    protected static final String[] packages = {"ASKOSI.jar", "askosiWebapp.zip", "classes.zip", "commons-dbcp.jar", "commons-pool.jar", "EDMCrosswalk.java", "EDMExport.war", "exampleAskosiData.zip", "jaxb-xalan-1.5.jar", "jsr311-api-1.1.1.jar", "jstl-1.2.jar", "log4j.jar", "openrdf-alibaba-2.0-beta6.jar", "openrdf-sesame-2.3.2-onejar.jar", "DIM2EDM.xsl", "jdom-1.0.jar"};

    /**
     * firma md5 de los archivos en packages
     */
    protected static final String[] packagesMD5 = {"f800262e9587383fa0dbd8f748cc831e", "ab932907d73a8031cb266d20d341a6e2", "0bffffb990ea99eb02a989d346454d8e", "2666cfeb7be74b1c2d8a1665ae21192c", "01f9bed60e2f88372132d34040ee81bb", "82c2aad9d201f5a6cba3515e14b68b4c", "fc08b60196c56a37456dda45f684b41a", "ac0e35b7978eb4b7d155d7d5f0122a8b", "261968cebe30ffe8adcc201ad0bfa395", "c9803468299ec255c047a280ddec510f", "51e15f798e69358cb893e38c50596b9b", "599b8ba07d1d04f0ea34414e861d7ad1", "1f699edb215bcee75cb6f0616fa56993", "3054aa9109f78903852d38991b5a4ea8", "429d5c7aeaadf81ec4604798d22ce78c", "0b8f97de82fc9529b1028a77125ce4f8"};


    /**
     * Constructor con parámetro el paso actual, llama al constructor sin parámetros
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerEDMBase(int currentStepGlobal)
    {
        this();
        this.currentStepGlobal = currentStepGlobal;
    }

    /**
     * Constructor, inicia el contexto de dspace e inicializa las variables estáticas
     */
    public InstallerEDMBase()
    {
        try {
            if (context == null) {
                context = new Context();
                if (dbName == null) dbName = ConfigurationManager.getProperty("db.name");
                InstallerEDMDAO.setContext(context);
                InstallerEDMDAO.setDbName(dbName);
                Collections.addAll(elementsNotAuthSet, elementsNotAuth);
            }
            if (context == null || !(context instanceof Context)) throw new Exception("Impossible to create dspace context.");
            if (metadataAuthorityManager == null) metadataAuthorityManager = MetadataAuthorityManager.getManager();
            if (installerEDMDisplay == null) installerEDMDisplay = new InstallerEDMDisplayImpl();
            if (isr == null) isr = new InputStreamReader(System.in);
            if (br == null) br = new BufferedReader(isr);
            if (myInstallerDirPath == null) myInstallerDirPath = new File(".").getAbsolutePath();
            if (myInstallerWorkDirPath == null) myInstallerWorkDirPath = myInstallerDirPath + fileSeparator + "work";
            File myInstallerWorkDirFile = new File(myInstallerWorkDirPath);
            if (!myInstallerWorkDirFile.exists()) myInstallerWorkDirFile.mkdir();
            if (elementsAuthDspaceCfg == null) loadAuthDspaceCfg();
            checkDspaceDC();
            if (language == null) language = ConfigurationManager.getProperty("default.language");
            if (language == null) language = "en";
            answerYes = installerEDMDisplay.getQuestion(0, "answer.yes");
        } catch (SQLException e) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(0, "step.fail");
            showException(e);
        } catch (Exception e) {
            showException(e);
        }
    }


    /**
     * Asigna el idioma
     *
     * @param language idioma
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Asigna el objeto que inicia el instalador
     *
     * @param installerEDM {@link InstallerEDM}
     */
    public void setInstallerEDMBase(InstallerEDM installerEDM)
    {
        this.installerEDM = installerEDM;
    }

    /**
     * Asigna la ruta de dspace
     *
     * @param DspaceDir ruta de dspace
     */
    public void setDspaceDir(String DspaceDir)
    {
        this.DspaceDir = DspaceDir;
    }

    /**
     * Asigna la ruta de tomcat
     *
     * @param TomcatBase ruta de tomcat
     */
    public void setTomcatBase(String TomcatBase)
    {
        this.TomcatBase = TomcatBase;
    }

    /**
     * Asigna la ruta de datos de askosi
     *
     * @param askosiDataDir ruta de datos de askosi
     */
    public void setAskosiDataDir(String askosiDataDir)
    {
        this.AskosiDataDir = askosiDataDir;
    }

    /**
     * Asigna la verbosidad
     *
     * @param verbose verbosidad
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * Asigna debug
     *
     * @param debug
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    /**
     * recoge el esquena DC
     *
     * @throws SQLException
     */
    private void checkDspaceDC() throws SQLException
    {
        if (dcSchema == null) dcSchema = MetadataSchema.findByNamespace(context, DCSCHEMA);
    }

    /**
     * Recoge todos los elementos DC
     *
     * @throws SQLException
     */
    protected void checkDspaceMetadataDC() throws SQLException
    {
        MetadataField[] metadataFieldsArr = MetadataField.findAllInSchema(context, dcSchema.getSchemaID());
        if (metadataFields != null) {
            metadataFields.clear();
            metadataFields = null;
        }
        metadataFields = new ArrayList<MetadataField>(Arrays.asList(metadataFieldsArr));
    }

    /**
     * devuelve la lista de POJOs de autoridades
     *
     * @return lista de POJOs de autoridades
     */
    public HashMap<String, InstallerEDMAuthBO> getAuthBOHashMap()
    {
        return authBOHashMap;
    }

    /**
     * devuelve el esquema dc
     *
     * @return esquema dc {@link MetadataSchema}
     */
    public MetadataSchema getDcSchema()
    {
        return dcSchema;
    }

    /**
     * devuelve el objeto de visualizar mensajes
     *
     * @return objeto de visualizar mensajes {@link InstallerEDMDisplayImpl}
     */
    public static InstallerEDMDisplayImpl getInstallerEDMDisplay()
    {
        return (InstallerEDMDisplayImpl) installerEDMDisplay;
    }

    /**
     * Salida del instalador por señal recibida
     *
     * @param o observador de señales
     * @param arg señal
     */
    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }

    /**
     * quitar acentos a una cadena
     *
     * @param text cadena original
     * @return cadena sin acentos
     */
    public String removeAccents(String text)
    {
        if (text != null) text = text.replaceAll(" +", "_");
        return text == null ? null : Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }//removeAccents

    /**
     * validar un cadena como uri
     *
     * @param uriStr cadena original
     * @return validez de la uri
     */
    protected boolean isValidURI(String uriStr)
    {
        try {
            URI uri = new URI(uriStr);
            uri.toURL();
            return true;
        } catch (URISyntaxException e) {
            return false;
        } catch (MalformedURLException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * hora actual en formato yyyy-MM-dd HH:mm:ss
     *
     * @return cadena con la hora actual
     */
    protected String getTime()
    {
        Calendar cal = Calendar.getInstance();
        cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(cal.getTime());
    }


    /**
     * Inicializar authBOHashMap
     */
    protected void initAuthBOHashMap()
    {
        if (authBOHashMap == null) authBOHashMap = new HashMap<String, InstallerEDMAuthBO>();
        else authBOHashMap.clear();
    }


    /**
     * autenticación de un usuario en dspace
     *
     * @return validez del usuario
     */
    protected boolean loginUser()
    {
        installerEDMDisplay.showQuestion(0, "authentication");
        String userAux = null;
        String passwordAux;
        int step = 1;
        while (true) {
            if (step == 1) installerEDMDisplay.showQuestion(0, "email.user");
            else if (step == 2) installerEDMDisplay.showQuestion(0, "password.user");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if (response == null || response.length() == 0) continue;
            response = response.trim();
            switch (step) {
                case 1:
                    userAux = response;
                    step++;
                    break;
                case 2:
                    passwordAux = response;
                    int status = 0;
                    try {
                        status = AuthenticationManager.authenticate(context, userAux, passwordAux, null, null);
                    } catch (Exception e) {
                        showException(e);
                    }
                    if (status == 1) {
                        eperson = context.getCurrentUser();
                        user = userAux;
                        password = passwordAux;
                        return true;
                    } else {
                        installerEDMDisplay.showQuestion(0, "invalid.user");
                        step = 1;
                    }
                    break;
            }
        }
    }


    /**
     * busca en un ítem si su tipo es de una autoridad SKOS_AUTH
     *
     * @param item objeto item de dspace {@link Item}
     * @return si es una autoridad
     */
    protected boolean searchSkosAuthTypeItem(Item item)
    {
        DCValue[] listDCTypeValues = item.getMetadata(dcSchema.getName(), "type", null, language);
        if (listDCTypeValues.length > 0) {
            for (DCValue dcTypeValue : listDCTypeValues) {
                if (dcTypeValue.value.equals("SKOS_AUTH")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * busca en un ítem si su campo autoridad tiene el handle del ítem y por lo tanto es una autoridad
     *
     * @param item objeto item de dspace {@link Item}
     * @return si es una autoridad
     */
    protected boolean searchSkosAuthItem(Item item)
    {
        if (searchSkosAuthTypeItem(item)) return true;
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName() + ".*.*");
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (elementsNotAuthSet.contains(dcValueName) || dcValueName.equals("type")) continue;
                if (dcValue.authority == null || dcValue.authority.isEmpty()) continue;
                if (dcValue.authority.equals(item.getHandle())) return true;
            }
        }
        return false;
    }

    /**
     * busca en todas las colecciones los elementos dc que son de autoridades
     *
     * @param authDCElements lista de elementos dc
     * @throws SQLException
     */
    protected void checkAllSkosAuthElements(ArrayList<MetadataField> authDCElements) throws SQLException
    {
        org.dspace.content.Collection[] listCollections = org.dspace.content.Collection.findAll(context);
        if (debug) installerEDMDisplay.showQuestion(0, "checkAllSkosAuthElements.searching.elements", new String[] {String.valueOf(listCollections.length)});
        if (listCollections.length > 0) {
            for (org.dspace.content.Collection collection : listCollections) {
                if (debug) installerEDMDisplay.showQuestion(0, "checkAllSkosAuthElements.searching.elements.collection", new String[] {collection.getName(), collection.getHandle()});
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    if (debug) installerEDMDisplay.showQuestion(0, "checkAllSkosAuthElements.searching.elements.item", new String[] { item.getName(), item.getHandle()});
                    else installerEDMDisplay.showProgress('.');
                    if (searchSkosAuthItem(item)) checkSkosAuthItem(authDCElements, item);
                }
            }
        }
    }


    /**
     * busca en un ítem si tiene ciertos elementos dc de autoridades, crea un POJO de autoridad {@link InstallerEDMAuthBO} y lo añade a authBOHashMap.
     * Como se crea el elemento title para cada ítem de autoridad con el valor del elemento real de la autoridad, se comprueba cuántos elementos hay y si sólo
     * existe uno y es el title, se supone que es el elemento de la autoridad, si no no se almacena porque sólo está para que tenga título el ítem.
     *
     * @param authDCElements lista de elementos dc
     * @param item objeto item de dspace {@link Item}
     */
    protected void checkSkosAuthItem(ArrayList<MetadataField> authDCElements, Item item)
    {
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName() + ".*.*");
        if (debug) installerEDMDisplay.showQuestion(0, "checkSkosAuthItem.elements", new String[]{Integer.toString(listDCValues.length)});
        if (listDCValues.length > 0) {
            Community community = null;
            org.dspace.content.Collection collection = null;
            try {
                org.dspace.content.Collection[] collections = item.getCollections();
                if (collections.length > 0) collection = collections[0];
                Community[] communities = item.getCommunities();
                if (communities.length > 0) community = communities[0];
            } catch (SQLException e) {
                showException(e);
            }
            MetadataField titleMetadataField = null;
            int numElements = 0;
            for (DCValue dcValue : listDCValues) {
                if (dcValue.value == null || dcValue.value.isEmpty()) continue;
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (!elementsNotAuthSet.contains(dcValueName)) {
                    numElements++;
                    if (!authBOHashMap.containsKey(dcValueName)) {
                        MetadataField metadataField = new MetadataField(dcSchema, dcValue.element, dcValue.qualifier, null);
                        if (dcValue.element.equals("title") && dcValue.qualifier == null) {
                            titleMetadataField = metadataField;
                            continue;
                        }
                        if (debug) installerEDMDisplay.showQuestion(0, "checkSkosAuthItem.element", new String[]{dcValueName});
                        InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(item, community, collection, dcSchema, metadataField);
                        authBOHashMap.put(dcValueName, installerEDMAuthBO);
                        authDCElements.add(metadataField);
                    }
                }
            }
            if (numElements == 1 && titleMetadataField != null && !authBOHashMap.containsKey("title")) {
                System.out.println(numElements);
                if (debug) installerEDMDisplay.showQuestion(0, "checkSkosAuthItem.element", new String[]{"title"});
                InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(item, community, collection, dcSchema, titleMetadataField);
                authBOHashMap.put("title", installerEDMAuthBO);
                authDCElements.add(titleMetadataField);
            }
        }
    }


    /**
     * busca el elemento dc con cierto nombre y calificador
     *
     * @param name nombre y calificador
     * @return elemento dc {@link MetadataField}
     */
    protected MetadataField findElementDC(String name)
    {
        int pos = name.indexOf(".");
        try {
            String element = (pos > 0)?name.substring(0, pos):name;
            String qualifier = (pos > 0)?name.substring(pos + 1):null;
            if (debug) installerEDMDisplay.showQuestion(0, "findElementDC", new String[]{Integer.toString(dcSchema.getSchemaID()), element, qualifier});
            MetadataField elementMD = MetadataField.findByElement(context, dcSchema.getSchemaID(), element, qualifier);
            if (elementMD == null) {
                return null;
            } else {
                return elementMD;
            }
        } catch (SQLException e) {
            showException(e);
        } catch (AuthorizeException e) {
            showException(e);
        }
        return null;
    }


    /**
     * saca formateado en dos columnas por pantalla los elementos dc de una lista
     *
     * @param arrFields lista de elementos dc
     */
    protected void listAllDCElements(ArrayList<MetadataField> arrFields)
    {
        int i = 1;
        for (MetadataField metadataField : arrFields) {
            String qualifier = metadataField.getQualifier();
            int elementLength = metadataField.getElement().length() + ((qualifier != null)?qualifier.length() + 1:0);
            int padding = 80 - elementLength;
            if (i % 2 == 1) {
                System.out.printf("%s%s%" + padding + "s", metadataField.getElement(), (qualifier != null)?"."+qualifier:"", " ");
            } else {
                System.out.printf("%s%s", metadataField.getElement(), (qualifier != null)?"."+qualifier:"");
                System.out.printf("%n");
            }
            i++;
        }
        System.out.flush();
        System.out.println("");
        System.out.println("");
    }

    /**
     * saca formateado en dos columnas por pantalla las cadenas de una lista
     *
     * @param list lista de cadenas
     */
    protected void listAllStrings(List<String> list)
    {
        int i = 1;
        for (String element : list) {
            int padding = 80 - element.length();
            if (i % 2 == 1) {
                System.out.printf("%s%" + padding + "s", element, " ");
            } else {
                System.out.printf("%s", element);
                System.out.printf("%n");
            }
            i++;
        }
        System.out.flush();
        System.out.println("");
        System.out.println("");
    }


    /**
     * copia un fichero a otro
     *
     * @param dspaceFileFile fichero original
     * @param dspaceFileNewFile fichero destino
     * @param prefixMessage prefijo para el código de los mensajes
     * @throws IOException
     */
    protected void copyDspaceFile2Work(File dspaceFileFile, File dspaceFileNewFile, String prefixMessage) throws IOException
    {
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(currentStepGlobal, prefixMessage + ".add", new String [] {myInstallerWorkDirPath, dspaceFileFile.getAbsolutePath()});
        if (dspaceFileNewFile.exists()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, prefixMessage + ".file.exists", new String[]{dspaceFileNewFile.getAbsolutePath()});
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (response.length() == 0 || response.equalsIgnoreCase(answerYes)) {
                    dspaceFileNewFile.delete();
                    break;
                }
                else return;
            } while (true);
        }
        org.apache.commons.io.FileUtils.copyFile(dspaceFileFile, dspaceFileNewFile);
    }


    /**
     * carga de dspace.cfg los elementos dc que están controlados como autoridades, i.e., tienen la propiedad authority.controlled
     */
    protected void loadAuthDspaceCfg()
    {
        elementsAuthDspaceCfg = new HashSet<String>();
        Properties properties = new Properties();
        InputStream is = null;
        try {
            String name = ConfigurationManager.getProperty("dspace.dir") + "/config/dspace.cfg";
            File file = new File(name);
            if (file.exists() && file.canRead()) {
                is = file.toURI().toURL().openStream();
                properties.load(is);
                for (Enumeration pe = properties.propertyNames(); pe.hasMoreElements(); ) {
                    String key = (String)pe.nextElement();
                    if (key.startsWith("authority.controlled.")) {
                        String field = key.substring("authority.controlled.".length());
                        int dot = field.indexOf(46);
                        if (dot < 0) continue;
                        String schema = field.substring(0, dot);
                        String element = field.substring(dot + 1);
                        String qualifier = null;
                        dot = element.indexOf(46);
                        if (dot >= 0) {
                            qualifier = element.substring(dot + 1);
                            element = element.substring(0, dot);
                        }
                        StringBuilder fkeySB = new StringBuilder(element);
                        if (qualifier != null) fkeySB.append(".").append(qualifier);
                        String fkey = fkeySB.toString();
                        if (ConfigurationManager.getBooleanProperty(key, true))
                            elementsAuthDspaceCfg.add(fkey);
                    }
                }
            }
        } catch (IOException e) {
            showException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    showException(e);
                }
            }
        }
    }


    /**
     * Obtiene un documento jdom de una fuente de entrada de datos, e.g.: un fichero
     *
     * @param IS fuente de entrada de datos
     * @return documento jdom
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    protected Document getDocumentFromInputSource(InputSource IS) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(IS);
    }


    /**
     * muestra la excepción y su mensaje si verbose y la pila si debug
     *
     * @param e excepción
     */
    protected void showException(Exception e)
    {
        if (verbose) installerEDMDisplay.showMessage(e.getMessage());
        if (debug) e.printStackTrace();
    }

}
