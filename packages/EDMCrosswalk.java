/**
 *
 * EDMCrosswalk.java
 *
 * Copyright 2013 Spanish Minister of Education, Culture and Sport
 *
 *  written by MasMedios
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work  except in compliance with the License. You may obtain a copy of the License at:
 * http://ec.europa.eu/idabc/servlets/Docbb6d.pdf?id=31979
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.dspace.app.oai;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import org.dspace.app.util.MetadataExposure;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.search.HarvestedItemInfo;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * OAICat Crosswalk implementation that extracts data from the Dublin Core elements from Dspace
 * and maps them to build the <i>Europeana Schema</i> inside the <i>OAI-PMH schema</i>.
 * This source code file is to be used together with the <tt>installerEDM</tt> that will modify it to fill
 * the <strong>EDMRIGHTS</strong> and <strong>EDMTYPES</strong> with the values entered. Afterwards this file will
 * be compile and added to the <i>Dspace Oai Api Jar file</i>.
 *
 * <p>This file also can be modified manually to compile and to add it to the jar file.</p>
 *
 * <p>The classes that are showed are: <tt>edm:ProvidedCHO</tt>, <tt>edm:WebResource</tt>, <tt>skos:Concept</tt>
 * and <tt>ore:Aggregation</tt>.
 * </p>
 * <p><tt>edm:ProvidedCHO</tt> receives all the data from the dc elements of the dspace item and its handle</p>
 * <p><tt>edm:WebResource</tt> receives all the data from the handle and bitstream information</p>
 * <p><tt>skos:Concept</tt> receives all the data from authority field of the dc element</p>
 * <p><tt>ore:Aggregation</tt> receives all the data from the dc elements of the dspace item and its handle and bitstream information</p>
 *
 * @author MasMedios S.L.
 */


@SuppressWarnings("deprecation")
public class EDMCrosswalk extends Crosswalk
{

    // Url with the licence of the rights
    /* BEGIN EDMRIGHTS */
    private static final String EDMRIGHTS = "";
    /* END EDMRIGHTS */

    // Boolean with the edm:ugc
    /* BEGIN EDMUGC */
    private static final boolean EDMUGC = false;
    /* END EDMUGC */

    // Map with the terms to search in dc.type and change if matched with the generic type
    /* BEGIN EDMTYPES */
    private static final Map<String, List<String>> EDMTYPES;
    static {
        EDMTYPES = new HashMap<String, List<String>>();
        EDMTYPES.put("TEXT", new ArrayList<String>());
        EDMTYPES.put("VIDEO", new ArrayList<String>());
        EDMTYPES.put("IMAGE", new ArrayList<String>());
        EDMTYPES.put("SOUND", new ArrayList<String>());
        EDMTYPES.put("3D", new ArrayList<String>());
        // Begin Add EDMTYPES
        EDMTYPES.get("TEXT").add("TEXT");
        EDMTYPES.get("VIDEO").add("VIDEO");
        EDMTYPES.get("IMAGE").add("IMAGE");
        EDMTYPES.get("SOUND").add("SOUND");
        EDMTYPES.get("3D").add("3D");
        // End Add EDMTYPES
    }
    /* END EDMTYPES */

    // Namespaces required by the EDM schema
    private static final String NAMESPACE_URI_DCTERMS = "http://purl.org/dc/terms/";
    private static final String NAMESPACE_URI_EDM = "http://www.europeana.eu/schemas/edm/";
    private static final String NAMESPACE_URI_ENRICHMENT = "http://www.europeana.eu/schemas/edm/enrichment/";
    private static final String NAMESPACE_URI_OWL = "http://www.w3.org/2002/07/owl#";
    private static final String NAMESPACE_URI_WGS84 = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static final String NAMESPACE_URI_SKOS = "http://www.w3.org/2004/02/skos/core#";
    private static final String NAMESPACE_URI_ORE = "http://www.openarchives.org/ore/terms/";
    private static final String NAMESPACE_URI_OAI = "http://www.openarchives.org/OAI/2.0/";
    private static final String NAMESPACE_URI_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String NAMESPACE_URI_DC = "http://purl.org/dc/elements/1.1/";
    private static final String NAMESPACE_URI_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String NAMESPACE_URI_XML = "http://www.w3.org/XML/1998/namespace";
    private static final String NAMESPACE_URI_SCHEMALOCATION = "http://www.w3.org/1999/02/22-rdf-syntax-ns# http://www.europeana.eu/schemas/edm/EDM.xsd";

