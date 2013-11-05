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

import eu.semlibproject.annotationserver.hibernate.*;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.restapis.ServicesAPI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response.Status;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;


/**
 * Main singleton class that implement the connection to
 * relational repository.
 * 
 * @author Michele Nucci
 */
public class MySqlRepository implements DataRepository {

    private Logger logger = Logger.getLogger(MySqlRepository.class.getName());
    
    /**
     * Default constructor
     */
    public MySqlRepository() {
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getCurrentNotebookID(String userID) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {                    
            hSession.beginTransaction();
                        
            Query query = hSession.createQuery("select cNotebook.notebooks from Currentnotebooks cNotebook where cNotebook.userid = :userid");
            query.setParameter("userid", userID);
            
            List<Notebooks> results = query.list();
            
            hSession.getTransaction().commit();            
            
            if (results.size() > 0) {
                Notebooks notebook = results.get(0);
                return notebook.getId();                
            } else {
                return null;
            }
            
        } catch (HibernateException he) {                       
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
        
    }

    
    /**
     * {@inheritDoc}
     */
    public Status createNotebookRecord(Notebook notebook) {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {            
            hSession.beginTransaction();
        
            Notebooks nNotebook = new Notebooks();
            nNotebook.setId(notebook.getID());
            nNotebook.setOwnerid(notebook.getOwner().getUserID());
            nNotebook.setPublic_(ConfigManager.getInstance().isDefaultNotebookStatusPublic());
            
            Permissions nPermission = new Permissions();
            nPermission.setNotebooks(nNotebook);
            nPermission.setUsergroupid(notebook.getOwner().getUserID());
            nPermission.setPermissions("011");
            
            hSession.save(nNotebook);
            hSession.save(nPermission);
            hSession.getTransaction().commit();
            
            return Status.OK;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);            
            hSession.getTransaction().rollback();                               
            return Status.INTERNAL_SERVER_ERROR;
        } 
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public Status createAnnotationRecord(Annotation annotation) {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {            
            hSession.beginTransaction();
            
            Annotations nAnnotation = new Annotations();
            nAnnotation.setAnnotationid(annotation.getID());            
            Notebooks nNotebook = new Notebooks();
            nNotebook.setId(annotation.getNotebookID());            
            nAnnotation.setNotebooks(nNotebook);
                        
            hSession.save(nAnnotation);
            hSession.getTransaction().commit();
            
            return Status.OK;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            return Status.INTERNAL_SERVER_ERROR;            
        }
    }
    
            
    /**
     * {@inheritDoc}
     */
    public boolean notebookExists(String notebookID) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {                        
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select count(*) from Notebooks as notebook where notebook.id = :notebookid");
            query.setParameter("notebookid", notebookID);
            
            int numOfRow = ((Long)query.uniqueResult()).intValue();
            
            hSession.getTransaction().commit();
            
            return ( numOfRow > 0) ? true : false;
            
        } catch (HibernateException ex) {
            logger.log(Level.SEVERE, null, ex);            
            hSession.getTransaction().rollback();
            throw new RepositoryException();
        }
        
    }

    
    /**
     * {@inheritDoc}
     */        
    public Status notebookExistsAndIsPublic(String notebookID) throws RepositoryException {
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Notebooks notebook = (Notebooks)hSession.get(Notebooks.class, notebookID);
            
            hSession.getTransaction().commit();
            
            if (notebook == null) {
                return Status.NOT_FOUND;
            } else if (!notebook.isPublic_()) {
                return Status.FORBIDDEN;
            } else {
                return Status.OK;
            }
            
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public boolean annotationExists(String annotationID) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {          
            hSession.beginTransaction();

            Query query = hSession.createQuery("select count(*) from Annotations as annotation where annotation.id = :annotationID");
            query.setParameter("annotationID", annotationID);

            int numOfRow = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return (numOfRow > 0) ? true : false;

        } catch (HibernateException ex) {
            logger.log(Level.SEVERE, null, ex);
            hSession.getTransaction().rollback();
            throw new RepositoryException();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public Status annotationExistsAndIsInPublicNotebooks(String annotationID) throws RepositoryException {
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Annotations annotation = (Annotations)hSession.get(Annotations.class, annotationID);
            Notebooks notebook     = annotation.getNotebooks();
            boolean notebookPublic = notebook.isPublic_();
            
            hSession.getTransaction().commit();                                    
                    
            if (annotation == null) {
                return Status.NOT_FOUND;
            } else if (!notebookPublic) {
                return Status.FORBIDDEN;
            } else {
                return Status.OK;
            }
            
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isCurrentNotebook(String notebookId) throws RepositoryException {        
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select count(*) from Currentnotebooks as cNotebook where cNotebook.notebooks.id = :notebookid");
            query.setParameter("notebookid", notebookId);
            
            int numOfRow = ((Long)query.uniqueResult()).intValue();
            
            hSession.getTransaction().commit();
            
            return ( numOfRow > 0) ? true : false;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }

    
    /**
     * {@inheritDoc}
     */    
    public boolean isNotebookPublic(String notebookId) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Notebooks notebook = (Notebooks)hSession.get(Notebooks.class, notebookId);
            
            hSession.getTransaction().commit();
            
            return (notebook == null) ? false : notebook.isPublic_();                    
            
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status setNotebookAsCurrent(String userdId, String notebookId) {        
        // Check if already exists a Notebook set as Current            
        String currentNotebookID = null;
        try {
            currentNotebookID = this.getCurrentNotebookID(userdId);
        } catch (RepositoryException he) {
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;
        }

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {
            
            hSession.beginTransaction();
            
            if (currentNotebookID != null) {
                Notebooks cdNotebook = new Notebooks();                        
                cdNotebook.setId(currentNotebookID);
                Currentnotebooks currentNotebookRecordToDelete = new Currentnotebooks(userdId, cdNotebook);
                hSession.delete(currentNotebookRecordToDelete);
            }                                                

            Notebooks nNotebook = new Notebooks();
            nNotebook.setId(notebookId);

            Currentnotebooks cNotebooks = new Currentnotebooks(userdId, nNotebook);
            Activenotebooks aNotebooks  = new Activenotebooks(nNotebook, userdId);
            
            hSession.save(cNotebooks);
            
            // Check if the notebooks is already set as active. Bug Ticket #575            
            Query query = hSession.createQuery("select distinct anotebooks.id from Activenotebooks as anotebooks where (anotebooks.userid = :userid AND anotebooks.notebooks.id = :notebookid)");
            query.setParameter("userid", userdId);
            query.setParameter("notebookid", notebookId);
            Long recordId = (Long) query.uniqueResult();

            if (recordId == null) {
                hSession.save(aNotebooks);
            }                        
            
            hSession.getTransaction().commit();

            return Status.OK;

        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public Status setNotebookVisibility(String notebookId, String userId, boolean notebookPublic) {
                
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {
            
            hSession.beginTransaction();
            
            Notebooks notebook = (Notebooks)hSession.get(Notebooks.class, notebookId);
            if (notebook == null) {
                // Strange case at this point...but, anyway, it is better to check it
                hSession.getTransaction().commit();
                return Status.NOT_FOUND;
            }
            
            notebook.setPublic_(notebookPublic);            
            
            hSession.update(notebook);                        
            
            if (!notebookPublic) {
                // if the Notebook is set as Private check delete all
                // record about this Notebook in ActiveNotebooks table where
                // the userID is different by the Notebook owner                                                                
                Query query = hSession.createQuery("delete Activenotebooks where notebooks.id = :nid AND userid != :uid");
                query.setParameter("nid", notebookId);
                query.setParameter("uid", notebook.getOwnerid());
                query.executeUpdate();
            }
            
            hSession.getTransaction().commit();
            
            return Status.OK;
            
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;
        }
    }
        
    
    /**
     * {@inheritDoc}
     */    
    public List<String> getNotebooksOwnedByUser(String userID) throws RepositoryException {

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select distinct notebook.id From Notebooks as notebook where notebook.ownerid = :oid");
            query.setParameter("oid", userID);
            
            List<String> notebooksId = query.list();
            
            hSession.getTransaction().commit();
            
            return notebooksId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }
    
    
    /**
     * Return a list with all Annotation's ID contained in a specified Notebooks
     * 
     * @param notebookID    a Notebook ID
     * @return              a List of Annotations ID
     */
    public List<String> getAllAnnotationsIDInNotebook(String notebookID) throws RepositoryException {
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        
        try {
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select distinct annotationid from Annotations where notebookid = :nid");
            query.setParameter("nid", notebookID);
            
            List<String> annotationsId = query.list();
            
            hSession.getTransaction().commit();
            
            return annotationsId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());            
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public List<String> getActiveNotebooksForCurrentLoggedUser(String userID) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();

            Query query = hSession.createQuery("select distinct ans.notebooks.id from Activenotebooks as ans where ans.userid = :userid");
            query.setParameter("userid", userID);
            
            List<String> notebooksId = query.list();
            
            hSession.getTransaction().commit();
            
            return notebooksId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());
        }
    }
    
            
    /**
     * {@inheritDoc}
     */    
    public Status setNotebookActive(String userId, String notebookId, boolean active) {
        

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {            
            hSession.beginTransaction();
            
            Status returnedStatus;
            
            Notebooks notebook = new Notebooks();
            notebook.setId(notebookId);
            
            Activenotebooks activeNotebook = new Activenotebooks();
            activeNotebook.setUserid(userId);
            activeNotebook.setNotebooks(notebook);
            
            Query query = hSession.createQuery("select distinct anotebooks.id from Activenotebooks as anotebooks where (anotebooks.userid = :userid AND anotebooks.notebooks.id = :notebookid)");
            query.setParameter("userid", userId);
            query.setParameter("notebookid", notebookId);
            Long recordId = (Long) query.uniqueResult();
                
            if (active) {
                if (recordId != null) {
                    activeNotebook.setId(recordId);
                    hSession.update(activeNotebook);
                } else {
                    hSession.save(activeNotebook);
                }
                
                returnedStatus = Status.OK;                
            } else {                
                if (recordId == null) {
                    returnedStatus = Status.NOT_FOUND;
                } else {
                    activeNotebook.setId(recordId);                
                    hSession.delete(activeNotebook);                    
                    returnedStatus = Status.OK;
                }                
            }
            
            hSession.getTransaction().commit();
            
            return returnedStatus;
            
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;            
        }
    }
    
    
    /** 
     * {@inheritDoc}
     */        
    public boolean isNotebookActive(String userId, String notebookID) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);
        
        try {            
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select distinct anotebooks.notebooks.id from Activenotebooks as anotebooks where (anotebooks.userid = :userid AND anotebooks.notebooks.id = :notebookid)");
            query.setParameter("userid", userId);
            query.setParameter("notebookid", notebookID);
                                
            List<String> results = query.list();
            
            hSession.getTransaction().commit();
            
            return (results.size() > 0) ? true : false;
        
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());                        
        }
    }
    
        
    /**
     * {@inheritDoc}
     */    
    public List<String> getAllPublicNotebooks() throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select distinct id from Notebooks as ns where public = 1");
            
