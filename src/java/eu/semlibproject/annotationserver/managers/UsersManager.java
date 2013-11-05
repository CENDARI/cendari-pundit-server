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
import eu.semlibproject.annotationserver.repository.RDFRepository;

/**
 * Singleton class to manage all aspects related to users
 * 
 * @author Michele Nucci
 */
public class UsersManager {
   
    // Shared instance
    private static UsersManager instance = null;
    
    /**
     * Default constructor
     */
    private UsersManager() { }
    
    
    /**
     * Return the shared instance of the UsersManager
     * 
     * @return the shared instance of the UserManager
     */
    public synchronized static UsersManager getInstance() {
        if (instance == null) {
            instance = new UsersManager();
        }
        
        return instance;
    }
    
             
    /**
     * Init or update the user's information
     * 
     * @param userToken the token associated to a logged user     
     * @return          <code>true</code> it the initialization/update process is correctly done
     */
    public boolean initOrUpdateUserInformationOnDB(String userToken) {    
        User currentUser = TokenManager.getInstance().getUserFromToken(userToken);
        return this.initOrUpdateUserInformationOnDB(currentUser);
    }
    
    
    /**
     * Init or update the user's information
     * 
     * @param user the User object containing the users information of a logged user
     * @return     <code>true</code> it the initialization/update process is correctly done
     */
    public boolean initOrUpdateUserInformationOnDB(User user) {
        
        if (user == null) {
            return false;
        }
        
        RDFRepository repository = RepositoryManager.getInstance().getCurrentRDFRepository();
        return repository.initOrUpdateUserInfos(user);
    }
    
}
