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
import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.RepositoryException;
import eu.semlibproject.annotationserver.security.PermissionsManager;
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

/**
 * Main class that implements all REST APIs under
 * the namespace "[our-webapps]/annotations/"
 * 
 * @author Michele Nucci
 */
@Path("/annotations")
public class AnnotationsAPI extends APIHelper {
    
    private Logger logger = Logger.getLogger(AnnotationsAPI.class.getName());
            
            
    /**
     * Default constructor
     * 
     * @param req the servlet's request passed by Jersy              
     * @param servletContext  the servlet context
     */
    public AnnotationsAPI(@Context HttpServletRequest req, @Context ServletContext servletContext) {                
        super(req, servletContext);
    }
    
    
    /**
     * Get all metadata associated to a specified annotation
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
    @GET
    @Path("{annotation-id}/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getAnnotationMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {                
        
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        return super.annotationMetadata(annotationID, callback, accept);
            
    }

         
    /** 
     * Get all the triples associated to a specific annotation
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
    @GET
    @Path("{annotation-id}/graph")
    @Produces({MediaType.APPLICATION_JAVASCRIPT, MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.TEXT_HTML})
    public Response getAnnotationTriples(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {
        
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        return super.annotationContent(annotationID, callback, accept);
    }
    
    
    /**
     * Get all data about a specified Annotation (metadata + content + items)
     * 
     * @return  All data about the specified Annotation in JSON/RDF format.<br/>
     *          HTTP Response Status Code:
     *          <ul>
     *              <li>"200 OK": in case of success</li>
     *              <li>"400 Bad Request": if the specified annotation ID is not valid</li>
     *              <li>"403 Forbidden": if the current logged user has not the correct right to access the specified annotation
     *              <li>"404 Not Found": if the speficied annotation ID does not exists</li>
     *              <li>"500 Internal Server Error": in case of error</li>
     *         </ul>
     *          
     */
    @GET
    @Path("{annotation-id}")
    @Produces({MediaType.APPLICATION_JAVASCRIPT,MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getAllAnnotationData(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {
        
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        try {
            String allAnnotationData = super.allAnnotationData(annotationID);
            if (StringUtils.isBlank(allAnnotationData)) {
                return Response.status(Status.NO_CONTENT).build();
            }
            
            // This is necessary to handle the GET request from normal browser
            String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);
            return super.createFinalResponseForAnnotationsAPI(callback, cAccepts, allAnnotationData);
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }        
    }
    
    
    /**
     * Search for all annotation metadata according to specific parameters
     * 
     * @param callback      callback function for JSONP
     * @param queryParams   parameters (JSON format), encoded
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * @param limit         max number of annotation to retrieve
     * @param offset        offset for annotations retrieval
     * @param orderby       property for order by
     * @param orderingMode  1/0 (desc/asc) for orderby
     * @param scope         possible values:
     *                      <ul>
     *                          <li>all: search for all annotations metadata in all active and public notebooks</li>
     *                          <li>active: search for all annotations metadata in all notebooks set as active<li>
     *                      </ul>
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>     
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("metadata/search")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response searchAnnotationMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @QueryParam(SemlibConstants.QUERY_PARAM) String queryParams, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode, @QueryParam(SemlibConstants.SCOPE_PARAM) String scope) {
    
        if (StringUtils.isBlank(queryParams)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        
        int qLimit   = UtilsManager.getInstance().parseLimitOrOffset(limit);
        int qOffset  = UtilsManager.getInstance().parseLimitOrOffset(offset);
        boolean desc = false;
                
        if (orderingMode != null && orderingMode.equalsIgnoreCase("1")) {
            desc = true;
        }

        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);        
        
        List<String> notebookIDsList = null;
        if (StringUtils.isBlank(scope)) {
            scope = SemlibConstants.ALL_PARAM_VALUE;
        }                
                
        if (scope.equalsIgnoreCase(SemlibConstants.ALL_PARAM_VALUE)) {
            try {
                notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getAllPublicAndOwnedNotebooks(currentLoggedUser.getUserID());
                if (notebookIDsList.isEmpty()) {
                    return Response.status(Status.NO_CONTENT).build();
                }
            } catch (RepositoryException re) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else if (scope.equalsIgnoreCase(SemlibConstants.ACTIVE_PARAM_VALUE)) {
            try {
                notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getActiveNotebooksForCurrentLoggedUser(currentLoggedUser.getUserID());
                if (notebookIDsList.isEmpty()) {
                    return Response.status(Status.NO_CONTENT).build();
                }
            } catch (RepositoryException re) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }        
        
        return super.searchAnnotationMetadata(notebookIDsList, queryParams, callback, cAccepts, qLimit, qOffset, orderby, desc);
    }
    
    
    /**
     * Return all Items associated to a given annotation
     * 
     * @param callback      callback function for JSONP
     * @param annotationID  a valid annotation ID
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */ 
    @GET
    @Path("{annotation-id}/items")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getAllAnnotationItems(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {
        
        // Check if the annotationID is valid
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }

        return super.annotationItems(annotationID, callback, accept);
    }

    
    /**
     * Search for all annotation's items basing on specific searching parameters
     * 
     * @param callback      callback function for JSONP
     * @param annotationID  a valid annotation ID
     * @param queryParams   parameters (JSON format), encoded
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" annotation metadata</li>
     *                          <li>"204 No Content" if there are no annotation metadata for the specified parameters</li>
     *                          <li>"400 Bad Request" if the specified parameters or the request are incorrect</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("{annotation-id}/items/search")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response searchAnnotationItems(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @QueryParam(SemlibConstants.QUERY_PARAM) String queryParams, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {

        if (StringUtils.isBlank(queryParams)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canReadAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        // Check if the annotationID is valid
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);        

        String annotationItems = null;
        try {
            
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }
            
            annotationItems = RepositoryManager.getInstance().getCurrentRDFRepository().searchAnnotationItems(annotationID, queryParams, triplesFormat);                    
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage().toString(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
        
        return super.createFinalResponseForAnnotationsAPI(callback, cAccepts, annotationItems);
    }
    
    
    /**
     * Add new triples to an existig annotation specified by the annotation ID.
     * 
     * @param annotationID  the ID of the annotation to which the triples will be added to
     * @param triplesData   the triples to add to the annotation 
     * @param contentType   the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    @POST
    @Path("{annotation-id}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    public Response addNewTripleToAnnotation(@PathParam("annotation-id") String annotationID, String triplesData, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return addTriplesToAnnotation(annotationID, triplesData, contentType, false);        
    }

    
    /**
     * Add new Items to an existig annotation specified by the annotation ID.
     * 
     * @param annotationID  the ID of the annotation to which the triples will be added to
     * @param itemsData     the triples to add to the annotation 
     * @param contentType   the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    @POST
    @Path("{annotation-id}/items")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    public Response addNewItemsToAnnotation (@PathParam("annotation-id") String annotationID, String itemsData, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return addItemsToAnnotation(annotationID, itemsData, contentType, false);
    }


    /**
     * Overwrite triples of an existig annotation specified by the annotation ID.
     * 
     * @param annotationID  the ID of the annotation to which the triples will be added to
     * @param triplesData   the triples to add to the annotation 
     * @param contentType   the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    @PUT
    @Path("{annotation-id}/content")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    public Response overwriteTripleAnnotation(@PathParam("annotation-id") String annotationID, String triplesData, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return addTriplesToAnnotation(annotationID, triplesData, contentType, true);
    }

    
    /**
     * Overwrite all Items of an existig annotation specified by the annotation ID.
     * 
     * @param annotationID  the ID of the annotation to which the triples will be added to
     * @param itemsData     the Items to add to the annotation 
     * @param contentType   the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    @PUT
    @Path("{annotation-id}/items")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    public Response overwriteAnnotationItems(@PathParam("annotation-id") String annotationID, String itemsData, @HeaderParam(SemlibConstants.HTTP_HEADER_CONTENT_TYPE) String contentType) {
        return addItemsToAnnotation(annotationID, itemsData, contentType, true);
    } 
    
    
    /**
     * Delete the annotation specified by the annotationID
     * 
     * @param annotationID  the ID of the annotation to delete
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"204 NoContent" if the annotation has correctly delete</li>
     *                          <li>"404 Not Found" it the specified annotation does not exists</li>
     *                          <li>"400 Bad Request" if the annotationID is not valid or is not specified</li>
     *                          <li>"500 Internal Server Error" in case of general problems</li>
     *                      </ul>
     */
    @DELETE
    @Path("{annotation-id}")
    public Response deleteAnnotation(@PathParam("annotation-id") String annotationID) {
        
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }        
        
        Status response = RepositoryManager.getInstance().deleteAnnotation(annotationID);
        if (response == Status.OK) {
            return Response.noContent().build();
        } else if (response == Status.NOT_FOUND || response == Status.INTERNAL_SERVER_ERROR) {
            return Response.status(response).build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
    }

    
    /**
     * Add or overwrite triples of an existig annotation specified by the annotation ID.
     * 
     * @param annotationID      the ID of the annotation to which the triples will be added to
     * @param triplesData       the triples to add to the annotation 
     * @param contentType       the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     * @param overwriteTriples  <code>true</code> clear existing triples
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    private Response addTriplesToAnnotation(String annotationID, String triplesData, String contentType, boolean overwriteTriples) {

        Response checkingResponse = this.checkAnnotationIDandData(annotationID, triplesData);
        if (checkingResponse != null) {
            return checkingResponse;
        }
        
        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }
        
        Annotation annotationObj = Annotation.getEmpyAnnotationObject();
        annotationObj.setID(annotationID);        
        annotationObj.setModifiedDate(UtilsManager.getInstance().getDate(true));                
        
        // set the annotation data
        try {            
            annotationObj.setAnnotationDataAsString(triplesData, contentType);
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        Status response = RepositoryManager.getInstance().getCurrentRDFRepository().addNewTriplesToAnnotation(annotationObj, overwriteTriples);
        return this.processResponseRequestForInputAPI(response);

    }
    
    
    /**
     * Add or overwrite items of an existig annotation specified by the annotation ID.
     * 
     * @param annotationID      the ID of the annotation to which the Items will be added to
     * @param itemsData         the itemsData to add to the annotation 
     * @param contentType       the content type of the triples data (application/json, application/rdf+xml, text/rdf+n3)
     * @param overwriteTriples  <code>true</code> clear existing triples    
     *  
     * @return              HTTP Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" if evrithing is OK</li>
     *                          <li>"400 Bad Request" if the specified annotation ID or annotation triple is not valid</li>
     *                          <li>"404 Not Found" if the specified annotation ID does not exists</li>
     *                          <li>"500 Internal Server Error" in case of problem (triplestore connection, bad data, etc)</li>
     *                      </ul>
     */
    private Response addItemsToAnnotation(String annotationID, String itemsData, String contentType, boolean overwriteTriples) {

        Response checkingResponse = this.checkAnnotationIDandData(annotationID, itemsData);
        if (checkingResponse != null) {
            return checkingResponse;
        }        

        User currentLoggedUser = super.getCurrentLoggedUserOrAnonUser();
        try {
            PermissionsManager permManager = eu.semlibproject.annotationserver.managers.SecurityManager.getInstance().getPermissionManager();
            
            boolean rightsOk = permManager.canWriteAnnotation(currentLoggedUser.getUserID(), annotationID);
            if (!rightsOk) {
                return Response.status(Status.FORBIDDEN).build();
            }
        } catch (eu.semlibproject.annotationserver.security.SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(ex.getStatusCode()).build();
        }

        Annotation annotationObj = Annotation.getEmpyAnnotationObject();
        annotationObj.setID(annotationID);
        annotationObj.setAnnotationDataItemsAsString(itemsData, contentType);        
        annotationObj.setModifiedDate(UtilsManager.getInstance().getDate(true));
        
        Status response = RepositoryManager.getInstance().getCurrentRDFRepository().addItemsToAnnotation(annotationObj, overwriteTriples);
        return this.processResponseRequestForInputAPI(response);        
    }
    
    
    /**
     * Check if the annotation's ID and annotation's data are correct. Is there is some 
     * problem in annotation's ID or annotation's data, this method return a Response object 
     * otherwise it returns <code>null</code>.
     * 
     * @param annotationID  the annotation's ID
     * @param data          the annotation's data to check
     * @return              <code>null</code> if everithing is OK or a Response if 
     *                      annotation's ID and/or annotation's data are not correct
     */
    private Response checkAnnotationIDandData(String annotationID, String data) {
        Status annotationIDStatus = super.checkAnnotationID(annotationID, false);
        if (annotationIDStatus != Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        
        // Check if the data coming from the client is ok
        if (StringUtils.isBlank(data)) {
            return Response.status(Status.BAD_REQUEST).build();            
        }        
        
        return null;
    }
    
            
    /**
     * Process the ResponseRequest object obtained from the input data methods
     * and generate the related HTTP Response
     * 
     * @param response  a Status object (@see Status)
     * @return          a Response (@see Response)
     */
    private Response processResponseRequestForInputAPI(Status response) {
        if (response == Status.OK) {
            return Response.ok().build();
        } else if (response == Status.NOT_FOUND || response == Status.BAD_REQUEST) {
            return Response.status(response).build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
}
