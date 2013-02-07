INSTALLER FOR ASKOSI, EDMEXPORT AND EDMCROSSWALK ON DSPACE
-----------------------------------------------------------

The installer is programmed in Java and so a Java virtual machine is required
to be run. It's tested with Oracle and OpenJdk 1.6 versions, beyond that no
reliability is assured.

It has not a gui view because is designed to run from a terminal or console. The
majority of servers (Linux/Unix) run without a graphic server to avoid the comsuption
of resources needed for the important processes. They only have remote access via ssh
or another secure protocol.
So there is not a pretty view, only operational.

The installer has only been tested with Tomcat 5.5 and Tomcat 6.

As the app install jar libraries in Tomcat dir libraries, it has to be run with a user
with write permissions.

The installer require the next dspace modules installed: jspui and oai.
It's been tested with dspace 1.7 and 1.8.


The installer comes with two directories:

packages: files required to uncompress, copy and/or modify. List of files:

    ASKOSI.jar ............................... Library required for Askosi.
    askosiWebapp.zip ......................... Compressed file with Askosi web app classes to be deployed in the
                                               Tomcat web apps directory.
    classes.zip .............................. Compressed file with Askosi jspui classes to be copied in the directory
                                               where the dspace jspui module is deployed.
    commons-dbcp.jar ......................... Library required for Askosi.
    commons-pool.jar ......................... Library required for Askosi.
    dspace-api-1.7.2.jar ..................... Dspace library required by the installer. Version 1.7.2 has been chosen
                                               because it's not the newer or an ancient one.
    EDMCrosswalk.java ........................ Java source file to be compiled by the installer and added to the
                                               dspace oai api jar library.
    EDMExport.war ............................ War file to be modified by the installer and deployed manually in the
                                               Tomcat web apps dir or wherever the admin decides to.
    exampleAskosiData.zip .................... Compressed file required by Askosi and processesed by the installer
                                               to create the Asosi data dir.
    jaxb-xalan-1.5.jar ....................... Library required for Askosi.
    jdom-1.0.jar ............................. Library required for Askosi and installer.
    jsr311-api-1.1.1.jar ..................... Library required for Askosi.
    jstl-1.2.jar ............................. Library required for Askosi.
    log4j.jar ................................ Library required for Askosi.
    oaicat-1.5.48.jar ........................ Library required for Askosi and installer.
    openrdf-alibaba-2.0-beta6.jar ............ Library required for Askosi.
    openrdf-sesame-2.3.2-onejar.jar .......... Library required for Askosi.


lib: libraries required by the installer. List of files:

    commons-cli-1.2.jar ...................... Library required for installer.
    commons-codec-1.7.jar .................... Library required for installer.
    commons-io-2.4.jar ....................... Library required for installer.
    lanterna-2.1.1.jar ....................... Library required for installer.
    normalizer.jar ........................... Library required for installer.
    postgresql-8.1-408.jdbc3.jar ............. Library required for installer.


There are two scripts to run the installer with the libraries path configured correctly and more pertinent checks.

For Linux/Unix: install.sh
For Windows: install.bat

Thw way to run the installer is:

Linux/Unix:

$ ./install.sh -d dspace_deployed_dir -t tomcat_base_physycal_dir -v -s 0 -l en

View the help:
$ ./install.sh -h


Windows:

drive:\path2installer\install.bat -d dspace_deployed_dir -t tomcat_base_physycal_dir -v -s 0 -l en

View the help:
$ ./install.bat -h

Result: Use: install.sh: [-d dir_space_runtime] [-h] [-l language] [-s step] [-t dir_tomcat_base] [-v]


The options are:

-d: path to root dir where dspace is deployed.
-t: path to Tomcat root dir.
-s: step to launch. 0 is preferred to see the main menu first.
-v: verbosity.
-l: Messages language. If blank the systema default locale will be taken.
    Actually spanish and english is supported. For spanish: es_ES.
    For english: en.


Por último está el archivo jar del instalador InstallerEDM.jar, que ha de poseer permisos de ejecución. Se podría lanzar
manualmente, pero sería algo como (los directorios son ficticios):

