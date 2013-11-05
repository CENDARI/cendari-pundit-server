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

import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import java.util.List;
import javax.ws.rs.core.Response.Status;


/**
 * This abstract class defines generic methods that
 * all RDF repository classes must be implement.
 * 
 * @author Michele Nucci
 */
public interface RDFRepository {
    
    /**
     * Get all metadata about the specified notebook
     * 
     * @param notebookID            the notebook ID
     * @param acceptedFormat        the format of the returned data (application/json, application/rdf+xml, application/rdf+n3)
     * @return                      all metadata about the specified Notebook
     * 
     * @throws RepositoryException 
     */
    public String getNotebookMetadata(String notebookID, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Get all metatada about the specified annotation
     * 
     * @param annotationID      the annotation ID
     * @param acceptedFormat    the format of the returned data (application/json, application/rdf+xml, application/rdf+n3)
     * @return                  all metadata about a specified annotation
     *
     * @throws RepositoryException 
     */
    public String getAnnotationMetadata(String annotationID, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Get all data about the specified annotation
     * 
     * @param annotationID      the specified annotation ID
     * @param acceptedFormat    the format of the returned data (application/json, application/rdf+xml, application/rdf+n3)
     * @return                  all data about a specified annotation
     * 
     * @throws RepositoryException 
     */
    public String getAnnotationTriples(String annotationID, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Get a list of Annotation ID contained in a specified Notebook. The ID of the annotations can be ordered by
     * creation date (ASC or DESC) and can be limited to return only a subset of all IDs. The output format is (JSON format):<br/>
     * 
     * <pre>
     * {
     *      "AnnotationIDs" : [
     *          ID1, ID2, ...
     *      ]
     * }
     * </pre>
     * 
     * @param notebookId    the notebook ID
     * @param limit         max number of ID to retrieve (-1 == all IDs)
     * @param offset        offset for annotations retrieval
     * @param desc          desc/asc for orderby. If <code>true</code> the IDs will be ordered in DESC mode
     * @return              a list of Annotation IDs
     * 
     * @throws RepositoryException 
     */
    public List<String> getAnnotationsIDsInNotebook(String notebookId, int limit, int offset, boolean desc) throws RepositoryException;

    
    /**
     * Get all annotations' items associated to a specific annotation
     * 
     * @param annotationID      a valid annotation ID
     * @param queryParam        a set of parameters encoded in JSON format
     * @param acceptedFormat    the accepted format for the returned data
     * @return                  annotations' item according to searching parameters
     * 
     * @throws RepositoryException
     */
    public String getAllAnnotationItems(String annotationID, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Search for all annotations' items basing on specific parameter
     * 
     * @param annotationID      a valid annotation ID
     * @param queryParam        a set of parameters encoded in JSON format
     * @param acceptedFormat    the accepted format for the returned data
     * @return                  annotations' item according to searching parameters
     * 
     * @throws RepositoryException
     */
    public String searchAnnotationItems(String annotationID, String queryParam, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Search for all annotations' metadata basing on specific parameters
     * 
     * @param jsonParams        a set of parameters encoded in JSON format
     * @param limit             max number of annotations to retrieve
     * @param offset            offset for annotations retrieval
     * @param orderBy           property for order by
     * @param desc              desc/asc for orderby
     * @param acceptedFormat    the accepted format for the returned data
     * @param notebookIDList    a list of notebook ID used to restrict the searching operation 
     *                          to a specific set of notebooks (e.g. current active notebook for
     *                          a given user). Set it to <code>null</code> if you want to perform
     *                          a global search (search in all notebooks).
     * @return                  annotations' metadata according to searching parameters
     * 
     * @throws RepositoryException 
     */
    public String searchMetadataWithParameters(String jsonParams, int limit, int offset, String orderBy, boolean desc, String acceptedFormat, List<String> notebookIDList) throws RepositoryException;
    
            
    /**
     * Check if the default public notebook exists
     * 
     * @return                      <code>true</code> if the public notebook already
     *                              exists, <code>false</code> otherwise.
     * @throws RepositoryException 
     */
    public boolean publicNotebookExists() throws RepositoryException;
    
                    
    /**
     * Check if a specified user exists
     * 
     * @param userID                the ID of the user to check
     * @return                      <code>true</code> if the user with the specified ID already
     *                              exists, <code>false</code> otherwise.
     * 
     * @throws RepositoryException 
     */    
    public boolean userExists(String userID) throws RepositoryException;
    
    
    /**
     * Create and write into the repository a new notebook and return a notebook ID
     * 
     * @param notebook   the Notebook object to write
     * @return           a Status (@see Status)
     */
    public Status writeNotebook(Notebook notebook);
    
    
    /**
     * Create and write into the repository a new annotation and return the annotation ID
     * 
     * @param annotation    the Annotation to write
     * @return              a Status (@see Status)
     * 
     * @throws              RepositoryException 
     */
    public Status writeAnnotation(Annotation annotation);
    
    
    /**
     * Add new triples to an existing annotation
     * 
     * @param annotation                the Annotation object thah identify the annotation 
     *                                  to which the new triples will be added to
     * @param clearExistingNamedGraph   if <code>true</code> empty the existing annotation's named graph
     * @return                          a Status (@see Status)
     */
    public Status addNewTriplesToAnnotation(Annotation annotation, boolean clearExistingNamedGraph);
    
    
    /**
     * Add new items data to an eisting annotation
     * 
     * @param annotation                the Annotation object thah identify the annotation to which the new Items will be added to
     * @param clearExistingNamedGraph   if <code>true</code> empty the existing annotation's Items named graph
     * @return                          a Status (@see Status)
     */
    public Status addItemsToAnnotation (Annotation annotation, boolean clearExistingNamedGraph);
    
    
    /**
     * Get the list of all annotations contained within a specified notebook with related metadata
     * 
     * @param notebook          a Notebook object
     * @param limit             max number of annotations to retrieve
     * @param offset            offset for annotations retrieval
     * @param orderBy           property for order by
     * @param desc              desc/asc for orderby
     * @param acceptedFormat    the accepted format for the returned data
     * @return                  a list of annotation contained within a notebook and the related metadata
     * 
     * @throws RepositoryException 
     */
    public String getNotebookAnnotationListAndMetadata(Notebook notebook, int limit, int offset, String orderBy, boolean desc, String acceptedFormat) throws RepositoryException;
    
    
    /**
     * Get all annotation triples in a given notebook.
     * 
     * @param notebookID        the notebook ID
     * @param acceptedFormats   the accepted format for the returned data
     * 
     * @return                  all annotation triples
     */
    public String getAllTriplesAnnotations(String notebookID, String acceptedFormats);
    
    
    /**
     * Delete an annotation
     * 
     * @param annotationID    the ID of the annotation to delete
     * 
     * @return                a Status (@see Status)
     */
    public Status deleteAnnotation(String annotationID);
    
    
    /**
     * Delete a notebook and all annotation contained within it. To optimize the
     * deletion process, this methods does not cyclically call the deleteAnnotation method.
     * 
     * @param notebookID                            the ID of the notebook to delete
     * @param queryForAllAnnotationBodyAndGraph     the optimized SPARQL query used to obtain all anntation 
     *                                              contained within the notebook and the related body and graph
     * @return                                      a Status (@see Status)
     */
    public Status deleteNotebook(String notebookID);
    
    
    /**
     * Update the notebook metadata (notebook name).
     * 
     * @param notebook  the notebook Object with updated data
     * @return          a Status (@see Status)
     */
    public Status updateNotebookMetadata(Notebook notebook);
    
    
    // Users management methods =======
    
    /**
     * Initialize or update user information into the repository.
     * 
     * @param user  the User object containing all users information
     * @return      <code>false</code> in case of problems/errors
     */
    public boolean initOrUpdateUserInfos(User user);
    
    
    /**
     * Get all information about a user from its ID
     * 
     * @param userID            the user ID
     * @param acceptedFormats   the accepted format for the returned data
     * @return                  all information about the specified user
     */
    public String getUserData(String userID, String acceptedFormats);
    
    
    /**
     * Return the full URL of the SPARQL end-point
     * 
     * @return the URL of the SPARQL end-point
     */
    public String getSPARQLEndPointUrl();
    
    
    // Administrations Methods =============    
    public List<String> getIDsOfNotebooksWithName(String notebookName, boolean desc, int limit, int offset) throws RepositoryException;
    public List<String> getIDsOfNotebooksWithOwnerName(String name, boolean desc, int limit, int offset) throws RepositoryException;
    public List<String> getIDsOfAnnotationsWithID(String id, boolean desc, int limit, int offset) throws RepositoryException;
    public List<String> getIDsofAnnotationsWithOwnerName(String name, boolean desc, int limit, int offset) throws RepositoryException;
}
