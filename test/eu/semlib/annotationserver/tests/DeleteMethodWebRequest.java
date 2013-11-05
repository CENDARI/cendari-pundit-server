/*
 *  File:    DeleteMethodWebRequest.java
 *  Created: 17-giu-2011
 */
package eu.semlib.annotationserver.tests;

import com.meterware.httpunit.HeaderOnlyWebRequest;
import java.net.URL;


/**
 *
 * @author Michele Nucci
 */
public class DeleteMethodWebRequest extends HeaderOnlyWebRequest {

    /**
     * initialize me - set method to DELETE
     */
    private void init() {
        super.setMethod("DELETE");
    }
 
    /**
     * Constructs a web request using a specific absolute url string.
     **/
    public DeleteMethodWebRequest( String urlString ) {
        super( urlString );
        init();
    }
 
 
    /**
     * Constructs a web request using a base URL and a relative url string.
     **/
    public DeleteMethodWebRequest( URL urlBase, String urlString ) {
        super( urlBase, urlString );
        init();
    }
 
 
    /**
     * Constructs a web request with a specific target.
     **/
    public DeleteMethodWebRequest( URL urlBase, String urlString, String target ) {
        super( urlBase, urlString, target );
        init();
    }
    
    
    @Override
    public String getMethod() {
        return "DELETE";
    }

}