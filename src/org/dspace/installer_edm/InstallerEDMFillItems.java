package org.dspace.installer_edm;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 7/02/13
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMFillItems extends InstallerEDMBase implements Observer
{

    private ArrayList<MetadataField> authDCElements;
    private Hashtable<String, Item> cacheAuthValues;

    public InstallerEDMFillItems(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initElementsNotAuthSet();
        initAuthBOHashMap();
        authDCElements = new ArrayList<MetadataField>();
        cacheAuthValues = new Hashtable<String, Item>();
    }


    public void configure()
    {
        if (eperson == null && !loginUser()) return;
        authDCElements.clear();
        cacheAuthValues.clear();
        try {
            if (verbose) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.checkAllSkosAuthElements");
            }
            checkAllSkosAuthElements(authDCElements);
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.auth.num", new String[]{Integer.toString(authDCElements.size())});
            if (authDCElements.size() > 0) {
                installerEDMDisplay.showLn();
                List<MetadataField> metadataFieldList = new ArrayList<MetadataField>();
                int statusElementsDC = showMenuDCElements(metadataFieldList);
                if (statusElementsDC > 0) {
                    List<Collection> collectionList = new ArrayList<Collection>();
                    int statusCollection = showMenuCollections(collectionList);
                    if (statusCollection > 0) {
                        if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.traverseNonauthItems");
                        traverseNonAuthItems(metadataFieldList, collectionList);
                    }
                }
            }
        } catch (SQLException e) {
            showException(e);
        } catch (AuthorizeException e) {
            showException(e);
        }
    }

    private int showMenuCollections(List<Collection> collectionList) throws SQLException
    {
        while (true) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuCollections");
            installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuCollections.menu");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return -1;
            }
            if (response == null) break;
            if (response.isEmpty()) continue;
            response = response.trim();
            if (response.equalsIgnoreCase("a")) {
                return 1;
            } else if (response.equalsIgnoreCase("x")) {
                return 0;
            } else {
                String[] handles = response.split(",");
                for (String handle : handles) {
                    if (handle.matches("^\\d+\\/\\d+$")) {
                        DSpaceObject collection = HandleManager.resolveToObject(context, handle);
                        if (collection == null || collection.getType() != Constants.COLLECTION) installerEDMDisplay.showQuestion
                                (currentStepGlobal,
                                "showMenuCollections.collection.notexist", new String[]{handle});
                        else collectionList.add((Collection) collection);
                    }
                }
                return 2;
            }
        }
        return 0;
    }

    private int showMenuDCElements(List<MetadataField> metadataFieldList)
    {
        while (true) {
            installerEDMDisplay.showLn();
            installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuDCElements");
            installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuDCElements.menu");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                showException(e);
                return -1;
            }
            if (response == null) break;
            if (response.isEmpty()) continue;
            response = response.trim();
            if (response.equalsIgnoreCase("l")) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuDCElements.listdc");
                listAllDCElements((MetadataField[]) authDCElements.toArray(new MetadataField[authDCElements.size()]));
            } else if (response.equalsIgnoreCase("a")) {
                return 1;
            } else if (response.equalsIgnoreCase("x")) {
                return 0;
            } else {
                String[] dcElements = response.split(",");
                for (String dcElement : dcElements) {
                    MetadataField elementObj = findElementDC(dcElement);
                    if (elementObj == null) installerEDMDisplay.showQuestion(currentStepGlobal,
                            "showMenuDCElements.dcelement.notexist", new String[]{dcElement});
                    else metadataFieldList.add(elementObj);
                }
                return 2;
            }
        }
        return 0;
    }

    private void traverseNonAuthItems(List<MetadataField> metadataFieldList, List<Collection> collectionList) throws SQLException, AuthorizeException
    {
        context.clearCache();
        if (metadataFieldList.isEmpty()) metadataFieldList = authDCElements;
        Collection[] listCollections = (!collectionList.isEmpty())?(Collection[]) collectionList.toArray(new Collection[collectionList.size()]):Collection.findAll(context);
        for (Collection collection : listCollections) {
            fillCollection(collection, metadataFieldList);
        }
    }


    private void fillCollection(Collection collection, List<MetadataField> metadataFieldList) throws SQLException
    {
        if (debug) installerEDMDisplay.showQuestion(7, "traverseNonauthItems.collection", new String[]{collection.getName(), collection.getHandle()});
        ItemIterator iter = collection.getAllItems();
        while (iter.hasNext()) {
            installerEDMDisplay.showProgress('.');
            Item item = iter.next();
            if (!AuthorizeManager.isAdmin(context, item)) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.nopermission", new String[]{eperson.getEmail(), item.getHandle()});
                continue;
            }
            boolean itemUpdated = false;
            if (searchSkosAuthItem(item)) continue;
            if (debug) installerEDMDisplay.showQuestion(7, "traverseNonauthItems.item", new String[]{item.getName(), item.getHandle()});
            Map<MetadataField, DCValue[]> metadataField2Clear = new Hashtable<MetadataField, DCValue[]>();
            for (MetadataField metadataField : metadataFieldList) {
                if (checkMetadataFieldIsModifiable(item, metadataField, metadataField2Clear)) itemUpdated = true;
            }
            if (itemUpdated) {
                try {
                    updateItem(item, metadataField2Clear);
                } catch (SQLException e) {
                    showException(e);
                } catch (AuthorizeException e) {
                    showException(e);
                }
            }
        }
    }


    private boolean checkMetadataFieldIsModifiable(Item item, MetadataField metadataField, Map<MetadataField, DCValue[]> metadataField2Clear) throws SQLException
    {
        boolean itemUpdated = false;

        String dcValueName = metadataField.getElement() + ((metadataField.getQualifier() != null && !metadataField.getQualifier().isEmpty())?"." + metadataField.getQualifier():"");
        if (elementsNotAuthSet.contains(dcValueName)) return false;
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
        if (listDCValues.length > 0) {
            for (DCValue dcValue : listDCValues) {
                if (dcValue.authority != null) {
                    if (isValidURI(dcValue.authority)) continue;
                    if (dcValue.authority.matches("^\\d+\\/\\d+$") && HandleManager.resolveToObject(context,
                            dcValue.authority) != null) continue;
                }
                try {
                    if (debug) installerEDMDisplay.showQuestion(7, "traverseNonauthItems.searchNonAuthItems", new String[]{dcValueName, dcValue.value});
                    String handle = searchNonAuthItems(metadataField, dcValue.value);
                    if (handle != null) {
                        if (debug) installerEDMDisplay.showQuestion(7, "traverseNonauthItems.changeitem", new String[]{item.getHandle(), handle});
                        dcValue.authority = handle;
                        itemUpdated = true;
                        if (!metadataField2Clear.containsKey(metadataField)) metadataField2Clear.put(metadataField, listDCValues);
                    }
                } catch (IOException e) {
                    showException(e);
                } catch (AuthorizeException e) {
                    showException(e);
                }
            }
        }
        return itemUpdated;
    }

    private void updateItem(Item item, Map<MetadataField, DCValue[]> metadataField2Clear) throws SQLException, AuthorizeException
    {
        for (MetadataField metadataField : metadataField2Clear.keySet()) {
            item.clearMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
            for (DCValue dcValue : metadataField2Clear.get(metadataField)) {
                System.out.format("%s.%s.%s %s %s %s", dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language, dcValue.value, dcValue.authority);
                item.addMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language, dcValue.value, dcValue.authority, -1);
            }
        }
        item.update();
        context.commit();
    }


    private String searchNonAuthItems(MetadataField metadataField, String value) throws SQLException, IOException, AuthorizeException
    {
        ItemIterator iterAuth = Item.findByMetadataField(context, dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), value);
        while (iterAuth.hasNext()) {
            Item itemMatched = iterAuth.next();
            if (searchSkosAuthItem(itemMatched)) return HandleManager.findHandle(context, itemMatched);
        }
        return null;
    }

    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