$ java -cp ./lib/commons-cli-1.2.jar:./lib/commons-codec-1.7.jar:./lib/commons-io-2.4.jar:./lib/lanterna-2.1.1.jar:./lib/normalizer.jar:./lib/postgresql-8.1-408.jdbc3.jar:/home/europeana/runtime//lib/activation-1.1.jar:/home/europeana/runtime//lib/ant-1.7.0.jar:/home/europeana/runtime//lib/ant-launcher-1.7.0.jar:.. muchas más librerías ..:InstallerEDM.jar:.:/home/europeana/runtime/config org.dspace.installer_edm.InstallerEDM -t /usr/share/tomcat5.5 -s 0  -v -l es_ES

lo que evidentemente es impráctico, por eso mejor lanzarlo con los scripts.
Las opciones del jar son las mismas que la de los scripts.


Como el instalador modifica la base de datos relacional de Dspace para crear comunidades, colecciones e ítems,
sería conveniente realizar una copia de ella para tener la seguridad de poderrealizar una regresión correcta.
Es necesario poseer un usuario en Dspace con permisos de creación de los comunidades, colecciones e ítems para
poder crear los ítems de las autoridades.
Cuando se termine, si se quiere que las comunidades, colecciones e ítems de las autoridades aparezcan en el
buscador de Dspace, se tendrá que reindexar Solr.

Con la aplicación Askosi se logra que cuando cataloga pueda asociar al valor de un elemento dc de su nuevo ítem, el
handle del ítem autoridad que tiene el mismo nombre, cualificador y valor. Este handle se guarda en el campo authority
de la tabla metadatavalue de la base de datos relacional.
El EDMExport y el EDMCrosswalk para generar la case de SKOS en el esquema EDM comprueba si el campo authority del ítems
tiene un valor, si es una url válida la muestra tal cual, si es un handle comprueba que exista en nuestra base de datos
y construye la url final del comcepto skos con la url base de dspace y el handle.

El instalador consta de los siguientes pasos:

1: Instalar Askosi
2: Crear Items de Autoridad
3: Configurar Dspace y Askosi
4: Configurar EDMExport
5: Configurar EDMCrosswalk
6: Salir

El primer paso instala la aplicación Askosi en el servidor. Todos los archivos necesarios los coge del directorio
packages del instalador.
Sería necesario reiniciar el Tomcat o Dspace al realizar este paso.

El segundo crea las comunidades, colecciones e ítems de las autoridades y las llena de valores buscando en las
colecciones de ítems que no son de autoridades. Hay que validarse con un usuario de Dspace com permisos.
Para cada elemento DC que se quiere que sea una autoridad se crea o asocia a una comunidad y colección de autoridades.
Esos elementos se buscan en el resto de colecciones de no autoridades para identificar valores únicos.
No sería necesario reiniciar el Tomcat o Dspace.

El tercer paso copia el fichero de configuración de Dspace "dspace.cfg" y el de catalogación "input-forms.xml"
a un directorio llamado work en el directorio donde está el instalador. Los modifica con las directivas o propiedades
nuevas y los deja ahí para revisión del administrador. Éste es el que manualmente (como medida de seguridad) tendrá
que llevarlos a Dspace.
Sería necesario reiniciar el Tomcat o Dspace al realizar este paso.

El cuarto paso copia el archivo EDMExport.war de packages a work y lo modifica para configurarlo con la ruta al archivo
"dspace.cfg" del dspace desplegado. Este war se tendría que copiar manualmente luego al directorio webapps de nuestro
Tomcat o a otro sitio que queramos para desplegarlo.
Sería necesario reiniciar el Tomcat si está configurado para desplegar de forma automática.

El quinto paso copia el archivo EDMCrosswalk.java de packages a work y el jar de la api de oai de dspace a work.
Se piden datos para parámetros exclusivos de EDM y se modifica el java. Se compila y se agrega al archivo jar. Éste
se ha de copiar manualmente al directorio de librerías donde está desplegado el módulo de oai de dspace.
También se copia el archivo oaicat.properties de dspace a work para añadirle el nuevo crosswalk. Se tendría que copiar
manualmente luego a dspace.
Sería necesario reiniciar el Tomcat o Dspace al realizar este paso.


A medida que se vayan descubriendo errores o se tenga que adaptar a nuevas versiones de Dspace se irán sacando
parches o nuevas versiones.



