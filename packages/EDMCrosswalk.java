/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 18/10/12
 * Time: 9:33
 * ESECrosswalk.java
 *
 * Date: $Date: 20120-10-18
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

@SuppressWarnings("deprecation")
public class EDMCrosswalk extends Crosswalk
{

    /* BEGIN EDMRIGHTS */
    private static final String EDMRIGHTS = "";
    /* END EDMRIGHTS */

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


    public EDMCrosswalk(Properties properties)
    {
        super("http://www.europeana.eu/schemas/edm/ http://www.europeana.eu/schemas/edm/EDM.xsd");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        return true;
    }

    public String createMetadata(Object nativeItem) throws CannotDisseminateFormatException
    {

        Element rdf_RDF = new Element("RDF", "rdf");

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

        Document doc = new Document(rdf_RDF);

        String baseUrl = ConfigurationManager.getProperty("dspace.url");

        List<Element> listElements = new ArrayList<Element>();

        Item item = ((HarvestedItemInfo) nativeItem).item;

        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        DCValue[] allEDM = item.getMetadata("edm", Item.ANY, Item.ANY, Item.ANY);

        Element ProviderCHO = processProviderCHO(item, baseUrl);
        listElements.add(ProviderCHO);

        Bundle[] origBundles = new Bundle[0];
        try {
            origBundles = item.getBundles("ORIGINAL");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (origBundles.length > 0) {
            Bundle[] thumbBundles = new Bundle[0];
            try {
                thumbBundles = item.getBundles("THUMBNAIL");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Bitstream[] bitstreams = origBundles[0].getBitstreams();
            if (bitstreams.length > 0) {
                Element WebResource = processWebResource(item, bitstreams[0], baseUrl);
                if (WebResource != null) listElements.add(WebResource);

                List<Element> listSkosConcept = processSkosConcept(allDC, nativeItem);
                if (listSkosConcept != null && listSkosConcept.size() > 0) {
                    for (Element skosConceptElement : listSkosConcept)
                        listElements.add(skosConceptElement);
                }
                Element oreAggregation = processOreAgreggation(item, origBundles, thumbBundles, bitstreams[0], baseUrl);
                if (oreAggregation != null) listElements.add(oreAggregation);
            }
        }

        for (Element element : listElements) {
            rdf_RDF.addContent(element);
        }

        doc.setContent(rdf_RDF);
        XMLOutputter xmlOutput = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setOmitDeclaration(true);
        xmlOutput.setFormat(format);

        return xmlOutput.outputString(doc);
    }


    private Element processProviderCHO(Item item, String baseUrl)
    {
        Element ProviderCHO = new Element("ProviderCHO", EDM);

        DCValue[] identifiers = item.getDC("identifier", "uri", null);
        if (identifiers.length > 0) ProviderCHO.setAttribute(new Attribute("about", identifiers[0].value, RDF));

        createElementDC(item, "contributor", DC, "contributor", Item.ANY, ProviderCHO, true);

        createElementDC(item, "coverage", DC, "coverage", null, ProviderCHO, true);

        createElementDC(item, "creator", DC, "creator", null, ProviderCHO, true);

        createElementDC(item, "date", DC, "date", null, ProviderCHO, true);

        createElementDC(item, "description", DC, "description", Item.ANY, ProviderCHO, true);

        createElementDC(item, "format", DC, "format", Item.ANY, ProviderCHO, true);

        createElementDC(item, "identifier", DC, "identifier", Item.ANY, ProviderCHO, true);

        createElementDC(item, "language", DC, "language", "iso", ProviderCHO, true);
        createElementDC(item, "language", DC, "language", null, ProviderCHO, true);

        createElementDC(item, "publisher", DC, "publisher", null, ProviderCHO, false);

        createElementDC(item, "relation", DC, "relation", null, ProviderCHO, false);

        createElementDC(item, "rights", DC, "rights", "holder", ProviderCHO, true);
        createElementDC(item, "rights", DC, "rights", "uri", ProviderCHO, true);

        createElementDC(item, "source", DC, "source", null, ProviderCHO, false);

        createElementDC(item, "subject", DC, "subject", Item.ANY, ProviderCHO, true);

        createElementDC(item, "title", DC, "title", null, ProviderCHO, true);

        createElementDC(item, "type", DC, "type", null, ProviderCHO, true);

        createElementDC(item, "alternative", DCTERMS, "title", "alternative", ProviderCHO, true);

        createElementDC(item, "created", DCTERMS, "date", "created", ProviderCHO, true);

        createElementDC(item, "extent", DCTERMS, "format", "extent", ProviderCHO, true);

        createElementDC(item, "hasFormat", DCTERMS, "relation", "hasformatof", ProviderCHO, true);

        createElementDC(item, "hasPart", DCTERMS, "relation", "haspart", ProviderCHO, true);

        createElementDC(item, "hasVersion", DCTERMS, "relation", "hasversion", ProviderCHO, true);

        createElementDC(item, "isPartOf", DCTERMS, "relation", "ispartof", ProviderCHO, true);
        createElementDC(item, "isPartOf", DCTERMS, "relation", "ispartofseries", ProviderCHO, true);

        createElementDC(item, "isReferencedBy", DCTERMS, "relation", "isreferencedby", ProviderCHO, true);

        createElementDC(item, "isReplacedBy", DCTERMS, "relation", "isreplacedby", ProviderCHO, true);

        createElementDC(item, "issued", DCTERMS, "date", "issued", ProviderCHO, true);

        createElementDC(item, "isVersionOf", DCTERMS, "relation", "isversionof", ProviderCHO, true);

        createElementDC(item, "medium", DCTERMS, "format", "medium", ProviderCHO, true);

        createElementDC(item, "provenance", DCTERMS, "description", "provenance", ProviderCHO, true);

        createElementDC(item, "replaces", DCTERMS, "relation", "replaces", ProviderCHO, true);

        createElementDC(item, "requires", DCTERMS, "relation", "requires", ProviderCHO, true);

        createElementDC(item, "spatial", DCTERMS, "coverage", "spatial", ProviderCHO, true);

        createElementDC(item, "tableOfContents", DCTERMS, "description", "tableofcontents", ProviderCHO, true);

        createElementDC(item, "temporal", DCTERMS, "coverage", "temporal", ProviderCHO, true);

        createElementDC(item, "temporal", DCTERMS, "coverage", "temporal", ProviderCHO, true);

        String currentLocation = null;
        try {
            currentLocation = item.getMetadata("edm", "currentLocation", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (currentLocation == null || currentLocation.isEmpty()) currentLocation = baseUrl + "/" + item.getHandle();
        ProviderCHO.addContent(new Element("currentLocation", EDM).setText(currentLocation));

        ProviderCHO.addContent(new Element("type", EDM).setText(processEDMType(item)));

        return ProviderCHO;
    }


    private String processEDMType(Item item)
    {
        String edmTypeElement = null;
        try {
            edmTypeElement = item.getMetadata("edm", "type", null, Item.ANY)[0].value;
        } catch (Exception e) {
        }
        if (edmTypeElement != null && !edmTypeElement.isEmpty()) return edmTypeElement;

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


    private Element processWebResource(Item item, Bitstream bitstream, String baseUrl)
    {
        Element WebResource = null;

        try {
            WebResource = new Element("WebResource", EDM);

            String url = baseUrl + "/bitstreams/"
                                 + item.getHandle() + "/" + bitstream.getSequenceID() + "/" + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

            WebResource.setAttribute(new Attribute("about", url, RDF));

            createElementDC(item, "rights", DC, "rights", null, WebResource, true);

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


    private List<Element> processSkosConcept(DCValue[] itemDC, Object nativeItem)
    {
        List<Element> listElementsSkosConcept = new ArrayList<Element>();
        final String REGEX_HANDLE_PATTERN = "^\\d+/\\d+$";
        Context context = ((HarvestedItemInfo)nativeItem).context;
        String prefixUrl = ConfigurationManager.getProperty("dspace.baseUrl") + "/handle/";

        for (DCValue dcv : itemDC) {
            String authority;
            if (dcv.authority != null && !dcv.authority.isEmpty()) {
                if (!isValidURI(dcv.authority)) {
                    try {
                        if (dcv.authority.matches(REGEX_HANDLE_PATTERN) && HandleManager.resolveToObject(context, dcv.authority) != null) {
                            authority = prefixUrl + dcv.authority;
                        } else continue;
                    } catch (SQLException e) {
                        //e.printStackTrace();
                        continue;
                    }
                } else authority = dcv.authority;
                Element skosConcept = null;
                try {
                    skosConcept = new Element("Concept", SKOS);
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


    private Element processOreAgreggation(Item item, Bundle[] origBundles, Bundle[] thumbBundles, Bitstream bitstream
                                                 , String baseUrl)
    {

        Element oreAggregation = null;

        try {
            oreAggregation = new Element("Aggregation", ORE);

            String url = baseUrl + "/" + item.getHandle();
            oreAggregation.setAttribute(new Attribute("about", url, RDF));

            createElementDC(item, "aggregatedCHO", EDM, "identifier", null, oreAggregation, false);

            oreAggregation.addContent(new Element("dataProvider", EDM).setText(ConfigurationManager.getProperty("dspace.name")));

            String urlObject = baseUrl + "/bitstreams/"
                                       + item.getHandle() + "/" + bitstream.getSequenceID() + "/"
                                       + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);

            oreAggregation.addContent(new Element("isShownAt", EDM).setText(url));

            oreAggregation.addContent(new Element("isShownBy", EDM).setText(urlObject));

            oreAggregation.addContent(new Element("object", EDM).setText(urlObject));

            int i = 0;
            for (Bundle bundle : origBundles) {
                try {
                    Bitstream[] bitstreamsOrig = bundle.getBitstreams();
                    Bitstream[] bitstreamsThumb = null;
                    if (thumbBundles.length > i && thumbBundles[i] != null) bitstreamsThumb = thumbBundles[i].getBitstreams();
                    for (Bitstream bitstream1 : bitstreamsOrig) {
                        urlObject = baseUrl + "/bitstreams/"
                                            + item.getHandle() + "/" + bitstream1.getSequenceID() + "/"
                                            + Util.encodeBitstreamName(bitstream1.getName(), Constants.DEFAULT_ENCODING);
                        String urlThumb = urlObject;
                        if (bitstreamsThumb != null) {
                            for (Bitstream bitThumb : bitstreamsThumb) {
                                if (bitThumb.getSequenceID() == bitstream1.getSequenceID()) {
                                    urlThumb = baseUrl + "/bitstreams/"
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

            oreAggregation.addContent(new Element("provider", EDM).setText(ConfigurationManager.getProperty("dspace.name")));

            createElementDC(item, "rights", DC, "rights", null, oreAggregation, true);

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

    private void createElementDC(Item item, String elementEDM, Namespace nameSpace, String elementDC, String qualifier, Element ProviderCHO, boolean repeat)
    {
        DCValue[] elements = item.getDC(elementDC, qualifier, Item.ANY);
        if (elements.length > 0) {
            for (DCValue element : elements) {
                ProviderCHO.addContent(new Element(elementEDM, nameSpace).setText(element.value));
                if (!repeat) break;
            }
        }
    }

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
