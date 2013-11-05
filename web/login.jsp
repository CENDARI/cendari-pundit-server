<%-- 
    Document   : login.jsp
    Created on : 11-nov-2011, 11.59.25
    Author     : Michele Nucci
--%>
<%@page import="eu.semlibproject.annotationserver.SemlibConstants"%>
<%@page import="java.util.*" contentType="text/html" pageEncoding="UTF-8"%>
<%     
    String mainContext = request.getContextPath();    
    
    String returnedPage = request.getRequestURI();
    int indexLastSlash = returnedPage.lastIndexOf("/");
    returnedPage = returnedPage.substring(indexLastSlash);        
                    
    // TODO: minimal implementation for now...
    boolean oidUserDefined = false;
    boolean userLogged     = false;
    boolean onError        = false;
    
    String errorMsg  = null;
    String attError  = (String) request.getAttribute(SemlibConstants.OPENID_AUTH_ERROR);
    String oidUser   = (String) request.getParameter(SemlibConstants.OPENID_DISC);    
    
    String strUserLogged = (String) request.getAttribute(SemlibConstants.OPENID_USERLOGGED);
    String strUserID     = (String) request.getAttribute(SemlibConstants.OPENID_USERID);
    
    String serverAddress = request.getScheme() + "://" + request.getServerName();
    int serverPort = request.getServerPort();
    if (serverPort != 80) {
        serverAddress += ":" + Integer.toString(serverPort);
    }                                

    String attMsgError = (String) request.getAttribute(SemlibConstants.OPENID_ERROR_MESSAGE);
    if (attMsgError == null && attError != null && attError.equals("1")) {
        attMsgError = SemlibConstants.OPENID_ERROR_MSG_UNKNOWN;
    }
        
    if ((attError != null && attError.equals("1"))) {
        onError = true;
        errorMsg = "<div class='error'><strong>Error</strong>: " + attMsgError + "</div>";
    } else {
        if (oidUser != null && !oidUser.equals("")) {
            oidUserDefined = true;
        }
        
        if ( (strUserID != null && !strUserID.equals("")) && "1".equals(strUserLogged) ) {
            userLogged = true;
        }        
    }       
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Semlib Annotation System - Login</title>
        <link type="text/css" rel="stylesheet" href="css/openid.css" />
        <script src="http://ajax.googleapis.com/ajax/libs/dojo/1.6.0/dojo/dojo.xd.js"></script>
        <script type="text/javascript" src="js/openid-selector.js" ></script>
        <script>            
        <% if (!oidUserDefined) { %>        
		dojo.ready(function(){                   
                    openid.init('<%= SemlibConstants.OPENID_DISC %>');
		});	
        <% } else { %>
                dojo.ready(function() { 
                    var form = dojo.byId("oidRedirectionForm");
                    setTimeout(form.submit(), 3000);
                });
        <% } %>
        </script>
    </head>
    
    <body class="hnomargin">
        <p>ssssssssssssssssss</p>
        <div id="header">
            <div class="ccontainer">
                <div class="middlecontent">
                    <h1 class="htitle">Sign-In</h1>
                </div>
            </div>                        
        </div>
        <div class="line"></div>
        <% if (oidUserDefined && !userLogged && !onError) { // autoredirect %>
        <form id="oidRedirectionForm" method="post" action="<%= serverAddress %><%= mainContext %>/openidauthentication">
            <fieldset class="fmt">
                <legend>Contacting your OpenID provider...</legend>
                <input type="hidden" name="<%= SemlibConstants.OPENID_IDENTIFIER %>" value="<%= oidUser %>" />
                <input type="hidden" name="<%= SemlibConstants.OPENID_RETURN_PAGE %>" value="<%= returnedPage %>" />
            </fieldset>
        </form>
        <% } else if (userLogged && !onError) { // user already logged %>
        <script>
            window.close();
        </script>
        <% } else { // show the login form%
                if (onError) { out.print(errorMsg); } 
        %>
        <form action="<%= request.getRequestURL() %>" method="post" id="openid_form">
            <input type="hidden" name="action" value="verify" />
            <input type="hidden" name="fredirection_form" value="1" />
            <input id="openid_form_submit" type="submit" style="display: none;" />
            <fieldset class="fmt">
                <legend>Sign-in with an existing account</legend>
                <div id="openid_choice">
                    <div id="openid_btns"></div>
                </div>
                <div id="openid_input_area">
                    <input id="openid_identifier" name="<%= SemlibConstants.OPENID_DISC %>" type="text" value="http://" />
                    <input id="openid_submit" type="submit" value="Sign-In"/>
		</div>
            </fieldset>                
        </form>
        <% } %>     
    </body>    
</html>
