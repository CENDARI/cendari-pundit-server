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
import eu.semlibproject.annotationserver.managers.RepositoryManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.RepositoryException;
import eu.semlibproject.annotationserver.security.PermissionsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Main class that implements all REST APIs under
 * the namespace "[our-webapps]/notebooks/"
 * 
 * @author Michele Nucci
 */
@Path("/notebooks")
public class NotebooksAPI extends APIHelper {
    
    private Logger logger = Logger.getLogger(NotebooksAPI.class.getName());
            
            
    public enum NotebookListType {
        PUBLIC_NOTEBOOKS,
        ACTIVE_NOTEBOOKS,
        OWNED_NOTEBOOKS
    }
        
    /**
     * Default constructor.
     * 
     * @param req               the servlet's request passed by the Jersy framework
     * @param servletContext    the servlet context
     */
    public NotebooksAPI(@Context HttpServletRequest req, @Context ServletContext servletContext) {        
        super(req, servletContext);
    }
    
    
    /**
     * Check if a specified Notebook is public
     * 
     * @param callback      the JSONP callback or null
     * @param notebookID    the ID of the Notebook to check
     * @param accepts       the Accept HTTP header
     * 
     * @return              This API return a response containing JSON data with the following format:<br/>
     *                      <code>{ "NotebookPublic": "0|1" }</code><br/><br/>
     *                      Value:
     *                      <ul>
     *                          <li>0: the specified Notebook is private</li>
     *                          <li>1: the specified Notebook is public</li>
     *                      </ul><br/>
     *                      HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" if the request or the Notebook ID is non correct</li>
     *                          <li>"404 Not Found" if the Notebook ID does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      </ul>
     */
    @GET
    @Path("public/{notebook-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response isNotebookPublic(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        return notebookPublicOrPrivate(callback, notebookID, accepts, false);
    }
    
    
    /**
     * Check if a specified Notebook is private
     * 
     * @param callback      the JSONP callback or null
     * @param notebookID    the ID of the Notebook to check
     * @param accepts       the Accept HTTP header
     * 
     * @return              This API return a response containing JSON data with the following format:<br/>
     *                      <code>{ "NotebookPrivate": "0|1" }</code><br/><br/>
     *                      Value:
     *                      <ul>
     *                          <li>0: the specified Notebook is public</li>
     *                          <li>1: the specified Notebook is private</li>
     *                      </ul><br/>
     *                      HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" if the request or the Notebook ID is non correct</li>
     *                          <li>"404 Not Found" if the Notebook ID does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      </ul>
     */
    @GET
    @Path("private/{notebook-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response isNotebookPrivate(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        return super.notebookPublicOrPrivate(callback, notebookID, accepts, true);
    }

    
    /**
     * Set a Notebook as public.
     * 
     * @param notebookID    the ID of the Notebook to set as Public
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" is the request or the Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebooks does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      <ul>
     */
    @PUT
    @Path("public/{notebook-id}")
    public Response setNotebookAsPublic(@PathParam("notebook-id") String notebookID) {
        return this.setNotebookVisibility(notebookID, true);        
    }
    
    
    /**
     * Set a Notebook as private.
     * 
     * @param notebookID    the ID of the Notebook to set as Private
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" is the request or the Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebooks does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      <ul>
     */
    @PUT
    @Path("private/{notebook-id}")
    public Response setNotebookAsPrivate(@PathParam("notebook-id") String notebookID) {
        return this.setNotebookVisibility(notebookID, false);
    }
    
    
    /**
     * Set a Notebook as public. This API is an alias of the API: PUT /notebooks/{notebook-id}/public
     * 
     * @param notebookID    the ID of the Notebook to set as Public
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" is the request or the Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebooks does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      <ul>
     */
    @DELETE
    @Path("private/{notebook-id}")
    public Response setNotebookAsPublicByDelete(@PathParam("notebook-id") String notebookID) {
        return this.setNotebookAsPublic(notebookID);
    }
    
    
    /**
     * Set a Notebook as private. This API is an alias of the API: PUT /notebooks/{notebook-id}/private
     * 
     * @param notebookID    the ID of the Notebook to set as Private
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" is the request or the Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebooks does not exist</li>
     *                          <li>"500 Internal Server Error" for internal server error</li>
     *                      <ul>
     */    
    @DELETE
    @Path("public/{notebook-id}")
    public Response setNotebookAsPrivateByDelete(@PathParam("notebook-id") String notebookID) {
        return this.setNotebookAsPrivate(notebookID);
    }
    
    
    /**
     * Return the current Notebook for the current User.
     * 
     * @param callback  the JSONP callback or null
     * @param accepts   the value of the HTTP Accept Header
     * @return          the ID of the current Notebook for the current User.
     *                  The Notebook ID is returned has JSON data:<br/>
     *                  <code>{ "NotebookID": "notebokid" }</code>     
     */
    @GET
    @Path("current")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response getCurrentActiveNotebook(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        String notebookID = null;
        try {
            notebookID = RepositoryManager.getInstance().getCurrentDataRepository().getCurrentNotebookID(currentLoggedUser.getUserID());
        } catch (RepositoryException re) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }        
        
