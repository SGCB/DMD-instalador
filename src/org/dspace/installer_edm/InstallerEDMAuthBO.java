package org.dspace.installer_edm;

import org.dspace.content.*;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 31/10/12
 * Time: 9:29
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMAuthBO
{
    private Item item;
    private Community community;
    private Collection collection;
    private MetadataSchema schema;
    private MetadataField metadataField;

    public InstallerEDMAuthBO(Item item, Community community, Collection collection, MetadataSchema schema, MetadataField metadataField)
    {
        this.item = item;
        this.community = community;
        this.collection = collection;
        this.schema = schema;
        this.metadataField = metadataField;
    }


    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Community getCommunity()
    {
        return community;
    }

    public Collection getCollection()
    {
        return collection;
    }

    public MetadataSchema getMetadataSchema()
    {
        return schema;
    }

    public MetadataField getMetadataField()
    {
        return metadataField;
    }
}
