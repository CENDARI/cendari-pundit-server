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

package eu.semlibproject.annotationserver.restapis;

import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.SesameRDFJSONConverter;
import eu.semlibproject.annotationserver.hibernate.Userdata;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.managers.RepositoryManager;
import eu.semlibproject.annotationserver.managers.TokenManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.RepositoryException;
import eu.semlibproject.annotationserver.security.PermissionsManager;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Base class for all class implementing restful API.
 * It contains contains shared and helper methods for restful API.
 * 
 * @author Michele Nucci
 */
public class APIHelper {

    // For user token
    protected String userToken = null;
    
    private Logger logger = Logger.getLogger(APIHelper.class.getName());
            
    public APIHelper(@Context HttpServletRequest req, @Context ServletContext servletContext) {
       ConfigManager configManager = ConfigManager.getInstance();
        
        if (servletContext != null) {
            configManager.setMainServletContext(servletContext);            
        }
            
        configManager.setServletContextPath(req.getContextPath());
        
        String token = (String) req.getAttribute(SemlibConstants.LOGGED_USER_ATTR);
        if (StringUtils.isNotBlank(token)) {
            this.userToken = token;
        }
    }   
    
    
    public User getCurrentLoggedUser() {        
        if (StringUtils.isNotBlank(userToken)) {
            return TokenManager.getInstance().getUserFromToken(userToken);
        }
        
        return null;
    }
    
    
    public User getCurrentLoggedUserOrAnonUser() {
        User currentLoggedUser = this.getCurrentLoggedUser();
        if (currentLoggedUser == null || StringUtils.isBlank(currentLoggedUser.getUserID()) || !currentLoggedUser.isAuthenticated() ) {
            return User.createAnonymousUser();
        }
        
        return currentLoggedUser;
    }
    
    
    // ==== Notebooks API Helpers ================    
    
