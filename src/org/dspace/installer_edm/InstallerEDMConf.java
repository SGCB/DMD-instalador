package org.dspace.installer_edm;

import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;


import java.io.*;
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

    private Set<String> elementsNotAuthSet = null;


    public InstallerEDMConf()
    {
        super();
        if (elementsNotAuthSet == null)
            elementsNotAuthSet = new HashSet<String>();
        else
            elementsNotAuthSet.clear();
        Collections.addAll(elementsNotAuthSet, elementsNotAuth);
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
                if (authBOHashMap == null) authBOHashMap = new HashMap<String, InstallerEDMAuthBO>();
                else authBOHashMap.clear();
            }
            try {
                checkAllSkosAuthElements(authDCElements);
                configureDspaceCfg(dspaceDirConfFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "dspace.cfg"), authDCElements);

                if (authDCElements.size() > 0) {
                    String dspaceInputFormsName = DspaceDir + "config" + fileSeparator + "input-forms.xml";
                    File dspaceInputFormsFile = new File(dspaceInputFormsName);

                    if (dspaceInputFormsFile.exists() && dspaceInputFormsFile.canRead()) {
                        configureInputFormsDspace(dspaceInputFormsFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "input-forms.xml"), authDCElements);

                        if (AskosiDataDir != null) {
                            File askosiDataDirFile = new File(AskosiDataDir);
                            if (askosiDataDirFile.exists() && askosiDataDirFile.isDirectory() && askosiDataDirFile.canWrite()) {
                                configureAskosiVocabularies(askosiDataDirFile);
                                installerEDMDisplay.showLn();
                                installerEDMDisplay.showQuestion(3, "configureAll.ok");
                                return true;
                            } else {
                                installerEDMDisplay.showQuestion(3, "configureAll.AskosiDataDir.notexist", new String[]{AskosiDataDir});
                            }
                        } else installerEDMDisplay.showQuestion(3, "configureAll.AskosiDataDir.notexist");
                    } else installerEDMDisplay.showQuestion(3, "configureAll.inputforms.notexist", new String[]{dspaceInputFormsName});
                } else {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(3, "configureAll.notauthdcelements");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } else installerEDMDisplay.showQuestion(3, "configureAll.dspacedirconf.notexist", new String [] {dspaceDirConfName, dspaceDirConfNewFile.getAbsolutePath()});
        return false;
    }


    private void configureAskosiVocabularies(File askosiDataDirFile) throws IOException
    {
        InstallerEDMAskosiVocabularies installerEDMAskosiVocabularies = new InstallerEDMAskosiVocabularies(askosiDataDirFile);
        installerEDMAskosiVocabularies.processAskosiVocabularies();
    }

    private void configureInputFormsDspace(File dspaceInputFormsFile, File dspaceInputFormsNewFile, ArrayList<MetadataField> authDCElements) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException, SQLException
    {
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(3, "configureInputFormsDspace.inputforms.add", new String [] {myInstallerWorkDirPath, dspaceInputFormsFile.getAbsolutePath()});
        if (dspaceInputFormsNewFile.exists()) {
            installerEDMDisplay.showQuestion(3, "configureInputFormsDspace.inputforms.file.exists", new String[]{dspaceInputFormsNewFile.getAbsolutePath()});
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (response.length() == 0 || response.equalsIgnoreCase("y")) {
                    dspaceInputFormsNewFile.delete();
                    break;
                }
                else return;
            } while (true);
        }
        org.apache.commons.io.FileUtils.copyFile(dspaceInputFormsFile, dspaceInputFormsNewFile);
        File dspaceInputFormsFileDtd = new File(DspaceDir + "config" + fileSeparator + "input-forms.dtd");
        File dspaceInputFormsFileDtdNew = new File(myInstallerWorkDirPath + fileSeparator + "input-forms.dtd");
        org.apache.commons.io.FileUtils.copyFile(dspaceInputFormsFileDtd, dspaceInputFormsFileDtdNew);
        InstallerEDMInputForms installerEDMInputForms = new InstallerEDMInputForms(dspaceInputFormsNewFile.getAbsolutePath());
        installerEDMInputForms.processInputForms();
        org.apache.commons.io.FileUtils.deleteQuietly(dspaceInputFormsFileDtdNew);
    }


    private void configureDspaceCfg(File dspaceDirConfFile, File dspaceDirConfNewFile, ArrayList<MetadataField> authDCElements) throws FileNotFoundException, IndexOutOfBoundsException, IOException, NullPointerException
    {
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(3, "configureDspaceCfg.dspacecfg.add", new String [] {myInstallerWorkDirPath, dspaceDirConfFile.getAbsolutePath()});
        if (dspaceDirConfNewFile.exists()) {
            installerEDMDisplay.showQuestion(3, "configureDspaceCfg.dspacecfg.file.exists", new String[]{dspaceDirConfNewFile.getAbsolutePath()});
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (response.length() == 0 || response.equalsIgnoreCase("y")) {
                    dspaceDirConfNewFile.delete();
                    break;
                }
                else return;
            } while (true);
        }
        org.apache.commons.io.FileUtils.copyFile(dspaceDirConfFile, dspaceDirConfNewFile);
        InstallerEDMDspaceCfg installerEDMDspaceCfg = new InstallerEDMDspaceCfg(dspaceDirConfNewFile);
        installerEDMDspaceCfg.processDspaceCfg(authDCElements);
    }


    private void checkAllSkosAuthElements(ArrayList<MetadataField> authDCElements) throws SQLException
    {
        String language;
        language = ConfigurationManager.getProperty("default.language");
        if (language == null) language = "en";
        Collection[] listCollections = Collection.findAll(context);
        if (verbose) installerEDMDisplay.showQuestion(3, "checkAllSkosAuthElements.searching.elements", new String[] {String.valueOf(listCollections.length)});
        if (listCollections.length > 0) {
            for (Collection collection : listCollections) {
                if (verbose) installerEDMDisplay.showQuestion(3, "checkAllSkosAuthElements.searching.elements.collection", new String[] {collection.getName(), collection.getHandle()});
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    if (verbose) installerEDMDisplay.showQuestion(3, "checkAllSkosAuthElements.searching.elements.item", new String[] { item.getName(), item.getHandle()});
                    DCValue[] listDCTypeValues = item.getMetadata(dcSchema.getName(), "type", null, language);
                    if (listDCTypeValues.length > 0) {
                        for (DCValue dcTypeValue : listDCTypeValues) {
                            if (dcTypeValue.value.equals("SKOS_AUTH")) {
                                checkSkosAuthItem(authDCElements, item);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }


    private void checkSkosAuthItem(ArrayList<MetadataField> authDCElements, Item item)
    {
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName() + ".*.*");
        if (verbose) installerEDMDisplay.showQuestion(3, "checkSkosAuthItem.elements", new String[]{Integer.toString(listDCValues.length)});
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                if (dcValue.value == null || dcValue.value.isEmpty()) continue;
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (!elementsNotAuthSet.contains(dcValueName) && !authBOHashMap.containsKey(dcValueName)) {
                    if (verbose) installerEDMDisplay.showQuestion(3, "checkSkosAuthItem.element", new String[]{dcValueName});
                    MetadataField metadataField = new MetadataField(dcSchema, dcValue.element, dcValue.qualifier, null);
                    Community community = null;
                    Collection collection = null;
                    try {
                        Collection[] collections = item.getCollections();
                        if (collections.length > 0) collection = collections[0];
                        Community[] communities = item.getCommunities();
                        if (communities.length > 0) community = communities[0];
                    } catch (SQLException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(item, community, collection, dcSchema, metadataField);
                    authBOHashMap.put(dcValueName, installerEDMAuthBO);
                    authDCElements.add(metadataField);
                }
            }
        }
    }


    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
