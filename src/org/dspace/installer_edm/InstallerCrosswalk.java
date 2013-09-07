package org.dspace.installer_edm;


import org.apache.commons.io.FileUtils;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * @class InstallerCrosswalk
 *
 * Clase añadir un crosswalk al jar de la api de oai y configurar oaicat.properties para mostrarlo
 *
 *
 */
public abstract class InstallerCrosswalk extends InstallerEDMBase
{

    /**
     * Nombre del formato
     */
    protected String crosswalkName;

    /**
     * Nombre completo de la clase
     */
    protected String canonicalCrosswalkName;

    /**
     * ruta del archivo XXXCrosswalk.java
     */
    protected String edmCrossWalk;

    /**
     * nombre del archivo XXXCrosswalk.java
     */
    protected String edmCrossWalkName;

    /**
     * nombre del archivo XXXCrosswalk.class
     */
    protected String edmCrossWalkClass;

    /**
     * archivo XXXCrosswalk.java
     */
    protected File edmCrossWalkFile;

    /**
     * contenido del archivo XXXCrosswalk.java
     */
    protected String edmCrossWalkContent;

    /**
     * ruta del archivo de la api de dspace para oai
     */
    protected String oaiApiJarName;

    /**
     * archivo de la api de dspace para oai
     */
    protected File oaiApiJarFile;

    /**
     * archivo jar de oaiApiJarWorkFile
     */
    protected JarFile oaiApiJarJarFile;

    /**
     * archivo de la apo de dspace para oai en el directorio de trabajo del instalador
     */
    protected File oaiApiJarWorkFile;

    /**
     * ruta del archivo oaicat.properties
     */
    protected String oaiCatPropertiesName;

    /**
     * archivo oaicat.properties
     */
    protected File oaiCatPropertiesFile;

    /**
     * archivo oaicat.properties en el directorio de trabajo del instalador
     */
    protected File oaiCatPropertiesWorkFile;


    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerCrosswalk(int currentStepGlobal, String crosswalkName)
    {
        super(currentStepGlobal);
        this.crosswalkName = crosswalkName;
        this.canonicalCrosswalkName = "org.dspace.app.oai." + crosswalkName + "Crosswalk";
        this.edmCrossWalk = myInstallerDirPath + fileSeparator + "packages" + fileSeparator + crosswalkName + "Crosswalk.java";
        this.edmCrossWalkFile = new File(edmCrossWalk);
    }


    /**
     * Método para ser llamado por las clases hijas para configuración específica
     */
    public abstract void configureCrosswalk();


