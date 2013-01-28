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

public class InstallerEDM extends InstallerEDMBase
{
    private static int iniStep = 0;

    private static DSpaceKernelImpl kernelImpl;

    final private MyShutdown mySH;
    private MySignalHandler sh = null;

    private InstallerEDMAskosi installerEDMAskosi;
    private InstallerEDMCreateAuth installerEDMCreateAuth = null;
    private InstallerEDMConf installerEDMConf = null;


    public static void main(String[] args)
    {
        InstallerEDM installerEDM = new InstallerEDM();

        if (args.length < 1) {
            installerEDM.getInstallerEDMDisplay().showQuestion(0, "error.commands");
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

            String message = installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.fail.init") + e.getMessage();
            installerEDM.getInstallerEDMDisplay().showMessage(message + ":" + e);
            throw new IllegalStateException(message, e);
        }




        try {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("d", "dspace_dir", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "dspace_dir.option"));
            options.addOption("h", "help", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "help.option"));
            options.addOption("m", "terminal", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "terminal.option"));
            options.addOption("s", "step", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "step.option"));
            options.addOption("t", "tomcat_base", true, installerEDM.getInstallerEDMDisplay().getQuestion(0, "tomcat_base.option"));
            options.addOption("v", "verbose", false, installerEDM.getInstallerEDMDisplay().getQuestion(0, "verbose.option"));

            CommandLine line = parser.parse(options, args);

            if (line.hasOption('h')) {
                  installerEDM.HelpInstallerEDM(options);
            }

            if (line.hasOption('d')) {
                 installerEDM.setDspaceDir(line.getOptionValue('d') + System.getProperty("file.separator"));
            }
            if (line.hasOption('t')) {
                installerEDM.setTomcatBase(line.getOptionValue('t') + System.getProperty("file.separator"));
            }
            if (line.hasOption('s')) {
                iniStep = Integer.parseInt(line.getOptionValue('s').trim());
            }

            if (line.hasOption('m')) {
                installerEDM.getInstallerEDMDisplay().setIsTerminal(true);
            }

            if (line.hasOption('v')) {
                installerEDM.setVerbose(true);
            }

            if (installerEDM.DspaceDir == null) {
                installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.dspace.dir");
                installerEDM.HelpInstallerEDM(options);
            }

            if (installerEDM.TomcatBase == null) {
                installerEDM.getInstallerEDMDisplay().getQuestion(0, "error.tomcat.base");
                installerEDM.HelpInstallerEDM(options);
            }

            installerEDM.installEDM();

        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        } finally {
            installerEDM.finishInstaller();
        }
    }

    public InstallerEDM()
    {
        super();
        setInstallerEDMBase(this);
        sh = new MySignalHandler();
        sh.addObserver(this);
        sh.handleSignal("INT");

        mySH = new MyShutdown(this);
        Runtime.getRuntime().addShutdownHook(mySH);
    }


    public void finishInstaller()
    {
        try {
            ((InstallerEDMDisplayImpl) installerEDMDisplay).exitTerminal();
        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        }
    }


    private void installEDM()
    {
        try {
            String[] stepsDisplay = installerEDMDisplay.getQuestion(0, "steps").split(",");
            for (String stepDisplay: stepsDisplay) {
                stepsSet.add(Integer.parseInt(stepDisplay.trim()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            finishInstaller();
        }
        installerEDMAskosi = new InstallerEDMAskosi();
        sh.addObserver( installerEDMAskosi );
        installEDM(iniStep);
    }

    private void installEDM(int step)
    {
        File dirPackage;
        if (step > 0) {
            if (step == 1) {
                if (verbose) installerEDMDisplay.showTitle(1);
                if ((dirPackage = installerEDMAskosi.checkPackages()) != null && installerEDMAskosi.installPackages(dirPackage)) {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(1, "ok");
                    iniStep++;
                } else {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(1, "fail");
                }
            }

            if (step == 2) {
                if (verbose) installerEDMDisplay.showTitle(2);
                installerEDMCreateAuth = new InstallerEDMCreateAuth();
                sh.addObserver( installerEDMCreateAuth );
                if (installerEDMCreateAuth.createAuth()) iniStep++;
                else {
                    installerEDMDisplay.showLn();
                    installerEDMDisplay.showQuestion(2, "fail");
                }
            }

            if (step == 3) {
                if (verbose) installerEDMDisplay.showTitle(3);
                installerEDMConf = new InstallerEDMConf();
                installerEDMConf.configureAll();
            }
        } else {
            installerEDMDisplay.showTitle(0);
            while (true) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showMenu(0);
                String response = null;
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if ((response != null) && !response.isEmpty() && stepsSet.contains(Integer.decode(response))) {
                    response = response.trim();
                    installEDM(Integer.parseInt(response));
                }
            }
        }
    }

    private void HelpInstallerEDM(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("InstallerEDM\n", options);
        System.exit(0);
    }


    public boolean getVerbose()
    {
        return verbose;
    }


    static public DSpaceKernelImpl getKernelImpl()
    {
        return kernelImpl;
    }


    public InstallerEDMAskosi getInstallerEDMAskosi()
    {
        return installerEDMAskosi;
    }


    public InstallerEDMCreateAuth getInstallerEDMCreateAuth()
    {
        return installerEDMCreateAuth;
    }


    public InstallerEDMConf getInstallerEDMConf()
    {
        return installerEDMConf;
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
        if (installerEDM.getVerbose()) installerEDM.getInstallerEDMDisplay().showQuestion(0, "shutdown.hook");
        DSpaceKernelImpl kernelImpl = InstallerEDM.getKernelImpl();
        if (kernelImpl != null) kernelImpl.destroy();
        installerEDM.finishInstaller();
    }
}
