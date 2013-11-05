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

import eu.semlibproject.annotationserver.hibernate.Admins;
import eu.semlibproject.annotationserver.hibernate.Annotations;
import eu.semlibproject.annotationserver.hibernate.Emails;
import eu.semlibproject.annotationserver.hibernate.Notebooks;
import eu.semlibproject.annotationserver.managers.HibernateManager;
import eu.semlibproject.annotationserver.managers.UtilsManager;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Data Helper for administration panel
 * 
 * @author Michele Nucci
 */
public class AdminDataHelper {
        
    private static AdminDataHelper instance;
    
    private AdminDataHelper() { }
    
    public static enum SearchingMode {
        ALL,
        ID,
        NID,
        NAME,
        UID,
        UNAME,
        DATE
    }
    
    public static AdminDataHelper getInstance() {
        if (instance == null) {
            instance = new AdminDataHelper();
        }
        
        return instance;
    }
    
    
    public Admins isAdminUser(String username, String password) {

        String sha1Password;
        try {
            sha1Password = UtilsManager.getInstance().SHA1(password);
        } catch (Exception ex) {
            return null;
        }

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();        
        
        hSession.beginTransaction();
        
        Query query = hSession.createQuery("from Admins as admin where (admin.username = :username AND admin.password = :password)");
        query.setParameter("username", username);
        query.setParameter("password", sha1Password);
        
        List<Admins> results = query.list();

        hSession.getTransaction().commit();
        
        return (results.size() > 0) ? results.get(0) : null;
    }
    
        
    public int getNumOfAnnotations(SearchingMode mode, String value) {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query;
            if (mode == SearchingMode.NID) {
                query = hSession.createQuery("select count(*) from Annotations where notebookid = :nid");
                query.setParameter("nid", value);
            } else if (mode == SearchingMode.ID) {
                query = hSession.createQuery("select count(*) from Annotations where annotationid = :aid");
                query.setParameter("aid", value);
            } else {
                query = hSession.createQuery("select count(*) from Annotations");
            }
                        
            int result = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return result;
        } catch (HibernateException e) {
            return -1;
        }
    }
            
    
    public int getNumOfNotebooks(SearchingMode mode, String value) {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            
            Query query;
            if (mode == SearchingMode.ID) {
                query = hSession.createQuery("select count(*) from Notebooks where id = :nid");
                query.setParameter("nid", value);
            } else if (mode == SearchingMode.UID) {
                query = hSession.createQuery("select count(*) from Notebooks where ownerid = :oid");
                query.setParameter("oid", value);
            } else {
                query = hSession.createQuery("select count(*) from Notebooks");                
            }
            
            int result = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return result;
        } catch (HibernateException e) {
            return -1;
        }        
    }
    
    
    public int getNumOfAdminUsers() {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = hSession.createQuery("select count(*) from Admins");
            int result = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return result;
        } catch (HibernateException e) {
            return -1;
        }                
    }
    
    
    public int getNumOfUsers() {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query = hSession.createQuery("select count(*) from Admins");
            int result = ((Long) query.uniqueResult()).intValue();

            hSession.getTransaction().commit();

            return result;
        } catch (HibernateException e) {
            return -1;
        }        
    }
    
            
    public List<Annotations> getAnnotations(boolean all, int pageNumber, int pageSize, SearchingMode searchingMode, String searchingValue, List<String> aidlist) {
        List<Annotations> results = null;
                
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
            
            Query query;
            if (searchingMode == SearchingMode.NID) {
               query = hSession.createQuery("from Annotations where notebookid = :nid");
               query.setParameter("nid", searchingValue);
            } else if (searchingMode == SearchingMode.ID) {
               query = hSession.createQuery("from Annotations where annotationid = :nid");
               query.setParameter("nid", searchingValue);                
            } else if (searchingMode == SearchingMode.UID || searchingMode == SearchingMode.UNAME) {
                query = hSession.createQuery("from Annotations as a where a.annotationid in :aidlist");
                query.setParameterList("aidlist", aidlist);
            } else {
               query = hSession.createQuery("from Annotations");
            }
            
            if (!all) {
                query.setFirstResult(pageSize * (pageNumber -1));
                query.setMaxResults(pageSize);
            }
            
            results = query.list();
            
            hSession.getTransaction().commit();
                        
        } catch (HibernateException he) {
            return null;
        }
        
        return results;            
    }
    
    public List<Notebooks> getNotebooks(boolean all, int pageNumber, int pageSize, SearchingMode searchingMode, String searchingValue, List<String> nids) {
                
        List<Notebooks> results = null;
                
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
        
            Query query;
            if (searchingMode == SearchingMode.ID) {
                query = hSession.createQuery("from Notebooks as n where n.id = :nid");
                query.setParameter("nid", searchingValue);
            } else if (searchingMode == SearchingMode.UID) {
                query = hSession.createQuery("from Notebooks as n where n.ownerid = :uid");
                query.setParameter("uid", searchingValue);
            } else if ((searchingMode == SearchingMode.NAME || searchingMode == SearchingMode.UNAME) && nids != null) {
                query = hSession.createQuery("from Notebooks as n where n.id in :nidlist");
                query.setParameterList("nidlist", nids);
            } else {
                query = hSession.createQuery("from Notebooks");
            }            
            
            if (!all) {
                query.setFirstResult(pageSize * (pageNumber -1));
                query.setMaxResults(pageSize);
            }
            
            results = query.list();
            
            hSession.getTransaction().commit();
                        
        } catch (HibernateException he) {
            return null;
        }
        
        return results;        
    }
    
    public List<Emails> getEmails() {
        List<Emails> results = null;
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();

            Query query;
            query = hSession.createQuery("from Emails");
            results = query.list();
            hSession.getTransaction().commit();

        } catch (HibernateException he) {
            return null;
        }

        return results;

    }

    public Emails getEmailWithId(String emailID) {
        Session hSession=null;
        
        try {  
            hSession = HibernateManager.getSessionFactory().getCurrentSession();   
            hSession.beginTransaction();
            Emails  email  = (Emails) hSession.get(Emails.class, Integer.parseInt(emailID));
            hSession.getTransaction().commit();
            return email;
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            return null;
        }
    }
    
     public boolean saveOrUpdateEmail(Emails email) {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
            hSession.saveOrUpdate(email);
            hSession.getTransaction().commit();
            return true;
        } catch (HibernateException he) {
            return false;
        }        
    }
     
    public boolean deleteEmail(String emailId){
        Emails emailObj = this.getEmailWithId(emailId);
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
            hSession.delete(emailObj);
            hSession.getTransaction().commit();
            return true;
        } catch (HibernateException he) {
            return false;
        } 
    }
    
    public boolean areEmailsValid(String receivers){
        String expression = "([A-Z0-9._%+-]+@(?:[A-Z0-9-]+\\.)+[A-Z]{2,6},?)*";
        Pattern pattern = Pattern.compile(expression,Pattern.CASE_INSENSITIVE); 
        if (receivers.contains(";")) {
            for(String receiver: receivers.split(";")){
                CharSequence inputStr =StringUtils.trim(receiver);
                Matcher matcher = pattern.matcher(inputStr); 
                if (!matcher.matches())
                    return false;
            } return true;
         } else {
              CharSequence inputStr =StringUtils.trim(receivers);
              Matcher matcher = pattern.matcher(inputStr); 
              return matcher.matches();
         }
    }
    
    
    
    public List<Admins> getAdminsUsers(boolean all, int pageNumber, int pageSize) {
                
        List<Admins> results = null;
                
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
        
            Query query = hSession.createQuery("from Admins");
            
            if (!all) {
                query.setFirstResult(pageSize * (pageNumber -1));
                query.setMaxResults(pageSize);
            }
            
            results = query.list();
            
            hSession.getTransaction().commit();
                        
        } catch (HibernateException he) {
            return null;
        }
        
        return results;        
    }

    
    public Admins getAdminUser(String userID) {
        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
        hSession.beginTransaction();
        
        try {            
            Admins adminUser = (Admins) hSession.get(Admins.class, Integer.parseInt(userID));

            hSession.getTransaction().commit();
            
            return adminUser;
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            return null;
        }
    }
    
    
    public boolean saveOrUpdateAdminUser(Admins user) {
        try {
            Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
            hSession.beginTransaction();
            
            hSession.saveOrUpdate(user);

            hSession.getTransaction().commit();
            
            return true;
        } catch (HibernateException he) {
            return false;
        }        
    }
    
    
    public Annotations getAnnotationWithID(String id) {

        Session hSession = HibernateManager.getSessionFactory().getCurrentSession();         
        hSession.beginTransaction();

        try {            
            Annotations ann = (Annotations)hSession.get(Annotations.class, id);            
            hSession.getTransaction().commit();
            return ann;
        } catch (HibernateException he) {
            hSession.getTransaction().rollback();
            return null;
        }
    }
}
