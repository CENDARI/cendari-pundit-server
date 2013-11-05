
package eu.semlibproject.annotationserver.managers;

import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.DataRepository;
import eu.semlibproject.annotationserver.repository.MySqlRepository;
import eu.semlibproject.annotationserver.repository.RDFRepository;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * The main class that manage and return the current active repository.
 * The repository used by the AnnotationServer can be configured within the
 * web.xml file. By default the AnnotationServer realies on Sesame and provides
 * a SesameRepository classe to work with it.<br/> 
 * 
 * It is possibile to implement the support for other kind of repositores 
 * developing a custom class, that must have these requirements:
 * <ol>
 *  <li>the custom class must have a default constructor (with no parameters)</li>
 *  <li>the custom class must implement the interface Repository</li>
 *  <li>there is the need to implement custom Model (@see ModelsManager)</li>
 * </ol>
 * 
 * @author Michele Nucci
 */
public class RepositoryManager {
    
    private static RepositoryManager instance = null;
    
    private static RDFRepository  currentRDFRepositoryInstance;
    private static DataRepository currentDataRepositoryInstance;
    
    private final String errorMessage  = "Check the application configuration in web.xml file.";
    private final String errorMessage2 = "The repositorie class specified in your web.xml file is not valid, it was not possible to instanciate it or some errors have occurred while instaciating it.";

