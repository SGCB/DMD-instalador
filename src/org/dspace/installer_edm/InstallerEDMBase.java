package org.dspace.installer_edm;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

import java.io.BufferedReader;
import java.io.File;
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
    protected static Context context = null;

    protected static String language = "en";

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

    protected int currentStepGlobal;

    protected static HashMap<String, InstallerEDMAuthBO> authBOHashMap;

    protected static final String[] elementsNotAuth = {"identifier.uri", "date.accessioned", "date.available", "date.issued", "description.provenance", "type"};


    public InstallerEDMBase(int currentStepGlobal)
    {
        super();
        this.currentStepGlobal = currentStepGlobal;
    }

    public InstallerEDMBase()
    {
        try {
            context = new Context();
            if (installerEDMDisplay == null) installerEDMDisplay = new InstallerEDMDisplayImpl();
            if (isr == null) isr = new InputStreamReader(System.in);
            if (br == null) br = new BufferedReader(isr);
            if (myInstallerDirPath == null) myInstallerDirPath = new File(".").getAbsolutePath();
            if (myInstallerWorkDirPath == null) myInstallerWorkDirPath = myInstallerDirPath + fileSeparator + "work";
            File myInstallerWorkDirFile = new File(myInstallerWorkDirPath);
            if (!myInstallerWorkDirFile.exists()) myInstallerWorkDirFile.mkdir();
            checkDspaceDC();
        } catch (SQLException e) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(0, "step.fail");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void checkDspaceDC() throws SQLException
    {
        if (dcSchema == null) dcSchema = MetadataSchema.findByNamespace(context, DCSCHEMA);
    }

    private void checkDspaceMetadataDC() throws SQLException
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


}
