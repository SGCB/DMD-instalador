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

/**
 * @class InstallerESECrosswalk
 *
 * Clase para instalar el plugin de ESECrosswalk de java en dspace para mostrar en OAI los ítems en formato ESE.
 *
 * Se copia el archivo ESECrosswalk.java al directorio de trabajo del instalador,
 * se cambian en el fuente del java, se compila y se genera un class que se añade al jar de la api de oai de dspace.
 * Se copia oaicat.properties al directorio de trabajo del instalador y se modifica para añadir el crosswalk para ese
 *
 */
public class InstallerESECrosswalk extends InstallerCrosswalk
{

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param crosswalkName nombre del crosswalk
     */
    public InstallerESECrosswalk(Integer currentStepGlobal, String crosswalkName)
    {
        super(currentStepGlobal.intValue(), crosswalkName);
    }

    @Override
    public void configureCrosswalk()
    {

    }
}
