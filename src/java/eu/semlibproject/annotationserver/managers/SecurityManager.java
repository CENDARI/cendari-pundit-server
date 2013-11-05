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

import eu.semlibproject.annotationserver.security.PermissionsManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * Singleton class implementing the main security manager. From this class 
 * it is possible to access to the current PermissionsManager and to any other
 * methods related to security stuff.
 * 
 * @author Michele Nucci
 */
public class SecurityManager {
    
    // The shared instance of the main SecurityManager
    private static SecurityManager instance     = null;
    
    // The main permission manager
    private PermissionsManager permissionsManager = null;
        
    private Logger logger = Logger.getLogger(RepositoryManager.class.getName());
    
    
    /**
     * Default constructor
     */
    private SecurityManager() {
        
    }
    
    
    /**
     * Get the shard instance of the SecurityManager
     * 
     * @return the shared instnce of the SecurityManager
     */
    public static SecurityManager getInstance() {
        
        if (instance == null) {
            instance = new SecurityManager();
        }
        
        return instance;
    }
    
    
    /**
     * Get the current PermissionManager
     * 
     * @return the current PermissionsManager
     */
    public PermissionsManager getPermissionManager() {
        
        if (permissionsManager == null) {
            String permissionManagerClassName = ConfigManager.getInstance().getCurrentSecurityManagerClassName();
            if (permissionManagerClassName == null) {
                logger.log(Level.SEVERE, "Class name for PermissionManager is null. Check the AnnotationServer configuration file (web.xml).");
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
            
            Object myPermissionManagerClass = UtilsManager.getInstance().createNewInstanceOfClass(permissionManagerClassName);
            if (myPermissionManagerClass instanceof PermissionsManager) {
                permissionsManager = (PermissionsManager)myPermissionManagerClass;
            } else {
                logger.log(Level.SEVERE, "The PermissionsManager class specified in your web.xml file is not valid, it was not possible to instanciate it or some errors have occurred while instaciating it.");
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        
        return permissionsManager;
    }

}
