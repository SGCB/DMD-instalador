package org.dspace.installer_edm;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 24/10/12
 * Time: 13:08
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMAskosi extends InstallerEDMBase
{
    private final String[] packages = {"ASKOSI.jar", "askosiWebapp.zip", "classes.zip", "commons-dbcp.jar", "commons-pool.jar", "exampleAskosiData.zip", "jaxb-xalan-1.5.jar", "jsr311-api-1.1.1.jar", "jstl-1.2.jar", "log4j.jar", "openrdf-alibaba-2.0-beta6.jar", "openrdf-sesame-2.3.2-onejar.jar"};
    private final String[] packagesMD5 = {"f800262e9587383fa0dbd8f748cc831e", "ab932907d73a8031cb266d20d341a6e2", "0bffffb990ea99eb02a989d346454d8e", "2666cfeb7be74b1c2d8a1665ae21192c", "01f9bed60e2f88372132d34040ee81bb", "2be860d3a2529cb8789d6c27cfae5a92", "261968cebe30ffe8adcc201ad0bfa395", "c9803468299ec255c047a280ddec510f", "51e15f798e69358cb893e38c50596b9b", "599b8ba07d1d04f0ea34414e861d7ad1", "1f699edb215bcee75cb6f0616fa56993", "3054aa9109f78903852d38991b5a4ea8"};

    private File finalAskosiDataDestDirFile = null;



    public InstallerEDMAskosi()
    {
        super();
    }

    public boolean installPackages(File dirPackages)
    {
        installerEDMDisplay.showLn();
        if (verbose) installerEDMDisplay.showQuestion(1, "installPackages.title");
        if (copyJarsShared(dirPackages)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(1, "installPackages.jarok");

            installerEDMDisplay.showLn();
            if (verbose) installerEDMDisplay.showQuestion(1, "installPackages.db.title");
            if (!copyDatabaseDrivers()) return false;

            int currentStep = 0;
            Iterator<File> fileIterPackZip = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"zip"}, false);
            while (fileIterPackZip.hasNext()) {
                File sourcePackageFile = fileIterPackZip.next();

                if (sourcePackageFile.getName().equals("askosiWebapp.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(1, "installPackages.deploy.title");
                    if (copyAskosiWebApp(sourcePackageFile)) currentStep++;

                } else if (sourcePackageFile.getName().equals("classes.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(1, "installPackages.deploy.plugin.title");
                    if (copyAskosiPlugJspui(sourcePackageFile)) currentStep++;

                } else if (sourcePackageFile.getName().equals("exampleAskosiData.zip")) {
                    installerEDMDisplay.showLn();
                    if (verbose) installerEDMDisplay.showQuestion(1, "installPackages.datadir.title");
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
            installerEDMDisplay.showQuestion(1, "copyJarsShared.tomcatbasedir.notexist", new String[]{TomcatBase});
            return false;
        }
        String TomcatBaseShared = new StringBuilder().append(TomcatBase).append("shared").append(fileSeparator).append("lib").append(fileSeparator).toString();
        File dirInstallJar = new File(TomcatBaseShared);
        if (!dirInstallJar.exists()) {
            TomcatBaseShared = new StringBuilder().append(TomcatBase).append("lib").append(fileSeparator).toString();
            dirInstallJar = new File(TomcatBaseShared);
        }
        if (!dirInstallJar.canWrite()) {
            if (verbose) installerEDMDisplay.showQuestion(1, "copyJarsShared.tomcatbaseshareddir.notwritable", new String[]{TomcatBaseShared});
            TomcatBaseShared = null;
        }
        dirInstallJar = null;
        while (true) {
            if (TomcatBaseShared != null) installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.copyjars", new String[]{TomcatBaseShared});
            else installerEDMDisplay.getQuestion(1, "copyJarsShared.dirnew.copyjars");
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
                installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }
        Iterator<File> fileIterPack = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, new String[] {"jar"}, false);
        return copyPackagesFiles(fileIterPack, TomcatBaseShared);
    }


    private boolean copyDatabaseDrivers()
    {
        String databaseDriverSourceDir = myInstallerDirPath + fileSeparator + "lib" + fileSeparator;
        File databaseDriverSourceDirFile = new File(databaseDriverSourceDir);
        if (databaseDriverSourceDirFile.exists() && databaseDriverSourceDirFile.canRead()) {
            FilenameFilter selectJdbc = new FileListFilter("jdbc", "jar");
            File[] filesJdbc = databaseDriverSourceDirFile.listFiles(selectJdbc);
            if (filesJdbc.length > 0) {
                String TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("common").append(fileSeparator).append("lib").append(fileSeparator).toString();
                File dirInstallJar = new File(TomcatBaseCommon);
                if (!dirInstallJar.exists()) {
                    TomcatBaseCommon = new StringBuilder().append(TomcatBase).append("lib").append(fileSeparator).toString();
                    dirInstallJar = new File(TomcatBaseCommon);
                }
                if (!dirInstallJar.canWrite()) {
                    if (verbose) installerEDMDisplay.showQuestion(1, "copyJarsShared.tomcatbaseshareddir.notwritable", new String[]{TomcatBaseCommon});
                    TomcatBaseCommon = null;
                }
                dirInstallJar = null;
                while (true) {
                    if (TomcatBaseCommon != null) installerEDMDisplay.showQuestion(1, "copyDatabaseDrivers.dir.copyjars", new String[]{TomcatBaseCommon});
                    else installerEDMDisplay.getQuestion(1, "copyDatabaseDrivers.dirnew.copyjars");
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
                        installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.notwritable", new String[]{response});
                    }
                }
                for (File fileJdbc : filesJdbc) {
                    if (!copyPackageFile(fileJdbc, TomcatBaseCommon)) {
                        installerEDMDisplay.showQuestion(1, "copyDatabaseDrivers.copy.fail", new String[]{fileJdbc.getAbsolutePath()});
                        return false;
                    }
                }
                installerEDMDisplay.showLn();
                installerEDMDisplay.getQuestion(1, "copyDatabaseDrivers.copy.ok");
                return true;
            } else {
                installerEDMDisplay.showQuestion(1, "copyDatabaseDrivers.dir.nojar", new String[]{databaseDriverSourceDir});
            }
        } else {
            installerEDMDisplay.showQuestion(1, "copyDatabaseDrivers.dir.noread", new String[]{databaseDriverSourceDir});
        }
        return false;
    }


    private boolean copyAskosiWebApp(File sourcePackageFile)
    {
        String askosiWebAppDestDir = TomcatBase + fileSeparator + "webapps" + fileSeparator;
        File askosiWebAppDestDirFile = new File(askosiWebAppDestDir);
        String message;
        if (!askosiWebAppDestDirFile.exists() || !askosiWebAppDestDirFile.canWrite()) {
            if (verbose) installerEDMDisplay.showQuestion(1, "copyAskosiWebApp.askosiwebappdestdir.notexist", new String[]{askosiWebAppDestDir});
            message = installerEDMDisplay.getQuestion(1, "copyAskosiWebApp.askosiwebappdestdir");
        } else {
            message = installerEDMDisplay.getQuestion(1, "copyAskosiWebApp.askosiwebappdestdir.new") + askosiWebAppDestDir;
        }
        File finalAskosiWebAppDestDirFile = null;
        while (true) {
            installerEDMDisplay.showMessage(message);
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
                installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }
        if (copyPackageZipFile(sourcePackageFile, finalAskosiWebAppDestDirFile.getAbsolutePath() + fileSeparator)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.getQuestion(1, "copyAskosiWebApp.ok");
            return true;
        } else {
            installerEDMDisplay.showLn();
            installerEDMDisplay.getQuestion(1, "copyAskosiWebApp.fail");
        }
        return false;
    }


    private boolean copyAskosiPlugJspui(File sourcePackageFile)
    {
        File finalAskosiPlugJspDestDirFile = null;
        while (true) {
            installerEDMDisplay.showQuestion(1, "copyAskosiPlugJspui.dir", new String[]{TomcatBase});
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(fileSeparator).append("WEB-INF").append(fileSeparator).append("classes").append(fileSeparator).toString();
                finalAskosiPlugJspDestDirFile = new File(response);
            }
            if (finalAskosiPlugJspDestDirFile != null && finalAskosiPlugJspDestDirFile.exists() && finalAskosiPlugJspDestDirFile.canWrite()) {
                break;
            } else {
                installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.notwritable", new String[]{response});
            }
        }
        if (copyPackageZipFile(sourcePackageFile, finalAskosiPlugJspDestDirFile.getAbsolutePath() + fileSeparator)) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.getQuestion(1, "copyAskosiPlugJspui.ok");
            return true;
        } else {
            installerEDMDisplay.showLn();
            installerEDMDisplay.getQuestion(1, "copyAskosiPlugJspui.fail");
        }
        return false;
    }


    private boolean copyAskosiDataDir(File sourcePackageFile)
    {
        finalAskosiDataDestDirFile = null;
        while (true) {
            installerEDMDisplay.getQuestion(1, "copyAskosiDataDir");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if ((response != null) && (response.length() > 0)) {
                response = new StringBuilder().append(response.trim()).append(fileSeparator).toString();
                finalAskosiDataDestDirFile = new File(response);
            }
            if (finalAskosiDataDestDirFile != null) {
                if (finalAskosiDataDestDirFile.exists()) {
                    if (finalAskosiDataDestDirFile.canWrite())
                        break;
                    else
                        installerEDMDisplay.showQuestion(1, "copyJarsShared.dir.notwritable", new String[]{response});
                } else {
                    installerEDMDisplay.showQuestion(1, "copyAskosiDataDir.notexist", new String[]{response});
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    if ((response != null) && (response.length() > 0) && response.trim().equals("y")) {
                        if (!finalAskosiDataDestDirFile.mkdir()) {
                            installerEDMDisplay.showQuestion(1, "copyAskosiDataDir.failcreate", new String[]{finalAskosiDataDestDirFile.getAbsolutePath()});
                        } else break;
                    }
                }
            }
        }
        installerEDMDisplay.showLn();
        if (copyPackageZipFile(sourcePackageFile, finalAskosiDataDestDirFile.getAbsolutePath() + fileSeparator)) {
            setAskosiDataDir(finalAskosiDataDestDirFile.getAbsolutePath());
            installerEDMDisplay.getQuestion(1, "copyAskosiDataDir.ok");
            return true;
        } else installerEDMDisplay.getQuestion(1, "copyAskosiDataDir.fail");
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
                        installerEDMDisplay.showQuestion(1, "copyPackageZipFile.failcreate", new String[]{dirAux.getAbsolutePath()});
                    }
                } else {
                    if (verbose) installerEDMDisplay.showQuestion(1, "copyPackageZipFile.extract", new String[]{entry.getName()});
                    int index = entry.getName().lastIndexOf(47);
                    if (index == -1) {
                        index = entry.getName().lastIndexOf(92);
                    }
                    if (index > 0) {
                        File dir = new File(destDir + entry.getName().substring(0, index));
                        if (!dir.exists() && !dir.mkdirs()) {
                            installerEDMDisplay.showQuestion(1, "copyPackageZipFile.failcreate.dir", new String[]{dir.getAbsolutePath()});
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
        String nameDestPackage = TomcatBaseShared + fileSeparator + nameSourcePackage;
        File desPackageFile = new File(nameDestPackage);
        if (verbose) installerEDMDisplay.showQuestion(1, "copyPackageFile.copyfile", new String[]{sourcePackageFile.getAbsolutePath(), TomcatBaseShared});
        if (desPackageFile.exists()) {
            installerEDMDisplay.showQuestion(1, "copyPackageFile.fileexists", new String[]{nameDestPackage});
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (response.trim().equalsIgnoreCase("y")) {
                if (verbose) installerEDMDisplay.getQuestion(1, "copyPackageFile.orverwritingfile");
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
        String myDirPackages = myInstallerDirPath + fileSeparator + "packages";
        File dirPackages = new File(myDirPackages);
        if (dirPackages.exists() && dirPackages.isDirectory() && dirPackages.canRead()) {
            Iterator<File> fileIter = org.apache.commons.io.FileUtils.iterateFiles(dirPackages, null, false);
            while (fileIter.hasNext()) {
                File packageFile = fileIter.next();
                if (verbose) installerEDMDisplay.showQuestion(1, "checkPackages.file", new String[]{packageFile.getAbsolutePath()});
                if (!packageFile.canRead()) {
                    installerEDMDisplay.showQuestion(1, "checkPackages.file.notreadable", new String[]{packageFile.getAbsolutePath()});
                    return null;
                }
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(packageFile);
                    String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                    if (verbose) {
                        installerEDMDisplay.showQuestion(1, "checkPackages.file.md5", new String[]{md5});
                    }
                    for (int i=0; i < packages.length; i++) {
                        if (packages[i].equals(packageFile.getName())) {
                            if (!md5.equals(packagesMD5[i])) {
                                installerEDMDisplay.showQuestion(1, "checkPackages.file.md5.nok", new String[]{packageFile.getAbsolutePath()});
                                return null;
                            } else if (verbose) {
                                installerEDMDisplay.showQuestion(1, "checkPackages.file.ok");
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
            installerEDMDisplay.showQuestion(1, "checkPackages.directory.notaccessible", new String[]{myDirPackages});
        }
        return null;
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

