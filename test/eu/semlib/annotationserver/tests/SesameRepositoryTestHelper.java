/*
 *  File:    SesameRepository.java
 *  Created: 31-mag-2011
 */
package eu.semlib.annotationserver.tests;

import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.models.Annotation;
import eu.semlibproject.annotationserver.repository.OntologyHelper;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.http.HTTPTupleQuery;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.trix.TriXWriter;

/**
 *
 * @author Michele Nucci
 */
public class SesameRepositoryTestHelper {
    
    public static enum QueryResultsCheck {
        QUERY_OK,
        NO_CONTENT,
        ANNOTATION_EXISTS,
        ANNOTATION_BODY_EXISTS,
        ANNOTATION_DATA_EXISTS,
        ANNOTATION_MA_EXISTS
    }
    
    private static SesameRepositoryTestHelper instance = null;
    private HTTPRepository sesameHTTPRepository = null;
    
    
    private SesameRepositoryTestHelper() throws IOException, RepositoryException  {
        initRepository();
    }
    
    
    public static SesameRepositoryTestHelper getInstance() throws IOException, RepositoryException {
        
        if (instance == null) {
            instance = new SesameRepositoryTestHelper();
        }
        
        return instance;
    }
    
    private void initRepository() throws IOException, RepositoryException  {

        Properties configProperties = new Properties();
        configProperties.load(getClass().getResourceAsStream("../../../../tests_configuration.properties"));
        String repositoryURL = configProperties.getProperty("sesame.repositoryurl");
        String repositoryID = configProperties.getProperty("sesame.repositoryid");
        String usingAuthentication = configProperties.getProperty("sesame.useauthentication");
          
        // Creating the main HTTPRepository
        sesameHTTPRepository = new HTTPRepository(repositoryURL, repositoryID);

        // This string will be parsed only is sesame.useauthentication is YES (little optimization)
        String repositoryUsername = null;
        String repositoryPassword = null;

        if (usingAuthentication != null && usingAuthentication.equalsIgnoreCase("yes") || usingAuthentication.equals("1")) {
            repositoryUsername = configProperties.getProperty("sesame.username");
            repositoryPassword = configProperties.getProperty("sesame.password");

            sesameHTTPRepository.setUsernameAndPassword(repositoryUsername, repositoryPassword);
        }

        try {
            sesameHTTPRepository.initialize();
        } catch (Exception ex) {
            throw new RepositoryException(ex);
        }

    }
    
    public QueryResultsCheck annotationAndDataExists(String annotationID) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        
        String annotationURI = OntologyHelper.SWN_NAMESPACE + annotationID;        
        String query = "SELECT DISTINCT ?o ?g WHERE { <" + annotationURI + "> <http://www.openannotation.org/ns/hasBody> ?o . ?o <http://purl.org/swickynotes/ao/v1#graph> ?g }";
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
        ValueFactory factory = connection.getValueFactory();
        
        int statementsCounter = 0;
        int statementsBodyCounter = 0;
        int statementsGraphCounter = 0;
        
        RepositoryResult stats = connection.getStatements(factory.createURI(annotationURI), null, null, true);
        while (stats.hasNext()) {
            Object stat = stats.next();
            statementsCounter++;
        }
                        
