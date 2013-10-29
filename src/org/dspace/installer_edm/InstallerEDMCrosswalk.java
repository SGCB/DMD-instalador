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


import org.apache.commons.io.FileUtils;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * @class InstallerEDMCrosswalk
 *
 * Clase para instalar el plugin de EDMCrosswalk de java en dspace para mostrar en OAI los ítems en formato EDM.
 *
 * Se copia el archivo EDMCrosswalk.java al directorio de trabajo del instalador, se piden algunos datos especiales para edm,
 * se cambian en el fuente del java, se compila y se genera un class que se añade al jar de la api de oai de dspace.
 * Se copia oaicat.properties al directorio de trabajo del instalador y se modifica para añadir el crosswalk para edm
 *
 */
public class InstallerEDMCrosswalk extends InstallerCrosswalk
{

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param crosswalkName nombre del crosswalk
     */
    public InstallerEDMCrosswalk(Integer currentStepGlobal, String crosswalkName)
    {
        super(currentStepGlobal.intValue(), crosswalkName);
    }


    /**
     * Configuración específica del fuente del crosswalk
     */
    @Override
    public void configureCrosswalk()
    {
        try {
            confEDMUgc();
            installerEDMDisplay.showLn();
            // configura edm.rights
            confEDMRights();
            installerEDMDisplay.showLn();
            // configura edm.types
            confEDMTypes();
        } catch (IOException e) {
            showException(e);
        }
    }


    /**
     * Busca en EDMCrosswalk.java la variable estática donde se introduce la true a edm:ugc
     * private static final String EDMUGC
     *
     * y la modifica para añadir true
     *
     * @throws IOException
     */
    private void confEDMUgc() throws IOException
    {
        final Pattern EDMUGC_PATTERN = Pattern.compile("private\\s+static\\s+final\\s+boolean\\s+EDMUGC\\s+=\\s+(.*);");
        String response = null;
        boolean change = false;
        do {
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmugc");
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
            if (response.isEmpty()) break;
            else if (response.equalsIgnoreCase(answerYes)) {
                change = true;
                break;
            } else if (response.equalsIgnoreCase("n")) break;
        } while (true);
        Matcher matcherEdmUgc = EDMUGC_PATTERN.matcher(edmCrossWalkContent);
        if (matcherEdmUgc.find() && matcherEdmUgc.groupCount() == 1) {
            String edmUgc = matcherEdmUgc.group(1);
            if (edmUgc != null && !edmUgc.isEmpty()) {
                edmCrossWalkContent = edmCrossWalkContent.replaceFirst("private\\s+static\\s+final\\s+boolean\\s+EDMUGC\\s+=\\s+" + edmUgc + ";",
                        "private static final boolean EDMUGC = " + ((change)?"true":"false") + ";");
            }
        }
    }


