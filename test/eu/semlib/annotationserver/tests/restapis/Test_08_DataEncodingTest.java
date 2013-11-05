/*
 *  File:    Test_8_DataEncodingTest.java
 *  Created: 4-ott-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author Michele Nucci
 */
public class Test_08_DataEncodingTest {
    
    private static String notebookID       = null;
    private static String serverAddress    = null;
    private static String annotationJSON_1 = null;
    private static String annotationJSON_2 = null;
    private static String annotationJSON_3 = null;
    
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting Data Encoding API tests...");        
        
        System.out.println("Getting repository address...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Server address is null!", serverAddress);
        System.out.println("Server address: " + serverAddress);
                        
        if (notebookID == null || notebookID.length() == 0) {
            notebookID = TestHelper.createNewNotebook(serverAddress);
            assertNotNull("NotebookID is null!", notebookID);
        } else {
            System.out.println("NotebookID: " + notebookID);
        }
                        
        try {
            annotationJSON_1 = TestHelper.readFixture("test/fixture_ann_encoding_1.json");
            if (StringUtils.isBlank(annotationJSON_1)) {
                fail("Problem loading fixture: fixture_ann_encoding_s_1.json");
            }

            annotationJSON_2 = TestHelper.readFixture("test/fixture_ann_encoding_2.json");
            if (StringUtils.isBlank(annotationJSON_2)) {
                fail("Problem loading fixture: fixture_ann_encoding_d_2.json");
            }
            
            annotationJSON_3 = TestHelper.readFixture("test/fixture_ann_htmlDataInComment.json");
            if (StringUtils.isBlank(annotationJSON_3)) {
                fail("Problem loading fixture: fixture_ann_htmlDataInComment.json");
            }

        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_annotations.json");
        }
                                                
        
    }
    
    @Test
    public void test_1_baseEncodingTest() {        
        // This test should fail. 
        // Try to decode a not URLEncoded string which contains special chars like %
        String notEncodedString = "This is a string with percent char: 1% 10%";
        
        try {
            notEncodedString = URLDecoder.decode(notEncodedString, "UTF8");
            fail("Test #1 failed!");
        } catch (Exception e) {
            System.out.println("Test #1 (URLdecoding string with special chars): ok1");
        }
    }
    
    
    @Test
    public void test_2_jsonWithEncoding() {
        try {
            
            System.out.println("Test with single Encoding (only field encoded) ====");
                        
            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/" + notebookID,
                        new ByteArrayInputStream(annotationJSON_1.getBytes("UTF-8")),
                        "application/json");             
           
            try {
                
                // Test with no URL encoding, only double quote escaping
                WebResponse response = wc.getResponse(request);
                assertNotNull("Response Object is null", response);
                
                int responseCode = response.getResponseCode();
                System.out.println("Response code: " + responseCode);
                assertEquals("Response code not correct!", 201, responseCode);
                
                String txtResponse = response.getText();
                assertNotNull("Response payload is null", txtResponse);
                
                JSONObject jsonData = new JSONObject(txtResponse);
                String annotationID = jsonData.getString("AnnotationID");
                assertNotNull("AnnotationID is null", annotationID);
                System.out.println("AnnotationID: " + annotationID);                
                
                // Test with URL encoding                
                request = new PostMethodWebRequest(serverAddress + "api/notebooks/" + notebookID,
                          new ByteArrayInputStream(annotationJSON_2.getBytes("UTF-8")),
                          "application/json");
                
                response = wc.getResponse(request);
                assertNotNull("Response Object is null", response);
                
                responseCode = response.getResponseCode();
                System.out.println("Response code: " + responseCode);
                assertEquals("Response code not correct!", 201, responseCode);
                
                String txtResponse_1 = response.getText();
                                
                JSONObject jsonData_1 = new JSONObject(txtResponse_1);
                annotationID = jsonData_1.getString("AnnotationID");
                assertNotNull("AnnotationID is null", annotationID);
                System.out.println("AnnotationID: " + annotationID); 
                
            } catch (Exception e) {
                fail("Problem getting the server response of or a general error has occurred during test execution");
            }

        } catch (UnsupportedEncodingException ex) {
            fail("Unable to send request to the server or a general error has occurred!");
        }
        
        System.out.println("Encoding test completed!");
    }
    
    
    @Test
    public void test_3_payloadWithHTMLInComment() {
        
        System.out.println("Test with single HTML code in Annotation comment ====");
        
        WebConversation wc = new WebConversation();
        
        try {
            String apiAddress = serverAddress + "api/notebooks/" + notebookID;
            System.out.println("Try to call: " + apiAddress);
            WebRequest request = new PostMethodWebRequest(apiAddress,
                    new ByteArrayInputStream(annotationJSON_3.getBytes("UTF-8")),
                    "application/json; charset=UTF-8");
            
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response object is null!", response);
            
        } catch (Exception ex) {
            fail("Unable to send request to the server or a general error has occurred!");
        }

    }

}