        if (statementsCounter > 0) {
            String annBodyURI  = null;
            String annGraphURI = null;
            
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            TupleQueryResult graphs = tupleQuery.evaluate();
            
            while (graphs.hasNext()) {
                BindingSet bindingSet = graphs.next();
                Value bodyURI  = bindingSet.getValue("o");
                Value graphURI = bindingSet.getValue("g");
                
                if (bodyURI != null && graphURI != null) {
                    annBodyURI  = bodyURI.toString();
                    annGraphURI = graphURI.toString();
                    break;
                }
            }
            
            if (annBodyURI != null) {
                RepositoryResult results = connection.getStatements(factory.createURI(annBodyURI), null, null, true);
                while (results.hasNext()) {
                    Object co = results.next();
                    statementsBodyCounter++;
                }                
            }
            
            if (annGraphURI != null) {
                RepositoryResult results = connection.getStatements(null, null, null, true, factory.createURI(annGraphURI));
                while (results.hasNext()) {
                    Object co = results.next();
                    statementsGraphCounter++;
                }
            }
            
            connection.close();
            
            if (statementsBodyCounter > 0 && statementsGraphCounter > 0) {
                return QueryResultsCheck.ANNOTATION_MA_EXISTS;
            } else if (statementsBodyCounter > 0) {
                return QueryResultsCheck.ANNOTATION_BODY_EXISTS;
            } else if (statementsGraphCounter > 0) {
                return QueryResultsCheck.ANNOTATION_DATA_EXISTS;
            } else {
                return QueryResultsCheck.ANNOTATION_EXISTS;
            }
            
        } else {
            connection.close();
            return QueryResultsCheck.QUERY_OK;
        }
        
    }
    
    
    public String getAValidNotebookID() {
        
        try {
            
            String notebookID = null;
            String query = "SELECT ?s WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/pundit/ont/ao#Notebook> }";
                
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            TupleQueryResult notebooks = tupleQuery.evaluate();

            while (notebooks.hasNext()) {
                BindingSet bindingSet = notebooks.next();
                Value notebookURI = bindingSet.getValue("s");
                notebookID = notebookURI.toString();
                break;
            }

            // Extract the notebookID
            int lastIndexOf = notebookID.lastIndexOf("/");
            notebookID = notebookID.substring(lastIndexOf + 1, notebookID.length());

            connection.close();

            return notebookID;
                
        } catch (Exception ex) {
            return null;
        }

    }
    
    
    public String getAValidNotebookIDNotCurrent() {
        
        try {
            
            String notebookID = null;
            String query = "SELECT ?s WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/swickynotes/ao/v1#Notebook> }";
                
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            TupleQueryResult notebooks = tupleQuery.evaluate();

            while (notebooks.hasNext()) {
                BindingSet bindingSet = notebooks.next();
                Value notebookURI = bindingSet.getValue("s");
                notebookID = notebookURI.toString();
                
                if (StringUtils.isNotBlank(notebookID)) {
                    // Extract the notebookID
                    int lastIndexOf = notebookID.lastIndexOf("/");
                    notebookID = notebookID.substring(lastIndexOf + 1, notebookID.length());
                    
                    Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                    hSession.beginTransaction();
                    
                    Query query1 = hSession.createQuery("select count(*) from Currentnotebooks as cn where cn.currentnotebook = :notebookid");
                    query1.setParameter("notebookid", notebookID);
                    
                    int numOfRow = ((Long)query1.uniqueResult()).intValue();
                    
                    hSession.getTransaction().commit();
                    
                    if (numOfRow == 0) {
                        //found!
                        break;
                    }

                }
            }

            connection.close();

            return notebookID;
                
        } catch (Exception ex) {
            return null;
        }

    }
    
    
    public String getNotebookContainerForAnnotation(String annotationID) {
        
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            ValueFactory factory = connection.getValueFactory();
            String annotationURI = Annotation.getURIFromID(annotationID);
            String subject = null;
            
            RepositoryResult<Statement> results = connection.getStatements(null, factory.createURI("http://purl.org/pundit/ont/ao#includes"), factory.createURI(annotationURI), false);
            while(results.hasNext()) {
                Statement statement = results.next();
                if (statement != null) {
                    subject = statement.getSubject().toString();
                    break;
                }
            }
            
            connection.close();
            
            return subject;
            
        } catch (RepositoryException ex) {
            return null;
        }
    }
    
    public String getValidAnnotationID() {

        try {
            
            String annotationID = null;
            String query = "SELECT ?s WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.openannotation.org/ns/Annotation> }";
                
                RepositoryConnection connection = sesameHTTPRepository.getConnection();
                HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                TupleQueryResult annotations = tupleQuery.evaluate();
                
                while (annotations.hasNext()) {
                    BindingSet bindingSet = annotations.next();
                    Value notebookURI = bindingSet.getValue("s");
                    annotationID = notebookURI.toString();                                                     
                }
                
                // Extract the notebookID
                int lastIndexOf = annotationID.lastIndexOf("/");
                annotationID = annotationID.substring(lastIndexOf+1, annotationID.length());
                
                connection.close();
                
                return annotationID;
                
        } catch (Exception ex) {
            return null;
        }

    }
        
    public HTTPRepository getHTTPRepository() {
        return sesameHTTPRepository;
    }
    
    public boolean notebookExists(String notebookID) throws RepositoryException {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();

        ValueFactory factory = connection.getValueFactory();

        boolean result = connection.hasStatement(factory.createURI(OntologyHelper.SWN_NAMESPACE + notebookID), RDF.TYPE, factory.createURI(OntologyHelper.URI_SEMLIB_NOTEBOOK), true);

        connection.close();

        return result;
            
    }
    
    public void importTrixData(String data) throws RepositoryException, IOException, RDFParseException {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
        
        StringReader stringReader = new StringReader(data);
        connection.add(stringReader, null, RDFFormat.TRIX);
        connection.close();
        
    }
    
    
    public void importTrigData(String data) throws RepositoryException, IOException, RDFParseException {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
        
        StringReader stringReader = new StringReader(data);
        connection.add(stringReader, null, RDFFormat.TRIG);
        connection.close();        
        
    }
    
    public String exportAllTriples() {
        
        try {
            
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
                                    
            StringWriter stringWriter = new StringWriter();        
            TriXWriter rdfWriter = new TriXWriter(stringWriter);            
            
            connection.export(rdfWriter);            
            connection.close();
            
            return stringWriter.toString();
            
        } catch (Exception ex) {
            return null;
        } 
        
    }
    
    public boolean clearRepository() {
        try {
            RepositoryConnection connection = sesameHTTPRepository.getConnection();
            connection.clear();
            connection.close();
            return true;
        } catch (RepositoryException ex) {
            return false;
        }
    }
    
    
    public boolean annotationhasStatement(String annotationID, String object, String predicate, String literalobject) throws RepositoryException {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
        
        ValueFactory factory = connection.getValueFactory();
        
        String graph = OntologyHelper.SWN_NAMESPACE + "graph-" + annotationID;
        boolean result = connection.hasStatement(factory.createURI(object), factory.createURI(predicate), factory.createLiteral(literalobject), true, factory.createURI(graph));
        
        return result;
        
    }
    
    
    public String getNotebookName(String notebookID) throws Exception {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
        
        String notebookURI = OntologyHelper.SWN_NAMESPACE + notebookID;
        
        String query = "SELECT ?o WHERE { <" + notebookURI + "> <" + RDFS.LABEL.toString() + "> ?o }";
        
        HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
        TupleQueryResult name = tupleQuery.evaluate();
        
        String notebookName = null;
        if (name.hasNext()) {
            BindingSet bindingSet = name.next();
            Value vNotebookName = bindingSet.getValue("o");
            notebookName = vNotebookName.stringValue();
        }
        
        connection.close();
        
        return notebookName;        
    }
    
    
    public String getValidUserID(boolean getAnonymousUser) throws Exception {
        
        RepositoryConnection connection = sesameHTTPRepository.getConnection();
              
        String query = null;
        if (getAnonymousUser) {
            query = "SELECT ?s, ?name WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_FOAF_PERSON + "> . ?s <" + OntologyHelper.URI_FOAF_NAME + "> ?name }";
        } else {
            query = "SELECT ?s WHERE { ?s <" + OntologyHelper.URI_RDF_TYPE + "> <" + OntologyHelper.URI_FOAF_PERSON + "> } ";
        }       
        
        

        HTTPTupleQuery tupleQuery = (HTTPTupleQuery) connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setIncludeInferred(false);
        TupleQueryResult users = tupleQuery.evaluate();
        
        String userID = null;
        while (users.hasNext()) {
            BindingSet bindingSet = users.next();
            Value cUserID = bindingSet.getValue("s");
            
            if (getAnonymousUser) {
                Value cUserName = bindingSet.getValue("name");
                if (cUserName != null && cUserName.stringValue().equals(SemlibConstants.ANONYMOUS_FULLNAME)) {
                    userID = cUserID.stringValue();
                    break;
                }
            } else {
                userID = cUserID.stringValue();
                break;
            }
        }
                        
        connection.close();
        
        // now we have a full URI but we need only the useri ID -> extract it
        int index = userID.lastIndexOf("/");
        userID = userID.substring(index+1);
        
        return userID;

    }
    
}
