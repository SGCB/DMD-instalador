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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * @class InstallerEDMCrosswalk
 *
 * Clase para instalar el plugin de EDMCrosswalk de java en dspace para mostrar en OAI los ítems en formato EDM.
 *
 * Se copia el archivo EDMCrosswalk.java al directorio de trabajo del instalador, se piden algunos datos especiales para edm,
 * se cambian en el fuente del java, se compila y se genera un class que se añade al jar de la api de oai de dspace.
 * Se copia oaicat.properties al directorio de trabajo del instalador y se modifica para añadir el crosswalk para edm
 *
 */
public class InstallerEDMCrosswalk extends InstallerEDMBase
{
    /**
     * ruta del archivo EDMCrosswalk.java
     */
    private String edmCrossWalk;

    /**
     * nombre del archivo EDMCrosswalk.java
     */
    private String edmCrossWalkName;

    /**
     * nombre del archivo EDMCrosswalk.class
     */
    private String edmCrossWalkClass;

    /**
     * archivo EDMCrosswalk.java
     */
    private File edmCrossWalkFile;

    /**
     * contenido del archivo EDMCrosswalk.java
     */
    private String edmCrossWalkContent;

    /**
     * ruta del archivo de la api de dspace para oai
     */
    private String oaiApiJarName;

    /**
     * archivo de la api de dspace para oai
     */
    private File oaiApiJarFile;

    /**
     * archivo jar de oaiApiJarWorkFile
     */
    private JarFile oaiApiJarJarFile;

    /**
     * archivo de la apo de dspace para oai en el directorio de trabajo del instalador
     */
    private File oaiApiJarWorkFile;

    /**
     * ruta del archivo oaicat.properties
     */
    private String oaiCatPropertiesName;

    /**
     * archivo oaicat.properties
     */
    private File oaiCatPropertiesFile;

    /**
     * archivo oaicat.properties en el directorio de trabajo del instalador
     */
    private File oaiCatPropertiesWorkFile;

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param edmCrossWalk ruta del archivo EDMCrosswalk.java
     */
    public InstallerEDMCrosswalk(int currentStepGlobal, String edmCrossWalk)
    {
        super(currentStepGlobal);
        this.edmCrossWalk = edmCrossWalk;
        this.edmCrossWalkFile = new File(edmCrossWalk);
    }

    /**
     * Comprueba que el archivo EDMCrosswalk.java existe y se puede modificar, si no se pide la ruta
     *
     * @return si existe y se puede modificar
     * @throws IOException
     */
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

