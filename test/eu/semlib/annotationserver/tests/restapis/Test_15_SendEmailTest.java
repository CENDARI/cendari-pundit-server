/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_15_SendEmailTest {
    
    private static String serverAddress = null;

    public Test_15_SendEmailTest() {        
    }

    
    @BeforeClass
    public static void setUpClassForTest() {
        System.out.println("Starting testing Open API...");        

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
    }
    
    
    @Test
    public void test_1_sendEmailTest() {
        try {
            System.out.println(">>> Test #01 ========");
            System.out.println("Testing: email sending");
            
            WebConversation wc = new WebConversation();
            
            WebRequest sendEmailRequest = new PostMethodWebRequest(serverAddress + "api/services/email");
            sendEmailRequest.setParameter("subject", "Test invio email");
            sendEmailRequest.setParameter("text", "invio email ");
            sendEmailRequest.setParameter("email","netseventest@gmail.com");
            sendEmailRequest.setParameter("name","Net7 test account");
            sendEmailRequest.setParameter("identifiers", "list1");
                
            WebResponse response = wc.getResponse(sendEmailRequest);
            assertNotNull("Response is null!", response);
            
            assertEquals(200, response.getResponseCode());
                
                        
        } catch (Exception ex) {
            fail("Test 01 failed!\n" + ex.getMessage());
        }
    }
    
}
