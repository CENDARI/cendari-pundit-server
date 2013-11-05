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
import java.util.List;
import javax.ws.rs.core.Response.Status;

/**
 * This abstract class defines generic methods that
 * all relational repository classes must be implement.
 * 
 * @author Michele Nucci
 */
public interface DataRepository {
            
        
    /**
     * Check if the specified Notebook is a current Notebook
     * 
     * @param notebookId    the ID of the Notebook to check
     * @return              <code>true</code> if the specified Notebooks is set as current
     * 
     * @throws              RepositoryException
     */
    public boolean isCurrentNotebook(String notebookId) throws RepositoryException;
    
    
    /**
     * Check if the specified Notebook is public or provate
     * 
     * @param notebookId    the ID of the Notebook to check
     * @return              <code>true</code> if the specified Notebook is public; <code>false</code> otherwise
     * 
     * @throws RepositoryException 
     */
    public boolean isNotebookPublic(String notebookId) throws RepositoryException;

    
    /**
     * Set the visbility flag for a specified Notebook
     * 
     * @param notebookId        the ID of the Notebook     
     * @param userId            the ID of the current logged User
     * @param notebookPublic    false: set the visibility as private;<br/>true: set the visbility as public
     * @return                  a Status (@see Status)
     */
    public Status setNotebookVisibility(String notebookId, String userId, boolean notebookPublic);
    
    
    /**
     * Get the current active notebook ID for the current logged user.
     * 
     * @param userID    the current logged user
     * @return          the current notebook ID as JSON data ({ "NotebookID": "[NOTEBOOK_ID]" }) or
     *                  <code>null</code> in case of problems.
     * 
     * @throws RepositoryException 
     */
    public String getCurrentNotebookID(String userID) throws RepositoryException;                                                               

        
    /**
     * Get the list of all Notebooks owned by a specified Users
     * 
     * @param userID    the ID of the current logged User
     * @return          a List<String> containing the ID of all Notebooks owned by the current logged user
     *                  or <code>null</code> in case of error/problem
     * @throws RepositoryException 
     */
    public List<String> getNotebooksOwnedByUser(String userID) throws RepositoryException;
    
   
    /**
     * Return a list with all Annotation's ID contained in a specified Notebooks
     * 
     * @param notebookID    a Notebook ID
     * @return              a List of Annotations ID
     */
    public List<String> getAllAnnotationsIDInNotebook(String notebookID) throws RepositoryException;
    
