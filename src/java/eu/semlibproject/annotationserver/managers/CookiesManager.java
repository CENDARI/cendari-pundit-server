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

import eu.semlibproject.annotationserver.SemlibConstants;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Singleton class used to manage the Annotation Server Cookies
 * 
 * @author Michele Nucci
 */
public class CookiesManager {
   
    // The shared instance
    private static CookiesManager instance;
    
    // Default constructor
    private CookiesManager() { }
    
    
    /**
     * Get the shared instance
     * 
     * @return the shared instance of the cookies Manager
     */
    public static synchronized CookiesManager getInstance() {
        if (instance == null) {
            instance = new CookiesManager();
        }
        
        return instance;
    }
    
    
    /**
     * Check if the current user is authenticated/authorized verifying
     * the annotation server cookie
     * 
     * @param httpRequest   the HTTP servlet request
     * @return              if the user is authorized/authenticaed, return the accessToken
     *                      otherwise return <code>null</code>
     */
    public String isUserAuthorizedFromCookie(HttpServletRequest httpRequest, HttpServletResponse response) {
        
        boolean authorized = false;                
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cCookie = cookies[i];
                if (SemlibConstants.COOCKIE_NAME.equalsIgnoreCase(cCookie.getName())) {
                    String cookieValue = cCookie.getValue();                    
                    authorized = TokenManager.getInstance().isTokenValid(cookieValue);
                    if (authorized) {
                        return cookieValue;
                    } else {
                        removeASCookie(response, cCookie);
                    }
                }
            }
        }
        
        return null;
    }
    
    
    /**
     * Check if a user is authorized from his/her token
     * 
     * @param token the token     
     * @return <code>true</code> if the user is authorized
     */
    public boolean isUserAuthorizedFromToken(String token) {
        
        if (StringUtils.isBlank(token)) {
            return false;
        }
        
        return TokenManager.getInstance().isTokenValid(token);
    }
    
    
    /**
     * Generate a new cookie for the annotation server
     * 
     * @param accessToken   the accessToken
     * @return              the new generated cookie
     */
    public Cookie generateNewASCookie(String accessToken) {                
        
        if (accessToken != null) {
            Cookie cookie = new Cookie(SemlibConstants.COOCKIE_NAME, accessToken);
            cookie.setComment(SemlibConstants.COOCKIE_DESCRIPTION);
            cookie.setPath(SemlibConstants.COOKIE_PATH);
            cookie.setMaxAge(SemlibConstants.COOKIE_TIME); 
            cookie.setVersion(1);
            cookie.setSecure(false);
            
            return cookie;
        }
        
        return null;
    }

        
    /**
     * Remove an annotation server cookie    
     */
    public void removeASCookie(HttpServletResponse response, Cookie cookie) {        
        cookie.setMaxAge(0);
        response.addCookie(cookie);        
    }
    
    
    /**
     * Remove an annotation server cookie
     * 
     * @param request
     * @param response
     * @param token 
     */
    public void removeASCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cCookie = cookies[i];
                if (SemlibConstants.COOCKIE_NAME.equalsIgnoreCase(cCookie.getName())) {
                    String value = cCookie.getValue();
                    if (value.equals(token)) {
                        removeASCookie(response, cCookie);
                    }
                    break;
                }
            }
        }
    }
    
}
