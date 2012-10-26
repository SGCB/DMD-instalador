package org.dspace.installer_edm;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 26/10/12
 * Time: 11:30
 * To change this template use File | Settings | File Templates.
 */
public abstract class InstallerEDMBase
{
    protected String DspaceDir = null;
    protected String TomcatBase = null;
    protected boolean verbose = false;

    protected InputStreamReader isr;
    protected BufferedReader br;


    public InstallerEDMBase(String DspaceDir, String TomcatBase, boolean verbose)
    {
        this.DspaceDir = DspaceDir;
        this.TomcatBase = TomcatBase;
        this.verbose = verbose;
        try {
            isr = new InputStreamReader(System.in);
            br = new BufferedReader(isr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
