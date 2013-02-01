package org.dspace.installer_edm;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 15/01/13
 * Time: 8:58
 * To change this template use File | Settings | File Templates.
 */
public interface InstallerEDMDisplay
{

    public void showTitle(int stage);

    public void showMenu(int stage);

    public String getQuestion(int stage, String code);

    public String getQuestion(int stage, String code, String[] args);

    public void showQuestion(int stage, String code, String[] args);

    public void showQuestion(int stage, String code);

    public void showMessage(String message);

    public void showLn();

    public void showProgress(char prog);

}
