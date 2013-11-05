/*
 *  File:    Test_3_GetAnnotationData.java
 *  Created: 8-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import java.io.IOException;
import java.util.Iterator;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_04_GetAnnotationDataTest {
    
    private static String serverAddress = null;
    private static String annotationID = null;
    
    
    public Test_04_GetAnnotationDataTest() {
    }

    
    @BeforeClass
    public static void setUpClass() throws Exception {

        System.out.println("Starting testing getting annotation data and metadata...");

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
    }
    
    @Test
    public void getAnnotationMetadata() {

        if (annotationID == null) {
            try {
                this.getValidAnnotationID();
            } catch (Exception e) {
                fail("Unable to get a valid annotation ID!");
            }
        }
                            
        String dataType = "metadata";

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/metadata");

        // Test metadata in RDF+N3
        request.setHeaderField("Accept", "text/rdf+n3");
        try {
            WebResponse response = wc.getResponse(request);
            checkResponse(response, dataType, "RDF+N3");

        } catch (Exception ex) {
            fail("Unable to get annotation metadata in RDF+N3");
        }

        // Test metadata in RDF+JSON
        request.setHeaderField("Accept", "application/json");
        try {
            WebResponse response = wc.getResponse(request);
            checkResponse(response, dataType, "JSON");

        } catch (Exception ex) {
            fail("Unable to get annotation metadata in JSON");
        }

        // Test metadata in RDF+XML
        request.setHeaderField("Accept", "application/rdf+xml");
        try {
            WebResponse response = wc.getResponse(request);
            checkResponse(response, dataType, "RDF+XML");

        } catch (Exception ex) {
            fail("Unable to get annotation metadata in RDF+XML");
        }
    }
    
    @Test
    public void getAnnotationContent() {

        if (annotationID != null) {
            
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/graph");
            
            String dataType = "content";
            
            // Test metadata in RDF+N3
            request.setHeaderField("Accept", "text/rdf+n3");
            try {
                WebResponse response = wc.getResponse(request);
                checkResponse(response, dataType, "RDF+N3");
                
            } catch (Exception ex) {
                fail("Unable to get annotation content in RDF+N3");
            }

            // Test metadata in RDF+JSON
            request.setHeaderField("Accept", "application/json");
            try {
                WebResponse response = wc.getResponse(request);
                checkResponse(response, dataType, "JSON");
                
            } catch (Exception ex) {
                fail("Unable to get annotation content in JSON");
            }
            
            // Test metadata in RDF+XML
            request.setHeaderField("Accept", "application/rdf+xml");
            try {
                WebResponse response = wc.getResponse(request);
                //checkResponse(response, dataType, "RDF+XML");
                
            } catch (Exception ex) {
                fail("Unable to get annotation content in RDF+XML");
            }

        } else {
            System.out.println("No valid annotationID found into the triplestore! Load fixtures!");
        }

    }
    
    
    public void checkResponse(WebResponse response, String dataType, String format) throws IOException {
        
        assertNotNull("Response is null", response);
                
        int responseCode = response.getResponseCode();
        assertEquals("Response code not correct", 200, responseCode);

        String data = response.getText();
        assertNotNull("Response data is null", data);
        
        System.out.println("Annotation " + dataType + " in: " + format);
        System.out.println(data + "\n");
        
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
        String notebookID = data.getString(SemlibConstants.JSON_NOTEBOOK_ID);        
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
