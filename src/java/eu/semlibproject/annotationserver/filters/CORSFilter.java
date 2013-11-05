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

import eu.semlibproject.annotationserver.SemlibConstantsConfiguration;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Basic implementation of a CORS filter
 * 
 * @author Michele Nucci
 */
public class CORSFilter extends SemlibFilter {

    // Default configuration values...
    private boolean filterEnabled      = true;
    private String allowOrigin         = "*";
    private String supportedMethods    = "HEAD, GET, POST, PUT, DELETE, OPTIONS";
    private String supportedHeaders    = "Content-Type, X-Requested-With, Accept";
    private String supportsCredentials = "true";
    
    
    public void init(FilterConfig filterConfig) throws ServletException {        
        
        super.initSemlibFilter(filterConfig);
        
        String _cEnabled = filterConfig.getInitParameter(SemlibConstantsConfiguration.CORS_ENABLED_PARAM);
        if (_cEnabled != null) {
            if (_cEnabled.equalsIgnoreCase("yes")) {
                filterEnabled = true;
            } else if (_cEnabled.equalsIgnoreCase("no")) {
                filterEnabled = false;
            }
        }
        
        String _origin = filterConfig.getInitParameter(SemlibConstantsConfiguration.CORS_ORIGINS_PARAM);
        if (StringUtils.isNotBlank(_origin)) {
            allowOrigin = _origin;
        }
        
        String _methods =  filterConfig.getInitParameter(SemlibConstantsConfiguration.CORS_METHODS_PARAM);
        if (StringUtils.isNotBlank(_methods)) {
            supportedMethods = _methods;
        }
        
        String _headers = filterConfig.getInitParameter(SemlibConstantsConfiguration.CORS_HEADERS_PARAM);
        if (StringUtils.isNotBlank(_headers)) {
            supportedHeaders = _headers;
        }
        
        String _credentials = filterConfig.getInitParameter(SemlibConstantsConfiguration.CORS_CREDENTIALS_PARAM);
        if (_credentials != null) {
            if (_credentials.equalsIgnoreCase("yes")) {
                supportsCredentials = "true";
            } else if (_credentials.equalsIgnoreCase("no")) {
                supportsCredentials = "false";
            }
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {        
        if (filterEnabled) {
            HttpServletResponse resp = (HttpServletResponse) response;
            HttpServletRequest req = (HttpServletRequest) request;
            
            // Try to set the servlet context path (used to overcome
            // limit of servlet containers that does not support sevlet
            // specification >= 2.5
            super.setServletContextPath(req);
            
            String origin = req.getHeader("Origin");
            if (origin != null && !origin.equals("")) {
                allowOrigin = origin;
            }
            
            resp.addHeader("Access-Control-Allow-Origin", allowOrigin);
            resp.addHeader("Access-Control-Allow-Methods", supportedMethods);
            resp.addHeader("Access-Control-Allow-Headers", supportedHeaders);
            resp.addHeader("Access-Control-Allow-Credentials", supportsCredentials);
            chain.doFilter(request, resp);
        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
        
    }    
}
