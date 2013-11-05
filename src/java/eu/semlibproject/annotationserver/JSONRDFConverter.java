/*
 * Copyright (c) 2013 Net7 SRL, <http://www.netseven.it/>
 * 
 * This file is part of Pundit: Annonation Server.
 * 
 * Pundit: Annonation Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Pundit: Annonation Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Pundit: Annonation Server.  If not, see <http ://www.gnu.org/licenses/>.
 *
 * See LICENSE.TXT or visit <http://thepund.it> for the full text of the license.
 */

package eu.semlibproject.annotationserver;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Convert JSON data to RDF
 * 
 * @author Michele Nucci
 */
public class JSONRDFConverter {

    // Shared instance
    private static JSONRDFConverter instance = null;
    
    // Flag for bad JSON data
    private boolean badJSONData = false;
    
    // The logger
    private Logger logger = Logger.getLogger(JSONRDFConverter.class.getName());
    
    
    /**
     * Default constructor
     */
    private JSONRDFConverter() {
    }
    
    
    /**
     * Get the shared instance of the JSONConverter
     * 
     * @return the shared instance of the JSONConverter
     */
    public static JSONRDFConverter getInstance() {
        if (instance == null){
            instance = new JSONRDFConverter();
        }
        return instance;
    }
    
    
    /**
     * Convert a String containing JSON data to RDF
     * 
     * @param jsonString    the String containing the JSON data
     * @return              a String cointaining RDF in Turtle format
     */
    public String ConvertJSONToTurtle(String jsonString) {
        
        badJSONData = false;        
        String myTurtle = "";
        
        try {
            JSONObject jsonObj = new JSONObject(jsonString);

            //Iterate over json properties
            Iterator iteratorSubject = jsonObj.keys();
            while (iteratorSubject.hasNext() && !badJSONData) {
                String subject = iteratorSubject.next().toString();

                JSONArray jsonArrayPredicate = null;

                //If predicates are not in an array
                //create an array putting the simple object
                try {
                    jsonArrayPredicate = jsonObj.getJSONArray(subject);
                } catch (JSONException ex) {
                    jsonArrayPredicate = new JSONArray();
                    jsonArrayPredicate.put(jsonObj.getJSONObject(subject));
                }
                
                for (int j = 0; j < jsonArrayPredicate.length() && !badJSONData; j++) {

                    Iterator iteratorPredicate = jsonArrayPredicate.getJSONObject(j).keys();

                    while (iteratorPredicate.hasNext() && !badJSONData) {
                        String predicate = iteratorPredicate.next().toString();
                        JSONObject jsonPredicateObject = jsonArrayPredicate.getJSONObject(j);

                        JSONArray jsonArrayObject = jsonPredicateObject.getJSONArray(predicate);

                        int i = 0;
                        for (i = 0; i < jsonArrayObject.length() && !badJSONData; i++) {
                            JSONObject jsonValueObj = jsonArrayObject.getJSONObject(i);
                            String value = jsonValueObj.getString("value");
                            String type = jsonValueObj.getString("type");
                            String mytriple = CreateTurtleTriple(subject, predicate, type, value);
                            if (StringUtils.isNotBlank(mytriple)) {
                                myTurtle += mytriple;
                            } else {
                                badJSONData = true;
                                break;
                            }                            
                        }
                    }                    
                }


            }
            
            if (badJSONData) {
                myTurtle = null;
            }
            
            return myTurtle;
            
        } catch (JSONException ex) {
            return null;
        }
    }
    
    
    /**
     * Create a Turtle triple
     * 
     * @param sub       the subject
     * @param pred      the predicate
     * @param predType  the object type (literal or uri(
     * @param obj       the object
     * @return          a Turtle triple or null if data are not correct
     */
    private String CreateTurtleTriple(String sub, String pred, String predType, String obj){
        
        String triple = null;
        
        // Check if the subject and the predicte are valid URI
        if (StringUtils.isNotBlank(sub) && StringUtils.isNotBlank(pred) && StringUtils.isNotBlank(predType)) {
            
            // Check if subject and predicate are valid URI
            // Disable for now...java.net.URI report some URI as invalid
            // if (isValidURI(sub) && isValidURI(pred)) {
                if (predType.equalsIgnoreCase("uri") /* && isValidURI(obj) */) {
                    triple = "<" + sub + "> " + " <" + pred + "> " + " <" + obj + "> . ";
                } else if (predType.equalsIgnoreCase("literal")) {                    
                    if (obj != null) {                    
                        /* Trick: sometime the client send content (old versions) that is URLEncoded.
                        * In this case we need to decode it before creaint the triple but...
                        * ...in some cases the client send correctly not URLEncoded content (charset UTF-8).
                        * In this case, if the string contains special char like "%" and we try to
                        * URLDecode, we will get an Exception.
                        * Solution:
                        * Try always to URLDecode a String. If we have an excheption, get the original string.
                        */
                        try {
                            obj = URLDecoder.decode(obj, "UTF-8");
                        } catch (Exception ex) {
                            logger.log(Level.INFO, "String 'Object' is not URLEncoded and cointains special char! Get the original String without encoding.", ex);                    
                        }                    
                    
                        obj = obj.replaceAll("\"", "\\\\\"");
                    } else {
                        obj = "";
                        logger.log(Level.INFO, "The Object of the triple <{0}> <{1}> is empty!", new Object[]{sub, pred});                    
                    }
                    
                    triple = "<" + sub + "> " + " <" + pred + "> " + "\"" + obj + "\" . ";
                }               
            //}            
        } 
                    
        return triple;    
    }

    
    /**
     * Check if a give URI is valid
     * 
     * @param uri   the URI to check as String
     * @return      <code>true</code> if the URI is valid
     */
    private boolean isValidURI(String uri) {
        try {
            final URI uriToCheck = new URI(uri);
            return true;
        } catch (Exception ex) {
            return false;
        }        
    }
    
 }