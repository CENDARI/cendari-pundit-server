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

import eu.semlibproject.annotationserver.JSONRDFConverter;
import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.SesameRDFJSONConverter;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.RepositoryManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openrdf.model.*;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.http.HTTPTupleQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Main singleton class that implement the connection to
 * a Sesame remote repository.
 * 
 * @author Michele Nucci
 */
public class SesameRepository implements RDFRepository {

    // A Sesame HTTPRepository
    private HTTPRepository sesameHTTPRepository = null;

    // The Logger
    private Logger logger = Logger.getLogger(SesameRepository.class.getName());
    
    
    /**
     * Default constructor
     */
    public SesameRepository() {                    
        try {
            initRepository();
        } catch (RepositoryInitializationException ex) {
            logger.log(Level.SEVERE, "Unable to intialize the main repository.", ex);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    private synchronized void initRepository() throws RepositoryInitializationException {

        // Get configuration from file
        ConfigManager configManager = ConfigManager.getInstance();

        String repositoryURL = configManager.getRepositoryUrl();
        String repositoryID = configManager.getRepositoryID();
        boolean usingAuthentication = configManager.useAuthenticationForRepository();

        // Creating the main HTTPRepository
        sesameHTTPRepository = new HTTPRepository(repositoryURL, repositoryID);                
        
        // This string will be parsed only is sesame.useauthentication is YES (little optimization)
        String repositoryUsername = null;
        String repositoryPassword = null;

        if (usingAuthentication == true) {
            repositoryUsername = configManager.getUsername();
            repositoryPassword = configManager.getPassword();

            if (StringUtils.isBlank(repositoryUsername) || StringUtils.isBlank(repositoryPassword)) {
                throw new RepositoryInitializationException("Username and/or password for main repository/db not specified. See the web.xml file.");
            }

            sesameHTTPRepository.setUsernameAndPassword(repositoryUsername, repositoryPassword);
        }

        try {

            // At this the repository configuration shoud be ok so, we will try to initialize it
            sesameHTTPRepository.initialize();

            logger.log(Level.INFO, "Sesame Repository Initialized!");

            // Check if the infos about anonymous user already exists. If not, add it to the repository.
            // The anonymous user is the default user for not logged users.            
            if (!configManager.isAnonymousUserInitialized()) {
                User anonymousUser = User.createAnonymousUser();
                boolean anonymousUserInitialized = this.initOrUpdateUserInfos(anonymousUser);                        
                configManager.setAnonymousUserInitialized(anonymousUserInitialized);
            }                        
            
        } catch (Exception ex) {

            logger.log(Level.SEVERE, "Sesame Initialization Exeption", ex);

            // In this case throw our generic repository exception
            throw new RepositoryInitializationException(ex.getMessage());
        }

    }

    
    /**
     * {@inheritDoc}
     */ 
    public boolean publicNotebookExists() throws RepositoryException {
        
        RepositoryConnection connection = null;
        try {
            
            boolean result = false;
            connection = sesameHTTPRepository.getConnection();
            
            ValueFactory factory = connection.getValueFactory();
            
            String query = "SELECT ?n WHERE { ?n <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_SEMLIB_NOTEBOOK + "> . " +
                           " ?n <" + OntologyHelper.URI_RDFS_LABEL + "> \"" + SemlibConstants.NOTEBOOK_PUBLIC + "\" }";

            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            tupleQuery.setIncludeInferred(false);
            TupleQueryResult tuples = tupleQuery.evaluate();
                        
            while (tuples.hasNext()) {
                BindingSet bindingSet = tuples.next();
                Value notebookURI = bindingSet.getValue("n");
                String notebookID = notebookURI.toString();
                if (StringUtils.isNotBlank(notebookID)) {
                    result = true;
                    break;
                }
            }            
            
            return result;
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        } finally {
            try {
                connection.close();
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
        }
                
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public boolean annotationExists(String annotationID) throws RepositoryException {
        
        String strAnnotationURI = Annotation.getURIFromID(annotationID);
        
        RepositoryConnection connection = null;
        try {
            connection = sesameHTTPRepository.getConnection();
            
            ValueFactory factory = connection.getValueFactory();
            
            boolean result = ( connection.hasStatement(null, factory.createURI(OntologyHelper.URI_SEMLIB_INCLUDES), factory.createURI(strAnnotationURI), true) 
                               && 
                               connection.hasStatement(factory.createURI(strAnnotationURI), RDF.TYPE, factory.createURI(OntologyHelper.URI_OAC_ANNOTATION), true) 
                             );
                        
            return result;
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        } finally {
            try {
                connection.close();
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
        }        
    }
    
        
    /**
     * {@inheritDoc}
     */
    public boolean userExists(String userID) throws RepositoryException {
        
        String strUserIDURI = User.getURIFromID(userID);
        
        RepositoryConnection connection = null;
        try {
            connection = sesameHTTPRepository.getConnection();
            
            ValueFactory factory = connection.getValueFactory();                       
            
            boolean result = connection.hasStatement(factory.createURI(strUserIDURI), RDF.TYPE, factory.createURI(OntologyHelper.URI_FOAF_PERSON), true, factory.createURI(OntologyHelper.URI_SWN_USERSGRAPH));
            
            return result;
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);            
            throw new RepositoryException();
        } finally {
            try {
                connection.close();
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
        }
    }
    
    
    /**
     * Used only for test
     * 
     * @param userID
     * @return 
     */
    public String getCurrentActiveNotebookID(String userID) {
        
         try {
            
            String notebookID = null;
            String query = "SELECT ?s WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_SEMLIB_NOTEBOOK +"> }";
                
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
                HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                tupleQuery.setIncludeInferred(false);
                TupleQueryResult notebooks = tupleQuery.evaluate();
                
                while (notebooks.hasNext()) {
                    BindingSet bindingSet = notebooks.next();
                    Value notebookURI = bindingSet.getValue("s");
                    notebookID = notebookURI.toString();
                    break;
                }
                
                connection.close();
                
                // Extract the notebookID
                int lastIndexOf = notebookID.lastIndexOf("/");
                notebookID = notebookID.substring(lastIndexOf+1, notebookID.length());
                
                return notebookID;
                
        } catch (Exception ex) {
            return null;
        }
         
    }
    
    
    /**
     * {@inheritDoc}
     */        
    public String getNotebookMetadata(String notebookID, String acceptedFormat) throws RepositoryException {
        
        boolean notebookPublic = false;
        try {
            notebookPublic = RepositoryManager.getInstance().getCurrentDataRepository().isNotebookPublic(notebookID);
        } catch (RepositoryException re) {
            return null;
        }
        
        String notebookURI = Notebook.getURIFromID(notebookID);
        
        String query = "CONSTRUCT { <" + notebookURI + "> <" + OntologyHelper.URI_OV_VISIBILITY + "> \"" + ( (notebookPublic) ? SemlibConstants.PUBLIC : SemlibConstants.PRIVATE)  + "\" . <" + notebookURI + "> ?p ?o . <" + notebookURI + "> <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName } WHERE { <" + notebookURI + "> ?p ?o . OPTIONAL {<" + notebookURI + "> <" + OntologyHelper.URI_DC_CREATOR + "> ?author  . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName }}";
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();

            try {
                GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                return createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, true, true);
            } catch (QueryEvaluationException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } catch (RDFHandlerException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } catch (MalformedQueryException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } finally {
                connection.close();
            }

        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        }        
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public String getNotebookAnnotationListAndMetadata(Notebook notebook, int limit, int offset, String orderBy, boolean desc, String acceptedFormat) throws RepositoryException {

        String qOrderBy = getPropertyForOrdering(orderBy);
                                
        // Check ordering ASC (default), DESC
        String orderMode;
        if (desc) {
            orderMode = "ORDER BY DESC(?c) ";
        } else {
            orderMode = "ORDER BY ?c ";
        }

        
        if (limit == -1 && offset == -1) {
            // queries without limit and offset are faster (only one queries to obtain all statements
            String annotationsQuery = "CONSTRUCT {?s ?p ?o . ?s <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName} WHERE { <" + notebook.getURI() +"> <" + OntologyHelper.URI_SEMLIB_INCLUDES +"> ?s . ?s ?p ?o . ?s <" + qOrderBy +"> ?c . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName} } " + orderMode;
            try {
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
                                
                try {
                    GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, annotationsQuery);                    
                    return createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, true, true);
                } catch (QueryEvaluationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (RDFHandlerException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (MalformedQueryException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } finally {
                    connection.close();
                }
                                
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
            
        } else {
            // query with limit and offset
            // 1. get all annotation URI ordered by ORDER BY with LIMIT and OFFSET
            // 2. for each annotation get all related metadata            
            String annotationsQuery = "SELECT DISTINCT ?a WHERE { <" + notebook.getURI() + "> <" + OntologyHelper.URI_SEMLIB_INCLUDES + "> ?a; <" + qOrderBy + "> ?c } " + orderMode;                        
            
            if (limit > 0) {
                annotationsQuery += "LIMIT " + limit + " ";
            }
            
            if (offset > 0) {
                annotationsQuery += "OFFSET " + offset;
            }
            
            try {
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
                
                try {
                    HTTPTupleQuery tupleQuery         = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, annotationsQuery);
                    TupleQueryResult tupleAnnotations = tupleQuery.evaluate();

                    int resultsCounter = 0;                    
                    String metadataQuery = "CONSTRUCT {?s ?p ?o . ?s <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName} WHERE { ?s ?p ?o . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName} . FILTER( ";
                    
                    while (tupleAnnotations.hasNext()) {                        
                        resultsCounter++;
                        BindingSet bindingSet = tupleAnnotations.next();
                        Value annotationURI   = bindingSet.getValue("a");                        
                        metadataQuery += "?s = <" + annotationURI +"> || ";
                    }
                    
                    if (resultsCounter > 0) {
                        if (metadataQuery.endsWith("|| ")) {
                            metadataQuery = metadataQuery.substring(0, metadataQuery.length()-3);
                        }
                        
                        metadataQuery += ") }";
                        
                        GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, metadataQuery);
                        return createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, false, true);

                    } else {
                        return null;
                    }
                    
                } catch (RDFHandlerException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (QueryEvaluationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (MalformedQueryException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } finally {
                    connection.close();
                }
                                
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }

        }                                                                                      
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public String searchMetadataWithParameters(String jsonParams, int limit, int offset, String orderBy, boolean desc, String acceptedFormat, List<String> notebookIDList) throws RepositoryException {
        
        // TODO: at the moment this method implement only the step one described in #74
        
        String finalResults = "";
        String qOrderBy     = getPropertyForOrdering(orderBy);
                                
        // Check ordering ASC (default), DESC
        String orderMode = null;
        if (desc) {
            orderMode = "ORDER BY DESC(?c) ";
        } else {
            orderMode = "ORDER BY ?c ";
        }
        
        try {            
            JSONObject jsonParameters = new JSONObject(jsonParams);
            
            // Process "resources" =========
            if (jsonParameters.has(SemlibConstants.JSON_RESOURCES)) {
                
                JSONArray jResources = jsonParameters.getJSONArray(SemlibConstants.JSON_RESOURCES);
                
                if (jResources.length() > 0) {
                    
                    RepositoryConnection connection = null;
                    
                    try {
                        
                        // This is necessary to restrict the query for a list of notebook ID ======
                        String restrictionToNotebooks = null;
                        if (notebookIDList != null) {                            
                            
                            int notebooksItem = notebookIDList.size();                                 
                            Iterator<String> nIterator = notebookIDList.iterator();                            
                            
                            if (notebooksItem > 1) {
                                restrictionToNotebooks = "{ ";
                            } else if (notebooksItem > 0) {
                                restrictionToNotebooks = "";
                            }
                            
                            while(nIterator.hasNext()) {
                                String cNotebook = Notebook.getURIFromID(nIterator.next());
                                restrictionToNotebooks += "{ <" + cNotebook + "> <" + OntologyHelper.URI_SEMLIB_INCLUDES + "> ?s } ";
                                if (nIterator.hasNext()) {
                                    restrictionToNotebooks += "UNION ";
                                }
                            }
                            
                            if (notebooksItem > 1) {
                                restrictionToNotebooks += "}";
                            }
                        }
                        // =======================================================================
                        
                        boolean firstResltSetAdded = false;
                        connection = sesameHTTPRepository.getConnection();                                                
                        
                        for (int i = 0; i < jResources.length(); i++) {
                            
                            String cUrl = jResources.get(i).toString();
                            
                            String query = null;
                            if (restrictionToNotebooks != null) {
                                query = "CONSTRUCT {?s ?p ?o . ?s <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName } WHERE { "
                                        + "{ ?s <" + OntologyHelper.URI_OAC_HASTARGET + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . " + restrictionToNotebooks + " . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "UNION "
                                        + "{ ?s <" + OntologyHelper.URI_SEMLIB_HASPAGECONTEXT + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . " + restrictionToNotebooks + " . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "UNION "
                                        + "{?s <" + OntologyHelper.URI_OAC_HASTARGET + "> ?x . ?x <" + OntologyHelper.URI_DC_ISPARTOF + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . " + restrictionToNotebooks + " . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "} ";
                            } else {
                                query = "CONSTRUCT {?s ?p ?o . ?s <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName } WHERE { "
                                        + "{ ?s <" + OntologyHelper.URI_OAC_HASTARGET + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "UNION "
                                        + "{ ?s <" + OntologyHelper.URI_SEMLIB_HASPAGECONTEXT + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "UNION "
                                        + "{?s <" + OntologyHelper.URI_OAC_HASTARGET + "> ?x . ?x <" + OntologyHelper.URI_DC_ISPARTOF + "> <" + cUrl + "> . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION + "> . ?s ?p ?o . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATOR +"> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } "
                                        + "} ";
                            }
                            
                            try {
                                GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);

                                // TODO: for the first step #74 it is ok. Modify this to support limit and offset (refactor method createStringFromRDFWithFormat)
                                try {
                                    String tempTriples = createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, false, false);
                                    if (StringUtils.isNotBlank(tempTriples)) {
                                        if (i == 0 || !firstResltSetAdded) {                                            
                                            firstResltSetAdded = true;
                                            
                                            // Necessary to avoid to create invalid rdf+xml and multiple closing tags
                                            if (acceptedFormat.contains(MediaType.APPLICATION_RDFXML)) {
                                                tempTriples = tempTriples.replaceAll("<\\/rdf:RDF>", "");
                                            }
                                            
                                            finalResults += tempTriples;
                                        } else {                                            
                                            if (acceptedFormat.contains(MediaType.APPLICATION_RDFXML)) {
                                                tempTriples = Pattern.compile("<\\?xml.*\\?>\n?+\r?+<rdf:RDF[^<>]+>", Pattern.DOTALL).matcher(tempTriples).replaceAll("");
                                                finalResults += "\n" + tempTriples;    
                                            } else {
                                                finalResults += ",\n" + tempTriples;    
                                            }                                                                                        
                                        }
                                    }
                                } catch (RDFHandlerException ex) {
                                    logger.log(Level.SEVERE, null, ex);
                                    continue;
                                }
                                // ================================================

                            } catch (QueryEvaluationException ex) {
                                logger.log(Level.SEVERE, null, ex);
                                connection.close();
                                return null;
                            } catch (org.openrdf.repository.RepositoryException ex) {
                                logger.log(Level.SEVERE, null, ex);
                                connection.close();
                                return null;
                            } catch (MalformedQueryException ex) {
                                logger.log(Level.SEVERE, null, ex);
                                connection.close();
                                return null;
                            }

                        }

                        connection.close();

                    } catch (org.openrdf.repository.RepositoryException ex) {
                        logger.log(Level.SEVERE, null, ex);
                        throw new RepositoryException();
                    }
                }                
            }
                    
        } catch (JSONException ex) {
            // JSON parameters not correct -> return null (BAD REQUEST)
            logger.log(Level.SEVERE, null, ex);
            return null;
        }        
        
        // Wrap all JSON result in a block "{ }" but only if finalResults cointains values
        if ( StringUtils.isNotBlank(finalResults) ) {
            if (acceptedFormat.contains(MediaType.APPLICATION_RDFXML)) {
                finalResults += "</rdf:RDF>"; 
            } else if (acceptedFormat.contains(MediaType.APPLICATION_JSON)) {
                finalResults = "{\n" + finalResults + "}";
            }
        }
        
        return finalResults;        
    }
 

    /**
     * {@inheritDoc}
     */
    public String getAllAnnotationItems(String annotationID, String acceptedFormat) throws RepositoryException {
        
        String query = "CONSTRUCT {?s ?p ?o} WHERE { <" + Annotation.getURIFromID(annotationID) + "> <" + OntologyHelper.URI_SEMLIB_ITEMS + "> ?g . GRAPH ?g { ?s ?p ?o } }";
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();

            try {
                GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                return createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, false, true);
            } catch (QueryEvaluationException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } catch (RDFHandlerException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } catch (MalformedQueryException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } finally {
                connection.close();
            }

        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String searchAnnotationItems(String annotationID, String queryParam, String acceptedFormat) throws RepositoryException {
                        
        try {                        
            JSONObject jsonParameters = new JSONObject(queryParam);
            JSONArray jsonResources = jsonParameters.getJSONArray(SemlibConstants.JSON_RESOURCES);
            if (jsonResources != null && jsonResources.length() <= 0) {
                // No searching parameter has been specified
                return null;
            } else {
                
                String dynQueryParam = "";
                for (int i = 0; i < jsonResources.length(); i++) {
                    String cResource = jsonResources.getString(i);
                    if (StringUtils.isNotBlank(cResource)) {
                        dynQueryParam += "<" + cResource + "> ?p" + i + " ?o" + i + " .";
                    }
                }
                
                if (StringUtils.isBlank(dynQueryParam)) {
                    return null;
                }
                
                String query = "CONSTRUCT { " + dynQueryParam + " } WHERE {  <" + Annotation.getURIFromID(annotationID) + "> <" + OntologyHelper.URI_SEMLIB_ITEMS + "> ?g . GRAPH ?g { " + dynQueryParam + "} }";
                
                try {
                    RepositoryConnection connection = sesameHTTPRepository.getConnection();
                    
                    try {
                        GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                        return createStringFromRDFWithFormat(triplesQuery, acceptedFormat, false, false, true);
                    } catch (QueryEvaluationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                        return null;
                    } catch (RDFHandlerException ex) {
                        logger.log(Level.SEVERE, null, ex);
                        return null;
                    } catch (MalformedQueryException ex) {
                        logger.log(Level.SEVERE, null, ex);                        
                        return null;
                    } finally {
                        connection.close();
                    }
                    
                } catch (org.openrdf.repository.RepositoryException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    throw new RepositoryException();
                }
            }
                    
        } catch (JSONException ex) {
            // JSON parameters not correct -> return null (BAD REQUEST)
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status updateNotebookMetadata(Notebook notebook) {
        
        boolean notebookExists = false;
        try {
            notebookExists = RepositoryManager.getInstance().getCurrentDataRepository().notebookExists(notebook.getID());
        } catch (RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Status.INTERNAL_SERVER_ERROR;
        }
        
        if (notebookExists) {
            
            GraphImpl graph = new GraphImpl();
            ValueFactory factory = graph.getValueFactory();
            
            try {                
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
                
                try {
                    Resource rscNotebookURI = factory.createURI(notebook.getURI());                    
                    Resource rscDefaultGraph = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH);
                    Resource rscModifiedDate = factory.createURI(OntologyHelper.URI_DC_MODIFIED);

                    connection.setAutoCommit(false);

                    connection.remove(rscNotebookURI, RDFS.LABEL, null, rscDefaultGraph);
                    if (connection.hasStatement(rscNotebookURI, (URI) rscModifiedDate, null, false, rscDefaultGraph)) {
                        connection.remove(rscNotebookURI, (URI) rscModifiedDate, null, rscDefaultGraph);
                    }
                    
                    connection.add(rscNotebookURI, (URI) RDFS.LABEL, factory.createLiteral(notebook.getName()), rscDefaultGraph);
                    connection.add(rscNotebookURI, (URI) rscModifiedDate, factory.createLiteral(notebook.getModifiedDate(), XMLSchema.DATETIME), rscDefaultGraph);

                    connection.commit();
                    
                    return Status.OK;
                    
                } catch (Exception e) {
                    logger.log(Level.SEVERE, null, e);
                    connection.rollback();
                    return Status.INTERNAL_SERVER_ERROR;
                } finally {
                    connection.close();
                }
                
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                return Status.INTERNAL_SERVER_ERROR;
            }
            
        } else {
            return Status.NOT_FOUND;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status writeNotebook(Notebook notebook) {
                
        GraphImpl newGraph = new GraphImpl();
        ValueFactory factory = newGraph.getValueFactory();

        Resource notebookURI  = factory.createURI(notebook.getURI());
        Resource defaultGraph = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH);
        
        newGraph.add(notebookURI, RDF.TYPE, factory.createURI(OntologyHelper.URI_SEMLIB_NOTEBOOK), defaultGraph);
        newGraph.add(notebookURI, factory.createURI(OntologyHelper.URI_SEMLIB_ID), factory.createLiteral(notebook.getID()), defaultGraph);
        newGraph.add(notebookURI, factory.createURI(OntologyHelper.URI_DC_CREATED), factory.createLiteral(notebook.getCreationDate(), XMLSchema.DATETIME), defaultGraph);
        newGraph.add(notebookURI, RDFS.LABEL, factory.createLiteral(notebook.getName()), defaultGraph);

        User notebookOwner = notebook.getOwner();
        if (notebookOwner != null) {
            newGraph.add(notebookURI, factory.createURI(OntologyHelper.URI_DC_CREATOR), factory.createURI(notebookOwner.getUserIDasURI()), defaultGraph);
        }
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            try {
                connection.add(newGraph);
                return Status.OK;                        
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage().toString(), e);
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                connection.close();
            }
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Status.INTERNAL_SERVER_ERROR;
        }
        
    }

    
    /**
     * {@inheritDoc}
     */
    public Status writeAnnotation(Annotation annotation) {
        
        GraphImpl newGraph   = new GraphImpl();
        ValueFactory factory = newGraph.getValueFactory();
                        
        Resource annotationURI = factory.createURI(annotation.getURI());
        Resource graphURI      = factory.createURI(annotation.getGraph());
        Resource notebookURI   = factory.createURI(annotation.getNotebookURI());
        Resource defaultGraph  = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH);
        
        // Write annotation creator infos
        String annotationAuthor = annotation.getAuthor();
        if (StringUtils.isNotBlank(annotationAuthor)) {                
            newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_DC_CREATOR), factory.createURI(annotationAuthor), defaultGraph);
        }        
        
