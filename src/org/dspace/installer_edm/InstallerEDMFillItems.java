package org.dspace.installer_edm;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
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

    public InstallerEDMFillItems(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initElementsNotAuthSet();
        initAuthBOHashMap();
    }


    public void configure()
    {
        if (eperson == null && !loginUser()) return;
        ArrayList<MetadataField> authDCElements = new ArrayList<MetadataField>();
        try {
            if (verbose) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.checkAllSkosAuthElements");
            }
            checkAllSkosAuthElements(authDCElements);
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.auth.num", new String[]{Integer.toString(authDCElements.size())});
            if (authDCElements.size() > 0) {
                installerEDMDisplay.showLn();
                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.traverseNonauthItems");
                traverseNonauthItems(authDCElements);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        }
    }

    private void traverseNonauthItems(ArrayList<MetadataField> authDCElements) throws SQLException, AuthorizeException
    {
        Collection[] listCollections = Collection.findAll(context);
        for (Collection collection : listCollections) {
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
                if (debug) installerEDMDisplay.showQuestion(7, "traverseNonauthItems.item", new String[]{item.getName(), item.getHandle()});
                Map<MetadataField, DCValue[]> metadataField2Clear = new HashMap<MetadataField, DCValue[]>();
                for (MetadataField metadataField : authDCElements) {
                    String dcValueName = metadataField.getElement() + ((metadataField.getQualifier() != null && !metadataField.getQualifier().isEmpty())?"." + metadataField.getQualifier():"");
                    if (elementsNotAuthSet.contains(dcValueName)) continue;
                    DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
                    if (listDCValues.length > 0) {
                        for (DCValue dcValue : listDCValues) {
                            if (dcValue.authority != null && isValidURI(dcValue.authority)) continue;
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
                                e.printStackTrace();
                            } catch (AuthorizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (itemUpdated) {
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
            }
        }
    }


    private String searchNonAuthItems(MetadataField metadataField, String value) throws SQLException, IOException, AuthorizeException
    {
        ItemIterator iterAuth = Item.findByMetadataField(context, dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), value);
        while (iterAuth.hasNext()) {
            Item itemMatched = iterAuth.next();
            DCValue[] listItemMatchedDCValues = itemMatched.getMetadata(dcSchema.getName(), "type", null, language);
            if (listItemMatchedDCValues.length > 0) {
                for (DCValue dcValueMatched : listItemMatchedDCValues) {
                    if (dcValueMatched.value.equals("SKOS_AUTH")) {
                        return HandleManager.findHandle(context, itemMatched);
                    }
                }
            }
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
