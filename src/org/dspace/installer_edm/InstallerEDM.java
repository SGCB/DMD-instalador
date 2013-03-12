/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 22/10/12
 * Time: 17:40
 * To change this template use File | Settings | File Templates.
 *
 * cd /home/europeana/instalador_edm
 * jar cf out/production/instalador_edm/InstallerEDM.jar out/production/instalador_edm/org/dspace/installer_edm/*.class
 * JARS=$(echo /home/europeana/instalador_edm/lib/*.jar | sed 's/ /\:/g'); JARS2=$(echo /home/europeana/runtime/lib/*.jar | sed 's/ /\:/g'); java -cp $JARS:$JARS2:out/production/instalador_edm/InstallerEDM.jar:out/production/instalador_edm:/home/europeana/runtime/config org.dspace.installer_edm.InstallerEDM -d /home/europeana/runtime/ -t /usr/share/tomcat5.5/ -v -s 2
 *
 * JARS=$(echo /home/salzaru/Download/instalador_edm/lib/*.jar | sed 's/ /\:/g'); JARS2=$(echo /home/salzaru/Download/dspace/lib/*.jar | sed 's/ /\:/g'); java -cp $JARS:$JARS2:out/production/instalador_edm/InstallerEDM.jar:out/production/instalador_edm:/home/salzaru/Download/dspace/config org.dspace.installer_edm.InstallerEDM -d /home/salzaru/Download/dspace/ -t /usr/local/apache-tomcat-6.0/ -v -s 2
 *
 * ant jar && ./install.sh -d /home/europeana/runtime/ -t /usr/share/tomcat5.5 -v -s 2
 */


package org.dspace.installer_edm;

import org.apache.commons.cli.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @class InstallerEDM
 *
 * Clase principal que es invicada con la ejecución del jar.
 * Recoge los argumentos y ejecuta los pasos del instalador.
 * Muestra el menú principal con los pasos.
 * hereda de la clase base {@link InstallerEDMBase} con las variables estáticas comunes y métodos comunes.
 *
 */
public class InstallerEDM extends InstallerEDMBase
{
    /**
     * Número del paso que viene como parámetro en el jar
     */
    private static int iniStep = 0;

    /**
     * Kernel de dspace que se incia en esta clase
     */
    private static DSpaceKernelImpl kernelImpl;

    /**
     * Clase {@link MyShutdown} para gestionar el cierre de la aplicación por la captura de una señal de terminación
     */
    final private MyShutdown mySH;

    /**
     * Clase {@link MySignalHandler} que captura las señales
     */
    private MySignalHandler sh = null;

    /**
     * Clase {@link InstallerEDMAskosi} para instalar el servicio de Askosi
     */
    private InstallerEDMAskosi installerEDMAskosi;

    /**
     * Clase {@link InstallerEDMCreateAuth} para crear las autoridades en dspace
     */
    private InstallerEDMCreateAuth installerEDMCreateAuth = null;

    /**
     * Clase {@link InstallerEDMConf} para configurar dspace con Askosi
     */
    private InstallerEDMConf installerEDMConf = null;

    /**
     * Clase {@link InstallerEDMConfEDMExport} para configurar el servicio EDMExport
     */
    private InstallerEDMConfEDMExport installerEDMConfEDMExport = null;

    /**
     * Clase {@link InstallerEDMCrosswalk} para configurar el plugin en java de OAI para mostrar elementos en formato EDM
     */
    private InstallerEDMCrosswalk installerEDMCrosswalk = null;

    /**
     * Clase {@link InstallerEDMCrosswalkXSL} para configurar el plugin en XSL de OAI para mostrar elementos en formato EDM
     */
    private InstallerEDMCrosswalkXSL installerEDMCrosswalkXSL = null;

    /**
     * Clase {@link InstallerEDMFillItems} para enlazar los items de dspace con las autoridades creadas
     */
    private InstallerEDMFillItems installerEDMFillItems = null;