    /**
     * Busca en EDMCrosswalk.java la variable estática donde se introduce la url de edm.rights
     * private static final String EDMRIGHTS
     *
     * y la modifica para añadir la url escogida
     *
     * @throws IOException
     */
    private void confEDMRights() throws IOException
    {
        final Pattern EDMRIGHTS_PATTERN = Pattern.compile("private\\s+static\\s+final\\s+String\\s+EDMRIGHTS\\s+=\\s+\"(.*)\";");
        String response = null;
        do {
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmrights");
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
            if (!response.isEmpty() && isValidURI(response)) {
                break;
            }
        } while (true);
        Matcher matcherEdmRights = EDMRIGHTS_PATTERN.matcher(edmCrossWalkContent);
        if (matcherEdmRights.find() && matcherEdmRights.groupCount() == 1) {
            String edmRigths = matcherEdmRights.group(1);
            if (edmRigths != null && !edmRigths.isEmpty()) {
                String response2 = null;
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmrights.new", new String[]{ edmRigths, response });
                    response2 = br.readLine();
                    if (response2 == null) continue;
                    response2 = response2.trim();
                    if (!response2.isEmpty() && response2.equalsIgnoreCase(answerYes)) {
                        break;
                    } else return;
                } while (true);
            }
            edmCrossWalkContent = edmCrossWalkContent.replaceFirst("private\\s+static\\s+final\\s+String\\s+EDMRIGHTS\\s+=\\s+\"" + edmRigths + "\";",
                    "private static final String EDMRIGHTS = \"" + response + "\";");
        }
    }


    /**
     * Busca en EDMCrosswalk.java el bloque del hash de los tipos con palabras claves
     * para cada tipo encontrado se piden las palabras asociadas a él y se modifica el hash para añadirlas
     *
     * @throws IOException
     */
    private void confEDMTypes() throws IOException
    {
        // hash de tipos con conjunto de palabras
        Map<String, Set<String>> EDMTYPES = new HashMap<String, Set<String>>();
        // patrón de grupo de tipos
        final Pattern EDMTYPES_PATTERN = Pattern.compile("\\/\\/\\s+Begin\\s+Add\\s+EDMTYPES\\s+(EDMTYPES.get\\(\".+?\"\\).add\\(\".+?\"\\);\\s+)+\\s+\\/\\/\\s+End\\s+Add\\s+EDMTYPES");
        // patrón de añadir palabra a tipo
        final Pattern EDMTYPE_PATTERN = Pattern.compile("EDMTYPES.get\\(\"(.+?)\"\\).add\\(\"(.+?)\"\\);");

        // bloque del hash de los tipos
        Matcher matcherEdmTypes = EDMTYPES_PATTERN.matcher(edmCrossWalkContent);
        if (matcherEdmTypes.find() && matcherEdmTypes.groupCount() > 0) {
            String TypesMatchStr = matcherEdmTypes.group(0);

            // recorrer grupo para recoger asociación tipo-palabra
            Matcher matcherEdmType;
            matcherEdmType = EDMTYPE_PATTERN.matcher(TypesMatchStr);
            while (matcherEdmType.find() && matcherEdmType.groupCount() > 0) {
                try {
                    String type = matcherEdmType.group(1);
                    String value = matcherEdmType.group(2);
                    if (!EDMTYPES.containsKey(type)) EDMTYPES.put(type, new HashSet<String>());
                    if (!EDMTYPES.get(type).contains(value)) EDMTYPES.get(type).add(value);
                    TypesMatchStr = TypesMatchStr.substring(matcherEdmType.end(2), TypesMatchStr.length() - 1);
                    matcherEdmType = EDMTYPE_PATTERN.matcher(TypesMatchStr);
                } catch (IndexOutOfBoundsException e) {
                    showException(e);
                }
            }

            // recorrer tipos para mostrar palabras
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.exist");
            for (String type : EDMTYPES.keySet()) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type", new String[]{type});
                StringBuilder types = new StringBuilder();
                for (String value : EDMTYPES.get(type)) {
                    types.append(value).append(", ");
                }
                String strTypes = types.toString();
                installerEDMDisplay.showMessage(strTypes.substring(0, strTypes.length() - 2));
            }

            // preguntar qué tipo actualizar
            installerEDMDisplay.showLn();
            String response = null;
            do {
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type.update");
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (!response.isEmpty()) {
                    if (EDMTYPES.containsKey(response)) {
                        updateType(response, EDMTYPES);
                    }
                } else break;
            } while (true);

            // modificar el java con los nuevos valores tipo-palabras
            edmCrossWalkContent = edmCrossWalkContent.replaceAll("\\s*EDMTYPES\\.get\\(.+?\\)\\.add\\(.*?\\);\\s*", "");
            edmCrossWalkContent = edmCrossWalkContent.replaceFirst("\\/\\/\\s+End\\s+Add\\s+EDMTYPES", "\n        // End Add EDMTYPES");
            for (String type : EDMTYPES.keySet()) {
                for (String typeValue : EDMTYPES.get(type)) {
                    StringBuilder strb = new StringBuilder("\n        EDMTYPES.get(\"").append(type).append("\").add(\"").append(typeValue).append("\");\n");
                    strb.append("        // End Add EDMTYPES");
                    edmCrossWalkContent = edmCrossWalkContent.replaceFirst("\\s*\\/\\/\\s+End\\s+Add\\s+EDMTYPES", strb.toString());
                }
            }
        }

    }


    /**
     * Pregunta una lista de palabras separadas por "," para asociar a un tipo
     *
     * @param type cadena con el tipo a modificar
     * @param EDMTYPES hasn con la asociación tipo-palabras
     * @throws IOException
     */
    private void updateType(String type, Map<String, Set<String>> EDMTYPES) throws IOException
    {
        // recoger palabras actuales del tipo
        StringBuilder types = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types.append(value).append(",");
        }
        // mostrarlas
        String strTypes = types.toString();
        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type.value.update", new String[]{type, strTypes.substring(0, strTypes.length() - 1)});
        // preguntar nueva lista de palabras separadas por ","
        String response = null;
        do {
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
            // añadir lista al hash
            if (!response.isEmpty()) {
                String [] newTypes = response.split("\\s*,\\s*");
                EDMTYPES.get(type).clear();
                for (String newType: newTypes) {
                    if (!EDMTYPES.get(type).contains(newType)) EDMTYPES.get(type).add(newType);
                }
                break;
            } else break;
        } while (true);
        installerEDMDisplay.showLn();
        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type", new String[]{type});
        // mostrar nueva lista de palabras
        StringBuilder types2 = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types2.append(value).append(", ");
        }
        String strTypes2 = types2.toString();
        installerEDMDisplay.showMessage(strTypes2.substring(0, strTypes2.length() - 2));
        installerEDMDisplay.showLn();
    }

}


