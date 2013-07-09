/**
 *
 * Date: 18/10/12
 * ESECrosswalk.java
 *
 * Date: $Date: 2012-10-18
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.oai;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
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
    private static final String NAMESPACE_URI_SCHEMALOCATION = "http://www.w3.org/1999/02/22-rdf-syntax-ns# EDM.xsd";

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
     * Constructs the crosswalk with the namespace and the validation schema
     *
     * @param properties    the properties table received from dspace
     */
    public EDMCrosswalk(Properties properties)
    {
        super("http://www.europeana.eu/schemas/edm/ http://www.europeana.eu/schemas/edm/EDM.xsd");
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

        // get url of our dspace from dspace configuration file
        String baseUrl = ConfigurationManager.getProperty("dspace.url");

        // List with all the children elements of the root
        List<Element> listElements = new ArrayList<Element>();

        // Cast object received from dspace to an item
        Item item = ((HarvestedItemInfo) nativeItem).item;

        // Get all the dc elements from the item
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        // Creates edm:ProvidedCHO element and add to the children list
        Element ProvidedCHO = processProvidedCHO(item, baseUrl);
        listElements.add(ProvidedCHO);

        // Create skos:Concept elements with the authority value of the dc element and add them to the children list
        List<Element> listSkosConcept = processSkosConcept(allDC, nativeItem, baseUrl);
        if (listSkosConcept != null && listSkosConcept.size() > 0) {
            for (Element skosConceptElement : listSkosConcept)
                listElements.add(skosConceptElement);
        }

        // Get the original set of bundles from the item
        Bundle[] origBundles = new Bundle[0];
        try {
            origBundles = item.getBundles("ORIGINAL");
        } catch (SQLException e) {
            e.printStackTrace();
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

                // Create edm:WebResource element with the first file and add it to the children list
                Element WebResource = processWebResource(item, bitstreams[0], baseUrl);
                if (WebResource != null) listElements.add(WebResource);

                // Create ore:Aggregation element with the files and add them to the children list
                Element oreAggregation = processOreAgreggation(item, origBundles, thumbBundles, bitstreams[0], baseUrl);
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
     * @param baseUrl   string with the dspace url
     * @return          jdom element
     */
    private Element processProvidedCHO(Item item, String baseUrl)
    {
        Element ProvidedCHO = new Element("ProvidedCHO", EDM);

        DCValue[] identifiers = item.getDC("identifier", "uri", null);
        if (identifiers.length > 0) ProvidedCHO.setAttribute(new Attribute("about", identifiers[0].value, RDF));

        createElementEDM(item, "contributor", DC, "contributor", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "coverage", DC, "coverage", null, ProvidedCHO, true);

        createElementEDM(item, "creator", DC, "creator", null, ProvidedCHO, true);

        createElementEDM(item, "date", DC, "date", null, ProvidedCHO, true);

        createElementEDM(item, "description", DC, "description", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "format", DC, "format", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "identifier", DC, "identifier", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "language", DC, "language", "iso", ProvidedCHO, true);
        createElementEDM(item, "language", DC, "language", null, ProvidedCHO, true);

        createElementEDM(item, "publisher", DC, "publisher", null, ProvidedCHO, false);

        createElementEDM(item, "relation", DC, "relation", null, ProvidedCHO, false);

        createElementEDM(item, "rights", DC, "rights", "holder", ProvidedCHO, true);
        createElementEDM(item, "rights", DC, "rights", "uri", ProvidedCHO, true);

        createElementEDM(item, "source", DC, "source", null, ProvidedCHO, false);

        createElementEDM(item, "subject", DC, "subject", Item.ANY, ProvidedCHO, true);

        createElementEDM(item, "title", DC, "title", null, ProvidedCHO, true);

        createElementEDM(item, "type", DC, "type", null, ProvidedCHO, true);

        createElementEDM(item, "alternative", DCTERMS, "title", "alternative", ProvidedCHO, true);

        createElementEDM(item, "created", DCTERMS, "date", "created", ProvidedCHO, true);

        createElementEDM(item, "extent", DCTERMS, "format", "extent", ProvidedCHO, true);

        createElementEDM(item, "hasFormat", DCTERMS, "relation", "hasformatof", ProvidedCHO, true);

        createElementEDM(item, "hasPart", DCTERMS, "relation", "haspart", ProvidedCHO, true);

        createElementEDM(item, "hasVersion", DCTERMS, "relation", "hasversion", ProvidedCHO, true);

        createElementEDM(item, "isPartOf", DCTERMS, "relation", "ispartof", ProvidedCHO, true);
        createElementEDM(item, "isPartOf", DCTERMS, "relation", "ispartofseries", ProvidedCHO, true);

        createElementEDM(item, "isReferencedBy", DCTERMS, "relation", "isreferencedby", ProvidedCHO, true);

        createElementEDM(item, "isReplacedBy", DCTERMS, "relation", "isreplacedby", ProvidedCHO, true);

        createElementEDM(item, "issued", DCTERMS, "date", "issued", ProvidedCHO, true);

        createElementEDM(item, "isVersionOf", DCTERMS, "relation", "isversionof", ProvidedCHO, true);

        createElementEDM(item, "medium", DCTERMS, "format", "medium", ProvidedCHO, true);

        createElementEDM(item, "provenance", DCTERMS, "description", "provenance", ProvidedCHO, true);

        createElementEDM(item, "replaces", DCTERMS, "relation", "replaces", ProvidedCHO, true);

        createElementEDM(item, "requires", DCTERMS, "relation", "requires", ProvidedCHO, true);

        createElementEDM(item, "spatial", DCTERMS, "coverage", "spatial", ProvidedCHO, true);

        createElementEDM(item, "tableOfContents", DCTERMS, "description", "tableofcontents", ProvidedCHO, true);

        createElementEDM(item, "temporal", DCTERMS, "coverage", "temporal", ProvidedCHO, true);

        createElementEDM(item, "temporal", DCTERMS, "coverage", "temporal", ProvidedCHO, true);

        String currentLocation = null;
        try {
            currentLocation = item.getMetadata("edm", "currentLocation", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (currentLocation == null || currentLocation.isEmpty()) currentLocation = baseUrl + "/handle/" + item.getHandle();
        ProvidedCHO.addContent(new Element("currentLocation", EDM).setText(currentLocation));

        ProvidedCHO.addContent(new Element("type", EDM).setText(processEDMType(item)));

        return ProvidedCHO;
    }


    /**
     * Traverse all the dc.type and compares the content with the terms of the map of types
     * and replaces the terms with a unique main type
     *
     * @param item      dspace item object to get information from
     * @return          string with the content of the edm.type containing the main types
     */
    private String processEDMType(Item item)
    {
        String edmTypeElement = null;
        // Items already has an edm.type element
        try {
            edmTypeElement = item.getMetadata("edm", "type", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (edmTypeElement != null && !edmTypeElement.isEmpty()) return edmTypeElement;

        // traverse all the dc.type elements and process them
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
                            edmType.append(type).append(',');
                        }
                    }
                }
            }
        }
        return edmType.toString();
    }


    /**
     *
     * Creates the edm:WebResource from the first original file, the url of dspace and
     * created a the dc.rights and edm.rights
     *
     * @param item          dspace item object to get information from
     * @param bitstream     dspace bitstream object with the file information
     * @param baseUrl       string with the dspace url
     * @return              jdom element
     */
    private Element processWebResource(Item item, Bitstream bitstream, String baseUrl)
    {
        Element WebResource = null;

        try {
            WebResource = new Element("WebResource", EDM);

            String url = baseUrl + "/bitstream/"
                                 + item.getHandle() + "/" + bitstream.getSequenceID() + "/" + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

            WebResource.setAttribute(new Attribute("about", url, RDF));

            // creates dc.rights
            createElementEDM(item, "rights", DC, "rights", null, WebResource, true);

            // creates edm.rights
            String edmRights = null;
            try {
                edmRights = item.getMetadata("edm", "rights", null, Item.ANY)[0].value;
            } catch (Exception e) {
            }
            if (edmRights == null || edmRights.isEmpty()) edmRights = EDMRIGHTS;
            WebResource.addContent(new Element("rights", EDM).setText(edmRights));
        } catch (Exception e) {

        }

        return WebResource;
    }


    /**
     *
     * Creates the skos:Concept elements for every dc element with an authority
     * The elements is built with the authority value and the dspace url.
     * The authority value must be a valid url or a handle from our dspace.
     *
     * @param itemDC        array of dcvalues objects to get the information from
     * @param nativeItem    dspace item object to get information from
     * @param baseUrl       string with the dspace url
     * @return              list of jdom elements
     */
    private List<Element> processSkosConcept(DCValue[] itemDC, Object nativeItem, String baseUrl)
    {
        List<Element> listElementsSkosConcept = new ArrayList<Element>();
        final String REGEX_HANDLE_PATTERN = "^\\d+/\\d+$";
        final String REGEX_HANDLE_VOCAB_PATTERN = "^.+_(\\d+_\\d+)$";
        Pattern patternHandleVocab = Pattern.compile(REGEX_HANDLE_VOCAB_PATTERN);
        Context context = ((HarvestedItemInfo)nativeItem).context;
        String prefixUrl = baseUrl + "/handle/";

        for (DCValue dcv : itemDC) {
            String authority;
            if (dcv.authority != null && !dcv.authority.isEmpty()) {
                Item itemAuth = null;
                if (!isValidURI(dcv.authority)) {
                    try {
                        Matcher matcherHandleVocab = patternHandleVocab.matcher(dcv.authority);
                        if (matcherHandleVocab.find()) dcv.authority = ((String) matcherHandleVocab.group(1)).replace('_', '/');
                        if (dcv.authority.matches(REGEX_HANDLE_PATTERN) && (itemAuth = (Item) HandleManager.resolveToObject(context, dcv.authority)) != null) {
                            authority = prefixUrl + dcv.authority;
                        } else continue;
                    } catch (SQLException e) {
                        //e.printStackTrace();
                        continue;
                    }
                } else authority = dcv.authority;
                Element skosConcept = null;
                try {
                    skosConcept = (dcv.element.equals("contributor") && dcv.qualifier.equals("author"))?new Element("Agent", EDM):new Element("Concept", SKOS);
                    skosConcept.setAttribute(new Attribute("about", authority, RDF));
                    Element prefLabel = new Element("prefLabel", SKOS);
                    if (dcv.language != null) prefLabel.setAttribute(new Attribute("lang", dcv.language, XML));
                    prefLabel.setText(dcv.value);
                    skosConcept.addContent(prefLabel);
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
     * @param baseUrl           string with the dspace url
     * @return                  jdom element
     */
    private Element processOreAgreggation(Item item, Bundle[] origBundles, Bundle[] thumbBundles, Bitstream bitstream
                                                 , String baseUrl)
    {

        Element oreAggregation = null;

        try {
            oreAggregation = new Element("Aggregation", ORE);

            String url = baseUrl + "/handle/" + item.getHandle();
            oreAggregation.setAttribute(new Attribute("about", url, RDF));

            createElementEDM(item, "aggregatedCHO", EDM, "identifier", null, oreAggregation, false);

            // edm:dataProvider
            oreAggregation.addContent(new Element("dataProvider", EDM).setText(ConfigurationManager.getProperty("dspace.name")));

            // url of the first original file
            String urlObject = baseUrl + "/bitstream/"
                                       + item.getHandle() + "/" + bitstream.getSequenceID() + "/"
                                       + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

            // edm:isShownAt
            oreAggregation.addContent(new Element("isShownAt", EDM).setText(url));

            // edm:isShownBy
            oreAggregation.addContent(new Element("isShownBy", EDM).setText(urlObject));

            // edm:object
            oreAggregation.addContent(new Element("object", EDM).setText(urlObject));

            // edm:hasView
            int i = 0;
            for (Bundle bundle : origBundles) {
                try {
                    Bitstream[] bitstreamsOrig = bundle.getBitstreams();
                    Bitstream[] bitstreamsThumb = null;
                    if (thumbBundles.length > i && thumbBundles[i] != null) bitstreamsThumb = thumbBundles[i].getBitstreams();
                    for (Bitstream bitstream1 : bitstreamsOrig) {
                        urlObject = baseUrl + "/bitstream/"
                                            + item.getHandle() + "/" + bitstream1.getSequenceID() + "/"
                                            + Util.encodeBitstreamName(bitstream1.getName(), Constants.DEFAULT_ENCODING);
                        String urlThumb = urlObject;
                        if (bitstreamsThumb != null) {
                            for (Bitstream bitThumb : bitstreamsThumb) {
                                if (bitThumb.getSequenceID() == bitstream1.getSequenceID()) {
                                    urlThumb = baseUrl + "/bitstream/"
                                                       + item.getHandle() + "/" + bitThumb.getSequenceID() + "/"
                                                       + Util.encodeBitstreamName(bitThumb.getName(), Constants.DEFAULT_ENCODING);
                                    break;
                                }
                            }
                        }

                        oreAggregation.addContent(new Element("hasView", EDM).setText(urlThumb));
                    }
                } catch (Exception ex) {

                }
                i++;
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
     */
    private void createElementEDM(Item item, String elementEDM, Namespace nameSpace, String elementDC, String qualifier, Element parent, boolean repeat)
    {
        DCValue[] elements = item.getDC(elementDC, qualifier, Item.ANY);
        if (elements.length > 0) {
            for (DCValue element : elements) {
                parent.addContent(new Element(elementEDM, nameSpace).setText(element.value));
                if (!repeat) break;
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
