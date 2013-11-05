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

package eu.semlibproject.annotationserver.managers;

import eu.semlibproject.annotationserver.models.User;
import java.util.HashMap;

/**
 * Class to manage authorization tokens
 * 
 * @author Michele Nucci
 */
public class TokenManager {
    
    // Default token time validity (default 2 hours)
    private long tokenTime = (60 * 2 * 60);
    
    // Singleton instance
    private static TokenManager instance = null;        
        
    // Main tokens hashMap
    private HashMap<String, User> tokens = null;
    
    
    /**
     * Default constructor
     */
    private TokenManager() { 
        tokens = new HashMap<String, User>();
    }
    
    
    /**
     * Get the shared instance of TokenManager (singleton)
     * @return 
     */
    public static synchronized TokenManager getInstance() {
        if (instance == null) {
            instance = new TokenManager();
        }
        
        return instance;
    }
    
    
    /**
     * Generate a new token for a logged user. Generate the User object, associate the generate 
     * token with the User object and add these infos into the token register
     * 
     * @param authUser  the generated token as <code>String</code>
     * @return 
     */
    public String generateAndAddNewToken(User authUser) {
        
        if (authUser != null) {
            
            String accessToken = null;
            
            if (!authUser.isAnonymous()) {
                String identity = authUser.getUserID();            
                UtilsManager utilsManager = UtilsManager.getInstance();
                String identityHash = utilsManager.CRC32(identity);
                String timeStamps = Long.toString((System.currentTimeMillis()/1000));
                String timeStampsHash = utilsManager.CRC32(timeStamps);
                accessToken = identityHash+timeStampsHash;
            
                authUser.setAccessToken(accessToken);
                authUser.setTokenTimeStamp(timeStamps);                                
            } else {
                accessToken = authUser.getAccessToken();
            }            

            
            tokens.put(accessToken, authUser);

            return accessToken;

        } else {
            return null;
        }
    }
    
    
    /**
     * Refresh the token time validity into the local Token register
     * 
     * @param token  the token to refresh as <code>String</code>
     * @return       <code>true</code> if the specified token has found, it is valid and it has been refreshed 
     */
    public boolean refreshTokenForUser(String token) {
        if (tokens.containsKey(token)) {
            User cUser = tokens.get(token);
            cUser.setTokenTimeStamp(Long.toString((System.currentTimeMillis()/1000)));
            tokens.put(token, cUser);            
            return true;
        } else {
            return false;
        }
    }
    
    
    /**
     * Check if a token is valid or if it is expired
     *     
     * @param token     the token to check as <code>String</code>
     * @return          <code>true</code> if the specified token is valid
     */
    public boolean isTokenValid(String token) {
        return this.isTokenValid(token, tokenTime);
    }
    
    
    /**
     * Check if a token is valid or if it is expired
     *     
     * @param token         the token to check as <code>String</code>
     * @param tokenTime     the token time
     * @return              <code>true</code> if the specified token is valid
     */
    public boolean isTokenValid(String token, long tokenTime) {
        
        if (tokens.containsKey(token)) {
            User cUser = tokens.get(token);
            if (cUser.isAuthenticated()) {
                
                String sTimeStamp = cUser.getTokenTimeStamp();
                try {
                    long ttimeStamp = Long.parseLong(sTimeStamp);
                    long currentTime = System.currentTimeMillis() / 1000;
                    
                    if (currentTime < ttimeStamp+tokenTime) {
                        // the token is valid...so update the token and continue
                        cUser.setTokenTimeStamp(Long.toString(currentTime));
                        tokens.put(token, cUser);
                        return true;
                    } else {
                        // the token is expired...so remove the record into the token register
                        removeToken(token);
                    }
                    
                } catch (Exception e) {
                    removeToken(token);
                }                
            } else {
                removeToken(token);
            }
        } 
           
        return false;
        
    }
    
    
    /**
     * Get the User object from a token
     * 
     * @param token the token
     * @return      a User object associated with the specified tokem. <code>null</code> if 
     *              there is no User object associated with the specified token
     */
    public User getUserFromToken(String token) {
        return tokens.get(token);
    }
    
    
    /**
     * Remove the token and the associated User object from the local token register
     * 
     * @param token the token to remove
     * @return      <code>true</code> if the token exists and it has been successfully removed
     */
    public boolean removeToken(String token) {
        if (tokens.containsKey(token)) {
            tokens.remove(token);
            return true;
        } else {
            return false;
        }
    }
    
    
    /**
     * Get the max token time validity
     * @return 
     */
    public long getTokenTimeValidity() {
        return this.tokenTime;
    }
    
    
    /**
     * Set the max token time validity
     * @param ttime 
     */
    public void setTokenTimeValidity(long ttime) {
        this.tokenTime = ttime;
    }
}
