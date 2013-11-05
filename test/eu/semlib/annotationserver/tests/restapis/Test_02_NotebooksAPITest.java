/*
 *  File:    NotebooksAPITest.java
 *  Created: 30-mag-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.*;
import eu.semlib.annotationserver.tests.DeleteMethodWebRequest;
import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.hibernate.Activenotebooks;
import eu.semlibproject.annotationserver.hibernate.Notebooks;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.models.Notebook;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.MySqlRepository;
import eu.semlibproject.annotationserver.repository.OntologyHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class for testing the creation of a new notebook and the creation of a new annotation
 *
 * @author Michele Nucci
 */
public class Test_02_NotebooksAPITest {
    
    private WebConversation webConversation = null;
    private String notebookID = null;
    private static String notebookID2 = null;
    private static String annotationID = null;
    private static String serverAddress = null;
    private static String annotationJSON = "";
    private static String annotationJSONAtomic = "";
    private static String annotationsWithTargetID = null;
    private static String annotationRDFXML_1 = "";
    private static String annotationRDFXML_2 = "";
    private static String annotationItem_1 = "";
    private static String annotationItem_2 = "";

    public Test_02_NotebooksAPITest() {
    }

    public void setWebConversation(WebConversation wc){
        this.webConversation=wc;
    }
    
     public WebConversation getWebConversation(){
         HttpUnitOptions.setScriptingEnabled( false ); 
        return this.webConversation != null ? this.webConversation : new WebConversation();
    }
    
    @BeforeClass
    public static void setUpClassForTest() {

        System.out.println("Starting testing notebook/annotation rest api...");
        System.out.println("Loading fixtures...");

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);

