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
 * @class InstallerEDMDisplay
 *
 * Interfaz para mostrar los mensajes de los archivos messages
 *
 */
public interface InstallerEDMDisplay
{

    /**
     * muestra el título de un paso. En el archivo la propiedad está como: stage=
     *
     * @param stage paso actual
     */
    public void showTitle(int stage);

    /**
     * muestra el menú de un paso. En el archivo la propiedad está como: stage.menu=
     *
     * @param stage paso actual
     */
    public void showMenu(int stage);

    /**
     * recoge el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @return mensaje
     */
    public String getQuestion(int stage, String code);

    /**
     * recoge el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     * @return mensaje
     */
    public String getQuestion(int stage, String code, String[] args);

    /**
     * muestra el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     */
    public void showQuestion(int stage, String code, String[] args);

    /**
     * muestra el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     */
    public void showQuestion(int stage, String code);

    /**
     * muestra el mensaje pasado como argumento
     *
     * @param message mensaje a mostrar
     */
    public void showMessage(String message);

    /**
     * muestra un salto de línea
     */
    public void showLn();

    /**
     * muestra progreso de una acción
     *
     * @param prog carácter a mostrar por cada iteración
     */
    public void showProgress(char prog);

    /**
     * recarga el archivo de mensajes para un idioma
     *
     * @param language idioma del archivo
     */
    public void reloadFileDisplayMessages(String language);

}
