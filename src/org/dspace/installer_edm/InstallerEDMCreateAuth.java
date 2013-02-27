package org.dspace.installer_edm;

import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 24/10/12
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMCreateAuth extends InstallerEDMBase implements Observer
{

    private ArrayList<MetadataField> authDCElements;


    public InstallerEDMCreateAuth(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initElementsNotAuthSet();
        initAuthBOHashMap();
    }


    public boolean configure()
    {
        if (eperson == null && !loginUser()) return false;
        try {
            if (!AuthorizeManager.isAdmin(context)) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.noadmin", new String[]{eperson.getEmail()});
                return false;
            }
        } catch (SQLException e) {
            showException(e);
        }
        if (dcSchema == null) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notschema", new String[]{DCSCHEMA});
            return false;
        }
        if (metadataFields != null && metadataFields.size() > 0) {
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.numelements", new String[]{DCSCHEMA, Integer.toString(metadataFields.size())});
            if (authDCElements != null) authDCElements.clear();
            else authDCElements = new ArrayList<MetadataField>();
            try {
                checkAllSkosAuthElements(authDCElements);
            } catch (SQLException e) {
                showException(e);
            }
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
                if (response.equalsIgnoreCase("a")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listauth");
                    listAllDCElements(authDCElements);
                    if (authDCElements.size() == 0) installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notauthdcelements");
                } else if (response.equalsIgnoreCase("l")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listdc");
                    listAllDCElements(metadataFields);
                } else if (response.equalsIgnoreCase("n")) {
                    createElementAuth();
                } else if (response.equalsIgnoreCase("x")) {
                    return true;
                }
            }

        } else installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notmetadata");
        return false;
    }


    private boolean createElementAuth()
    {
        String element = null;
        MetadataField elementObj = null;
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
                case 1:
                    if (response.length() == 0) continue;
                    if (response.equalsIgnoreCase("x")) return true;
                    elementObj = findElementDC(response);
                    if (elementObj == null) installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.notexist", new String[]{response});
                    else {
                        element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");
                        if (!metadataAuthorityManager.isAuthorityControlled(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier())) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.nonauthcontrolled", new String[]{element});
                            installerEDMDisplay.showLn();
                            element = null;
                        } else {
                            step++;
                        }
                    }
                    break;
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
            if (step == 4 && element != null && community != null && collection != null) {
                if (!authDCElements.contains(elementObj)) authDCElements.add(elementObj);
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.create", new String[] {community, collection});
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
                    else return fillAuthItems(elementObj, communityObj, collectionObj);
                } while(true);
                break;
            }
        }
        return false;
    }

    private boolean fillAuthItems(MetadataField elementObj, Community communityObj, Collection collectionObj)
    {
        try {
            int numItemsModified = 0;
            String element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");
            Collection[] listCollections = Collection.findAll(context);
            for (Collection collection : listCollections) {
                if (collection.getID() == collectionObj.getID()) continue;
                Community[] listCommunities = collection.getCommunities();
                if (communityObj.getID() == listCommunities[0].getID()) continue;
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.getitems", new String[]{collection.getName()});
                ItemIterator iter = collection.getAllItems();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    if (searchSkosAuthItem(item)) continue;
                    DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), language);
                    if (listDCValues.length > 0) {
                        for (DCValue dcValue : listDCValues) {
                            if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.additem", new String[]{dcValue.value});
                            boolean isAuth = false;
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
                            if (isAuth) continue;
                            if (createAuth(elementObj, communityObj, collectionObj, element, dcValue)) numItemsModified++;
                        }
                    } else if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.collection.item.noelement", new String[] {collection.getName(), item.getHandle(), element, language});
                }
            }
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.numItemsModified", new String[]{element, Integer.toString(numItemsModified)});
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


    private boolean createAuth(MetadataField elementObj, Community communityObj, Collection collectionObj, String element, DCValue dcValue) throws SQLException, IOException, AuthorizeException
    {
        Item itemAuth = null;
        WorkspaceItem wi = WorkspaceItem.create(context, collectionObj, false);
        itemAuth = wi.getItem();
        InstallItem.installItem(context, wi, null);
        String myhandle = HandleManager.findHandle(context, itemAuth);
        if (myhandle.equals(itemAuth.getHandle())) {
            if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.addmetadata", new String[] {element, language, dcValue.value, itemAuth.getHandle()});
            itemAuth.addMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), language, new String[] {dcValue.value}, new String[] {itemAuth.getHandle()}, null);
            itemAuth.addMetadata(dcSchema.getName(), "type", null, language, new String[] {"SKOS_AUTH"}, null, null);
            //collectionObj.addItem(itemAuth);
            itemAuth.update();
            InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(itemAuth, communityObj, collectionObj, dcSchema, elementObj);
            authBOHashMap.put(element, installerEDMAuthBO);
            context.commit();
            return true;
        } else {
            installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.item.mismatch", new String[] {String.valueOf(itemAuth.getID()), itemAuth.getHandle(), myhandle});
            return false;
        }
    }


    private int askCommunity(String response, int step, Object[] argv)
    {
        Community communityObj = (Community) argv[0];
        String community = (String) argv[1];
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


    private int askCollection(String response, int step, Community communityObj, Object[] argv)
    {
        org.dspace.content.Collection collectionObj = (org.dspace.content.Collection) argv[0];
        String collection = (String) argv[1];
        if (response.length() == 0) {
            response = null;
            int stepCollection = 1;
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


    public ArrayList<MetadataField> getAuthDCElements()
    {
        return authDCElements;
    }


    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
