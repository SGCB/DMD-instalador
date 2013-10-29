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


import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * @class InstallerManagerCrosswalk
 *
 * Clase para pedir el formato deseado a instalar como crosswalk
 *
 * Se buscan los crosswalk existentes en packages, se pide el deseado y se instancia la clase
 *
 */
public class InstallerManagerCrosswalk extends InstallerEDMBase
{

    private Map<String, File> crosswalks;

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerManagerCrosswalk(int currentStepGlobal)
    {
        super(currentStepGlobal);
        crosswalks = new HashMap<String, File>();
    }


    /**
     * Se buscan los crosswalk en el directorio packages, se pide el deseado y se instancia la clase.
     * Se llama a su configuración
     * @return
     */
    public void configure()
    {
        String suffix = "Crosswalk.java";
        String dirPackages = myInstallerDirPath + fileSeparator + "packages" + fileSeparator;
        File dir = new File(dirPackages);
        String[] files = dir.list(new SuffixFileFilter(suffix));
        for (int i = 0; i < files.length; i++) {
            String crosswalk = files[i].replaceFirst(suffix, "");
            File fileCrosswalk = new File(dirPackages + files[i]);
            if (fileCrosswalk.canRead()) {
                crosswalks.put(crosswalk, fileCrosswalk);
            }
        }
        if (!crosswalks.isEmpty()) {
            String crosswalk = chooseCrosswalk();
            if (crosswalk == null) return;
            String installerCrosswalk = "org.dspace.installer_edm.Installer" + crosswalk + "Crosswalk";
            try {
                Class installerCrosswalkClass = Class.forName(installerCrosswalk);
                Constructor ctor = installerCrosswalkClass.getDeclaredConstructor(new Class[]{Integer.class, String.class});
                Object crosswalkClass = (InstallerCrosswalk) ctor.newInstance(new Integer(currentStepGlobal), crosswalk);
                ((InstallerCrosswalk) crosswalkClass).configure();
            } catch (ClassNotFoundException e) {
                showException(e);
            } catch (InstantiationException e) {
                showException(e);
            } catch (IllegalAccessException e) {
                showException(e);
            } catch (InvocationTargetException e) {
                showException(e);
            } catch (NoSuchMethodException e) {
                showException(e);
            }
        }
    }


    /**
     * Se muestran los crosswalk existentes y se pide uno
     *
     * @return cadena con el formato deseado
     */
    public String chooseCrosswalk()
    {
        SortedSet<String> crosswalkNames = new TreeSet<String>(crosswalks.keySet());
        StringBuilder listCrosswalks = new StringBuilder();
        for (String cw : crosswalkNames) {
            listCrosswalks.append(cw).append(", ");
        }
        String finalListCrosswalks = listCrosswalks.toString();
        finalListCrosswalks = finalListCrosswalks.substring(0, finalListCrosswalks.length() - 2);
        while (true) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "chooseCrosswalk", new String[] {finalListCrosswalks});
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return null;
            }
            if (response == null || response.isEmpty()) continue;
            response = response.trim();
            if (!crosswalks.containsKey(response)) continue;
            return response;
        }
    }


}
