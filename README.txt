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
    DIM2EDM.xsl .............................. Xsl schema for EDM PluginCrosswalk.
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
    ojdbc14-10.2.0.2.0.jar ................... Library required for installer.
    postgresql-8.1-408.jdbc3.jar ............. Library required for installer.


There are two scripts to run the installer with the libraries path configured correctly and more pertinent checks.

For Linux/Unix: install.sh
For Windows: install.bat

Thw way to run the installer is:

Linux/Unix:

$ bash ./install.sh -d dspace_deployed_dir -t tomcat_base_physycal_dir -v -s 0 -l en

View the help:
$ ./install.sh -h


Windows:

drive:\path2installer\install.bat -d dspace_deployed_dir -t tomcat_base_physycal_dir -v -s 0 -l en

View the help:
$ ./install.bat -h

Result: Use: install.sh: [-d dir_space_runtime] [-j path_java_command] [-h] [-l language] [-s step] [-t dir_tomcat_base] [-v] [-g]


The options are:

-d: path to root dir where dspace is deployed.
-j: path to java command
-t: path to Tomcat root dir.
-s: step to launch. 0 is preferred to see the main menu first.
-v: verbosity.
-g: show debug messages
-l: Messages language. If blank the systema default locale will be taken.
    Actually spanish and english is supported. For spanish: es_ES.
    For english: en.

Finally the jar file of the installer InstallerEDM.jar, with execution permissions. It might be runned manually, but the
command line would be something like (the directories are fake):

$ java -cp ./lib/commons-cli-1.2.jar:./lib/commons-codec-1.7.jar:./lib/commons-io-2.4.jar:./lib/lanterna-2.1.1.jar:./lib/normalizer.jar:./lib/postgresql-8.1-408.jdbc3.jar:/home/europeana/runtime//lib/activation-1.1.jar:/home/europeana/runtime//lib/ant-1.7.0.jar:/home/europeana/runtime//lib/ant-launcher-1.7.0.jar:.. many more libraries ..:InstallerEDM.jar:.:/home/europeana/runtime/config org.dspace.installer_edm.InstallerEDM -t /usr/share/tomcat5.5 -s 0  -v -l es_ES

evidently is impratical, so it's much better run it from the scripts
The options for the jar file are the same than the scripts ones.

As the installer modifies the Dspace relational database to create communities, collections and items, it's
mandatory to make a backup to be able to restore the original one.
It's necessary to be validated as a dspace user with permission to create communities, collections and items to be able to
create the authorities.
It's covenient to reindex Solr to merge and commit all the changes.

Askosi application makes possible to link an authority with an item through the cataloging process, the handle from the
authority with the same dc element than the item will be stored in the authority field of the metadatavalue table from
the dspace relational database.
EDMExport and EDMCrosswalk apps to be able to build the SKOS class on the EDM schema check whether the authority field of the item has a
nonempty value, if it's a valid url it's showed as it is, if it's a handle checks that it exists on the database and
build a final url with the dspace base url and the handle.

Installer has the next steps:

1: Installing Askosi
2: Configuring Dspace: file dspace.cfg
3: Create Auth Items
4: Configuring dspace: input-forms.xml and askosi
5: Configure EDMExport
6: Configure EDMCrosswalk
7: Configure EDMCrosswalk with xsl
8: Fill skos:about items with values of authorities
9: Exit


First step installs Askosi app in the server. All the files required are taken from the packages dir of the installer.
The Servlet container is mandatory to be restarted and the installer thereafter.


Second step copies dspace configuraction file "dspace.cfg" to a directory
called "work" located in the installer directory.
It will ask whether more authority elements will be added to the files
dspace.cfg.
This file will be modified with the new properties. It's now a task for the admin to check this file.
The admin will copy manually the files to dspace to deploy them.
The Servlet container is mandatory to be restarted and the installer thereafter.


Third step creates communities, collections and items for the authorities and are fullfilled with values searched
from the items of collections which no possess authorities
It's mandatory to be validated as a dspace user.
For every dc element required as an authority is necessary to link it to a community and a collection. If they don't exist
they'll have to be created.
These dc elements have to exist in the file "dspace.cfg" and be authority controlled.
They are searched on the collections with no authorities and fullfilled with their unique values.
The Servlet container is not required to be restarted.


Fourth step copies cataloguing file "input-forms.xml" to a directory
called "work" located in the installer directory.
his file will be modified with the new properties. It's now a task for the admin to check this file.
The admin will copy manually the files to dspace to deploy them.
The Servlet container is mandatory to be restarted and the installer thereafter.


Fifth step copies the EDMExport.war file from packages to work and modifies it to add the path of the dspace.cfg deployed.
The final war file has to be copied manually to be deployed.
The Servlet container is mandatory to be restarted and the installer thereafter.


Sixth step copies the EDMCrosswalk.java file from packages to work and the dspace oai api file to work.
A set of edm specific parameters will be asked and the java file modified:
An url for the element edm.rights.
For the edm.types words to be matched against the dc.types values and replaced by their type.
The java file is compiled and add to the jar file.
The jar file must be copied manually to the dir where the libraries of the oai module is deployed.
The oaicat.properties will be copied from dspace to work to add the new crosswalk property.
This file will have to be copied manually to dspace.
The Servlet container is mandatory to be restarted and the installer thereafter.
This step is incompatible with "Configure EDMCrosswalk with xsl" one.


Seventh step copies the "dspace.cfg" file from dspace to work.
The file "DIM2EDM.xsl" will be copied from packages to work.
The oaicat.properties will be copied from dspace to work to add the new crosswalk property.
The file "DIM2EDM.xsl" will be configured with edm specific elements:
An url that uses dspace as root for the handles, default: http://hdl.handle.net/
A root url to create the links to the handles: e.g.: http://example.com/bitstream/handle/
Values for elements: edm:dataProvider, edm:provider, edm:language
An url for edm:rights
Values for edm:type, allowed values are: TEXT, VIDEO, IMAGE, SOUND, 3D
The file dspace.cfg will be modified to add this dissemination crosswalk.
The file oaicat.properties will be modified to add this dissemination crosswalk.
These files will have to be copied manuall to dspace.
The file DIM2EDM.xsl will have to be copied manually to the dspace crosswalk directory.
The Servlet container is mandatory to be restarted and the installer thereafter.
This step is incompatible with "Configure EDMCrosswalk" one.


Eighth step traverse all the colecctions with authority items to collect the dc elements.
It will traverse the collections with nonauthority items to collect the items with the dc elements from the former traversing.
If there's a match the handle of the authority will be stored in the field authority of the table metadatavalue in the row belonging
to the item.
The Servlet container is not required to be restarted.