    /**
     * Check if a specified Notebook exists and if it is valid.
     * 
     * @param notebookID                the Notebook ID to check
     * @param checkForPublicReadability if <code>true</code> check if the specified Notebook exists
     *                                  and if it is public readable.
     * 
     * @return                          <ul>
     *                                      <li>Status.OK:  if the specified Notebook exists and if it is valid. If the
     *                                          param <code>checkForPublicReadability</code> is <code>true</code> 
     *                                          the specified Notebooks is also public readable</li>
     *                                      <li>Status.NOT_FOUND: if the specified Notebook does not exists</li>
     *                                      <li>Status.FORBIDDEN: if the specified Notebook exists but it is not public readable</li>
     *                                      <li>Status.BAD_REQUEST: if the specified Notebook is not valid</li>
     *                                      <li>Status.INTERNAL_SERVER_ERROR: in case of general internal error</li>
     *                                  </ul>
     */
    public Response.Status checkNotebookID(String notebookID, boolean checkForPublicReadability) {        
        
        int hashNumOfChars = UtilsManager.getInstance().getCurrentHASHLenght();
        if (hashNumOfChars == -1) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
        
        if (StringUtils.isBlank(notebookID) || notebookID.length() < hashNumOfChars) {
            return Response.Status.BAD_REQUEST;            
        } else {

            try {
                if (checkForPublicReadability) {
                    // Check if the specified notebook exists and it is public readable
                    return RepositoryManager.getInstance().getCurrentDataRepository().notebookExistsAndIsPublic(notebookID);
                } else {
                    // Check if the specified notebook exists        
                    return (!RepositoryManager.getInstance().getCurrentDataRepository().notebookExists(notebookID)) ? Status.NOT_FOUND : Status.OK;
                }
            } catch (RepositoryException ex) {
               logger.log(Level.SEVERE, null, ex);
                return Response.Status.INTERNAL_SERVER_ERROR;
            }
        }
    }
    
    
    /**
     * Check if a specified Notebook is public or private and return a JSON response
     * 
     * @param callback          the JSONP callback or null
     * @param notebookID        the Notebook ID
     * @param accepts           the accepted data forma
     * @param checkForPrivate   If <code>true</code> check if the specified Notebook is private;
     *                          if <code>false</code> check if the specified Notebook is public
     * 
     * @return              This API return a response containing JSON data with the following format:<br/>
     *                      <code>{ "NotebookPublic"|"NotebookPrivate": "0|1" }</code><br/><br/>
     */
    public Response notebookPublicOrPrivate(String callback, String notebookID, String accepts, boolean checkForPrivate) {
        Status notebookIDStatus = this.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        try {
            boolean notebookPublic = RepositoryManager.getInstance().getCurrentDataRepository().isNotebookPublic(notebookID);
            
            JSONObject jsonData = new JSONObject();
            if (checkForPrivate) {
                jsonData.put(SemlibConstants.JSON_NOTEBOOK_PRIVATE, ( (notebookPublic) ? "0" : "1" ) );
            } else {
                jsonData.put(SemlibConstants.JSON_NOTEBOOK_PUBLIC, ( (notebookPublic) ? "1" : "0" ) );
            }
            
            String fResponse = jsonData.toString(2);
            String faccepts = MediaType.APPLICATION_JSON;

            return this.createFinalResponseForNotebooksAPI(Response.Status.OK, callback, faccepts, fResponse, null);            
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Get all metadata about a specified Notebook. 
     * Core implementation. This API do not check user rights.     
     * 
     * @param notebookID    the Notebook ID
     * @param callback      the JSONP callback function or null
     * @param accepts       the accepted format (application/rdf+xml or application/json)
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" and the RDF graph as payload response</li>
     *                          <li>"204 No Content" if there are no triples in the specified notebook</li>
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"404 Not Found" if the specified notebook does not exists</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    public Response notebookMetadata(String notebookID, String callback, String accepts) {        
        // This is necessary to handle the GET request from normal browser
        UtilsManager utilsManager = UtilsManager.getInstance();
        String cAccepts = utilsManager.getCorrectAcceptValue(callback, accepts);
        String triplesFormat = utilsManager.getCorrectTripleFormat(callback, accepts, cAccepts);
                
        try {
            String triples = RepositoryManager.getInstance().getCurrentRDFRepository().getNotebookMetadata(notebookID, triplesFormat);
            if (triples == null || triples.length() == 0) {
                return Response.noContent().build();
            } else {
                return createFinalResponseForNotebooksAPI(Response.Status.OK, callback, cAccepts, triples, null);
            }
        } catch (RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }        
    }
    
    
    /**
     * Get all content contained in a specified Notebook
     * 
     * @param notebookId        the Notebook ID
     * @param limit             max number of annotation to retrieve (-1 for no limits)
     * @param offset            offset for annotations retrieval (-1 for no offset)
     * @param orderingMode      ordering mode (1 = desc)
     * @return 
     */
    public Response dumpOfNotebook(String notebookId, String callback, String accepts, String limit, String offset, String orderingMode) {
        try {
            String notebookMetadata = RepositoryManager.getInstance().getCurrentRDFRepository().getNotebookMetadata(notebookId, MediaType.APPLICATION_JSON);
            if (notebookMetadata == null) {
                return Response.status(Status.BAD_REQUEST).build();
            } else if (notebookMetadata.length() == 0) {
                return Response.status(Status.NO_CONTENT).build();
            }
            
            JSONObject finalJSONData = new JSONObject();
            finalJSONData.put(SemlibConstants.JSON_METADATA, notebookMetadata);
            
            JSONArray annotationsData = new JSONArray();
            
            int qLimit   = UtilsManager.getInstance().parseLimitOrOffset(limit);
            int qOffset  = UtilsManager.getInstance().parseLimitOrOffset(offset);
        
            boolean desc = false;
            if (orderingMode != null && orderingMode.equals("1")) {
                desc = true;
            }
            
            List<String> annotationIDs = RepositoryManager.getInstance().getCurrentRDFRepository().getAnnotationsIDsInNotebook(notebookId, qLimit, qOffset, desc);
            if (annotationIDs.size() > 0) {
                for (String cID : annotationIDs) {
                    String data = this.allAnnotationData(cID);
                    if (StringUtils.isNotBlank(data)) {
                        annotationsData.put(data);
                    }
                }
            }
            
            finalJSONData.put(SemlibConstants.JSON_ANNOTATIONS, annotationsData);
            
            // This is necessary to handle the GET request from normal browser
            String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accepts);
            return this.createFinalResponseForAnnotationsAPI(callback, cAccepts, finalJSONData.toString(2));
            
        } catch (JSONException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (RepositoryException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Get a list of Notebooks basing on NotebookAPI.NotebookListType
     * 
     * @param listType  the Notebooks list type
     * @param callback  the JSONP callback function
     * 
     * @return          a list of Notebook IDs
     */
    public Response getNotebooksList(NotebooksAPI.NotebookListType listType, String callback) {
        
        User currentLoggedUser = getCurrentLoggedUserOrAnonUser();
        
        try {
            List<String> notebookIDsList = null;
            if (listType == NotebooksAPI.NotebookListType.ACTIVE_NOTEBOOKS) {
                notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getActiveNotebooksForCurrentLoggedUser(currentLoggedUser.getUserID());
            } else if (listType == NotebooksAPI.NotebookListType.OWNED_NOTEBOOKS) {
                notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getNotebooksOwnedByUser(currentLoggedUser.getUserID());
            } else if (listType == NotebooksAPI.NotebookListType.PUBLIC_NOTEBOOKS) {
                notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getAllPublicNotebooks();
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }            
            
            if (notebookIDsList == null || notebookIDsList.isEmpty()) {
                return Response.status(Status.NO_CONTENT).build();
            }
            
            JSONObject jsonData = new JSONObject();
            JSONArray notebookList = new JSONArray(notebookIDsList);
            
            try {
                jsonData.put(SemlibConstants.JSON_NOTEBOOK_IDS, notebookList);
                
                String fResponse = jsonData.toString(2);
                String faccepts = MediaType.APPLICATION_JSON;

                return createFinalResponseForNotebooksAPI(Response.Status.OK, callback, faccepts, fResponse, null);            

            } catch (JSONException ex) {
                logger.log(Level.SEVERE, null, ex);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (RepositoryException re) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    /**
     * Get the list of all annotations contained within a Notebook with related metadata. 
     * Core implementation. This API do not check user rights. 
     * 
     * @param notebookID    the Notebook ID
     * @param callback      the JSONP callback function or null     
     * @param accepts       the accepted format (application/rdf+xml or application/json)
     * @param limit         max number of annotation to retrieve (-1 for no limits)
     * @param offset        offset for annotations retrieval (-1 for no offset)
     * @param orderby       property for order by
     * @param orderingMode  (optional) 1/0 (desc/asc) for orderby
     * @return              The list of all annotations contained within a Notebook and related metadata as response payload.
     *                      The returned data format could be: application/rdf+xml or application/json (see the accepts parameter)<br/><br/>
     *                      HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of succe</li>
     *                          <li>"204 No Content" if there are no triples in the specified notebook</li>
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"403 Forbidden" if the current logged User has not the correct right to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified notebook does not exists</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */ 
    public Response annotationsListAndMetadata(String notebookID, String callback, String accepts, String orderby, String orderingMode, String limit, String offset) {
        int qLimit   = UtilsManager.getInstance().parseLimitOrOffset(limit);
        int qOffset  = UtilsManager.getInstance().parseLimitOrOffset(offset);
        boolean desc = false;
                
        if (orderingMode != null && orderingMode.equals("1")) {
            desc = true;
        }
        
        // This is necessary to handle the GET request from normal browser
        UtilsManager utilsManager = UtilsManager.getInstance();
        String cAccepts = utilsManager.getCorrectAcceptValue(callback, accepts);
        String triplesFormat = utilsManager.getCorrectTripleFormat(callback, accepts, cAccepts);
                        
        Notebook tempNotebook = Notebook.getEmptyNotebookObject();
        tempNotebook.setID(notebookID);
        
        try {
            String triples = RepositoryManager.getInstance().getCurrentRDFRepository().getNotebookAnnotationListAndMetadata(tempNotebook, qLimit, qOffset, orderby, desc, triplesFormat);
            if (triples == null || triples.length() == 0) {
                return Response.status(Status.NO_CONTENT).build();
            } else {
                return createFinalResponseForNotebooksAPI(Response.Status.OK, callback, cAccepts, triples, null);
            }
        } catch (RepositoryException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Get the RDF graph composed by all the triples of the annotations in the specified notebook. 
     * Core implementation. This API do not check user rights.
     * 
     * @param callback      the JSONP callback function or null
     * @param notebookID    the notebook ID
     * @param accepts       the accepted format (application/rdf+xml or application/json)
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" and the RDF graph as payload response</li>
     *                          <li>"204 No Content" if there are no triples in the specified notebook</li>
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"404 Not Found" if the specified notebook does not exists</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    public Response annotationsTriples(String notebookID, String callback, String accepts) {
        // This is necessary to handle the GET request from normal browser
        UtilsManager utilsManager = UtilsManager.getInstance();
        String cAccepts           = utilsManager.getCorrectAcceptValue(callback, accepts);
        String triplesFormat      = utilsManager.getCorrectTripleFormat(callback, accepts, cAccepts);
        
        String triples = RepositoryManager.getInstance().getCurrentRDFRepository().getAllTriplesAnnotations(notebookID, triplesFormat);
        if (StringUtils.isBlank(triples)) {
            return Response.noContent().build();
        } else {
            return createFinalResponseForNotebooksAPI(Response.Status.OK, callback, cAccepts, triples, null);
        }        
    }
    
    
    /**
     * Create the final response for the client basing on callback and contentType.
     * 
     * @param responseStatus        the response status for the final response
     * @param callback              the JSONP callback function
     * @param contentType           the final Content-Type for this response
     * @param data                  the response data
     * @param createdURI            the URI for the Created response (could be null for other type of response)
     * 
     * @return                      the final Response for the current request
     */
    public Response createFinalResponseForNotebooksAPI(Response.Status responseStatus, String callback, String contentType, String data, String createdURI) {
        
        String fContentType = contentType;
        String fResponse    = data;
        
        if (StringUtils.isNotBlank(callback)) {
            
            // We have a JSONP request
            fContentType = MediaType.APPLICATION_JAVASCRIPT;
            fResponse    = UtilsManager.getInstance().wrapJSONPResponse(fResponse, callback);
        
        } else if (contentType != null && contentType.contains(MediaType.TEXT_HTML)) {
            
            // We have a request from a normal browser
            // In this case is correct becouse we have JSON that will be shown as plain text
            fContentType = MediaType.TEXT_PLAIN;
            
        }

        if (responseStatus == Response.Status.CREATED && (createdURI != null && createdURI.length() > 0) ) {
            return Response.created(URI.create(createdURI)).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, fContentType).entity(fResponse).build();
        } else if (fContentType != null) {
            return Response.ok(fResponse).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, fContentType).build();
        } else {
            return Response.ok(fResponse).build();
        }       

    }

    /**
     * Create a new Annotation
     * 
     * @param notebookID            a Notebook ID
     * @param annotationData        the annotation data
     * @param contentType           the content type of the annotation data
     * @param annotationContext     additional annotation contexts
     * 
     * @return                  The ID of the created annotation in JSON format as response payload:<br/>
     *                          <code>{ AnnotationID : { ANNOTATION-ID } }</code><br/>
     *                          HTTP Header:<br/> 
     *                          Location: URI-of-the-new-annotation<br/><br/>
     *                          HTTP Response Status Code:
     *                          <ul>
     *                              <li>"201 Created" on success</li>
     *                              <li>"400 Bad Request" if the request is not correct</li>
     *                              <li>"403 Forbidden" if the current logged User has not the correct rights to access to the specified Notebooks</li>
     *                              <li>"500 Internal Server Error" in case of internal error</li>
     *                          </ul>            
     */
    protected Response createNewAnnotation(String notebookID, String annotationData, String contentType, String annotationContext) {
        
        User currentLoggedUser = this.getCurrentLoggedUserOrAnonUser();        
        
        Status notebookIDStatus = this.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        try {            
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean canPostNewAnnotation = permManager.canWriteNotebook(currentLoggedUser.getUserID(), notebookID);        
            if (!canPostNewAnnotation) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException se) {
            logger.log(Level.SEVERE, null, se);
            return Response.status(se.getStatusCode()).build();
        }
        
        // ..everything should be ok so, create the new Annotation
        Annotation newAnnotation = null; 
        try {
            newAnnotation = Annotation.createNewAnnotation(currentLoggedUser, annotationData, contentType, notebookID, annotationContext);
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (UnsupportedEncodingException ex1) {
            logger.log(Level.SEVERE, null, ex1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (NoSuchAlgorithmException ex2) {            
            logger.log(Level.SEVERE, null, ex2);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        Status response = RepositoryManager.getInstance().writeAnnotation(currentLoggedUser, newAnnotation);
        if (response == Status.BAD_REQUEST) {
            return Response.status(response).build();
        } else if (response != Status.OK) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
                
        // Create JSON response
        JSONObject jsonResponse = new JSONObject();
        try {

            jsonResponse.put(SemlibConstants.JSON_ANNOTATION_ID, newAnnotation.getID());
            
            String fResponse    = jsonResponse.toString();
            String fContentType = MediaType.APPLICATION_JSON;
            
            return this.createFinalResponseForNotebooksAPI(Response.Status.CREATED, null, fContentType, fResponse, newAnnotation.getURI());            
                        
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Prepare a SPARQL query to be executed on the internal SPARQL end-point
     * 
     * @param query             the SPARQL query
     * @param annotationsIDs    the list of all annotations ID in a Notebooks
     * @return                  the prepared query
     */
    public String prepareQueryForNotebooksSPARQLEndPoint(String query, List<String> annotationsIDs) {
        
        String froms      = "";
        String fromNameds = "";
        
        for (String annID : annotationsIDs) {
            String annotationGraph = Annotation.getGraphURIFromID(annID);
            String itemGraph       = Annotation.getItemsGraphURIFormID(annID);
            froms += "FROM <" + annotationGraph + "> FROM <" + itemGraph + "> ";
            fromNameds += "FROM NAMED <" + annotationGraph + "> FROM NAMED <" + itemGraph + "> ";
        }
        
        String finalFroms = " " + froms + fromNameds;
        
        // Remove any existing FROM and FROM NAMED from the original query
        int startIndex       = -1;
        Pattern regExPattern = Pattern.compile("(FROM <.+>\\s*|FROM NAMED <.+>\\s*)+", Pattern.CASE_INSENSITIVE);
        Matcher matcher      = regExPattern.matcher(query);
                
        if (matcher.find()) {
            startIndex = matcher.start();
            String cleanQuery = matcher.replaceAll("");
            
            StringBuilder finalQuery = new StringBuilder(cleanQuery);
            finalQuery.insert(startIndex, finalFroms);
                                                
            return finalQuery.toString();
        } else {
            int indexOfWhere = query.toLowerCase().indexOf("where");
            StringBuilder strBuilder = new StringBuilder(query);
            strBuilder.insert(indexOfWhere, finalFroms);
            return strBuilder.toString();
        }                
    }
    
    
    /**
     * Prepare the path for the URL to query the internal SPARQL endpoint
     * 
     * @param query             the query to process
     * @param annotationIDs     a list of Annotation's IDs
     * @param httpRequest       the original HTTP request
     * @return                  the prepared URL query path
     */
    public String prepareParametersForSPARQLEndPointQuery(String query, List<String> annotationIDs, HttpServletRequest httpRequest) {
        String finalQuery      = this.prepareQueryForNotebooksSPARQLEndPoint(query, annotationIDs);    
        String finalParameters = "query=" + finalQuery;
        Enumeration<String> parametersName = httpRequest.getParameterNames();
        while (parametersName.hasMoreElements()) {
            String parameter = parametersName.nextElement();
            if (parameter.equalsIgnoreCase("query")) {
                continue;
            }
            
            String parameterValue = httpRequest.getParameter(parameter);
            finalParameters += "&" + parameter + "=" + parameterValue;
        }
        
        return finalParameters;
    }
    
    
    /**
     * Execute a given SPARQL query
     * 
     * @param requestParameters all Requeste parameters
     * @param accept            the type of the accepted payload. 
     *                          For this, see the supported payload by the current RDF Storage
     * @return                  a Response with the result of the SPARQL query
     */
    public Response executeQueryOnSPARQLEndPoint(String query, String requestParameters, String accept) {                
        
        ConfigManager configManager = ConfigManager.getInstance();        
        String sparqlEndPointURL    = RepositoryManager.getInstance().getCurrentRDFRepository().getSPARQLEndPointUrl();
        
        try {                        
            // This is necessary to obtain a correct URL escaping! =====
            // For more info, see the Java documentation...
            URL tempUrl = new URL(sparqlEndPointURL);
            java.net.URI uri = new java.net.URI(tempUrl.getProtocol(), tempUrl.getHost() + ":" + tempUrl.getPort(), tempUrl.getPath(), requestParameters, null);            
            String queryURL = uri.toString();
            // =========================================================
                        
            GetMethod getRequest = new GetMethod(queryURL);
            if (StringUtils.isNotBlank(accept)) {
                if (accept.contains(MediaType.APPLICATION_RDFJSON)) {
                    Pattern startPattern = Pattern.compile("^\\s*CONSTRUCT.", Pattern.CASE_INSENSITIVE);
                    Matcher nMatcher = startPattern.matcher(query);
                    if (!nMatcher.find()) {
                        // in this case is a select query...JSON/RDF, at the moment, is not supported for tuples
                        return Response.status(Status.BAD_REQUEST).build();
                    }
                    
                    getRequest.addRequestHeader(SemlibConstants.HTTP_HEADER_ACCEPT, MediaType.APPLICATION_RDFXML);
                } else {
                    getRequest.addRequestHeader(SemlibConstants.HTTP_HEADER_ACCEPT, accept);
                }                
            }
            
            if (configManager.useAuthenticationForRepository()) {
                String username = configManager.getUsername();
                String password = configManager.getPassword();
                String encodedData = UtilsManager.getInstance().base64Encode(username + ":" + password);                
                getRequest.addRequestHeader("Authorization", "Basic " + encodedData);                                                
            }
            
            HttpClient httpClient = new HttpClient();
            
            try {
                int statusCode = httpClient.executeMethod(getRequest);
                if (statusCode != HttpStatus.SC_OK) {
                    // Get the returned payload                    
                    String payloadContent = getRequest.getResponseBodyAsString();
                    
                    ResponseBuilder responseBuilder = Response.status(statusCode);
                    
                    if (StringUtils.isNotBlank(payloadContent)) {
                        responseBuilder.entity(payloadContent);
                                                                                
                        Header CTheader = getRequest.getResponseHeader(SemlibConstants.HTTP_HEADER_CONTENT_TYPE);
                        if (CTheader != null) {
                            String contentTypeValue = CTheader.getValue();
                            if (StringUtils.isNotBlank(contentTypeValue)) {
                                responseBuilder.header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, contentTypeValue);
                            }
                        }                                                                
                    }
                    
                    return responseBuilder.build();
                } else {
                    // Get the returned payload                    
                    String payloadContent = getRequest.getResponseBodyAsString();
                    
                    // Get the Content-Type
                    String contentTypeValue        = null;
                    String contentDispositionValue = null;
                    
                    ResponseBuilder responseBuilder = Response.status(statusCode);
                    
                    Header CTheader = getRequest.getResponseHeader(SemlibConstants.HTTP_HEADER_CONTENT_TYPE);
                    Header CDheader = getRequest.getResponseHeader(SemlibConstants.HTTP_HEADER_CONTENT_DISPOSITION);
                    
                    if (CTheader != null) {
                        if (accept.contains(MediaType.APPLICATION_RDFJSON)) {
                            payloadContent = SesameRDFJSONConverter.getInstance().RDFToJson(payloadContent);
                            responseBuilder.header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_RDFJSON);
                        } else {
                            contentTypeValue = CTheader.getValue();
                            responseBuilder.header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, contentTypeValue);
                        }
                    }
                    
                    if (CDheader != null) {
                        contentDispositionValue = CDheader.getValue();
                        responseBuilder.header(SemlibConstants.HTTP_HEADER_CONTENT_DISPOSITION, contentDispositionValue);
                    }                                                                                
                    
                    return responseBuilder.entity(payloadContent).build();
                }
            } catch (Exception ex) {
                Logger.getLogger(APIHelper.class.getName()).log(Level.SEVERE, null, ex);                
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } finally {
                getRequest.releaseConnection();
            }
            
        } catch (URISyntaxException ex) {
            Logger.getLogger(APIHelper.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (MalformedURLException ex) {
            Logger.getLogger(APIHelper.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    
    // ==== Annotations API Helpers ================
    
    /**
     * Get specific data about a specific annotation
     * 
     * @param annotationID          an Annotation ID
     * @param annotationDataType    an AnnotationDataType (@see Annotation.AnnotationDataType)
     * @param dataFormat            the format of the output data           
     * @return                      a String containing specific data about a specified annotation
     * 
     * @throws RepositoryException 
     */
    public String annotationData(String annotationID, Annotation.AnnotationDataType annotationDataType, String dataFormat) throws RepositoryException {
        String data = null;
        if (annotationDataType == Annotation.AnnotationDataType.ANNOTATION_METADATA) {
            return RepositoryManager.getInstance().getCurrentRDFRepository().getAnnotationMetadata(annotationID, dataFormat);
        } else if (annotationDataType == Annotation.AnnotationDataType.ANNOTATION_GRAPH) {
            return RepositoryManager.getInstance().getCurrentRDFRepository().getAnnotationTriples(annotationID, dataFormat);
        } else if (annotationDataType == Annotation.AnnotationDataType.ANNOTATION_ITEM) {
            return RepositoryManager.getInstance().getCurrentRDFRepository().getAllAnnotationItems(annotationID, dataFormat);
        }
        
        return data;
    }
    
    
    /**
     * Get all data metadata, graph and items (if they exist) about a specified annotation.
     * The output data format is:<br/>
     * <pre>
     * {
     *      "metadata": {
     *          // JSON/RDF
     *          ... ... ...
     *      },
     *      "graph": {
     *          // JSON/RDF
     *          ... ... ...
     *      },
     *      "itesm": {
     *          // JSON/RDF
     *          ... ... ...
     *      }
     * }
     * </pre>
     * 
     * 
     * @param annotationID      an Annotation ID
     * @return                  all data about a specified annotation
     * 
     * @throws JSONException 
     */
    public String allAnnotationData(String annotationID) throws JSONException {
        
        String annotationMetadata = null;
        String annotationGraph    = null;
        String annotationItems    = null;
        
        try {
            annotationMetadata = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_METADATA, MediaType.APPLICATION_JSON);                        
        } catch (RepositoryException re) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, null, re);
        }
        
        try {
            annotationGraph    = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_GRAPH, MediaType.APPLICATION_JSON);
        } catch (RepositoryException re) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, null, re);
        }
        
        try {
            annotationItems    = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_ITEM, MediaType.APPLICATION_JSON);
        } catch (RepositoryException re) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, null, re);
        }
        
        if (StringUtils.isBlank(annotationMetadata) && StringUtils.isBlank(annotationGraph) && StringUtils.isBlank(annotationItems)) {
            return null;
        } else {
            JSONObject jsonData = new JSONObject();
            
            if (StringUtils.isNotBlank(annotationMetadata)) {
                JSONObject jsonMetadata = new JSONObject(annotationMetadata);
                jsonData.put(SemlibConstants.JSON_METADATA, jsonMetadata);
            }

            if (StringUtils.isNotBlank(annotationGraph)) {
                JSONObject jsonGraph = new JSONObject(annotationGraph);
                jsonData.put(SemlibConstants.JSON_GRAPH, jsonGraph);
            }

            if (StringUtils.isNotBlank(annotationItems)) {
                JSONObject jsonItems = new JSONObject(annotationItems);
                jsonData.put(SemlibConstants.JSON_ITEMS, jsonItems);
            }

            return jsonData.toString();
        }
    }    
    
    
    /**
     * Get all metadata associated to a specified annotation.
     * Core implementation. This API do not check user rights.
     * 
     * @param annotationID  the ID of the Annotation
     * @param callback      the JSONP callback function
     * @param accept        the accepted format
     * 
     * @return all metadata associated to a a specified annotation The returned format 
     *         must be specified using the HTTP header "Accept:".<br/><br/>
     *         HTTP Response Status Code:
     *         <ul>
     *          <li>200 "OK": and the annotation metadata in payload (using the specified format)</li>
     *          <li>400 "Bad Request": if the specified annotation ID is not valid</li>
     *          <li>404 "Not Found": if the speficied annotation ID does not exists</li>
     *          <li>500 "Internal Server Error": in case of error</li>
     *         </ul>
     */
    public Response annotationMetadata(String annotationID, String callback, String accept) {
        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);        
        
        String annotationMetadata = null;
        try {
            
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }
            
            annotationMetadata = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_METADATA, triplesFormat);
            
        } catch (Exception ex) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        return createFinalResponseForAnnotationsAPI(callback, cAccepts, annotationMetadata);
    }
    
    
    /**
     * Get all the triples associated to a specific annotation.
     * Core implementation. This API do not check user rights.
     * 
     * @param annotationID  the ID of the Annotation
     * @param callback      the JSONP callback function
     * @param accept        the accepted format
     * 
     * @return all triples associated to a specified annotation. The returned format 
     *         must be specified using the HTTP header "Accept:".<br/><br/>
     *         HTTP Response Status Code:
     *         <ul>
     *          <li>200 "OK": and the annotation metadata in payload (using the specified format)</li>
     *          <li>400 "Bad Request": if the specified annotation ID is not valid</li>
     *          <li>404 "Not Found": if the speficied annotation ID does not exists</li>
     *          <li>500 "Internal Server Error": in case of error</li>
     *         </ul>
     */
    public Response annotationContent(String annotationID, String callback, String accept) {
        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);
        
        String annotationData = null;
        try {
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }

            annotationData = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_GRAPH, triplesFormat);
                    
        } catch (Exception ex) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        return createFinalResponseForAnnotationsAPI(callback, cAccepts, annotationData);
    }
    
    
    /**
     * Return all Items associated to a given annotation
     * Core implementation. This API do not check user rights.
     * 
     * @param annotationID  a valid annotation ID
     * @param callback      callback function for JSONP 
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * 
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */ 
    public Response annotationItems(String annotationID, String callback, String accept) {
        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);

