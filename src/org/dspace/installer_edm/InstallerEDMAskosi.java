package org.dspace.installer_edm;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 24/10/12
 * Time: 13:08
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMAskosi extends InstallerEDMBase implements Observer
{
    private final String[] packages = {"ASKOSI.jar", "askosiWebapp.zip", "classes.zip", "commons-dbcp.jar", "commons-pool.jar", "exampleAskosiData.zip", "jaxb-xalan-1.5.jar", "jsr311-api-1.1.1.jar", "jstl-1.2.jar", "log4j.jar", "openrdf-alibaba-2.0-beta6.jar", "openrdf-sesame-2.3.2-onejar.jar"};
    private final String[] packagesMD5 = {"f800262e9587383fa0dbd8f748cc831e", "ab932907d73a8031cb266d20d341a6e2", "0bffffb990ea99eb02a989d346454d8e", "2666cfeb7be74b1c2d8a1665ae21192c", "01f9bed60e2f88372132d34040ee81bb", "2be860d3a2529cb8789d6c27cfae5a92", "261968cebe30ffe8adcc201ad0bfa395", "c9803468299ec255c047a280ddec510f", "51e15f798e69358cb893e38c50596b9b", "599b8ba07d1d04f0ea34414e861d7ad1", "1f699edb215bcee75cb6f0616fa56993", "3054aa9109f78903852d38991b5a4ea8"};

    private File finalAskosiDataDestDirFile = null;



    public InstallerEDMAskosi(InstallerEDM installerEDM, String DspaceDir, String TomcatBase, boolean verbose)
    {
        super(installerEDM, DspaceDir, TomcatBase, verbose);
    }

    public boolean installPackages(File dirPackages)
    {
        System.out.println("");
        if (verbose) System.out.println("Beginning copy of jar's needed by Askosi");
        if (copyJarsShared(dirPackages)) {
            System.out.println("");
            System.out.println("Jar files copied ok");

            System.out.println("");
            if (verbose) System.out.println("Beginning copy of database driver");
            if (!copyDatabaseDrivers()) return false;

            int currentStep = 0;
            Iterator<File> fileIterPackZip = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"zip"}, false);
            while (fileIterPackZip.hasNext()) {
                File sourcePackageFile = fileIterPackZip.next();

                if (sourcePackageFile.getName().equals("askosiWebapp.zip")) {
                    System.out.println("");
                    if (verbose) System.out.println("Beginning deploy of Askosi Web App");
                    if (copyAskosiWebApp(sourcePackageFile)) currentStep++;

                } else if (sourcePackageFile.getName().equals("classes.zip")) {
                    System.out.println("");
                    if (verbose) System.out.println("Beginning deploy of Askosi Plugin for Jspui");
                    if (copyAskosiPlugJspui(sourcePackageFile)) currentStep++;

                } else if (sourcePackageFile.getName().equals("exampleAskosiData.zip")) {
                    System.out.println("");
                    if (verbose) System.out.println("Beginning creation of Askosi data directory");
                    if (copyAskosiDataDir(sourcePackageFile)) currentStep++;
                }
            }
            if (currentStep == 3) {
                return true;
            }
        }
        return false;
    }


    private boolean copyJarsShared(File dirPackages)
    {
        File TomcatBaseFile = new File(TomcatBase);
        if (!TomcatBaseFile.exists() || !TomcatBaseFile.canRead()) {
            System.out.println("Tomcat base dir " + TomcatBase + " does not exist or is not readable");
            return false;
        }
        String TomcatBaseShared = new StringBuilder().append(TomcatBase).append("shared").append(System.getProperty("file.separator")).append("lib").append(System.getProperty("file.separator")).toString();
        File dirInstallJar = new File(TomcatBaseShared);
        if (!dirInstallJar.exists()) {
            TomcatBaseShared = new StringBuilder().append(TomcatBase).append("lib").append(System.getProperty("file.separator")).toString();
            dirInstallJar = new File(TomcatBaseShared);
        }
        if (!dirInstallJar.canWrite()) {
            if (verbose) System.out.println("Directory " + TomcatBaseShared + " is not writable");
            TomcatBaseShared = null;
        }
        dirInstallJar = null;
        while (true) {
            if (TomcatBaseShared != null) System.out.println("The jar files needed by Askosi are going to be copied to " + TomcatBaseShared + " or give new path:");
            else System.out.println("The jar files needed by Askosi are going to be copied to new path:");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if ((response != null) && (response.length() > 0)) {
                response = response.trim();
                dirInstallJar = new File(response);
            } else if (TomcatBaseShared != null) {
                response = TomcatBaseShared;
                dirInstallJar = new File(TomcatBaseShared);
            }
            if (dirInstallJar != null && dirInstallJar.exists() && dirInstallJar.canWrite()) {
                if (!response.equals(TomcatBaseShared)) TomcatBaseShared = response;
                break;
            } else {
                System.out.println("Directory " + response + " does not exist or is not writable");
            }
        }
        Iterator<File> fileIterPack = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"jar"}, false);
        return copyPackagesFiles(fileIterPack, TomcatBaseShared);
    }


    private boolean copyDatabaseDrivers()
    {
        String databaseDriverSourceDir = myInstallerDirPath + System.getProperty("file.separator") + "lib" + System.getProperty("file.separator");
        File databaseDriverSourceDirFile = new File(databaseDriverSourceDir);
        if (databaseDriverSourceDirFile.exists() && databaseDriverSourceDirFile.canRead()) {
            FilenameFilter selectJdbc = new FileListFilter("jdbc", "jar");
            File[] filesJdbc = databaseDriverSourceDirFile.listFiles(selectJdbc);
            if (filesJdbc.length > 0) {
                String TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("common").append(System.getProperty("file.separator")).append("lib").append(System.getProperty("file.separator")).toString();
                File dirInstallJar = new File(TomcatBaseCommon);
                if (!dirInstallJar.exists()) {
                    TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("lib").append(System.getProperty("file.separator")).toString();
                    dirInstallJar = new File(TomcatBaseCommon);
                }
                if (!dirInstallJar.canWrite()) {
                    if (verbose) System.out.println("Directory " + TomcatBaseCommon + " is not writable");
                    TomcatBaseCommon = null;
                }
                dirInstallJar = null;
                while (true) {
                    if (TomcatBaseCommon != null) System.out.println("The database jar files needed by Askosi are going to be copied to " + TomcatBaseCommon + " or give new path:");
                    else System.out.println("The database jar files needed by Askosi are going to be copied to new path:");
                    String response = null;
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    if ((response != null) && (response.length() > 0)) {
                        response = response.trim();
                        dirInstallJar = new File(response);
                    } else if (TomcatBaseCommon != null) {
                        response = TomcatBaseCommon;
                        dirInstallJar = new File(TomcatBaseCommon);
                    }
                    if (dirInstallJar != null && dirInstallJar.exists() && dirInstallJar.canWrite()) {
                        if (!response.equals(TomcatBaseCommon)) TomcatBaseCommon = response;
                        break;
                    } else {
                        System.out.println("Directory " + response + " does not exist or is not writable");
                    }
                }
                for (File fileJdbc : filesJdbc) {
                    if (!copyPackageFile(fileJdbc, TomcatBaseCommon)) {
                        System.out.println("Copy failed " + fileJdbc.getAbsolutePath());
                        return false;
                    }
                }
                System.out.println("");
                System.out.println("Database Jar files copied ok");
                return true;
            } else {
                System.out.println("Directory " + databaseDriverSourceDir + " doesn't have any jdbc driver");
            }
        } else {
            System.out.println("Directory " + databaseDriverSourceDir + " doesn't exist or is not readable");
        }
        return false;
    }


    private boolean copyAskosiWebApp(File sourcePackageFile)
    {
        String askosiWebAppDestDir = TomcatBase + System.getProperty("file.separator") + "webapps" + System.getProperty("file.separator");
        File askosiWebAppDestDirFile = new File(askosiWebAppDestDir);
        String message;
        if (!askosiWebAppDestDirFile.exists() || !askosiWebAppDestDirFile.canWrite()) {
            if (verbose) System.out.println("Tomcat webapps dir " + askosiWebAppDestDir + " does not exist or is not readable");
            message = "Path to copy the extracted files needed from askosiWebapp.zip:";
        } else {
            message = "The extracted files needed from askosiWebapp.zip are going to be copied to " + askosiWebAppDestDir + " or give new path:";
        }
        File finalAskosiWebAppDestDirFile = null;
        while (true) {
            System.out.println(message);
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = response.trim();
                finalAskosiWebAppDestDirFile = new File(response);
            } else {
                response = askosiWebAppDestDir;
                finalAskosiWebAppDestDirFile = new File(askosiWebAppDestDir);
            }
            if (finalAskosiWebAppDestDirFile != null && finalAskosiWebAppDestDirFile.exists() && finalAskosiWebAppDestDirFile.canWrite()) {
                break;
            } else {
                System.out.println("Directory " + response + " does not exist or is not writable");
            }
        }
        if (copyPackageZipFile(sourcePackageFile, finalAskosiWebAppDestDirFile.getAbsolutePath() + System.getProperty("file.separator"))) {
            System.out.println("");
            System.out.println("Askosi Web App extracted and copied ok");
            return true;
        } else {
            System.out.println("");
            System.out.println("Askosi Web App extracted and copied with fails");
        }
        return false;
    }


    private boolean copyAskosiPlugJspui(File sourcePackageFile)
    {
        File finalAskosiPlugJspDestDirFile = null;
        while (true) {
            System.out.println("Directory where Jspui is deployed (e.g.: " + TomcatBase + "/webapps/jspui): ");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(System.getProperty("file.separator")).append("WEB-INF").append(System.getProperty("file.separator")).append("classes").append(System.getProperty("file.separator")).toString();
                finalAskosiPlugJspDestDirFile = new File(response);
            }
            if (finalAskosiPlugJspDestDirFile != null && finalAskosiPlugJspDestDirFile.exists() && finalAskosiPlugJspDestDirFile.canWrite()) {
                break;
            } else {
                System.out.println("Directory " + response + " does not exist or is not writable");
            }
        }
        if (copyPackageZipFile(sourcePackageFile, finalAskosiPlugJspDestDirFile.getAbsolutePath() + System.getProperty("file.separator"))) {
            System.out.println("");
            System.out.println("Askosi Plugin for Jspui extracted and copied ok");
            return true;
        } else {
            System.out.println("");
            System.out.println("Askosi Plugin for Jspui extracted and copied with fails");
        }
        return false;
    }


    private boolean copyAskosiDataDir(File sourcePackageFile)
    {
        finalAskosiDataDestDirFile = null;
        while (true) {
            System.out.println("Directory of Askosi data: ");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(System.getProperty("file.separator")).toString();
                finalAskosiDataDestDirFile = new File(response);
            }
            if (finalAskosiDataDestDirFile != null) {
                if (finalAskosiDataDestDirFile.exists()) {
                    if (finalAskosiDataDestDirFile.canWrite())
                        break;
                    else
                        System.out.println("Directory " + response + " is not writable");
                } else {
                    System.out.println("Directory " + response + " does not exist, do you want to create it ([y]/n)?");
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    if ((response != null) && (response.length() > 0) && response.trim().equals("y")) {
                        if (!finalAskosiDataDestDirFile.mkdir()) {
                            System.out.println("Unable to create data directory: " + finalAskosiDataDestDirFile.getAbsolutePath());
                        } else break;
                    }
                }
            }
        }
        System.out.println("");
        if (copyPackageZipFile(sourcePackageFile, finalAskosiDataDestDirFile.getAbsolutePath() + System.getProperty("file.separator"))) {
            InstallerEDM.setAskosiDataDir(finalAskosiDataDestDirFile.getAbsolutePath());
            System.out.println("Creation of Askosi data directory ok");
            return true;
        } else System.out.println("Creation of Askosi data directory");
        return false;
    }


    private boolean copyPackageZipFile(File sourcePackageFile, String destDir)
    {
        try {
            ZipFile zf = new ZipFile(sourcePackageFile.getAbsolutePath());
            Enumeration entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if (entry.isDirectory()) {
                    File dirAux = new File(destDir + entry.getName());
                    if (!dirAux.exists() && !dirAux.mkdir()) {
                        System.out.println("Unable to create contents directory: " + dirAux.getAbsolutePath());
                    }
                } else {
                    if (verbose) System.out.println("Extracting file: " + entry.getName());
                    int index = entry.getName().lastIndexOf(47);
                    if (index == -1) {
                        index = entry.getName().lastIndexOf(92);
                    }
                    if (index > 0) {
                        File dir = new File(destDir + entry.getName().substring(0, index));
                        if (!dir.exists() && !dir.mkdirs()) {
                            System.out.println("Unable to create directory: " + dir.getAbsolutePath());
                        }
                    }
                    byte[] buffer = new byte[1024];

                    InputStream in = zf.getInputStream(entry);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destDir + entry.getName()));
                    int len;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean copyPackagesFiles(Iterator<File> fileIterPack, String TomcatBaseShared)
    {
        while (fileIterPack.hasNext()) {
            File sourcePackageFile = fileIterPack.next();
            if (!copyPackageFile(sourcePackageFile, TomcatBaseShared)) return false;
        }
        return true;
    }


    private boolean copyPackageFile (File sourcePackageFile, String TomcatBaseShared)
    {
        String nameSourcePackage = sourcePackageFile.getName();
        String nameDestPackage = TomcatBaseShared + System.getProperty("file.separator") + nameSourcePackage;
        File desPackageFile = new File(nameDestPackage);
        if (verbose) System.out.println("Copy " + sourcePackageFile.getAbsolutePath() + " to " + TomcatBaseShared);
        if (desPackageFile.exists()) {
            System.out.println("File " + nameDestPackage + " already exists. Do you want to overwrite it (y/[n])?");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (response.trim().equalsIgnoreCase("y")) {
                if (verbose) System.out.println("Overwriting file");
                try {
                    org.apache.commons.io.FileUtils.copyFile(sourcePackageFile, desPackageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            try {
                org.apache.commons.io.FileUtils.copyFile(sourcePackageFile, desPackageFile);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }


    public File checkPackages()
    {
        String myDirPackages = myInstallerDirPath + System.getProperty("file.separator") + "packages";
        File dirPackages = new File(myDirPackages);
        if (dirPackages.exists() && dirPackages.isDirectory() && dirPackages.canRead()) {
            Iterator<File> fileIter = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, null, false);
            while (fileIter.hasNext()) {
                File packageFile = fileIter.next();
                if (verbose) System.out.println("File: " + packageFile);
                if (!packageFile.canRead()) {
                    System.out.println("File " + packageFile + " is not readable");
                    return null;
                }
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(packageFile);
                    String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                    if (verbose) {
                        System.out.println("Md5 file: " + md5);
                    }
                    for (int i=0; i < packages.length; i++) {
                        if (packages[i].equals(packageFile.getName())) {
                            if (!md5.equals(packagesMD5[i])) {
                                System.out.println("Md5 didn't match in file: " + packageFile);
                                return null;
                            } else if (verbose) {
                                System.out.println("File ok");
                            }
                            break;
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return dirPackages;
        } else {
            System.out.println("Directory " + myDirPackages + " is not accesible");
        }
        return null;
    }


    public File getFinalAskosiDataDestDirFile()
    {
        return finalAskosiDataDestDirFile;
    }


    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }

}



class FileListFilter implements FilenameFilter
{
    private String inName;
    private String extension;

    public FileListFilter(String inName, String extension)
    {
        this.inName = inName;
        this.extension = extension;
    }

    public boolean accept(File directory, String filename)
    {
        boolean fileOK = true;

        if (inName != null) {
            fileOK &= filename.indexOf(inName) > 0;
        }
        if (extension != null) {
            fileOK &= filename.endsWith('.' + extension);
        }
        return fileOK;
    }
}

