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
import eu.semlibproject.annotationserver.repository.RepositoryException;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;

/**
 * Main class that implements all REST APIs under
 * the namespace "[our-webapps]/open/"
 * 
 * @author Michele Nucci
 */
@Path("/open")
public class OpenAPI extends APIHelper {

    /**
     * Default constructor.
     * 
     * @param req               the servlet's request passed by the Jersy framework
     * @param servletContext    the servlet context
     */
    public OpenAPI(@Context HttpServletRequest req, @Context ServletContext servletContext) {        
        super(req, servletContext);
    }
    
    
    /**
     * Get all a list of all public Notebooks
     * 
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
    @Path("notebooks/public")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_JAVASCRIPT})
    public Response getAllActiveNotebooks(@QueryParam(SemlibConstants.JSONP_PARAM) String callback) {
        return super.getNotebooksList(NotebooksAPI.NotebookListType.PUBLIC_NOTEBOOKS, callback);
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
    @Path("notebooks/{notebook-id}/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML}) 
    public Response openGetNotebookMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {    
        Response.Status notebookIDStatus = super.checkNotebookID(notebookID, true);
        if (notebookIDStatus != Response.Status.OK) {
            return Response.status(notebookIDStatus).build();
        }

        return super.notebookMetadata(notebookID, callback, accepts);
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
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"404 Not Found" if the specified notebook does not exists</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */ 
    @GET
    @Path("notebooks/{notebook-id}/annotations/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response openGetAnnotationListAndMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {
        Response.Status notebookIDStatus = super.checkNotebookID(notebookID, true);
        if (notebookIDStatus != Response.Status.OK) {
            return Response.status(notebookIDStatus).build();
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
    @Path("notebooks/{notebook-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response openGetDumpOfNotebook(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {
        Response.Status notebookIDStatus = super.checkNotebookID(notebookID, true);
        if (notebookIDStatus != Response.Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        return super.dumpOfNotebook(notebookID, callback, accepts, limit, offset, orderingMode);
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
    @Path("notebooks/{notebook-id}/graph")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response openGetAllAnnotationsTriples(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("notebook-id") String notebookID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts) {
        Response.Status notebookIDStatus = super.checkNotebookID(notebookID, true);
        if (notebookIDStatus != Response.Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        return super.annotationsTriples(notebookID, callback, accepts);
    }

    
    /**
     * Sparql end-point for a specified notebook.
     * 
     * @param callback      the JSONP callback function or null
     * @param notebookID    the notebook ID
     * @param accepts       the accepted formats
     * @param query         the SPARQL query
     * @return              The SPARQL query restults. HSST Response Status Code:
     *                      <ul>
     *                          <li>"200 OK" and the RDF graph as payload response</li>
     *                          <li>"204 No Content" if there are no triples in the SPARQL enpoint related to the specified Notebooks</li>
     *                          <li>"400 Bad Request" if the notebook id is not specified or it is incorrect</li>
     *                          <li>"403 Forbidden" if the specified Notebooks is not public</li>
     *                          <li>"404 Not Found" if the specified notebook does not exists</li>
     *                          <li>"500 Internal Server Error" in case of error</li>
     *                      </ul>
     */
    @GET
    @Path("notebooks/{notebook-id}/sparql")    
    public Response notebooksSPARQLEndPoint(@PathParam("notebook-id") String notebookID, @QueryParam("query") String query, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accepts, @Context HttpServletRequest httpRequest) {
        Response.Status notebookIDStatus = super.checkNotebookID(notebookID, true);
        if (notebookIDStatus != Response.Status.OK) {
            return Response.status(notebookIDStatus).build();
        }
        
        if (StringUtils.isBlank(query)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
                
        List<String> annotationsIDs = null;                
        try {            
            annotationsIDs = RepositoryManager.getInstance().getCurrentDataRepository().getAllAnnotationsIDInNotebook(notebookID);
        } catch (RepositoryException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        if (annotationsIDs.isEmpty()) {
            return Response.status(Status.NO_CONTENT).build();
        }
        
        // This will also contain the query correctly processed to restrict it on a specified Notebook
        String finalParameters = super.prepareParametersForSPARQLEndPointQuery(query, annotationsIDs, httpRequest);                
        
        // Execute the query and return the related results
        return super.executeQueryOnSPARQLEndPoint(query, finalParameters, accepts);        
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
    @Path("annotations/{annotation-id}/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response openGetAnnotationMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {                        
        Response.Status annotationIDStatus = super.checkAnnotationID(annotationID, true);
        if (annotationIDStatus != Response.Status.OK) {
            return Response.status(annotationIDStatus).build();
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
    @Path("annotations/{annotation-id}/graph")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.TEXT_HTML})
    public Response openGetAnnotationTriples(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {        
        Response.Status annotationIDStatus = super.checkAnnotationID(annotationID, true);
        if (annotationIDStatus != Response.Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        return super.annotationContent(annotationID, callback, accept);
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
    @Path("annotations/{annotation-id}/items")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3, MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_HTML})
    public Response getAllAnnotationItems(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("annotation-id") String annotationID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {        
        // Check if the annotationID is valid
        Response.Status annotationIDStatus = super.checkAnnotationID(annotationID, true);
        if (annotationIDStatus != Response.Status.OK) {
            return Response.status(annotationIDStatus).build();
        }
        return super.annotationItems(annotationID, callback, accept);
    }
    
    
    /**
     * Search for all annotation metadata according to specific parameters in all public Notebooks
     * 
     * @param callback      callback function for JSONP
     * @param queryParams   parameters (JSON format), encoded
     * @param accept        the accepted format (n3, application/rdf+xml or application/json)
     * @param limit         max number of annotation to retrieve
     * @param offset        offset for annotations retrieval
     * @param orderby       property for order by
     * @param orderingMode  1/0 (desc/asc) for orderby
     * 
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
    public Response searchAnnotationMetadata(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @QueryParam(SemlibConstants.QUERY_PARAM) String queryParams, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept, @QueryParam(SemlibConstants.LIMIT_PARAM) String limit, @QueryParam(SemlibConstants.OFFSET_PARAM) String offset, @QueryParam(SemlibConstants.ORDERBY_PARAM) String orderby, @QueryParam(SemlibConstants.DESC_PARAM) String orderingMode) {
        
        if (StringUtils.isBlank(queryParams)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        int qLimit   = UtilsManager.getInstance().parseLimitOrOffset(limit);
        int qOffset  = UtilsManager.getInstance().parseLimitOrOffset(offset);
        boolean desc = false;
                
        if (orderingMode != null && orderingMode.equalsIgnoreCase("1")) {
            desc = true;
        }

        // This is necessary to handle the GET request from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);        
        
        // Get the list if all public Notebooks
        List<String> notebookIDsList = null;
        try {
            notebookIDsList = RepositoryManager.getInstance().getCurrentDataRepository().getAllPublicNotebooks();
            if (notebookIDsList.isEmpty()) {
                return Response.status(Status.NO_CONTENT).build();
            }
        } catch (RepositoryException ex) {            
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return super.searchAnnotationMetadata(notebookIDsList, queryParams, callback, cAccepts, qLimit, qOffset, orderby, desc);
    }
}