    /**
     * Get the list of all active Notebooks for the current logged user
     * 
     * @param userID    the ID of the current logged user
     * @return          a List<String> containing the ID of all active Notebooks for the current logged user
     *                  or <code>null</code> in case of error/problem
     * 
     * @throws RepositoryException 
     */
    public List<String> getActiveNotebooksForCurrentLoggedUser(String userID) throws RepositoryException; 
    
    
    /**
     * Get the list of all public Notebooks
     * 
     * @return  a List<String> containing the ID of all public Notebooks 
     *          or <code>null</code> in case of error/problem
     * 
     * @throws RepositoryException 
     */
    public List<String> getAllPublicNotebooks() throws RepositoryException;
    
    
    /**
     * Get the list of all public and owned Notebooks
     * 
     * @param   userId the ID of the current logged User
     * @return  a List<String> containing the ID of all public or owned Notebooks 
     *          or <code>null</code> in case of error/problem
     * 
     * @throws RepositoryException 
     */
    public List<String> getAllPublicAndOwnedNotebooks(String userId) throws RepositoryException;
    
    
    /**
     * Create a new notebook record into the main data storage
     * 
     * @param notebook   the Notebook object to write
     * @return           a Status (@see Status)
     */
    public Status createNotebookRecord(Notebook notebook);
    
    
    /**
     * Create a new annotation record into the main data storage
     * 
     * @param annotation    the Annotation object to write
     * @return              a Status (@see Status)
     */
    public Status createAnnotationRecord(Annotation annotation);
    
    
    /**
     * Set a Notebook as current and active
     * 
     * @param notebookId  the id of the Notebook to set as current
     * @param userid      the id of the User who want to set this Notebook as current
     * @return            a Status (@see Status)
     */
    public Status setNotebookAsCurrent(String userId, String notebookId);            
    
    
    /**
     * Check if a specified Notebook is currently marked as active for a given user
     * 
     * @param userId        the ID of the User
     * @param notebookID    the ID of the Notebook to check 
     * @return              <code>true</code> if the specified Notebook as currently marked as active
     * 
     * @throws RepositoryException 
     */
    public boolean isNotebookActive(String userId, String notebookID) throws RepositoryException;
    
    
    /**
     * Activate or deactivate a Notebook
     * 
     * @param userId        the id of the User who want to set this Notebook as active
     * @param notebookId    the id of the Notebook to activate or deactivate
     * @param active        <code>true</code> to activate the specified Notebook, 
     *                      <code>false</code> to deactivate the specified Notebook
     * @return              a Status (@see Status)
     */
    public Status setNotebookActive(String userId, String notebookId, boolean active);
    
    
    /**
     * Check if a Notebook identified by a notebookID exists.
     * 
     * @param notebookID            the Notebook ID
     * @return                      <code>true</code> if the notebook with the specified ID already
     *                              exists, <code>false</code> otherwise.
     * 
     * @throws                      RepositoryException 
     */
    public boolean notebookExists(String notebookID) throws RepositoryException;    
    
    
    /**
     * Check if a Notebook identified by a notebookID exists and it is public.
     * This is used by open APIs.
     * 
     * @param notebookID            the Notebook ID
     * @return                      <ul>
     *                                  <li>Status.OK: if the specified Notebooks exists and is public</li>
     *                                  <li>Status.NOT_FOUND: if the specified Notebooks does not exists</li>
     *                                  <li>Status.FORBIDDEN: if the specified Notebooks is not public</li>
     *                              </ul>
     * 
     * @throws RepositoryException 
     */
    public Status notebookExistsAndIsPublic(String notebookID) throws RepositoryException;
    
    
    /**
     * Check if an annotation identified by an annotationID exists.
     * 
     * @param annotationID          the Annotation ID
     * @return                      <code>true</code> if the annotation with the specified ID already
     *                              exists, <code>false</code> otherwise.
     * 
     * @throws RepositoryException 
     */
    public boolean annotationExists(String annotationID) throws RepositoryException;
    
    
    /**
     * Check if an annotation identified by an annotationID exists and if it is in a public Notebooks.
     * This is used by open APIs.
     * 
     * @param annotationID          the Annotation ID
     * @return                      <ul>
     *                                  <li>Status.OK: if the specified Annotation exists and is in a public Notebook</li>
     *                                  <li>Status.NOT_FOUND: if the specified Annotation does not exists</li>
     *                                  <li>Status.FORBIDDEN: if the specified Annotation is exists but it is in a private Notebook</li>
     *                                  <li>Status.BAD_REQUEST: if the specified Annotation ID is not valid</li>
     *                                  <li>Status.INTERNAL_SERVER_ERROR: in case of internal error</li>
     *                              </ul>
     */
    public Status annotationExistsAndIsInPublicNotebooks(String annotationID) throws RepositoryException;
    
    
    /**
     * Delete a notebook record from the main data storage
     * 
     * @param notebook          the Notebook object to write
     * @param autocommit        if <code>true</code> close the transaction as soon as possibile (after the record has been deleted; 
     *                          if <code>false</code> the transaction will be not closed end the user should commit it by "hand".
     * @return                  a Status (@see Status)
     */
    public Status deleteNotebookRecord(Notebook notebook, boolean autocommit);
 
    
    /**
     * Delete an annotation record from the main data storage
     * 
     * @param annotation        the annotation to write
     * @param autocommit        if <code>true</code> close the transaction as soon as possibile (after the record has been deleted; 
     *                          if <code>false</code> the transaction will be not closed end the user should commit it by "hand".
     * @return                  a Status (@see Status)
     */
    public Status deleteAnnotationRecord(Annotation annotation, boolean autocommit);          
}
