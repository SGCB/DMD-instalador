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
 */


package org.dspace.installer_edm;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.Terminal;
import org.apache.commons.cli.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Observable;
import java.util.Observer;

public class InstallerEDM implements Observer
{
    private static String DspaceDir = null;
    private static String TomcatBase = null;
    private static boolean verbose = false;
    private static boolean isTerminal = false;
    private static int iniStep = 1;

    private static String AskosiDataDir;

    private static Terminal terminal;

    private static DSpaceKernelImpl kernelImpl;

    final private MyShutdown mySH;
    private MySignalHandler sh = null;

    private InstallerEDMAskosi installerEDMAskosi;
    private InstallerEDMCreateAuth installerEDMCreateAuth;


    public static void main(String[] args)
    {
        if (args.length < 1) {
            System.out.println("You must provide at least one command argument");
            System.exit(1);
        }

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

            String message = "Failure during filter init: " + e.getMessage();
            System.out.println(message + ":" + e);
            throw new IllegalStateException(message, e);
        }


        InstallerEDM installerEDM = new InstallerEDM();

        try {

            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("d", "dspace_dir", true, "DSpace installation directory");
            options.addOption("h", "help", false, "Help");
            options.addOption("m", "terminal", false, "Use lanterna library for display");
            options.addOption("s", "step", true, "Go to this installation step");
            options.addOption("t", "tomcat_base", true, "Tomcat Base directory");
            options.addOption("v", "verbose", false, "Verbosity");

            CommandLine line = parser.parse(options, args);

            if (line.hasOption('h')) {
                  installerEDM.HelpInstallerEDM(options);
            }

            if (line.hasOption('d')) {
                 DspaceDir = line.getOptionValue('d') + System.getProperty("file.separator");
            }
            if (line.hasOption('t')) {
                TomcatBase = line.getOptionValue('t') + System.getProperty("file.separator");
            }
            if (line.hasOption('s')) {
                iniStep = Integer.parseInt(line.getOptionValue('s').trim());
            }

            if (line.hasOption('m')) {
                isTerminal = true;
            }

            if (line.hasOption('v')) {
                verbose = true;
            }

            if (DspaceDir == null) {
                System.out.println("The DSpace installation directory is no defined!!");
                installerEDM.HelpInstallerEDM(options);
            }

            if (TomcatBase == null) {
                System.out.println("The Tomcat Base directory is no defined!!");
                installerEDM.HelpInstallerEDM(options);
            }

            installerEDM.installEDM();

        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        } finally {
            if (terminal != null) terminal.exitPrivateMode();
        }
    }

    public InstallerEDM()
    {
        sh = new MySignalHandler();
        sh.addObserver(this);
        sh.handleSignal("INT");

        mySH = new MyShutdown(this);
        Runtime.getRuntime().addShutdownHook(mySH);

        if (isTerminal) {
            terminal = TerminalFacade.createTerminal(Charset.forName("UTF8"));
            terminal.enterPrivateMode();
            //TerminalSize screenSize = terminal.getTerminalSize();
            terminal.clearScreen();
        }
    }


    private void installEDM()
    {
        installerEDMAskosi = new InstallerEDMAskosi(DspaceDir, TomcatBase, verbose);
        sh.addObserver( installerEDMAskosi );
        File dirPackage;
        if ((iniStep == 1 && (dirPackage = installerEDMAskosi.checkPackages()) != null && installerEDMAskosi.installPackages(dirPackage)) || iniStep > 1) {
            if (iniStep == 1) {
                System.out.println("");
                System.out.println("Askosi installed ok. Tomcat must be restarted after the end of the installation process.");
                iniStep++;
            }

            if (iniStep == 2) {
                installerEDMCreateAuth = new InstallerEDMCreateAuth(DspaceDir, TomcatBase, verbose);
                sh.addObserver( installerEDMCreateAuth );
                installerEDMCreateAuth.createAuth();
            }
        } else {
            System.out.println("");
            System.out.println("Askosi installation failed.");
        }
    }

    private void HelpInstallerEDM(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("InstallerEDM\n", options);
        System.exit(0);
    }

    static public void setAskosiDataDir(String askosiDataDir)
    {
        InstallerEDM.AskosiDataDir = AskosiDataDir;
    }

    static public Terminal getTerminal()
    {
        return terminal;
    }

    static public boolean getIsTerminal()
    {
        return isTerminal;
    }

    static public boolean getVerbose()
    {
        return verbose;
    }


    static public DSpaceKernelImpl getKernelImpl()
    {
        return kernelImpl;
    }

    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}



class MyShutdown extends Thread
{
    private InstallerEDM installerEDM;

    public MyShutdown(InstallerEDM installerEDM)
    {
        this.installerEDM = installerEDM;
    }

    @Override public void run()
    {
        if (InstallerEDM.getVerbose()) System.out.println("MyShutdown hook called");
        Terminal terminal = installerEDM.getTerminal();
        if (terminal != null) terminal.exitPrivateMode();
        DSpaceKernelImpl kernelImpl = InstallerEDM.getKernelImpl();
        if (kernelImpl != null) kernelImpl.destroy();
    }
}
