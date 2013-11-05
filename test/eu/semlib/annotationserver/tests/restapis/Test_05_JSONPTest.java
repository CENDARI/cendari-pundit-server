/*
 *  File:    Test_5_JSONPTest.java
 *  Created: 13-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper;
import eu.semlib.annotationserver.tests.TestHelper;
import java.io.IOException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_05_JSONPTest {
    
    private static String serverAddress = null;
    
    private static String notebookID = null;
    private static String annotationID = null;
    
    private String CALLBACK_FUNC = "myCallbackFunc";
    
    public Test_05_JSONPTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        System.out.println("Starting testing API with JSONP...");

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);

        System.out.println("Getting a valid annotation ID for test...");
        annotationID = SesameRepositoryTestHelper.getInstance().getValidAnnotationID();
        System.out.println("Valid annotation ID found: " + annotationID);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
    public void test_0_testAPIWithJSONPforNotebooks() {

        WebConversation wc = new WebConversation();            

        // API GET /notebooks/current
        try {
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/current?jsonp=" + CALLBACK_FUNC);
            WebResponse response = wc.getResponse(request); 
            
            checkResponse(response);            
            
            String content = response.getText();
            System.out.println("Content [GET /notebooks/current] using JSONP:");
            System.out.println(content);

            // Get the JSON content
            content = content.replace(CALLBACK_FUNC+"(", "");
            content = content.substring(0, content.length()-1);
            
            JSONObject json = new JSONObject(content);
            notebookID = json.getString("NotebookID");
            assertNotNull("Current Notebooks is null", notebookID);
            
        } catch (Exception ex) {
            fail("Testing API using JSONP faild!");
        } 
       
        // API GET /notebooks/NOTEBOOKID/graph
        try {
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/graph?jsonp=" + CALLBACK_FUNC);
                                
            WebResponse response = wc.getResponse(request); 
            checkResponse(response);  
            
            String content = response.getText();
            System.out.println("Content [/notebooks/"+notebookID+"/graph] using JSONP:");
            System.out.println(content);

        } catch (Exception ex) {
            fail("Testing API using JSONP faild!");
        } 
        
    }
    
    
    
    @Test
    public void test_1_testAPIWithJSONPforAnnotations() {
        
        WebConversation wc = new WebConversation();  
        
        // API GET /annotations/ANNOTATIONID/metadata
        try {
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/metadata?jsonp=" + CALLBACK_FUNC);
                                
            WebResponse response = wc.getResponse(request); 
            checkResponse(response);  
            
            String content = response.getText();
            System.out.println("Content [GET /annotations/" + annotationID + "/metadata] using JSONP:");
            System.out.println(content);

        } catch (Exception ex) {
            fail("Testing API using JSONP faild!");
        } 
        
        
        // API GET /annotations/ANNOTATIONID/content
        try {
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/graph?jsonp=" + CALLBACK_FUNC);
                                
            WebResponse response = wc.getResponse(request); 
            checkResponse(response);  
            
            String content = response.getText();
            System.out.println("Content [GET /annotations/" + annotationID + "/content] using JSONP:");
            System.out.println(content);

        } catch (Exception ex) {
            fail("Testing API using JSONP faild!");
        } 
        
    }
    
    
    private void checkResponse(WebResponse response) throws IOException {
        
        assertNotNull("Response is null", response);

        String content = response.getText();
        assertNotNull("Response Content is null", content);

        boolean length = (content.length() > 0);
        assertTrue("Response is empty", length);

        boolean result = content.contains(CALLBACK_FUNC + "(") && content.endsWith(")");
        assertTrue("Returned data not wrapped by the JSONP callback", result);

    }
}