    private Namespace DCTERMS = Namespace.getNamespace("dcterms", NAMESPACE_URI_DCTERMS);
    private Namespace EDM = Namespace.getNamespace("edm", NAMESPACE_URI_EDM);
    private Namespace ENRICHMENT = Namespace.getNamespace("enrichment", NAMESPACE_URI_ENRICHMENT);
    private Namespace OWL = Namespace.getNamespace("owl", NAMESPACE_URI_OWL);
    private Namespace WGS84 = Namespace.getNamespace("wgs84", NAMESPACE_URI_WGS84);
    private Namespace SKOS = Namespace.getNamespace("skos", NAMESPACE_URI_SKOS);
    private Namespace ORE = Namespace.getNamespace("ore", NAMESPACE_URI_ORE);
    private Namespace OAI = Namespace.getNamespace("oai", NAMESPACE_URI_OAI);
    private Namespace RDF = Namespace.getNamespace("rdf", NAMESPACE_URI_RDF);
    private Namespace DC = Namespace.getNamespace("dc", NAMESPACE_URI_DC);
    private Namespace XSI = Namespace.getNamespace("xsi", NAMESPACE_URI_XSI);
    private Namespace XML = Namespace.getNamespace("xml", NAMESPACE_URI_XML);

    /**
     * get url of our dspace from dspace configuration file
     */
    private String baseUrl;

    /**
     * Handle url
     */
    private String handleUrl;

    /**
     * prefix handle
     */
    private String handlePrefix;

    /**
     * default prefix handle
     */
    private String handlePrefixDefault = "123456789";

    /**
     * Dspace context
     */
    private Context context;


    /**
     * Constructs the crosswalk with the namespace and the validation schema
     *
     * @param properties    the properties table received from dspace
     */
    public EDMCrosswalk(Properties properties)
    {
        super("http://www.europeana.eu/schemas/edm/ http://www.europeana.eu/schemas/edm/EDM.xsd");
        checkHandleUrl();
    }

    /**
     * Check and build the base url for the handles
     */
    private void checkHandleUrl()
    {
        baseUrl = ConfigurationManager.getProperty("dspace.url");
        handleUrl = null;
        handlePrefix = ConfigurationManager.getProperty("handle.prefix");
        if (!handlePrefix.equals(handlePrefixDefault)) {
            String handleCanonicalPrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
            if (isValidURI(handleCanonicalPrefix)) handleUrl = handleCanonicalPrefix + ((!handleCanonicalPrefix.endsWith("/"))?"/":"");
        }
        if (handleUrl == null) {
            handleUrl = baseUrl + ((!baseUrl.endsWith("/"))?"/handle/":"handle/");
        }

    }


    /**
     *
     * @param nativeItem    object received from dspace
     * @return  always true because we have DC for everything
     */
    public boolean isAvailableFor(Object nativeItem)
    {
        return true;
    }


