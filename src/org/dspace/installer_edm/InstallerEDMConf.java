package org.dspace.installer_edm;

import org.dspace.content.MetadataField;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 29/10/12
 * Time: 11:54
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMConf extends InstallerEDMBase implements Observer
{


    public InstallerEDMConf(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initElementsNotAuthSet();
    }


    public boolean configureAll()
    {
        String dspaceDirConfName = DspaceDir + "config" + fileSeparator + "dspace.cfg";
        File dspaceDirConfFile = new File(dspaceDirConfName);
        File dspaceDirConfNewFile = new File(myInstallerWorkDirPath);

        if (dspaceDirConfFile.exists() && dspaceDirConfFile.canRead() && dspaceDirConfNewFile.canWrite()) {
            ArrayList<MetadataField> authDCElements;
            if (installerEDM.getInstallerEDMCreateAuth() != null) {
                authDCElements = installerEDM.getInstallerEDMCreateAuth().getAuthDCElements();
            } else {
                authDCElements = new ArrayList<MetadataField>();
                initAuthBOHashMap();
            }
            try {
                checkAllSkosAuthElements(authDCElements);
                boolean  modified = configureDspaceCfg(dspaceDirConfFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "dspace.cfg"), authDCElements);

                if (authDCElements.size() > 0) {
                    String dspaceInputFormsName = DspaceDir + "config" + fileSeparator + "input-forms.xml";
                    File dspaceInputFormsFile = new File(dspaceInputFormsName);

                    if (dspaceInputFormsFile.exists() && dspaceInputFormsFile.canRead()) {
                        if (configureInputFormsDspace(dspaceInputFormsFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "input-forms.xml"), authDCElements)) {

                            if (AskosiDataDir != null) {
                                File askosiDataDirFile = new File(AskosiDataDir);
                                if (askosiDataDirFile.exists() && askosiDataDirFile.isDirectory() && askosiDataDirFile.canWrite()) {
                                    configureAskosiVocabularies(askosiDataDirFile);
                                    installerEDMDisplay.showLn();
                                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.ok");
                                    return true;
                                } else {
                                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.AskosiDataDir.notexist", new String[]{AskosiDataDir});
                                }
                            } else installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.AskosiDataDir.notexist");
                        }
                    } else installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.inputforms.notexist", new String[]{dspaceInputFormsName});
                } else {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.notauthdcelements");
                }
                if (modified) {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.restart", new String[]{myInstallerWorkDirPath});
                }
            } catch (SQLException e) {
                showException(e);
            } catch (FileNotFoundException e) {
                showException(e);
            } catch (IOException e) {
                showException(e);
            } catch (XPathExpressionException e) {
                showException(e);
            } catch (ParserConfigurationException e) {
                showException(e);
            } catch (SAXException e) {
                showException(e);
            } catch (TransformerException e) {
                showException(e);
            }
        } else installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.dspacedirconf.notexist", new String [] {dspaceDirConfName, dspaceDirConfNewFile.getAbsolutePath()});
        return false;
    }


    private void configureAskosiVocabularies(File askosiDataDirFile) throws IOException
    {
        InstallerEDMAskosiVocabularies installerEDMAskosiVocabularies = new InstallerEDMAskosiVocabularies(currentStepGlobal, askosiDataDirFile);
        installerEDMAskosiVocabularies.processAskosiVocabularies();
    }

    private boolean configureInputFormsDspace(File dspaceInputFormsFile, File dspaceInputFormsNewFile, ArrayList<MetadataField> authDCElements) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException, SQLException
    {
        if (authBOHashMap.size() == 0) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "configureInputFormsDspace.notauth");
            return false;
        }
        copyDspaceFile2Work(dspaceInputFormsFile, dspaceInputFormsNewFile, "configureInputFormsDspace.inputforms");
        File dspaceInputFormsFileDtd = new File(DspaceDir + "config" + fileSeparator + "input-forms.dtd");
        File dspaceInputFormsFileDtdNew = new File(myInstallerWorkDirPath + fileSeparator + "input-forms.dtd");
        org.apache.commons.io.FileUtils.copyFile(dspaceInputFormsFileDtd, dspaceInputFormsFileDtdNew);
        InstallerEDMInputForms installerEDMInputForms = new InstallerEDMInputForms(currentStepGlobal, dspaceInputFormsNewFile.getAbsolutePath());
        installerEDMInputForms.processInputForms();
        org.apache.commons.io.FileUtils.deleteQuietly(dspaceInputFormsFileDtdNew);
        return true;
    }


    private boolean configureDspaceCfg(File dspaceDirConfFile, File dspaceDirConfNewFile, ArrayList<MetadataField> authDCElements) throws IndexOutOfBoundsException, IOException, NullPointerException
    {
        copyDspaceFile2Work(dspaceDirConfFile, dspaceDirConfNewFile, "configureDspaceCfg.dspacecfg");
        InstallerEDMDspaceCfg installerEDMDspaceCfg = new InstallerEDMDspaceCfg(currentStepGlobal, dspaceDirConfNewFile);
        return installerEDMDspaceCfg.processDspaceCfg(authDCElements);
    }



    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
