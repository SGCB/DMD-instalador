<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns="http://www.loc.gov/MARC21/slim"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:edm="http://www.europeana.eu/schemas/edm/"
    exclude-result-prefixes="dim">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="text()"></xsl:template>

    <xsl:variable name="ident_uri" select="//dim:field[@element='identifier'][@qualifier='uri']" />
    <xsl:variable name="ident_uri_orig">http://example.com/bitstream/handle/</xsl:variable>
    <xsl:variable name="handle" select="substring-after($ident_uri, 'http://hdl.handle.net/')" />


    <xsl:template name="bundles-files">
        <xsl:param name="list" />
        <xsl:param name="count_bundles" />
        <xsl:variable name="newlist" select="$list" />
        <xsl:variable name="first" select="substring-before(substring($newlist, 2, string-length($newlist)), ':')" />
        <xsl:variable name="first-char" select="substring($first, 1, 1)" />
        <xsl:variable name="first-final">
            <xsl:choose>
                <xsl:when test="contains('&#xA;', $first-char)">
                    <xsl:value-of select="substring($first, 2, string-length($first))" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$first" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="remaining" select="substring-after($newlist, '(MD5)')" />
        <edm:hasView>
            <xsl:attribute name="rdf:about">
                <xsl:copy-of select="concat($ident_uri_orig, $handle, '/', $first-final)"/>
            </xsl:attribute>
        </edm:hasView>
        <xsl:if test="$count_bundles = 1">
            <edm:isShownBy>
                <xsl:attribute name="rdf:about">
                    <xsl:copy-of select="concat($ident_uri_orig, $handle, '/', $first-final)"/>
                </xsl:attribute>
            </edm:isShownBy>
        </xsl:if>
        <xsl:if test="string-length($remaining) &gt; 0">
            <xsl:call-template name="bundles-files">
                <xsl:with-param name="list" select="$remaining" />
                <xsl:with-param name="count_bundles" select="$count_bundles + 1" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>



    <xsl:template name="popNumbers">
        <xsl:param name="popString"/>
        <xsl:variable name="length" select="string-length($popString)"/>
        <xsl:choose>
            <xsl:when test="$length=0"/>
            <xsl:when test="contains('1234567890', substring($popString,1,1))">
                <xsl:call-template name="popNumbers">
                    <xsl:with-param name="popString" select="substring($popString,2,$length)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="not($popString)"/>
            <xsl:otherwise><xsl:value-of select="$popString"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="/">

        <rdf:RDF xmlns:dcterms="http://purl.org/dc/terms/" xmlns:edm="http://www.europeana.eu/schemas/edm/"
          xmlns:enrichment="http://www.europeana.eu/schemas/edm/enrichment/"
          xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:wgs84="http://www.w3.org/2003/01/geo/wgs84_pos#"
          xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:oai="http://www.openarchives.org/OAI/2.0/"
          xmlns:ore="http://www.openarchives.org/ore/terms/"
          xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
          xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.w3.org/1999/02/22-rdf-syntax-ns# EDM.xsd">

            <edm:ProvidedCHO>
                <xsl:attribute name="rdf:about">
                    <xsl:value-of select="//dim:field[@element='identifier'][@qualifier='uri']"/>
                </xsl:attribute>

                <dc:identifier>
                    <xsl:copy-of select="$ident_uri"/>
                </dc:identifier>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='contributor'][@qualifier='author']">
                    <dc:contributor>
                        <xsl:value-of select="."/>
                    </dc:contributor>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='title'][not(@qualifier) or @qualifier='']">
                    <dc:title>
                        <xsl:value-of select="."/>
                    </dc:title>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='format'][not(@qualifier) or @qualifier='']">
                    <dc:format>
                        <xsl:value-of select="."/>
                    </dc:format>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='subtitle'][not(@qualifier) or @qualifier='']">
                    <dcterms:alternative>
                        <xsl:value-of select="."/>
                    </dcterms:alternative>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='publisher'][@qualifier='place']">
                    <dcterms:spatial>
                        <xsl:value-of select="."/>
                    </dcterms:spatial>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='publisher'][not(@qualifier) or @qualifier='']">
                    <dc:publisher>
                        <xsl:value-of select="."/>
                    </dc:publisher>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='date'][not(@qualifier) or @qualifier='']">
                    <dc:date>
                        <xsl:value-of select="."/>
                    </dc:date>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='format'][@qualifier='extent']">
                    <dcterms:extent>
                        <xsl:value-of select="."/>
                    </dcterms:extent>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='format'][@qualifier='size']">
                    <dc:format>
                        <xsl:value-of select="."/>
                    </dc:format>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='format'][@qualifier='accompanyingmaterial']">
                    <dc:format>
                        <xsl:value-of select="."/>
                    </dc:format>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][not(@qualifier) or @qualifier='']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='contains']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='event']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='abstract']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='languagenote']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='contributornote']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='titlenote']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='note']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='tableofcontents']">
                    <dcterms:tableOfContents>
                        <xsl:value-of select="."/>
                    </dcterms:tableOfContents>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='subject'][not(@qualifier) or @qualifier='']">
                    <dc:subject>
                        <xsl:value-of select="."/>
                    </dc:subject>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='title'][@qualifier='othertitles']">
                    <dcterms:alternative>
                        <xsl:value-of select="."/>
                    </dcterms:alternative>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='contributor'][@qualifier='performer']">
                    <dc:contributor>
                        <xsl:value-of select="."/>
                    </dc:contributor>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='item']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='identifier'][@qualifier='callnumber']">
                    <dc:description>
                        <xsl:value-of select="."/>
                    </dc:description>
                </xsl:for-each>

                <edm:type></edm:type>
                <edm:language></edm:language>

            </edm:ProvidedCHO>

            <ore:Aggregation>
                <xsl:attribute name="rdf:about">
                    <xsl:copy-of select="$ident_uri"/>
                </xsl:attribute>

                <edm:dataProvider></edm:dataProvider>

                <edm:provider></edm:provider>

                <xsl:for-each select="//dim:field[@mdschema='dc'][@element='description'][@qualifier='provenance']">
                    <xsl:variable name="list_aux" select="substring-after(text(), 'No. of bitstreams:')" />
                    <xsl:variable name="list_aux2">
                        <xsl:call-template name="popNumbers">
                            <xsl:with-param name="popString" select="substring($list_aux, 2, string-length($list_aux))" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:call-template name="bundles-files">
                        <xsl:with-param name="list" select="$list_aux2" />
                        <xsl:with-param name="count_bundles" select="1" />
                    </xsl:call-template>
                </xsl:for-each>

                <edm:isShownAt>
                    <xsl:attribute name="rdf:about">
                        <xsl:copy-of select="$ident_uri"/>
                    </xsl:attribute>
                </edm:isShownAt>


                <edm:rights rdf:resource="http://example.com"/>

            </ore:Aggregation>

        </rdf:RDF>

    </xsl:template>

</xsl:stylesheet>
