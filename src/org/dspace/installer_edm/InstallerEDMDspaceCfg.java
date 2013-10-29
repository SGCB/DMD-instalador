/**
 *  Copyright 2013 Spanish Minister of Education, Culture and Sport
 *
 *  written by MasMedios
 *
 *  Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work  except in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://ec.europa.eu/idabc/servlets/Docbb6d.pdf?id=31979
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis,
 *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.dspace.installer_edm;

import org.dspace.content.MetadataField;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @class InstallerEDMDspaceCfg
 *
 * Clase para configurar dspace para usar Askosi.
 * Se configura el archivo dspace.cfg para habilitar el plugin de Askosi para jspui
 * e indicarle qué elementos dc se controlan mediante vocabularios de Askosi.
 *
 *
 */
public class InstallerEDMDspaceCfg extends InstallerEDMBase
{

    /**
     * Elementos dc agregados a dspace.cfg para controlarse como vocabularios
     */
    private HashMap<String, Integer> authDCElementsSetWritten;

    /**
     * Flujo de datos de salida para escribir en dspaceDirConfNewFile
     */
    private Writer out = null;

    /**
     * Archivo dspace.cfg en el directori ode trabajo del instalador
     */
    private File dspaceDirConfNewFile;

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param dspaceDirConfNewFile Archivo dspace.cfg en el directori ode trabajo del instalador
     */
    public InstallerEDMDspaceCfg(int currentStepGlobal, File dspaceDirConfNewFile)
    {
        super(currentStepGlobal);
        this.dspaceDirConfNewFile = dspaceDirConfNewFile;
        authDCElementsSetWritten = new HashMap<String, Integer>();
    }