            List<String> notebooksId = query.list();
            
            hSession.getTransaction().commit();
            
            return notebooksId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage()); 
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public List<String> getAllPublicAndOwnedNotebooks(String userId) throws RepositoryException {
        
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {        
            hSession.beginTransaction();
            
            Query query = hSession.createQuery("select distinct id from Notebooks as ns where (public = 1 or ownerid = :oid)");
            query.setParameter("oid", userId);
            
            List<String> notebooksId = query.list();
            
            hSession.getTransaction().commit();
            
            return notebooksId;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, null, he);
            hSession.getTransaction().rollback();
            throw new RepositoryException(he.getMessage());             
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status deleteNotebookRecord(Notebook notebook, boolean autocommit) {                
        Notebooks nNotebook = new Notebooks();
        nNotebook.setId(notebook.getID());        

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {
            hSession.beginTransaction();

            hSession.delete(nNotebook);

            if (autocommit) {
                hSession.getTransaction().commit();
            }

            return Status.OK;

        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Status deleteAnnotationRecord(Annotation annotation, boolean autocommit) {                                    
        Annotations nAnnotation = new Annotations();
        nAnnotation.setAnnotationid(annotation.getID());

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
        hSession.getTransaction().setTimeout(10);

        try {
            hSession.beginTransaction();

            hSession.delete(nAnnotation);

            if (autocommit) {
                hSession.getTransaction().commit();
            }

            return Status.OK;

        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            logger.log(Level.SEVERE, null, he);
            return Status.INTERNAL_SERVER_ERROR;
        }
    }        
}
