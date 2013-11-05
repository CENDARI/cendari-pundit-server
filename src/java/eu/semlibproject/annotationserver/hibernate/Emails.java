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
package eu.semlibproject.annotationserver.hibernate;
// Generated 25-set-2012 10.31.34 by Hibernate Tools 3.2.1.GA


public class Emails  implements java.io.Serializable {


     private Integer id;
     private String label;
     private String receivers;

    public Emails() {
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer ID){
        this.id= ID;
    }
    
    public String getLabel(){
        return label;
    }
    
    
    public void setLabel(String label){
        this.label = label;
    }

    public String getReceivers(){
        return receivers;
    }
    
    public void setReceivers(String receivers){
        this.receivers=receivers;
    }

}


