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

import eu.semlibproject.annotationserver.managers.ConfigManager;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * Base interface for all Semlib Servlet Filter
 * 
 * @author Michele Nucci
 */
public abstract class SemlibFilter implements Filter {    
    
    public void initSemlibFilter(FilterConfig filterConfig) {
        ServletContext servletContext = filterConfig.getServletContext();
        if (servletContext != null) {
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.setMainServletContext(servletContext);
        }        
    }
        
    public void setServletContextPath(HttpServletRequest request) {        
        ConfigManager.getInstance().setServletContextPath(request.getContextPath());
    }
}