    /**
     * Lee dspace.cfg y si falta la habilitación de Askosi, la configura modificando el archivo
     * Añade los elementos dc que son autoridades, se han elegido para controlarse con vocabularios y todavía no están en dspace.cfg
     * Si no está definido un directorio de datos de Askosi lo solicita.
     *
     * @param authDCElements lista de elementos dc que son autoridades
     * @return si se ha modificado dspace.cfg
     * @throws IOException
     * @throws NullPointerException
     * @throws IndexOutOfBoundsException
     */
    public boolean processDspaceCfg(ArrayList<MetadataField> authDCElements) throws IOException, NullPointerException, IndexOutOfBoundsException
    {
        boolean modified = false;
        try {
            // menú de petición de elementos dc a añadir
            addAuthDCElements(authDCElements);
            // lectura del archivo dspace.cfg para comprobar qué hay configurado de askosi
            String askosiDataDir = readDspaceCfg();
            // comprobación del directorio de datos de Askosi
            File askosiDataDestDirFile = null;
            boolean askosiDataDirAdd = false;
            if (askosiDataDir != null) askosiDataDestDirFile = new File(askosiDataDir);
            if (askosiDataDestDirFile != null && askosiDataDestDirFile.exists() && askosiDataDestDirFile.canRead()) {
                AskosiDataDir = askosiDataDestDirFile.getAbsolutePath();
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.askosidatadir",
                        new String[]{AskosiDataDir});
            } else {
                if (askosiDataDir == null) askosiDataDirAdd = true;
                if (AskosiDataDir != null) {
                    askosiDataDestDirFile = new File(AskosiDataDir);
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.askosidatadir", new String[]{
                            askosiDataDestDirFile.getAbsolutePath()});
                } else {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.askosidatadir", new String[]{""});
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

            out = new OutputStreamWriter(new FileOutputStream(dspaceDirConfNewFile, true));
            // añadir directiva de Askosi
            if (AskosiDataDir != null && askosiDataDirAdd) {
                String plugin = "\nASKOSI.directory = " + askosiDataDestDirFile.getAbsolutePath() + "\nplugin.named.org.dspace.content.authority.ChoiceAuthority = \\\n" + "be.destin.dspace.AskosiPlugin = ASKOSI\n";
                out.write("\n\n# " + getTime() + " Appended by installerEDM to add the ASKOSI plugin\n");
                out.write(plugin);
                modified = true;
            }

            // añadir los elementos dc de autoridad que todavía no están en dspace.cfg
            if (authDCElements.size() > 0) {

                for (MetadataField metadataField : authDCElements) {
                    String element = metadataField.getElement() + ((metadataField.getQualifier() != null)?"." + metadataField.getQualifier():"");
                    boolean isOk = false;
                    if (authDCElementsSetWritten.containsKey(element)) {
                        if (authDCElementsSetWritten.get(element).intValue() == 3) isOk = true;
                        else if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.element.incorrect",
                         new String[]{element});
                    }
                    if (!isOk) {
                        if (!writeDspaceCfg(out, element)) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.element.add.fail",
                                    new String[]{element});
                        } else {
                            modified = true;
                            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.element.add",
                                    new String[]{element});
                        }
                    }
                }
                if (modified) {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.messages");
                }
            } else if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configureDspaceCfg.nothing.add");
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return modified;
    }


    /**
     * Menú para elegir los elementos dc a controlar como vocabulario de autoridades
     *
     * @param authDCElements lista de elementos dc que son autoridad
     */
    private void addAuthDCElements(ArrayList<MetadataField> authDCElements)
    {
        while (true) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElements");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return;
            }
            if (response == null) break;
            if (response.isEmpty()) continue;
            response = response.trim();
            if (response.equalsIgnoreCase("a")) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElements.listauth");
                listAllDCElements(authDCElements);
                if (authDCElements.size() == 0) installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElements.notauthdcelements");
            } else if (response.equalsIgnoreCase("l")) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElements.listdc");
                listAllDCElements(metadataFields);
            } else if (response.equalsIgnoreCase("n")) {
                addAuthDCElement(authDCElements);
            } else if (response.equalsIgnoreCase("x")) {
                return;
            }
        }
    }


    /**
     * Menú para añadir elemento dc a la lista de elementos dc que son autoridades
     *
     * @param authDCElements lista de elementos dc que son autoridad
     */
    private void addAuthDCElement(ArrayList<MetadataField> authDCElements)
    {
        while (true) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElement");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return;
            }
            if (response == null) break;
            if (response.isEmpty()) continue;
            response = response.trim();
            if (response.equalsIgnoreCase("x")) {
                return;
            } else {
                ArrayList<MetadataField> listElementsObj = findElementsDC(response);
                if (listElementsObj == null) installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElement.element.notexist", new String[]{response});
                else {
                    for (MetadataField elementObj : listElementsObj) {
                        String element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");
                        if (elementsNotAuthSet.contains(element)) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "addAuthDCElement.element.notallowed", new String[]{element});
                            installerEDMDisplay.showLn();
                        } else {
                            boolean exists = false;
                            for (MetadataField metadataField : authDCElements) {
                                String element2 = metadataField.getElement() + ((metadataField.getQualifier() != null)?"." + metadataField.getQualifier():"");
                                if (element2.equals(element)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) authDCElements.add(elementObj);
                        }
                    }
                }
            }
        }
    }


    /**
     * Lee el archivo dspace.cfg para recoger el directori ode datos de Askosi y
     * los elementos dc ya configurados como vocabularios
     *
     * @return ruta del directorio de datos de Askosi
     * @throws FileNotFoundException
     * @throws IndexOutOfBoundsException
     */
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
                        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found.data", new String[] {line, dataDir});
                    }
                }
                if (!patternAskosiAuthPluginRead) {
                    Matcher matcherAskosiAuthPlugin = patternAskosiAuthPlugin.matcher(line);
                    if (matcherAskosiAuthPlugin.find()) {
                        patternAskosiAuthPluginRead = true;
                        String content = (String) matcherAskosiAuthPlugin.group(1);
                        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found.content", new String[] {line, content});
                        Matcher matcherAskosiAuthPluginBe = patternAskosiAuthPluginBe.matcher(content);
                        if (matcherAskosiAuthPluginBe.find()) {
                            patternAskosiAuthPluginBeRead = true;
                        }
                    }
                }
                if (!patternAskosiAuthPluginBeRead) {
                    Matcher matcherAskosiAuthPluginBe = patternAskosiAuthPluginBe.matcher(line);
                    if (matcherAskosiAuthPluginBe.find()) {
                        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found",
                                new String[]{line});
                        patternAskosiAuthPluginBeRead = true;
                    }
                }

                Matcher matcherPlugin = patternPlugin.matcher(line);
                if (matcherPlugin.find()) {
                    String element = (String) matcherPlugin.group(1);
                    if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found.element", new String[] {line, element});
                    if (authDCElementsSetWritten.containsKey(element)) {
                        int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                        authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                    } else authDCElementsSetWritten.put(element, new Integer(1));
                } else {
                    Matcher matcherPresentation = patternPresentation.matcher(line);
                    if (matcherPresentation.find()) {
                        String element = (String) matcherPresentation.group(1);
                        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found.element", new String[] {line, element});
                        if (authDCElementsSetWritten.containsKey(element)) {
                            int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                            authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                        } else authDCElementsSetWritten.put(element, new Integer(1));
                    } else {
                        Matcher matcherControlled = patternControlled.matcher(line);
                        if (matcherControlled.find()) {
                            String element = (String) matcherControlled.group(1);
                            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "readDspaceCfg.line.found.element", new String[] {line, element});
                            if (authDCElementsSetWritten.containsKey(element)) {
                                int valor = authDCElementsSetWritten.get(element).intValue() + 1;
                                authDCElementsSetWritten.put(element, Integer.valueOf(valor));
                            } else authDCElementsSetWritten.put(element, new Integer(1));
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


    /**
     * Añade nuevos elementos dc en dspace.cfg
     *
     * @param out flujo de datos para escribir en dspace.cfg
     * @param element elemento dc a añadir
     * @return éxito de la operación
     */
    private boolean writeDspaceCfg(Writer out, String element)
    {
        String choice = "\nchoices.plugin." + dcSchema.getName() + "." + element + " = ASKOSI\n" + "choices.presentation." + dcSchema.getName() + "." + element + " = lookup\n" + "authority.controlled." + dcSchema.getName() + "." + element + " = true";
        try {
            String element2 = "\n\n# " + getTime() + " Appended by installerEDM to add the " + dcSchema.getName() + "." + element + "\n";
            out.write(element2);
            out.write(choice);
        } catch (IOException e) {
            showException(e);
            return false;
        }
        return true;
    }


}
