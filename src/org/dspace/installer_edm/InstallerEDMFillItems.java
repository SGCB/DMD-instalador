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
 * @class InstallerEDMFillItems
 *
 * Clase para rellenar el campo autoridad de la tabla metadatavalue con el handle del ítem de la autoridad
 * cuyo valor se corresponde con el valor del mismo elemento dc del ítem actual que no es autoridad
 * Extiende la clase base {@link InstallerEDMBase}
 *
 */
public class InstallerEDMFillItems extends InstallerEDMBase implements Observer
{

    /**
     * lista de elementos dc de las autoridades
     */
    private ArrayList<MetadataField> authDCElements;

    /**
     * Conjunto para cacheo de elementos dc con los valores de las autoridades encontradas para ella.
     * Se agrupan en niveles de coincidencias, los que se borran antes al alcanzar el límite de capacidad
     * son los de menor nivel.
     * Se usa paa evitar consultas a la base de datos de valores muy recientes o muy repetidos
     */
    private Map<MetadataField, CacheAuthValues> cacheAuthValues;

    /**
     * número de ítems modificados
     */
    private int numItemsModified = 0;

    /**
     * Constructor
     * Inicializa la lista de elementos dc de las autoridades y la caché
     *
     * @param currentStepGlobal paso actual
     */
    public InstallerEDMFillItems(int currentStepGlobal)
    {
        super(currentStepGlobal);
        initAuthBOHashMap();
        if (authDCElements == null) authDCElements = new ArrayList<MetadataField>();
        if (cacheAuthValues == null) cacheAuthValues = new Hashtable<MetadataField, CacheAuthValues>();
    }

