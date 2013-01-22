package org.dspace.installer_edm;

import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 29/10/12
 * Time: 11:54
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMConf extends InstallerEDMBase implements Observer
{

    private final String[] elementsNotAuth = {"identifier.uri", "date.accessioned", "date.available", "date.issued", "description.provenance", "type"};
    private Set<String> elementsNotAuthSet;


    public InstallerEDMConf()
    {
        super();
        elementsNotAuthSet = new HashSet<String>();
        Collections.addAll(elementsNotAuthSet, elementsNotAuth);
    }


    public boolean configureAll()
    {
        String dspaceDirConfName = DspaceDir + "config" + System.getProperty("file.separator") + "dspace.cfg";
        File dspaceDirConfFile = new File(dspaceDirConfName);
        File dspaceDirConfNewFile = new File(".");
        if (dspaceDirConfFile.exists() && dspaceDirConfFile.canRead() && dspaceDirConfNewFile.canWrite()) {
            ArrayList<MetadataField> authDCElements;
            if (installerEDM.getInstallerEDMCreateAuth() != null) {
                authDCElements = installerEDM.getInstallerEDMCreateAuth().getAuthDCElements();
            } else {
                authDCElements = new ArrayList<MetadataField>();
                if (authBOHashMap == null) authBOHashMap = new HashMap<String, InstallerEDMAuthBO>();
            }
            try {
                checkAllSkosAuthElements(authDCElements);
                configureDspaceCfg(dspaceDirConfFile, new File(myInstallerDirPath + System.getProperty("file.separator") + "dspace.cfg"), authDCElements);
                String dspaceInputFormsName = DspaceDir + "config" + System.getProperty("file.separator") + "input-forms.xml";
                File dspaceInputFormsFile = new File(dspaceInputFormsName);
                if (dspaceInputFormsFile.exists() && dspaceInputFormsFile.canRead()) {
                    configureInputFormsDspace(dspaceInputFormsFile, new File(myInstallerDirPath + System.getProperty("file.separator") + "input-forms.xml"), authDCElements);
                } else installerEDMDisplay.showMessage(dspaceInputFormsName + installerEDMDisplay.getQuestion(3, "configureAll.inputform.notexist"));
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else installerEDMDisplay.showQuestion(3, "configureAll.dspacedirconf.notexist", new String [] {dspaceDirConfName, dspaceDirConfNewFile.getAbsolutePath()});
        return false;
    }


    private void configureInputFormsDspace(File dspaceInputFormsFile, File dspaceInputFormsNewFile, ArrayList<MetadataField> authDCElements) throws IOException
    {
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(3, "configureInputFormsDspace.inputforms.add", new String [] {myInstallerDirPath, dspaceInputFormsFile.getAbsolutePath()});
        if (dspaceInputFormsNewFile.exists()) {
            installerEDMDisplay.showMessage(dspaceInputFormsNewFile.getAbsolutePath() + installerEDMDisplay.getQuestion(3, "configureInputFormsDspace.inpuforms.file.exists"));
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

        Writer out = new OutputStreamWriter(new FileOutputStream(dspaceInputFormsNewFile, true));
        try {

        } finally {
            out.close();
        }
    }


    private void configureDspaceCfg(File dspaceDirConfFile, File dspaceDirConfNewFile, ArrayList<MetadataField> authDCElements) throws FileNotFoundException, IndexOutOfBoundsException, IOException, NullPointerException
    {
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(3, "configureDspaceCfg.dspacecfg.add", new String [] {myInstallerDirPath, dspaceDirConfFile.getAbsolutePath()});
        if (dspaceDirConfNewFile.exists()) {
            installerEDMDisplay.showMessage(dspaceDirConfNewFile.getAbsolutePath() + installerEDMDisplay.getQuestion(3, "configureDspaceCfg.dspacecfg.file.exists"));
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
        HashMap<String, Integer> authDCElementsSetWritten = new HashMap<String, Integer>();
        String askosiDataDir = readDspaceCfg(dspaceDirConfNewFile, authDCElementsSetWritten);
        Writer out = new OutputStreamWriter(new FileOutputStream(dspaceDirConfNewFile, true));
        try {
            File askosiDataDestDirFile = null;
            if (askosiDataDir != null) askosiDataDestDirFile = new File(askosiDataDir);
            if (askosiDataDestDirFile != null && askosiDataDestDirFile.exists() && askosiDataDestDirFile.canRead()) {
                if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "configureDspaceCfg.askosidatadir") + askosiDataDir);
            } else {
                askosiDataDir = null;
                if (installerEDM.getInstallerEDMAskosi() != null && installerEDM.getInstallerEDMAskosi().getFinalAskosiDataDestDirFile() != null) {
                    askosiDataDestDirFile = installerEDM.getInstallerEDMAskosi().getFinalAskosiDataDestDirFile();
                    if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "configureDspaceCfg.askosidatadir") + askosiDataDestDirFile.getAbsolutePath());
                } else {
                    installerEDMDisplay.showQuestion(3, "configureDspaceCfg.askosidatadir");
                    String response = null;
                    do {
                        response = br.readLine();
                        if (response == null || response.length() == 0) continue;
                        response = response.trim();
                        askosiDataDestDirFile = new File(response);
                        if (askosiDataDestDirFile.exists()) {
                            break;
                        }
                    } while (true);
                }
            }

            if (authDCElements.size() > 0) {
                if (askosiDataDir == null) {
                    String plugin = new StringBuilder().append("\nASKOSI.directory = ").append(askosiDataDestDirFile.getAbsolutePath()).append("\nplugin.named.org.dspace.content.authority.ChoiceAuthority = \\\n").append("be.destin.dspace.AskosiPlugin = ASKOSI\n").toString();
                    out.write(plugin);
                }
                for (MetadataField metadataField : authDCElements) {
                    String element = metadataField.getElement() + ((metadataField.getQualifier() != null)?"." + metadataField.getQualifier():"");
                    boolean isOk = false;
                    if (authDCElementsSetWritten.containsKey(element)) {
                        if (authDCElementsSetWritten.get(element).intValue() == 3) isOk = true;
                        else if (verbose) installerEDMDisplay.showMessage(element + installerEDMDisplay.getQuestion(3, "configureDspaceCfg.element.incorrect"));
                    }
                    if (!isOk) {
                        if (!writeDspaceCfg(out, element)) {
                            installerEDMDisplay.showMessage(element + installerEDMDisplay.getQuestion(3, "configureDspaceCfg.element.add.fail"));
                        } else if (verbose) installerEDMDisplay.showMessage(element + installerEDMDisplay.getQuestion(3, "configureDspaceCfg.element.add"));
                    }
                }
            } else if (verbose) installerEDMDisplay.showQuestion(3, "configureDspaceCfg.nothing.add");
        } finally {
            out.close();
        }
    }


    private String readDspaceCfg(File dspaceDirConfNewFile, HashMap<String, Integer> authDCElementsSetWritten) throws FileNotFoundException, IndexOutOfBoundsException
    {
        String dataDir = null;
        Scanner scanner = new Scanner(new FileInputStream(dspaceDirConfNewFile));
        Pattern patternAskosiDir = Pattern.compile("^\\s*ASKOSI\\.directory\\s*=\\s*(.+)\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternAskosiAuthPlugin = Pattern.compile("^\\s*plugin\\.named\\.org\\.dspace\\.content\\.authority\\.ChoiceAuthority\\s*=\\s*(\\Q\\\\E|be\\.destin\\.dspace\\.AskosiPlugin\\s*=\\s*ASKOSI)\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternAskosiAuthPluginBe = Pattern.compile("^\\s*be\\.destin\\.dspace\\.AskosiPlugin\\s*=\\s*ASKOSI\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternPlugin = Pattern.compile("^\\s*choices\\.plugin\\." + dcSchema.getName() + "\\.(.+?)\\s*=\\s*ASKOSI\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternPresentation = Pattern.compile("^\\s*choices\\.presentation\\." + dcSchema.getName() + "\\.(.+?)\\s*=\\s*lookup\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternControlled = Pattern.compile("^\\s*authority\\.controlled\\." + dcSchema.getName() + "\\.(.+?)\\s*=\\s*true\\s*$", Pattern.CASE_INSENSITIVE);

        boolean patternAskosiAuthPluginRead = false;
        boolean patternAskosiAuthPluginBeRead = false;
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                if (dataDir == null) {
                    Matcher matcherAskosiDir = patternAskosiDir.matcher(line);
                    if (matcherAskosiDir.find()) {
                         dataDir = (String) matcherAskosiDir.group(1);
                        if (verbose) installerEDMDisplay.showQuestion(3, "readDspaceCfg.line.found.data", new String[] {line, dataDir});
                    }
                }
                if (!patternAskosiAuthPluginRead) {
                    Matcher matcherAskosiAuthPlugin = patternAskosiAuthPlugin.matcher(line);
                    if (matcherAskosiAuthPlugin.find()) {
                        patternAskosiAuthPluginRead = true;
                        String content = (String) matcherAskosiAuthPlugin.group(1);
                        if (verbose) installerEDMDisplay.showQuestion(3, "readDspaceCfg.line.found.content", new String[] {line, content});
                        Matcher matcherAskosiAuthPluginBe = patternAskosiAuthPluginBe.matcher(content);
                        if (matcherAskosiAuthPluginBe.find()) {
                            patternAskosiAuthPluginBeRead = true;
                        }
                    }
                }
                if (!patternAskosiAuthPluginBeRead) {
                    Matcher matcherAskosiAuthPluginBe = patternAskosiAuthPluginBe.matcher(line);
                    if (matcherAskosiAuthPluginBe.find()) {
                        if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "readDspaceCfg.line.found") + line);
                        patternAskosiAuthPluginBeRead = true;
                    }
                }

                Matcher matcherPlugin = patternPlugin.matcher(line);
                if (matcherPlugin.find()) {
                    String element = (String) matcherPlugin.group(1);
                    if (authBOHashMap.containsKey(element)) {
                        if (verbose) installerEDMDisplay.showQuestion(3, "readDspaceCfg.line.found.element", new String[] {line, element});
                        if (authDCElementsSetWritten.containsKey(element)) {
                            int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                            authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                        } else authDCElementsSetWritten.put(element, new Integer(1));
                    }
                } else {
                    Matcher matcherPresentation = patternPresentation.matcher(line);
                    if (matcherPresentation.find()) {
                        String element = (String) matcherPresentation.group(1);
                        if (authBOHashMap.containsKey(element)) {
                            if (verbose) installerEDMDisplay.showQuestion(3, "readDspaceCfg.line.found.element", new String[] {line, element});
                            if (authDCElementsSetWritten.containsKey(element)) {
                                int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                                authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                            } else authDCElementsSetWritten.put(element, new Integer(1));
                        }
                    } else {
                        Matcher matcherControlled = patternControlled.matcher(line);
                        if (matcherControlled.find()) {
                            String element = (String) matcherControlled.group(1);
                            if (authBOHashMap.containsKey(element)) {
                                if (verbose) installerEDMDisplay.showQuestion(3, "readDspaceCfg.line.found.element", new String[] {line, element});
                                if (authDCElementsSetWritten.containsKey(element)) {
                                    int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                                    authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                                } else authDCElementsSetWritten.put(element, new Integer(1));
                            }
                        }
                    }
                }
            }
            if (dataDir != null && (!patternAskosiAuthPluginBeRead || !patternAskosiAuthPluginRead)) dataDir = null;
        } finally {
            scanner.close();
        }
        return dataDir;
    }


    private boolean writeDspaceCfg(Writer out, String element)
    {
        String choice = new StringBuilder().append("\nchoices.plugin.").append(dcSchema.getName()).append(".").append(element).append(" = ASKOSI\n").append("choices.presentation.").append(dcSchema.getName()).append(".").append(element).append(" = lookup\n").append("authority.controlled.").append(dcSchema.getName()).append(".").append(element).append(" = true").toString();
        try {
            out.write(choice);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
                if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "checkAllSkosAuthElements.searching.elements.collection") + collection.getName() + " " + collection.getHandle());
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "checkAllSkosAuthElements.searching.elements.item") + item.getName() + " " + item.getHandle());
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
        if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "checkSkosAuthItem.elements") + listDCValues.length);
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                if (dcValue.value == null || dcValue.value.isEmpty()) continue;
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (!elementsNotAuthSet.contains(dcValueName) && !authBOHashMap.containsKey(dcValueName)) {
                    if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "checkSkosAuthItem.element") + dcValueName);
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
