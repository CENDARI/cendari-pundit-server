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
 * Helper class for shared costants
 * 
 * @author Michele Nucci
 */
public class SemlibConstants {

    // Internal Status Response
    public enum RequestResponse {

        YES,
        NO,
        OWNER,
        BAD_REQUEST,
        INTERNAL_ERROR,
        RESOURCE_NOT_FOUND,
        REQUEST_OK,
        NO_CONTENT,
        FORBIDDEN
    }
    // Charset ====
    public static final String UTF8 = "UTF-8";
    // General constants ====
    public static final String ID = "id";
    public static final String URI = "uri";
    public static final String OPENID = "openid";
    public static final String FIRST_NAME = "firstName";
    public static final String FIRST_NAME_AX = "firstNameAX";
    public static final String LAST_NAME = "lastName";
    public static final String LAST_NAME_AX = "lastNameAX";
    public static final String FULL_NAME = "fullName";
    public static final String FULL_NAME_AX = "fullNameAX";
    public static final String SCREEN_NAME = "screenName";
    public static final String SCREEN_NAME_AX = "screenNameAX";
    public static final String EMAIL = "email";
    public static final String EMAIL_AX = "emailAX";
    public static final String LOGOUT = "logout";
    public static final String LOGIN_STATUS = "loginStatus";
    public static final String LOGIN_SERVER = "loginServer";
    public static final String REDIRECT_TO = "redirectTo";
    public static final String JSONP_PARAM = "jsonp";
    public static final String QUERY_PARAM = "query";
    public static final String LIMIT_PARAM = "limit";
    public static final String ORDERBY_PARAM = "orderby";
    public static final String DESC_PARAM = "desc";
    public static final String OFFSET_PARAM = "offset";
    public static final String TARGET_PARAM = "target";
    public static final String CONTEXT_PARAM = "context";
    public static final String URL_PARAM = "url";
    public static final String SCOPE_PARAM = "scope";
    public static final String ALL_PARAM_VALUE = "all";
    public static final String ACTIVE_PARAM_VALUE = "active";
    public static final String PUBLIC = "public";
    public static final String PRIVATE = "private";
    // Constants for anonymous user
    public static final String ANONYMOUS_PLAIN_ID = "__SEMLIB__ANONYMOUS USER__";
    public static final String ANONYMOUS = "Anonymous";
    public static final String ANONYMOUS_FULLNAME = "Anonymous User";
    // Constants for public notebook
    public static final String NOTEBOOK_PUBLIC = "Public Notebook";
    // HTTP Headers ====
    public static final String HTTP_HEADER_ACCEPT = "Accept";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_LOCATION = "Location";
    // Cookie constants ====
    public static final int COOKIE_TIME = (24 * 60 * 60); // 1 days for now...
    public static final String COOKIE_PATH = "/";
    public static final String COOCKIE_NAME = "SemLibASToken";
    public static final String COOCKIE_DESCRIPTION = "SemLib Annotation Server Cookie";
    // Auth contants ====
    public static final String LOGGED_USER_ATTR = "loggedUserToken";
    // JSON constants ====
    public static final String JSON_NOTEBOOK_ID = "NotebookID";
    public static final String JSON_NOTEBOOK_IDS = "NotebookIDs";
    public static final String JSON_NOTEBOOK_PUBLIC = "NotebookPublic";
    public static final String JSON_NOTEBOOK_PRIVATE = "NotebookPrivate";
    public static final String JSON_NOTEBOOK_NAME = "NotebookName";
    public static final String JSON_NOTEBOOK_ACTIVE = "NotebookActive";
    public static final String JSON_ANNOTATION_ID = "AnnotationID";
    public static final String JSON_ANNOTATION_IDS = "AnnotationIDs";
    public static final String JSON_RESOURCES = "resources";
    public static final String JSON_TARGETS = "targets";
    public static final String JSON_PAGE_CONTEXT = "pageContext";
    public static final String JSON_GRAPH = "graph";
    public static final String JSON_ITEMS = "items";
    public static final String JSON_METADATA = "metadata";
    public static final String JSON_ANNOTATIONS = "annotations";
    // OpenID constants ====
    public static final String OPENID_IDENTIFIER = "openid_identifier";
    public static final String OPENID_RETURNED = "returned";
    public static final String OPENID_RETURN_PAGE = "return_page";
    public static final String OPENID_DISC = "openid-disc";
    public static final String OPENID_ATTRIBUTE_ERROR = "attribute_error";
    public static final String OPENID_ERROR_MESSAGE = "error_message";
    public static final String OPENID_AUTH_ERROR = "authentication_error";
    public static final String OPENID_USERLOGGED = "userLogged";
    public static final String OPENID_USERID = "userId";
    public static final String OPENID_SCHEMA_URI_FULLNAME = "http://schema.openid.net/namePerson";
    public static final String OPENID_SCHEMA_URI_AX_FULLNAME = "http://axschema.org/namePerson";             // Yahoo
    public static final String OPENID_SCHEMA_URI_FIRSTNAME = "http://schema.openid.net/namePerson/first";
    public static final String OPENID_SCHEMA_URI_AX_FIRSTNAME = "http://axschema.org/namePerson/first";       // Google
    public static final String OPENID_SCHEMA_URI_LASTNAME = "http://schema.openid.net/namePerson/last";
    public static final String OPENID_SCHEMA_URI_AX_LASTNAME = "http://axschema.org/namePerson/last";        // Google    
    public static final String OPENID_SCHEMA_URI_EMAIL = "http://schema.openid.net/contact/email";
    public static final String OPENID_SCHEMA_URI_AX_MAIL = "http://axschema.org/contact/email";          // Google/Yahoo
    public static final String OPENID_SCHEMA_URI_SCREEN_NAME = "http://schema.openid.net/namePerson/friendy";
    public static final String OPENID_SCHEMA_URI_AX_SCREEN_NAME = "http://axschema.org/namePerson/friendly";
    // OpenID constants - error message ====
    public static final String OPENID_ERROR_MSG_UNKNOWN = "Unknown error";
    public static final String OPENID_ERROR_MSG_LOGIN_PARAM_NOT_VALID = "Login parameter not valid!";
    public static final String OPENID_ERROR_MSG_AUTH_ERROR = "Authentication error or OpenID not valid.";
}
