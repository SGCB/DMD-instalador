package org.dspace.installer_edm;

import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 26/10/12
 * Time: 11:30
 * To change this template use File | Settings | File Templates.
 */
public abstract class InstallerEDMBase implements Observer
{
    protected static InstallerEDM installerEDM;

    protected static String DspaceDir = null;
    protected static String TomcatBase = null;
    protected static String AskosiDataDir;
    protected static boolean verbose = false;
    protected static boolean debug = false;
    protected static Context context = null;

    protected static String language = null;

    protected static Set<Integer> stepsSet = new HashSet<Integer>();

    protected static InstallerEDMDisplay installerEDMDisplay = null;


    protected static String myInstallerDirPath = null;
    protected static String myInstallerWorkDirPath = null;
    protected static String fileSeparator = System.getProperty("file.separator");

    protected static InputStreamReader isr = null;
    protected static BufferedReader br = null;

    protected final String DCSCHEMA = "http://dublincore.org/documents/dcmi-terms/";
    protected MetadataSchema dcSchema = null;

    protected MetadataField[] metadataFields;
    protected Set<String> elementsNotAuthSet = null;

    protected int currentStepGlobal;

    protected static EPerson eperson;
    protected static String user;
    protected static String password;


    protected static HashMap<String, InstallerEDMAuthBO> authBOHashMap;

    protected static final String[] elementsNotAuth = {"identifier.uri", "date.accessioned", "date.available", "date.issued", "description.provenance", "type"};

    protected static final String[] packages = {"ASKOSI.jar", "askosiWebapp.zip", "classes.zip", "commons-dbcp.jar", "commons-pool.jar", "EDMCrosswalk.java", "EDMExport.war", "exampleAskosiData.zip", "jaxb-xalan-1.5.jar", "jsr311-api-1.1.1.jar", "jstl-1.2.jar", "log4j.jar", "openrdf-alibaba-2.0-beta6.jar", "openrdf-sesame-2.3.2-onejar.jar", "DIM2EDM.xsl"};

    protected static final String[] packagesMD5 = {"f800262e9587383fa0dbd8f748cc831e", "ab932907d73a8031cb266d20d341a6e2", "0bffffb990ea99eb02a989d346454d8e", "2666cfeb7be74b1c2d8a1665ae21192c", "01f9bed60e2f88372132d34040ee81bb", "b44cb32a1651322074ab077d3d1113b2", "202e9994bdd6f04ac4f83208a5bbec3b", "2be860d3a2529cb8789d6c27cfae5a92", "261968cebe30ffe8adcc201ad0bfa395", "c9803468299ec255c047a280ddec510f", "51e15f798e69358cb893e38c50596b9b", "599b8ba07d1d04f0ea34414e861d7ad1", "1f699edb215bcee75cb6f0616fa56993", "3054aa9109f78903852d38991b5a4ea8", "4848484a04285097cba450dcd329880c"};



    public InstallerEDMBase(int currentStepGlobal)
    {
        this();
        this.currentStepGlobal = currentStepGlobal;
    }