    /**
     * Comprueba que el archivo de la api de dspace para oai existe y se puede leer, si no se pide la ruta
     *
     * @return si existe y se pude leer
     * @throws IOException
     */
    private boolean checkOaiApiJar() throws IOException
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
     * Se comprueba que exista el archivo EDMCrosswalk.java y se pueda modificar
     * Se comprueba que exista el archivo de la api de oai de dspace y se pueda leer y se copia al directorio de trabajo del instalador
     *
     * Se abre el jar de la api de oai de dsapce y se comprueba si ya existe el archivo de la clase EDMCrosswalk.class y se pregunta si se sustituye.
     * Se configura edm.rights, edm.types, se escribe el java, se compila y se introduce en el jar de la api de oai de dspace.
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
                // configura edm.rights
                confEDMRights();
                installerEDMDisplay.showLn();
                // configura edm.types
                confEDMTypes();
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
     * Recoge oaicat.properties de dspace o pide su ruta.
     * Lo copia al directorio de trabajo del instalador.
     * Modifica su contenido para añadir la propiedad del crosswalk: Crosswalks.edm=org.dspace.app.oai.EDMCrosswalk
     *
     * @return éxito de la operación
     * @throws IOException
     */
    private boolean confOaiCatProperties() throws IOException
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
            // modifica su contenido para añadir la propiedad del crosswalk
            if (!properties.containsKey("Crosswalks.edm")) {
                out = new OutputStreamWriter(new FileOutputStream(oaiCatPropertiesWorkFile, true));
                out.write("\n\n# " + getTime() + " Appended by installerEDM to add the EDMCrosswalk\n");
                out.write("Crosswalks.edm=org.dspace.app.oai.EDMCrosswalk\n");
            } else {
                installerEDMDisplay.showQuestion(currentStepGlobal, "confOaiCatProperties.exist", new String[]{properties.getProperty("Crosswalks.edm"), "org.dspace.app.oai.EDMCrosswalk"});
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
     * Busca en EDMCrosswalk.java la variable estática donde se introduce la url de edm.rights
     * private static final String EDMRIGHTS
     *
     * y la modifica para añadir la url escogida
     *
     * @throws IOException
     */
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
                    if (!response2.isEmpty() && response2.equalsIgnoreCase(answerYes)) {
                        break;
                    } else return;
                } while (true);
            }
            edmCrossWalkContent = edmCrossWalkContent.replaceFirst("private\\s+static\\s+final\\s+String\\s+EDMRIGHTS\\s+=\\s+\"" + edmRigths + "\";",
                    "private static final String EDMRIGHTS = \"" + response + "\";");
        }
    }


    /**
     * Busca en EDMCrosswalk.java el bloque del hash de los tipos con palabras claves
     * para cada tipo encontrado se piden las palabras asociadas a él y se modifica el hash para añadirlas
     *
     * @throws IOException
     */
    private void confEDMTypes() throws IOException
    {
        // hash de tipos con conjunto de palabras
        Map<String, Set<String>> EDMTYPES = new HashMap<String, Set<String>>();
        // patrón de grupo de tipos
        final Pattern EDMTYPES_PATTERN = Pattern.compile("\\/\\/\\s+Begin\\s+Add\\s+EDMTYPES\\s+(EDMTYPES.get\\(\".+?\"\\).add\\(\".+?\"\\);\\s+)+\\s+\\/\\/\\s+End\\s+Add\\s+EDMTYPES");
        // patrón de añadir palabra a tipo
        final Pattern EDMTYPE_PATTERN = Pattern.compile("EDMTYPES.get\\(\"(.+?)\"\\).add\\(\"(.+?)\"\\);");

        // bloque del hash de los tipos
        Matcher matcherEdmTypes = EDMTYPES_PATTERN.matcher(edmCrossWalkContent);
        if (matcherEdmTypes.find() && matcherEdmTypes.groupCount() > 0) {
            String TypesMatchStr = matcherEdmTypes.group(0);

            // recorrer grupo para recoger asociación tipo-palabra
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
                    showException(e);
                }
            };

            // recorrer tipos para mostrar palabras
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

            // preguntar qué tipo actualizar
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

            // modificar el java con los nuevos valores tipo-palabras
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


    /**
     * Pregunta una lista de palabras separadas por "," para asociar a un tipo
     *
     * @param type cadena con el tipo a modificar
     * @param EDMTYPES hasn con la asociación tipo-palabras
     * @throws IOException
     */
    private void updateType(String type, Map<String, Set<String>> EDMTYPES) throws IOException
    {
        // recoger palabras actuales del tipo
        StringBuilder types = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types.append(value).append(",");
        }
        // mostrarlas
        String strTypes = types.toString();
        installerEDMDisplay.showQuestion(currentStepGlobal, "configure.edmcrosswalk.conf.edmtypes.type.value.update", new String[]{type, strTypes.substring(0, strTypes.length() - 1)});
        // preguntar nueva lista de palabras separadas por ","
        String response = null;
        do {
            response = br.readLine();
            if (response == null) continue;
            response = response.trim();
            // añadir lista al hash
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
        // mostrar nueva lista de palabras
        StringBuilder types2 = new StringBuilder();
        for (String value : EDMTYPES.get(type)) {
            types2.append(value).append(", ");
        }
        String strTypes2 = types2.toString();
        installerEDMDisplay.showMessage(strTypes2.substring(0, strTypes2.length() - 2));
        installerEDMDisplay.showLn();
    }


    /**
     * Compila el java en tiempo real
     *
     * @return éxito de la operación
     */
    private boolean compileEDMCrossWalk()
    {
        boolean status = false;

        SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject ("org.dspace.app.oai.EDMCrosswalk", edmCrossWalkContent) ;
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
     * @throws FileNotFoundException
     */
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


    /**
     * Abre el jar para escribir el nuevo archivo class compilado, elresto de archivos los copia tal cual
     *
     * @throws IOException
     */
    private void writeNewJar() throws IOException
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
