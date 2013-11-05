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

package eu.semlibproject.annotationserver.security;


/**
 * Main interface for permissions management. All PermissionManager class must implement this Interface
 * 
 * @author Michele Nucci
 */
public interface PermissionsManager {
    
    /**
     * Check if a specified user is the owner of a specified notebook
     * 
     * @param userId        the id of the User
     * @param notebookId    the id of the Notebook to check
     * @return              <code>true</code> it the specified User is the Owner of the specified Notebook
     * @throws              SecurityException
     */
    public boolean isUserOwnerOfNotebook(String userId, String notebookId) throws SecurityException;
    
    
    /**
     * Check if the specified user is the owner of the specified Notebook or if the Notebook is public
     * 
     * @param userId        a user ID
     * @param notebookId    a notebook ID
     * @return              <code>true</code> is the user is the owner of the specified Notebook or the 
     *                      specified Notebook is public
     */
    public boolean isNotebookPublicOrOwnedByUser(String userId, String notebookId) throws SecurityException;

    
    /**
     * Check if a specified user if the owner of a specified annotation (the specified user
     * is the owner of the Notebook cointaining the specified annotation)
     * 
     * @param userId            the user to check
     * @param annotationId      the Annotation ID
     * @return                  <code>true</code> it the specified User is the Owner of the specified Notebook
     * @throws                  SecurityException
     */
    public boolean isUserOwnerOfAnnotation(String userId, String annotationId) throws SecurityException;

    
    /**
     * Check if the specified User can read the specified Notebooks
     * 
     * @param userId        the userId of the User to check
     * @param notebookId    a Notebook ID
     * @return              <code>true</code> if the specified User has the correct rights 
     *                      to read the specified Notebook
     * @throws              SecurityException
     */
    public boolean canReadNotebook(String userId, String notebookId) throws SecurityException;
    
    
    /**
     * Check if the specified User can write the specified Notebooks
     * 
     * @param userId       the userId of the User to check
     * @param notebookId   a Notebook ID
     * @return             <code>true</code> if the specified User has the correct rights 
     *                     to write the specified Notebook
     * @throws             SecurityException
     */
    public boolean canWriteNotebook(String userId, String notebookId) throws SecurityException;
    
    
    /**
     * 
     * Check if the specified User can read the specified Annotation
     * @param userId        the userId of the User to check
     * @param annotationId  an Annotation ID
     * @return              <code>true</code> if the specified User has the correct rights 
     *                      to read the specified Annotation
     * 
     * @throws SecurityException 
     */
    public boolean canReadAnnotation(String userId, String annotationId) throws SecurityException;
    
    /**
     * Check if the specified User can write the specified Annotation
     * 
     * @param userId        the userId of the User to check
     * @param annotationId  an Annotation ID
     * @return              <code>true</code> if the specified User has the correct rights 
     *                      to write the specified Annotation
     * @throws              SecurityException
     */
    public boolean canWriteAnnotation(String userId, String annotationId) throws SecurityException;
}
