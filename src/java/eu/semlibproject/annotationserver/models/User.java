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
import eu.semlibproject.annotationserver.managers.TokenManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.repository.OntologyHelper;
import org.apache.commons.lang3.StringUtils;
import org.openid4java.discovery.Identifier;

/**
 * Model class for a User
 * 
 * @author Michele Nucci
 */
public class User {
    
    // This is the user ID HASH(receviedID). It is not a URI
    private String id               = null;
    
    // This is the full user ID. It is URI
    private String extendedIDasURI  = null;
    
    private String firtsName        = null;
    private String lastName         = null;    
    private String fullName         = null;
    private String screenName       = null;    
    private String email            = null;
    private String accessToken      = null;
    private String tokenTimeStamp   = null;
    private boolean authenticated   = false;
    private boolean anonymous       = false;
    private boolean checkedForRepo  = false;
    
    // Specific OpenID attributes    
    private Identifier openidIdentifier = null;
    

    /**
     * Default constructor
     */
    public User() {
    }
    
    
    /**
     * Construcor Used for OpenID authenticaed user
     * 
     * @param identifier the OpenID identifier
     */
    public User(Identifier identifier) {
        if (identifier != null) {
            this.openidIdentifier = identifier;
        }
    }
    
    
    public User(String id, String token, String tokenTimeStamp) {
        this.id = id;
        this.extendedIDasURI = OntologyHelper.SWN_NAMESPACE + id;        
        this.accessToken = token;
        this.tokenTimeStamp = tokenTimeStamp;
    }
    
    
    /**
     * Factory method used to create a new anonymous user
     * 
     * @return an anonymous User
     */
    public static User createAnonymousUser() {                
        User anonymousUser              = new User();                                
        anonymousUser.id                = UtilsManager.getInstance().CRC32(SemlibConstants.ANONYMOUS_PLAIN_ID);
        anonymousUser.extendedIDasURI   = User.getURIFromID(anonymousUser.getUserID());
        anonymousUser.anonymous         = true;
        anonymousUser.fullName          = SemlibConstants.ANONYMOUS_FULLNAME;
        anonymousUser.authenticated     = true;
        anonymousUser.accessToken       = anonymousUser.id + "ffffffff";
        
        TokenManager.getInstance().generateAndAddNewToken(anonymousUser);        
        return anonymousUser;
    }
    
    
    /**
     * Helper method that returns the full user's URI from its ID
     * 
     * @param userID    the user's ID
     * @return          the full user's URI
     */
    public static String getURIFromID(String userID) {
        return OntologyHelper.SWN_NAMESPACE + userID;
    }
    
    
    // Getter methods =================
    public String getUserID() {
        return this.id;
    }
    
    public String getUserIDasURI() {
        return this.extendedIDasURI;
    }
    
    public String getFirstName() {
        return this.firtsName;
    }
        
    public String getLastName() {
        return this.lastName;
    }

    public String getFullName() {                
        if (StringUtils.isBlank(this.fullName)) {
            
            fullName = "";
            
            if (StringUtils.isNotBlank(this.firtsName)) {
                fullName += this.firtsName + " ";
            }

            if (StringUtils.isNotBlank(this.lastName)) {
                fullName += this.lastName;
            }            
        }
        
        return this.fullName;
    }
    
    public String getScreenName() {
        return this.screenName;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public String getAccessToken() {
        return this.accessToken;
    }
    
    public String getTokenTimeStamp() {
        return this.tokenTimeStamp;
    }
    
    public boolean isAuthenticated() {
        return this.authenticated;
    }
    
    public boolean isAnonymous() {
        return this.anonymous;
    }
    
    public boolean isAlreadyCheckedForRepository() {
        return this.checkedForRepo;
    }
    
    public Identifier getOpenIDIdentifier() {
        return this.openidIdentifier;
    }
   
    public String getOpenIDIdentifierAsString() {
        String openIDIdentifierAsString = null;
        if (this.openidIdentifier != null) {
            openIDIdentifierAsString = this.openidIdentifier.getIdentifier();
        }        
        return openIDIdentifierAsString;
    }
    
    // Setter Methods ====
    public void setID(String id) {
        if (StringUtils.isNotBlank(id)) {
            this.id = id;
            this.extendedIDasURI = OntologyHelper.SWN_NAMESPACE + id;
        }
    }
        
    public void setFirstName(String firstName) {
        if (StringUtils.isNotBlank(firstName)) {
            this.firtsName = firstName;
        }
    }
    
    public void setLastName(String lastName) {
        if (StringUtils.isNotBlank(lastName)) {
            this.lastName = lastName;
        }
    }
    
    public void setFullName(String fullName) {
        if (StringUtils.isNotBlank(fullName)) {
            this.fullName = fullName;
        }
    }
    
    public void setScreenName(String screenName) {
        if (StringUtils.isNotBlank(screenName)) {
            this.screenName = screenName;
        }
    }
    
    public void setEmail(String email) {
        if (StringUtils.isNotBlank(email)) {
            this.email = email;
        }
    }
    
    public void setAccessToken(String accessToken) {
        if (StringUtils.isNotBlank(accessToken)) {
            this.accessToken = accessToken;
        }        
    }
    
    public void setTokenTimeStamp(String tokentimestamp) {
        if (StringUtils.isNotBlank(tokentimestamp)) {
            this.tokenTimeStamp = tokentimestamp;
        }
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public void setCheckedForRepository(boolean checked) {
        this.checkedForRepo = checked;
    }    
}
