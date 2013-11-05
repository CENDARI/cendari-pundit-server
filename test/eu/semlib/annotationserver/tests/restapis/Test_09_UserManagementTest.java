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
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.repository.OntologyHelper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_09_UserManagementTest {
    
    private static String serverAddress  = null;    

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting User Management tests...");
        
        System.out.println("Getting repository address...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Server address is null!", serverAddress);
        System.out.println("Server address: " + serverAddress);        
    }

    
    @Test
    public void test_1_currentUser() {
        // Since the automatic test will be executed with authentication not enabled
        // an anonymous user will be used. Using the API: /users/current shoud return
        // only basic data about login status and login server
        
        System.out.println(">>> Test #1 ========");
        System.out.println("Testing: /users/current");
        
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/users/current");
        request.setHeaderField("Accept", "application/json");
        
        try {
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting current user infos is incorrect", 200, responseCode);
            
            String responseData = response.getText();
            assertNotNull("Response data in current user infos is null", responseData);
            
            JSONObject jsonData = new JSONObject(responseData);
            
            System.out.println("Received data:");
            System.out.println(jsonData.toString(2));
            
            String loginStatus = jsonData.getString(SemlibConstants.LOGIN_STATUS);
            assertNotNull("Login status key is not defined! (null)", loginStatus);
            
            String loginServer = jsonData.getString(SemlibConstants.LOGIN_SERVER);
            assertNotNull("Login Server key not defined! (null)", loginServer);
            
            int loginStatusValue = Integer.parseInt(loginStatus);
            assertEquals("Login status must be 0", 0, loginStatusValue);
            
        } catch (Exception e) {
            fail("Test /users/current: failed!");
        }
    }
    
    
    @Test
    public void test_2_logout() {
        // Since the automatic test will be executed with authentication not enabled
        // invoking the API /users/logout should return the JSON for not logged users
        
        System.out.println(">>> Test #2 ========");
        System.out.println("Testing: /users/logout");
        
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/users/logout");
        request.setHeaderField("Accept", "application/json");
        
        try {
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting current user infos is incorrect", 200, responseCode);
            
            String responseData = response.getText();
            assertNotNull("Response data in current user infos is null", responseData);
            
            JSONObject jsonData = new JSONObject(responseData);
                        
            System.out.println("Received data:");
            System.out.println(jsonData.toString(2));
                        
            String logoutStatus = jsonData.getString(SemlibConstants.LOGOUT);
            assertNotNull("Logout key is not defined! (null)", logoutStatus);
                        
            int logoutStatusValue = Integer.parseInt(logoutStatus);
            assertEquals("Logout status value must be 0", 0, logoutStatusValue);
            
        } catch (Exception e) {
            fail("Test /users/logout: failed!");
        }
    }
    
    
    @Test
    public void test_3_checkUserData() {
        // Test for /users/{users-id} API. This test is based on fixture
        
        System.out.println(">>> Test #3 ========");
        System.out.println("Testing: /users/{user-id}");
        
        // Get a valid user ID
        String userID = "84c72ac8";
        
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/users/" + userID);
        request.setHeaderField("Accept", "application/json");

        try {
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting current user infos is incorrect", 200, responseCode);
            
            String responseData = response.getText();
            assertNotNull("Response data in current user infos is null", responseData);
            
            JSONObject jsonData = new JSONObject(responseData);
                        
            System.out.println("Received data:");
            System.out.println(jsonData.toString(2));
            
            
            JSONObject jsonMainDataForUser = jsonData.getJSONObject("http://swickynotes.org/notebook/resource/84c72ac8");
            assertNotNull("JSON Data for the specified user is null!", jsonMainDataForUser);
            
            JSONArray jsonType = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_RDF_TYPE);
            assertNotNull("RDFType in JSON data is null!", jsonType);
            
            JSONObject jsonDataType = jsonType.getJSONObject(0);
            assertNotNull("Values for rdf:type is null!", jsonDataType);
                        
            boolean result = checkValue(jsonDataType, "uri", OntologyHelper.URI_FOAF_PERSON);
            assertTrue("rdf:type values are not correct!", result);
                        
            JSONArray jsonMbox = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_FOAF_MBOX);
            assertNotNull("MBOX in JSON data is null!", jsonMbox);            
            JSONObject jsonMboxValue = jsonMbox.getJSONObject(0);
            result = checkValue(jsonMboxValue, "uri", "mailto:mik.nucci@gmail.com");
            assertTrue("Mbox values are not correct!", result);
            
            JSONArray jsonOpenID = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_FOAF_OPENID);
            assertNotNull("OpenID in JSON data is null!", jsonOpenID);
            JSONObject jsonOpenIDValue = jsonOpenID.getJSONObject(0);
            result = checkValue(jsonOpenIDValue, "uri", "http://www.google.com/profiles/mik.nucci");
            assertTrue("OpenID values are not correct!", result);
                        
            JSONArray jsonFamilyName = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_FOAF_FAMILYNAME);
            assertNotNull("Familyname in JSON data is null!", jsonFamilyName);
            JSONObject jsonFamilyNameValue = jsonFamilyName.getJSONObject(0);
            result = checkValue(jsonFamilyNameValue, "literal", "Nucci");
            assertTrue("FamilyName values are not correct!", result);
            
            JSONArray jsonGivenName = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_FOAF_GIVENNAME);
            assertNotNull("GivenName in JSON data is null!", jsonGivenName);
            JSONObject givenNameValue = jsonGivenName.getJSONObject(0);
            result = checkValue(givenNameValue, "literal", "Michele");
            assertTrue("GivenName values are not correct!", result);
                        
            JSONArray jsonName = jsonMainDataForUser.getJSONArray(OntologyHelper.URI_FOAF_NAME);
            assertNotNull("GivenName in JSON data is null!", jsonName);
            JSONObject nameValue = jsonName.getJSONObject(0);
            result = checkValue(nameValue, "literal", "Michele Nucci");
            assertTrue("Name values are not correct!", result);
                                    
        } catch (Exception e) {
            fail("Test /users/{user-id}: failed!");
        }
    }
    
    
    private boolean checkValue(JSONObject data, String type, String value) throws JSONException {
        
        String dValue = data.getString("value");
        String dType  = data.getString("type");
        
        System.out.println("--> Current value to check");
        System.out.println("Type: " + dType + " (" + type + ")");
        System.out.println("Value: " + dValue + " (" + value +")");
        
        if (dValue == null || dType == null) {
            return false;
        } else {
            return (!type.equalsIgnoreCase(dType) || !value.equalsIgnoreCase(dValue)) ? false : true;
        }
    }
}
