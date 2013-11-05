/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.semlib.annotationserver.tests.restapis;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.PostMethodWebRequest;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.SemlibConstants;
import java.io.IOException;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author martinelli
 */
public class Test_Authentication {

    final static String GOOGLE_ENDPOINT = "https://www.google.com/accounts/o8/id";
    private static String serverAddress = null;
    private static WebConversation wc = null;

    @BeforeClass
    public static void setUpClassForTest() {
        System.out.println("Starting testing openID Authentication...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Unable to parse the server address!", serverAddress);
        wc = new WebConversation();
        wc.getClientProperties().setAcceptCookies(true);
        wc.getClientProperties().setUserAgent("Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.172 Safari/537.22");
        
    }
    public static  void main(String[] args) throws IOException {
        WebClient webClient = new WebClient();
         HtmlPage page = webClient.getPage("http://htmlunit.sourceforge.net");
         System.out.println(webClient.getCookieManager().getCookies());
    }
    

    
    public void test_Authentication() {
        System.out.println(">>> Testing #Authentication ========");
        try {
            int i=1; 
            wc.getClientProperties().setAutoRedirect(true);
            
            WebRequest wr = new GetMethodWebRequest(serverAddress + "/login.jsp");
            WebResponse response = wc.getResource(wr);
            
            assertNotNull("Response Null!" + response);
            assertEquals(response.getResponseCode(), 200);
            System.out.println("RESPONSE #"+i++ +" CODE: "+response.getResponseCode() +
                    "\r\n\t ##### response: "+response.toString());
                    
            printCookies(wc);
                    
            
            wr = new PostMethodWebRequest(serverAddress + "/openidauthentication");
            wr.setParameter(SemlibConstants.OPENID_IDENTIFIER, GOOGLE_ENDPOINT);
            wr.setParameter(SemlibConstants.OPENID_RETURN_PAGE, "/login.jsp");
            response = wc.getResource(wr);
            assertNotNull("Response Null!" + response);
            if (response.getResponseCode() == 302) {
               response =  manageRedirect(wc,wr,response,i);
            }
            
            assertEquals(response.getResponseCode(), 200);
            String continue_id = null,scc =null ,sarp=null,GALX = null;
            String[] header_set_cookies = response.getHeaderFields("SET-COOKIE");
            for (String param: header_set_cookies){
                if (param.startsWith("GALX=")){
                    GALX = param.split("=")[1].split(";")[0];
                }
            }
            String queryUrl = response.getURL().getQuery();
            String[] parameters_values =queryUrl.split("&");
            for (String param: parameters_values){
                if (param.startsWith("continue=")){
                    continue_id = param.split("=")[1];
                }
                if (param.startsWith("scc=")){
                      scc= param.split("=")[1];
                }
                if (param.startsWith("sarp=")){
                      sarp= param.split("=")[1];
                }
            }
            //System.out.println("continue:"+continue_id+"\r\n scc:"+scc+"\r\n sarp:"+sarp+"\r\n GALX:"+GALX);        
            WebForm form = (WebForm) response.getFormWithID("gaia_loginform");
            
            HTMLElement email = response.getElementWithID("Email");
            assertNotNull("Cannot fill email field", email);
            HTMLElement password = response.getElementWithID("Passwd");
            assertNotNull("Cannot fill email field", password);
            
            HTMLElement continue_f = response.getElementWithID("continue");
            assertNotNull("Cannot fill email field", continue_f);
            HTMLElement scc_f = response.getElementWithID("scc");
            assertNotNull("Cannot fill email field", scc_f);
            HTMLElement sarp_f = response.getElementWithID("sarp");
            assertNotNull("Cannot fill email field", sarp_f);
            HTMLElement[] GALX_f = response.getElementsWithName("GALX");
            assertNotNull("Cannot fill email field", GALX_f[0]);
            
            
            email.setAttribute("value", "martinelli@netseven.it");
            password.setAttribute("value", "martinelli");
            continue_f.setAttribute("value",continue_id);
            scc_f.setAttribute("value",scc);
            sarp_f.setAttribute("value",sarp);
            GALX_f[0].setAttribute("value",GALX);
            response = form.submit();
            
             System.out.println("RESPONSE #"+i++ +" CODE: "+response.getResponseCode() +
                    "\r\n\t ##### response: "+response.toString() 
            
                     );
             
         printCookies(wc);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private  WebResponse manageRedirect(WebConversation wc, WebRequest wr, WebResponse response,int i) throws IOException {
            String _LOCATION = response.getHeaderField("LOCATION");
            assertNotNull("LOCATION header not present in the response",_LOCATION);
            
            wr = new PostMethodWebRequest(_LOCATION);
            response = wc.getResource(wr);
            
            System.out.println("RESPONSE #"+i++ +" CODE: "+response.getResponseCode() +
                    "\r\n\t ##### response: "+response.toString());
            printCookies(wc);
            assertNotNull("Response Null!" + response);
            if (response.getResponseCode() == 302) {
                response = manageRedirect(wc,wr,response,i);
            } return response;
            
        
    }
    
    private void printCookies(WebConversation wc){
       for (String cookieName :wc.getCookieNames() ){
            System.out.println(cookieName+": "+wc.getCookieDetails(cookieName).getValue());
    }
    }
}

    
