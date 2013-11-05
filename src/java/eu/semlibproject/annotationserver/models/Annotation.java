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

package eu.semlibproject.annotationserver.models;

import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.repository.OntologyHelper;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * The main abstract class for the Annotation Model
 * 
 * @author Michele Nucci
 */
public class Annotation {
    
    // The annotation ID and the annotation-body-id
    private String id = null;
    
    // The full annotation URI
    private String annotationURI = null;
    
    // The context associated to this annotation
    private String annotationGraph = null;
    
    // The context associated to the annotation's item
    private String annotationItemGraph = null;
    
    // The notebookID in which the annotation is located
    private String notebookID    = null;
    private String notebookURI   = null;
    private String notebookOwner = null;
    
    // The creation date
    private String creationDate = null;
    
    // The modified date
    private String modifiedDate = null;
    
    // The author of the annotation
    private String author = null;
    
    // The content type of the whole annotation data (in/out)
    private String contentType = null;
    
    // The annotation context informationin JSON format
    private String annotationContext = null;
    
    // The annotation author
    private User user = null;
    
    // Internal use only
    private AnnotationModelVersion annotationModelVersion = AnnotationModelVersion.ANNOTATION_MODEL_VERSION_NA;
    
    // Annotation data/item as String
    private String annotationDataAsRDF       = null;
    private String annotationDataAsJSON      = null;
    private String annotationDataItemsAsRDF  = null;
    private String annotationDataItemsAsJSON = null;
    
    public enum AnnotationDataType {
        ANNOTATION_METADATA,
        ANNOTATION_GRAPH,
        ANNOTATION_ITEM
    }
    
    public enum DataToWrite {
        ANNOTATION_TRIPLES,
        ANNOTATION_ITEMS,
        ANNOTATION_TRIPLES_ITEMS
    }
    
    public enum AnnotationModelVersion {
        ANNOTATION_MODEL_VERSION_NA,
        ANNOTATION_MODEL_VERSION_1,
        ANNOTATION_MODEL_VERSION_2        
    }
    
    /**
     * Default constructor
     */
    private Annotation() {        
    }

    
    /**
     * Main constructor
     * 
     * @param user              an User object
     * @param notebookID        a valid Notebook ID
     * @param annotationData    Annotation data
     * @param contentType       contentType of the Annotation data
     * @param annotationContext Annotation Context
     * 
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException 
     */
    private Annotation(User user, String notebookID, String annotationData, String contentType, String annotationContext) throws NoSuchAlgorithmException, UnsupportedEncodingException, JSONException {
    
        this.creationDate     = UtilsManager.getInstance().getDate(true);
        this.notebookURI      = Notebook.getURIFromID(notebookID);
        this.notebookID       = notebookID;
        
        if (StringUtils.isNotBlank(annotationContext)) {            
            this.annotationContext = annotationContext;            
        }
        
        if (user != null) {
            this.user   = user;
            this.author = user.getUserIDasURI();
        }
                        
        // Compute the ANNOTATION-ID
        // CRC32(ANNOTATION-DATA+TIMESTAMPS);
        String hashType = ConfigManager.getInstance().getHASHAlgorithmName();        
        this.id = UtilsManager.getInstance().computeHashWithDigest( (annotationData+this.creationDate), hashType);           
        this.setAnnotationURIs();
                
        // Set the annotation data
        setAnnotationDataAsString(annotationData, contentType);
    }
    
    
    /**
     * Create a new Annotation object
     * 
     * @param annotationData    the annotation data as String
     * @param contentType       the contentType of the annotation data. Supported type: JSON, RDF/XML. 
     *                          If the contentType is not specified it will be setted to the default value (application/json).
     * @param notebookID        the ID of the notebook in which to insert the new annotation
     * @param annotationTarget  the annotation target as URL
     * @return                  the annotstion ID
     * 
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException 
     */
    public static Annotation createNewAnnotation(User user, String annotationData, String contentType, String notebookID, String annotationTarget) throws NoSuchAlgorithmException, UnsupportedEncodingException, JSONException {
        return new Annotation(user, notebookID, annotationData, contentType, annotationTarget);
    }
    
    
    /**
     * Get an empty annotation object
     * 
     * @return a new empty Annotation object
     */
    public static Annotation getEmpyAnnotationObject() {
        return new Annotation();
    }
    
    
    /**
     * Set the annotation's data. The data must be in JSON, RDF/XML, N3 format.     
     * 
     * @param annotationData the annotation's data as String
     * @param contentType    the content-type of the annotationData
     */
    public final void setAnnotationDataAsString(String annotationData, String contentType) throws JSONException {
        
        this.setDataContentType(contentType);
                        
        if (this.contentType.contains(MediaType.APPLICATION_JSON)) {            
            JSONObject jsonData = new JSONObject(annotationData);
            if (jsonData.has(SemlibConstants.JSON_GRAPH)) {
                // Annotation model version 2
                this.annotationModelVersion = AnnotationModelVersion.ANNOTATION_MODEL_VERSION_2;
                
                JSONObject jsonGraph = jsonData.getJSONObject(SemlibConstants.JSON_GRAPH);
                this.annotationDataAsJSON = jsonGraph.toString();
                
                // Check if items are specified
                if (jsonData.has(SemlibConstants.JSON_ITEMS)) {
                    JSONObject jsonItems = jsonData.getJSONObject(SemlibConstants.JSON_ITEMS);
                    this.annotationDataItemsAsJSON = jsonItems.toString();
                }
            } else {
                // Annotation model version 1 (old model)
                this.annotationModelVersion = AnnotationModelVersion.ANNOTATION_MODEL_VERSION_1;
                this.annotationDataAsJSON = annotationData;
            }
        } else {
            // In this case we have alway an annotation with model version 1 (old)
            this.annotationModelVersion = AnnotationModelVersion.ANNOTATION_MODEL_VERSION_1;
            this.annotationDataAsRDF    = annotationData;
        }        
    }

    
    /**
     * Set the annotation's items. The data must be in JSON, RDF/XML, N3 format.
     * 
     * @param annotationDataItem    the annotation's items as String
     * @param contentType           the content-type of the annotationData
     */
    public final void setAnnotationDataItemsAsString(String annotationDataItem, String contentType) {
        
        this.setDataContentType(contentType);
        
        if (this.contentType.contains(MediaType.APPLICATION_JSON)) {
            this.annotationDataItemsAsJSON = annotationDataItem;
        } else {
            this.annotationDataItemsAsRDF = annotationDataItem;
        }
        
    }
    
    
    /**
     * Set the annotation id. Starting from the id, this methods also set
     * the annotationURI and the annotationGraph.
     * 
     * @param id    the annotation id
     */ 
    public void setID(String id) {
        if (StringUtils.isNotBlank(id)) {
            this.id = id;
            this.setAnnotationURIs();
        }
    }
        
    
    /**
     * Set the notebook ID (or notebook URI)
     * 
     * @param notebookURI the annotation body URI
     */
    public void setNotebook(String notebookURI) {
        if (StringUtils.isNotBlank(notebookURI)) {
            this.notebookURI = notebookURI;
        }        
    }
    
    
    /**
     * Set the annotation modified date.
     * 
     * @param date the modified date. If <code>null</code> the modified date
     *             will be automatically set.
     */
    public void setModifiedDate(String date) {
        if (StringUtils.isNotBlank(date)) {
            this.modifiedDate = date;
        } else {
            this.modifiedDate = UtilsManager.getInstance().getDate(true);
        }
    }
    
    
    /**
     * Set the annotation user
     * 
     * @param user the annotation User
     */
    public void setUser(User user) {
        if (user != null) {
            this.user = user;
        }
    }
    
    
    public void setNotebookOwner(String notebookOwner) {
        if (notebookOwner != null) {
            this.notebookOwner = notebookOwner;
        }
    }
    
    
    // Getter methods =========================
    