    /**
     * Se ha de validar como usuario de dspace con privilegios de administrador.
     * Muestra el menú de los elementos dc de las autoridades para recoger valores de los ítems
     * tras esto muestra el menú de las colecciones a recorrer para recoger los valores
     */
    public void configure()
    {
        // validación y comprobar privilegios de admin
        if (eperson == null && !loginUser()) return;
        authDCElements.clear();
        cacheAuthValues.clear();
        numItemsModified = 0;
        try {
            if (verbose) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.checkAllSkosAuthElements");
            }
            // recoger elementos dc de autoridades
            checkAllSkosAuthElements(authDCElements);
            if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.auth.num", new String[]{Integer.toString(authDCElements.size())});
            if (authDCElements.size() > 0) {
                installerEDMDisplay.showLn();
                List<MetadataField> metadataFieldList = new ArrayList<MetadataField>();
                int statusElementsDC;
                do {
                    // mostrar el menú de escoger elementos dc
                    statusElementsDC = showMenuDCElements(metadataFieldList);
                    if (statusElementsDC > 0) {
                        List<Collection> collectionList = new ArrayList<Collection>();
                        int statusCollection;
                        do {
                            // mostrar el menú de las colecciones a elegir
                            statusCollection = showMenuCollections(collectionList);
                            if (statusCollection > 0) {
                                if (verbose) installerEDMDisplay.showQuestion(currentStepGlobal, "configure.traverseNonauthItems");
                                // recorrer los ítems que no son autoridades
                                traverseNonAuthItems(metadataFieldList, collectionList);
                                installerEDMDisplay.showQuestion(currentStepGlobal, "configure.numItemsModified", new String[]{Integer.toString(numItemsModified)});
                            }
                        } while (statusCollection == 0);
                        if (statusCollection < 0) statusElementsDC = 0;
                    }
                } while (statusElementsDC == 0);
            }
        } catch (SQLException e) {
            showException(e);
        } catch (AuthorizeException e) {
            showException(e);
        }
    }

    /**
     * Muestra el menú para elegir las colecciones para recorrer
     * se pueden escoger todas las colecciones, o handles separadas por ","
     *
     * @param collectionList lista de las colecciones que se escogen
     * @return tamaño de la lista de colecciones, positivo para todos o alguna de ellas
     * @throws SQLException
     */
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
            // todas las colecciones
            if (response.equalsIgnoreCase("a")) {
                return 1;
            // salir
            } else if (response.equalsIgnoreCase("x")) {
                return -1;
            // handle de colecciones separadas por ","
            } else {
                String[] handles = response.split(",");
                for (String handle : handles) {
                    // se validan los handle
                    if (handle.matches("^\\d+\\/\\d+$")) {
                        DSpaceObject collection = HandleManager.resolveToObject(context, handle);
                        if (collection == null || collection.getType() != Constants.COLLECTION) installerEDMDisplay.showQuestion
                                (currentStepGlobal,
                                "showMenuCollections.collection.notexist", new String[]{handle});
                        else collectionList.add((Collection) collection);
                    }
                }
                return collectionList.size();
            }
        }
        return 0;
    }

    /**
     * Muestra el menú para elegir las colecciones para recorrer
     * se pueden escoger todas las colecciones, o handles separadas por ","
     *
     * @param metadataFieldList lista de elementos dc que se escogen
     * @return tamaño de la lista de elementos dc, positivo para todos o alguna de ellas
     */
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
            // lista de elementos dc de las autoridades
            if (response.equalsIgnoreCase("l")) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuDCElements.listdc");
                listAllDCElements(authDCElements);
            // todos los elementos dc
            } else if (response.equalsIgnoreCase("a")) {
                return 1;
            // salir
            } else if (response.equalsIgnoreCase("x")) {
                return -1;
                // elementos dc separados por ","
            } else {
                String[] dcElements = response.split(",");
                for (String dcElement : dcElements) {
                    // see validan los elementos dc
                    MetadataField elementObj = findElementDC(dcElement);
                    if (elementObj == null) installerEDMDisplay.showQuestion(currentStepGlobal,
                            "showMenuDCElements.dcelement.notexist", new String[]{dcElement});
                    else {
                        String element = elementObj.getElement() + ((elementObj.getQualifier() != null)?"." + elementObj.getQualifier():"");
                        if (elementsNotAuthSet.contains(element)) {
                            installerEDMDisplay.showQuestion(currentStepGlobal, "showMenuDCElements.dcelement.notallowed", new String[]{element});
                            installerEDMDisplay.showLn();
                        } else metadataFieldList.add(elementObj);
                    }
                }
                return metadataFieldList.size();
            }
        }
        return 0;
    }

    /**
     * Recorrer los ítems de las colecciones elegidas para los elementos dc elegidos
     *
     * @param metadataFieldList lista de elementos dc elegidos
     * @param collectionList lista de colecciones elegidos
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void traverseNonAuthItems(List<MetadataField> metadataFieldList, List<Collection> collectionList) throws SQLException, AuthorizeException
    {
        // limpiamos la cache para recoger los datos desde la base de datos y no de pasos anteriores
        context.clearCache();
        // recoger todos los elementos dc de las autoridades
        if (metadataFieldList.isEmpty()) metadataFieldList = authDCElements;
        // recoger las colecciones de la lista o todas
        Collection[] listCollections = (!collectionList.isEmpty())?(Collection[]) collectionList.toArray(new Collection[collectionList.size()]):Collection.findAll(context);
        for (Collection collection : listCollections) {
            fillCollection(collection, metadataFieldList);
        }
    }

    /**
     * Recorrer una colección para buscar los ítems con una lista de elementos dc
     *
     * @param collection colección actual
     * @param metadataFieldList lista de elementos dc elegidos
     * @throws SQLException
     */
    private void fillCollection(Collection collection, List<MetadataField> metadataFieldList) throws SQLException
    {
        if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.collection", new String[]{collection.getName(), collection.getHandle()});
        // recoger todos los ítems de una colección
        ItemIterator iter = collection.getAllItems();
        while (iter.hasNext()) {
            installerEDMDisplay.showProgress('.');
            Item item = iter.next();
            // se puede cambiar el ítem
            if (!AuthorizeManager.isAdmin(context, item) || !item.canEdit()) {
                installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.nopermission", new String[]{eperson.getEmail(), item.getHandle()});
                continue;
            }
            boolean itemUpdated = false;
            // si el ítem es una autoridad lo saltamos
            if (searchSkosAuthItem(item)) continue;
            if (debug) {
                installerEDMDisplay.showLn();
                installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.item", new String[]{item.getName(), item.getHandle()});
            }
            // comprobar q tiene el elemento dc
            Map<MetadataField, DCValue[]> metadataField2Clear = new Hashtable<MetadataField, DCValue[]>();
            for (MetadataField metadataField : metadataFieldList) {
                if (!cacheAuthValues.containsKey(metadataField)) cacheAuthValues.put(metadataField, new CacheAuthValues());
                if (checkMetadataFieldIsModifiable(item, metadataField, metadataField2Clear)) itemUpdated = true;
            }
            // ítem modificado, se cambia en la bbdd
            if (itemUpdated) {
                try {
                    updateItem(item, metadataField2Clear);
                    numItemsModified++;
                } catch (SQLException e) {
                    showException(e);
                } catch (AuthorizeException e) {
                    showException(e);
                }
            }
        }
    }


    /**
     * Busca el ítem de la autoridad en la caché o en bbdd para añadir al campo autoridad del ítem el handle de la autoridad
     *
     * @param item objeto ítem
     * @param metadataField objeto elemento dc
     * @param metadataField2Clear tabla para comprobar si ya existe el elemento dc
     * @return si se ha actualizado el ítem
     * @throws SQLException
     */
    private boolean checkMetadataFieldIsModifiable(Item item, MetadataField metadataField, Map<MetadataField, DCValue[]> metadataField2Clear) throws SQLException
    {
        boolean itemUpdated = false;

        String dcValueName = metadataField.getElement() + ((metadataField.getQualifier() != null && !metadataField.getQualifier().isEmpty())?"." + metadataField.getQualifier():"");
        // elemento prohibido para autoridad, lo saltamos
        if (elementsNotAuthSet.contains(dcValueName)) return false;
        // valores del elemento dc en este ítem
        DCValue[] listDCValues = item.getMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
        if (listDCValues.length > 0) {
            // recorremos todos los elementos dc con este nombre
            for (DCValue dcValue : listDCValues) {
                // hay autoridad
                if (dcValue.authority != null) {
                    // url válida, ya tiene autoridad válida
                    if (isValidURI(dcValue.authority)) continue;
                    // handle existente, ya tiene autoridad válida
                    if (dcValue.authority.matches("^\\d+\\/\\d+$") && HandleManager.resolveToObject(context,
                            dcValue.authority) != null) continue;
                }

                try {
                    if (debug) installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.searchNonAuthItems", new String[]{dcValueName, dcValue.value});
                    // se busca si está el element cacheado
                    String handle = cacheAuthValues.get(metadataField).searchHandleFromValue(dcValue.value);
                    // si no se busca en la bbdd
                    if (handle == null) {
                        handle = searchNonAuthItems(metadataField, dcValue.value);
                    }
                    // existe la autoridad con ese valor
                    if (handle != null) {
                        // se actualiza la caché
                        cacheAuthValues.get(metadataField).addCacheAuthValue(dcValue.value, handle);
                        if (debug) {
                            System.out.println(cacheAuthValues.get(metadataField).getNumAuth());
                            installerEDMDisplay.showQuestion(currentStepGlobal, "traverseNonauthItems.changeitem", new String[]{item.getHandle(), handle});
                        }
                        // campo autoridad con el handle de la autoridad
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

    /**
     * Actualiza el ítem en la base de datos
     *
     * @param item objeto ítem a actualizar
     * @param metadataField2Clear elementos dc a modificar
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void updateItem(Item item, Map<MetadataField, DCValue[]> metadataField2Clear) throws SQLException, AuthorizeException
    {
        for (MetadataField metadataField : metadataField2Clear.keySet()) {
            item.clearMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language);
            for (DCValue dcValue : metadataField2Clear.get(metadataField)) {
                if (debug) {
                    System.out.format("%s.%s.%s %s %s %s", dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language, dcValue.value, dcValue.authority);
                    installerEDMDisplay.showLn();
                }
                item.addMetadata(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), language, dcValue.value, dcValue.authority, -1);
            }
        }
        item.update();
        context.commit();
    }

    /**
     * Busca ítem de autoridad con un valor de element dc determinado
     *
     * @param metadataField
     * @param value
     * @return cadena con el handle del ítem
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private String searchNonAuthItems(MetadataField metadataField, String value) throws SQLException, IOException, AuthorizeException
    {
        ItemIterator iterAuth = InstallerEDMDAO.findByMetadataField(dcSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), value);
        if (iterAuth != null) {
            try {
                while (iterAuth.hasNext()) {
                    Item itemMatched = iterAuth.next();
                    if (searchSkosAuthItem(itemMatched)) return HandleManager.findHandle(context, itemMatched);
                }
            } finally {
                iterAuth.close();
            }
        }
        return null;
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
