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

    private final String[] elementsNotAuth = {"identifier.uri", "date.accessioned", "date.available", "date.issued", "description.provenance"};
    private Set<String> elementsNotAuthSet;


    public InstallerEDMConf(InstallerEDM installerEDM, Context context, String DspaceDir, String TomcatBase, boolean verbose)
    {
        super(installerEDM, context, DspaceDir, TomcatBase, verbose);
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
            Set <String> authDCElementsSet = new HashSet<String>();
            if (installerEDM.getInstallerEDMCreateAuth() != null) {
                authDCElements = installerEDM.getInstallerEDMCreateAuth().getAuthDCElements();
                for (MetadataField dataF : authDCElements) {
                    authDCElementsSet.add(dataF.getElement() + ((dataF.getQualifier() != null)?"." + dataF.getQualifier():""));
                }
            } else {
                authDCElements = new ArrayList<MetadataField>();
            }
            try {
                checkAllSkosAuthElements(authDCElements, authDCElementsSet);
                configureDspaceCfg(dspaceDirConfFile, new File(myInstallerDirPath + System.getProperty("file.separator") + "dspace.cfg"), authDCElements, authDCElementsSet);
                String dspaceInputFormsName = DspaceDir + "config" + System.getProperty("file.separator") + "input-forms.xml";
                File dspaceInputFormsFile = new File(dspaceInputFormsName);
                if (dspaceInputFormsFile.exists() && dspaceInputFormsFile.canRead()) {
                    configureInputFormsDspace(dspaceInputFormsFile, new File(myInstallerDirPath + System.getProperty("file.separator") + "input-forms.xml"), authDCElements);
                } else System.out.println("There is not exist " + dspaceInputFormsName + " or is not readable.");
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else System.out.println("There is not exist " + dspaceDirConfName + " or is not readable, or the current directory " + dspaceDirConfNewFile.getAbsolutePath() + " is not writable.");
        return false;
    }


    private void configureInputFormsDspace(File dspaceInputFormsFile, File dspaceInputFormsNewFile, ArrayList<MetadataField> authDCElements) throws IOException
    {
        System.out.println("");
        System.out.println("Creating new input-forms.xml in " + myInstallerDirPath + " to add the authorities.\nWhen you have checked the file, replace " + dspaceInputFormsFile.getAbsolutePath() + " with this new one.");
        if (dspaceInputFormsNewFile.exists()) {
            System.out.println("There is already a file " + dspaceInputFormsNewFile.getAbsolutePath() + ". Replace it ([y]/n)?");
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


    private void configureDspaceCfg(File dspaceDirConfFile, File dspaceDirConfNewFile, ArrayList<MetadataField> authDCElements, Set<String> authDCElementsSet) throws FileNotFoundException, IndexOutOfBoundsException, IOException, NullPointerException
    {
        System.out.println("");
        System.out.println("Creating new configuration dspace.cfg in " + myInstallerDirPath + " to add the authorities.\nWhen you have checked the file, replace " + dspaceDirConfFile.getAbsolutePath() + " with this new one.");
        if (dspaceDirConfNewFile.exists()) {
            System.out.println("There is already a file " + dspaceDirConfNewFile.getAbsolutePath() + ". Replace it ([y]/n)?");
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
        HashMap<String, Integer> authDCElementsSetWritten = readDspaceCfg(dspaceDirConfFile, authDCElementsSet);
        Writer out = new OutputStreamWriter(new FileOutputStream(dspaceDirConfNewFile, true));
        try {
            File askosiDataDestDirFile;
            if (installerEDM.getInstallerEDMAskosi() != null && installerEDM.getInstallerEDMAskosi().getFinalAskosiDataDestDirFile() != null) {
                askosiDataDestDirFile = installerEDM.getInstallerEDMAskosi().getFinalAskosiDataDestDirFile();
            } else {
                System.out.println("Askosi data directory: ");
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
            String plugin = new StringBuilder().append("ASKOSI.directory = ").append(askosiDataDestDirFile.getAbsolutePath()).append("\nplugin.named.org.dspace.content.authority.ChoiceAuthority = \\\n").append("be.destin.dspace.AskosiPlugin = ASKOSI\n").toString();
            if (authDCElements.size() > 0) {
                out.write(plugin);
                for (MetadataField metadataField : authDCElements) {
                    String element = metadataField.getElement() + ((metadataField.getQualifier() != null)?"." + metadataField.getQualifier():"");
                    boolean isOk = false;
                    if (authDCElementsSetWritten.containsKey(element)) {
                        if (authDCElementsSetWritten.get(element).intValue() == 3) isOk = true;
                        else if (verbose) System.out.println("Element " + element + " is not properly configured. One ok is going to be added. Please remove the bad one.");
                    }
                    if (!isOk) {
                        if (!writeDspaceCfg(out, element)) {
                            System.out.println("Element " + element + " failed to be added.");
                        } else if (verbose) System.out.println("Added element " + element);
                    }
                }
            } else if (verbose) System.out.println("Nothing added.");
        } finally {
            out.close();
        }
    }


    private HashMap<String, Integer> readDspaceCfg(File dspaceDirConfFile, Set<String> authDCElementsSet) throws FileNotFoundException, IndexOutOfBoundsException
    {
        HashMap<String, Integer> authDCElementsSetWritten = new HashMap<String, Integer>();
        Scanner scanner = new Scanner(new FileInputStream(dspaceDirConfFile));
        Pattern patternPlugin = Pattern.compile("^\\s*choices\\.plugin\\." + dcSchema.getName() + "\\.(.+)\\s*=\\s*ASKOSI\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternPresentation = Pattern.compile("^\\s*choices\\.presentation\\." + dcSchema.getName() + "\\.(.+)\\s*=\\s*lookup\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern patternControlled = Pattern.compile("^\\s*choices\\.controlled\\." + dcSchema.getName() + "\\.(.+)\\s*=\\s*true\\s*$", Pattern.CASE_INSENSITIVE);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher matcherPlugin = patternPlugin.matcher(line);
                if (matcherPlugin.find()) {
                    String element = (String) matcherPlugin.group(1);
                    if (authDCElementsSet.contains(element)) {
                        if (authDCElementsSetWritten.containsKey(element)) {
                            int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                            authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                        } else authDCElementsSetWritten.put(element, new Integer(1));
                    }
                } else {
                    Matcher matcherPresentation = patternPresentation.matcher(line);
                    if (matcherPresentation.find()) {
                        String element = (String) matcherPresentation.group(1);
                        if (authDCElementsSet.contains(element)) {
                            if (authDCElementsSetWritten.containsKey(element)) {
                                int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                                authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                            } else authDCElementsSetWritten.put(element, new Integer(1));
                        }
                    } else {
                        Matcher matcherControlled = patternControlled.matcher(line);
                        if (matcherControlled.find()) {
                            String element = (String) matcherControlled.group(1);
                            if (authDCElementsSet.contains(element)) {
                                if (authDCElementsSetWritten.containsKey(element)) {
                                    int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                                    authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                                } else authDCElementsSetWritten.put(element, new Integer(1));
                            }
                        }
                    }
                }
            }
        } finally {
            scanner.close();
        }
        return authDCElementsSetWritten;
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

    private void checkAllSkosAuthElements(ArrayList<MetadataField> authDCElements, Set<String> authDCElementsSet) throws SQLException
    {
        String language;
        language = ConfigurationManager.getProperty("default.language");
        if (language == null) language = "en";
        Collection[] listCollections = Collection.findAll(context);
        if (listCollections.length > 0) {
            for (Collection collection : listCollections) {
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    DCValue[] listDCTypeValues = item.getMetadata(dcSchema.getName(), "type", "", language);
                    if (listDCTypeValues.length > 0) {
                        for (DCValue dcTypeValue : listDCTypeValues) {
                            if (dcTypeValue.value.equals("SKOS_AUTH")) {
                                checkSkosAuthItem(authDCElements, authDCElementsSet, item);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }


    private void checkSkosAuthItem(ArrayList<MetadataField> authDCElements, Set<String> authDCElementsSet, Item item)
    {
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName() + ".*.*");
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                if (dcValue.value == null || dcValue.value.isEmpty()) continue;
                String dcValueName = dcValue.element + ((dcValue.qualifier != null && !dcValue.qualifier.isEmpty())?"." + dcValue.qualifier:"");
                if (!elementsNotAuthSet.contains(dcValueName) && !authDCElementsSet.contains(dcValueName)) {
                    authDCElementsSet.add(dcValueName);
                    authDCElements.add(new MetadataField(dcSchema, dcValue.element, dcValue.qualifier, null));
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
