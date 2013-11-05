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

package eu.semlibproject.annotationserver.filters;

import eu.semlibproject.annotationserver.MediaType;
import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.SemlibConstantsConfiguration;
import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.CookiesManager;
import eu.semlibproject.annotationserver.managers.TokenManager;
import eu.semlibproject.annotationserver.managers.UsersManager;
import eu.semlibproject.annotationserver.models.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Implementation of a basic Authentication Filter
 * 
 * @author Michele Nucci
 */
public class AuthFilter extends SemlibFilter {
    
    // Default values    
    private boolean authenticationEnabled  = false;
    private boolean authenticationLoginFormSet = false;
    private String authenticationLoginFrom = "/login.jsp";   
    private ArrayList<String> noAuthAPI    = null;
    
    // The Logger
    private Logger logger = Logger.getLogger(AuthFilter.class.getName());
        
    public void init(FilterConfig filterConfig) throws ServletException {                        
        
        super.initSemlibFilter(filterConfig);        
        
        String openIDEnabled = filterConfig.getInitParameter(SemlibConstantsConfiguration.AUTHENTICATION_ENABLED_PARAM);
        if (openIDEnabled != null) {
            if (openIDEnabled.equalsIgnoreCase("yes")) {
                authenticationEnabled = true;                                                               
            } else if (openIDEnabled.equalsIgnoreCase("no")) {
                authenticationEnabled = false;
            }
            
            String loginForm = filterConfig.getInitParameter(SemlibConstantsConfiguration.AUTHENTICATION_LOGINFORM_PARAM);
            if (StringUtils.isNotBlank(loginForm)) {
                if (!loginForm.startsWith("http://")) {
                    // we have a relative path
                    if (!loginForm.startsWith("/")) {
                        loginForm = "/" + loginForm;
                    }
                }

                authenticationLoginFrom = loginForm;
            }

            String sTokenTime = filterConfig.getInitParameter(SemlibConstantsConfiguration.AUTHENTICATION_TOKEN_TIME_PARAM);
            if (sTokenTime != null) {
                try {
                    long tTockenTime = Long.parseLong(sTokenTime);
                    TokenManager.getInstance().setTokenTimeValidity(tTockenTime);
                } catch (Exception e) {
                    // do nothing...use default value
                }
            }
            
            // Specify here a list of API that does not require any authentication
            // and any fake authentication (anonym user)
            if (noAuthAPI == null) {
                noAuthAPI = new ArrayList<String>();
                noAuthAPI.add("/users/current");
                noAuthAPI.add("/users/logout");
            }            
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {                
                        
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse) response;                            
        
        // Try to set the servlet context path (used to overcome
        // limit of servlet containers that does not support sevlet
        // specification >= 2.5
        super.setServletContextPath(httpRequest);
            
        // This is a workaround for Tomcat 5.x. A better option would be to make this setting
        // in filter initialization but in this case Tomcat 5.x throw an exception 'cos
        // getServletContext.getContextPath is not supported in Servlet specfication 2.4 (Tomcat 5.x).
        if (!authenticationLoginFormSet) {
            authenticationLoginFormSet = true;                                                                                                            
            ConfigManager.getInstance().setAuthenticationFormPath(authenticationLoginFrom);            
        }
        
        String requestedMethods = httpRequest.getMethod();
        String cookieValue      = CookiesManager.getInstance().isUserAuthorizedFromCookie(httpRequest, resp);                
        String requestedPath    = httpRequest.getPathInfo();
        
        boolean openApi = false;
        if (requestedPath != null && requestedPath.contains("/open/")) {
            openApi = true;
        }
        
        // skip this part if authentication is not enabled and if we are a preflight request
        if ( authenticationEnabled && !"OPTIONS".equalsIgnoreCase(requestedMethods) && !isANoAuthAPI(httpRequest) && !openApi) {
                                                           
            if (request instanceof HttpServletRequest) {
                
                boolean authorized = false;
                String acceptedFormat = httpRequest.getHeader(SemlibConstants.HTTP_HEADER_ACCEPT);
                
                // Check the cookie to see if the user is authorized or not                                
                if (StringUtils.isNotBlank(cookieValue)) {
                    authorized = true;
                }
                
                if (!authorized) {
                    if (acceptedFormat != null && acceptedFormat.contains(MediaType.APPLICATION_JSON)) {
                        // Generate the JSON redirect response
                        try {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put(SemlibConstants.REDIRECT_TO, authenticationLoginFrom);

                            resp.setContentType(MediaType.APPLICATION_JSON);
                            resp.setCharacterEncoding(SemlibConstants.UTF8);

                            PrintWriter out = resp.getWriter();
                            out.print(jsonData.toString());
                        } catch (JSONException ex) {
                            logger.log(Level.SEVERE, null, ex);
                            resp.sendError(500);
                        }
                    } else {
                        // send a normal 302 response                        
                        resp.sendRedirect(authenticationLoginFrom);
                    }                    
                } else {
                    
                    // Pass the token of the current logged user to the APIs                    
                    request.setAttribute(SemlibConstants.LOGGED_USER_ATTR, cookieValue);                    
                    
                    User currentUser = TokenManager.getInstance().getUserFromToken(cookieValue);
                    if (!currentUser.isAlreadyCheckedForRepository()) {
                        // Init or update the users information (into the DB)
                        boolean correctlyInitiliazed = UsersManager.getInstance().initOrUpdateUserInformationOnDB(cookieValue);
                        currentUser.setCheckedForRepository(correctlyInitiliazed);
                    }                    
                    
                    // The user is authenticated/authorized
                    chain.doFilter(request, response);
                }                
                
            } else {
                // Return a bad request                
                resp.sendError(400);
            }
            
        } else {
            
            if (!"OPTIONS".equalsIgnoreCase(requestedMethods)) {
                if (StringUtils.isNotBlank(cookieValue)) {
                    request.setAttribute(SemlibConstants.LOGGED_USER_ATTR, cookieValue);
                } else if (!authenticationEnabled && !isANoAuthAPI(httpRequest) && !openApi) {
                    // In this case we need to create an anonymous user
                    User anonUser = User.createAnonymousUser();
                    request.setAttribute(SemlibConstants.LOGGED_USER_ATTR, anonUser.getAccessToken());
                }
            }                        

            chain.doFilter(request, response);
        }
    }

    public void destroy() {
        // throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    /**
     * Check if the request for an open API (authentication not required)
     * 
     * @param request   the httpServletRequest
     * @return          <code>true</code> is the requested API is open
     */
    private boolean isANoAuthAPI(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return noAuthAPI.contains(pathInfo);
    }
}