    private Logger logger = Logger.getLogger(RepositoryManager.class.getName());
    
    
    private RepositoryManager() {
        
    }
    
    
    /**
     * Get the shared instance of the RepositoryManager
     * 
     * @return the shared instance of the RepositoryManager
     */
    public static synchronized RepositoryManager getInstance() {
        
        if (instance == null) {
            instance = new RepositoryManager();
        }
        
        return instance;
        
    }
    
    
    /**
     * Get the current RDF repository for the AnnotationManager
     * 
     * @return an instance of the RDF repository configured for this AnnotationManager
     */
    public RDFRepository getCurrentRDFRepository() {
        
        if (currentRDFRepositoryInstance == null) {
                                
            // Use the reflection to instance the current repository
            String repositoryClassName = ConfigManager.getInstance().getCurrentRDFRepositoryClassName();
            if (repositoryClassName == null) {
                logger.log(Level.SEVERE, "Class name for repository is null. " + errorMessage);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            
            Object myRepositoryClass = UtilsManager.getInstance().createNewInstanceOfClass(repositoryClassName);
            if (myRepositoryClass instanceof RDFRepository) {
                currentRDFRepositoryInstance = (RDFRepository) myRepositoryClass;
            } else {
                logger.log(Level.SEVERE, errorMessage2);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
                        
        }
        
        return currentRDFRepositoryInstance;
        
    }
    
    
    /**
     * Get the current relational data repository for the AnnotationManager
     * 
     * @return an instance of the relational data repository configured for this AnnotationManager
     */    
    public DataRepository getCurrentDataRepository() {
        
        if (currentDataRepositoryInstance == null) {
            // Use the reflection to instance the current repository
            String repositoryClassName = ConfigManager.getInstance().getCurrentDataRepositoryClassName();
            if (repositoryClassName == null) {
                logger.log(Level.SEVERE, "Class name for data repository is null. " + errorMessage);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            
            Object myRepositoryClass = UtilsManager.getInstance().createNewInstanceOfClass(repositoryClassName);
            if (myRepositoryClass instanceof MySqlRepository) {
                currentDataRepositoryInstance = (MySqlRepository) myRepositoryClass;
            } else {
                logger.log(Level.SEVERE, errorMessage2);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }            
        }
        
        return currentDataRepositoryInstance;
    }
    
    
    /**
     * Write a new Notebook object into the main storage (RDF + relational database) emulating a single transaction
     * 
     * @param notebook              the Notebook to write     
     * @param currentLoggedUser     the current logged user
     * @return                      a Status (@see Status)
     */
    public Status writeNotebook(User currentLoggedUser, Notebook notebook) {
        
        Status response = this.getCurrentDataRepository().createNotebookRecord(notebook);
        if (response == Status.OK) {
            Status responseWritingNotebookInRDFStorgae = this.getCurrentRDFRepository().writeNotebook(notebook);
            
            if (responseWritingNotebookInRDFStorgae != Status.OK) {
                this.getCurrentDataRepository().deleteNotebookRecord(notebook, true);
                return Status.INTERNAL_SERVER_ERROR;
            }
            
            return Status.OK;
        }
        
        return Status.INTERNAL_SERVER_ERROR;
    }

    
    /**
     * Write a new Annotation object into the main storage (RDF + relational database) emulating a single transaction
     * 
     * @param notebook              the Notebook to write     
     * @param currentLoggedUser     the current logged user
     * @return                      a Status (@see Status)
     */
    public Status writeAnnotation(User currentLoggedUser, Annotation annotation) {
        
        Status response = this.getCurrentDataRepository().createAnnotationRecord(annotation);
        if (response == Status.OK) {
            Status responseWritingAnnotationInRDFStorage = this.getCurrentRDFRepository().writeAnnotation(annotation);
            if (responseWritingAnnotationInRDFStorage != Status.OK) {
                this.getCurrentDataRepository().deleteAnnotationRecord(annotation, true);
                return Status.INTERNAL_SERVER_ERROR;
            }
            
            return Status.OK;
        }
        
        return Status.INTERNAL_SERVER_ERROR;
    }
    
    
    /**
     * Delete a Notebook (data storage + RDF storage) emulating a single transaction
     * 
     * @param notebook              the Notebook to delete
     * @return                      a Status (@see Status)
     */
    public Status deleteNotebook(Notebook notebook) {
        
        // 1. Try to delete a Notebook record without closing the transaction (autocommit = false)
        Status rdbRecordDeletionStatus = RepositoryManager.getInstance().getCurrentDataRepository().deleteNotebookRecord(notebook, false);
        if (rdbRecordDeletionStatus == Status.OK) {
            
            // 2. Delete the Notebook RDF data
            Status rdfNotebookDeletionStatus = RepositoryManager.getInstance().getCurrentRDFRepository().deleteNotebook(notebook.getID());
            if (rdfNotebookDeletionStatus == Status.OK) {
                
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                
                // 3. RDF data deletion -> OK + Data deletion OK --> commit transaction
                hSession.getTransaction().commit();
                return Status.OK;
                
            } else {
                
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                
                // 3. RDF data deletion -> filed Data deletion OK --> rollback data deletion
                hSession.getTransaction().rollback();
                return Status.INTERNAL_SERVER_ERROR;
                
            }
            
        } else {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            if (hSession.isOpen()) {
                Transaction transaction = hSession.getTransaction();
                if (transaction != null && transaction.isActive() && !transaction.wasCommitted()) {
                    transaction.commit();
                }
            }
            return rdbRecordDeletionStatus;
        }
    }
    
    
    /**
     * Delete an Annotation (data storage + RDF storage) emulating a single transaction
     *
     * @param annotationId          the ID of the Annotation to delete
     * @return                      a Status (@see Status)
     */
    public Status deleteAnnotation(String annotationId) {
        
        Annotation nAnnotation = Annotation.getEmpyAnnotationObject();
        nAnnotation.setID(annotationId);
                        
        // 1. Try to delete the Annotation record without closing the transaction (autocommit = false)        
        Status rdbRecordDeletionStatus = RepositoryManager.getInstance().getCurrentDataRepository().deleteAnnotationRecord(nAnnotation, false);
                        
        if (rdbRecordDeletionStatus == Status.OK) {
            // 2. Delete the Annotation RDF data
            Status rdfAnnotationDeletionStatus = RepositoryManager.getInstance().getCurrentRDFRepository().deleteAnnotation(annotationId);
            if (rdfAnnotationDeletionStatus == Status.OK) {
                
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                
                // 3. RDF data deletion -> OK + Data deletion OK --> commit transaction
                hSession.getTransaction().commit();
                return Status.OK;
                
            } else {
                
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                
                // 3. RDF data deletion -> filed Data deletion OK --> rollback data deletion
                hSession.getTransaction().rollback();
                return Status.INTERNAL_SERVER_ERROR;
                
            }
        } else {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            if (hSession.isOpen()) {
                Transaction transaction = hSession.getTransaction();
                if (transaction != null && transaction.isActive() && !transaction.wasCommitted()) {
                    transaction.commit();
                }
            }
            return rdbRecordDeletionStatus;
        }
    }
    
    
    /**
     * This is only an helper method to clear all table into the RDBMS repository. 
     * 
     * @return <code>true</code> if the operation has been executed whit success
     */
    public boolean clearAllDataStorageTables() {
        
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            
            String strQuery1 = "DELETE FROM notebooks";
            String strQuery2 = "DELETE FROM activenotebooks";
            String strQuery3 = "DELETE FROM currentnotebooks";
            String strQuery4 = "DELETE FROM annotations";
            String strQuery5 = "DELETE FROM permissions";
            
            Query query1 = hSession.createSQLQuery(strQuery1);
            Query query2 = hSession.createSQLQuery(strQuery2);
            Query query3 = hSession.createSQLQuery(strQuery3);
            Query query4 = hSession.createSQLQuery(strQuery4);
            Query query5 = hSession.createSQLQuery(strQuery5);
            
            query1.executeUpdate();
            query2.executeUpdate();
            query3.executeUpdate();
            query4.executeUpdate();
            query5.executeUpdate();
            
            hSession.getTransaction().commit();
            
            return true;
            
        } catch (HibernateException he) {
            logger.log(Level.SEVERE, he.getMessage());
            return false;
        }
        
    }
    
}
