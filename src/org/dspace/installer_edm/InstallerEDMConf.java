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
 * @class InstallerEDMConf
 *
 * Clase para lanzar la configuración de los archivos de dspace para poder usar askosi
 * Extiende la clase {@link InstallerEDMBase}
 *
 */
public class InstallerEDMConf extends InstallerEDMBase implements Observer
{

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerEDMConf(int currentStepGlobal)
    {
        super(currentStepGlobal);
    }


    /**
     * Configurar el archivo de dspace de dspace.cfg para activar el plugin de askosi para jspui y decir qué
     * elementos serán controlados mediaten vocabularios de askosi.
     *
     * Configurar el archivo de dspace input-forms.xml para poder recoger datos de los repositorios de askosi
     * en los elementos reseñados cuando se cataloga.
     * Configurar los vocabularios en el directorio de datos de askosi para los elementos dc que son autoridades.
     * Se crean consultas a la base de datos de dspace para buscar los ítems en las comunidades relacionadas con
     * los elementos dc de autoridades.
     *
     * @param typeConfiguration tipo de configuración: dspace.cfg o distinto para input-forms
     * @return si ha ido bien
     */
    public boolean configureAll(String typeConfiguration)
    {
        // recoger dspcae.cfg de dspace
        String dspaceDirConfName = DspaceDir + "config" + fileSeparator + "dspace.cfg";
        File dspaceDirConfFile = new File(dspaceDirConfName);
        File dspaceDirConfNewFile = new File(myInstallerWorkDirPath);

        if (dspaceDirConfFile.exists() && dspaceDirConfFile.canRead() && dspaceDirConfNewFile.canWrite()) {

            // recoger los elementos dc que son autoridades creadas en el paso de su creación o lo inicia vacío
            ArrayList<MetadataField> authDCElements;
            if (installerEDM.getInstallerEDMCreateAuth() != null) {
                authDCElements = installerEDM.getInstallerEDMCreateAuth().getAuthDCElements();
            } else {
                authDCElements = new ArrayList<MetadataField>();
                initAuthBOHashMap();
            }
            try {
                // busca en todas las colecciones los elementos dc que son de autoridades
                checkAllSkosAuthElements(authDCElements);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.listAuth");
                // lista todos los elementos dc que pueden ser autoridades
                listAllDCElements(authDCElements);
                installerEDMDisplay.showLn();

                // configurar dspace.cfg
                if (typeConfiguration.equals("dspace.cfg")) {
                    if (configureDspaceCfg(dspaceDirConfFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "dspace.cfg"), authDCElements)) {
                        installerEDMDisplay.showLn();
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.restart", new String[]{myInstallerWorkDirPath});
                    }
                } else {

                    // si hay elementos dc de autoridades
                    if (authDCElements.size() > 0) {

                        // recoge input-forms.xml
                        String dspaceInputFormsName = DspaceDir + "config" + fileSeparator + "input-forms.xml";
                        File dspaceInputFormsFile = new File(dspaceInputFormsName);

                        if (dspaceInputFormsFile.exists() && dspaceInputFormsFile.canRead()) {

                            // configura input-forms.xml
                            if (configureInputFormsDspace(dspaceInputFormsFile, new File(dspaceDirConfNewFile.getAbsolutePath() + fileSeparator + "input-forms.xml"), authDCElements)) {

                                installerEDMDisplay.showLn();
                                installerEDMDisplay.showQuestion(currentStepGlobal,
                                        "configureAll.askosiVocabularies");
                                File askosiDataDirFile = null;

                                // no hay directorio de datos de askois, se pide uno
                                if (AskosiDataDir == null) {
                                    String response = null;
                                    do {
                                        installerEDMDisplay.showQuestion(currentStepGlobal, "configureAll.AskosiDataDir");
                                        response = br.readLine();
                                        if (response == null || response.length() == 0) continue;
                                        response = response.trim();
                                        askosiDataDirFile = new File(response);
                                        if (askosiDataDirFile.exists()) {
                                            AskosiDataDir = askosiDataDirFile.getAbsolutePath();
                                            break;
                                        }
                                    } while (true);
                                }
                                if (AskosiDataDir != null) {
                                    if (askosiDataDirFile == null) askosiDataDirFile = new File(AskosiDataDir);
                                    if (askosiDataDirFile.exists() && askosiDataDirFile.isDirectory() && askosiDataDirFile.canWrite()) {

                                        // se configuran los vocabularios de los elementos dc de autoridades
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


    /**
     * Configura las consultas para los vocabularios de las autoridades
     * Crea un objeto {@link InstallerEDMAskosiVocabularies}
     *
     * @param askosiDataDirFile directorio de datos de askosi
     * @throws IOException
     */
    private void configureAskosiVocabularies(File askosiDataDirFile) throws IOException
    {
        InstallerEDMAskosiVocabularies installerEDMAskosiVocabularies = new InstallerEDMAskosiVocabularies(currentStepGlobal, askosiDataDirFile);
        installerEDMAskosiVocabularies.processAskosiVocabularies();
    }

    /**
     * Configura el input-forms.xml para la catalogación mediante vocabularios de askosi
     * Crea un objeto {@link InstallerEDMInputForms}
     *
     * @param dspaceInputFormsFile archivo input-forms.xml de dspace
     * @param dspaceInputFormsNewFile archivo input-forms.xml del directorio de trabajo
     * @param authDCElements lista de elementos dc de autoridades
     * @return éxito de la operación
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     * @throws SQLException
     */
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


    /**
     * Configura dspace.cfg para añadir el plugin de askosi y los elementos dc que serán controlados por vocabularios de askosi
     * Crea un objeto {@link InstallerEDMDspaceCfg}
     *
     * @param dspaceDirConfFile archivo dspace.cfg de dspace
     * @param dspaceDirConfNewFile archivo dspace.cfg del directorio de trabajo
     * @param authDCElements lista de elementos dc de autoridades
     * @return éxito de la operación
     * @throws IndexOutOfBoundsException
     * @throws IOException
     * @throws NullPointerException
     */
    private boolean configureDspaceCfg(File dspaceDirConfFile, File dspaceDirConfNewFile, ArrayList<MetadataField> authDCElements) throws IndexOutOfBoundsException, IOException, NullPointerException
    {
        copyDspaceFile2Work(dspaceDirConfFile, dspaceDirConfNewFile, "configureDspaceCfg.dspacecfg");
        InstallerEDMDspaceCfg installerEDMDspaceCfg = new InstallerEDMDspaceCfg(currentStepGlobal, dspaceDirConfNewFile);
        return installerEDMDspaceCfg.processDspaceCfg(authDCElements);
    }


    /**
     * Cerrar aplicación tras recibir señal
     *
     * @param o observable
     * @param arg señal
     */
    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