        try {
            // Read annotation fixture JSON format
            annotationJSON = TestHelper.readFixture("test/fixture_annotations.json");
            if (StringUtils.isBlank(annotationJSON)) {
                fail("Problem loading fixture: fixture_annotations.json");
            }
            
            annotationJSONAtomic = TestHelper.readFixture("test/fixture_ann_model2.json");
            if (StringUtils.isBlank(annotationJSONAtomic)) {
                fail("Problem loading fixture: fixture_ann_model2.json");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_annotations.json");
        }


        // Read annotation fixture RDF/XML format
        try {
            annotationRDFXML_1 = TestHelper.readFixture("test/fixture_annotations_1.rdf");
            if (annotationRDFXML_1 == null || annotationRDFXML_1.equals("")) {
                fail("Problem loading fixture: fixture_annotations_1.rdf");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_annotations_1.rdf");
        }



        // Read annotation fixture RDF/XML format
        try {
            annotationRDFXML_2 = TestHelper.readFixture("test/fixture_annotations_2.rdf");
            if (annotationRDFXML_2 == null || annotationRDFXML_2.equals("")) {
                fail("Problem loading fixture: fixture_annotations_2.rdf");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_annotations_2.rdf");
        }


        // Read the annotation Item fixtures
        try {
            annotationItem_1 = TestHelper.readFixture("test/fixture_items_1.json");
            if (StringUtils.isBlank(annotationItem_1)) {
                fail("Problem loading fixture: fixture_items_1.json");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_items_1.json");
        }
        
        try {
            annotationItem_2 = TestHelper.readFixture("test/fixture_items_2.json");
            if (StringUtils.isBlank(annotationItem_2)) {
                fail("Problem loading fixture: fixture_items_2.json");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_items_2.json");
        }

        System.out.println("Fixture loaded!");   
        
        System.out.println("Setting RDBMS parameters...");
        if (TestHelper.setRDBMSConnection()) {
            System.out.println("RDBMS parameters set!");
        } else {
            System.out.println("Problem setting RDBMS parameters.");
        }

    }


    @Test
    public void test_01_notebookAndAnnotationCreation() {

        System.out.println(">>> Test #01 ========");
        System.out.println("Testing: notebookAndAnnotationCreation...");

        try {

            String notebookName = "{ \"NotebookName\": \"My New Notebook\" }";
            
            WebConversation wc = getWebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks",
                    new ByteArrayInputStream(notebookName.getBytes("UTF-8")),
                    "application/json");
            try {
                WebResponse response = wc.getResponse(request);
                assertNotNull("Response Object is null", response);

                // Check the response status (it must be 201 created)
                int responseCode = response.getResponseCode();
                assertEquals("Response code in Notebook creation not correct", 201, responseCode);

                // Check the Location header (it must be present if the notebook is correctly created
                String location = response.getHeaderField("Location");
                assertNotNull(location);

                // Get the response body and try to parse it
                String responseBody = response.getText();
                JSONObject jsonResponse = new JSONObject(responseBody);

                // Get the notebookID
                notebookID = jsonResponse.getString("NotebookID");
                assertNotNull("The NotebookID in JSON is null", notebookID);

                // Check the hash lenght (for MD5 must be )
                int hashLength = notebookID.length();
                assertEquals("Hash length incorrect", 8, hashLength);

                // Try to create a new annotation
                WebRequest newAnnotation = new PostMethodWebRequest(serverAddress + "api/notebooks/graph/" + notebookID,
                        new ByteArrayInputStream(annotationJSON.getBytes("UTF-8")),
                        "application/json");

                WebResponse annResponse = wc.getResource(newAnnotation);
                assertNotNull("Put Response Object is null", annResponse);

                // Check the response status (it must be 201 created)
                int annResponseConde = annResponse.getResponseCode();
                assertEquals("Response code in Annotation creation not correct", 201, annResponseConde);

                // Check the Location header (it must be present if the notebook is correctly created
                String annLocation = response.getHeaderField("Location");
                assertNotNull(annLocation);

                // Get the response body and try to parse it
                String annResponseBody = annResponse.getText();
                JSONObject jsonResponseForAnnotation = new JSONObject(annResponseBody);

                // Get the notebookID
                String annotationID = jsonResponseForAnnotation.getString("AnnotationID");
                assertNotNull("The AnnotationID in JSON is null", annotationID);
                System.out.println("First AnnotationID: " + annotationID);

                // Check the hash lenght (for MD5 must be )
                int annHashLength = annotationID.length();
                assertEquals("Hash length incorrect", 8, annHashLength);

                // Try to add a new annotation using rdf/xml format...after waiting 2 second
                Thread.sleep(2000);

                JSONObject jsonContext = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                jsonArray.put("http%3A%2F%2Fexample.org%2Ftarget_1");
                jsonArray.put("http%3A%2F%2Fexample.org%2Ftarget_2");
                jsonContext.put("targets", jsonArray);
                jsonContext.put("pageContext", "http%3A%2F%2Fexample.org%2Fcontext_1");

                System.out.println("JSONContext: " + jsonContext.toString());

                WebRequest rdfAnnotation = new PostMethodWebRequest(serverAddress + "api/notebooks/graph/" + notebookID + "?context=" + jsonContext.toString(),
                        new ByteArrayInputStream(annotationRDFXML_1.getBytes("UTF-8")),
                        "application/rdf+xml");

                WebResponse rdfResponse = wc.getResource(rdfAnnotation);
                assertNotNull("Put Response Object is null", rdfResponse);

                // Check the response status (it must be 201 created)
                int rdfResponseConde = rdfResponse.getResponseCode();
                assertEquals("Response code in Annotation creation not correct", 201, rdfResponseConde);

                // Get the response body and try to parse it
                String rdfResponseBody = rdfResponse.getText();
                JSONObject jsonResponseForRDFAnnotation = new JSONObject(rdfResponseBody);

                // Get the annotationID
                annotationsWithTargetID = jsonResponseForRDFAnnotation.getString("AnnotationID");
                assertNotNull("The AnnotationID in RDF is null", annotationsWithTargetID);
                System.out.println("Second AnnotationID: " + annotationsWithTargetID);

            } catch (InterruptedException ex) {
                fail("Problem stopping the main thread.");
            } catch (JSONException ex) {
                fail("Response not valid in notebook or annotation creation.");
            } catch (IOException ex) {
                fail("Unable to get a response. The server or the servlet may be down.");
            } catch (SAXException ex) {
                fail("Unable to parase the server response.");
            }
        } catch (UnsupportedEncodingException ex) {
            fail("UnsupportedEncodingException, HTTPUnit?");
        }
    }

    @Test
    public void test_02_annotationWithBadData() {

        System.out.println(">>> Test #02 ========");
        System.out.println("Testing: annotationWithBadData...");

        String annotationData = "{\"http://localhost/semlib-client/test.html\":[{\"http://www.holygoat.co.uk/owl/redwood/0.1/tags/hasTag\":[]}]}";

        try {

            if (notebookID == null) {
                try {
                    notebookID = SesameRepositoryTestHelper.getInstance().getAValidNotebookID();
                } catch (Exception ec) {
                    fail("Unable to connect to the Sesame Repository");
                }
            }


            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/" + notebookID,
                    new ByteArrayInputStream(annotationData.getBytes("UTF-8")),
                    "application/json");

            WebResponse response = wc.getResponse(request);

            int statusCode = response.getResponseCode();
            assertEquals("Response Incorrect: testing creating annotation with bad RDF data", 400, statusCode);

        } catch (Exception ex) {
            // This test must fail! So if we are here, everithing is ok!!
            System.out.println("Testing annotation with bad data: OK!");
        }

    }

    @Test
    public void test_03_annotationWithBADNotebookID() {

        System.out.println(">>> Test #03 ========");
        System.out.println("Testing: annotationWithBADNotebookID...");

        try {

            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/this-is-a-fakeNotebookID",
                    new ByteArrayInputStream(annotationJSON.getBytes("UTF-8")),
                    "application/json");

            WebResponse response = wc.getResponse(request);
            int statusCode = response.getResponseCode();
            assertEquals("Status code not correct in testing creating a new annotation with bad notebook ID", 400, statusCode);

        } catch (Exception ex) {
            System.out.println("Testing creating annotation with bad notebookid: OK!");
        }

    }

    @Test
    public void test_04_getCurrentActiveNotebookID() {

        System.out.println(">>> Test #04 ========");
        System.out.println("Testing: getCurrentActiveNotebookID...");

        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
            request.setHeaderField("Accept", "application/json");

            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting NotebookID is incorrect", 200, responseCode);

            String responseData = response.getText();
            assertNotNull("Response data in getting current active notebook is null", responseData);

            JSONObject jsonResponse = new JSONObject(responseData);
            String cnotebookID = jsonResponse.getString("NotebookID");
            assertNotNull("Current active NotebookID is null", cnotebookID);

            if (cnotebookID.length() == 0) {
                fail("Current active notebookID is not valid");
            }

            System.out.println("Current action notebookID: " + cnotebookID);

        } catch (Exception ex) {
            fail("Test getNotebookID failed!");
        }
    }

