/*
 *  File:    GetAllTriplesTest.java
 *  Created: 31-mag-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.SemlibConstants;
import java.io.IOException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author Michele Nucci
 */
public class Test_03_GetAllTriplesTest {
    
    private static String serverAddress = null;
    
    private static String notebookID    = null;
    private static String n3Triple      = null;
    
    public Test_03_GetAllTriplesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Starting GetAllTriplesTest...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void Test_1_testGetAllTriples() {

        try {          
            
            WebConversation wc = new WebConversation();
            
            try {
                WebRequest notebookIdRequest = new GetMethodWebRequest(serverAddress + "api/notebooks/current");
                notebookIdRequest.setHeaderField("Accept", "application/json");
                WebResponse wResponse = wc.getResponse(notebookIdRequest);
                assertNotNull("Response from server is null!", wResponse);
                
                String responseEntity = wResponse.getText();
                assertNotNull("Response is null!", responseEntity);
                
                JSONObject data = new JSONObject(responseEntity);                
                notebookID = data.getString(SemlibConstants.JSON_NOTEBOOK_ID);
                assertNotNull("NotebookID is null!", notebookID);
                
            } catch (Exception ec) {
                fail("Unable to connect to the repository or unable to get a valid Notebook ID! " + ec.toString());
            }                        
            
            WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/" + notebookID + "/graph");
            request.setHeaderField("Accept", "application/json");

            try {

                // Test JSON response
                WebResponse response = wc.getResponse(request);
                int responseCode = response.getResponseCode();
                assertEquals("Response code not correct", 200, responseCode);

                System.out.println("Response Content-Type: " + response.getHeaderField("Content-Type"));
                
                String triplesJSON = response.getText();
                assertNotNull("JSON Triples null", triplesJSON);

                if (triplesJSON != null && !triplesJSON.equals("")) {
                    System.out.println("Triples JSON:\n" + triplesJSON + "\n\n");
                }

                // Test RDF/XML response
                request.setHeaderField("Accept", "application/rdf+xml");
                response = wc.getResponse(request);
                responseCode = response.getResponseCode();
                assertEquals("Response code not correct", 200, responseCode);
                
                System.out.println("Response Content-Type: " + response.getHeaderField("Content-Type"));
                
                String triplesRDFXML = response.getText();
                assertNotNull("RDFXML Triples null", triplesRDFXML);

                if (triplesRDFXML != null && !triplesRDFXML.equals("")) {
                    System.out.println("Triples RDF/XML:\n" + triplesRDFXML + "\n\n");
                }

                // Test N3 response
                request.setHeaderField("Accept", "text/rdf+n3");
                response = wc.getResponse(request);
                responseCode = response.getResponseCode();
                assertEquals("Response code not correct", 200, responseCode);

                System.out.println("Response Content-Type: " + response.getHeaderField("Content-Type"));
                
                n3Triple = response.getText();
                assertNotNull("N3 Triples null", n3Triple);

                if (n3Triple != null && !n3Triple.equals("")) {
                    System.out.println("Triples RDF/XML:\n" + n3Triple + "\n\n");
                }

            } catch (SAXException ex) {
                fail("Unable to query the main repository using the notebookID: " + notebookID + ". " + ex.toString());
            } catch (IOException ex) {
                fail("Unable to query the main repository using the notebookID: " + notebookID + ". " + ex.toString());
            }
                       
        } catch (Exception ex) {
            fail("General problem: repository initialization? " + ex.toString());
        }
                   
    }
    
    
    @Test
    public void Test_2_getAllTriplesInTheCurrentNotebook() {
        
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(serverAddress + "api/notebooks/current/graph");
        request.setHeaderField("Accept", "text/rdf+n3");
        
        try {
            WebResponse response = wc.getResponse(request);
            int responseCode = response.getResponseCode();
            assertEquals("Response code not correct", 200, responseCode);

            System.out.println("Response Content-Type: " + response.getHeaderField("Content-Type"));
            
            String triples = response.getText();
            assertNotNull("N3 Triples null", triples);
            
            if ( (triples == null && n3Triple != null) || (triples != null && n3Triple == null) ) {
                fail("Problem getting notebook graph. Data from the two tests inconsistent");
            }
            
            if (!n3Triple.equals(triples)) {
                fail("Triples from test 1 are different from test 2 tiples!");
            }
            
        } catch (Exception ex) {
            fail("Unable to query main repository. Server reachable? " + ex.toString());
        }        

    }
    
}