    /**
     * Creates the metadata element of the OAI schema with the data for the EDM-RDF schema
     * We use the <i>jdom</i> library to generate the document that will represent the metadata
     *
     * @param nativeItem    object received from dspace, in this case an item
     * @return              returns the xml formatted string with the content of the OAI metadata element
     * @throws CannotDisseminateFormatException
     */
    public String createMetadata(Object nativeItem) throws CannotDisseminateFormatException
    {

        // Cast object received from dspace to an item
        Item item = ((HarvestedItemInfo) nativeItem).item;

        context = ((HarvestedItemInfo) nativeItem).context;

        // Get the original set of bundles from the item
        Bundle[] origBundles = new Bundle[0];
        try {
            origBundles = item.getBundles("ORIGINAL");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Just items with digital content
        if (origBundles.length == 0) return null;

        // element rdf with all the namespaces
        //Element rdf_RDF = new Element("RDF", "rdf");
        Element rdf_RDF = new Element("RDF", RDF);

        rdf_RDF.addNamespaceDeclaration(DCTERMS);

        rdf_RDF.addNamespaceDeclaration(EDM);

        rdf_RDF.addNamespaceDeclaration(ENRICHMENT);

        rdf_RDF.addNamespaceDeclaration(OWL);

        rdf_RDF.addNamespaceDeclaration(WGS84);

        rdf_RDF.addNamespaceDeclaration(SKOS);

        rdf_RDF.addNamespaceDeclaration(ORE);

        rdf_RDF.addNamespaceDeclaration(OAI);

        rdf_RDF.addNamespaceDeclaration(RDF);

        rdf_RDF.addNamespaceDeclaration(DC);

        rdf_RDF.addNamespaceDeclaration(XSI);
        rdf_RDF.setAttribute("schemaLocation", NAMESPACE_URI_SCHEMALOCATION, XSI);

        // creates a document with element rdf as root
        Document doc = new Document(rdf_RDF);

        // List with all the children elements of the root
        List<Element> listElements = new ArrayList<Element>();

        // Get all the dc elements from the item
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        // Creates edm:ProvidedCHO element and add to the children list
        Element ProvidedCHO = processProvidedCHO(item);
        listElements.add(ProvidedCHO);

        // Create skos:Concept elements with the authority value of the dc element and add them to the children list
        List<Element> listSkosConcept = processSkosConcept(allDC, item);
        if (listSkosConcept != null && listSkosConcept.size() > 0) {
            for (Element skosConceptElement : listSkosConcept)
                listElements.add(skosConceptElement);
        }

        if (origBundles.length > 0) {

            // Get the thumbnail set of bundles from the item
            Bundle[] thumbBundles = new Bundle[0];
            try {
                thumbBundles = item.getBundles("THUMBNAIL");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Get the files from the original bundle, only create edm:WebResource, skos:Concept and ore:Aggregation
            // elements if there are files
            Bitstream[] bitstreams = origBundles[0].getBitstreams();
            if (bitstreams.length > 0) {

                // Create edm:WebResource element with the bitstream
                Element[] WebResources = processWebResource(item, origBundles);
                if (WebResources != null && WebResources.length > 0) {
                    for (Element element : WebResources)
                        listElements.add(element);
                }

                // Create ore:Aggregation element with the files and add them to the children list
                Element oreAggregation = processOreAgreggation(item, origBundles, thumbBundles, bitstreams[0]);
                if (oreAggregation != null) listElements.add(oreAggregation);
            }
        }

        // Add all the children to the root
        for (Element element : listElements) {
            rdf_RDF.addContent(element);
        }

        // Get the string content of the document
        doc.setContent(rdf_RDF);
        XMLOutputter xmlOutput = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setOmitDeclaration(true);
        xmlOutput.setFormat(format);

        return xmlOutput.outputString(doc);
    }


    /**
     *
     * Creates the edm:ProvidedCHO element with all the dc and dcterms elements required.
     * The data is taken from the dc elements of the dspace item
     * To elaborate the types, the terms from the map are searched and matched against the dc.type element
     * and then replaced by the corresponding type
     *
     * @param item      dspace item object to get information from
     * @return          jdom element
     */
    private Element processProvidedCHO(Item item)
    {
        Element ProvidedCHO = new Element("ProvidedCHO", EDM);

        String urlH = handleUrl + item.getHandle();
        DCValue[] identifiers = item.getDC("identifier", "uri", null);
        if (identifiers.length > 0) ProvidedCHO.setAttribute(new Attribute("about", identifiers[0].value, RDF));
        else ProvidedCHO.setAttribute(new Attribute("about", urlH, RDF));

        createElementEDMExclusion(item, "contributor", DC, "contributor", new HashSet<String>(Arrays.asList("author")), ProvidedCHO, true, RDF);

        createElementEDM(item, "coverage", DC, "coverage", null, ProvidedCHO, true);

        createElementEDM(item, "creator", DC, "creator", null, ProvidedCHO, true, true);
        createElementEDM(item, "creator", DC, "contributor", "author", ProvidedCHO, true, true);

        createElementEDM(item, "date", DC, "date", null, ProvidedCHO, true);

        createElementEDM(item, "description", DC, "description", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "format", DC, "format", Item.ANY, ProvidedCHO, true);

        // createElementEDM(item, "identifier", DC, "identifier", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "language", DC, "language", "iso", ProvidedCHO, true);
        createElementEDM(item, "language", DC, "language", null, ProvidedCHO, true);

        createElementEDM(item, "publisher", DC, "publisher", null, ProvidedCHO, false);

        createElementEDM(item, "rights", DC, "rights", "holder", ProvidedCHO, true);
        createElementEDM(item, "rights", DC, "rights", "uri", ProvidedCHO, true);

        createElementEDM(item, "source", DC, "source", null, ProvidedCHO, false);

        createElementEDM(item, "subject", DC, "subject", null, ProvidedCHO, true, true);
        createElementEDM(item, "subject", DC, "subject", Item.ANY, ProvidedCHO, true, true);

        createElementEDM(item, "title", DC, "title", null, ProvidedCHO, true);

        createElementEDM(item, "type", DC, "type", null, ProvidedCHO, true);

        createElementEDM(item, "alternative", DCTERMS, "title", "alternative", ProvidedCHO, true);

        createElementEDM(item, "created", DCTERMS, "date", "created", ProvidedCHO, true);

        createElementEDM(item, "extent", DCTERMS, "format", "extent", ProvidedCHO, true);

        createElementEDM(item, "isPartOf", DCTERMS, "relation", "isPartOf", ProvidedCHO, true);

        createElementEDM(item, "isPartOf", DCTERMS, "relation", "ispartofseries", ProvidedCHO, true);

        createElementEDM(item, "hasPart", DCTERMS, "relation", "hasPart", ProvidedCHO, true);

        createElementEDM(item, "isRequiredBy", DCTERMS, "relation", "isRequiredBy", ProvidedCHO, true);

        createElementEDM(item, "isReplacedBy", DCTERMS, "relation", "isReplacedBy", ProvidedCHO, true);

        createElementEDM(item, "isVersionOf", DCTERMS, "relation", "isVersionOf", ProvidedCHO, true);

        createElementEDM(item, "hasVersion", DCTERMS, "relation", "hasVersion", ProvidedCHO, true);

        createElementEDM(item, "isFormatOf", DCTERMS, "relation", "isFormatOf", ProvidedCHO, true);

        createElementEDM(item, "hasFormat", DCTERMS, "relation", "hasFormat", ProvidedCHO, true);

        createElementEDM(item, "isReferencedBy", DCTERMS, "relation", "isReferencedBy", ProvidedCHO, true);

        createElementEDM(item, "conformsTo", DCTERMS, "relation", "conformsTo", ProvidedCHO, true);

        createElementEDM(item, "replaces", DCTERMS, "relation", "replaces", ProvidedCHO, true);

        createElementEDM(item, "requires", DCTERMS, "relation", "requires", ProvidedCHO, true);

        createElementEDMExclusion(item, "references", DCTERMS, "relation",
                new HashSet<String>(Arrays.asList("isPartOf", "ispartofseries", "hasPart", "isRequiredBy", "isReplacedBy"
                        , "isVersionOf", "hasVersion", "isFormatOf", "hasFormat", "isReferencedBy", "conformsTo"
                        , "replaces", "requires")), ProvidedCHO, true, null);

        createElementEDM(item, "issued", DCTERMS, "date", "issued", ProvidedCHO, true);

        createElementEDM(item, "medium", DCTERMS, "format", "medium", ProvidedCHO, true);

        createElementEDM(item, "provenance", DCTERMS, "description", "provenance", ProvidedCHO, true);

        createElementEDM(item, "spatial", DCTERMS, "coverage", "spatial", ProvidedCHO, true);

        createElementEDM(item, "tableOfContents", DCTERMS, "description", "tableofcontents", ProvidedCHO, true);

        createElementEDM(item, "temporal", DCTERMS, "coverage", "temporal", ProvidedCHO, true);

        createElementEDM(item, "temporal", DCTERMS, "coverage", "temporal", ProvidedCHO, true);

        /*
        String currentLocation = null;
        try {
            currentLocation = item.getMetadata("edm", "currentLocation", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (currentLocation == null || currentLocation.isEmpty()) currentLocation = handleUrl + item.getHandle();
        ProvidedCHO.addContent(new Element("currentLocation", EDM).setText(currentLocation));
        */

        ProvidedCHO.addContent(new Element("type", EDM).setText(processEDMType(item, false)));

        getOwlSameAs(item, ProvidedCHO);

        return ProvidedCHO;
    }


    /**
     * Traverse all the dc.type and compares the content with the terms of the map of types
     * and replaces the terms with a unique main type
     *
     * @param item      dspace item object to get information from
     * @param multiValued boolean para indicar si recogemos más de un valor
     * @return          string with the content of the edm.type containing the main types
     */
    private String processEDMType(Item item, boolean multiValued)
    {
        String edmTypeElement = null;
        // Items already has an edm.type element
        try {
            edmTypeElement = item.getMetadata("edm", "type", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (edmTypeElement != null && !edmTypeElement.isEmpty()) return edmTypeElement;

        // traverse all the dc.type elements and process them
        boolean found = false;
        StringBuilder edmType = new StringBuilder();
        DCValue[] elements = item.getDC("type", null, Item.ANY);
        if (elements.length > 0) {
            for (DCValue element : elements) {
                String value = element.value;
                Iterator<String> it1 = EDMTYPES.keySet().iterator();
                while (it1.hasNext()) {
                    String type = it1.next();
                    List<String> typeList = EDMTYPES.get(type);
                    for (String patternType : typeList) {
                        if (value.toLowerCase().indexOf(patternType.toLowerCase()) >= 0 && edmType.toString().toLowerCase().indexOf(type.toLowerCase()) < 0) {
                            if (multiValued) {
                                edmType.append(type).append(',');
                                found = true;
                            }
                            else return type;
                        }
                    }
                }
            }
        }
        if (!found) return "TEXT";
        return (edmType.length() > 0)?edmType.toString().substring(0, edmType.length() - 1):edmType.toString();
    }


    /**
     *
     * Creates the edm:WebResource from the bitstreams, the url of dspace and
     * created a the dc.rights and edm.rights
     *
     * @param item          dspace item object to get information from
     * @param bundles       array dspace bundle object with the file information
     * @return              array jdom element
     */
    private Element[] processWebResource(Item item, Bundle[] bundles)
    {
        List<Element> listWebResources = new ArrayList<Element>();

        Element WebResource = null;

        try {
            WebResource = new Element("WebResource", EDM);
            WebResource.setAttribute(new Attribute("about", handleUrl + item.getHandle() + "#&lt;/edm:isShownAt&gt", RDF));
            fillWebResource(item, WebResource);
            listWebResources.add(WebResource);

            for (Bundle bundle: bundles) {
                Bitstream[] bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream: bitstreams) {

                    WebResource = new Element("WebResource", EDM);

                    String url = baseUrl + "/bitstream/"
                                         + item.getHandle() + "/" + bitstream.getSequenceID() + "/" + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

                    WebResource.setAttribute(new Attribute("about", url, RDF));
                    fillWebResource(item, WebResource);
                    listWebResources.add(WebResource);
                }
            }
        } catch (Exception e) {

        }

        return listWebResources.toArray(new Element[listWebResources.size()]);
    }


    /**
     * Fill WebResource with dc and edm elements
     *
     * @param item Dspace Item object
     * @param WebResource parent jdom element
     */
    private void fillWebResource(Item item, Element WebResource)
    {
        createElementEDM(item, "rights", DC, "rights", null, WebResource, true);

        createElementEDM(item, "format", DC, "format", "mimetype", WebResource, true);

        createElementEDM(item, "extend", DCTERMS, "format", "extend", WebResource, true);

        createElementEDM(item, "issued", DCTERMS, "date", "available", WebResource, true);

        // creates edm.rights
        String edmRights = null;
        try {
            edmRights = item.getMetadata("edm", "rights", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (edmRights == null || edmRights.isEmpty()) edmRights = EDMRIGHTS;
        WebResource.addContent(new Element("rights", EDM).setText(edmRights));
    }


    /**
     *
     * Creates the skos:Concept elements for every dc element with an authority
     * The elements is built with the authority value and the dspace url.
     * The authority value must be a valid url or a handle from our dspace.
     *
     * @param itemDC        array of dcvalues objects to get the information from
     * @param item    dspace item object to get information from
     * @return              list of jdom elements
     */
    private List<Element> processSkosConcept(DCValue[] itemDC, Item item)
    {
        List<Element> listElementsSkosConcept = new ArrayList<Element>();

        for (DCValue dcv : itemDC) {
            String authority;
            if (dcv.authority != null && !dcv.authority.isEmpty()) {
                authority = checkAuthority(dcv.authority);
                if (authority == null) continue;
                Item itemAuth = getItemFromAuthority(authority);
                Element skosConcept = null;
                try {
                    skosConcept = ((dcv.element.equals("creator") && dcv.qualifier == null) || dcv.element.equals("contributor"))?new Element("Agent", EDM):new Element("Concept", SKOS);
                    skosConcept.setAttribute(new Attribute("about", authority, RDF));
                    Element prefLabel = new Element("prefLabel", SKOS);
                    if (dcv.language != null) prefLabel.setAttribute(new Attribute("lang", dcv.language, XML));
                    prefLabel.setText(dcv.value);
                    skosConcept.addContent(prefLabel);
                    if (itemAuth != null) {
                        DCValue[] elementsTitleAlt = itemAuth.getDC("title", "alternative", dcv.language);
                        if (elementsTitleAlt.length > 0) {
                            Element altLabel;
                            for (DCValue elementDCV : elementsTitleAlt) {
                                altLabel = new Element("altLabel", SKOS);
                                altLabel.setAttribute(new Attribute("lang", elementDCV.language, XML));
                                altLabel.setText(elementDCV.value);
                                skosConcept.addContent(altLabel);
                            }
                        }
                        DCValue[] elementsDesc = itemAuth.getDC("description", null, dcv.language);
                        if (elementsDesc.length > 0) {
                            Element note;
                            for (DCValue elementDCV : elementsDesc) {
                                note = new Element("note", SKOS);
                                note.setAttribute(new Attribute("lang", elementDCV.language, XML));
                                note.setText(elementDCV.value);
                                skosConcept.addContent(note);
                            }
                        }
                        getOwlSameAs(itemAuth, skosConcept);
                    }
                    listElementsSkosConcept.add(skosConcept);
                } catch (Exception e) {

                }
            }
        }

        return listElementsSkosConcept;
    }


    /**
     *
     * Creates the ore:Aggregation element with the information from the files.
     * The edm:isShownAt has the url from the item page detail
     * The edm:isShownBy and edm:object has the url from the first original file
     * The edm:hasView elements have the url from the thumbnail files
     * dc.rights and edm.rights are created as well
     * edm:dataProvider and edm:provider are created as well from dspace.name property
     *
     * @param item              dspace item object to get information from
     * @param origBundles       array with the original files information
     * @param thumbBundles      array with the thumbnail files information
     * @param bitstream         dspace bitstream object with the file information
     * @return                  jdom element
     */
    private Element processOreAgreggation(Item item, Bundle[] origBundles, Bundle[] thumbBundles, Bitstream bitstream)
    {

        Element oreAggregation = null;

        try {
            oreAggregation = new Element("Aggregation", ORE);

            String url = handleUrl + item.getHandle();
            oreAggregation.setAttribute(new Attribute("about", url + "#aggregation", RDF));

            Element aggregatedCHO = new Element("aggregatedCHO", EDM);
            aggregatedCHO.setAttribute("resource", url, RDF);
            oreAggregation.addContent(aggregatedCHO);

            // edm:ugc
            if (EDMUGC) {
                oreAggregation.addContent(new Element("ugc", EDM).setText("true"));
            }

            // edm:dataProvider
            oreAggregation.addContent(new Element("dataProvider", EDM).setText(ConfigurationManager.getProperty("dspace.name")));

            // url of the first original file
            String urlObject = baseUrl + "/bitstream/"
                                       + item.getHandle() + "/" + bitstream.getSequenceID() + "/"
                                       + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

            // edm:isShownAt
            oreAggregation.addContent(new Element("isShownAt", EDM).setAttribute("resource", url + "#&lt;/edm:isShownAt&gt", RDF));

            // edm:isShownBy
            oreAggregation.addContent(new Element("isShownBy", EDM).setAttribute("resource", urlObject, RDF));

            // edm:object
            if (thumbBundles != null && thumbBundles.length > 0) {
                Bitstream[] bitstreamsThumb = thumbBundles[0].getBitstreams();
                if (bitstreamsThumb != null && bitstreamsThumb.length > 0) {
                    String urlObjectThumb = baseUrl + "/bitstream/"
                            + item.getHandle() + "/" + bitstreamsThumb[0].getSequenceID() + "/"
                            + Util.encodeBitstreamName(bitstreamsThumb[0].getName(), Constants.DEFAULT_ENCODING);
                    oreAggregation.addContent(new Element("object", EDM).setAttribute("resource", urlObjectThumb, RDF));
                }
            }

            // edm:hasView
            int i = 0;
            for (Bundle bundle : origBundles) {
                try {
                    Bitstream[] bitstreamsOrig = bundle.getBitstreams();
                    for (Bitstream bitstream1 : bitstreamsOrig) {
                        if (i++ == 0) continue;
                        urlObject = baseUrl + "/bitstream/"
                                            + item.getHandle() + "/" + bitstream1.getSequenceID() + "/"
                                            + Util.encodeBitstreamName(bitstream1.getName(), Constants.DEFAULT_ENCODING);

                        oreAggregation.addContent(new Element("hasView", EDM).setAttribute("resource", urlObject, RDF));
                    }
                } catch (Exception ex) {

                }
            }

            // edm.provider
            oreAggregation.addContent(new Element("provider", EDM).setText(ConfigurationManager.getProperty("dspace.name")));

            // dc.rights
            createElementEDM(item, "rights", DC, "rights", null, oreAggregation, true);

            // edm.rights
            String edmRights = null;
            try {
                edmRights = item.getMetadata("edm", "rights", null, Item.ANY)[0].value;
            } catch (Exception e) {
            }
            if (edmRights == null || edmRights.isEmpty()) edmRights = EDMRIGHTS;
            oreAggregation.addContent(new Element("rights", EDM).setText(edmRights));

        } catch (Exception e) {

        }

        return oreAggregation;
    }


    /**
     * Creates a new element getting the information from another and adding it to the parent element
     *
     * @param item Item object from dspace {@link Item} to get its dc elements
     * @param elementEDM EDM element to add data to
     * @param nameSpace namespace of the new EDM element
     * @param elementDC DC element to get the data from
     * @param qualifier DC element qualifier to get the data from
     * @param ProvidedCHO jdom element with the class EDM ProvidedCHO
     * @param repeat earch more than one element
     * @param resource authority to be added as resource
     *
     * @return array of objects
     */
    protected Object[] createElementEDM(Item item, String elementEDM, Namespace nameSpace, String elementDC, String qualifier
            , Element ProvidedCHO, boolean repeat, boolean resource)
    {
        if (qualifier != null && qualifier.equals(Item.ANY)) return null;
        Object[] elementsDom = createElementEDM(item, elementEDM, nameSpace, elementDC, qualifier, ProvidedCHO, repeat);
        if (resource && elementsDom != null && elementsDom.length > 0) {
            for (int i=0; i < elementsDom.length; i+=2) {
                Element elementDom = (Element) elementsDom[i];
                DCValue elementDCV = (DCValue) elementsDom[i + 1];
                if (elementDCV.authority != null && !elementDCV.authority.isEmpty()) {
                    String authority = checkAuthority(elementDCV.authority);
                    if (authority == null) continue;
                    elementDom.setAttribute("resource", authority, RDF);
                }
            }
        }
        return null;
    }


    /**
     *
     * Creates a new element getting the information from another and adding it to the parent element
     * Is optional to search more than one element.
     *
     * @param item              dspace item object to get information from
     * @param elementEDM        name of the edm element to create
     * @param nameSpace         namespace to the new element
     * @param elementDC         name of the dc element to get the data from
     * @param qualifier         qualifier of the dc element to get the data from
     * @param parent            parent element to add the new element
     * @param repeat            search more than one element
     *
     * @return array with pair Dom , DCValue elements
     */
    private Object[] createElementEDM(Item item, String elementEDM, Namespace nameSpace, String elementDC, String qualifier, Element parent, boolean repeat)
    {
        try {
            if (MetadataExposure.isHidden(null, DC.getPrefix(), elementDC, qualifier)) return null;
        } catch (SQLException e) {
            return null;
        }
        ArrayList<Object> elementsDom = null;
        DCValue[] elements = item.getDC(elementDC, qualifier, Item.ANY);
        if (elements.length > 0) {
            elementsDom = new ArrayList<Object>();
            for (DCValue element : elements) {
                if (qualifier != null && qualifier.equals(Item.ANY)) {
                    try {
                        if (MetadataExposure.isHidden(null, DC.getPrefix(), element.element, element.qualifier)) continue;
                    } catch (SQLException e) {
                        continue;
                    }
                }
                Element  elementDom = new Element(elementEDM, nameSpace).setText(element.value);
                parent.addContent(elementDom);
                elementsDom.add(elementDom);
                elementsDom.add(element);
                if (!repeat) break;
            }
        }
        return (elementsDom != null)?elementsDom.toArray(new Object[elementsDom.size()]):null;
    }


    /**
     * Creates a new dc element to add to the class EDM ProvidedCHO
     *
     * @param item object Item from dspace {@link Item} to get the dc elements
     * @param elementEDM EDM element with the resulting elements
     * @param nameSpace namespace from new EDM element
     * @param elementDC DC element from which to get the elements
     * @param noQualifier qualifiers forbidden to get data from
     * @param ProvidedCHO jdom element with class EDM ProvidedCHO
     * @param repeat search for more dc elements in the item
     *
     * @return last dom element created
     */
    protected Element createElementEDMExclusion(Item item, String elementEDM, Namespace nameSpace, String elementDC, Set<String> noQualifier, Element ProvidedCHO, boolean repeat, Namespace resource)
    {
        Element elementDom = null;
        DCValue[] elements = item.getDC(elementDC, Item.ANY, Item.ANY);
        if (elements.length > 0) {
            for (DCValue element : elements) {
                if (noQualifier.contains(element.qualifier)) continue;
                try {
                    if (MetadataExposure.isHidden(null, DC.getPrefix(), element.element, element.qualifier)) continue;
                } catch (SQLException e) {
                    continue;
                }
                elementDom = new Element(elementEDM, nameSpace).setText(element.value);
                if (resource != null && elementDom != null) {
                    if (element.authority != null && !element.authority.isEmpty()) {
                        String authority = checkAuthority(element.authority);
                        if (authority != null)
                            elementDom.setAttribute("resource", authority, resource);
                    }
                }
                ProvidedCHO.addContent(elementDom);
                if (!repeat) break;
            }
        }
        return elementDom;
    }


    /**
     * Check whether an authoriy is valid: must be an url or an existing handle. The url is built
     *
     * @param authority The authority to check
     *
     * @return authority or null
     */
    protected String checkAuthority(String authority)
    {
        final String REGEX_HANDLE_PATTERN = "^\\d+/\\d+$";
        final String REGEX_HANDLE_VOCAB_PATTERN = "^.+_(\\d+_\\d+)$";
        Pattern patternHandleVocab = Pattern.compile(REGEX_HANDLE_VOCAB_PATTERN);
        Item itemAuth = null;

        if (!isValidURI(authority)) {
            try {
                Matcher matcherHandleVocab = patternHandleVocab.matcher(authority);
                if (matcherHandleVocab.find()) authority = ((String) matcherHandleVocab.group(1)).replace('_', '/');
                // check it's a handle and from our dspace
                if (authority.matches(REGEX_HANDLE_PATTERN) && (itemAuth = (Item) HandleManager.resolveToObject(context, authority)) != null) {
                    authority = handleUrl + authority;
                    return authority;
                } else return null;
            } catch (Exception e) {
                return null;
            }
            // a valid url
        } else return authority;
    }


    /**
     * Returns the item from the handle or its url
     *
     * @param authority string with the handle or the url
     * @return Item from dspace
     */
    protected Item getItemFromAuthority(String authority)
    {
        final String REGEX_HANDLE_PATTERN = "^\\d+/\\d+$";

        if (authority.startsWith(handleUrl)) authority = authority.replaceFirst(handleUrl, "");
        if (authority.matches(REGEX_HANDLE_PATTERN)) {
            try {
                return (Item) HandleManager.resolveToObject(context, authority);
            } catch (Exception e) {
            }
        }
        return null;
    }


    /**
     * Get elements owl:sameAs from the item and add them to the jdom element
     *
     * @param item Item object from dspace {@link Item}
     * @param elementDOMParent Deom element to add to
     */
    private void getOwlSameAs(Item item, Element elementDOMParent)
    {
        try {
            if (MetadataExposure.isHidden(null, OWL.getPrefix(), "sameAs", null)) return;
        } catch (SQLException e) {
            return;
        }
        DCValue[] elements = item.getMetadata(OWL.getPrefix(), "sameAs", null, Item.ANY);
        if (elements.length > 0) {
            for (DCValue element : elements) {
                Element elementDom = new Element("sameAs", OWL).setAttribute("resource", element.value, RDF);
                if (elementDOMParent != null) elementDOMParent.addContent(elementDom);
            }
        }
    }


    /**
     * Check if a string is a valid url
     *
     * @param uriStr    url to check
     * @return          boolean with the result
     */
    private boolean isValidURI(String uriStr)
    {
        try {
            URI uri = new URI(uriStr);
            uri.toURL();
            return true;
        } catch (URISyntaxException e) {
            return false;
        } catch (MalformedURLException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
