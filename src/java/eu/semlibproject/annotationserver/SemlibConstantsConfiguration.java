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
 * Helper class for configuration constants
 * 
 * @author Michele Nucci
 */
public class SemlibConstantsConfiguration {
    
    // Annotation Server configuration constants ====
    public final static String PARAM_DB_URL                   = "eu.semlibproject.annotationserver.config.db.url";
    public final static String PARAM_DB_PORT                  = "eu.semlibproject.annotationserver.config.db.port";
    public final static String PARAM_DB_ID                    = "eu.semlibproject.annotationserver.config.db.id";
    public final static String PARAM_DB_USEAUTHENTICATION     = "eu.semlibproject.annotationserver.config.db.useauthentication";
    public final static String PARAM_DB_USERNAME              = "eu.semlibproject.annotationserver.config.db.username";
    public final static String PARAM_DB_PASSWORD              = "eu.semlibproject.annotationserver.config.db.password";
    public final static String PARAM_RDBMS_DRIVER             = "eu.semlibproject.annotationserver.config.rdbms.driver";
    public final static String PARAM_RDBMS_HDIALECT           = "eu.semlibproject.annotationserver.config.rdbms.hibernatedialect";
    public final static String PARAM_RDBMS_CONNECTION_URL     = "eu.semlibproject.annotationserver.config.rdbms.connectionurl";
    public final static String PARAM_RDBMS_USERNAME           = "eu.semlibproject.annotationserver.config.rdbms.username";
    public final static String PARAM_RDBMS_PASSWORD           = "eu.semlibproject.annotationserver.config.rdbms.password";
    public final static String PARAM_HASH_TYPE                = "eu.semlibproject.annotationserver.config.hash";
    public final static String PARAM_NOTEBOOK_DEFAULTSTATUS   = "eu.semlibproject.annotationserver.config.notebooks.defaultStatus";
    public final static String PARAM_RDFREPO_CLASS            = "eu.semlibproject.annotationserver.config.rdfrepositoryclass";
    public final static String PARAM_DATAREPO_CLASS           = "eu.semlibproject.annotationserver.config.datarepositoryclass";
    public final static String PARAM_PERMISSION_CLASS         = "eu.semlibproject.annotationserver.config.securitymanagerclass";
    public final static String PARAM_SERVICES_PROXY_TIMEOUT   = "eu.semlibproject.annotationserver.config.services.proxy.timeout";    
    public final static String PARAM_SERVICES_PROXY_MIMETYPES = "eu.semlibproject.annotationserver.config.services.proxy.mimetypes";
    //smtp
    public final static String PARAM_SMTP_HOST                = "eu.semlibproject.annotationserver.config.mail.smtp.host";
    public final static String PARAM_SMTP_PORT                = "eu.semlibproject.annotationserver.config.mail.smtp.port";
    public final static String PARAM_SMTP_AUTH                = "eu.semlibproject.annotationserver.config.mail.smtp.auth";
    public final static String PARAM_SMTP_USER                = "eu.semlibproject.annotationserver.config.mail.smtp.user";
    public final static String PARAM_SMTP_PASSWORD            = "eu.semlibproject.annotationserver.config.mail.smtp.password";
    // Authentication configuraion constants ====
    public static final String AUTHENTICATION_ENABLED_PARAM    = "eu.semlibproject.annotationserver.config.authentication.enabled";
    public static final String AUTHENTICATION_LOGINFORM_PARAM  = "eu.semlibproject.annotationserver.config.loginform";    
    public static final String AUTHENTICATION_TOKEN_TIME_PARAM = "eu.semlibproject.annotationserver.config.authentication.tokenTime";    

    
    // CORS Filter configuration constants ====
    public static final String CORS_ENABLED_PARAM     = "eu.semlibproject.annotationserver.config.cors.enabled";
    public static final String CORS_ORIGINS_PARAM     = "eu.semlibproject.annotationserver.config.cors.allowOrigin";
    public static final String CORS_METHODS_PARAM     = "eu.semlibproject.annotationserver.config.cors.supportedMethods";
    public static final String CORS_HEADERS_PARAM     = "eu.semlibproject.annotationserver.config.cors.supportedHeaders";
    public static final String CORS_CREDENTIALS_PARAM = "eu.semlibproject.annotationserver.config.cors.supportsCredentials";

}
