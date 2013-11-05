/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_12_OpenAPITest {

    private static String serverAddress = null;
    
    public Test_12_OpenAPITest() {
        
    }
    
    @BeforeClass
    public static void setUpClassForTest() {
        System.out.println("Starting testing Open API...");        

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
    }
    
    @Test
    public void test_1_publicDataTest() {
        try {
            System.out.println(">>> Test #01 ========");
            System.out.println("Testing: get public Notebooks list"); 
            
            WebConversation wc = new WebConversation();
            
            WebRequest notebookListRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/public");
            notebookListRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            WebResponse response = wc.getResponse(notebookListRequest);
            assertNotNull("Response is null!", response);
            
            String strNotebookList = response.getText();
            assertNotNull("The public Notebook list is null!", strNotebookList);
            
            System.out.println("Public Notebook List:");
            System.out.println(strNotebookList);
            
            JSONObject jsonNotebookList = new JSONObject(strNotebookList);
            JSONArray annList = jsonNotebookList.getJSONArray(SemlibConstants.JSON_NOTEBOOK_IDS);
            assertNotNull("AnnotationList is null!", annList);
            
            String notebookID        = null;
            String annotationID      = null;
            String strAnnotationList = null;
            
            for (int i = 0; i < annList.length(); i++) {
                notebookID = jsonNotebookList.getJSONArray(SemlibConstants.JSON_NOTEBOOK_IDS).getString(i);
                assertNotNull("The Notebook ID is null!", notebookID);

                // Try to get Notebook Metadata
                System.out.println("Try to get metadata for public Notebook " + notebookID);
            
                WebRequest metadataRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/" + notebookID + "/annotations/metadata");
                metadataRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
                response = wc.getResponse(metadataRequest);
                assertNotNull("Response is null!", response);
            
                String strNotebookMetadata = response.getText();
                assertNotNull("Metadata for Notebook " + notebookID + " is null!", strNotebookMetadata);
            
                System.out.println("Notebook Metadata:");
                System.out.println(strNotebookMetadata);

                // try to get annotation list and metadata for the specified notebook
                WebRequest annotationList = new GetMethodWebRequest(serverAddress + "api/open/notebooks/" + notebookID + "/annotations/metadata");
                annotationList.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
                response = wc.getResponse(annotationList);
                assertNotNull("Response is null!", response);

                try {
                    strAnnotationList = response.getText();
                    
                    if (StringUtils.isBlank(strAnnotationList)) {
                        continue;
                    }
                                        
                    System.out.println("Annotation List for Notebook " + notebookID + ":");
                    System.out.println(strAnnotationList);
                    break;                    
                } catch(Exception e) {
                    continue;
                }
            }

            JSONObject jsonAnnotationList = new JSONObject(strAnnotationList);
            assertNotNull("JSONAnnotationList is null!", jsonAnnotationList);
            Iterator iterator = jsonAnnotationList.keys();
            while (iterator.hasNext()) {
                String strResource = (String) iterator.next();
                System.out.println("Resource: " + strResource);
                assertNotNull("Resource URI is null!", strResource);
                
                annotationID = strResource.substring(strResource.lastIndexOf("/") + 1);
                if (annotationID != null) {
                    System.out.println("Annotation ID: " + annotationID);
                    break;
                }                                 
            }
            
            
            // Try to the Notebook Graph            
            WebRequest graphRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/" + notebookID + "/graph");
            graphRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            response = wc.getResponse(graphRequest);
            assertNotNull("Response is null!", response);
            
            String strNotebookGraph = response.getText();
            assertNotNull("The graph for the Notebook " + notebookID + " is null!", strNotebookGraph);
            
            System.out.println("Graph for Notebook " + notebookID +":");
            System.out.println(strNotebookGraph);
            
            
            // Getting annotation metadata
            WebRequest annotationMetadataRequest = new GetMethodWebRequest(serverAddress + "api/open/annotations/" + annotationID + "/metadata");
            annotationMetadataRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            response = wc.getResponse(annotationMetadataRequest);
            assertNotNull("Response is null!", response);
            
            String strAnnotationMetadata = response.getText();
            assertNotNull("Metadata for Annotation " + annotationID + " is null!", strAnnotationMetadata);
            
            System.out.println("Metadata for Anntoation " + annotationID + ": ");
            System.out.println(strAnnotationMetadata);
            
            
            // Getting annotation content
            WebRequest annotationContentRequest = new GetMethodWebRequest(serverAddress + "api/open/annotations/" + annotationID + "/graph");
            annotationContentRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            response = wc.getResponse(annotationContentRequest);
            assertNotNull("Response is null!", response);
            
            String strAnnotationContent = response.getText();
            assertNotNull("Metadata for Annotation " + annotationID + " is null!", strAnnotationContent);
            
            System.out.println("Content for Anntoation " + annotationID + ": ");
            System.out.println(strAnnotationContent);

            
            // Getting annotation Items
            WebRequest annotationItemRequest = new GetMethodWebRequest(serverAddress + "api/open/annotations/" + annotationID + "/items");
            annotationItemRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            response = wc.getResponse(annotationMetadataRequest);
            assertNotNull("Response is null!", response);
            
            String strAnnotationItems = response.getText();
            assertNotNull("Metadata for Annotation " + annotationID + " is null!", strAnnotationMetadata);
            
            System.out.println("Items for Anntoation " + annotationID + ": ");
            System.out.println(strAnnotationItems);

        } catch (Exception ex) {
            fail("Test #01 failed!");
        }
    }
}
