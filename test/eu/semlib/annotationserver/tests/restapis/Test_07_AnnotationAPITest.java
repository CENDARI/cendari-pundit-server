/*
 *  File:    Test_7_AnnotationAPITest.java
 *  Created: 27-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.*;
import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author Michele Nucci
 */
public class Test_07_AnnotationAPITest {
    
    private static String serverAddress = null;
    private static String notebookID    = null;
    private static String annotationID  = null;
    
    
    public Test_07_AnnotationAPITest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting Annotation API tests...");
        
        
        System.out.println("Getting repository address...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Server address is null!", serverAddress);
        System.out.println("Server address: " + serverAddress);
        
        System.out.println("Getting a valid annotation ID for tests...");
    }

    
    @Test
    public void test_0_addNewTriples() {
        
        if (notebookID == null || annotationID == null) {
            try {
                this.getValidAnnotationID();
            } catch (Exception ex) {
                fail("Unable to retrieve a valid Notebook and Annotation ID!");
            }
        }
        
        String subject = "http://example.org/resource/8278";
        String predicate = "http://www.w3.org/2000/01/rdf-schema#comment";
        String object = "My Test Comment";
        
        String n3statement = "<" + subject + "> <" + predicate + "> \"" + object + "\" .";
        
        WebConversation wc = new WebConversation();
        WebRequest request = new PostMethodWebRequest(serverAddress + "api/annotations/" + annotationID,
                                        new ByteArrayInputStream(n3statement.getBytes()),
                                        "text/rdf+n3");
        
        try {
            WebResponse response = wc.getResource(request);
            assertNotNull("Response is null!", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response not correct!", 200, responseCode);
            
            // Check if the statement is really annonced
            boolean result = SesameRepositoryTestHelper.getInstance().annotationhasStatement(annotationID, subject, predicate, object);
            assertTrue("Triple does not exists!", result);
            
            
        } catch (RepositoryException ex) {
            fail("Unable to check statement into the repository");
        } catch (IOException ex) {
            fail("Unable to post new data to the annotation: " + annotationID);
        }
        
    }
    
    
    @Test
    public void test_1_overwriteTriples() {

        if (notebookID == null || annotationID == null) {
            try {
                this.getValidAnnotationID();
            } catch (Exception ex) {
                fail("Unable to retrieve a valid Notebook and Annotation ID!\n" + ex.getMessage());
            }           
        }
        
        String subject = "http://example.org/resource/8278";
        String predicate = "http://www.w3.org/2000/01/rdf-schema#comment";
        String oldObject = "My Test Comment";
        String newObject = "My new Test Comment";
        
        String n3statement = "<" + subject + "> <" + predicate + "> \"" + newObject + "\" .";
        
        WebConversation wc = new WebConversation();
        WebRequest request = new PutMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/content",
                                        new ByteArrayInputStream(n3statement.getBytes()),
                                        "text/rdf+n3");
        
        try {
            WebResponse response = wc.getResource(request);
            assertNotNull("Response is null!", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response not correct!", 200, responseCode);
            
            // Check if the statement is really annonced
            boolean result = SesameRepositoryTestHelper.getInstance().annotationhasStatement(annotationID, subject, predicate, newObject);
            assertTrue("New triple does not exists!", result);
            
            result = SesameRepositoryTestHelper.getInstance().annotationhasStatement(annotationID, subject, predicate, oldObject);
            assertFalse("Old triple already exists!", result);
            
        } catch (RepositoryException ex) {
            fail("Unable to check statement into the repository");
        } catch (IOException ex) {
            fail("Unable to post new data to the annotation: " + annotationID);
        }

    }
    
    
    @Test
    public void test_2_getAllAnnotationDataTest() {
        
        System.out.println("Starting test #2...");
        System.out.println("Getting all annotation data");
        
        
        WebConversation wc = new WebConversation();
        
        // 1. Get the list of all public Notebooks
        WebRequest notebooksRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/public");
        
        try {
            WebResponse notebooksResponse = wc.getResponse(notebooksRequest);
            assertNotNull("Response is null!", notebooksResponse);
            
            String responseContent = notebooksResponse.getText();
            assertNotNull("Response content is null!");
            
            JSONObject notebookList = new JSONObject(responseContent);
            JSONArray jsonNotebookList = notebookList.getJSONArray(SemlibConstants.JSON_NOTEBOOK_IDS);
            
            for (int i = 0; i < jsonNotebookList.length(); i++) {
                String cNotebookID = (String)jsonNotebookList.get(i);
                assertNotNull("The Notebook ID is null!", cNotebookID);
                
                WebRequest annListRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/" + cNotebookID + "/annotations/metadata");
                annListRequest.setHeaderField(SemlibConstants.HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON);
                
                WebResponse response = wc.getResponse(annListRequest);
                assertNotNull("Response is null!", response);
                
                int responseStatusCode = response.getResponseCode();
                if (responseStatusCode == 204) {
                    System.out.println("The Notebook '" + cNotebookID + "' is empty. Continue with the next one...");
                    continue;
                }
                
                String resContent = response.getText();
                assertNotNull("Response is null!", resContent);
                
                
                boolean continueWithNextNotebook = true;
                
                JSONObject jsonData = new JSONObject(resContent);
                Iterator iterator = jsonData.keys();                                
                
                while(iterator.hasNext()) {
                    String aid = (String)iterator.next();
                    aid = aid.substring(aid.lastIndexOf("/")+1);
                    if (aid != null) {
                        WebRequest annotationDataRequest = new GetMethodWebRequest(serverAddress + "api/annotations/" + aid);
                        annotationDataRequest.setHeaderField(SemlibConstants.HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON);
                        
                        WebResponse dataResponse = wc.getResponse(annotationDataRequest);
                        assertNotNull("Response is null!", dataResponse);
                        
                        int responseCode = dataResponse.getResponseCode();
                        if (responseCode == 204) {
                            System.out.println("No content for annotation " + aid);
                        } else {
                            String responseBody = dataResponse.getText();
                                                        
                            JSONObject data = new JSONObject(responseBody);
                            
                            System.out.println("Annotation Content:");
                            System.out.println(data.toString(2));

                            if (!data.has(SemlibConstants.JSON_METADATA) && !data.has(SemlibConstants.JSON_GRAPH)) {
                                System.out.println("Found a not valid annotations (non metadata and no content)! Annotation: " + aid);
                            } else {
                                if (data.has(SemlibConstants.JSON_ITEMS)) {
                                    continueWithNextNotebook = false;
                                    System.out.println("Found a valid Annotation (metadata+graph+items). Annotation ID: " + aid);
                                    System.out.println("Test #2: OK!");
                                } 
                            }                                                        
                        }
                    }
                }
                
                if (!continueWithNextNotebook) {
                    break;
                }
            }                        
            
        } catch (Exception e) {
            fail("Test #2 failed!\n" + e.getMessage());
        }
    }
    
    
    private void getValidAnnotationID() throws Exception {
        
        WebConversation wc = new WebConversation();
        
        WebRequest getNotebookIDRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
        getNotebookIDRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
        
        WebResponse response = wc.getResponse(getNotebookIDRequest);
        String jsonData = response.getText();
        System.out.println("JSONData:");
        System.out.println(jsonData);
        
        JSONObject data = new JSONObject(jsonData);
        notebookID = data.getString(SemlibConstants.JSON_NOTEBOOK_ID);        
        assertNotNull("NotebookID is null!", notebookID);
        System.out.println("Current Notebook ID: " + notebookID);        
        
        WebRequest annotationListRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata");
        annotationListRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
        
        response = wc.getResource(annotationListRequest);
        String dataList = response.getText();
        System.out.println("Annotation List Response:");
        System.out.println(dataList);
        
        JSONObject annList = new JSONObject(dataList);
        
        String aid = null;     
        Iterator iterator = annList.keys();
        while(iterator.hasNext()) {
            aid = (String) iterator.next();
            aid = aid.substring(aid.lastIndexOf("/")+1);
            if (aid != null) {
                annotationID = aid;
                break;
            }
        }
        
        System.out.println("Annotation ID found: " + aid);
    }

}
