package org.dspace.installer_edm;

import org.dspace.content.*;

/**
 * @class InstallerEDMAuthBO
 *
 * POJO con los datos de la autoridad
 *
 */
public class InstallerEDMAuthBO
{
    /**
     * objeto Item de dspace {@link Item} que posee la autoridad
     */
    private Item item;

    /**
     * objeto Community de dspace {@link Community} que contiene la colección
     */
    private Community community;

    /**
     * objeto Collection de dspace {@link Collection} que contiene el ítem
     */
    private Collection collection;

    /**
     * objeto MetadataSchema de dspace {@link MetadataSchema} con el esquema del ítem
     */
    private MetadataSchema schema;

    /**
     * objeto MetadataField de dspace {@link MetadataField} con los metadatos del ítem
     */
    private MetadataField metadataField;


    /**
     * Constructor. Se asignan los valores
     *
     * @param item objeto Item de dspace {@link Item} que posee la autoridad
     * @param community objeto Community de dspace {@link Community} que contiene la colección
     * @param collection objeto Community de dspace {@link Community} que contiene la colección
     * @param schema objeto MetadataSchema de dspace {@link MetadataSchema} con el esquema del ítem
     * @param metadataField objeto MetadataField de dspace {@link MetadataField} con los metadatos del ítem
     */
    public InstallerEDMAuthBO(Item item, Community community, Collection collection, MetadataSchema schema, MetadataField metadataField)
    {
        this.item = item;
        this.community = community;
        this.collection = collection;
        this.schema = schema;
        this.metadataField = metadataField;
    }


    /**
     * Devuelve el objeto Item {@link Item}
     *
     * @return Item de dspace
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * Devuelve el objeto Item {@link Item}
     *
     * @param item el objeto Item {@link Item}
     */
    public void setItem(Item item)
    {
        this.item = item;
    }

    /**
     * Devuelve el objeto Community {@link Community}
     *
     * @return el objeto Community {@link Community}
     */
    public Community getCommunity()
    {
        return community;
    }

    /**
     * Devuelve el objeto Collection {@link Collection}
     *
     * @return el objeto Collection {@link Collection}
     */
    public Collection getCollection()
    {
        return collection;
    }

    /**
     * Devuelve el objeto MetadataSchema {@link MetadataSchema}
     *
     * @return el objeto MetadataSchema {@link MetadataSchema}
     */
    public MetadataSchema getMetadataSchema()
    {
        return schema;
    }

    /**
     * Devuelve el objeto MetadataField {@link MetadataField}
     *
     * @return el objeto MetadataField {@link MetadataField}
     */
    public MetadataField getMetadataField()
    {
        return metadataField;
    }
}
