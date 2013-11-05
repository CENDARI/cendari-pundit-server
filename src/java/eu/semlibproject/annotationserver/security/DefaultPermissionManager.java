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

import eu.semlibproject.annotationserver.hibernate.Notebooks;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.restapis.ServicesAPI;
import eu.semlibproject.annotationserver.security.SecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Default implementation of the PermissionManager
 * 
 * @author Michele Nucci
 */
public class DefaultPermissionManager implements PermissionsManager {

    private Logger logger = Logger.getLogger(DefaultPermissionManager.class.getName());
            
    
    /** 
     * {@inheritDoc}
     */
    public boolean isUserOwnerOfNotebook(String userId, String notebookId) throws SecurityException {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = hSession.createQuery("select count(*) from Notebooks as notebook where notebook.id = :notebookid AND notebook.ownerid = :userid");
            query.setParameter("notebookid", notebookId);
            query.setParameter("userid", userId);

            int numOfRow = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return (numOfRow > 0) ? true : false;

        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException(he.getMessage());
        }
    }

    
    /** 
     * {@inheritDoc}
     */
    public boolean isNotebookPublicOrOwnedByUser(String userId, String notebookId) throws SecurityException {
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();

        try {
            hSession.beginTransaction();
            Notebooks notebook = (Notebooks) hSession.get(Notebooks.class, notebookId);
            hSession.getTransaction().commit();

            if (notebook != null) {
                return (notebook.isPublic_() || userId.equals(notebook.getOwnerid())) ? true : false;
            } else {
                return false;
            }
        } catch (HibernateException he) {            
            throw new SecurityException(he.getMessage());
        }
    }

    
    /** 
     * {@inheritDoc}
     */    
    public boolean isUserOwnerOfAnnotation(String userId, String annotationId) throws SecurityException {                        
        try {            
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select annotation.notebooks from Annotations as annotation where annotation.id = :annotationId");
            query.setParameter("annotationId", annotationId);

            List<Notebooks> notebooks = query.list();
            
            hSession.getTransaction().commit();
            
            if (notebooks.size() > 0) {
                Notebooks cNotebook = notebooks.get(0);
                return (cNotebook != null && userId.equals(cNotebook.getOwnerid())) ? true : false; 
            } else {
                return false;
            }

        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException(he.getMessage());
        }
    }

    
    /** 
     * {@inheritDoc}
     */    
    public boolean canReadNotebook(String userId, String notebookId) throws SecurityException {
        try {
            
            boolean firstCheck = this.isNotebookPublicOrOwnedByUser(userId, notebookId);
            if (firstCheck) {
                return true;
            }
                        
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = this.getNotebookPermissionQuery(hSession, notebookId, userId);
            
            String permissions = (String)query.uniqueResult();
            
            hSession.getTransaction().commit();
            
            return this.checkReadPermission(permissions);
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException(he.getMessage());            
        }                
    }

    
    /** 
     * {@inheritDoc}
     */
    public boolean canWriteNotebook(String userId, String notebookId) throws SecurityException {
        try {
            boolean firstCheck = this.isUserOwnerOfNotebook(userId, notebookId);
            if (firstCheck) {
                return true;
            }            
            
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = this.getNotebookPermissionQuery(hSession, notebookId, userId);
            
            String permissions = (String)query.uniqueResult();
            
            hSession.getTransaction().commit();
            
            return this.checkWritePermission(permissions);
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException(he.getMessage());            
        }                
    }

    
    /** 
     * {@inheritDoc}
     */    
    public boolean canReadAnnotation(String userId, String annotationId) throws SecurityException {
        
        try {
            boolean firstCheck = this.hasUserRightsToAccessAnnotation(userId, annotationId);
            if (firstCheck) {
                return true;
            }
            
            String notebookId = this.getNotebookIdFromAnnotationId(annotationId, userId);                        
            return this.canReadNotebook(userId, notebookId);
            
        } catch (HibernateException hex) {
            logger.log(Level.SEVERE, null, hex);
            throw new SecurityException(hex.getMessage());     
        }
    }
        
    
    /** 
     * {@inheritDoc}
     */
    public boolean canWriteAnnotation(String userId, String annotationId) throws SecurityException {
        try {
            boolean firstCheck = this.isUserOwnerOfAnnotation(userId, annotationId);
            if (firstCheck) {
                return true;
            }            
                        
            String notebookId = this.getNotebookIdFromAnnotationId(annotationId, userId);                        
            return this.canReadNotebook(userId, notebookId);
                        
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException(he.getMessage());            
        }     
    }

    
    // ----- Private Methods -----
    
    private Query getNotebookPermissionQuery(Session hSession, String notebookId, String userId) {        
        Query query = hSession.createQuery("select permissions.permissions from Permissions as permissions where (permissions.notebooks.id = :nid AND permissions.usergroupid = :uid)");
        query.setParameter("nid", notebookId);
        query.setParameter("uid", userId);
        return query;
    }
        
    private Query getAnnotationPermissionQuery(Session hSession, String annotationId, String userId) {
        Query query = hSession.createQuery("select annotations.notebookid from Annotations as annotations where annotations.annotationid = :annotationid)");
        query.setParameter("annotationid", annotationId);
        return query;
    }
    
    private boolean checkReadPermission(String permissions) {
        if (StringUtils.isBlank(permissions)) {
            return false;
        } else if (permissions.length() >= 2 && (permissions.charAt(0) == 1 || permissions.charAt(1) == 1) ) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean checkWritePermission(String permissions) {
        if (StringUtils.isBlank(permissions)) {
            return false;
        } else if (permissions.length() >= 3 && (permissions.charAt(0) == 1 || permissions.charAt(2) == 1) ) {
            return true;
        } else {
            return false;
        }
    }
    
    private String getNotebookIdFromAnnotationId(String annotationId, String userId) throws SecurityException {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = this.getAnnotationPermissionQuery(hSession, annotationId, userId);

            String notebookId = (String) query.uniqueResult();

            hSession.getTransaction().commit();

            if (StringUtils.isBlank(notebookId)) {
                SecurityException secException = new SecurityException();
                secException.setStatusCode(Status.BAD_REQUEST);
                throw secException;
            }
            
            return notebookId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            throw new SecurityException();
        }
    }
    
    private boolean hasUserRightsToAccessAnnotation(String userId, String annotationId) throws HibernateException {        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.beginTransaction();
        
        Query query = hSession.createQuery("select annotation.notebooks from Annotations as annotation where annotation.id = :annotationId");
        query.setParameter("annotationId", annotationId);
        
        List<Notebooks> results = query.list();
        
        hSession.getTransaction().commit();

        if (results.size() > 0) {
            Notebooks notebook = results.get(0);
            String nOwner = notebook.getOwnerid();
            return (StringUtils.isNotBlank(nOwner) && (notebook.isPublic_() || userId.equals(nOwner))) ? true : false;

        } else {
            return false;
        }            
    } 
    
}
