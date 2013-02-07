package org.dspace.installer_edm;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

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
        ArrayList<MetadataField> authDCElements = new ArrayList<MetadataField>();
        try {
            checkAllSkosAuthElements(authDCElements);
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.auth.num", new String[]{Integer.toString(authDCElements.size())});
            if (authDCElements.size() > 0) {
                installerEDMDisplay.showLn();
                traverseNonauthItems(authDCElements);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void traverseNonauthItems(ArrayList<MetadataField> authDCElements) throws SQLException, AuthorizeException
    {
        Collection[] listCollections = Collection.findAll(context);
        for (Collection collection : listCollections) {
            ItemIterator iter = collection.getAllItems();
            while (iter.hasNext()) {
                installerEDMDisplay.showProgress('.');
                Item item = iter.next();
                boolean itemUpdated = false;
                boolean isTypeAuth = false;
                DCValue[] listDCTypeValues = item.getMetadata(dcSchema.getName(), "type", "", language);
                if (listDCTypeValues.length > 0) {
                    for (DCValue dcTypeValue : listDCTypeValues) {
                        if (dcTypeValue.value.equals("SKOS_AUTH")) {
                            isTypeAuth = true;
                            break;
                        }
                    }
                    if (isTypeAuth) continue;
                }
                for (MetadataField metadataField : authDCElements) {
                    listDCTypeValues = item.getMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
                    if (listDCTypeValues.length > 0) {
                        for (DCValue dcTypeValue : listDCTypeValues) {
                            if (dcTypeValue.authority != null && isValidURI(dcTypeValue.authority)) continue;
                            try {
                                String handle = searchNonAuthItems(metadataField, dcTypeValue.value);
                                if (handle != null) {
                                    dcTypeValue.authority = handle;
                                    itemUpdated = true;
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
                    item.update();
                }
            }
        }
    }


    private String searchNonAuthItems(MetadataField metadataField, String value) throws SQLException, IOException, AuthorizeException
    {
        ItemIterator iterAuth = Item.findByMetadataField(context, dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), value);
        if (iterAuth.hasNext()) {
            Item itemMatched = iterAuth.next();
            DCValue[] listItemMatchedDCValues = itemMatched.getMetadata(dcSchema.getName(), "type", "", language);
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
