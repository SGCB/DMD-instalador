package org.dspace.installer_edm;

import org.dspace.content.MetadataField;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 28/01/13
 * Time: 13:25
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMDspaceCfg extends InstallerEDMBase
{

    private HashMap<String, Integer> authDCElementsSetWritten;
    private Writer out = null;
    private File dspaceDirConfNewFile;

    public InstallerEDMDspaceCfg(File dspaceDirConfNewFile)
    {
        super();
        this.dspaceDirConfNewFile = dspaceDirConfNewFile;
        authDCElementsSetWritten = new HashMap<String, Integer>();
    }

    public void processDspaceCfg(ArrayList<MetadataField> authDCElements) throws IOException, NullPointerException, FileNotFoundException, IndexOutOfBoundsException
    {
        try {
            String askosiDataDir = readDspaceCfg();
            File askosiDataDestDirFile = null;
            if (askosiDataDir != null) askosiDataDestDirFile = new File(askosiDataDir);
            if (askosiDataDestDirFile != null && askosiDataDestDirFile.exists() && askosiDataDestDirFile.canRead()) {
                AskosiDataDir = askosiDataDestDirFile.getAbsolutePath();
                if (verbose) installerEDMDisplay.showMessage(installerEDMDisplay.getQuestion(3, "configureDspaceCfg.askosidatadir") + AskosiDataDir);
            } else {
                if (AskosiDataDir != null) {
                    askosiDataDestDirFile = new File(AskosiDataDir);
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
                            AskosiDataDir = askosiDataDestDirFile.getAbsolutePath();
                            break;
                        }
                    } while (true);
                }
            }

            if (authDCElements.size() > 0) {
                out = new OutputStreamWriter(new FileOutputStream(dspaceDirConfNewFile, true));
                if (AskosiDataDir == null) {
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
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }



    private String readDspaceCfg() throws FileNotFoundException, IndexOutOfBoundsException
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


}
