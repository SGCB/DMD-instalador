package org.dspace.installer_edm;

import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
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
    }


    public boolean createAuth()
    {
        if (eperson == null && !loginUser()) return false;
        if (dcSchema == null) {
            installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.notschema", new String[]{DCSCHEMA});
            return false;
        }
        try {
            checkDspaceMetadataDC();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (metadataFields != null && metadataFields.length > 0) {
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.numelements", new String[]{DCSCHEMA, Integer.toString(metadataFields.length)});
            if (authDCElements != null) authDCElements.clear();
            else authDCElements = new ArrayList<MetadataField>();
            initAuthBOHashMap();
            while (true) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.menu");
                String response;
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                if (response == null) break;
                if (response.length() == 0) continue;
                response = response.trim();
                if (response.equalsIgnoreCase("a")) {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createAuth.listauth");
                    MetadataField[] authArray = new MetadataField[authDCElements.size()];
                    listAllDCElements((MetadataField[])authDCElements.toArray(authArray));
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
                e.printStackTrace();
                return false;
            }
            if (response == null) break;
            response = response.trim();
            switch (step) {
                case 1:
                    if (response.length() == 0) continue;
                    if (response.equalsIgnoreCase("x")) return true;
                    int pos = response.indexOf(".");
                    try {
                        MetadataField elementMD = MetadataField.findByElement(context, dcSchema.getSchemaID(), (pos > 0)?response.substring(0, pos - 1):response, (pos > 0)?response.substring(pos + 1):null);
                        if (elementMD == null) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.element.notexist", new String[]{response});
                        } else {
                            elementObj = elementMD;
                            element = elementMD.getElement() + ((elementMD.getQualifier() != null)?"." + elementMD.getQualifier():"");
                            step++;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (AuthorizeException e) {
                        e.printStackTrace();
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
                authDCElements.add(elementObj);
                do {
                    installerEDMDisplay.showQuestion(currentStepGlobal, "createElementAuth.create", new String[] {community, collection});
                    response = null;
                    try {
                        response = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    if (response == null) break;
                    response = response.trim();
                    if (response.length() == 0) response = "y";
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
                    boolean isTypeAuth = false;
                    DCValue[] listDCTypeValues = item.getMetadata(dcSchema.getName(), "type", null, language);
                    if (listDCTypeValues.length > 0) {
                        for (DCValue dcTypeValue : listDCTypeValues) {
                            if (dcTypeValue.value.equals("SKOS_AUTH")) {
                                isTypeAuth = true;
                                break;
                            }
                        }
                        if (isTypeAuth) continue;
                    }
                    DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), language);
                    if (listDCValues.length > 0) {
                        for (DCValue dcValue : listDCValues) {
                            if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.additem", new String[]{dcValue.value});
                            ItemIterator iterAuth = Item.findByMetadataField(context, dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), dcValue.value);
                            if (iterAuth.hasNext()) {
                                Item itemMatched = iterAuth.next();
                                DCValue[] listItemMatchedDCValues = itemMatched.getMetadata(dcSchema.getName(), "type", null, language);
                                if (listItemMatchedDCValues.length > 0) {
                                    boolean repeated = false;
                                    for (DCValue dcValueMatched : listItemMatchedDCValues) {
                                        if (dcValueMatched.value.equals("SKOS_AUTH")) {
                                            repeated = true;
                                            break;
                                        }
                                    }
                                    if (repeated) {
                                        if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.canceladd");
                                        continue;
                                    }
                                }
                            }
                            Item itemAuth = null;
                            WorkspaceItem wi = WorkspaceItem.create(context, collectionObj, false);
                            itemAuth = wi.getItem();
                            InstallItem.installItem(context, wi, null);
                            String myhandle = HandleManager.findHandle(context, itemAuth);
                            if (myhandle.equals(itemAuth.getHandle())) {
                                if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.addmetadata", new String[] {element, language, dcValue.value});
                                itemAuth.addMetadata(dcSchema.getName(), elementObj.getElement(), elementObj.getQualifier(), language, new String[] {dcValue.value}, new String[] {itemAuth.getHandle()}, null);
                                itemAuth.addMetadata(dcSchema.getName(), "type", null, language, new String[] {"SKOS_AUTH"}, null, null);
                                //collectionObj.addItem(itemAuth);
                                itemAuth.update();
                                InstallerEDMAuthBO installerEDMAuthBO = new InstallerEDMAuthBO(itemAuth, communityObj, collectionObj, dcSchema, elementObj);
                                authBOHashMap.put(element, installerEDMAuthBO);
                                context.commit();
                            } else {
                                installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.item.mismatch", new String[] {String.valueOf(itemAuth.getID()), itemAuth.getHandle(), myhandle});
                            }
                        }
                    } else if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "fillAuthItems.collection.item.noelement", new String[] {collection.getName(), item.getHandle(), element, language});
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
                    e.printStackTrace();
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
                                if (communityObjParent != null && communityObjParent instanceof Community) {
                                    stepCommunity++;
                                }
                            } else stepCommunity++;
                        } catch (SQLException e) {
                            e.printStackTrace();
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
                            e.printStackTrace();
                        } catch (AuthorizeException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
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
                e.printStackTrace();
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
                    e.printStackTrace();
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
                                if (collectionObjHandle != null && collectionObjHandle instanceof org.dspace.content.Collection) {
                                    collectionObj = (org.dspace.content.Collection) collectionObjHandle;
                                    collection = collectionObj.getName();
                                    stepCollection = 3;
                                    step++;
                                }
                            } else stepCollection++;
                        } catch (SQLException e) {
                            e.printStackTrace();
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
                            e.printStackTrace();
                        } catch (AuthorizeException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
        argv[0] = collectionObj;
        argv[1] = collection;
        return step;
    }


    private void listAllDCElements(MetadataField[] arrFields)
    {
        int i = 1;
        for (MetadataField metadataField : arrFields) {
            String qualifier = metadataField.getQualifier();
            int elementLength = metadataField.getElement().length() + ((qualifier != null)?qualifier.length() + 1:0);
            int padding = 80 - elementLength;
            if (i % 2 == 1) {
                System.out.printf("%s%s%" + padding + "s", metadataField.getElement(), (qualifier != null)?"."+qualifier:"", " ");
            } else {
                System.out.printf("%s%s", metadataField.getElement(), (qualifier != null)?"."+qualifier:"");
                System.out.printf("%n");
            }
            i++;
        }
        System.out.flush();
        System.out.println("");
        System.out.println("");
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