    /**
     *
     * Llamada principal del jar.
     * Se inicia el kernel de dspace
     * Se recogen los parámetros pasadaos al jar
     * Se llama al mostrar el menú principal
     *
     * @param args argumentos pasados al jar
     */
    public static void main(String[] args)
    {
        // Crea la clase principal del instalador
        InstallerEDM installerEDM = new InstallerEDM();

        // Ha de existir parámetros
        if (args.length < 1) {
            installerEDM.getInstallerEDMDisplay().showQuestion(0, "error.commands");
            System.exit(1);
        }

        // Se incia el kernel de dspace
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning()) {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
            }
        } catch (Exception e) {
            try {
                kernelImpl.destroy();
            } catch (Exception e1) {
            }

            String message = installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.fail.init", new String[]{e.getMessage()});
            installerEDM.getInstallerEDMDisplay().showMessage(message + ":" + e);
            throw new IllegalStateException(message, e);
        }



        // Se recogen los parámetros
        try {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("d", "dspace_dir", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "dspace_dir.option"));
            options.addOption("h", "help", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "help.option"));
            options.addOption("g", "debug", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "debug.option"));
            options.addOption("l", "language", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "language.option"));
            options.addOption("m", "terminal", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "terminal.option"));
            options.addOption("s", "step", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "step.option"));
            options.addOption("t", "tomcat_base", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "tomcat_base.option"));
            options.addOption("v", "verbose", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "verbose.option"));

            CommandLine line = parser.parse(options, args);

            // mostrar la ayuda
            if (line.hasOption('h')) {
                  installerEDM.HelpInstallerEDM(options);
            }

            // directorio donde está ubicado el dspace desplegado
            if (line.hasOption('d')) {
                 installerEDM.setDspaceDir(line.getOptionValue('d') + System.getProperty("file.separator"));
            }

            // directorio base del servidor de servlets (Tomcat)
            if (line.hasOption('t')) {
                installerEDM.setTomcatBase(line.getOptionValue('t') + System.getProperty("file.separator"));
            }

            // paso que ejecutar
            if (line.hasOption('s')) {
                iniStep = Integer.parseInt(line.getOptionValue('s').trim());
            }

            // idioma con el que trabajar los elementos dc y los mensajes a mostrar. Se soporta es y en
            if (line.hasOption('l')) {
                installerEDM.setLanguage(line.getOptionValue('l').trim());
                installerEDMDisplay.reloadFileDisplayMessages(language);
            }

            // terminal con el que mostrar los mensajes.
            if (line.hasOption('m')) {
                installerEDM.getInstallerEDMDisplay().setIsTerminal(true);
            }

            // debug, muestra mucha información
            if (line.hasOption('g')) {
                installerEDM.setDebug(true);
            }

            // verbose, muesta más información
            if (line.hasOption('v')) {
                installerEDM.setVerbose(true);
            }

            // ha de tener directorio de dspace
            if (installerEDM.DspaceDir == null) {
                installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.dspace.dir");
                installerEDM.HelpInstallerEDM(options);
            }

            // ha de tener directorio de tomcat
            if (installerEDM.TomcatBase == null) {
                installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.tomcat.base");
                installerEDM.HelpInstallerEDM(options);
            }

            // llamar al menú de pasos
            installerEDM.installEDM();

        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        } finally {
            installerEDM.finishInstaller();
        }
    }

    /**
     * Constructor de la clase que muestra el menú principal y lanza los pasos
     */
    public InstallerEDM()
    {
        super();
        setInstallerEDMBase(this);

        // captura la señal de terminación
        sh = new MySignalHandler();
        sh.addObserver(this);
        sh.handleSignal("INT");

        mySH = new MyShutdown(this);
        Runtime.getRuntime().addShutdownHook(mySH);
    }


    /**
     * Se sale limpiamente del terminal
     */
    public void finishInstaller()
    {
        try {
            ((InstallerEDMDisplayImpl) installerEDMDisplay).exitTerminal();
        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        }
    }


    /**
     * Se recogen los pasos que existe  de los fichero messages
     * Se recogen los elementos DC en dspace
     * Se llama al menú principal
     */
    private void installEDM()
    {
        try {
            String[] stepsDisplay = installerEDMDisplay.getQuestion(0, "steps").split(",");
            for (String stepDisplay: stepsDisplay) {
                stepsSet.add(Integer.parseInt(stepDisplay.trim()));
            }
            checkDspaceMetadataDC();
        } catch (Exception e) {
            showException(e);
            finishInstaller();
        }
        installerEDMAskosi = new InstallerEDMAskosi(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.askosi")));
        sh.addObserver( installerEDMAskosi );
        installEDM(iniStep);
    }


    /**
     * Se muestra el menúr principal y se recoge el paso que se introduce para lanzarlo
     *
     * @param step paso a lanzar
     */
    private void installEDM(int step)
    {
        File dirPackage;

        // se ha elegido un paso correcto
        if (step > 0) {

            // instalar Askosi
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.askosi"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                if ((dirPackage = installerEDMAskosi.checkPackages()) != null && installerEDMAskosi.installPackages(dirPackage)) {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(step, "ok");
                } else {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(step, "fail");
                }
            }

            // configurar dspace para usar Askosi
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.conf_dspace"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMConf = new InstallerEDMConf(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.conf_dspace")));
                installerEDMConf.configureAll("dspace.cfg");
            }

            // crear las autoridades en dspace
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.auth_item"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMCreateAuth = new InstallerEDMCreateAuth(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.auth_item")));
                sh.addObserver( installerEDMCreateAuth );
                if (!installerEDMCreateAuth.configure()) {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(step, "fail");
                }
            }

            // configurar los input-form.xml para las autoridades y Askosi
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.input_forms"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMConf = new InstallerEDMConf(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.input_forms")));
                installerEDMConf.configureAll("");
            }

            // configurar el servicio EDMExport
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmexport"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMConfEDMExport = new InstallerEDMConfEDMExport(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmexport")),
                        myInstallerDirPath + fileSeparator + "packages" + fileSeparator + "EDMExport.war");
                installerEDMConfEDMExport.configure();
            }

            // configurar el plugin en java del oai para EDM
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmcrosswalk"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMCrosswalk = new InstallerEDMCrosswalk(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmcrosswalk")),
                        myInstallerDirPath + fileSeparator + "packages" + fileSeparator + "EDMCrosswalk.java");
                installerEDMCrosswalk.configure();
            }

            // configurar el plugin en xsl del oai para EDM
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmcrosswalkxsl"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMCrosswalkXSL = new InstallerEDMCrosswalkXSL(Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.edmcrosswalkxsl")));
                installerEDMCrosswalkXSL.configure();
            }

            // enlazar los items de dspace con las autoridades
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.fillitems"))) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showTitle(step);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(step, "summary");
                installerEDMDisplay.showLn();
                if (!proceed()) {
                    installEDM(0);
                    return;
                }
                installerEDMFillItems = new InstallerEDMFillItems(step);
                installerEDMFillItems.configure();
            }

            // salir
            if (step == Integer.parseInt(installerEDMDisplay.getQuestion(0, "step.exit"))) {
                System.exit(0);
            } else {
                installEDM(0);
                return;
            }
        } else {
            // bucle para esperar un paso correcto
            installerEDMDisplay.showLn();
            installerEDMDisplay.showLn();
            installerEDMDisplay.showTitle(0);
            while (true) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showMenu(0);
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(0, "option.step");
                String response = null;
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    showException(e);
                }
                if ((response != null) && !response.isEmpty() && stepsSet.contains(Integer.decode(response))) {
                    response = response.trim();
                    installEDM(Integer.parseInt(response));
                }
            }
        }
    }

    /**
     * Pregunta para confirmar lanzamiento del paso
     *
     * @return si se procede con el paso elegido
     */
    private boolean proceed()
    {
        String response = null;
        while (true) {
            installerEDMDisplay.showQuestion(0, "proceed");
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
            }
            if (response == null) continue;
            response = response.trim();
            if (!response.isEmpty()) {
                if (response.equalsIgnoreCase("n")) {
                    return false;
                } else if (response.equalsIgnoreCase(answerYes)) break;
            } else break;
        }
        return true;
    }

    /**
     * Mostrar la ayuda
     *
     * @param options parámetros del jar
     */
    private void HelpInstallerEDM(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("InstallerEDM\n", options);
        System.exit(0);
    }


    /**
     * Devuelve la verbosidad
     *
     * @return verbosidad
     */
    public boolean getVerbose()
    {
        return verbose;
    }


    /**
     * Devuelve el kernel de dspace
     *
     * @return kernel de dspace
     */
    static public DSpaceKernelImpl getKernelImpl()
    {
        return kernelImpl;
    }


    /**
     * Devuelve la clase que instala Askosi {@link InstallerEDMAskosi}
     *
     * @return clase que instala Askosi
     */
    public InstallerEDMAskosi getInstallerEDMAskosi()
    {
        return installerEDMAskosi;
    }


    /**
     * Devuelve la clase que crea las autoridades {@link InstallerEDMCreateAuth}
     *
     * @return clase que crea las autoridades
     */
    public InstallerEDMCreateAuth getInstallerEDMCreateAuth()
    {
        return installerEDMCreateAuth;
    }


    /**
     * Devuelve la clase que configura dspace para usar Askosi {@link InstallerEDMConf}
     *
     * @return clase que configura dspace para usar Askosi
     */
    public InstallerEDMConf getInstallerEDMConf()
    {
        return installerEDMConf;
    }

}


/**
 *
 */
class MyShutdown extends Thread
{
    private InstallerEDM installerEDM;

    /**
     *
     * @param installerEDM
     */
    public MyShutdown(InstallerEDM installerEDM)
    {
        this.installerEDM = installerEDM;
    }

    /**
     *
     */
    @Override public void run()
    {
        if (installerEDM.getVerbose()) installerEDM.getInstallerEDMDisplay().showQuestion(0, "shutdown.hook");
        DSpaceKernelImpl kernelImpl = InstallerEDM.getKernelImpl();
        if (kernelImpl != null) kernelImpl.destroy();
        installerEDM.finishInstaller();
    }
}