    /**
     * Get the full annotation URI given a valid annotation ID
     * 
     * @param annotationID  the annotation ID
     * @return              the full annotation URI    
     */
    public static String getURIFromID(String annotationID) {
        return OntologyHelper.SWN_NAMESPACE +  annotationID;
    }
    
    
    /**
     * Get the full annotation graph URI given a valid annotation ID
     * 
     * @param annotationID  the annotation ID
     * @return              the full URI of the annotation graph
     */
    public static String getGraphURIFromID(String annotationID) {
        return OntologyHelper.SWN_NAMESPACE + "graph-" + annotationID;
    }            
    
    
    /**
     * Get the full annotation item URI given a valid annotation ID
     * 
     * @param annotationID  the annotation ID
     * @return              the full URI of the annotation item graph
     */
    public static String getItemsGraphURIFormID(String annotationID) {
        return OntologyHelper.URI_SWN_ITEMSGRAPH + "-" + annotationID;
    }
    
    
    /**
     * Get the annotation data as String basing on the contentType
     * 
     * @return the annotation's data as String
     */
    public String getAnnotationDataAsString() {

        if (this.contentType.contains(MediaType.APPLICATION_JSON)) {
            return this.annotationDataAsJSON;
        } else {
            return this.annotationDataAsRDF;
        }
        
    }

    public String getAnnotationItemsAsString() {
        
        if (this.contentType.contains(MediaType.APPLICATION_JSON)) {
            return this.annotationDataItemsAsJSON;
        } else {
            return this.annotationDataItemsAsRDF;
        }
        
    }
    
    public String getID() {
        return this.id;
    }
            
    public String getURI() {
        return this.annotationURI;
    }
    
    public String getGraph() {
        return this.annotationGraph;
    }
    
    public String getAnnotationItemGraph() {
        return this.annotationItemGraph;
    }
    
    public String getAuthor() {
        return this.author;
    }
    
    public String getContentType() {
        return this.contentType;
    }
    
    public String getCreationDate() {
        return this.creationDate;
    }
    
    public String getModifiedDate() {
        return this.modifiedDate;
    }
    
    public String getNotebookID() {
        return this.notebookID;
    }
    
    public String getNotebookURI() {
        return this.notebookURI;
    }
    
    
    public String getAdditionalContext() {
        return this.annotationContext;
    }
    
    public User getUser() {
        return this.user;
    }
    
    public String getNotebookOwner() {
        return this.notebookOwner;
    }
    
    public AnnotationModelVersion getAnnotationModelVersion() {
        return this.annotationModelVersion;
    }
    
    
    // Private helpers methods ====
   
    private void setAnnotationURIs() {
        this.annotationURI = Annotation.getURIFromID(id);
        this.annotationGraph = Annotation.getGraphURIFromID(id);
        this.annotationItemGraph = Annotation.getItemsGraphURIFormID(id);
    }

    
    private void setDataContentType(String contentType) {
        if (StringUtils.isNotBlank(contentType)){
            this.contentType = contentType;
        } else {
            // Default value
            this.contentType = MediaType.APPLICATION_JSON;            
        }
    }
            
}