        if (notebookID == null) {                                                
            notebookID = this.createCurrentNotebook(currentLoggedUser);
            if (notebookID != null) {
                return this.createResponseForNewNotebook(notebookID, Status.OK);
            } else {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }                                    
        } else {

            try {
                
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put(SemlibConstants.JSON_NOTEBOOK_ID, notebookID);

                String fResponse = jsonResponse.toString();
                String faccepts = MediaType.APPLICATION_JSON;

                return super.createFinalResponseForNotebooksAPI(Response.Status.OK, callback, faccepts, fResponse, null);

            } catch (JSONException ex) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }

        }        
        
    }
    
    
    /**
     * Get the RDF graph composed by all the annotation's triples in the current Notebook for the current logged User.
     * 
     * @param callback      the JSONP callback function or null
     * @param accepts       the accepted format (application/rdf+xml, application/json, text/rdf+n3)     
     * @return              Return a graph composed by all annotation's triples stored in the current Notebook as a payload response.
     *                      The response data format could be: application/rdf+xml, application/json, text/rdf+n3 (see the accepts paramenter).<br/>
     *                      HTTP Status code:
     *                      <ul>
     *                          <li>"200 OK" and the RDF graph as payload response</li>
     *                          <li>"204 No Content" if there are no triples in the current notebook</li>
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"403 Forbidden" if the current logged User has not the correct right to access the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified notebooks do not exist</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("current/graph")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getCurrentNotebookGraph(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {        
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();

        String notebookID = null;
        try {
            notebookID = RepositoryManager.getInstance().getCurrentDataRepository().getCurrentNotebookID(currentLoggedUser.getUserID());
        } catch (RepositoryException re) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        if (notebookID == null) {        
            notebookID = this.createCurrentNotebook(currentLoggedUser);            
        }
                
        if (notebookID == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return this.getAllAnnotationsTriples(callback, notebookID, accepts);
        }        
    }
    
    
    /**
     * Post a new annotation in the current active notebook for the current logged user.
     * 
     * @param annotationData    the JSON data extracted from the payload
     * @param notebookID        the Notebook ID in which to put the annotation
     * @param annotationContex  the context of the annotation
     * @param contentType       the contentType of the annotation data
     * 
     * @return                  The ID of the created annotation in JSON format as response payload:<br/>
     *                          <code>{ AnnotationID : { ANNOTATION-ID } }</code><br/>
     *                          HTTP Header:<br/> 
     *                          Location: URI-of-the-new-annotation<br/><br/>
     *                          HTTP Response Status Code:
     *                          <ul>
     *                              <li>"201 Created" on success</li>
     *                              <li>"400 Bad Request" if the request is not correct</li>     
     *                              <li>"500 Internal Server Error" in case of internal error</li>
     *                          </ul>
     */
    @POST
    @Path("current")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT})
    public Response postAnnotationInCurrentActiveNotebook(String annotationData, @QueryParam(SemlibConstants.CONTEXT_PARAM) String annotationContext, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        
        Response response = getCurrentActiveNotebook(null, contentType);
        if (response.getStatus() != 200) {
            return response;
        } else {
            String strJsonObject = (String)response.getEntity();
            try {
                JSONObject jsonData = new JSONObject(strJsonObject);
                String notebookID = jsonData.getString(SemlibConstants.JSON_NOTEBOOK_ID);
                if (notebookID == null) {
                    return Response.status(Status.BAD_REQUEST).build();
                } else {
                    return createNewAnnotation(notebookID, annotationData, contentType, annotationContext);
                }
                
            } catch (JSONException ex) {
                logger.log(Level.SEVERE, null, ex);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }        
        
    }
    
    
    /**    
     * Create a new notebook with a specific name.
     * 
     * @param notebookName The name of the new Notebooks in JSON format or null. JSON format:<br/>
     *                     <code>{ "NotebookName": "{Notebook-Name}" }
     * @return             The ID od the new created Notebook as payload response in JSON format:<br/>
     *                     <code>{ NotebookID : { NOTEBOOK-ID } }<code><br/>
     *                     HTTP Header:<br/>
     *                     Location: URI-of-the-new-notebook<br/><br/>
     *                     HTTP Response Status Code:
     *                     <ul>
     *                      <li>"201 Created" on success</li>
     *                      <li>"400 Bad Request" if the request is not correct</li>
     *                      <li>"500 Internal Server Error" in case of internal error</li>
     *                     </ul>
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT})
    public Response createNewNotebook(String notebookNameAsJSON) {
        
        User currentUser = super.getCurrentLoggedUserOrAnonUser();
        
        Notebook notebook   = null;        
        String notebookName = null;
        
        if (notebookNameAsJSON != null && notebookNameAsJSON.length() > 0) {
            try {
                JSONObject jsonObj = new JSONObject(notebookNameAsJSON);
                notebookName = jsonObj.getString(SemlibConstants.JSON_NOTEBOOK_NAME);    
            } catch (JSONException ex) {
                notebookName = null;
            }
        }
                
        try {
            notebook = Notebook.createNewNotebook(currentUser, notebookName);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        Status responseCode = RepositoryManager.getInstance().writeNotebook(currentUser, notebook);                
        if (responseCode != Status.OK) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return this.createResponseForNewNotebook(notebook.getID(), Status.CREATED);        
    }
        
    
    /**
     * Create a new Annotation in the specified Notebook writing at the same time the
     * Annotation's graph and the Annotation's items (if they exists). 
     * This is the main API that must be used to create new Annotations with data model version 2:
     * 
     * <pre>
     * {
     *      "graph": {
     *       // Annotation's Graph
     *       JSON/RDF
     *  },
     *      "items": {
     *       // Annotation's items
     *       JSON/RDF
     *      }
     *  }
     * </pre>
     * 
     * @param annotationData    the JSON data extracted from the payload
     * @param notebookID        the Notebook ID in which to put the annotation
     * @param annotationContext additional annotation metadata in JSON format
     * @param contentType       the contentType of the annotation data
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
    @POST
    @Path("{notebook-id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT})
    public Response createNewAnnotationWithGraphAndItems(String annotationData, @PathParam("notebook-id") String notebookID, @QueryParam(SemlibConstants.CONTEXT_PARAM) String annotationContext, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return super.createNewAnnotation(notebookID, annotationData, contentType, annotationContext);        
    }
    
    
    /**
     * Create a new Annotation in the specified Notebook writing only the
     * Annotation's graph. 
     * This is used to write Annotation with data model version 1 (old).
     *      
     * @param annotationData    the JSON data extracted from the payload
     * @param notebookID        the Notebook ID in which to put the annotation
     * @param annotationContext additional annotation metadata in JSON format
     * @param contentType       the contentType of the annotation data
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
    @POST
    @Path("graph/{notebook-id}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT})
    public Response createNewAnnotationWithGraph(String annotationData, @PathParam("notebook-id") String notebookID, @QueryParam(SemlibConstants.CONTEXT_PARAM) String annotationContext, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return super.createNewAnnotation(notebookID, annotationData, contentType, annotationContext);
    }
    
    
    /**
     * Get the list of all annotations contained within a Notebook with related metadata.
     * 
     * @param callback      the JSONP callback function or null
     * @param notebookID    the Notebook ID
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
    @GET
    @Path("{notebook-id}/annotations/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getAnnotationListAndMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {

        Status notebookIDStatus = super.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        return super.annotationsListAndMetadata(notebookID, callback, accepts, orderby, orderingMode, limit, offset);
    }
    
    
    /**
     * Get all data in JSON format about a specified Notebook: metadata, annotations list and related data.
     * The data output format is:<br/>
     * <pre>
     * {
     *      "metadata": {
     *          // JSON/RDF
     *          ... ... ...
     *      },
     *      "annotations": [
     *          {
     *              "metadata: {
     *                  // JSON/RDF
     *                  ... ... ...
     *              },
     *              "graph": {
     *                  // JSON/RDF
     *                  ... ... ...
     *              },
     *              "items": {
     *                  // JSON/RDF
     *                  ... ... ...
     *              }
     *          },
     *          {
     *              ... ... ...
     *          }
     *      ]
     * }
     * </pre>
     * 
     * If the specified Notebook does not contain annotations, this API return:<br/>
     * <pre>
     * {
     *      "metadata": {
     *          // JSON/RDF
     *          ... ... ...
     *      },
     *      "annotations": []
     * }      
     * 
     * @param callback      the JSONP callback function or null
     * @param notebookID    the Notebook ID
     * @param accepts       the accepted format
     * @param limit         max number of annotation to retrieve (-1 for no limits)
     * @param offset        offset for annotations retrieval (-1 for no offset)
     * 
     * @return              all data about a specified Notebook
     */
    @GET
    @Path("{notebook-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getDumpOfNotebook(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {
        
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        return super.dumpOfNotebook(notebookID, callback, accepts, limit, offset, orderingMode);
    }
    
    
    /**
     * Return all metadata about a specified Notbooks
     * 
     * @param callback      the JSONP callback function or null
     * @param notebookID    the Notebook ID
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
    @GET
    @Path("{notebook-id}/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getNotebookMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {

        Status notebookIDStatus = super.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        return super.notebookMetadata(notebookID, callback, accepts);        
    }

    
    /**
     * Get the RDF graph composed by all the triples of the annotations in the specified notebook.
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
    @GET
    @Path("{notebook-id}/graph")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getAllAnnotationsTriples(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        
        Status notebookIDStatus = super.checkNotebookID(notebookID, true);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }

        return super.annotationsTriples(notebookID, callback, accepts);
    }
    
    
    /**
     * Search for all annotation metadata according to specific parameters within a specified Notebook.
     * 
     * @param callback      callback function for JSONP
     * @param queryParams   parameters (JSON format), encoded
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * @param limit         max number of annotation to retrieve
     * @param offset        offset for annotations retrieval
     * @param orderby       property for order by
     * @param orderingMode  1/0 (desc/asc) for orderby
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("{notebook-id}/search")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response searchAnnotationMetadataInNotebooks(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @QueryParam(SemlibConstants.QUERY_PARAM) String queryParams, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {
        
        if (queryParams == null || queryParams.length() == 0) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();

            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }

                
        int qLimit   = UtilsManager.getInstance().parseLimitOrOffset(limit);
        int qOffset  = UtilsManager.getInstance().parseLimitOrOffset(offset);
        boolean desc = false;
                
        if (orderingMode != null && orderingMode.equals("1")) {
            desc = true;
        }

        Status notebookIDStatus = super.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        UtilsManager utilsManager = UtilsManager.getInstance();
        String cAccepts = utilsManager.getCorrectAcceptValue(callback, accepts);
        String tripleFormat = utilsManager.getCorrectTripleFormat(callback, accepts, cAccepts);
        
        List<String> notebookIDsList = new ArrayList<String>();
        notebookIDsList.add(notebookID);
        
        String annotationMetadata = null;
        try {
            annotationMetadata = RepositoryManager.getInstance().getCurrentRDFRepository().searchMetadataWithParameters(queryParams, qLimit, qOffset, orderby, desc, tripleFormat, notebookIDsList);
        } catch (RepositoryException ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        if (annotationMetadata != null) {            
            if (StringUtils.isBlank(annotationMetadata)) {
                return Response.status(Status.NO_CONTENT).build();
            } else {
                return super.createFinalResponseForNotebooksAPI(Response.Status.OK, callback, cAccepts, annotationMetadata, null);
            }
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }                
    }
    
    
    /**
     * Update metadata of an existing notebook. At the only supported metadata is NotebookName.
     * 
     * @param notebookMetadata  the notebook metadata (JSON). At the moment the only supported metadata is NotebookName
     * @param notebookID        the ID of the notebook to update
     * @return                  <ul>
     *                              <li>"200 OK" and the RDF graph as payload response</li>
     *                              <li>"204 No Content" if there are no triples in the current notebook</li>
     *                              <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                              <li>"403 Forbidden" if the current logged user has not the correct right to access the specified Notebook</li>
     *                              <li>"404 Not Found" if the specified notebooks do not exist</li>
     *                              <li>"500 Internal Server Error" in case of error</li>
     *                          </ul>
     */
    @PUT
    @Path("{notebook-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateNotebookName(String notebookMetadata, @PathParam("notebook-id") String notebookID) {
     
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);                
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }        
        
        String notebookName;
        try {
            JSONObject jsonObject = new JSONObject(notebookMetadata);
            notebookName = jsonObject.getString(SemlibConstants.JSON_NOTEBOOK_NAME);
            if (StringUtils.isBlank(notebookName)) {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        Notebook notebook = Notebook.getEmptyNotebookObject();
        notebook.setID(notebookID);
        notebook.setName(notebookName);
        notebook.setModifiedDate(UtilsManager.getInstance().getDate(true));
        
        Status response = RepositoryManager.getInstance().getCurrentRDFRepository().updateNotebookMetadata(notebook);
        
        if (response == Status.INTERNAL_SERVER_ERROR || response == Status.NOT_FOUND) {
            return Response.status(response).build();
        } else {
            return Response.ok().build();
        }
    }
    
    /**
     * Get all Notebooks owned by the current logged User
     * 
     * @param callback callback function for JSONP
     * @return         a list of all Notebooks owned by the current logged User in JSON format<br/>
     *                 HTTP Response Status Code:
     *                 <ul>
     *                  <li>"200 OK" in case of success</li>
     *                  <li>"204 No Content" if there are no active Notebooks for the current logged user</li>
     *                  <li>"400 Bad Request" if the request is not correct</li>
     *                  <li>"403 Forbidden" if no users is logged or if the current logged user has not the correct rights to access to this API</li>
     *                  <li>"500 Internal Server Error" for internal server error</li>
     *                 </ul>
     */
    @GET
    @Path("/owned")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response getAllNotebookOwnedByUser(@QueryParam(SemlibConstants.JSONP_PARAM) String callback) {
        return super.getNotebooksList(NotebookListType.OWNED_NOTEBOOKS, callback);
    }
    
    
    /**
     * Get all active Notebooks for the current logged User
     * 
     * @param callback callback function for JSONP
     * @return         a list of all active Notebooks in JSON format<br/>
     *                 HTTP Response Status Code:
     *                 <ul>
     *                  <li>"200 OK" in case of success</li>
     *                  <li>"204 No Content" if there are no active Notebooks for the current logged user</li>
     *                  <li>"400 Bad Request" if the request is not correct</li>
     *                  <li>"403 Forbidden" if no users is logged or if the current logged user has not the correct rights to access to this API</li>
     *                  <li>"500 Internal Server Error" for internal server error</li>
     *                 </ul>
     */
    @GET
    @Path("/active")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response getAllActiveNotebooks(@QueryParam(SemlibConstants.JSONP_PARAM) String callback) {
        return super.getNotebooksList(NotebookListType.ACTIVE_NOTEBOOKS, callback);
    }
    
    
    /**
     * Check if a specified Notebook is active or not.
     * 
     * @param callback      callback function for JSONP
     * @param notebookID    the ID of the Notebook to check
     * @param accepts       the HTTP accept header
     * @return              Return JSON data as payload response with the following format:<br>
     *                      <code>{ "NotebookActive": "0|1" }</code><br/><br/>
     *                      HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of succe</li>
     *                          <li>"400 Bad Request" if the request or the ID of the Notebook are not valid</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("/active/{notebook-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getActiveNotebookStatus(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }

        try {
            boolean notebookActive = RepositoryManager.getInstance().getCurrentDataRepository().isNotebookActive(currentLoggedUser.getUserID(), notebookID);
            
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put(SemlibConstants.JSON_NOTEBOOK_ACTIVE, (notebookActive) ? "1" : "0");
            
            String fResponse = jsonResponse.toString();
            String faccepts = MediaType.APPLICATION_JSON;
            
            return super.createFinalResponseForNotebooksAPI(Response.Status.OK, callback, faccepts, fResponse, null);
            
        } catch (Exception re) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Activate a specified Notebook.
     * 
     * @param notebookID    the ID of the Notebook to activate
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" if the request or the specified Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logget user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebook does not exist</li>
     *                          <li>"500 Internal Server Error" in case of internal error</li>
     *                      </ul>
     */
    @PUT
    @Path("/active/{notebook-id}")
    public Response activateNotebook(@PathParam("notebook-id") String notebookID) {

        Status notebookIDStatus = super.checkNotebookID(notebookID, false);
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
            
            Status notebookActivation = RepositoryManager.getInstance().getCurrentDataRepository().setNotebookActive(currentLoggedUser.getUserID(), notebookID, true);
           
            return Response.status(notebookActivation).build();
            
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }                
    }
    
    
    /**
     * Set a specified Notebook as "current". If antoher Notebooks is already set as current 
     * this API set it as "not current".
     * 
     * 
     * @param notebookID    The ID of the Notebook to set as current
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" if there quest and/or the Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logged user has not the correct rights to access the specified Notebook</li>
     *                          <li>"404 Not Found if the specified Notebook does not exist</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </u>
     */
    @PUT
    @Path("/current/{notebook-id}")
    public Response setNotebookAsCurrent(@PathParam("notebook-id") String notebookID) {
     
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
            
            Status operationStatus = RepositoryManager.getInstance().getCurrentDataRepository().setNotebookAsCurrent(currentLoggedUser.getUserID(), notebookID);
           
            return Response.status(operationStatus).build();
            
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }  
    }
    
    
    /**
     * Deactivate a specified Notebook. By default a Notebook set as current can not be deactivated. If a user try to 
     * deactivate a Notebook that is set as current, this API return a 403 HTTP error.
     * 
     * @param notebookID    the ID of the Notebook to deactivate
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" in case of success</li>
     *                          <li>"400 Bad Request" if the request or the specified Notebook ID is not correct</li>
     *                          <li>"403 Forbidden" if the current logget user has not the correct rights to access to the specified Notebook</li>
     *                          <li>"404 Not Found" if the specified Notebook does not exist</li>
     *                          <li>"500 Internal Server Error" in case of internal error</li>
     *                      </ul>
     */
    @DELETE
    @Path("/active/{notebook-id}")
    public Response deactiveteNotebook(@PathParam("notebook-id") String notebookID) {
        
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
            
            Status notebookActivation = RepositoryManager.getInstance().getCurrentDataRepository().setNotebookActive(currentLoggedUser.getUserID(), notebookID, false);
           
            return Response.status(notebookActivation).build();
            
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
    }
    
    
    /**
     * Delete a notebook and all annotation contained within it.
     * 
     * @param notebookID    the ID of the notebook to delete
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"204 No Content" if the notebook has correctly deleted</li>
     *                          <li>"403 Forbidde" if the user is not the owner of the specified Notebook or if the notebook is set as current</li>
     *                          <li>"404 Not Found" it the specified notebook does not exists</li>
     *                          <li>"400 Bad Request" if the notebook is not valid or is not specified</li>
     *                          <li>"500 Internal Server Error" in case of general problems</li>
     *                      </ul>
     */
    @DELETE
    @Path("{notebook-id}")
    public Response deleteNotebook(@PathParam("notebook-id") String notebookID) {        
        
        Status notebookStatus = super.checkNotebookID(notebookID, false);
        if ( notebookStatus != Status.OK) {
            return Response.status(notebookStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteNotebook(currentLoggedUser.getUserID(), notebookID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            } else {
                // Check if the Notebook is a current Notebook. If so, we can't delete it
                boolean isCurrentNotebook = RepositoryManager.getInstance().getCurrentDataRepository().isCurrentNotebook(notebookID);
                if (isCurrentNotebook) {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (RepositoryException re) {
            logger.log(Level.SEVERE, null, re);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        Notebook notebookToDelete = Notebook.getEmptyNotebookObject();
        notebookToDelete.setID(notebookID);
        
        Status notebookDeletion = RepositoryManager.getInstance().deleteNotebook(notebookToDelete);
        if (notebookDeletion == Status.OK) {
            return Response.status(Status.NO_CONTENT).build();
        } else {
            return Response.status(notebookDeletion).build();
        }
        
    }
            
        
    /**
     * Create a new notebook and set it as current for the current logged user.
     * 
     * @param currentLoggedUser the current logged User
     * @return                  the new Notebook ID or <code>null</code> if some problem has occurred during the Notebook creation
     */
    private String createCurrentNotebook(User currentLoggedUser) {
    
        Response response;
        if (currentLoggedUser.isAnonymous()) {
            String notebookData = "{ \"" + SemlibConstants.JSON_NOTEBOOK_NAME + "\": \"" + SemlibConstants.NOTEBOOK_PUBLIC + "\" }";
            response = createNewNotebook(notebookData);
        } else {
            response = createNewNotebook(null);
        }
            
        if (response.getStatus() == 201) {
                
            String entity = response.getEntity().toString();
                
            try {
                JSONObject nData = new JSONObject(entity);
                String notebookId = nData.getString(SemlibConstants.JSON_NOTEBOOK_ID);
                
                Status setCurrentResponse = RepositoryManager.getInstance().getCurrentDataRepository().setNotebookAsCurrent(currentLoggedUser.getUserID(), notebookId);                    
                if (setCurrentResponse == Status.OK) {
                    return notebookId;
                } else {
                    return null;
                }
                    
            } catch (Exception e) {
                return null;
            }                                
                
        } else {
            return null;
        }
    }
    
    
    /**
     * Create the final HTTP Response for the creation of a new Notebook.
     * 
     * @param notebookId    the ID of the newly created Notebook
     * @param status        the status code to return
     * @return              a Response (@see Response)
     */
    private Response createResponseForNewNotebook(String notebookId, Status status) {
        try {
            // Create JSON response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put(SemlibConstants.JSON_NOTEBOOK_ID, notebookId);
            
            String fResponse = jsonResponse.toString();
            String fContentType = MediaType.APPLICATION_JSON;
            
            return super.createFinalResponseForNotebooksAPI(status, null, fContentType, fResponse, Notebook.getURIFromID(notebookId));
                        
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();           
        } 
    }
    
    
    private Response setNotebookVisibility(String notebookID, boolean notebookPublic) {
        
        Status notebookIDStatus = super.checkNotebookID(notebookID, false);
        if (notebookIDStatus != Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {            
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
                        
            if (!permManager.canWriteNotebook(currentLoggedUser.getUserID(), notebookID)) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException se) {
            logger.log(Level.SEVERE, null, se);
            return Response.status(se.getStatusCode()).build();
        }

        Status status = RepositoryManager.getInstance().getCurrentDataRepository().setNotebookVisibility(notebookID, currentLoggedUser.getUserID(), notebookPublic);
        
        return Response.status(status).build();
    }        
}
