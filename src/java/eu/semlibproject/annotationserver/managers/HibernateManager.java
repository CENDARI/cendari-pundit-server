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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * Singleton/Helper class for Hybernate
 * 
 * @author Michele Nucci
 */
public class HibernateManager {

    private static final SessionFactory sessionFactory;
    private static final ServiceRegistry serviceRegistry;
    
    // The Logger
    private static Logger logger = Logger.getLogger(HibernateManager.class.getName());
            
            
    static {
        try {
            Configuration configuration = new Configuration();
            configuration.configure();
            
            // Base configuration parameter read from web.xml
            configuration.setProperty("hibernate.dialect", ConfigManager.getInstance().getRDBMSDialect());
            configuration.setProperty("hibernate.connection.driver_class", ConfigManager.getInstance().getRDBMSDriver());
            configuration.setProperty("hibernate.connection.url", ConfigManager.getInstance().getRDBMSConnectionUrl());
            configuration.setProperty("hibernate.connection.username", ConfigManager.getInstance().getRDBMSUsername());
            configuration.setProperty("hibernate.connection.password", ConfigManager.getInstance().getRDBMSPassword());
            
            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, "Unable to intialize Hybernate Session Factory.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
 
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
