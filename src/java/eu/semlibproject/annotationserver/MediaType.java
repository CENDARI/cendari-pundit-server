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

/**
 * Defines additional MimeType not defined by the original class
 * 
 * @author Michele Nucci
 */
public class MediaType extends javax.ws.rs.core.MediaType {    
    public static final String APPLICATION_JAVASCRIPT = "application/javascript";
    public static final String APPLICATION_RDFXML     = "application/rdf+xml";
    public static final String TEXT_RDFN3             = "text/rdf+n3";
    public static final String TEXT_TURTLE            = "text/turtle";    
    public static final String TEXT_PLAIN_UTF8        = "text/plain; charset=UTF-8";
    public static final String APPLICATION_RDFJSON    = "application/rdf+json charset=UTF-8";
}
