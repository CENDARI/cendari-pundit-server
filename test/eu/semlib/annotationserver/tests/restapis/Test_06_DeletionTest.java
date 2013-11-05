/*
 *  File:    Test_6_DeletionTest.java
 *  Created: 17-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper;
import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper.QueryResultsCheck;
import eu.semlib.annotationserver.tests.TestHelper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_06_DeletionTest {

    private static String serverAddress = null;
    private static String notebookID = null;
    private static String annotationID = null;
    private static String annotationJSON = "";

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting deletion tests...");

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);

        System.out.println("Getting a valid annotation ID for tests...");        
        
        notebookID = TestHelper.createNewNotebook(serverAddress);
        assertNotNull("Notebook ID is null!", notebookID);
        
        // Read annotation fixture JSON format
        try {
            annotationJSON = TestHelper.readFixture("test/fixture_annotations.json");
            if (annotationJSON == null || annotationJSON.equals("")) {
                fail("Problem loading fixture: fixture_annotations.json");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_annotations.json");
        }


        if (annotationID != null) {
            System.out.println("Valid annotation ID found: " + annotationID);
        } else {
            System.out.println("No annotation found into the current repository!");
            System.out.println("Loading annotation fixture...");
                         
            try {
                annotationID = TestHelper.loadAnnotationFixture(serverAddress, notebookID, annotationJSON, "application/json");
                if (annotationID == null) {
                    fail("Unable to load fixture!");
                }

                System.out.println("Annotation fixture loaded!");
                System.out.println("New annotation ID: " + annotationID);
            } catch (Exception e) {
                fail("Unable to load fixture into the triplestore during deletion tests");
            }

        }
    }

    
    @Test
    public void test_1_deleteSingleAnnotation() {
        
        System.out.println("Testing annotation deletion...");
        
        String errorMsg = "Unable to execute deletion tests! Some problems have occurred!";
        
        try {
            // HTTPunit does not provide a DeleteMethodWebRequest so we can use the DEL
            HttpClient client = new HttpClient();
            DeleteMethod delete = new DeleteMethod(serverAddress + "api/annotations/" + annotationID);
            int statusCode = client.executeMethod(delete);
            
            assertEquals("Response status code not correct", 204, statusCode);
            
            QueryResultsCheck results = SesameRepositoryTestHelper.getInstance().annotationAndDataExists(annotationID);
            if (results == QueryResultsCheck.ANNOTATION_MA_EXISTS) {
                fail("Annotation data+body+metadata exists!");
            } else if (results == QueryResultsCheck.ANNOTATION_BODY_EXISTS) {
                fail("Annotation body exists!");
            } else if (results == QueryResultsCheck.ANNOTATION_DATA_EXISTS) {
                fail("Annotation data graph exists!");
            }
            
            System.out.println("Annotation correctly deleted!");
            System.out.println("Reloading annotation fixtures...");
            
            annotationID = TestHelper.loadAnnotationFixture(serverAddress, notebookID, annotationJSON, "application/json");
            if (annotationID == null) {
                fail("Unable to load fixture!");
            }
                        
            System.out.println("Annotation fixture reloaded!");
            
        } catch (Exception ex) {
            fail(errorMsg);
        }

    }
    
    
    @Test
    public void test_2_deleteNotebook() {
        
        try {
            System.out.println("Testing notebook deletion...");
            
            String triplestoreDump = SesameRepositoryTestHelper.getInstance().exportAllTriples();
            assertNotNull("The triplestore is empty!", triplestoreDump);
            
            if (triplestoreDump.length() == 0) {
                fail("The triplestore is empty!");
            }
            
            HttpClient client   = new HttpClient();
            DeleteMethod delete = new DeleteMethod(serverAddress + "api/notebooks/" + notebookID);
            int statusCode      = client.executeMethod(delete);
            assertEquals("Status code not correct!", 204, statusCode);
            
            // Check if the notebook has benn really deleted
            boolean notebookExists = SesameRepositoryTestHelper.getInstance().notebookExists(notebookID);
            assertFalse("Notebook exists but should note exists!", notebookExists);
            
            System.out.println("Notebook deletion test ok!");
            
            reloadFixtureData(triplestoreDump);
            
        } catch (Exception ex) {
            fail("Unable to delete notebook!");
        }        
        
    }
    
    
    private void reloadFixtureData(String data) {
        System.out.println("Realoading fixture data into the triplestore...");
        try {
            SesameRepositoryTestHelper.getInstance().importTrixData(data);
            System.out.println("Fixture reloaded!");
        } catch (Exception ex) {
            System.err.println("Unable to realod fixture!");
        } 
    }
}
