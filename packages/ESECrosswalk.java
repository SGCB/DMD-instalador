/*
 * ESECrosswalk.java
 *
 * Date: $Date: 2010-03-07
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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.SQLException;

import org.dspace.app.util.Util;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.core.Constants;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

/**
 * An OAICat Crosswalk implementation that extracts unqualified Dublin Core AND Europeana ESE
 * DSpace items into the oai_dc format.
 *
 * This is based on Dublin Core Plugin
 * 
 * @author Evangelos Banos 
 * @created 2010-03-07
 */
public class ESECrosswalk extends Crosswalk
{
	// Pattern containing all the characters we want to filter out / replace
	// converting a String to xml
	private static final Pattern invalidXmlPattern =
		Pattern.compile("([^\\t\\n\\r\\u0020-\\ud7ff\\ue000-\\ufffd\\u10000-\\u10ffff]+|[&<>])");
    
    // Pattern to test for only true dc elements.
    private static final Pattern dcElementPattern = Pattern
            .compile("(^(title|creator|subject|description|"
                    + "publisher|contributor|date|"
                    + "format|identifier|source|language|"
                    + "provider|isShownBy|isShownAt|object|country|year"
                    + "relation|coverage|rights|type|object)$)");
	
    public ESECrosswalk(Properties properties)
    {
		super("http://www.europeana.eu/schemas/ese/ http://www.europeana.eu/schemas/ese/ESE-V3.3.xsd");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        // We have DC for everything
        return true;
    }

    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        Item item = ((HarvestedItemInfo) nativeItem).item;

        // Get all the DC elements
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

		// Get all the Europeana elements
		DCValue[] allEuropeana = item.getMetadata("europeana", Item.ANY, Item.ANY, Item.ANY);

		// Get Thumbnail Bundles (useful to find if there is any jpg to use for thumbnail in europeana:object element
		String baseUrl = ConfigurationManager.getProperty("dspace.url");
		String thumbnail_url = null;
		Bundle[] origBundles = null;
		Bundle[] thumbBundles = null;
        try {
			origBundles = item.getBundles("ORIGINAL");
            thumbBundles = item.getBundles("THUMBNAIL");
		} catch (SQLException sqlE) {
			;
		}
		
		// check if a bistream is available in the thumbnails bundle and return is as the item's object
		if (thumbBundles.length > 0){
			Bitstream[] bitstreams = thumbBundles[0].getBitstreams();
			if (bitstreams.length > 0){
				Bitstream tb = bitstreams[0];
				try
                {
					thumbnail_url = baseUrl + "/retrieve/" + tb.getID() + "/" + 
					Util.encodeBitstreamName(tb.getName(), Constants.DEFAULT_ENCODING);
                }
				catch(Exception e)
                {
                }
			}
		}

        StringBuffer metadata = new StringBuffer();

        metadata.append("<europeana:record xmlns:europeana=\"http://www.europeana.eu/schemas/ese/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");

	/**
	 * DC Elements
	 */
        for (int i = 0; i < allDC.length; i++)
        {
            String element = allDC[i].element;
            String qualifier = allDC[i].qualifier;
             String language = allDC[i].language;

         
             List notAcceptedLanguageString=new ArrayList<String>();
             notAcceptedLanguageString.add("*");
             notAcceptedLanguageString.add("");
             notAcceptedLanguageString.add("null");
             notAcceptedLanguageString.add(item.ANY);
             notAcceptedLanguageString.add(null);

            if (!notAcceptedLanguageString.contains(language)){
            
                language = " xml:lang=\""+language+"\">";
         
            }else{
                 language = ">";
            }

            // Do not include description.provenance
            boolean provenance = "description".equals(element)
                    && "provenance".equals(qualifier);

            // Include only OAI DC (guard against outputing invalid DC)
            if (dcElementPattern.matcher(element).matches() && !provenance)
            {
                // contributor.author exposed as 'creator'
                if ("contributor".equals(element) && "author".equals(qualifier))
                {
                    element = "creator";
                }

                String value = allDC[i].value;
                
                // Escape XML chars <, > and &
                // Also replace all invalid characters with ' '
                if (value != null)
                {
                	StringBuffer valueBuf = new StringBuffer(value.length());
                	Matcher xmlMatcher = invalidXmlPattern.matcher(value.trim());
                	while (xmlMatcher.find())
                	{
                		String group = xmlMatcher.group();
                		
                		// group will either contain a character that we need to encode for xml
                		// (ie. <, > or &), or it will be an invalid character
                		// test the contents and replace appropriately
                		
                		if (group.equals("&"))
                			xmlMatcher.appendReplacement(valueBuf, "&amp;");
                		else if (group.equals("<"))
                   			xmlMatcher.appendReplacement(valueBuf, "&lt;");
                		else if (group.equals(">"))
                   			xmlMatcher.appendReplacement(valueBuf, "&gt;");
                		else
                			xmlMatcher.appendReplacement(valueBuf, " ");
                	}
                	
                	// add bit of the string after the final match
                	xmlMatcher.appendTail(valueBuf);
	
	                metadata.append("<dc:").append(element).append(language).append(
	                        valueBuf.toString()).append("</dc:").append(element).append(">");
				}
            }
        }

