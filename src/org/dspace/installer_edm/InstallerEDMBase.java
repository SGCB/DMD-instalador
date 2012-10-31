package org.dspace.installer_edm;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 26/10/12
 * Time: 11:30
 * To change this template use File | Settings | File Templates.
 */
public abstract class InstallerEDMBase
{
    protected InstallerEDM installerEDM;

    protected String DspaceDir = null;
    protected String TomcatBase = null;
    protected boolean verbose = false;
    protected Context context;

    protected String myInstallerDirPath;

    protected InputStreamReader isr;
    protected BufferedReader br;

    protected final String DCSCHEMA = "http://dublincore.org/documents/dcmi-terms/";
    protected MetadataSchema dcSchema;

    protected MetadataField[] metadataFields;

    protected HashMap<String, InstallerEDMAuthBO> authBOHashMap;


    public InstallerEDMBase(InstallerEDM installerEDM, String DspaceDir, String TomcatBase, boolean verbose)
    {
        this.installerEDM = installerEDM;
        this.DspaceDir = DspaceDir;
        this.TomcatBase = TomcatBase;
        this.verbose = verbose;
        try {
            myInstallerDirPath = new File(".").getAbsolutePath();
            isr = new InputStreamReader(System.in);
            br = new BufferedReader(isr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public InstallerEDMBase(InstallerEDM installerEDM, Context context, String DspaceDir, String TomcatBase, boolean verbose)
    {
        this(installerEDM, DspaceDir, TomcatBase, verbose);
        this.context = context;
        try {
            checkDspaceDC();
            checkDspaceMetadataDC();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

}
