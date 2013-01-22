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
    protected InstallerEDM installerEDM;

    protected static String DspaceDir = null;
    protected static String TomcatBase = null;
    protected boolean verbose = false;
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
            if (installerEDMDisplay == null) installerEDMDisplay = new InstallerEDMDisplayImpl();
            if (isr == null) isr = new InputStreamReader(System.in);
            if (br == null) br = new BufferedReader(isr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InstallerEDMBase(InstallerEDM installerEDM, String DspaceDir, String TomcatBase, boolean verbose)
    {
        this();
        this.installerEDM = installerEDM;
        if (this.DspaceDir == null) this.DspaceDir = DspaceDir;
        if (this.TomcatBase == null) this.TomcatBase = TomcatBase;
        this.verbose = verbose;
        try {
            if (myInstallerDirPath == null) myInstallerDirPath = new File(".").getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public InstallerEDMBase(InstallerEDM installerEDM, Context context, String DspaceDir, String TomcatBase, boolean verbose)
    {
        this(installerEDM, DspaceDir, TomcatBase, verbose);
        if (this.context == null) this.context = context;
        try {
            checkDspaceDC();
            checkDspaceMetadataDC();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void setDspaceDir(String DspaceDir)
    {
        this.DspaceDir = DspaceDir;
    }

    public void setTomcatBase(String TomcatBase)
    {
        this.TomcatBase = TomcatBase;
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
