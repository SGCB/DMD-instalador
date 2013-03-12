package org.dspace.installer_edm;


import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @class InstallerEDMDAO
 * Clase para sustituir aplgnas llamadas de la api de dspace que no funcionan bien o para implementar nuevas
 *
 */

public class InstallerEDMDAO
{
    /**
     * Contexto de dspace {@link Context}
     */
    private static Context context = null;

    /**
     * Nombre de la base de datos
     */
    private static String dbName = null;


    /**
     * Asignar contexto nuevo
     *
     * @param context Contexto de dspace {@link Context}
     */
    public static void setContext(Context context)
    {
        InstallerEDMDAO.context = context;
    }

    /**
     * Asignar nuevo nombre de la base de datos
     *
     * @param dbName Nombre de la base de datos
     */
    public static void setDbName(String dbName)
    {
        InstallerEDMDAO.dbName = dbName;
    }


    /**
     * Busca en la base de datos de dspace ítems con un elemento dc determinado y con un valor suministrado
     * Si es oracle llama al método especial, si es Postgres llama al de la api de dspace
     * Debido a que en ORACLE el campo text_value es de tipo CLOB, no se puede comparar directamente con una cadena
     * hay que hacer una conversión antes con la función to_char
     *
     * @param schema esquema de elementos
     * @param element nombre del elemento dc
     * @param qualifier cualificador del elemento dc
     * @param value valor del elemento
     * @return objeto ItemIterator de dspace {@link ItemIterator}
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static ItemIterator findByMetadataField(String schema, String element, String qualifier, String value)
            throws SQLException, AuthorizeException, IOException
    {
        ItemIterator iterAuth = null;
        try {
            iterAuth = Item.findByMetadataField(context, schema, element, qualifier, value);
        } catch (SQLException e) {
            if (dbName.equalsIgnoreCase("postgres")) throw e;
            else {
                try {
                    return InstallerEDMDAO._findMetadataField(schema, element, qualifier, value);
                } catch (SQLException e2) {
                    throw e2;
                } catch (AuthorizeException e2) {
                    throw e2;
                } catch (IOException e2) {
                    throw e2;
                }
            }
        } catch (AuthorizeException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return iterAuth;
    }


    /**
     * Busca en la base de datos ORACLE de dspace ítems con un elemento dc determinado y con un valor suministrado
     *
     * @param schema esquema de elementos
     * @param element nombre del elemento dc
     * @param qualifier cualificador del elemento dc
     * @param value valor del elemento
     * @return objeto ItemIterator de dspace {@link ItemIterator}
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private static ItemIterator _findMetadataField(String schema, String element, String qualifier, String value)
            throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException(new StringBuilder().append("No such metadata schema: ").append(schema).toString());
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException(new StringBuilder().append("No such metadata field: schema=").append(schema).append(", element=").append(element).append(", qualifier=").append(qualifier).toString());
        }

        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' AND item.item_id = metadatavalue.item_id AND metadata_field_id = ?";

        TableRowIterator rows = null;
        if ("*".equals(value))
        {
            rows = DatabaseManager.queryTable(context, "item", query, new Object[]{Integer.valueOf(mdf.getFieldID())});
        }
        else
        {
            query = new StringBuilder().append(query).append(" AND to_char(metadatavalue.text_value) = ?").toString();
            rows = DatabaseManager.queryTable(context, "item", query, new Object[] { Integer.valueOf(mdf.getFieldID()), value });
        }
        return new ItemIterator(context, rows);
    }


}
