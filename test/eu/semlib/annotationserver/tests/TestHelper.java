/*
 *  File:    TestHelper.java
 *  Created: 27-giu-2011
 */
package eu.semlib.annotationserver.tests;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.codehaus.jettison.json.JSONObject;

/**
 * A test Helper class
 * 
 * @author Michele Nucci
 */
public class TestHelper {
    
    public static String getServerAddress() {
        try {
            Properties configProperties = new Properties();            
            configProperties.load(new FileReader("test/tests_configuration.properties"));
            String serverAddress = configProperties.getProperty("annotationserver.instanceURL");
            System.out.println("AnnotationServer Instance: " + serverAddress);
            return serverAddress;
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static boolean setRDBMSConnection() {
        try {
            Properties configProperties = new Properties();
            configProperties.load(new FileReader("test/tests_configuration.properties"));
            String dialect = configProperties.getProperty("rdbms.dialect");
            System.out.println("RDBMS Dialect: " + dialect);
            String driver = configProperties.getProperty("rdbms.driver");
            System.out.println("RDBMS Driver: " + driver);
            String connectionString = configProperties.getProperty("rdbms.connectionString");
            System.out.println("RDBMS Connection String: " + connectionString);
            String user = configProperties.getProperty("rdbms.user");
            System.out.println("RDBMS User: " + user);
            String password = configProperties.getProperty("rdbms.password");
            ConfigManager.getInstance().setRDBMSParameters(dialect, driver, connectionString, user, password);           
            return true;
        } catch (IOException ex) {
            return false;
        }        
    }
    
    public static String createNewNotebook(String serverAddress) throws Exception {
        // Create a new notebook
        String notebookName = "{ \"NotebookName\": \"My New Notebook\" }";

        WebConversation wc = new WebConversation();
        WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks",
                new ByteArrayInputStream(notebookName.getBytes("UTF-8")),
                "application/json");

        WebResponse response = wc.getResponse(request);
        if (response == null) {
            return null;
        }
         
        // Get the response body and try to parse it
        String responseBody = response.getText();
        JSONObject jsonResponse = new JSONObject(responseBody);

        // Get the notebookID
        return jsonResponse.getString("NotebookID");

    }
    
    public static String readFixture(String fixture) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(fixture));
        
        String str;
        String allData = "";
        
        while ((str = in.readLine()) != null) {
            allData += str;
        }
        in.close();

        return allData;
    }
    
    public static String loadAnnotationFixture(String serverAddress, String notebookID, String annotationData, String contentType) throws Exception {
        
        WebConversation wc = new WebConversation();
        
        WebRequest request = new PostMethodWebRequest(serverAddress + "api/notebooks/" + notebookID,
                                    new ByteArrayInputStream(annotationData.getBytes("UTF-8")),
                                    contentType);

        WebResponse response = wc.getResponse(request);
        int statusCode = response.getResponseCode();
        if (statusCode != 201) {
            return null;
        }
       
        String jsonResponseTxt = response.getText();
        JSONObject jsonResponse1 = new JSONObject(jsonResponseTxt);
        String annotationID = jsonResponse1.getString("AnnotationID");
        return annotationID;
    }
    
}