        newGraph.add(annotationURI, RDF.TYPE, factory.createURI(OntologyHelper.URI_OAC_ANNOTATION), defaultGraph);
        newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_SEMLIB_ID), factory.createLiteral(annotation.getID()), defaultGraph);
        newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_DC_CREATED), factory.createLiteral(annotation.getCreationDate(), XMLSchema.DATETIME), defaultGraph);
        newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_SEMLIB_IS_INCLUDED_IN), notebookURI, defaultGraph);
        
        String additionalMetadata = annotation.getAdditionalContext();
        if (StringUtils.isNotBlank(additionalMetadata)) {
            try {
                JSONObject newJSONMetadata = new JSONObject(additionalMetadata);
                
                if (newJSONMetadata.has(SemlibConstants.JSON_TARGETS)) {
                    JSONArray targetsAsJSON = newJSONMetadata.getJSONArray(SemlibConstants.JSON_TARGETS);
                    if (targetsAsJSON != null) {
                        for (int i = 0; i < targetsAsJSON.length(); i++) {
                            String currentTarget = targetsAsJSON.getString(i);                            
                            if (currentTarget != null) {                                
                                newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_OAC_HASTARGET), factory.createURI(currentTarget), defaultGraph);
                            }
                        }
                    }
                }
                
                if (newJSONMetadata.has(SemlibConstants.JSON_PAGE_CONTEXT)) {
                    String pageContext = newJSONMetadata.getString(SemlibConstants.JSON_PAGE_CONTEXT);
                    if (StringUtils.isNotBlank(pageContext)) {
                        newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_SEMLIB_HASPAGECONTEXT), factory.createURI(pageContext), defaultGraph);
                    }
                }
                
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, "Additional Data Context invalid!");
                return Status.BAD_REQUEST;
            }
        }
        
        newGraph.add(annotationURI, factory.createURI(OntologyHelper.URI_OAC_HASBODY), graphURI, defaultGraph);                        
        newGraph.add(notebookURI, factory.createURI(OntologyHelper.URI_SEMLIB_INCLUDES), annotationURI, defaultGraph);
                        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            try {
                
                connection.setAutoCommit(false);
                              
                Status result = null;
                Annotation.AnnotationModelVersion annotationModelVersion = annotation.getAnnotationModelVersion();
                if (annotationModelVersion == Annotation.AnnotationModelVersion.ANNOTATION_MODEL_VERSION_1) {
                    result = writeAnnotationInGraph(annotation.getGraph(), annotation, Annotation.DataToWrite.ANNOTATION_TRIPLES, connection);
                } else if (annotationModelVersion == Annotation.AnnotationModelVersion.ANNOTATION_MODEL_VERSION_2) {
                    if (StringUtils.isNotBlank(annotation.getAnnotationItemsAsString())) {
                        URI rscItems          = factory.createURI(OntologyHelper.URI_SEMLIB_ITEMS);
                        Resource rscItemGraph = factory.createURI(annotation.getAnnotationItemGraph());                        
                        
                        newGraph.add(annotationURI, rscItems, rscItemGraph, defaultGraph);
                    }
                    
                    result = writeAnnotationInGraph(annotation.getGraph(), annotation, Annotation.DataToWrite.ANNOTATION_TRIPLES_ITEMS, connection);                                                    
                } else {
                    // Strange case...bad data
                    result = Status.BAD_REQUEST;
                }                
                
                if (result != Status.OK) {
                    connection.rollback();
                    return result;
                }
                
                connection.add(newGraph);
                
                // Commit data
                connection.commit();

                return Status.OK;
            
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                connection.close();
            }
                    
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Status.INTERNAL_SERVER_ERROR;
        }

    }    

        
    /**
     * {@inheritDoc}
     */
    public Status addNewTriplesToAnnotation(Annotation annotation, boolean clearExistingNamedGraph) {
                                                        
        try {
            
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
                        
            try {

                String queryForNotebook = "SELECT DISTINCT ?notebook WHERE { ?notebook "
                        + "<" + OntologyHelper.URI_SEMLIB_INCLUDES + "> <" + annotation.getURI() + "> }";

                HTTPTupleQuery tupleQuery;
                tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, queryForNotebook);
                TupleQueryResult data = tupleQuery.evaluate();

                while (data.hasNext()) {
                    BindingSet bindingSet = data.next();

                    String tNotebookURI = bindingSet.getValue("notebook").toString();
                    if (tNotebookURI != null && tNotebookURI.length() > 0) {
                        annotation.setNotebook(tNotebookURI);
                        break;
                    }
                }
               
                if (annotation.getNotebookURI() == null) {
                    // The specified annotation does not exists
                    return Status.NOT_FOUND;
                }                                
                
                // at this point everything should be ok so add triples to the specific annotation
                // and update it adding the metada modified                                                                                                                
                connection.setAutoCommit(false);
                
                ValueFactory factory = connection.getValueFactory();
                
                if (clearExistingNamedGraph) {                
                    Resource rscGraph = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH);                    
                    if ( connection.hasStatement(factory.createURI(annotation.getURI()), factory.createURI(OntologyHelper.URI_OAC_HASBODY), factory.createURI(annotation.getGraph()), false, rscGraph) ) {
                        connection.clear(factory.createURI(annotation.getGraph()));                          
                    }
                }
                
                Status result = writeAnnotationInGraph(annotation.getGraph(), annotation, Annotation.DataToWrite.ANNOTATION_TRIPLES, connection);
                if (result != Status.OK) {
                    connection.rollback();
                    return result;
                }
                                
                Resource rscAnnotationURI = factory.createURI(annotation.getURI());
                Resource rscModifiedURI = factory.createURI(OntologyHelper.URI_DC_MODIFIED);
                Resource rscDefgraphURI = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH);

                if (connection.hasStatement(rscAnnotationURI, (URI) rscModifiedURI, null, false, rscDefgraphURI)) {
                    connection.remove(rscAnnotationURI, (URI) rscModifiedURI, null, rscDefgraphURI);
                }

                connection.add(rscAnnotationURI, (URI) rscModifiedURI, factory.createLiteral(annotation.getModifiedDate(), XMLSchema.DATETIME), rscDefgraphURI);
                
                connection.commit();
                
                return Status.OK;
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                    connection.rollback();
                }                
                return Status.INTERNAL_SERVER_ERROR;
            } catch (RDFParseException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                    connection.rollback();                        
                }                
                return Status.BAD_REQUEST;
            } catch (QueryEvaluationException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                   connection.rollback(); 
                }                
                return Status.INTERNAL_SERVER_ERROR;
            } catch (MalformedQueryException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                   connection.rollback(); 
                }                
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                if (connection != null) {
                    connection.close();                        
                }                
            }                                        
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Status.INTERNAL_SERVER_ERROR;
        }
        
    }
    
    
    /**
     * {@inheritDoc}
     */        
    public Status addItemsToAnnotation (Annotation annotation, boolean clearExistingNamedGraph) {
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            try {
                boolean annotationExists = this.annotationExists(annotation.getID());
                if (!annotationExists) {
                    return Status.NOT_FOUND;
                }
                
                ValueFactory factory = connection.getValueFactory();
                
                URI rscItems = factory.createURI(OntologyHelper.URI_SEMLIB_ITEMS);
                Resource rscDefGraph  = factory.createURI(OntologyHelper.URI_SWN_DEFGRAPH); 
                Resource rscItemGraph = factory.createURI(annotation.getAnnotationItemGraph());
                
                if (clearExistingNamedGraph) {                                                       
                    if ( connection.hasStatement(factory.createURI(annotation.getURI()), rscItems, rscItemGraph, false, rscDefGraph) ) {
                        connection.clear(rscItemGraph);                          
                    }
                }
                                
                // at this point everything should be ok so add triples to the specific annotation
                // and update it adding the metada modified                                                                                                                
                connection.setAutoCommit(false);
                
                Status result = writeAnnotationInGraph(annotation.getAnnotationItemGraph(), annotation, Annotation.DataToWrite.ANNOTATION_ITEMS, connection);
                if (result != Status.OK) {
                    connection.rollback();
                    return result;
                }
                
                Resource rscAnnotationURI = factory.createURI(annotation.getURI());
                Resource rscModifiedURI   = factory.createURI(OntologyHelper.URI_DC_MODIFIED);
                
                if (!connection.hasStatement(rscAnnotationURI, rscItems, rscItemGraph, false, rscDefGraph)) {
                    connection.add(rscAnnotationURI, rscItems, rscItemGraph, rscDefGraph);
                }
                
                if (connection.hasStatement(rscAnnotationURI, (URI) rscModifiedURI, null, false, rscDefGraph)) {
                    connection.remove(rscAnnotationURI, (URI) rscModifiedURI, null, rscDefGraph);
                }

                connection.add(rscAnnotationURI, (URI) rscModifiedURI, factory.createLiteral(annotation.getModifiedDate(), XMLSchema.DATETIME), rscDefGraph);
                
                connection.commit();
                
                return Status.OK;
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                    connection.rollback();
                }                
                return Status.INTERNAL_SERVER_ERROR;
            } catch (RDFParseException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                    connection.rollback();
                }                
                return Status.BAD_REQUEST;
            } catch (RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                if (connection != null) {
                    connection.rollback();
                }                
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Status.INTERNAL_SERVER_ERROR;
        }        
    }
    
    
    /**
     * {@inheritDoc}
     */        
    public List<String> getAnnotationsIDsInNotebook(String notebookId, int limit, int offset, boolean desc) throws RepositoryException {
        
        String orderingMode;
        if (desc) {
            orderingMode = "ORDER BY DESC(?c) ";
        } else {
            orderingMode = "ORDER BY ?c ";
        }
        
        String notebookURI = Notebook.getURIFromID(notebookId);
        
        String query = "SELECT ?s WHERE {" +
                       "<" + notebookURI + "> <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_SEMLIB_NOTEBOOK +"> . " +
                       "<" + notebookURI + "> <" + OntologyHelper.URI_SEMLIB_INCLUDES + "> ?s . " +
                       "?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION +"> . " +
                       "?s <" + OntologyHelper.URI_DC_CREATED + "> ?c } " + orderingMode;
        
        if (limit != -1) {
            query += "LIMIT " + limit + " ";
        }
        
        if (offset != -1) {
            query += "OFFSET " + offset + " ";
        }
        
        List<String> annotationIDs = new ArrayList<String>();
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
                         
            try {
                HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                TupleQueryResult tuples = tupleQuery.evaluate();
                
                while(tuples.hasNext()) {
                    BindingSet bindingSet = tuples.next();
                    Value annotationURI = bindingSet.getValue("s");
                    
                    if (annotationURI == null) {
                        continue;
                    }
                    
                    String strAnnotationURI = annotationURI.stringValue();
                    int indexSlash = strAnnotationURI.lastIndexOf("/");
                    if (indexSlash != -1) {                    
                        String annID = strAnnotationURI.substring(indexSlash+1);
                    
                        if (StringUtils.isNotBlank(annID)) {
                            annotationIDs.add(annID);
                        }
                    }
                }
                
                return annotationIDs;
                
            } catch (QueryEvaluationException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException(ex.toString());
            } catch (MalformedQueryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException(ex.toString());
            } finally {
                connection.close();
            }
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException(ex.toString());
        }                
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public String getAnnotationMetadata(String annotationID, String acceptedFormat) throws RepositoryException {
        
        if (annotationID != null && acceptedFormat != null) {
            String strAnnotationURI = Annotation.getURIFromID(annotationID);
            
            String queryForMetadata = "CONSTRUCT { <" + strAnnotationURI + "> ?p ?o . <" + strAnnotationURI + "> <" + OntologyHelper.URI_DCELEMENT_CREATOR + "> ?authorName } "
                                    + "WHERE { { <" + strAnnotationURI + "> ?p ?o . OPTIONAL {<" + strAnnotationURI + "> <" + OntologyHelper.URI_DC_CREATOR + "> ?author . ?author <" + OntologyHelper.URI_FOAF_NAME + "> ?authorName } } . }";
            
            return getAnnotationData(acceptedFormat, queryForMetadata);
        }
        
        return null;        

    }
    
    
    /**
     * {@inheritDoc}
     */        
    public String getAnnotationTriples(String annotationID, String acceptedFormat) throws RepositoryException {
        
        if (annotationID != null && acceptedFormat != null) {                        

            String queryForTriples = "CONSTRUCT {?s ?p ?o} FROM <" + Annotation.getGraphURIFromID(annotationID) +"> WHERE {?s ?p ?o}";

            return getAnnotationData(acceptedFormat, queryForTriples);
        }
        
        return null;
    }
    
    
    /**
     * Get data about annotation given a SPARQL query
     * 
     * @param acceptedFormat        the output format
     * @param query                 the SPARQL query used to extract the annotation data
     * @return                      a string cointaining the annotation data formatted with the specified format
     * 
     * @throws RepositoryException 
     */
    private String getAnnotationData(String acceptedFormat, String query) throws RepositoryException {
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            try {
                
                GraphQuery graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);                
                return createStringFromRDFWithFormat(graphQuery, acceptedFormat, false, false, true);
                
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            } finally {
                connection.close();
            }
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException("Unable to connect to the triplestore!");
        }        
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getAllTriplesAnnotations(String notebookID, String acceptedFormats) {
        
        String strNotebookURI = Notebook.getURIFromID(notebookID);                
        
        // We need two quries:
        // 1) to extract all namegraph in the specified notebook to which the current user has the read or read/write access
        // 2) to extract all triples from the previous extracted named graph
        String querygraph   = "SELECT DISTINCT ?graph WHERE { <" + strNotebookURI + "> <" + OntologyHelper.URI_SEMLIB_INCLUDES +"> ?annotation . ?annotation <" + OntologyHelper.URI_OAC_HASBODY +"> ?graph }";
        String queryTriples = "CONSTRUCT { ?s ?p ?o } %%FROM%% WHERE { ?s ?p ?o }";

        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, querygraph);
            TupleQueryResult graphs = tupleQuery.evaluate();
            
            String namedGraphsFound = "";
            while (graphs.hasNext()) {
                BindingSet bindingSet = graphs.next();
                Value graphURI = bindingSet.getValue("graph");
                
                if (graphURI != null) {
                    namedGraphsFound += "FROM <" + graphURI.toString() + "> \r\n";
                }
            }
            
            String finalResults = null;
            if (!namedGraphsFound.equals("")) {
                                
                queryTriples = queryTriples.replaceAll("%%FROM%%", namedGraphsFound);
                
                // Perform the query to get all triples
                GraphQuery triplesQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, queryTriples);
                finalResults = createStringFromRDFWithFormat(triplesQuery, acceptedFormats, false, false, true);
                                
            }
            
            // Close the connection and return the results
            connection.close();            
            return finalResults;
            
        } catch (RDFHandlerException ex) {
            logger.log(Level.SEVERE, "GetAllTriplesAnnotation: RDFHandler Exception", ex);
            return null;
        } catch (QueryEvaluationException ex) {
            logger.log(Level.SEVERE, "GetAllTriplesAnnotation: Query Evaluation Eception", ex);
            return  null;
        } catch (MalformedQueryException ex) {
            logger.log(Level.SEVERE, "GetAllTriplesAnnotation: Malformed query", ex);
            return null;
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, "GetAllTriplesAnnotation: Repository Connection Exception", ex);
            return null;
        }
        
    }

    
    /**
     * Get the Sesame data format from a simple Content-Type String
     * 
     * @param contentType   the Content-Type String
     * @return              a Sesame's RDFFormat object
     */
    public RDFFormat getSesameDataFormatFromContetType(String contentType) {
        
        // Note: when checking the String for ContentType it is necessary
        // to use "contains" and not methods based on equals 'cos sometime
        // the clients add other data in ContentType (e.g. charset=).
        // In this case contains is the only safe method
        if (contentType.contains(MediaType.APPLICATION_RDFXML)) {
            return RDFFormat.RDFXML;
        } else if (contentType.contains(MediaType.TEXT_TURTLE)) {
            return RDFFormat.TURTLE;
        } else if (contentType.contains(MediaType.TEXT_RDFN3)) {
            return RDFFormat.N3;
        } else {
            return null;
        }
        
    }

    
    /**
     * {@inheritDoc}
     */
    public Status deleteAnnotation(String annotationID) {
                
        Value notebookURI      = null;        
        Value annGraphURI      = null;
        Value annItemsGraphURI = null;
        
        String strAnnotationURI = OntologyHelper.SWN_NAMESPACE + annotationID;
        String queryForNotebookBodyAndGraph = "SELECT DISTINCT ?n ?g ?ig WHERE { ?n <" + OntologyHelper.URI_SEMLIB_INCLUDES + "> <" + strAnnotationURI + "> . <" + strAnnotationURI + "> <" + OntologyHelper.URI_OAC_HASBODY + "> ?g . OPTIONAL { <" + strAnnotationURI + "> <" + OntologyHelper.URI_SEMLIB_ITEMS + "> ?ig } }";
                
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();

            try {
                ValueFactory factory = connection.getValueFactory();
                Resource rscAnnotationURI = factory.createURI(strAnnotationURI);

                HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, queryForNotebookBodyAndGraph);
                TupleQueryResult graphs = tupleQuery.evaluate();

                while (graphs.hasNext()) {
                    BindingSet bindingSet = graphs.next();
                    notebookURI      = bindingSet.getValue("n");        
                    annGraphURI      = bindingSet.getValue("g");                    
                    annItemsGraphURI = bindingSet.getValue("ig");
                    
                    if (notebookURI != null && annGraphURI != null) {
                        break;
                    }
                }

                if (notebookURI == null || annGraphURI == null) {
                    return Status.BAD_REQUEST;
                }

                // Init transaction
                connection.setAutoCommit(false);

                // Remove the annotation from the notebook
                connection.remove( (Resource)notebookURI, factory.createURI(OntologyHelper.URI_SEMLIB_INCLUDES), rscAnnotationURI);
                
                // Remove all annotation metadata
                connection.remove(rscAnnotationURI, null, null);

                // Remove all annotation triples
                connection.clear( (Resource)annGraphURI );

                // Remove all annotation items
                if (annItemsGraphURI != null) {
                    connection.clear( (Resource)annItemsGraphURI );
                }
                
                // Commit transaction
                connection.commit();

                return Status.OK;

            } catch (QueryEvaluationException ex) {
                logger.log(Level.SEVERE, null, ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } catch (MalformedQueryException ex) {
                logger.log(Level.SEVERE, null, ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                connection.close();
            }

        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Status.INTERNAL_SERVER_ERROR;
        }
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status deleteNotebook(String notebookID) {
                
        String strNotebookURI = Notebook.getURIFromID(notebookID);                          
        String queryForAllAnnotationBodyAndGraph = "SELECT DISTINCT ?ann ?graph ?ig WHERE { <" + strNotebookURI + ">  <" + OntologyHelper.URI_SEMLIB_INCLUDES + "> ?ann . ?ann <" + OntologyHelper.URI_OAC_HASBODY +"> ?graph . OPTIONAL { ?ann <" + OntologyHelper.URI_SEMLIB_ITEMS + "> ?ig } }";
        
        // The notebook exists so...start deleting it
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            
            try {
                ValueFactory factory = connection.getValueFactory();
                Resource rscNotebookURI = factory.createURI(strNotebookURI);

                // Start transaction
                connection.setAutoCommit(false);

                HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, queryForAllAnnotationBodyAndGraph);
                TupleQueryResult graphs = tupleQuery.evaluate();
                
                while( graphs.hasNext() ) {
                    BindingSet bindingSet = graphs.next();
                    
                    Value annotationID = bindingSet.getValue("ann");
                    Value graphID      = bindingSet.getValue("graph");
                    
                    Value itemsID = null;
                    if (bindingSet.hasBinding("ig")) {
                        itemsID = bindingSet.getValue("ig");
                    }                                        
                    
                    if (annotationID == null || graphID == null) {
                        continue;
                    }
                    
                    // Remove all annotaton metadata
                    connection.remove( (Resource)annotationID, null, null);
                    
                    // Remove all annotation triples
                    connection.clear( (Resource)graphID );
                    
                    if (itemsID != null) {
                        connection.clear(((Resource)itemsID));
                    }                    
                }
                
                // Delete the notebook
                connection.remove(rscNotebookURI, null, null);
                
                // Commit all deletion operation
                connection.commit();
                
                return Status.OK;
                
            } catch (QueryEvaluationException ex) {                
                logger.log(Level.SEVERE, null, ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } catch (MalformedQueryException ex) {                
                logger.log(Level.SEVERE, null, ex);
                connection.rollback();
                return Status.INTERNAL_SERVER_ERROR;
            } finally {
                connection.close();
            }
            
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Status.INTERNAL_SERVER_ERROR;            
        }
        
    }
    
    
    /**
     * Create a string cointainig RDF triples formatted using a specific format 
     * and a specific prepared SPARQL query
     * 
     * @param triplesQuery                  the prepared GraphQuery
     * @param format                        the output format
     * @return                              a string with RDF data formatted with the specified format 
     *                                      or <code>null</code> in case of error/problems
     * @throws QueryEvaluationException
     * @throws RDFHandlerException
     * @throws org.openrdf.repository.RepositoryException
     */
    private String createStringFromRDFWithFormat(GraphQuery triplesQuery, String format, boolean includesDuplicatedStatements, boolean orderedTriples, boolean wrapInBlockIfJSON) throws QueryEvaluationException, RDFHandlerException, org.openrdf.repository.RepositoryException {

        GraphQueryResult results = triplesQuery.evaluate();                
        
        if (format.contains(MediaType.APPLICATION_JSON)) {
            
            String finalResult = null;
            String psubject    = null;
                        
            GraphImpl newGraph = new GraphImpl();
            
            if (!orderedTriples) {
                while (results.hasNext()) {
                    Statement currentStatement = (Statement) results.next();
                    if (!includesDuplicatedStatements) {
                        if (!newGraph.contains(currentStatement)) {
                            newGraph.add(currentStatement);
                        }

                    } else {
                        newGraph.add(currentStatement);
                    }
                }
                
                if (!newGraph.isEmpty()) {                    
                    finalResult = SesameRDFJSONConverter.getInstance().RDFGraphToJson(newGraph);
                    if (!wrapInBlockIfJSON) {
                        finalResult = finalResult.substring(1, finalResult.length()-1);
                    }
                } else {
                    finalResult = "";
                }
                
            } else {
                boolean firstResultAdded = false;
                
                while (results.hasNext()) {
                    Statement currentStatement = (Statement) results.next();
                    String currentSubject = currentStatement.getSubject().toString();
                    
                    if (psubject == null) {
                        psubject = currentSubject;
                    }
                    
                    if (psubject.equalsIgnoreCase(currentSubject)) {
                        if (!includesDuplicatedStatements) {
                            if (!newGraph.contains(currentStatement)) {
                                newGraph.add(currentStatement);
                            }
                        } else {
                            newGraph.add(currentStatement);
                        }
                        
                        if (!results.hasNext()) {
                            if (!newGraph.isEmpty()) {
                                
                                if (finalResult == null) {
                                    if (wrapInBlockIfJSON) {
                                        finalResult = "{\n";                                        
                                    } else {
                                        finalResult = "";
                                    }                                    
                                }
                                
                                String partialResult = SesameRDFJSONConverter.getInstance().RDFGraphToJson(newGraph);
                                partialResult = partialResult.substring(1, partialResult.length()-1);
                                
                                finalResult += "  " + partialResult;
                                
                                if (wrapInBlockIfJSON) {
                                    finalResult += "\n}";
                                }
                            }
                        }
                        
                    } else {
                        String partialResult = SesameRDFJSONConverter.getInstance().RDFGraphToJson(newGraph);
                        partialResult = partialResult.substring(1, partialResult.length()-1);

                        if (finalResult == null) {
                            if (wrapInBlockIfJSON) {
                                finalResult = "{\n";
                            } else {
                                finalResult = "";
                            }                            
                        }
                        
                        if (partialResult != null && partialResult.length() > 1) {
                            finalResult += "  " + partialResult;
                            firstResultAdded = true;
                        }
                        
                        if (results.hasNext() && firstResultAdded){                            
                            finalResult += ",\n";
                        }                      

                        newGraph.clear();
                        psubject = currentSubject;
                        newGraph.add(currentStatement);

                    }
                }
            }
            
            // This is necessary to free any resources it keeps hold of
            results.close();
            
            return finalResult;
            
        } else {
            
            // In order to output a correct RDF/XML or RDF/N3 format efficently 
            // we use a temporary in Memory reporitory to manupulate the statements
            // returned by the query avoiding to include duplicate statements
            SailRepository tempRepository = new SailRepository(new MemoryStore());
            tempRepository.initialize();
            
            RepositoryConnection tempRepositoryConnection = tempRepository.getConnection();
                                    
            // Add the query result to the tempRepository            
            int counter = 0;
            for (; results.hasNext(); counter++) {            
                Statement currentStatement = results.next();
                if ( !includesDuplicatedStatements ) {
                    if (!tempRepositoryConnection.hasStatement(currentStatement, true)) {
                        tempRepositoryConnection.add(currentStatement);
                    }                    
                } else {
                    tempRepositoryConnection.add(currentStatement);
                }
            }
                        
            if (counter > 0) {
                // ...now export triples basing and the requested format
                RDFWriter rdfWriter = null;
                StringWriter stringWriter = new StringWriter();

                if (format.contains(MediaType.APPLICATION_RDFXML)) {
                    rdfWriter = new RDFXMLPrettyWriter(stringWriter);
                } else if (format.contains(MediaType.TEXT_RDFN3)) {
                    rdfWriter = new N3Writer(stringWriter);
                }

                tempRepositoryConnection.export(rdfWriter);

                // Close the connection and free all temporary resources
                tempRepositoryConnection.clear();
                tempRepositoryConnection.close();
                
                tempRepository.shutDown();
                        
                results.close();
                
                return stringWriter.toString();
            } else {                
                results.close();
                return null;
            }
                        
        }                

    }

    
    /**
     * Utility method used to write annotation triples into a specific context
     * 
     * @param annotation                    the annotation to write
     * @param currentRepositoryConnection   the current RepositoryConnection already setted and initialized
     * @return                              a Status (@see Status)
     * 
     * @throws IOException
     * @throws RDFParseException
     * @throws org.openrdf.repository.RepositoryException 
     */
    private Status writeAnnotationInGraph(String graphURI, Annotation annotation, Annotation.DataToWrite dataType, RepositoryConnection currentRepositoryConnection) throws IOException, RDFParseException, org.openrdf.repository.RepositoryException {
        
        String annotationContent = null;
        String annotationItems   = null;
        
        ValueFactory factory = currentRepositoryConnection.getValueFactory();                                
        Resource rscGraphURI = factory.createURI(graphURI);
        
        String graphToWrite = null;
        String itemsToWrite = null;
        if (dataType == Annotation.DataToWrite.ANNOTATION_TRIPLES_ITEMS) {
            graphToWrite = annotation.getAnnotationDataAsString();
            itemsToWrite = annotation.getAnnotationItemsAsString();
        } else if (dataType == Annotation.DataToWrite.ANNOTATION_TRIPLES) {
            graphToWrite = annotation.getAnnotationDataAsString();
        } else if (dataType == Annotation.DataToWrite.ANNOTATION_ITEMS) {
            graphToWrite = annotation.getAnnotationItemsAsString();
        } else {
            // Strange case...bad data;
            return Status.BAD_REQUEST;
        }
        
        if (annotation.getContentType().contains(MediaType.APPLICATION_JSON)) {

            annotationContent = JSONRDFConverter.getInstance().ConvertJSONToTurtle(graphToWrite);
            if (StringUtils.isBlank(annotationContent)) {            
                return Status.BAD_REQUEST;
            }

            currentRepositoryConnection.add(new StringReader(annotationContent), OntologyHelper.SWN_NAMESPACE, RDFFormat.TURTLE, rscGraphURI);

            if (StringUtils.isNotBlank(itemsToWrite)) {
                Resource rscItemsURI = factory.createURI(annotation.getAnnotationItemGraph());
                annotationItems = JSONRDFConverter.getInstance().ConvertJSONToTurtle(itemsToWrite);
                currentRepositoryConnection.add(new StringReader(annotationItems), OntologyHelper.SWN_NAMESPACE, RDFFormat.TURTLE, rscItemsURI);
            }
            
            return Status.OK;
            
        } else {

            RDFFormat dataFormat = getSesameDataFormatFromContetType(annotation.getContentType());
            if (dataFormat != null) {
                annotationContent = annotation.getAnnotationDataAsString();
                currentRepositoryConnection.add(new StringReader(annotationContent), OntologyHelper.SWN_NAMESPACE, dataFormat, rscGraphURI);
                return Status.OK;
            } else {                
                return Status.INTERNAL_SERVER_ERROR;
            }
        }

    }
    
    
    /**
     * Check ang get the correct property for ordering
     * 
     * @param orderBy   the property to use for ordering triples
     * @return          the correct property for orderBy
     */
    private String getPropertyForOrdering(String orderBy) {
        
        // Default value for order by "created"
        String qOrderBy = OntologyHelper.URI_DC_CREATED;
                        
        if (StringUtils.isNotBlank(orderBy)) {
            // Check if the orderBy string is a valid property            
            GraphImpl graph = new GraphImpl();
            ValueFactory factory = graph.getValueFactory();

            try {
                Resource propertyForOrdering = factory.createURI(orderBy);

                // if no exception is thrown we have a valid URI/property
                qOrderBy = orderBy;

            } catch (Exception e) {
                // The specified orderby property is not valid or is not a valid URI, so log this and use the default value
                logger.log(Level.SEVERE, null, "The specified properties (" + orderBy + ") for ORDER BY is not a valid URI.");
            }
        }

        return qOrderBy;
    }
    
    
    // User management implementation methods ======
    
    /**
     * {@inheritDoc}
     */
    public boolean initOrUpdateUserInfos(User user) {
    
        if (user == null) {
            return false;
        }
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
                        
            ValueFactory valueFactory = connection.getValueFactory();
            
            Resource rscUserID       = null;
            Resource usersNamedGraph = valueFactory.createURI(OntologyHelper.URI_SWN_USERSGRAPH);
            
            try {
                String userID = user.getUserID();
                if (userID != null) {
                    
                    rscUserID = valueFactory.createURI(user.getUserIDasURI());
                    Statement userIDStatement = valueFactory.createStatement(rscUserID, 
                                                                             valueFactory.createURI(OntologyHelper.URI_RDF_TYPE), 
                                                                             valueFactory.createURI(OntologyHelper.URI_FOAF_PERSON));

                    if (!connection.hasStatement(userIDStatement, false, usersNamedGraph)) {
                        connection.add(userIDStatement, usersNamedGraph);
                    }
                    
                    String strOpenID = user.getOpenIDIdentifierAsString();
                    if (strOpenID != null) {
                        URI rscOpenID = valueFactory.createURI(OntologyHelper.URI_FOAF_OPENID);
                        URI rscObjOpenIDValue = valueFactory.createURI(strOpenID);
                        
                        Statement openIDStatement = valueFactory.createStatement(rscUserID, rscOpenID, rscObjOpenIDValue);
                        this.checkAndWriteUserStatement(connection, openIDStatement, usersNamedGraph);
                    }
                }
                
                String firstName = user.getFirstName();
                if (firstName != null) {                    
                    URI rscFamilyName = valueFactory.createURI(OntologyHelper.URI_FOAF_GIVENNAME);
                    Literal ltrFamilyName = valueFactory.createLiteral(firstName);
                    
                    Statement stmForWriting = valueFactory.createStatement(rscUserID, rscFamilyName, ltrFamilyName);
                    this.checkAndWriteUserStatement(connection, stmForWriting, usersNamedGraph);                    
                }
                
                String lastName = user.getLastName();
                if (lastName != null) {
                    
                    URI rscGivenName = valueFactory.createURI(OntologyHelper.URI_FOAF_FAMILYNAME);
                    Literal ltrGivenName = valueFactory.createLiteral(lastName);
                    
                    Statement stmForWriting = valueFactory.createStatement(rscUserID, rscGivenName, ltrGivenName);
                    this.checkAndWriteUserStatement(connection, stmForWriting, usersNamedGraph);
                }
                
                String fullName = user.getFullName();
                if (fullName != null) {
                    URI rscFullName = valueFactory.createURI(OntologyHelper.URI_FOAF_NAME);
                    Literal ltrFullName = valueFactory.createLiteral(fullName);
                    
                    Statement stmForWriting = valueFactory.createStatement(rscUserID, rscFullName, ltrFullName);
                    this.checkAndWriteUserStatement(connection, stmForWriting, usersNamedGraph);
                }

                String mailBox = user.getEmail();
                if (mailBox != null) {
                    if (!mailBox.startsWith("mailto:")) {
                        mailBox = "mailto:" + mailBox;
                    }
                    
                    URI rscMailBox = valueFactory.createURI(OntologyHelper.URI_FOAF_MBOX);
                    URI rscMailValue = valueFactory.createURI(mailBox);
                    
                    Statement stmForWriting = valueFactory.createStatement(rscUserID, rscMailBox, rscMailValue);                    
                    this.checkAndWriteUserStatement(connection, stmForWriting, usersNamedGraph);
                    
                    String sha1Mbox = UtilsManager.getInstance().SHA1(mailBox);
                    
                    URI rscSHA1Mbox = valueFactory.createURI(OntologyHelper.URI_FOAF_SHA1SUM);
                    Literal ltrSHA1Mbox = valueFactory.createLiteral(sha1Mbox);
                    
                    Statement stmForWritingMboxSha1 = valueFactory.createStatement(rscUserID, rscSHA1Mbox, ltrSHA1Mbox);
                    this.checkAndWriteUserStatement(connection, stmForWritingMboxSha1, usersNamedGraph);
                }
                
                return true;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, null, e);
                return false;
            } finally {
                connection.close();
            }           
        } catch (org.openrdf.repository.RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getUserData(String userID, String acceptedFormats) {
        
        String userIDURI = User.getURIFromID(userID);
        if (UtilsManager.getInstance().isValidURI(userIDURI)) {
            
            String query = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o . ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_FOAF_PERSON + "> . filter (?s = <" + userIDURI +">)}";            
            
            try {
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
                
                try {
                    GraphQuery graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                    graphQuery.setIncludeInferred(false);
                    String finalResults   = createStringFromRDFWithFormat(graphQuery, acceptedFormats, false, false, true);                    
                    connection.close();
                    
                    return finalResults;
                } catch (QueryEvaluationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (RDFHandlerException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } catch (MalformedQueryException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return null;
                } finally {
                    connection.close();
                }
                
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            }
            
        } else {
            return null;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public String getSPARQLEndPointUrl() {
        ConfigManager configManager = ConfigManager.getInstance();
        return configManager.getRepositoryUrl() + "repositories/" + configManager.getRepositoryID(); 
    }
    
    
    private void checkAndWriteUserStatement(RepositoryConnection connection, Statement stmForWriting, Resource usersNamedGraph) throws org.openrdf.repository.RepositoryException {
        
        // Check if there is already a first name defined for this user                    
        RepositoryResult<Statement> results = connection.getStatements(stmForWriting.getSubject(), stmForWriting.getPredicate(), null, true, usersNamedGraph);
        List<Statement> cResults = results.asList();
                    
        boolean writeStatement = false;
        if (cResults.size() > 1) {
            // we have more value for the same person...delete all and update
            writeStatement = true;
            connection.remove(stmForWriting.getSubject(), stmForWriting.getPredicate(), null, usersNamedGraph);
        } else if (cResults.size() == 1) {
            // check if the value is the same
            Statement stmValue = cResults.get(0);
            String storedValue = stmValue.getObject().stringValue();                       
            if (!stmForWriting.getObject().stringValue().equals(storedValue)) {
                writeStatement = true;
                connection.remove(stmForWriting.getSubject(), stmForWriting.getPredicate(), null, usersNamedGraph);                            
            }
        } else {
            writeStatement = true;                        
        }
                    
        if (writeStatement) {
            connection.add(stmForWriting, usersNamedGraph);
        }
    }
    
    
    // Administrations Methods =============
    
    public List<String> getIDsOfNotebooksWithName(String name, boolean desc, int limit, int offset) throws RepositoryException {
        return this.getIDsOfNotebooksSearchingWithData(false, name, desc, limit, offset);
    }
    
    
    public List<String> getIDsOfNotebooksWithOwnerName(String name, boolean desc, int limit, int offset) throws RepositoryException {
        return this.getIDsOfNotebooksSearchingWithData(true, name, desc, limit, offset);
    }
    
    public List<String> getIDsOfAnnotationsWithID(String id, boolean desc, int limit, int offset) throws RepositoryException {
        return this.getIDsOfAnnotationsSearchingWithData(false, id, desc, limit, offset);
    }
    
    public List<String> getIDsofAnnotationsWithOwnerName(String name, boolean desc, int limit, int offset) throws RepositoryException {
        return this.getIDsOfAnnotationsSearchingWithData(true, name, desc, limit, offset);
    }
       
    private List<String> getIDsOfNotebooksSearchingWithData(boolean byowner, String name, boolean desc, int limit, int offset) throws RepositoryException {
        List<String> ids = new ArrayList<String>();
        
        String orderMode;
        if (desc) {
            orderMode = "ORDER BY DESC(?date) ";
        } else {
            orderMode = "ORDER BY ?date ";
        }
        
        RepositoryConnection connection = null;
        try {
            connection = sesameHTTPRepository.getConnection();
            
            String query;
            if (byowner) {
                query = "SELECT ?s where { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_SEMLIB_NOTEBOOK + "> . ?s <" + OntologyHelper.URI_DC_CREATOR + "> ?n . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATED + "> ?date . ?n <" + OntologyHelper.URI_FOAF_NAME + "> ?name} filter regex(?name, \"" + name + "\", \"i\") } " + orderMode;
            } else {
                query = "SELECT ?s WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_SEMLIB_NOTEBOOK + "> . ?s <" + OntologyHelper.URI_RDFS_LABEL + "> ?n . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATED + "> ?date} filter regex(?n, \"" + name +"\", \"i\") } " + orderMode;
            }
            
            if (limit != -1 && offset != -1) {
                query += "LIMIT " + limit + " OFFSET " + offset;
            }
            
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            tupleQuery.setIncludeInferred(false);
            TupleQueryResult tuples = tupleQuery.evaluate();
                        
            while (tuples.hasNext()) {
                BindingSet bindingSet = tuples.next();
                Value notebookURI = bindingSet.getValue("s");
                String notebookID = notebookURI.toString();
                if (StringUtils.isNotBlank(notebookID)) {
                    String id = notebookID.substring(notebookID.lastIndexOf("/")+1);
                    ids.add(id);                            
                }
            }    
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        } finally {
            try {
                connection.close();
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
        }
        
        return ids;    
    }
    
    
    private List<String> getIDsOfAnnotationsSearchingWithData(boolean byowner, String name, boolean desc, int limit, int offset) throws RepositoryException {
        List<String> ids = new ArrayList<String>();
        
        String orderMode;
        if (desc) {
            orderMode = "ORDER BY DESC(?date) ";
        } else {
            orderMode = "ORDER BY ?date ";
        }
        
        RepositoryConnection connection = null;
        try {
            connection = sesameHTTPRepository.getConnection();
            
            String query;
            if (byowner) {
                query = "SELECT ?s WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION +"> . ?s <" + OntologyHelper.URI_DC_CREATOR + "> ?n . OPTIONAL {?s <" + OntologyHelper.URI_DC_CREATED + "> ?date . ?n <" + OntologyHelper.URI_FOAF_NAME + "> ?name} FILTER regex(?name, \"" + name +"\", \"i\") } " + orderMode;
            } else {
                query = "SELECT ?s WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_OAC_ANNOTATION +"> . ?s <" + OntologyHelper.URI_DC_CREATOR + "> <" + User.getURIFromID(name) + "> . OPTIONAL { ?s <" + OntologyHelper.URI_DC_CREATED + "> ?date } } " + orderMode;
            }
            
            if (limit != -1 && offset != -1) {
                query += "LIMIT " + limit + " OFFSET " + offset;
            }
            
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            tupleQuery.setIncludeInferred(false);
            TupleQueryResult tuples = tupleQuery.evaluate();
                        
            while (tuples.hasNext()) {
                BindingSet bindingSet = tuples.next();
                Value notebookURI = bindingSet.getValue("s");
                String notebookID = notebookURI.toString();
                if (StringUtils.isNotBlank(notebookID)) {
                    String id = notebookID.substring(notebookID.lastIndexOf("/")+1);
                    ids.add(id);                            
                }
            }    
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException();
        } finally {
            try {
                connection.close();
            } catch (org.openrdf.repository.RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RepositoryException();
            }
        }
        
        return ids;            
    }
}
