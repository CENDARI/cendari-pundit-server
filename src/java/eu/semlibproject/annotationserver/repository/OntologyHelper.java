/*
 * Copyright (c) 2013 Net7 SRL, <http://www.netseven.it/>
 * 
 * This file is part of Pundit: Annonation Server.
 * 
 * Pundit: Annonation Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Pundit: Annonation Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Pundit: Annonation Server.  If not, see <http ://www.gnu.org/licenses/>.
 *
 * See LICENSE.TXT or visit <http://thepund.it> for the full text of the license.
 */

package eu.semlibproject.annotationserver.repository;

/**
 *
 * @author Michele Nucci
 */
public class OntologyHelper {
    
    // Namespaces
    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String DCTERMS_NAMESPACE = "http://purl.org/dc/terms/";
    public static final String DCTERMS_ELEMENTS = "http://purl.org/dc/elements/1.1/";
    public static final String OAC_NAMESPACE = "http://www.openannotation.org/ns/";
    public static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static final String SWN_NAMESPACE = "http://swickynotes.org/notebook/resource/";
    public static final String SEMLIB_NAMESPACE = "http://purl.org/pundit/ont/ao#";
    public static final String OV_NAMESPACE = "http://open.vocab.org/terms/";
    
    // Classes and properties
    public static final String RDF_TYPE = "type";
    public static final String RDFS_LABEL = "label";
    public static final String DCTERMS_CREATED = "created";
    public static final String DCTERMS_MODIFIED = "modified";
    public static final String DCTERMS_CREATOR = "creator";
    public static final String DCTERMS_ISPARTOF = "isPartOf";
    public static final String OAC_ANNOTATION = "Annotation";
    public static final String OAC_HASBODY = "hasBody";
    public static final String OAC_HASTARGET = "hasTarget";
    public static final String OAC_BOBY = "Body";
    public static final String FOAF_PERSON = "Person";
    public static final String FOAF_OPENID = "openid";
    public static final String FOAF_GIVENAME = "givenName";
    public static final String FOAF_FAMILYNAME = "familyName";
    public static final String FOAF_NAME = "name";
    public static final String FOAF_MBOX = "mbox";
    public static final String FOAF_SHA1SUM = "sha1sum";
    public static final String SEMLIB_NOTEBOOK = "Notebook";
    public static final String SEMLIB_INCLUDES = "includes";
    public static final String SEMLIB_AUTHORNAME = "authorName";    
    public static final String SEMLIB_GRAPH = "graph";
    public static final String SEMLIB_ID = "id";
    public static final String SEMLIB_HASPAGECONTEXT = "hasPageContext";   
    public static final String SEMLIB_ITEMS = "items";
    public static final String SEMLIB_IS_INCLUDED_IN = "isIncludedIn";
    public static final String SWN_GRAPH_DEFAULT = "defaultGraph";
    public static final String SWN_GRAPH_ITEMS = "itemsGraph";
    public static final String SWN_GRAPH_USERS = "usersGraph";
    public static final String OV_VISIBILITY = "visibility";
    
    public static final String URI_RDF_TYPE = getURI(RDF_NAMESPACE, RDF_TYPE);
    public static final String URI_RDFS_LABEL = getURI(RDFS_NAMESPACE, RDFS_LABEL);
    public static final String URI_DC_CREATOR = getURI(DCTERMS_NAMESPACE, DCTERMS_CREATOR);
    public static final String URI_DC_CREATED = getURI(DCTERMS_NAMESPACE, DCTERMS_CREATED);
    public static final String URI_DC_MODIFIED = getURI(DCTERMS_NAMESPACE, DCTERMS_MODIFIED);
    public static final String URI_DC_ISPARTOF = getURI(DCTERMS_NAMESPACE, DCTERMS_ISPARTOF);
    public static final String URI_DCELEMENT_CREATOR = getURI(DCTERMS_ELEMENTS, DCTERMS_CREATOR);
    public static final String URI_OAC_ANNOTATION = getURI(OAC_NAMESPACE, OAC_ANNOTATION);
    public static final String URI_OAC_HASBODY = getURI(OAC_NAMESPACE, OAC_HASBODY);
    public static final String URI_OAC_BODY = getURI(OAC_NAMESPACE, OAC_BOBY);
    public static final String URI_OAC_HASTARGET = getURI(OAC_NAMESPACE, OAC_HASTARGET);
    public static final String URI_FOAF_PERSON = getURI(FOAF_NAMESPACE, FOAF_PERSON);
    public static final String URI_FOAF_OPENID = getURI(FOAF_NAMESPACE, FOAF_OPENID);
    public static final String URI_FOAF_GIVENNAME = getURI(FOAF_NAMESPACE, FOAF_GIVENAME);
    public static final String URI_FOAF_FAMILYNAME = getURI(FOAF_NAMESPACE, FOAF_FAMILYNAME);
    public static final String URI_FOAF_NAME = getURI(FOAF_NAMESPACE, FOAF_NAME);
    public static final String URI_FOAF_MBOX = getURI(FOAF_NAMESPACE, FOAF_MBOX);
    public static final String URI_FOAF_SHA1SUM = getURI(FOAF_NAMESPACE, FOAF_SHA1SUM);
    public static final String URI_SWN_DEFGRAPH = getURI(SWN_NAMESPACE, SWN_GRAPH_DEFAULT);
    public static final String URI_SWN_USERSGRAPH = getURI(SWN_NAMESPACE, SWN_GRAPH_USERS);
    public static final String URI_SWN_ITEMSGRAPH = getURI(SWN_NAMESPACE, SWN_GRAPH_ITEMS);
    
    public static final String URI_SEMLIB_NOTEBOOK = getURI(SEMLIB_NAMESPACE, SEMLIB_NOTEBOOK);
    public static final String URI_SEMLIB_INCLUDES = getURI(SEMLIB_NAMESPACE, SEMLIB_INCLUDES);    
    public static final String URI_SEMLIB_GRAPH = getURI(SEMLIB_NAMESPACE, SEMLIB_GRAPH);    
    public static final String URI_SEMLIB_ID = getURI(SEMLIB_NAMESPACE, SEMLIB_ID);
    public static final String URI_SEMLIB_HASPAGECONTEXT = getURI(SEMLIB_NAMESPACE, SEMLIB_HASPAGECONTEXT);  
    public static final String URI_SEMLIB_ITEMS = getURI(SEMLIB_NAMESPACE, SEMLIB_ITEMS);
    public static final String URI_SEMLIB_IS_INCLUDED_IN = getURI(SEMLIB_NAMESPACE, SEMLIB_IS_INCLUDED_IN);
    
    public static final String URI_OV_VISIBILITY = getURI(OV_NAMESPACE, OV_VISIBILITY);
    
    private static String getURI(String namespace, String term) {
        return namespace + term;
    }
    
}
