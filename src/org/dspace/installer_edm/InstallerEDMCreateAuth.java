package org.dspace.installer_edm;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 24/10/12
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMCreateAuth implements Observer
{
    private String DspaceDir = null;
    private String TomcatBase = null;
    private boolean verbose = false;

    private final String DCSCHEMA = "http://dublincore.org/documents/dcmi-terms/";
    private InputStreamReader isr;
    private BufferedReader br;

    private MetadataSchema dcSchema;
    private Context context;
    private MetadataField[] metadataFields;
    private ArrayList<MetadataField> authDCElements;


    public InstallerEDMCreateAuth(String DspaceDir, String TomcatBase, boolean verbose)
    {
        this.DspaceDir = DspaceDir;
        this.TomcatBase = TomcatBase;
        this.verbose = verbose;
        try {
            isr = new InputStreamReader(System.in);
            br = new BufferedReader(isr);
            checkDspaceMetadataDC();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkDspaceMetadataDC() throws SQLException, NullPointerException
    {
        context = new Context();
        dcSchema = MetadataSchema.findByNamespace(context, DCSCHEMA);
        metadataFields = MetadataField.findAllInSchema(context, dcSchema.getSchemaID());
    }

    public boolean createAuth()
    {
        if (dcSchema == null) {
            System.out.println("There are not schema " + DCSCHEMA);
            return false;
        }
        if (metadataFields != null && metadataFields.length > 0) {
            if (verbose) System.out.println("Number elements in schema " + DCSCHEMA + ": " + metadataFields.length);
            if (authDCElements != null) authDCElements.clear();
            else authDCElements = new ArrayList<MetadataField>();
            while (true) {
                System.out.println("DC elements as authorities collections (a:list all authorities / l: list all dc elements / n: enter new authority / x: exit");
                String response = null;
                try {
                    response = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                if (response == null) break;
                response = response.trim();
                if (response.equalsIgnoreCase("a")) {
                    System.out.println("List all Authorities");
                    MetadataField[] authArray = new MetadataField[authDCElements.size()];
                    listAllDCElements((MetadataField[])authDCElements.toArray(authArray));
                } else if (response.equalsIgnoreCase("l")) {
                    System.out.println("List all DC elements");
                    listAllDCElements(metadataFields);
                } else if (response.equalsIgnoreCase("n")) {
                    createElementAuth();
                } else if (response.equalsIgnoreCase("x")) {
                    break;
                }
            }

        } else System.out.println("There are not metatata elements");
        return false;
    }


    private boolean createElementAuth()
    {
        String element = null;
        MetadataField elementObj = null;
        String community = null;
        Community communityObj;
        String collection = null;
        org.dspace.content.Collection collectionObj;
        int step = 1;

        while (true) {
            if (element == null)
                System.out.println("DC Element (type x to go former menu: ");
            else if (community == null)
                System.out.println("Handle community or empty to create new: ");
            String response = null;
            try {
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (response == null) continue;
            response = response.trim();
            switch (step) {
                case 1:
                    if (response.length() == 0) continue;
                    if (response.equalsIgnoreCase("x")) return false;
                    int pos = response.indexOf(".");
                    try {
                        MetadataField elementMD = MetadataField.findByElement(context, dcSchema.getSchemaID(), (pos > 0)?response.substring(0, pos - 1):response, (pos > 0)?response.substring(pos + 1):null);
                        if (elementMD == null) {
                            System.out.println("Element " + response + " does not exist.");
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
                    if (response == null || response.length() == 0) {
                        response = null;
                        int stepCommunity = 1;
                        DSpaceObject communityObjParent = null;
                        while (true) {
                            if (stepCommunity == 1) System.out.println("Handle of parent collection or empty: ");
                            else if (stepCommunity == 2) System.out.println("Collection name: ");
                            try {
                                response = br.readLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                            if (response == null) continue;
                            response = response.trim();
                            if (stepCommunity > 1 && response.length() == 0) continue;
                            switch(stepCommunity) {
                                case 1:
                                    try {
                                        communityObjParent = HandleManager.resolveToObject(context, response);
                                        if (communityObjParent != null && communityObjParent instanceof Community) {
                                            stepCommunity++;
                                        }
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 2:
                                    try {
                                        communityObj = Community.create((Community) communityObjParent, context);
                                        communityObj.setMetadata("name", response);
                                        communityObj.update();
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
                            if (communityObjAux != null && communityObjAux instanceof Community) {
                                communityObj = (Community) communityObjAux;
                                community = communityObj.getName();
                            } else {
                                System.out.print("Handle: " + response + " does not exist or is not a community.");
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 3:
                    break;
            }
            if (step == 3 && element != null && community != null && collection != null) {
                authDCElements.add(elementObj);
                break;
            }
        }
        return false;
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

    @Override
    public void update(Observable o, Object arg)
    {
        System.out.println( "Received signal: " + arg );
        System.exit(0);
    }
}
