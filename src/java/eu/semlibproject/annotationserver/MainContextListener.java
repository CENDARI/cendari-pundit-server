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

import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Simple Servlet Context Listener for Hybernate
 * 
 * @author Michele Nucci
 */
public class MainContextListener implements ServletContextListener{

    // The Logger
    private Logger logger = Logger.getLogger(MainContextListener.class.getName());
    
    public void contextInitialized(ServletContextEvent event) {                
        logger.log(Level.INFO, "Context Initialization.");
        
        // This is needed to force the parsing of main configuration parameter
        ConfigManager.initInstance(event.getServletContext());
        
        // Create the main hibernate session factory
        HibernateManager.getSessionFactory();
        
        logger.log(Level.INFO, "Context Initialized.");
    }
 
    public void contextDestroyed(ServletContextEvent event) {
        logger.log(Level.INFO, "Destroying Hibernate Context...");
        if (!HibernateManager.getSessionFactory().isClosed()) {
            try {
                HibernateManager.getSessionFactory().close();
                logger.log(Level.INFO, "Hibernate Context Destroyed.");
            } catch (Exception e) {
                logger.log(Level.INFO, "Unable to destroy the Hibernate Context.\n{0}", e.getMessage());
            }            
        }                
    }
}