    public InstallerEDMBase()
    {
        try {
            if (context == null) context = new Context();
            if (installerEDMDisplay == null) installerEDMDisplay = new InstallerEDMDisplayImpl();
            if (isr == null) isr = new InputStreamReader(System.in);
            if (br == null) br = new BufferedReader(isr);
            if (myInstallerDirPath == null) myInstallerDirPath = new File(".").getAbsolutePath();
            if (myInstallerWorkDirPath == null) myInstallerWorkDirPath = myInstallerDirPath + fileSeparator + "work";
            File myInstallerWorkDirFile = new File(myInstallerWorkDirPath);
            if (!myInstallerWorkDirFile.exists()) myInstallerWorkDirFile.mkdir();
            checkDspaceDC();
            if (language == null) language = ConfigurationManager.getProperty("default.language");
            if (language == null) language = "en";
        } catch (SQLException e) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(0, "step.fail");
            showException(e);
        } catch (Exception e) {
            showException(e);
        }
    }


    public void setLanguage(String language)
    {
        this.language = language;
    }

    public void setInstallerEDMBase(InstallerEDM installerEDM)
    {
        this.installerEDM = installerEDM;
    }

    public void setDspaceDir(String DspaceDir)
    {
        this.DspaceDir = DspaceDir;
    }

    public void setTomcatBase(String TomcatBase)
    {
        this.TomcatBase = TomcatBase;
    }

    public void setAskosiDataDir(String askosiDataDir)
    {
        this.AskosiDataDir = askosiDataDir;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    private void checkDspaceDC() throws SQLException
    {
        if (dcSchema == null) dcSchema = MetadataSchema.findByNamespace(context, DCSCHEMA);
    }

    protected void checkDspaceMetadataDC() throws SQLException
    {
        metadataFields = MetadataField.findAllInSchema(context, dcSchema.getSchemaID());
    }

    public HashMap<String, InstallerEDMAuthBO> getAuthBOHashMap()
    {
        return authBOHashMap;
    }

    public MetadataSchema getDcSchema()
    {
        return dcSchema;
    }

    public static InstallerEDMDisplayImpl getInstallerEDMDisplay()
    {
        return (InstallerEDMDisplayImpl) installerEDMDisplay;
    }

    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }

    public String removeAccents(String text)
    {
        if (text != null) text = text.replaceAll(" +", "_");
        return text == null ? null : Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }//removeAccents

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

    protected String getTime()
    {
        Calendar cal = Calendar.getInstance();
        cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(cal.getTime());
    }


    protected void initAuthBOHashMap()
    {
        if (authBOHashMap == null) authBOHashMap = new HashMap<String, InstallerEDMAuthBO>();
        else authBOHashMap.clear();
    }


    protected void initElementsNotAuthSet()
    {
        if (elementsNotAuthSet == null)
            elementsNotAuthSet = new HashSet<String>();
        else
            elementsNotAuthSet.clear();
        Collections.addAll(elementsNotAuthSet, elementsNotAuth);
    }


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
                    int status = AuthenticationManager.authenticate(context, userAux, passwordAux, null, null);
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


    protected void checkSkosAuthItem(ArrayList<MetadataField> authDCElements, Item item)
    {
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName() + ".*.*");
        if (debug) installerEDMDisplay.showQuestion(0, "checkSkosAuthItem.elements", new String[]{Integer.toString(listDCValues.length)});
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                if (dcValue.value == null || dcValue.value.isEmpty()) continue;
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (!elementsNotAuthSet.contains(dcValueName) && !authBOHashMap.containsKey(dcValueName)) {
                    if (debug) installerEDMDisplay.showQuestion(0, "checkSkosAuthItem.element", new String[]{dcValueName});
                    MetadataField metadataField = new MetadataField(dcSchema, dcValue.element, dcValue.qualifier, null);
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
                    InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(item, community, collection, dcSchema, metadataField);
                    authBOHashMap.put(dcValueName, installerEDMAuthBO);
                    authDCElements.add(metadataField);
                }
            }
        }
    }


    protected MetadataField findElementDC(String name)
    {
        int pos = name.indexOf(".");
        try {
            MetadataField elementMD = MetadataField.findByElement(context, dcSchema.getSchemaID(), (pos > 0)?name.substring(0, pos - 1):name, (pos > 0)?name.substring(pos + 1):null);
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


    protected void listAllDCElements(MetadataField[] arrFields)
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
                if (response.length() == 0 || response.equalsIgnoreCase("y")) {
                    dspaceFileNewFile.delete();
                    break;
                }
                else return;
            } while (true);
        }
        org.apache.commons.io.FileUtils.copyFile(dspaceFileFile, dspaceFileNewFile);
    }


    protected Document getDocumentFromInputSource(InputSource IS) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(IS);
    }


    protected void showException(Exception e)
    {
        if (verbose) installerEDMDisplay.showMessage(e.getMessage());
        if (debug) e.printStackTrace();
    }

}