    /**
     * Se comprueba que exista el archivo XXXCrosswalk.java y se pueda modificar
     * Se comprueba que exista el archivo de la api de oai de dspace y se pueda leer y se copia al directorio de trabajo del instalador
     *
     * Se abre el jar de la api de oai de dsapce y se comprueba si ya existe el archivo de la clase XXXCrosswalk.class y se pregunta si se sustituye.
     * Se configura, se escribe el java, se compila y se introduce en el jar de la api de oai de dspace.
     *
     * Se modifica oai.properties para añadir el crosswalk
     */
    public void configure()
    {
        try {
            // comprueba que exista el archivo EDMCrosswalk.java y se pueda modificar. Comprueba que exista el archivo de la api de oai de dspace y se pueda leer
            if (checkEdmCrowssWalk() && checkOaiApiJar()) {
                edmCrossWalkName = edmCrossWalkFile.getName();
                edmCrossWalkClass = "org/dspace/app/oai/" + edmCrossWalkName.replaceFirst("\\.java", ".class");

                // copia al directorio de trabajo del instalador
                oaiApiJarWorkFile = new File(myInstallerWorkDirPath + fileSeparator + oaiApiJarFile.getName());
                copyDspaceFile2Work(oaiApiJarFile, oaiApiJarWorkFile, "configure.edmcrosswalk.oaiapijar");
                oaiApiJarName = oaiApiJarWorkFile.getAbsolutePath();

                // abre el jar de la api de oai de dsapce
                oaiApiJarJarFile = new JarFile(oaiApiJarWorkFile);
                // comprueba si ya existe el archivo de la clase EDMCrosswalk.class
                ZipEntry edmOaiApiEdmCrossWalkZipentry = oaiApiJarJarFile.getEntry(edmCrossWalkClass);
                if (edmOaiApiEdmCrossWalkZipentry != null) {
                    // se pregunta si se sustituye
                    String response = null;
                    do {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.exists.class", new String[]{ edmCrossWalkClass, oaiApiJarFile.getAbsolutePath() });
                        response = br.readLine();
                        if (response == null) continue;
                        response = response.trim();
                        if (!response.isEmpty() && response.equalsIgnoreCase(answerYes)) {
                            break;
                        } else return;
                    } while (true);
                }
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.begin", new String[]{edmCrossWalkName});
                installerEDMDisplay.showLn();
                // se lee el java y se recoge el contenido en edmCrossWalkContent
                readCrossWalk2String();
                configureCrosswalk();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.compile");
                // compila
                if (compileEDMCrossWalk()) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.updatejar", new String[]{oaiApiJarWorkFile.getAbsolutePath()});
                    // se introduce en el jar de la api de oai de dspace
                    writeNewJar();
                    org.apache.commons.io.FileUtils.deleteDirectory(new File(myInstallerWorkDirPath + fileSeparator + "org"));
                    oaiApiJarJarFile = new JarFile(oaiApiJarWorkFile);
                    edmOaiApiEdmCrossWalkZipentry = oaiApiJarJarFile.getEntry(edmCrossWalkClass);
                    if (edmOaiApiEdmCrossWalkZipentry != null) {
                        // modifica oai.properties para añadir el crosswalk
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



    /**
     * Comprueba que el archivo XXXCrosswalk.java existe y se puede modificar, si no se pide la ruta
     *
     * @return si existe y se puede modificar
     * @throws IOException
     */
    protected boolean checkEdmCrowssWalk() throws IOException
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


    /**
     * Comprueba que el archivo de la api de dspace para oai existe y se puede leer, si no se pide la ruta
     *
     * @return si existe y se pude leer
     * @throws IOException
     */
    protected boolean checkOaiApiJar() throws IOException
    {
        if (oaiApiJarFile == null || !oaiApiJarFile.exists() || !oaiApiJarFile.isFile() || !oaiApiJarFile.canRead()) {
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




    /**
     * Recoge oaicat.properties de dspace o pide su ruta.
     * Lo copia al directorio de trabajo del instalador.
     * Modifica su contenido para añadir la propiedad del crosswalk: Crosswalks.edm=org.dspace.app.oai.EDMCrosswalk
     *
     * @return éxito de la operación
     * @throws IOException
     */
    protected boolean confOaiCatProperties() throws IOException
    {
        boolean status = false;
        // recoge oaicat.properties de dspace
        oaiCatPropertiesName = DspaceDir + fileSeparator + "config" + fileSeparator + "oaicat.properties";
        oaiCatPropertiesFile = new File(oaiCatPropertiesName);
        // pide su ruta
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
        // copia al directorio de trabajo del instalador
        oaiCatPropertiesWorkFile = new File(myInstallerWorkDirPath + fileSeparator + oaiCatPropertiesFile.getName());
        copyDspaceFile2Work(oaiCatPropertiesFile, oaiCatPropertiesWorkFile, "configure.edmcrosswalk.oaicat");

        // lee propiedades del archivo
        Properties properties = new Properties();
        URL url = oaiCatPropertiesWorkFile.toURI().toURL();
        InputStream is = url.openStream();
        properties.load(is);
        Writer out = null;
        try {
            String propCrossw = "Crosswalks." + crosswalkName.toLowerCase();
            // modifica su contenido para añadir la propiedad del crosswalk
            if (!properties.containsKey(propCrossw)) {
                out = new OutputStreamWriter(new FileOutputStream(oaiCatPropertiesWorkFile, true));
                out.write("\n\n# " + getTime() + " Appended by installerEDM to add the " + crosswalkName  +"Crosswalk\n");
                out.write(propCrossw + "=" + canonicalCrosswalkName + "\n");
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "confOaiCatProperties.exist", new String[]{propCrossw, properties.getProperty(propCrossw), canonicalCrosswalkName});
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




    /**
     * Compila el java en tiempo real
     *
     * @return éxito de la operación
     */
    protected boolean compileEDMCrossWalk()
    {
        boolean status = false;

        SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject (canonicalCrosswalkName, edmCrossWalkContent) ;
        JavaFileObject javaFileObjects[] = new JavaFileObject[]{fileObject};

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFileObjects);

        // librerías necesarias para linkar con el fuente a compilar
        String myInstallerPackagesDirPath = myInstallerDirPath + fileSeparator + "packages" + fileSeparator;
        StringBuilder jars = (fileSeparator.equals("\\"))?new StringBuilder(myInstallerPackagesDirPath).append("dspace-api-1.7.2.jar;").append(myInstallerPackagesDirPath).append("jdom-1.0.jar;").append(myInstallerPackagesDirPath).append("oaicat-1.5.48.jar"):new StringBuilder(myInstallerPackagesDirPath).append("dspace-api-1.7.2.jar:").append(myInstallerPackagesDirPath).append("jdom-1.0.jar:").append(myInstallerPackagesDirPath).append("oaicat-1.5.48.jar");

        String[] compileOptions = new String[]{"-d", myInstallerWorkDirPath, "-source", "1.6", "-target", "1.6", "-cp", jars.toString()};
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



    /**
     * Lee el fuente del java para introducirlo en una cadena
     *
     * @throws java.io.FileNotFoundException
     */
    protected void readCrossWalk2String() throws FileNotFoundException
    {
        Scanner edmCrossWalkScanner = null;
        try {
            StringBuilder fileContents = new StringBuilder((int)edmCrossWalkFile.length());
            edmCrossWalkScanner = new Scanner(edmCrossWalkFile);
            String lineSeparator = System.getProperty("line.separator");
            while (edmCrossWalkScanner.hasNextLine()) {
                fileContents.append(edmCrossWalkScanner.nextLine()).append(lineSeparator);
            }
            edmCrossWalkContent = fileContents.toString();
        } finally {
            if (edmCrossWalkScanner != null) edmCrossWalkScanner.close();
        }
    }


    /**
     * Abre el jar para escribir el nuevo archivo class compilado, elresto de archivos los copia tal cual
     *
     * @throws IOException
     */
    protected void writeNewJar() throws IOException
    {
        // buffer para leer datos de los archivos
        final int BUFFER_SIZE = 1024;
        // directorio de trabajo
        File jarDir = new File(this.oaiApiJarJarFile.getName()).getParentFile();
        // nombre del jar
        String name = oaiApiJarWorkFile.getName();
        String extension = name.substring(name.lastIndexOf('.'));
        name = name.substring(0, name.lastIndexOf('.'));
        // archivo temporal del nuevo jar
        File newJarFile = File.createTempFile(name, extension, jarDir);
        newJarFile.deleteOnExit();
        // flujo de escritura del nuevo jar
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newJarFile));

        // recorrer todos los archivos del jar menos el crosswalk para replicarlos
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
            // añadir class compilado
            addClass2Jar(jarOutputStream);
            // cerrar jar original
            oaiApiJarJarFile.close();
            // borrar jar original
            oaiApiJarWorkFile.delete();
            // cambiar jar original por nuevo
            try {
                /*if (newJarFile.renameTo(oaiApiJarWorkFile) && oaiApiJarWorkFile.setExecutable(true, true)) {
                    oaiApiJarWorkFile = new File(oaiApiJarName);
                } else {
                    throw new IOException();
                }*/
                if (jarOutputStream != null) jarOutputStream.close();
                FileUtils.moveFile(newJarFile, oaiApiJarWorkFile);
                oaiApiJarWorkFile.setExecutable(true, true);
                oaiApiJarWorkFile = new File(oaiApiJarName);
            } catch (Exception io) {
                io.printStackTrace();
                throw new IOException();
            }
        } finally {
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
        }
    }


    /**
     * Añade contenido archivo class a nuevo archivo class en el jar
     *
     * @param jarOutputStream flujo de escritura para el jar
     * @throws IOException
     */
    protected void addClass2Jar(JarOutputStream jarOutputStream) throws IOException
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



/**
 * Clase con el fuente preparado para se compilado
 */
class DynamicJavaSourceCodeObject extends SimpleJavaFileObject
{
    private String qualifiedName ;
    private String sourceCode ;

    /**
     * Constructor
     *
     * @param name nombre del fuente
     * @param code contenido del fuente
     */
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
