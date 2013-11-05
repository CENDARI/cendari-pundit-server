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
public class Test_14_SPARQLEndPointTest {
    
    private static String serverAddress = null;

    public Test_14_SPARQLEndPointTest() {        
    }

    
    @BeforeClass
    public static void setUpClassForTest() {
        System.out.println("Starting testing Open API...");        

        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
    }
    
    
    @Test
    public void test_1_notebookSPARQLEndPointTest() {
        try {
            System.out.println(">>> Test #01 ========");
            System.out.println("Testing: SPARQL end-point for notebook");
            
            WebConversation wc = new WebConversation();
            
            // Get the list of all public Notebooks
            WebRequest notebookListRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/public");
            notebookListRequest.setHeaderField("Accept", MediaType.APPLICATION_JSON);
                
            WebResponse response = wc.getResponse(notebookListRequest);
            assertNotNull("Response is null!", response);
                
            String strNotebookList = response.getText();
            assertNotNull("The public Notebook list is null!", strNotebookList);
                
            System.out.println("Public Notebook List:");
            System.out.println(strNotebookList);
                
            JSONObject jsonNotebookList = new JSONObject(strNotebookList);
            JSONArray notebookList = jsonNotebookList.getJSONArray(SemlibConstants.JSON_NOTEBOOK_IDS);
            assertNotNull("AnnotationList is null!", notebookList);
            
            for (int i = 0; i < notebookList.length(); i++) {
                String notebookID = notebookList.getString(i);
                assertNotNull("Notebook ID is null!", notebookID);
                System.out.println("Notebooko ID: " + notebookID);

                // CONSTRUCT {?s ?p ?o FROM} <http://mynotebook> WHERE {?s ?p ?o}
                String query = "CONSTRUCT%20%7B%3Fs%20%3Fp%20%3Fo%7D%20FROM%20%3Chttp%3A%2F%2Fmynotebook%3EWHERE%20%7B%3Fs%20%3Fp%20%3Fo%7D";
                WebRequest sparqlRequest = new GetMethodWebRequest(serverAddress + "api/open/notebooks/" + notebookID + "/sparql?query=" + query);
                sparqlRequest.setHeaderField(SemlibConstants.HTTP_HEADER_ACCEPT, MediaType.TEXT_RDFN3);

                response = wc.getResource(sparqlRequest);
                assertNotNull("Response is null!", response);

                int responseCode = response.getResponseCode();
                if (responseCode != 200) {
                    continue;
                }

                String responseData = response.getText();
                if (StringUtils.isBlank(responseData)) {
                    fail("Response data is null!");
                }

                System.out.println("RDF data (N3):");
                System.out.println(responseData);

                // Simple checks
                String data1 = "<http://rdf.freebase.com/ns/en.pluto_press> a <http://www.freebase.com/schema/business/business_operation> , <http://www.freebase.com/schema/organization/organization> , <http://www.freebase.com/schema/book/publishing_company> , <http://www.freebase.com/schema/m/04l1354> , <http://www.freebase.com/schema/business/employer> , <http://www.freebase.com/schema/common/topic> ;";
                String data2 = "<http://aretino-autografi-viterbo.ctl.sns.it/vt_handwritten_text_cards/169004809#xpointer(start-point(string-range(//DIV[@about='http://aretino-autografi-viterbo.ctl.sns.it/vt_handwritten_text_cards/169004809']/P[2]/SPAN[12]/text()[1],'',25))/range-to(string-range(//DIV[@about='http://aretino-autografi-viterbo.ctl.sns.it/vt_handwritten_text_cards/169004809']/P[2]/SPAN[26]/text()[1],'',27)))> a <http://www.semlibproject.eu/vocab/text-fragment> ;";
            
                if (!responseData.contains(data1) || !responseData.contains(data2)) {
                    break;
                }
            }
                        
        } catch (Exception ex) {
            fail("Test 01 failed!\n" + ex.getMessage());
        }
    }
    
}
