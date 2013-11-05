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

package eu.semlibproject.annotationserver.restapis;

import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.managers.*;
import eu.semlibproject.annotationserver.models.User;
import eu.semlibproject.annotationserver.repository.RepositoryException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Main class that implements all REST APIs under
 * the namespace "[our-webapps]/users/"
 * 
 * @author Michele Nucci
 */
@Path("/users")
public class UsersAPI extends APIHelper {
    
    private Logger logger = Logger.getLogger(UsersAPI.class.getName());
            
            
    /**
     * Default constructor
     * 
     * @param req               the HttpServletRequest       
     * @param servletContext    the ServletContext
     */
    public UsersAPI(@Context HttpServletRequest req, @Context ServletContext servletContext) {                
        super(req, servletContext);
    }

    
    /**
     * Return a JSON response with information about the current logged user.
     * 
     * @param httpRequest   the HTTP request received by the servlet (passed by Jersey)    
     * @return              a JSON response with some user's infos
     */
    @GET
    @Path("current")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getInfosAboutCurrentLoggedUsers(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {
                        
        if (StringUtils.isNotBlank(userToken)) {
            
            TokenManager tokenManager = TokenManager.getInstance();
            
            if (tokenManager.isTokenValid(userToken)) {
                User currentUser = tokenManager.getUserFromToken(userToken);
                if (currentUser.isAuthenticated()) {
                    return generateJSONLoggedUser(currentUser, callback);
                }
            } else {
                tokenManager.removeToken(userToken);
            }
        }
        
        return generateJSONUserNotLogged(callback);
    }
    
    
    /**
     * Get all information about a specific user from a userID
     * 
     * @param callback      the JSONP callback (optional)
     * @param userID        the ID of the user
     * @param accept        the accepted data format
     * @return              information about a specific userID
     */
    @GET
    @Path("{user-id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_RDFXML, MediaType.TEXT_RDFN3})
    public Response getInfosAboutUser(@QueryParam(SemlibConstants.JSONP_PARAM) String callback, @PathParam("user-id") String userID, @HeaderParam(SemlibConstants.HTTP_HEADER_ACCEPT) String accept) {
        
        Status userIDStatus = this.checkUserID(userID);
        if (userIDStatus != Status.OK) {
            return Response.status(userIDStatus).build();
        }
        
        // This is necessary to handle the GET requests from normal browser
        String cAccepts = UtilsManager.getInstance().getCorrectAcceptValue(callback, accept);        

        String userData = null;
        try {
            String triplesFormat = cAccepts;
            if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
                triplesFormat = MediaType.APPLICATION_JSON;
            }
            
            userData = RepositoryManager.getInstance().getCurrentRDFRepository().getUserData(userID, triplesFormat);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage().toString(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return createFinalResponse(callback, cAccepts, userData);
    }     
    
    
    /**
     * Logout a current logget user
     * 
     * @param request       the HTTPServletRequest
     * @param asCookie      the cookie associated with the logged user
     * @return              
     * 
     * @throws IOException
     * @throws URISyntaxException 
     */
    @GET
    @Path("logout")    
    public Response logoutUser(@Context HttpServletRequest request, @CookieParam(SemlibConstants.COOCKIE_NAME) Cookie asCookie) throws IOException, URISyntaxException {
        
        String contextPath = request.getContextPath();
        String contentType = request.getHeader(SemlibConstants.HTTP_HEADER_ACCEPT);        
        
        if (asCookie != null) {
            
            String pathForNewCookie   = null;
            String token              = asCookie.getValue();    
            String cookiePath         = asCookie.getPath();
            
            if (StringUtils.isNotBlank(cookiePath)) {
                pathForNewCookie = cookiePath;
            } else {
                pathForNewCookie = contextPath;
            }
                        
            NewCookie expiredCookie = new NewCookie(SemlibConstants.COOCKIE_NAME, null, pathForNewCookie, null, 1, null, -1, false);                       
            
            if (contentType.contains(MediaType.APPLICATION_JSON)) {
                
                String responseContent = null;
                if (CookiesManager.getInstance().isUserAuthorizedFromToken(token)) {
                    TokenManager.getInstance().removeToken(token);                            
                    responseContent = generateLogoutResponse(true);
                } else {
                    responseContent = generateLogoutResponse(false);
                }
                
                return Response.ok(responseContent, javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).cookie(expiredCookie).build();
                
            } else {
                
                if (StringUtils.isNotBlank(token)) {
                    TokenManager.getInstance().removeToken(token);
                }
                
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length()-2);
                }
                
                return Response.temporaryRedirect(new URI(contextPath + "/logout.jsp")).cookie(expiredCookie).build();
            }
            
        }
        
        if (contentType.contains(MediaType.APPLICATION_JSON)) {
             String responseContent = generateLogoutResponse(false);
             return Response.ok(responseContent, javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 2);
            }

            return Response.temporaryRedirect(new URI(contextPath + "/logout.jsp")).build();
        }
                
    }
    
    
    /**
     * Generate the JSON response for a logged user
     * 
     * @return JSON response
     */
    private Response generateJSONLoggedUser(User user, String callback) {
        
        String jsonResponseValue = null;
        
        JSONObject json = new JSONObject();
        try {
            json.put(SemlibConstants.LOGIN_STATUS, 1);
            json.put(SemlibConstants.ID, user.getUserID());
            json.put(SemlibConstants.URI, user.getUserIDasURI());            
            
            String openID = user.getOpenIDIdentifierAsString();
            if (StringUtils.isNotBlank(openID)) {
                json.put(SemlibConstants.OPENID, openID);
            }
            
            String firstName = user.getFirstName();
            if (!StringUtils.isBlank(firstName)) {
                json.put(SemlibConstants.FIRST_NAME, firstName);
            }
                        
            String lastName = user.getLastName();
            if (!StringUtils.isBlank(lastName)) {
                json.put(SemlibConstants.LAST_NAME, lastName);
            }
            
            String screenName = user.getScreenName();
            if (!StringUtils.isBlank(screenName)) {
                json.put(SemlibConstants.SCREEN_NAME, screenName);
            }
            
            String fullName = user.getFullName();
            if (!StringUtils.isBlank(fullName)) {
                json.put(SemlibConstants.FULL_NAME, fullName);
            }
            
            String email = user.getEmail();
            if (!StringUtils.isBlank(email)) {
                json.put(SemlibConstants.EMAIL, email);
            }
             
            json.put(SemlibConstants.LOGIN_SERVER, ConfigManager.getInstance().getAuthenticationFormPath());
            
            jsonResponseValue = json.toString(2);
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            jsonResponseValue = " { \"" + SemlibConstants.LOGIN_STATUS + "\": 1 }"; 
        }
        
        if (StringUtils.isNotBlank(callback)) {
            String finalResponse = UtilsManager.getInstance().wrapJSONPResponse(jsonResponseValue, callback);
            return Response.ok(finalResponse).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_JAVASCRIPT).build();
        } else {
            return Response.ok(jsonResponseValue).build();
        }                
    }
    
    
    /**
     * Check if a specified User ID exists and if it is valid
     * 
     * @param userID        the user ID to check
     * 
     * @return              Status.OK                       if the specified User exists and if it is valid<br/>
     *                      Status.NOT_FOUND                if the specified User does not exists
     *                      Status.BAD_REQUEST              if the specified User is not valid
     *                      Status.INTERNAL_SERVER_ERROR    in case of general internal error
     */
    private Status checkUserID(String userID) {                
        int hashNumOfChars = UtilsManager.getInstance().getCurrentHASHLenght();
        if (hashNumOfChars == -1) {
            return Status.INTERNAL_SERVER_ERROR;
        }
        
        if (userID == null || userID.length() < hashNumOfChars) {
            return Status.BAD_REQUEST;            
        } else {

            try {
                // Check if the specified notebook exists        
                boolean userExists = RepositoryManager.getInstance().getCurrentRDFRepository().userExists(userID);
                if (!userExists) {
                    return Status.NOT_FOUND;                            
                } else {
                    return Status.OK;                    
                }
            } catch (RepositoryException ex) {
                Logger.getLogger(NotebooksAPI.class.getName()).log(Level.SEVERE, null, ex);
                return Status.INTERNAL_SERVER_ERROR;
            }
        }
    }
    
    
    /**
     * Generate the JSON for not logged user
     * 
     * @return JSON response
     */
    private Response generateJSONUserNotLogged(String callback) {
        
        String jsonResponseValue = null;
        
        JSONObject json = new JSONObject();
        try {
            json.put(SemlibConstants.LOGIN_STATUS, 0);
            
            String loginServer = ConfigManager.getInstance().getAuthenticationFormPath();
            if (!StringUtils.isBlank(loginServer)) {
                json.put(SemlibConstants.LOGIN_SERVER, loginServer);
            }
            
            jsonResponseValue = json.toString(2);
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            jsonResponseValue = " { \"" + SemlibConstants.LOGIN_STATUS + "\": 0 }"; 
        }
                
        if (StringUtils.isNotBlank(callback)) {
            String finalResponse = UtilsManager.getInstance().wrapJSONPResponse(jsonResponseValue, callback);
            return Response.ok(finalResponse).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_JAVASCRIPT).build();
        } else {
            return Response.ok(jsonResponseValue).build();
        }          
    }
    
    
    /**
     * Generate the final response
     * 
     * @param request
     * @param response
     * @param logout 
     */
    private String generateLogoutResponse(boolean logout) throws IOException {
        String strResponse = "{ \"" + SemlibConstants.LOGOUT + "\": ";
        if (logout) {
            strResponse += "1 }";
        } else {
            strResponse += "0 }";
        }
        
        return strResponse;
    }
    
    
    /**
     * Create the final response for the current request
     * 
     * @param callback          the callback function to use (jsonp). <code>null</code> for no JSONP response
     * @param contentType       the response Content-Type
     * @param userData          the annotation data
     * 
     * @return                  the Response with the annotation data
     */
    private Response createFinalResponse(String callback, String contentType, String userData) {
        if (userData == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } else {
            if (StringUtils.isBlank(userData)) {
                return Response.status(Status.NO_CONTENT).build();
            } else if (userData.length() > 0) {
                if (StringUtils.isNotBlank(callback)) {
                    String fResponse = UtilsManager.getInstance().wrapJSONPResponse(userData, callback);
                    return Response.ok(fResponse, contentType).build();
                } else {
                    return Response.ok(userData, contentType).build();
                }
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        }        
    } 
    
}
