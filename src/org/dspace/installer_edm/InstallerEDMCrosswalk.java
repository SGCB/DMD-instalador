package org.dspace.installer_edm;


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
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 4/02/13
 * Time: 12:51
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMCrosswalk extends InstallerEDMBase
{
    private String edmCrossWalk;
    private String edmCrossWalkName;
    private String edmCrossWalkClass;
    private File edmCrossWalkFile;
    private String edmCrossWalkContent;

    private String oaiApiJarName;
    private File oaiApiJarFile;
    private JarFile oaiApiJarJarFile;
    private File oaiApiJarWorkFile;

    private String oaiCatPropertiesName;
    private File oaiCatPropertiesFile;
    private File oaiCatPropertiesWorkFile;

    public InstallerEDMCrosswalk(int currentStepGlobal, String edmCrossWalk)
    {
        super(currentStepGlobal);
        this.edmCrossWalk = edmCrossWalk;
        this.edmCrossWalkFile = new File(edmCrossWalk);
    }

    private boolean checkEdmCrowssWalk() throws IOException
    {
        if (!edmCrossWalkFile.exists() || !edmCrossWalkFile.isFile() || !edmCrossWalkFile.canWrite()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkEdmCrowssWalk.notexists", new String[]{edmCrossWalk});
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkEdmCrowssWalk.newclass");
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (!response.isEmpty()) {
                    edmCrossWalkFile = new File(response);
                    return checkEdmCrowssWalk();
                }
                else return false;
            } while (true);
        }
        return true;
    }

    private boolean checkOaiApiJar() throws IOException
    {
        if (oaiApiJarFile == null || !oaiApiJarFile.exists() || !oaiApiJarFile.isFile() || !oaiApiJarFile.canWrite()) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkOaiApiJar.notexists", new String[]{((oaiApiJarName == null)?"dspace-oai-api":oaiApiJarName)});
            installerEDMDisplay.showQuestion(currentStepGlobal, "checkOaiApiJar.newjar");
            String response = null;
            do {
                response = br.readLine();
                if (response == null) continue;
                response = response.trim();
                if (!response.isEmpty()) {
                    oaiApiJarFile = new File(response);
                    return checkOaiApiJar();
                }
                else return false;
            } while (true);
        }
        return true;
    }


    public void configure()
    {
        try {
            if (checkEdmCrowssWalk() && checkOaiApiJar()) {
                edmCrossWalkName = edmCrossWalkFile.getName();
                edmCrossWalkClass = "org/dspace/app/oai/" + edmCrossWalkName.replaceFirst("\\.java", ".class");

                oaiApiJarWorkFile = new File(myInstallerWorkDirPath + fileSeparator + oaiApiJarFile.getName());
                copyDspaceFile2Work(oaiApiJarFile, oaiApiJarWorkFile, "configure.edmcrosswalk.oaiapijar");
                oaiApiJarName = oaiApiJarWorkFile.getAbsolutePath();
                oaiApiJarJarFile = new JarFile(oaiApiJarWorkFile);

                ZipEntry edmOaiApiEdmCrossWalkZipentry = oaiApiJarJarFile.getEntry(edmCrossWalkClass);
                if (edmOaiApiEdmCrossWalkZipentry != null) {
                    String response = null;
                    do {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.exists.class", new String[]{ edmCrossWalkClass, oaiApiJarFile.getAbsolutePath() });
                        response = br.readLine();
                        if (response == null) continue;
                        response = response.trim();
                        if (!response.isEmpty() && response.equalsIgnoreCase("y")) {
                            break;
                        } else return;
                    } while (true);
                }
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.begin", new String[]{edmCrossWalkName});
                installerEDMDisplay.showLn();
                readCrossWalk2String();
                confEDMRights();
                installerEDMDisplay.showLn();
                confEDMTypes();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.compile");
                if (compileEDMCrossWalk()) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.updatejar", new String[]{oaiApiJarWorkFile.getAbsolutePath()});
                    writeNewJar();
                    org.apache.commons.io.FileUtils.deleteDirectory(new File(myInstallerWorkDirPath + fileSeparator + "org"));
                    oaiApiJarJarFile = new JarFile(oaiApiJarWorkFile);
                    edmOaiApiEdmCrossWalkZipentry = oaiApiJarJarFile.getEntry(edmCrossWalkClass);
                    if (edmOaiApiEdmCrossWalkZipentry != null) {
                        if (confOaiCatProperties()) {
                            installerEDMDisplay.showLn();
                            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.ok", new String[]{oaiApiJarWorkFile.getName()});
                        } else installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.nok");
                    } else installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.nok");
                    oaiApiJarJarFile.close();
                } else installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.nok");
            }
        } catch (IOException e) {
            showException(e);
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.nok");
        }
    }


    private boolean confOaiCatProperties() throws IOException
    {
        boolean status = false;
        oaiCatPropertiesName = DspaceDir + fileSeparator + "config" + fileSeparator + "oaicat.properties";
        oaiCatPropertiesFile = new File(oaiCatPropertiesName);
        String response = null;
        installerEDMDisplay.showLn();
        do {
            installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.oaicat", new String[]{ oaiCatPropertiesName });
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
            if (!response.isEmpty()) {
                oaiCatPropertiesFile = new File(response);
            }
            if (oaiCatPropertiesFile.exists() && oaiCatPropertiesFile.canRead()) break;
        } while (true);
        oaiCatPropertiesWorkFile = new File(myInstallerWorkDirPath + fileSeparator + oaiCatPropertiesFile.getName());
        copyDspaceFile2Work(oaiCatPropertiesFile, oaiCatPropertiesWorkFile, "configure.edmcrosswalk.oaicat");

        Properties properties = new Properties();
        URL url = oaiCatPropertiesWorkFile.toURI().toURL();
        InputStream is = url.openStream();
        properties.load(is);
        Writer out = null;
        try {
            if (!properties.containsKey("Crosswalks.edm")) {
                out = new OutputStreamWriter(new FileOutputStream(oaiCatPropertiesWorkFile, true));
                out.write("\n# " + getTime() + " Appended by installerEDM to add the EDMCrosswalk\n");
                out.write("Crosswalks.edm=org.dspace.app.oai.EDMCrosswalk\n");
            }
            status = true;
        } finally {
            if (is != null) is.close();
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return status;
    }


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
                    if (!response2.isEmpty() && response2.equalsIgnoreCase("y")) {
                        break;
                    } else return;
                } while (true);
            }
            edmCrossWalkContent = edmCrossWalkContent.replaceFirst("private\\s+static\\s+final\\s+String\\s+EDMRIGHTS\\s+=\\s+\"" + edmRigths + "\";",
                    "private static final String EDMRIGHTS = \"" + response + "\";");
        }
    }


    private void confEDMTypes() throws IOException
    {
        Map<String, Set<String>> EDMTYPES = new HashMap<String, Set<String>>();
        final Pattern EDMTYPES_PATTERN = Pattern.compile("\\/\\/\\s+Begin\\s+Add\\s+EDMTYPES\\s+(EDMTYPES.get\\(\".+?\"\\).add\\(\".+?\"\\);\\s+)+\\s+\\/\\/\\s+End\\s+Add\\s+EDMTYPES");
        final Pattern EDMTYPE_PATTERN = Pattern.compile("EDMTYPES.get\\(\"(.+?)\"\\).add\\(\"(.+?)\"\\);");

        Matcher matcherEdmTypes = EDMTYPES_PATTERN.matcher(edmCrossWalkContent);
        if (matcherEdmTypes.find() && matcherEdmTypes.groupCount() > 0) {
            String TypesMatchStr = matcherEdmTypes.group(0);

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

                }
            };

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


    private void updateType(String type, Map<String, Set<String>> EDMTYPES) throws IOException
    {
        StringBuilder types = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types.append(value).append(",");
        }
        String strTypes = types.toString();
        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type.value.update", new String[]{type, strTypes.substring(0, strTypes.length() - 1)});
        String response = null;
        do {
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
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
        StringBuilder types2 = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types2.append(value).append(", ");
        }
        String strTypes2 = types2.toString();
        installerEDMDisplay.showMessage(strTypes2.substring(0, strTypes2.length() - 2));
        installerEDMDisplay.showLn();
    }


    private boolean compileEDMCrossWalk()
    {
        boolean status = false;

        SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject ("org.dspace.app.oai.EDMCrosswalk", edmCrossWalkContent) ;
        JavaFileObject javaFileObjects[] = new JavaFileObject[]{fileObject};

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFileObjects);

        String myInstallerPackagesDirPath = myInstallerDirPath + fileSeparator + "packages" + fileSeparator;
        StringBuilder jars = new StringBuilder(myInstallerPackagesDirPath).append("dspace-api-1.7.2.jar:").append(myInstallerPackagesDirPath).append("jdom-1.0.jar:").append(myInstallerPackagesDirPath).append("oaicat-1.5.48.jar");
        String[] compileOptions = new String[]{"-d", myInstallerWorkDirPath, "-target", "1.6","-cp", jars.toString()};
        Iterable<String> compilationOptionss = Arrays.asList(compileOptions);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();


        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);


        JavaCompiler.CompilationTask compilerTask = compiler.getTask(null, stdFileManager, diagnostics, compilationOptionss, null, compilationUnits) ;

        status = compilerTask.call();

        if (!status) {
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                installerEDMDisplay.showMessage("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic);
            }
        }

        try {
            stdFileManager.close();
        } catch (IOException e) {
            showException(e);
        }
        return status;
    }


    private void readCrossWalk2String() throws FileNotFoundException
    {
        Scanner edmCrossWalkScanner = null;
        try {
            StringBuilder fileContents = new StringBuilder((int)edmCrossWalkFile.length());
            edmCrossWalkScanner = new Scanner(edmCrossWalkFile);
            String lineSeparator = System.getProperty("line.separator");
            while (edmCrossWalkScanner.hasNextLine()) {
                fileContents.append(edmCrossWalkScanner.nextLine() + lineSeparator);
            }
            edmCrossWalkContent = fileContents.toString();
        } finally {
            if (edmCrossWalkScanner != null) edmCrossWalkScanner.close();
        }
    }


    private void writeNewJar() throws IOException
    {
        final int BUFFER_SIZE = 1024;
        File jarDir = new File(this.oaiApiJarJarFile.getName()).getParentFile();
        String name = oaiApiJarWorkFile.getName();
        String extension = name.substring(name.lastIndexOf('.'));
        name = name.substring(0, name.lastIndexOf('.'));
        File newJarFile = File.createTempFile(name, extension, jarDir);
        newJarFile.deleteOnExit();
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newJarFile));

        try {
            Enumeration<JarEntry> entries = oaiApiJarJarFile.entries();
            while (entries.hasMoreElements()) {
                installerEDMDisplay.showProgress('.');
                JarEntry entry = entries.nextElement();
                if (!entry.getName().equals(edmCrossWalkClass)) {
                    JarEntry entryOld = new JarEntry(entry);
                    entryOld.setCompressedSize(-1);
                    jarOutputStream.putNextEntry(entryOld);
                    InputStream intputStream = oaiApiJarJarFile.getInputStream(entry);
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    while ((count = intputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                        jarOutputStream.write(data, 0, count);
                    }
                    intputStream.close();
                }
            }
            installerEDMDisplay.showLn();
            addClass2Jar(jarOutputStream);
            oaiApiJarJarFile.close();
            oaiApiJarWorkFile.delete();
            if (newJarFile.renameTo(oaiApiJarWorkFile) && oaiApiJarWorkFile.setExecutable(true, true)) {
                oaiApiJarWorkFile = new File(oaiApiJarName);
            } else {
                throw new IOException();
            }
        } finally {
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
        }
    }


    private void addClass2Jar(JarOutputStream jarOutputStream) throws IOException
    {
        File file = new File(myInstallerWorkDirPath + fileSeparator + edmCrossWalkClass);
        byte [] fileData = new byte[(int)file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(fileData);
        dis.close();
        jarOutputStream.putNextEntry(new JarEntry(edmCrossWalkClass));
        jarOutputStream.write(fileData);
        jarOutputStream.closeEntry();
    }
}


class DynamicJavaSourceCodeObject extends SimpleJavaFileObject
{
    private String qualifiedName ;
    private String sourceCode ;

    protected DynamicJavaSourceCodeObject(String name, String code)
    {
        super(URI.create("string:///" + name.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.qualifiedName = name ;
        this.sourceCode = code ;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException
    {
        return sourceCode ;
    }

    public String getQualifiedName()
    {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName)
    {
        this.qualifiedName = qualifiedName;
    }

    public String getSourceCode()
    {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode)
    {
        this.sourceCode = sourceCode;
    }
}