    @Test
    public void test_05_postAnnotationInCurrentActiveNotebook() {

        System.out.println(">>> Test #05 ========");
        System.out.println("Testing: postAnnotationInCurrentActiveNotebook...");

        try {
            // Try to add a new annotation using rdf/xml format...after waiting 2 second
            Thread.sleep(2000);

            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/current",
                    new ByteArrayInputStream(annotationJSONAtomic.getBytes("UTF-8")),
                    MediaType.APPLICATION_JSON);

            WebResponse response = wc.getResponse(request);
            int statusCode = response.getResponseCode();
            assertEquals("Problem creating annotation using api POST /notebooks/current", 201, statusCode);

            String jsonResponseTxt = response.getText();
            JSONObject jsonResponse = new JSONObject(jsonResponseTxt);

            String cAnnotationID = jsonResponse.getString("AnnotationID");
            assertNotNull("Annotation ID is null!", cAnnotationID);

            System.out.println("AnnotationID: " + cAnnotationID);

            this.annotationID = cAnnotationID;

        } catch (Exception ex) {
            fail("Test postAnnotationInCurrentActiveNotebook failed!");
        }

    }

    @Test
    public void test_06_postAnnotationItemTest() {

        System.out.println(">>> Test #06 ========");
        System.out.println("Testing: postAnnotationItem...");

        WebConversation wc = new WebConversation();
        
        try {
            // Try to post annotation Items
            WebRequest requestForItems = new PostMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/items",
                                         new ByteArrayInputStream(annotationItem_1.getBytes("UTF-8")),
                                         "application/json");

            WebResponse requestResponse = wc.getResponse(requestForItems);
            int rStatusCode = requestResponse.getResponseCode();
            assertEquals("Annotation Items Fixture not loaded!", 200, rStatusCode);
        } catch (Exception e) {
            fail("Test postAnnotationItem failed!");
        }               
    }

    
    @Test
    public void test_07_getAnnotationItemTest() {

        System.out.println(">>> Test #07 ========");
        System.out.println("Testing: getAnnotationItems...");

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/items");
        request.setHeaderField("Accept", "application/json");

        try {
            WebResponse requestResponse = wc.getResponse(request);

            int rStatusCode = requestResponse.getResponseCode();
            assertEquals("Annotation Items Fixture not loaded!", 200, rStatusCode);

            String returnedData1 = requestResponse.getText();
            assertNotNull("Returned Data is null!", returnedData1);

            System.out.println("Returned Data:");
            System.out.println(returnedData1);

            JSONObject json1 = new JSONObject(returnedData1);
            Iterator json1Iterator = json1.keys();
            while (json1Iterator.hasNext()) {
                String subject = (String) json1Iterator.next();
                System.out.println("Subject: " + subject);
                if (!subject.equals("http://example.org/items1") && !subject.equals("http://example.org/items2")) {
                    fail("Test failed: subject is not correct!");
                    break;
                }
            }
            
        } catch (Exception ex) {
            fail("Test getAnnotationItem failed!");
        }        
    }

    
    @Test
    public void test_08_searchAnnotationItemTest() {

        System.out.println(">>> Test #08 ========");
        System.out.println("Testing: searchAnnotationItem...");

        try {

            String paramS = "{ \"" + SemlibConstants.JSON_RESOURCES + "\": [ \"http://example.org/items1\" ] } ";
            String paramD = "{ \"" + SemlibConstants.JSON_RESOURCES + "\": [ \"http://example.org/items1\", \"http://example.org/items2\" ] } ";

            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/items/search?query=" + paramS);
            request.setHeaderField("Accept", "application/json");

            WebResponse requestResponse = wc.getResponse(request);
            int rStatusCode = requestResponse.getResponseCode();
            assertEquals("Annotation Items Fixture not loaded!", 200, rStatusCode);

            String returnedData1 = requestResponse.getText();
            assertNotNull("Returned Data is null!", returnedData1);

            System.out.println("Returned Data:");
            System.out.println(returnedData1);

            JSONObject json1 = new JSONObject(returnedData1);
            Iterator json1Iterator = json1.keys();
            while (json1Iterator.hasNext()) {
                String subject = (String) json1Iterator.next();
                System.out.println("Subject: " + subject);
                assertEquals("Subject not correct!", "http://example.org/items1", subject);
            }

            WebRequest request2 = new GetMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/items/search?query=" + paramD);
            request2.setHeaderField("Accept", "application/json");
            WebResponse requestResponse2 = wc.getResponse(request2);
            rStatusCode = requestResponse2.getResponseCode();
            assertEquals("Annotation Items Fixture not loaded!", 200, rStatusCode);

            String returnedData2 = requestResponse.getText();
            assertNotNull("Returned Data is null!", returnedData2);

            System.out.println("Returned Data:");
            System.out.println(returnedData2);


            JSONObject json2 = new JSONObject(returnedData1);
            Iterator json1Iterator2 = json2.keys();
            for (int i = 1; json1Iterator2.hasNext(); i++) {
                String subject = (String) json1Iterator2.next();
                System.out.println("Subject: " + subject);
                assertEquals("Subject not correct!", "http://example.org/items" + i, subject);
            }

        } catch (Exception e) {
            fail("Test searchAnnotationItem failed!");
        }
    }

    @Test
    public void test_09_changeNotebookMetadataTest() {

        System.out.println(">>> Test #09 ========");
        System.out.println("Testing: changeNotebookMetadataTest...");

        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
            request.setHeaderField("Accept", "application/json");

            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting NotebookID is incorrect", 200, responseCode);

            String responseData = response.getText();
            assertNotNull("Response data in getting current active notebook is null", responseData);

            JSONObject jsonResponse = new JSONObject(responseData);
            String cnotebookID = jsonResponse.getString("NotebookID");
            assertNotNull("Current active NotebookID is null", cnotebookID);

            if (cnotebookID.length() == 0) {
                fail("Current active notebookID is not valid");
            }

            System.out.println("Current action notebookID: " + cnotebookID);

            String notebookNameOld = SesameRepositoryTestHelper.getInstance().getNotebookName(cnotebookID);
            assertNotNull("Notebook Name is null!", notebookNameOld);

            System.out.println("Notebook Name: " + notebookNameOld);

            final String newName = "New Name For Notebook";
            final String newNameJSON = "{ \"NotebookName\" : \"" + newName + "\" }";


            WebRequest putrequest = new PutMethodWebRequest(serverAddress + "api/notebooks/" + cnotebookID,
                    new ByteArrayInputStream(newNameJSON.getBytes("UTF-8")),
                    "application/json");

            response = wc.getResource(putrequest);
            assertNotNull("Response is null", response);

            responseCode = response.getResponseCode();
            assertEquals("Response Code not correct", 200, responseCode);

            String newNameFromRepository = SesameRepositoryTestHelper.getInstance().getNotebookName(cnotebookID);
            if (!newNameFromRepository.equals(newName)) {
                fail("New notebook name not correctly set!");
            }

        } catch (Exception ex) {
            fail("Test changeNotebookMetadata failed!");
        }

    }

    @Test
    public void test_10_postAnnotationInCurrentActiveNotebookWithBadData() {

        System.out.println(">>> Test #10 ========");
        System.out.println("Testing: postAnnotationInCurrentActiveNotebookWithBadData...");

        try {
            if (notebookID == null) {
                try {
                    notebookID = SesameRepositoryTestHelper.getInstance().getAValidNotebookID();
                } catch (Exception ec) {
                    fail("Unable to connect to the Sesame Repository");
                }
            }

            System.out.println("Current NotebookID: " + notebookID);

            // Bad URI test
            JSONObject jsonContext = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            jsonArray.put("abcd1234567890");
            jsonContext.put("targets", jsonArray);

            System.out.println("Bad JSON Data: " + jsonContext.toString());

            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "?context=" + jsonContext.toString(),
                    new ByteArrayInputStream(annotationRDFXML_2.getBytes("UTF-8")),
                    "application/rdf+xml");

            // In this case, the test mast fail to be ok!
            WebResponse response = wc.getResponse(request);


        } catch (Exception ex) {
            System.out.println("Test postAnnotationInCurrentActiveNotebookWithBadData: OK!");
        }

    }

    @Test
    public void test_11_getNotebookAnnotationList() {

        System.out.println(">>> Test #11 ========");
        System.out.println("Testing: getNotebookAnnotationList...");

        try {

            WebConversation wc = new WebConversation();
            
            WebRequest req1 = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
            req1.setHeaderField("Accept", "application/json");
            
            WebResponse resp1 = wc.getResource(req1);
            assertNotNull("Response is null!", resp1);
            
            String strNotebookIDContent = resp1.getText();
            JSONObject jsonObj1 = new JSONObject(strNotebookIDContent);
            
            String notebookID = jsonObj1.getString(SemlibConstants.JSON_NOTEBOOK_ID);
            assertNotNull("NotebookID (current Notebook) is null!");

            // Testing without limit and offset            
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata");
            request.setHeaderField("Accept", "application/json");

            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            String strJsonContent_1 = response.getText();

            System.out.println("Response:");
            System.out.println(strJsonContent_1);

            JSONObject jsonContent_1 = new JSONObject(strJsonContent_1);
            assertNotNull("JSONObject is null!", jsonContent_1);

            // Testing ASC/DESC ordering ============================================
            request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata?desc=0");
            request.setHeaderField("Accept", "application/json");

            response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            String strJsonContent_2 = response.getText();
            System.out.println("Response 2 (desc=0):");
            System.out.println(strJsonContent_2);

            if (strJsonContent_1.equalsIgnoreCase(strJsonContent_2)) {
                System.out.println("ASC ordering ok!");
            } else {
                fail("ASC ordering incorrect!");
            }

            request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata?desc=1");
            request.setHeaderField("Accept", "application/json");

            response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            strJsonContent_2 = response.getText();

            System.out.println("Response 2 (desc=1):");
            System.out.println(strJsonContent_2);

            JSONObject jsonContent_2 = new JSONObject(strJsonContent_2);
            assertNotNull("JSONObject 2 is null!");

            String lastKey_2 = null;
            Iterator jsonKeys = jsonContent_2.keys();
            System.out.println("JSONContent_2 Keys: ");
            while (jsonKeys.hasNext()) {
                lastKey_2 = jsonKeys.next().toString();
                System.out.println(lastKey_2);
            }

            String firstKey_1 = null;
            jsonKeys = jsonContent_1.keys();
            System.out.println("JSONObject_1 First Key:");
            while (jsonKeys.hasNext()) {
                firstKey_1 = jsonKeys.next().toString();
                break;
            }
            System.out.println(firstKey_1);

            if (firstKey_1.equalsIgnoreCase(lastKey_2)) {
                System.out.println("DESC ordering ok!");
            } else {
                fail("DESC ordering incorrect!");
            }

            // Test for limit and offset ===============================================
            request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata?limit=1&desc=0");
            request.setHeaderField("Accept", "application/json");

            response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            String strJsonContent_3 = response.getText();

            System.out.println("Response 3 (desc=0, limit=1):");
            System.out.println(strJsonContent_3);

            JSONObject jsonContent_3 = new JSONObject(strJsonContent_3);
            int jsonLenght = jsonContent_3.length();
            System.out.println("Number of Annotations: " + jsonLenght);
            assertEquals("Error on limit: result number incorrect", 1, jsonLenght);

            String firstKey = null;
            jsonKeys = jsonContent_3.keys();
            while (jsonKeys.hasNext()) {
                firstKey = jsonKeys.next().toString();
                break;
            }

            if (!firstKey_1.equalsIgnoreCase(firstKey)) {
                fail("Limit with ordered ASC (default) failed!");
            } else {
                System.out.println("Limit ok!");
            }

            // Test for offset
            request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/annotations/metadata?limit=1&offset=2&desc=0");
            request.setHeaderField("Accept", "application/json");

            responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            String strJsonContent_4 = response.getText();

            System.out.println("Response 4 (desc=0, limit=1, offset=2):");
            System.out.println(strJsonContent_4);

            JSONObject jsonContent_4 = new JSONObject(strJsonContent_4);
            jsonLenght = jsonContent_4.length();
            System.out.println("Number of Annotations: " + jsonLenght);
            assertEquals("Error on limit: result number incorrect", 1, jsonLenght);

            firstKey = null;
            jsonKeys = jsonContent_4.keys();
            while (jsonKeys.hasNext()) {
                firstKey = jsonKeys.next().toString();
                break;
            }

            if (!lastKey_2.equalsIgnoreCase(firstKey)) {
                fail("Offset with limit with ordered ASC (default) failed!");
            } else {
                System.out.println("Offset ok!");
            }

        } catch (Exception ex) {
            fail("Testing getNotebookAnnotationList failed! " + ex.toString());
        }
    }

    @Test
    public void test_12_searchingTest() {

        System.out.println(">>> Test #12 ========");
        System.out.println("Testing: searching...");

        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/annotations/metadata/search?query=%7B%22resources%22%3A%5B%22http%3A%2F%2Fexample.org%2Ftarget_1%22%5D%7D");
            request.setHeaderField("Accept", "application/json");

            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            int responseCode = response.getResponseCode();
            assertEquals("Response code in getting annotation list is incorrect", 200, responseCode);

            String strJsonContent_1 = response.getText();
            System.out.println("Returned metadata in JSON:");
            System.out.println(strJsonContent_1);

            JSONObject annotationDataAsJSON = new JSONObject(strJsonContent_1);
            JSONArray dataKeys = annotationDataAsJSON.names();

            boolean found = false;
            for (int i = 0; i < dataKeys.length(); i++) {
                String cAnnotationID = null;
                JSONObject cObject = annotationDataAsJSON.getJSONObject(dataKeys.getString(i));
                if (cObject.has("http://purl.org/pundit/ont/ao#id")) {
                    JSONObject values = cObject.getJSONArray("http://purl.org/pundit/ont/ao#id").getJSONObject(0);
                    if (values.has("value")) {
                        cAnnotationID = values.getString("value");
                    }

                    if (cAnnotationID != null && cAnnotationID.equalsIgnoreCase(annotationsWithTargetID)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                System.out.println("Annotation found correctly!");
            } else {
                fail("Searching Annotation metadata with specific parameters faild!");
            }

            // Get the ID of the notebook which contains the found annotation
            String tNotebookID = SesameRepositoryTestHelper.getInstance().getNotebookContainerForAnnotation(annotationsWithTargetID);
            assertNotNull("Notebook container is Null!", tNotebookID);

            int indexOfFragment = tNotebookID.lastIndexOf("/");
            tNotebookID = tNotebookID.substring(indexOfFragment + 1);
            System.out.println("NotebookID Container: " + tNotebookID);

            // Repeat the searching limiting to the found notebook
            request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + tNotebookID + "/search?query=%7B%22resources%22%3A%5B%22http%3A%2F%2Fexample.org%2Ftarget_1%22%5D%7D");
            request.setHeaderField("Accept", "application/json");
            response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            String strJsonContent_2 = response.getText();
            System.out.println("Returned metadata in JSON:");
            System.out.println(strJsonContent_2);

            JSONObject annotationDataAsJSON2 = new JSONObject(strJsonContent_2);
            dataKeys = annotationDataAsJSON2.names();

            found = false;
            for (int i = 0; i < dataKeys.length(); i++) {
                String cAnnotationID = null;
                JSONObject cObject = annotationDataAsJSON.getJSONObject(dataKeys.getString(i));
                if (cObject.has("http://purl.org/pundit/ont/ao#id")) {
                    JSONObject values = cObject.getJSONArray("http://purl.org/pundit/ont/ao#id").getJSONObject(0);
                    if (values.has("value")) {
                        cAnnotationID = values.getString("value");
                    }

                    if (cAnnotationID != null && cAnnotationID.equalsIgnoreCase(annotationsWithTargetID)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                System.out.println("Annotation found correctly!");
            } else {
                fail("Searching Annotation metadata with specific parameters faild!");
            }

            // ... and now, create a new empy notebook e search the same annotation
            // metadata in that notebook. This time there should be no results
            String notebookName = "{ \"NotebookName\": \"My New Notebook 2\" }";
            request = new PostMethodWebRequest(serverAddress + "api/notebooks",
                    new ByteArrayInputStream(notebookName.getBytes("UTF-8")),
                    "application/json");

            response = wc.getResponse(request);
            assertNotNull("Response Object is null", response);

            // Check the response status (it must be 201 created)
            responseCode = response.getResponseCode();
            assertEquals("Response code in Notebook creation not correct", 201, responseCode);

            // Get the response body and try to parse it
            String responseBody = response.getText();
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Get the notebookID
            String nNotebookID = jsonResponse.getString("NotebookID");
            assertNotNull("The NotebookID in JSON is null", nNotebookID);

            // Repeat the seaching request
            String urlAPI = serverAddress + "api/notebooks/" + nNotebookID + "/search?query=%7B%22resources%22%3A%5B%22http%3A%2F%2Fexample.org%2Ftarget_1%22%5D%7D";
            request = new GetMethodWebRequest(urlAPI);
            //request.setHeaderField("Accept", "application/json");
            response = wc.getResponse(request);
            assertNotNull("Response is null", response);

            responseCode = response.getResponseCode();
            assertEquals("The response is correct (NO CONTENT)", 204, responseCode);

            System.out.println("Searching test completed!");

        } catch (Exception e) {
            fail("Testing searching failed!" + e.toString());
        }
    }
    
    @Test
    public void test_13_hibernateNotebookCreationDeletionTest() {
        
        System.out.println(">>> Test #13");
        System.out.println("Testing Notebook creation and deletion with Hibernate...");
        
        User anonUser = User.createAnonymousUser();
        
        Notebook newNotebook = null;
        
        try {
            newNotebook = Notebook.createNewNotebook(anonUser, "TEST_NOTEBOOK");
            newNotebook.setID("ABCDEF01");
        } catch (Exception ex) {
            fail("Exception: unable to create a new Notebook Object.\n" + ex.getLocalizedMessage());
            return;
        }
        
        System.out.println("NotebookID: " + newNotebook.getID());
        MySqlRepository repository = new MySqlRepository();        
        assertNotNull("Repository object is null!", repository);
        
        Status response = repository.createNotebookRecord(newNotebook);
        if (response != Status.OK) {
            fail("Unable to create a new Notebook using the Hibernate API");
            return;
        }
        
        Notebooks nNotebook = new Notebooks();
        nNotebook.setId(newNotebook.getID());
                       
        Activenotebooks activeNotebook1 = new Activenotebooks();
        activeNotebook1.setNotebooks(nNotebook);
        activeNotebook1.setUserid(anonUser.getUserID());                
        
        Activenotebooks activeNotebook2 = new Activenotebooks();
        activeNotebook2.setNotebooks(nNotebook);
        activeNotebook2.setUserid("12345678");
        
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            hSession.save(activeNotebook1);
            hSession.save(activeNotebook2);
            hSession.getTransaction().commit();
            
            System.out.println("Hibernate fixture objects saved!");
            System.out.println("Try to delete the newly created Notebook...");
            
            Status delResponse = repository.deleteNotebookRecord(newNotebook, true);
            if (delResponse != Status.OK) {
                fail("Notebook deletion via Hibernate failed!");
            }
            
            System.out.println(">>>> Test #13 completed!");
            
        } catch (HibernateException he) {
            fail("Hibernate Exception!\n" + he.getMessage());            
        }
    }
    
    
    @Test
    public void test_14_activeNotebookTest() {
        
        System.out.println(">>>> Test #14");
        System.out.println("Testing Notebook activation/deactivation");
        
        String notebookName = "{ \"NotebookName\": \"Temp Notebook\" }";

        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks",
                        new ByteArrayInputStream(notebookName.getBytes("UTF-8")),
                        "application/json");
            
            WebResponse response1 = wc.getResponse(request);
            assertNotNull("Notebook creation error. Response is null!", response1);
            
            String responseContent = response1.getText();
            assertNotNull("Response data is null!", responseContent);
            
            JSONObject jsonData = new JSONObject(responseContent);
            notebookID2 = jsonData.getString(SemlibConstants.JSON_NOTEBOOK_ID);
            assertNotNull("Notebook ID is null!", notebookID2);
            
            System.out.println("NotebookID: " + notebookID2);
            
            // Make the Notebook active
            WebRequest requestActive = new PutMethodWebRequest(serverAddress + "api/notebooks/active/" + notebookID2, 
                                                        new ByteArrayInputStream("".getBytes("UTF-8")), "application/json");
            
            WebResponse response2 = wc.getResponse(requestActive);
            assertNotNull("Notebook activation error!", response2);
            
            int responseCode = response2.getResponseCode();
            assertEquals("Response code is not corrent!", 200, responseCode);
            System.out.println("Notebook " + notebookID2 + " is active!");
            
            // Get the notebook Status
            WebRequest notebookStatusRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/active/" + notebookID2);
            WebResponse responseNotebookStatus = wc.getResponse(notebookStatusRequest);            
            assertNotNull("Response Notebook status is null!", responseNotebookStatus);

            String responseNContent = responseNotebookStatus.getText();
            assertNotNull("Notebook status is null!", responseNContent);
            
            JSONObject jsonDataStatus = new JSONObject(responseNContent);
            String activeStatus = jsonDataStatus.getString(SemlibConstants.JSON_NOTEBOOK_ACTIVE);
            assertNotNull("Notebook status String is null", activeStatus);
            
            System.out.println("Notebook " + notebookID2 + " is " + (activeStatus.equals("0") ? "not active!" : "active!"));
            
            // Try to get the list of all active Notebooks
            System.out.println("Getting the list of all active Notebooks...");
            WebRequest listRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/active");
            listRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            WebResponse listResponse = wc.getResponse(listRequest);
            assertNotNull("Response is null!", listResponse);
            
            String strList = listResponse.getText();
            assertNotNull("The list is null!", strList);
            
            System.out.println("List of active Notebooks:");
            System.out.println(strList);
            
            // Try to deactivate a Notebook
            WebRequest deactiveRequest = new DeleteMethodWebRequest(serverAddress + "api/notebooks/active/" + notebookID2);
            WebResponse deactiveResponse = wc.getResponse(deactiveRequest);
            assertNotNull("Deactivation response is null!", deactiveRequest);
            
            int deactivationResponseCode = deactiveResponse.getResponseCode();
            assertEquals("Response code for deactivation is not correct!", 200, deactivationResponseCode);
            
            // Re-check notebook status
            responseNotebookStatus = wc.getResponse(notebookStatusRequest);
            assertNotNull("Checking response code is null!", responseNotebookStatus);
            
            String responseN1Content = responseNotebookStatus.getText();
            assertNotNull("Checking response code is null!", responseN1Content);
            
            jsonDataStatus = new JSONObject(responseN1Content);
            String data = jsonDataStatus.getString(SemlibConstants.JSON_NOTEBOOK_ACTIVE);
            assertNotNull("Notebook status is null!", data);
            
            if (data.equals("1")) {
                fail("Notebook status is not correct!");
            } else if (data.equals("0")) {
                System.out.println("Test #14 completed!");
            }
            
        } catch (Exception e) {
            fail("Test #14 failed!\n" + e);
        }
    }
    
    
    @Test
    public void test_15_postItemWithSpecialCharsTest() {
        System.out.println(">>> Test #15 ========");
        System.out.println("Testing: postAnnotationItem using content with special chars...");

        WebConversation wc = new WebConversation();
                
        try {
            WebRequest requestForItems = new PostMethodWebRequest(serverAddress + "api/annotations/" + annotationID + "/items",
                                         new ByteArrayInputStream(annotationItem_2.getBytes("UTF-8")),
                                         "application/json");
            
            WebResponse requestResponse = wc.getResponse(requestForItems);
            int rStatusCode = requestResponse.getResponseCode();
            assertEquals("Annotation Items Fixture not loaded!", 200, rStatusCode);
            
        } catch (Exception e) {
            fail("Test postAnnotationItem failed!");
        }
    }
    
    
    @Test
    public void test_16_resetCurrentNotebookTest() {
        
        // Try to set a Notebook as current when anothe
        // Notebook is already set as current
        
        System.out.println(">>>> Test #16");
        System.out.println("Testing set Notebook as current");
        System.out.println("Notebook to set as current: " + notebookID2);
        
        try {
            WebConversation wc = new WebConversation();
            
            // Get the current Notebook
            WebRequest currentNotebookRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
            currentNotebookRequest.setHeaderField("Accept", "application/json");
            
            WebResponse currentNotebookResponse = wc.getResponse(currentNotebookRequest);
            assertNotNull("Response is null!", currentNotebookResponse);
            
            String responseDataCurrentNotebook = currentNotebookResponse.getText();
            assertNotNull("Returned Data is null!", responseDataCurrentNotebook);
            
            JSONObject jsonDataCurrentNotebook = new JSONObject(responseDataCurrentNotebook);
            String originalCurrentNotebook = jsonDataCurrentNotebook.getString(SemlibConstants.JSON_NOTEBOOK_ID);
            assertNotNull("Returned Notebook ID is null!", originalCurrentNotebook);            
            
            // Make the Notebook as current
            WebRequest requestNewCurrent = new PutMethodWebRequest(serverAddress + "api/notebooks/current/" + notebookID2, 
                                                               new ByteArrayInputStream("".getBytes("UTF-8")), "application/json");
            
            WebResponse response = wc.getResponse(requestNewCurrent);
            assertNotNull("The response is null!", response);
            
            int responseCode = response.getResponseCode();
            assertEquals("Response code in setting Notebook as current is not correct!", 200, responseCode);
            
            WebRequest getCurrentNotebookRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
            getCurrentNotebookRequest.setHeaderField("Accept", "application/json");
            
            WebResponse cResponse = wc.getResponse(getCurrentNotebookRequest);
            assertNotNull("Response is null!", cResponse);
            
            String jsonData = cResponse.getText();
            assertNotNull("Returned Data is null!", jsonData);
            
            JSONObject json = new JSONObject(jsonData);
            String returnedNotebookID = json.getString(SemlibConstants.JSON_NOTEBOOK_ID);
            assertNotNull("Returned Notebook ID is null!", returnedNotebookID);
            
            if (!returnedNotebookID.equals(notebookID2)) {
                fail("The new Notebook (" + notebookID2 + ") has not be set as current!");
            }
            
            WebRequest requestOldCurrent = new PutMethodWebRequest(serverAddress + "api/notebooks/current/" + originalCurrentNotebook, 
                                                               new ByteArrayInputStream("".getBytes("UTF-8")), "application/json");
            
            WebResponse fResponse = wc.getResponse(requestOldCurrent);
            assertNotNull("Response is null!", fResponse);
            
            int fResponseCode = fResponse.getResponseCode();
            assertEquals("Response code in setting Notebook as current is not correct!", 200, fResponseCode);
            
            System.out.println(">>>> Test #15 completed!");
            
        } catch (Exception e) {
            fail("Test #15 failed!");
        }
    }
    
    
    @Test
    public void test_17_getNotebookMetadataTest() {
        
        System.out.println(">>>> Test #17");
        System.out.println("Getting Notebook Metadata for Notebook: " + notebookID2);
        
        try {
            WebConversation wc = new WebConversation();                        
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID2 + "/metadata");
            request.setHeaderField("Accept", "application/json");
            
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null!", response);
            
            String dataResponse = response.getText();
            assertNotNull("Data response for Notebook metadata is null!", dataResponse);
            
            System.out.println("Notebook Metadata:");
            System.out.println(dataResponse);
            
            JSONObject data = new JSONObject(dataResponse);
            JSONObject jsonMetadata = data.getJSONObject(OntologyHelper.SWN_NAMESPACE + notebookID2);
            JSONArray visibility = jsonMetadata.getJSONArray(OntologyHelper.URI_OV_VISIBILITY);
            
            assertNotNull("Visbility metadata is not specified!", visibility);

            JSONObject visibilityData = (JSONObject)visibility.get(0);
            
            System.out.println("Visibility Status: " + visibilityData.getString("value"));
                        
        } catch (Exception e) {
            fail("Test #16 failed!");
        }
    }
    
    
    @Test
    public void test_18_notebookVisibilityTest() {
    
        System.out.println(">>>> Test #18");
        System.out.println("Check if the Notebook " + notebookID2 + " is public or private...");
        
        WebConversation wc = new WebConversation();
        
        try {

            String nStatus = this.checkNotebookVisibility(notebookID2);
            
            System.out.println("The Notebook " + notebookID2 + " is " + ( (nStatus.equals("0") ? "private" : "public") ) );
            assertEquals("The status of the Notebook" + notebookID2 + " is not correct!", "1", nStatus);
            
            System.out.println("Try to se the Notebook " + notebookID + " as private...");
            
            // make a fake record in ActiveNotebooks to test private
            Notebooks notebook = new Notebooks(notebookID2, "1db22337", true);
            Activenotebooks an = new Activenotebooks(notebook, "11111111");
            
            Long recordId = null;
            try {
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                hSession.beginTransaction();
                
                hSession.save(an);
                
                hSession.getTransaction().commit();
                
                recordId = an.getId();
                System.out.println("Fake ActiveNotebook record ID: " + recordId.toString());
                
            } catch (Exception he) {
                fail("Hiberante exception occurred!");
            }                       
            
            WebRequest privateRequest = new PutMethodWebRequest(serverAddress + "api/notebooks/private/" + notebookID2,
                    new ByteArrayInputStream("".getBytes("UTF-8")),
                    "application/json");
            
            WebResponse privateResponse = wc.getResponse(privateRequest);
            assertNotNull("Response is null!", privateResponse);
            
            int responseStatusCode = privateResponse.getResponseCode();
            assertEquals("The response status code is not correct!", 200, responseStatusCode);
            
            // Check if now the notebook is correctly set as private            
            System.out.println("Try to re-check the Notebook " + notebookID2 + " visibility...");
            nStatus = this.checkNotebookVisibility(notebookID2);
            
            System.out.println("The visibility of the Notebook " + notebookID2 + " is " + ( (nStatus.equals("1") ? "public" : "private") ) );
            assertEquals("The Notebook visibility is not correct!", "0", nStatus);
            
            // now the record recordId in Activenotebook must not exist...check it!
            try {
                Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
                hSession.beginTransaction();
                
                Activenotebooks ann = (Activenotebooks) hSession.get(Activenotebooks.class, recordId);
                
                if (ann != null) {
                    System.out.println("Fake record in Activenotebooks already exists!");
                    fail("Fake record in Activenotebooks already exists!");
                } else {
                    System.out.println("Fake record in Activenotebooks correctly deleted! It works! ;)");
                }
            } catch (Exception hee) {
                fail("Hibernate exception occurred!");
            }            
            
            // Set again the Notebook as public ===============================================
            System.out.println("Set again the Notebook " + notebookID2 + " as Public");
            WebRequest publicRequest = new PutMethodWebRequest(serverAddress + "api/notebooks/public/" + notebookID2,
                    new ByteArrayInputStream("".getBytes("UTF-8")),
                    "application/json");
            
            WebResponse publicResponse = wc.getResponse(publicRequest);
            assertNotNull("Response is null!", publicResponse);
            
            responseStatusCode = publicResponse.getResponseCode();
            assertEquals("The response status code is not correct!", 200, responseStatusCode);
            
            System.out.println("Try to re-check the Notebook " + notebookID2 + " visibility...");
            nStatus = this.checkNotebookVisibility(notebookID2);

            System.out.println("The visibility of the Notebook " + notebookID2 + " is " + ( (nStatus.equals("1") ? "public" : "private") ) );
            assertEquals("The Notebook visibility is not correct!", "1", nStatus);
            
        } catch (Exception ex) {
            fail("Test #17 failed!");
        }        
    }
    
    
    @Test
    public void test_19_getOwnedNotebooksTest() {
        
        System.out.println(">>>> Test #19");
        System.out.println("Get list of all owned Notebooks");
        
        WebConversation wc = new WebConversation();

        try {
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/owned");
            request.setHeaderField("Accept", MediaType.APPLICATION_JSON);
            
            WebResponse response = wc.getResponse(request);
            assertNotNull("Response is null!", response);
            
            String responseData = response.getText();
            assertNotNull("Response data is null!", responseData);
            
            System.out.println("Notebook Owned:");
            System.out.println(responseData);
            
        } catch (Exception e) {
            fail("Test #18 failed!");
        }
    }
    
    
    @Test
    public void test_20_getAllNotebookData() {
        
        System.out.println(">>>> Test #20");
        System.out.println("Get all data about a specified Notebook");
        
        // Get the list of all public Notebook
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
                
                System.out.println("Current Notebook ID: " + cNotebookID.toUpperCase());
                
                WebRequest getallNotebookContentRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/" + cNotebookID);                
                WebResponse response = wc.getResponse(getallNotebookContentRequest);
                assertNotNull("Response is null!", response);
                
                int responseStatusCode = response.getResponseCode();
                assertEquals("HTTP response code is not correct!", 200, responseStatusCode);
                
                String content = response.getText();
                assertNotNull("Response content is null!", content);                                
                
                JSONObject jsonData = new JSONObject(content);
                
                System.out.println("Notebook data:");
                System.out.println(jsonData.toString(2));
                
                if (!jsonData.has(SemlibConstants.JSON_METADATA)) {
                    fail("The response for Notebook '" + cNotebookID.toUpperCase() + "' does not contain metadata!");
                }
                
                if (!jsonData.has(SemlibConstants.JSON_ANNOTATIONS)) {
                    fail("The response for Notebook '" + cNotebookID.toUpperCase() + "' does not contain annotations array!");
                }
                
                JSONArray annotationsData = jsonData.getJSONArray(SemlibConstants.JSON_ANNOTATIONS);
                if (annotationsData.length() > 0) {
                    // we found a Notebooko with metadata and Annotations data. The API is OK.
                    break;
                }
            }
            
        } catch (Exception ex) {
            fail("Test #19 failed!\n" + ex.toString());
        }
    }
    
    
    private String checkNotebookVisibility(String nid) throws Exception {

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/public/" + nid);
        request.setHeaderField("Accept", MediaType.APPLICATION_JSON);

        WebResponse response = wc.getResponse(request);
        assertNotNull("Response is null!", response);

        String jsonData = response.getText();              
        assertNotNull("The response data is null!", jsonData);

        JSONObject data = new JSONObject(jsonData);
        
        System.out.println("Response:");  
        System.out.println(data.toString(2));
        
        String nStatus = data.getString(SemlibConstants.JSON_NOTEBOOK_PUBLIC);
        assertNotNull("The Notebook visibility is null!", nStatus);

        return nStatus;
    }
}