		/**
		 * Bugfix for Europeana content checker,
		 * If the first element is type, parse error !!!
		 * If the first element is provider, no problem !!!
		 *
		 *	Europeana Elements Order must be the following or there will be Validation Error and no harvesting
		 *	europeana:unstored
		 *  europeana:object
		 *  europeana:provider
		 *  europeana:type
		 *  europeana:isShownBy
		 *  europeana:isShownAt
		 */

		/**
		 *	We store europeana element in different StringBuffer items because in the end, we need to
		 *	output them in a SPECIFIC order
		 **/
		StringBuffer europeana_unstored = new StringBuffer();
		StringBuffer europeana_object = new StringBuffer();
		StringBuffer europeana_provider = new StringBuffer();
		StringBuffer europeana_type = new StringBuffer();
		StringBuffer europeana_isShownBy = new StringBuffer();
		StringBuffer europeana_isShownAt = new StringBuffer();

		/**
		 * Europeana Elements
		 */
        for (int i = 0; i < allEuropeana.length; i++)
        {
            String element = allEuropeana[i].element;
            String qualifier = allEuropeana[i].qualifier;

            // Do not include description.provenance
            boolean provenance = "description".equals(element)
                    && "provenance".equals(qualifier);

            // Include only OAI DC (guard against outputing invalid DC)
            if (dcElementPattern.matcher(element).matches() && !provenance)
            {
                // contributor.author exposed as 'creator'
                if ("contributor".equals(element) && "author".equals(qualifier))
                {
                    element = "creator";
                }

                String value = allEuropeana[i].value;
                
                // Escape XML chars <, > and &
                // Also replace all invalid characters with ' '
                if (value != null)
                {
                	StringBuffer valueBuf = new StringBuffer(value.length());
                	Matcher xmlMatcher = invalidXmlPattern.matcher(value.trim());
                	while (xmlMatcher.find())
                	{
                		String group = xmlMatcher.group();
                		
                		// group will either contain a character that we need to encode for xml
                		// (ie. <, > or &), or it will be an invalid character
                		// test the contents and replace appropriately
                		
                		if (group.equals("&"))
                			xmlMatcher.appendReplacement(valueBuf, "&amp;");
                		else if (group.equals("<"))
                   			xmlMatcher.appendReplacement(valueBuf, "&lt;");
                		else if (group.equals(">"))
                   			xmlMatcher.appendReplacement(valueBuf, "&gt;");
                		else
                			xmlMatcher.appendReplacement(valueBuf, " ");
                	}
                	
                	// add bit of the string after the final match
                	xmlMatcher.appendTail(valueBuf);
			
					// we do all this because the order of the european objects is important	
					if(element.compareTo("unstored") == 0) {
		                europeana_unstored.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					} else if(element.compareTo("object") == 0) {
		                europeana_object.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					} else if(element.compareTo("provider") == 0) {
		                europeana_provider.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					} else if(element.compareTo("type") == 0) {
		                europeana_type.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					} else if(element.compareTo("isShownBy") == 0) {
		                europeana_isShownBy.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					} else if(element.compareTo("isShownAt") == 0) {
		                europeana_isShownAt.append("<europeana:").append(element).append(">").append(valueBuf.toString()).append("</europeana:").append(element).append(">");
					}
                }
            }
        }

		// europeana:unstored
		if(europeana_unstored.toString().compareTo("") != 0) {
			metadata.append(europeana_unstored.toString());
		}
	
		// europeana:object
		// if europeana_object is empty, use Thumbnail (generated by dspace) URL, if exists
		if(europeana_object.toString().compareTo("") == 0 && thumbnail_url != null) {
			europeana_object.append("<europeana:object>").append(thumbnail_url).append("</europeana:object>");
		}
		if(europeana_object.toString().compareTo("") != 0) {
			metadata.append(europeana_object.toString());
		}

		// europeana:provider
		if(europeana_provider.toString().compareTo("") != 0) {
			metadata.append(europeana_provider.toString());
		}

		// europeana:type
		if(europeana_type.toString().compareTo("") != 0) {
			metadata.append(europeana_type.toString());
		}
		
		// europeana:isShownBy
		if(europeana_isShownBy.toString().compareTo("") != 0) {
			metadata.append(europeana_isShownBy.toString());
		}
		
		// europeana:isShownAt
		if(europeana_isShownAt.toString().compareTo("") != 0) {
			metadata.append(europeana_isShownAt.toString());
		}

		//
		// if there is neither europeana:isShownBy nor europeana:isShownAt, 
		// try to find out the item's URL by checking if the dc:identifier element is a URL (if it starts with http:// or https://)
		//
		if(europeana_isShownBy.toString().compareTo("") == 0 && europeana_isShownAt.toString().compareTo("") == 0) {
			for (int i = 0; i < allDC.length; i++)
       		{
            	String element = allDC[i].element;
				if(element.compareTo("identifier") == 0) {
					String value = allDC[i].value;
					if(value.startsWith("http://") || value.startsWith("https://")) {
		                metadata.append("<europeana:isShownAt>").append(value).append("</europeana:isShownAt>");
						break;
					}
				}
			}
		}

        metadata.append("</europeana:record>");

        return metadata.toString();
    }
}
