package org.dspace.installer_edm;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.SQLException;
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

    protected static Set<Integer> stepsSet = new HashSet<Integer>();

    protected static InstallerEDMDisplay installerEDMDisplay = null;


    protected static String myInstallerDirPath = null;

    protected static InputStreamReader isr = null;
    protected static BufferedReader br = null;

    protected final String DCSCHEMA = "http://dublincore.org/documents/dcmi-terms/";
    protected MetadataSchema dcSchema;

    protected MetadataField[] metadataFields;

    protected HashMap<String, InstallerEDMAuthBO> authBOHashMap;

    public InstallerEDMBase()
    {
        try {
            context = new Context();
            if (installerEDMDisplay == null) installerEDMDisplay = new InstallerEDMDisplayImpl();
            if (isr == null) isr = new InputStreamReader(System.in);
            if (br == null) br = new BufferedReader(isr);
            if (myInstallerDirPath == null) myInstallerDirPath = new File(".").getAbsolutePath();
        } catch (SQLException e) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(0, "step.fail");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        dcSchema = MetadataSchema.findByNamespace(context, DCSCHEMA);
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


}
