package org.dspace.installer_edm;

import com.sun.jndi.toolkit.dir.ContextEnumerator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * @class InstallerEDMCreateAuth
 *
 * Clase que crea autoridades como ítems en la base de datos de dspace. Los elementos dc que constituyen los ítems creados
 * son type con valor SKOS_AUTH y el elemento dc que se quiere indexar como autoridad.
 * Se pide la comunidad y la colección en las que se quieren crear las autoridades.
 * En cada ítem, en el campo autoridad de la tabla metadatavalue se introduce el handle del ítem recién creado
 * Extiende la clase base {@link InstallerEDMBase}
 *
 */
public class InstallerEDMCreateAuth extends InstallerEDMBase implements Observer
{

    /**
     * lista de elementos dc de las autoridades
     */
    private ArrayList<MetadataField> authDCElements;

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerEDMCreateAuth(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initAuthBOHashMap();
    }

    /**
     * Se ha de validar como usuario de dspace con privilegios de administrador.
     * Muestra los elementos dc que son de autoridades actualmente, y pregunta cuáles más se quieren crear.
     * Se recorre todas las colecciones que no son de autoridad para recoger los valores de los elementos dc
     * que se quiere crear como autoridad y crea ítems para cada valor distinto.
     *
     * @return éxito de la operación
     */
    public boolean configure()
    {
        // validación y comprobar privilegios de admin
        if (eperson == null && !loginUser()) return false;
        try {
            if (!AuthorizeManager.isAdmin(context)) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.noadmin", new String[]{eperson.getEmail()});
                return false;
            }
        } catch (SQLException e) {
            showException(e);
        }
        // comprobar esquema
        if (dcSchema == null) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notschema", new String[]{DCSCHEMA});
            return false;
        }
        // tenemos lista de elementos dc
        if (metadataFields != null && metadataFields.size() > 0) {
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.numelements", new String[]{DCSCHEMA, Integer.toString(metadataFields.size())});
            // recoger elementos dc de autoridades
            if (authDCElements != null) authDCElements.clear();
            else authDCElements = new ArrayList<MetadataField>();
            try {
                checkAllSkosAuthElements(authDCElements);
            } catch (SQLException e) {
                showException(e);
            }
            // menú de listado de elementos dc
            installerEDMDisplay.showLn();
            while (true) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.menu");
                String response;
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    showException(e);
                    return false;
                }
                if (response == null) break;
                if (response.length() == 0) continue;
                response = response.trim();
                // listar elementos dc de autoridades
                if (response.equalsIgnoreCase("a")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listauth");
                    listAllDCElements(authDCElements);
                    if (authDCElements.size() == 0) {
                        installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notauthdcelements");
                        installerEDMDisplay.showLn();
                    }
                // listar elementos dc de autoridades en dspace.cfg
                } else if (response.equalsIgnoreCase("d")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listauth.dspace");
                    listAllStrings(new ArrayList<String>(elementsAuthDspaceCfg));
                // listar elementos dc
                } else if (response.equalsIgnoreCase("l")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listdc");
                    listAllDCElements(metadataFields);
                // elegir elemento dc para crear autoridad
                } else if (response.equalsIgnoreCase("n")) {
                    createElementAuth();
                // salir
                } else if (response.equalsIgnoreCase("x")) {
                    return true;
                }
            }

        } else installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notmetadata");
        return false;
    }

    /**
     * Crear la autoridad para el elemento dc escogido
     * Se pide a qué comunidad y colección se quieren asociar los nuevos ítems
     *
     *
     * @return éxito de la operación
     */
    private boolean createElementAuth()
    {
        String element = null;
        ArrayList<MetadataField> listElementsObj = null;
        String community = null;
        Community communityObj = null;
        String collection = null;
        org.dspace.content.Collection collectionObj = null;
        int step = 1;

        while (true) {
            if (element == null)
                installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.dc.element");
            else if (community == null)
                installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.handle.community");
            else if (collection == null)
                installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.handle.collection");
            String response;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return false;
            }
            if (response == null) break;
            response = response.trim();
            switch (step) {
                // elemento dc escogido, se mira que no esté en dspace.cfg controlado como autoridad
                case 1:
                    if (response.length() == 0) continue;
                    if (response.equalsIgnoreCase("x")) return true;

                    // busca los elementos dc con cierto nombre y calificador
                    listElementsObj = findElementsDC(response);
                    if (listElementsObj == null) installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.notexist", new String[]{response});
                    else {
                        boolean elementOk = false;
                        for (int i= listElementsObj.size() - 1; i >=0 ; i--) {
                            MetadataField elementObj = listElementsObj.get(i);
                            element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");

                            // no esté en dspace.cfg controlado como autoridad
                            if (!metadataAuthorityManager.isAuthorityControlled(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier()) && !elementsAuthDspaceCfg.contains(element)) {
                                installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.nonauthcontrolled", new String[]{element});
                                installerEDMDisplay.showLn();
                                listElementsObj.remove(i);
                            } else if (elementsNotAuthSet.contains(element)) {
                                installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.notallowed", new String[]{element});
                                installerEDMDisplay.showLn();
                                listElementsObj.remove(i);
                            } else {
                                elementOk = true;
                            }
                        }
                        if (elementOk) step++;
                    }
                    break;
                // comunidad
                case 2:
                    Object[] argv = new Object[2];
                    argv[0] = communityObj;
                    argv[1] = community;
                    step = askCommunity(response, step, argv);
                    if (step == 3) {
                        communityObj = (Community) argv[0];
                        community = (String) argv[1];
                    }
                    break;
                // colección
                case 3:
                    Object[] argvCol = new Object[2];
                    argvCol[0] = collectionObj;
                    argvCol[1] = collection;
                    step = askCollection(response, step, communityObj, argvCol);
                    if (step == 4) {
                        collectionObj = (Collection) argvCol[0];
                        collection = (String) argvCol[1];
                    }
                    break;
            }
            // se crea la autoridad, se recorren todas colecciones para buscar valores para esos elementos dc y crear ítems
            if (step == 4 && listElementsObj.size() > 0 && community != null && collection != null) {
                for (MetadataField elementObj: listElementsObj)
                    if (!authDCElements.contains(elementObj)) authDCElements.add(elementObj);
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.create", new String[] {community, collection, language});
                    response = null;
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        showException(e);
                        return false;
                    }
                    if (response == null) break;
                    response = response.trim();
                    if (response.length() == 0) response = answerYes;
                    if (response.equalsIgnoreCase("n")) break;
                    else return fillAuthItems(listElementsObj.toArray(new MetadataField[listElementsObj.size()]), communityObj, collectionObj);
                } while(true);
                break;
            }
        }
        return false;
    }

    /**
     * Recorre todas las colecciones para recoger valores para el elemento dc de la autoridad para crear ítems con cada valor distinto
     *
     * @param elementsObj array de objetos elemento dc de dspace {@link MetadataField}
     * @param communityObj objeto comunidad de dspace {@link Community}
     * @param collectionObj objeto colección de dspace {@link Collection}
     * @return éxito de la operación
     */
    private boolean fillAuthItems(MetadataField[] elementsObj, Community communityObj, Collection collectionObj)
    {
        try {
            int numItemsModified = 0;

            // listar todas las colecciones
            Collection[] listCollections = Collection.findAll(context);

            for (Collection collection : listCollections) {
                // colección distinta a la que albergará las autoridades
                if (collection.getID() == collectionObj.getID()) continue;

                // comunidad de la colección
                Community[] listCommunities = collection.getCommunities();
                if (communityObj.getID() == listCommunities[0].getID()) continue;
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.getitems", new String[]{collection.getName()});

                // ítems de la colección
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    // si este ítem es una autoridad lo saltamos
                    if (searchSkosAuthItem(item)) continue;

                    // recogemos valores de los campos
                    for (MetadataField elementObj: elementsObj) {
                        String element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");
                        // recogemos los elementos dc del ítem
                        DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), Item.ANY);
                        if (listDCValues.length > 0) {
                            for (DCValue dcValue : listDCValues) {
                                if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.additem", new String[]{dcValue.value});
                                boolean isAuth = false;

                                // buscamos ítems de autoridad con este elemento dc y ese valor
                                ItemIterator iterAuth = InstallerEDMDAO.findByMetadataField(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), dcValue.value);
                                if (iterAuth != null) {
                                    while (iterAuth.hasNext()) {
                                        Item itemMatched = iterAuth.next();
                                        if (searchSkosAuthItem(itemMatched)) {
                                            if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.canceladd");
                                            isAuth = true;
                                            break;
                                        }
                                    }
                                    iterAuth.close();
                                }
                                // si ya existe una autoridad se salta este valor
                                if (isAuth) continue;

                                // crea el nuevo ítem para la autoridad
                                if (createAuth(elementObj, communityObj, collectionObj, element, dcValue)) numItemsModified++;
                            }
                        } else if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.collection.item.noelement", new String[] {collection.getName(), item.getHandle(), element, language});
                    }
                }
            }
            if (verbose) {
                String element = "";
                if (elementsObj.length > 1) {
                    element = elementsObj[0].getElement() + ".*";
                } else element = elementsObj[0].getElement() + ((elementsObj[0].getQualifier() != null)?"." + elementsObj[0].getQualifier():"");
                installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.numItemsModified", new String[]{
                        element, Integer.toString(numItemsModified)});
            }
            installerEDMDisplay.showLn();
            return true;
        } catch (SQLException e) {
            showException(e);
        } catch (AuthorizeException e) {
            showException(e);
        } catch (IOException e) {
            showException(e);
        }
        return false;
    }

    /**
     * Creación del nuevo ítem de autoridad en la base de datos de dspace
     * se añade nuevo elemento dc para la autoridad
     * añade el elemento dc type con valor SKOS_AUTH
     *
     * @param elementObj objeto elemento dc de dspace {@link MetadataField}
     * @param communityObj objeto comunidad de dspace {@link Community}
     * @param collectionObj objeto colección de dspace {@link Collection}
     * @param element nombre del elemento dc
     * @param dcValue valores del elemento dc {@link DCValue}
     * @return éxito de la operación
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private boolean createAuth(MetadataField elementObj, Community communityObj, Collection collectionObj, String element, DCValue dcValue) throws SQLException, IOException, AuthorizeException
    {
        Item itemAuth = null;
        // espacio de trabajo de dspace para manipular ítems
        WorkspaceItem wi = WorkspaceItem.create(context, collectionObj, false);
        // creación de ítem en dspace
        itemAuth = wi.getItem();
        InstallItem.installItem(context, wi, null);
        // handle del nuevo ítem
        String myhandle = HandleManager.findHandle(context, itemAuth);
        if (myhandle.equals(itemAuth.getHandle())) {
            if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.addmetadata", new String[] {element, language, dcValue.value, itemAuth.getHandle()});
            // se añade nuevo elemento dc para la autoridad
            itemAuth.addMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), language, new String[] {dcValue.value}, new String[] {itemAuth.getHandle()}, null);
            // se añade el title con el valor del nuevo elemento dc
            if (!(elementObj.getElement().equals("title") && elementObj.getQualifier() == null))
                itemAuth.addMetadata(dcSchema.getName(), "title", null, language, new String[] {dcValue.value}, null, null);
            // añade el elemento dc type con valor SKOS_AUTH
            itemAuth.addMetadata(dcSchema.getName(), "type", null, language, new String[] {"SKOS_AUTH"}, null, null);
            //collectionObj.addItem(itemAuth);
            // guardar ítem en bbdd
            itemAuth.update();
            InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(itemAuth, communityObj, collectionObj, dcSchema, elementObj);
            // crear POJO con valores del ítem
            authBOHashMap.put(element, installerEDMAuthBO);
            context.commit();
            return true;
        } else {
            installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.item.mismatch", new String[] {String.valueOf(itemAuth.getID()), itemAuth.getHandle(), myhandle});
            return false;
        }
    }

    /**
     * Pide la comunidad (su handle) de dspace donde irá la nueva autoridad o la posibilidad de crear una nueva
     *
     * @param response cadena con la respuesta de la pregunta
     * @param step paso actual de la operación, si todo va bien se incrementa para continuar con la colección
     * @param argv array para paso por referencia del objeto comunidad y de su nombre
     * @return paso de la operación
     */
    private int askCommunity(String response, int step, Object[] argv)
    {
        Community communityObj = (Community) argv[0];
        String community = (String) argv[1];
        // creación de la comunidad
        if (response.length() == 0) {
            response = null;
            int stepCommunity = 1;
            DSpaceObject communityObjParent = null;
            while (true) {
                if (stepCommunity > 2) break;
                if (stepCommunity == 1) installerEDMDisplay.showQuestion(currentStepGlobal, "askCommunity.handle");
                else if (stepCommunity == 2) installerEDMDisplay.showQuestion(currentStepGlobal, "askCommunity.name");
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    showException(e);
                    return 2;
                }
                if (response == null) break;
                response = response.trim();
                if (stepCommunity > 1 && response.length() == 0) continue;
                switch(stepCommunity) {
                    // verfificar existencia de la comunidad
                    case 1:
                        try {
                            if (response.length() > 0) {
                                communityObjParent = HandleManager.resolveToObject(context, response);
                                if (communityObjParent != null && communityObjParent instanceof Community && communityObjParent.getType() == Constants.COMMUNITY) {
                                    stepCommunity++;
                                }
                            } else stepCommunity++;
                        } catch (SQLException e) {
                            showException(e);
                        }
                        break;
                    // creación de la comunidad
                    case 2:
                        try {
                            communityObj = Community.create((Community) communityObjParent, context);
                            communityObj.setMetadata("name", response);
                            communityObj.update();
                            context.commit();
                            stepCommunity = 3;
                            community = communityObj.getName();
                            step++;
                        } catch (SQLException e) {
                            showException(e);
                        } catch (AuthorizeException e) {
                            showException(e);
                        } catch (IOException e) {
                            showException(e);
                        }
                        break;
                }
            }
        // verfificar existencia de la comunidad
        } else {
            try {
                DSpaceObject communityObjAux = HandleManager.resolveToObject(context, response);
                if (communityObjAux != null && communityObjAux instanceof org.dspace.content.Community) {
                    communityObj = (Community) communityObjAux;
                    community = communityObj.getName();
                    step++;
                } else {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "askCommunity.notexist", new String[]{response});
                }
            } catch (SQLException e) {
                showException(e);
            }
        }
        argv[0] = communityObj;
        argv[1] = community;
        return step;
    }

    /**
     * Pide la colección (su handle) de dspace donde irá la nueva autoridad o la posibilidad de crear una nueva
     *
     * @param response cadena con la respuesta de la pregunta
     * @param step paso actual de la operación, si todo va bien se incrementa para continuar con recorrer colecciones
     * @param communityObj objeto de la comunidad de dspace {@link Community} asociada a la colección
     * @param argv array para paso por referencia del objeto colección y de su nombre
     * @return paso de la operación
     */
    private int askCollection(String response, int step, Community communityObj, Object[] argv)
    {
        org.dspace.content.Collection collectionObj = (org.dspace.content.Collection) argv[0];
        String collection = (String) argv[1];
        // creación de la colección
        if (response.length() == 0) {
            response = null;
            int stepCollection = 2;
            DSpaceObject collectionObjHandle = null;
            while (true) {
                if (stepCollection > 2) break;
                if (stepCollection == 1) installerEDMDisplay.showQuestion(currentStepGlobal, "askCollection.handle");
                else if (stepCollection == 2) installerEDMDisplay.showQuestion(currentStepGlobal, "askCollection.name");
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    showException(e);
                    return 3;
                }
                if (response == null) break;
                response = response.trim();
                if (stepCollection > 1 && response.length() == 0) continue;
                switch (stepCollection) {
                    // verfificar existencia de la colección
                    case 1:
                        try {
                            if (response.length() > 0) {
                                collectionObjHandle = HandleManager.resolveToObject(context, response);
                                if (collectionObjHandle != null && collectionObjHandle instanceof org.dspace.content.Collection && collectionObjHandle.getType() == Constants.COLLECTION) {
                                    collectionObj = (org.dspace.content.Collection) collectionObjHandle;
                                    collection = collectionObj.getName();
                                    stepCollection = 3;
                                    step++;
                                }
                            } else stepCollection++;
                        } catch (SQLException e) {
                            showException(e);
                        }
                        break;
                    // creación de la colección
                    case 2:
                        try {
                            collectionObj = communityObj.createCollection();
                            collectionObj.setMetadata("name", response);
                            collectionObj.update();
                            context.commit();
                            stepCollection++;
                            collection = collectionObj.getName();
                            step++;
                        } catch (SQLException e) {
                            showException(e);
                        } catch (AuthorizeException e) {
                            showException(e);
                        } catch (IOException e) {
                            showException(e);
                        }
                        break;
                }
            }
            // verfificar existencia de la colección
        } else {
            try {
                DSpaceObject collectionObjAux = HandleManager.resolveToObject(context, response);
                if (collectionObjAux != null && collectionObjAux instanceof org.dspace.content.Collection) {
                    collectionObj = (org.dspace.content.Collection) collectionObjAux;
                    collection = collectionObj.getName();
                    step++;
                } else {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "askCollection.notexist", new String[]{response});
                }
            } catch (SQLException e) {
                showException(e);
            }
        }
        argv[0] = collectionObj;
        argv[1] = collection;
        return step;
    }

    /**
     * Devuelve el listado de elementos dc de las autoridades
     *
     * @return listado de elementos dc de las autoridades
     */
    public ArrayList<MetadataField> getAuthDCElements()
    {
        return authDCElements;
    }

    /**
     * Terminar instalador tras recibir señal
     *
     * @param o observable
     * @param arg señal
     */
    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
