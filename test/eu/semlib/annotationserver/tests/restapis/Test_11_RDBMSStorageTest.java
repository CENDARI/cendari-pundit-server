/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.*;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.MediaType;
import java.io.ByteArrayInputStream;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_11_RDBMSStorageTest {

    private static String serverAddress  = null;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting RDBMS storage tests...");
        
        System.out.println("Getting repository address...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Server address is null!", serverAddress);
        System.out.println("Server address: " + serverAddress);        
    }
    
    @Test
    public void test_1_insertData() {
        
        System.out.println(">>> Test #1 ========");
        System.out.println("Testing: POST /services/favories");
        
        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/services/favorites",
                        new ByteArrayInputStream("FAVORITES_DATA".getBytes("UTF-8")),
                        MediaType.TEXT_PLAIN_UTF8);
            
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);
            
            int statusCode = response.getResponseCode();
            assertEquals("Response code in posting favorites is incorrect", 200, statusCode);
            
        } catch (Exception ex) {
            fail("Test for POST /services/favorites failed! " + ex.getLocalizedMessage());
        }
    }
    
    @Test
    public void test_2_getFavorites() {
    
        System.out.println(">>> Test #2 ========");
        System.out.println("Testing: GET /services/favories");
        
        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/services/favorites");
            
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            int statusCode = response.getResponseCode();
            assertEquals("Response code in posting favorites is incorrect", 200, statusCode);

            String responseData = response.getText();
            if (StringUtils.isBlank(responseData) || !"FAVORITES_DATA".equals(responseData)) {
                fail("Response data is not correct! - " + responseData);
            }            
            
        } catch (Exception e) {
            fail("Test for GET /services/favorites failed! " + e.getLocalizedMessage());
        }
    }
    
}
