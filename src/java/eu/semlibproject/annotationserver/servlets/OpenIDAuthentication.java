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

package eu.semlibproject.annotationserver.servlets;

import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.managers.CookiesManager;
import eu.semlibproject.annotationserver.managers.TokenManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import eu.semlibproject.annotationserver.models.User;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.openid4java.OpenIDException;
import org.openid4java.association.AssociationSessionType;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.*;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

/**
 * A servlet for OpenID Authentication
 * 
 * @author Michele Nucci
 */
public class OpenIDAuthentication extends HttpServlet {

    private ConsumerManager consumerManager;
    
    private String returnPath = "/login.jsp";
    
    private Logger logger = Logger.getLogger(OpenIDAuthentication.class.getName());
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);        
        this.consumerManager = new ConsumerManager();
                
        consumerManager.setAssociations(new InMemoryConsumerAssociationStore());
        consumerManager.setNonceVerifier(new InMemoryNonceVerifier(5000));
        consumerManager.setMinAssocSessEnc(AssociationSessionType.DH_SHA256);
    }
    
    
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String userID    = request.getParameter(SemlibConstants.OPENID_IDENTIFIER);        
        String returned  = request.getParameter(SemlibConstants.OPENID_RETURNED);
        
        String _returnPath = request.getParameter(SemlibConstants.OPENID_RETURN_PAGE);
        if (!StringUtils.isBlank(_returnPath)) {
            returnPath = _returnPath;
        }
                
        if ( (StringUtils.isBlank(userID)) && returned == null ) {        
            request.setAttribute(SemlibConstants.OPENID_ATTRIBUTE_ERROR, "1");
            request.setAttribute(SemlibConstants.OPENID_ERROR_MESSAGE, SemlibConstants.OPENID_ERROR_MSG_LOGIN_PARAM_NOT_VALID);            
            
            RequestDispatcher rd = getServletContext().getRequestDispatcher(returnPath);
            rd.forward(request, response);
            return;
        }
        
        if (returned != null) {
            processReturn(request, response);
        } else {
            authRequest(userID, request, response);
        }                
    }

    
    private void authRequest(String identifier, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        
        try {
            String openIDReturnedURL = request.getRequestURL() + "?" + SemlibConstants.OPENID_RETURNED +"=1";
            
            // perform discovery on the user-supplied identifier
            List discoveries = consumerManager.discover(identifier);
            
            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = consumerManager.associate(discoveries);
            
            // store the discovery information in the user's session
            request.getSession().setAttribute(SemlibConstants.OPENID_DISC, discovered);
            
            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = consumerManager.authenticate(discovered, openIDReturnedURL);
            
            // Add attribute exchange to the requests
            addAttributesExchangeToRequest(authReq);
            
            // This is necessary send a request to an OpenID providers which
            // support popup mode (like Google, etc.).
            String url = authReq.getDestinationUrl(true);
            url = url + "&openid.ns.ui=http%3A%2F%2Fspecs.openid.net%2Fextensions%2Fui%2F1.0&openid.ui.mode=popup";
            
            response.sendRedirect(url);
            
        } catch (OpenIDException ex) {
            request.setAttribute(SemlibConstants.OPENID_AUTH_ERROR, "1");
            request.setAttribute(SemlibConstants.OPENID_ERROR_MESSAGE, ex.toString());
            logger.log(Level.SEVERE, null, ex);
            getServletContext().getRequestDispatcher(returnPath).forward(request, response);
        }        
    }
    
    
    private void processReturn(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {        
        User cUser = this.verifyResponse(req);        
        if (cUser == null || !cUser.isAuthenticated()) {
            req.setAttribute(SemlibConstants.OPENID_AUTH_ERROR, "1");
            req.setAttribute(SemlibConstants.OPENID_ERROR_MESSAGE, SemlibConstants.OPENID_ERROR_MSG_AUTH_ERROR);	
            
            getServletContext().getRequestDispatcher(returnPath).forward(req, resp);
	} else {
            String userID = cUser.getUserID();
                        
            String accessToken = TokenManager.getInstance().generateAndAddNewToken(cUser);
            
            Cookie cookie = CookiesManager.getInstance().generateNewASCookie(accessToken);
            
            resp.addCookie(cookie);
            
            req.setAttribute(SemlibConstants.OPENID_USERLOGGED, "1");
            req.setAttribute(SemlibConstants.OPENID_USERID, userID);
            
            //String urlForRedirect = resp.encodeRedirectURL(returnPath.toString());            
            //resp.sendRedirect(urlForRedirect);
            
	    getServletContext().getRequestDispatcher(returnPath).forward(req, resp);
	}        
    }
    
    
    private User verifyResponse(HttpServletRequest httpReq) throws ServletException {
        try {
            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response = new ParameterList(httpReq.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation) httpReq.getSession().getAttribute(SemlibConstants.OPENID_DISC);

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = httpReq.getRequestURL();
            String queryString = httpReq.getQueryString();
            if (queryString != null && queryString.length() > 0) {
                receivingURL.append("?").append(httpReq.getQueryString());
            }

            // verify the response; ConsumerManager needs to be the same
            // (static) instance used to place the authentication request
            VerificationResult verification = consumerManager.verify(receivingURL.toString(), response, discovered);

            // examine the verification result and extract the verified identifier
            Identifier verified = verification.getVerifiedId();
            if (verified != null) {
                
                User cUser = new User(verified);
                cUser.setAuthenticated(true);
                
                // Compute the user ID
                String openIDIdentifier = verified.getIdentifier();
                String openIDHash = UtilsManager.getInstance().CRC32(openIDIdentifier);                
                cUser.setID(openIDHash);
                
                AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
                parseAttributesExchangeValues(cUser, authSuccess);                
                
                return cUser; // success
            }
        } catch (OpenIDException e) {
            // present error to the user
            throw new ServletException(e);
        }

        return null;
    }
    
    
    private void addAttributesExchangeToRequest(AuthRequest request) throws MessageException {                
        FetchRequest fetch = FetchRequest.createFetchRequest();        
        fetch.addAttribute(SemlibConstants.FIRST_NAME, SemlibConstants.OPENID_SCHEMA_URI_FIRSTNAME, true);
        fetch.addAttribute(SemlibConstants.FIRST_NAME_AX, SemlibConstants.OPENID_SCHEMA_URI_AX_FIRSTNAME, true);
        fetch.addAttribute(SemlibConstants.LAST_NAME, SemlibConstants.OPENID_SCHEMA_URI_LASTNAME, true);
        fetch.addAttribute(SemlibConstants.LAST_NAME_AX, SemlibConstants.OPENID_SCHEMA_URI_AX_LASTNAME, true);
        fetch.addAttribute(SemlibConstants.FULL_NAME, SemlibConstants.OPENID_SCHEMA_URI_FULLNAME, true);
        fetch.addAttribute(SemlibConstants.FULL_NAME_AX, SemlibConstants.OPENID_SCHEMA_URI_AX_FULLNAME, true);
        fetch.addAttribute(SemlibConstants.EMAIL, SemlibConstants.OPENID_SCHEMA_URI_EMAIL, true);
        fetch.addAttribute(SemlibConstants.EMAIL_AX, SemlibConstants.OPENID_SCHEMA_URI_AX_MAIL, true);
        fetch.addAttribute(SemlibConstants.SCREEN_NAME, SemlibConstants.OPENID_SCHEMA_URI_SCREEN_NAME, true);
        fetch.addAttribute(SemlibConstants.SCREEN_NAME_AX, SemlibConstants.OPENID_SCHEMA_URI_AX_SCREEN_NAME, true);
        request.addExtension(fetch);
    }
    
    
    /**
     * Process recevied attribute (attributes exchange) and set the User attribute
     * 
     * @param user
     * @param authSuccess 
     */
    private void parseAttributesExchangeValues(User user, AuthSuccess authSuccess) {
        
        if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
            try {
                MessageExtension ext = authSuccess.getExtension(AxMessage.OPENID_NS_AX);
                if (ext instanceof FetchResponse) {                    
                    
                    FetchResponse fetchResp = (FetchResponse) ext;
                    
                    String firstName   = fetchResp.getAttributeValue(SemlibConstants.FIRST_NAME);
                    String firstNameAX = fetchResp.getAttributeValue(SemlibConstants.FIRST_NAME_AX);
                    if (StringUtils.isNotBlank(firstName)) {
                        user.setFirstName(firstName);
                    } else if (StringUtils.isNotBlank(firstNameAX)) {
                        user.setFirstName(firstNameAX);
                    }
                    
                    String lastName    = fetchResp.getAttributeValue(SemlibConstants.LAST_NAME);
                    String lastNameAX  = fetchResp.getAttributeValue(SemlibConstants.LAST_NAME_AX);
                    if (StringUtils.isNotBlank(lastName)) {
                        if (!lastName.equals(user.getFirstName())) {
                            user.setLastName(lastName);
                        }
                    } else if (StringUtils.isNotBlank(lastNameAX)) {
                        if (!lastNameAX.equals(user.getFirstName())) {
                            user.setLastName(lastNameAX);
                        }                        
                    }
                    
                    String fullName    = fetchResp.getAttributeValue(SemlibConstants.FULL_NAME);
                    String fullNameAX  = fetchResp.getAttributeValue(SemlibConstants.FULL_NAME_AX);
                    if (StringUtils.isNotBlank(fullName)) {
                        user.setFullName(fullName);
                    } else if (StringUtils.isNotBlank(fullNameAX)) {
                        user.setFullName(fullNameAX);
                    }
                    
                    String screenName   = fetchResp.getAttributeValue(SemlibConstants.SCREEN_NAME);
                    String screenNameAX = fetchResp.getAttributeValue(SemlibConstants.SCREEN_NAME_AX);
                    if (StringUtils.isNotBlank(screenName)) {
                        user.setScreenName(screenName);
                    } else if (StringUtils.isNotBlank(screenNameAX)) {
                        user.setScreenName(screenNameAX);
                    }
                                                            
                    
                    List<String> emails  = fetchResp.getAttributeValues(SemlibConstants.EMAIL);
                    List<String> emailsAX = fetchResp.getAttributeValues(SemlibConstants.EMAIL_AX);
                    if (emails.size() > 0) {
                        String mail = emails.get(0);
                        if (StringUtils.isNotBlank(mail)) {
                            user.setEmail(mail);
                        }
                    } else if (emailsAX.size() > 0) {                        
                        String mail = emailsAX.get(0);
                        if (StringUtils.isNotBlank(mail)) {
                            user.setEmail(mail);
                        }
                    }
                }
                
            } catch (MessageException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    
    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

}