        String annotationItems = null;
        try {
            
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }
            
            annotationItems = this.annotationData(annotationID, Annotation.AnnotationDataType.ANNOTATION_ITEM, triplesFormat);
            
        } catch (Exception ex) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        return createFinalResponseForAnnotationsAPI(callback, cAccepts, annotationItems);
    }
    
    
    /**
     * Search for all annotation metadata according to specific parameters in all public Notebooks
     * Core implementation. This API do not check user rights.
     * 
     * @param notebookIDsList   a list of Notebooks ID
     * @param callback          callback function for JSONP
     * @param queryParams       parameters (JSON format), encoded
     * @param cAccept           the accepted format (n3, application/rdf+xml or application/json)
     * @param qLimit            max number of annotation to retrieve
     * @param qOffset           offset for annotations retrieval
     * @param orderby           property for order by
     * @param desc              <code>true</code> descending order
     * 
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>     
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    public Response searchAnnotationMetadata(List<String> notebookIDsList, String queryParams, String callback, String cAccepts, int qLimit, int qOffset, String orderby, boolean desc) {
        String annotationMetadata = null;
        try {
            
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }
            
            annotationMetadata = RepositoryManager.getInstance().getCurrentRDFRepository().searchMetadataWithParameters(queryParams, qLimit, qOffset, orderby, desc, triplesFormat, notebookIDsList);
            
        } catch (Exception ex) {
            Logger.getLogger(AnnotationsAPI.class.getName()).log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        return createFinalResponseForAnnotationsAPI(callback, cAccepts, annotationMetadata);
    }

    
    /**
     * Get generic data from the RDB given a key
     * 
     * @param key   the key
     * @return  a Response contains the favorites items for the current logged user<br/><br/>
     *          HTTP Response Status Code:
     *          <ul>
     *              <li>"200 Ok" in case of success</li>
     *              <li>"203 Forbidden" if this API is called whit no logged user</li>
     *              <li>"204 No Content" if there are no favorites items for the current logged user</li>
     *              <li>"500 Internal Server Error" for internal errors</li>
     *          </ul>
     */
    public Response getGenericDataByKey(String key) {
        User currentLoggedUser = this.getCurrentLoggedUser();
        if (currentLoggedUser == null || StringUtils.isBlank(currentLoggedUser.getUserID())) {
            return Response.status(Status.FORBIDDEN).build();
        }         

        if (StringUtils.isBlank(key)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        String userID = currentLoggedUser.getUserID();
                
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();        
            hSession.beginTransaction();        
            Query query = hSession.createQuery("from Userdata as cdata where cdata.userid = :uid and cdata.datakey = :datakey");
            query.setParameter("uid", userID);
            query.setParameter("datakey", key);
            List<Userdata> userdata = query.list();
            hSession.getTransaction().commit();
            
            if (userdata != null && userdata.size() > 0) {
                Userdata cUserData = userdata.get(0);
                String data = cUserData.getData();
                if (StringUtils.isNotBlank(data)) {
                    return Response.status(Status.OK).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.TEXT_PLAIN_UTF8).entity(data).build();
                } else {
                    return Response.status(Status.NO_CONTENT).build();
                }
            } else {
                return Response.status(Status.NO_CONTENT).build();
            }
            
        } catch (HibernateException he) {            
            Logger.getLogger(ServicesAPI.class.getName()).log(Level.SEVERE, null, he);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }        
    }
    
    
    
    /**
     * Store generic data into the RDB (key => value)
     * 
     * @param key   the key
     * @param data  the data to store
     * @return      HTTP Response Status Code:<br/>
     *              <ul>
     *                  <li>"200 Ok" in case of success</li>
     *                  <li>"203 Forbidden" if this API is called whit no logged user</li>
     *                  <li>"400 Bad Request" if the data or the request is not valid</li>
     *                  <li>"500 Internal Server Error" for internal errors</li>
     *              </ul>
     */
    public Response storeGenericDataByKey(String key, String data) {        
        User currentLoggedUser = this.getCurrentLoggedUser();
        if (currentLoggedUser == null || StringUtils.isBlank(currentLoggedUser.getUserID()) ) {
            return Response.status(Status.FORBIDDEN).build();
        }
        
        if (StringUtils.isBlank(data) || StringUtils.isBlank(key)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        String userID = currentLoggedUser.getUserID();

        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("update Userdata set data = :data where userid = :uid and datakey = :datakey");
            query.setParameter("data", data);
            query.setParameter("uid", userID);            
            query.setParameter("datakey", key);
            int updResults = query.executeUpdate();
            hSession.getTransaction().commit();
            
            if (updResults <= 0) {
                Userdata userdata = new Userdata();
                userdata.setUserid(userID);
                userdata.setData(data);
                userdata.setDatakey(key);
                
                Session h2Session = HibernateManager.getSessionFactory().getCurrentSession();
                
                h2Session.beginTransaction();
                h2Session.save(userdata);                                                
                h2Session.getTransaction().commit();
                
                int recordId = userdata.getId();
                if (recordId <= 0) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            
            return Response.status(Status.OK).build();
            
        } catch (HibernateException he) {
            Logger.getLogger(ServicesAPI.class.getName()).log(Level.SEVERE, null, he);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }        
    }
    
    
    /**
     * Check if a specified Annotation ID exists and if it is valid
     * 
     * @param annotationID                  the Annotation ID to check
     * @param checkIfIsInAPublicNotebook    if <code>true</code> check if the specified Annotation is also in a public Notebook
     * 
     * @return              <ul>
     *                          <li>Status.OK: if the specified Annotation exists and if it is valid</li>
     *                          <li>Status.NOT_FOUND: if the specified Annotation does not exists</li>
     *                          <li>Status.FORBIDDEN: if the specified Annotation is not in a public Notebook</li>
     *                          <li>Status.BAD_REQUEST: if the specified Annotation is not valid</li>
     *                          <li>Status.INTERNAL_SERVER_ERROR: in case of general internal error</li>
     *                      </ul>
     */
    public Status checkAnnotationID(String annotationkID, boolean checkIfIsInAPublicNotebook) {                
        int hashNumOfChars = UtilsManager.getInstance().getCurrentHASHLenght();
        if (hashNumOfChars == -1) {
            return Status.INTERNAL_SERVER_ERROR;
        }
        
        if (annotationkID == null || annotationkID.length() < hashNumOfChars) {
            return Status.BAD_REQUEST;            
        } else {

            try {
                if (checkIfIsInAPublicNotebook) {
                    return RepositoryManager.getInstance().getCurrentDataRepository().annotationExistsAndIsInPublicNotebooks(annotationkID);
                } else {
                    return (!RepositoryManager.getInstance().getCurrentDataRepository().annotationExists(annotationkID)) ? Status.NOT_FOUND : Status.OK;
                }
            } catch (RepositoryException ex) {
                logger.log(Level.SEVERE, null, ex);
                return Status.INTERNAL_SERVER_ERROR;
            }
        }
    }

    
    /**
     * Create the final response for the current request
     * 
     * @param callback          the callback function to use (jsonp). <code>null</code> for no JSONP response
     * @param contentType       the response Content-Type
     * @param annotationData    the annotation data
     * 
     * @return                  the Response with the annotation data
     */
    public Response createFinalResponseForAnnotationsAPI(String callback, String contentType, String annotationData) {
        
        if (annotationData == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } else {
            if (StringUtils.isBlank(annotationData)) {
                return Response.status(Status.NO_CONTENT).build();
            } else if (annotationData.length() > 0) {
                if (StringUtils.isNotBlank(callback)) {
                    String fResponse = UtilsManager.getInstance().wrapJSONPResponse(annotationData, callback);
                    return Response.ok(fResponse, contentType).build();
                } else {
                    return Response.ok(annotationData, contentType).build();
                }
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        }
        
    }
    
}
