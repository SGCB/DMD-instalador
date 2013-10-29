/**
 *  Copyright 2013 Spanish Minister of Education, Culture and Sport
 *
 *  written by MasMedios
 *
 *  Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work  except in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://ec.europa.eu/idabc/servlets/Docbb6d.pdf?id=31979
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis,
 *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and limitations under the License.
 */

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
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException("No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' AND item.item_id = metadatavalue.item_id AND metadata_field_id = ?";

        TableRowIterator rows = null;
        if ("*".equals(value))
        {
            rows = DatabaseManager.queryTable(context, "item", query, new Object[]{Integer.valueOf(mdf.getFieldID())});
        }
        else
        {
            query = query + " AND to_char(metadatavalue.text_value) = ?";
            rows = DatabaseManager.queryTable(context, "item", query, new Object[] { Integer.valueOf(mdf.getFieldID()), value });
        }
        return new ItemIterator(context, rows);
    }


}
