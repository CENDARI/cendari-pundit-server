<%@page import="eu.semlibproject.annotationserver.hibernate.Emails"%>
<%@page import="eu.semlibproject.annotationserver.managers.UtilsManager"%>
<%@page import="eu.semlibproject.annotationserver.models.Notebook"%>
<%@page import="java.util.ArrayList"%>
<%@page import="javax.ws.rs.core.Response.Status"%>
<%@page import="eu.semlibproject.annotationserver.managers.RepositoryManager"%>
<%@page import="org.apache.http.HttpRequest"%>
<%@page import="eu.semlibproject.annotationserver.AdminDataHelper"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@ page language="java" %>
<%!
   // JSP Methods ==================

   boolean checkUserAuth(HttpSession session) {
       String auth = (String)session.getAttribute("auth");
       if (auth != null && auth.equals("1")) {
           Admins admin = (Admins)session.getAttribute("user");
           if (admin == null) {
               return false;
           } else {
               String username = admin.getUsername();
               if (!StringUtils.isBlank(username)) {
                    return true;
               }
           }
       }
       
       return false;
   }
   
   Admins isValidUser(String username, String password) {       
       return AdminDataHelper.getInstance().isAdminUser(username, password);
   }   
%>
<%
    boolean loginError        = false;    
    boolean userAuthenticated = false;
    boolean commandError      = false;
    String  commandMessage    = null;    
    String  msgError          = null;
           
    String action     = request.getParameter("a");
    String rpage      = request.getParameter("p");
    String command    = request.getParameter("c");
    String uid        = request.getParameter("uid");
    
    int requestedPage = 1;
    if (StringUtils.isNotBlank(rpage)) {
        try {
            requestedPage = Integer.parseInt(rpage);
        } catch (Exception e) {
            requestedPage = 1;
        }
    }
    
    if (StringUtils.isBlank(action)) {
        action = "home";
    } else if (action.equalsIgnoreCase("logout")) {
        session.removeAttribute("auth");
        session.removeAttribute("user");
    } else if (action.equalsIgnoreCase("annotations")) {
        
        if (command != null && command.equalsIgnoreCase("delete")) {
            String id    = request.getParameter("id");
            String[] ids = request.getParameterValues("ids");
            
            RepositoryManager repoManager = RepositoryManager.getInstance();
            
            if (StringUtils.isNotBlank(id)) {                
                
                boolean annotationExists = repoManager.getCurrentDataRepository().annotationExists(id.toLowerCase());
                if (!annotationExists) {
                    commandError   = true;
                    commandMessage = "The Annotation '" + id.toUpperCase() + "' has not been found,";                    
                } else {
                    Status result = repoManager.deleteAnnotation(id.toLowerCase());
                    if (result == Status.INTERNAL_SERVER_ERROR) {
                        commandError   = true;
                        commandMessage = "Unable to delete Annotation '" + id.toUpperCase() + "'. Internal Server Error.";
                    } else if (result == Status.OK) {
                        commandError   = false;
                        commandMessage = "Annotation '" + id.toUpperCase() + "' has been deleted!";
                    }
                }                                       
            } else if (ids != null && ids.length > 0) {
                for (int i = 0; i < ids.length; i++) {
                    String cId = ids[i].toLowerCase();
                    
                    boolean annotationExists = repoManager.getCurrentDataRepository().annotationExists(cId);
                    if (!annotationExists) {
                        if (!commandError) {
                            commandError = true;
                            commandMessage = "Unable to delete some of the selected Annotations";
                        }                                               
                    } else {
                        Status result = repoManager.deleteAnnotation(cId);
                        if (result != Status.OK) {
                            if (!commandError) {
                                commandError = true;
                                commandMessage = "Unable to delete some of the selected Annotations";
                            }
                        } 
                    }                    
                }
                
                if (!commandError) {
                    commandMessage = "Annotations deleted!";
                }
            }                       
        }
               
    } else if (action.equalsIgnoreCase("notebooks")) {

        if (command != null && command.equalsIgnoreCase("delete")) {
            String id    = request.getParameter("id");
            String[] ids = request.getParameterValues("ids");
            
            RepositoryManager repoManager = RepositoryManager.getInstance();
            
            if (StringUtils.isNotBlank(id)) {
                                
                boolean notebookExists = repoManager.getCurrentDataRepository().notebookExists(id.toLowerCase());
                if (!notebookExists) {
                    commandError   = true;
                    commandMessage = "The Notebook " + id.toUpperCase() + " has not been found,";                    
                } else {
                    Notebook notebook = Notebook.getEmptyNotebookObject();
                    notebook.setID(id.toLowerCase());

                    Status result = repoManager.deleteNotebook(notebook);
                    if (result == Status.INTERNAL_SERVER_ERROR) {
                        commandError   = true;
                        commandMessage = "Unable to delete Notebook '" + id.toUpperCase() + "'. Internal Server Error.";
                    } else if (result == Status.OK) {
                        commandError   = false;
                        commandMessage = "The Notebook '" + id.toUpperCase() + "' has been deleted!";
                    }                    
                }                
            } else if (ids != null && ids.length > 0) {
                for (int i = 0; i < ids.length; i++) {
                    String cId = ids[i].toLowerCase();
                    
                    boolean notebookExists = repoManager.getCurrentDataRepository().notebookExists(cId);
                    if (!notebookExists) {
                        if (!commandError) {
                            commandError = true;
                            commandMessage = "Unable to delete some of the selected Notebooks";
                        }                        
                    } else {
                        Notebook notebook = Notebook.getEmptyNotebookObject();
                        notebook.setID(cId);

                        Status result = repoManager.deleteNotebook(notebook);
                        if (result != Status.OK) {
                            if (!commandError) {
                                commandError = true;
                                commandMessage = "Unable to delete some of the selected Notebooks";
                            }
                        }                                                                      
                    }                    
                }
                
                if (!commandError) {
                    commandMessage = "Notebook deleted!";
                }
            }                       
        }             
    } else if (action.equalsIgnoreCase("users")) {
        
        if (command != null && command.equalsIgnoreCase("save")) {
            String _userId       = request.getParameter("userid");
            String _checkV       = request.getParameter("check");
            String _username     = request.getParameter("username");
            String _password     = request.getParameter("password");
            String _firstName    = request.getParameter("firstName");
            String _lastName     = request.getParameter("lastName");
            String _email        = request.getParameter("email");
            String _openid       = request.getParameter("openid");
            
            if (StringUtils.isBlank(_userId) || StringUtils.isBlank(_username) || StringUtils.isBlank(_password) || StringUtils.isBlank(_checkV)) {
                commandError = true;
                commandMessage = "Unable to save/update administration data. Some fields are incorrect!";
            } else {
                Admins cadmin = new Admins();
                cadmin.setId(Integer.parseInt(_userId));
                cadmin.setUsername(_username);
                cadmin.setFirstname(_firstName);
                cadmin.setLastname(_lastName);
                cadmin.setEmail(_email);
                cadmin.setOpenid(_openid);
                
                if (_password.equalsIgnoreCase("000000")) {
                    cadmin.setPassword(_checkV);
                } else {
                    cadmin.setPassword(UtilsManager.getInstance().SHA1(_password));
                }
                
                boolean result = AdminDataHelper.getInstance().saveOrUpdateAdminUser(cadmin);
                if (!result) {
                    commandError = true;
                    commandMessage = "Unable to save/update administration data. Maybe some fields are incorrect!";                    
                } else {
                    commandMessage = "Data updated correctly!";
                }
            }
        }
    } else if (action.equalsIgnoreCase("emails")) {
        
        if (command != null && command.equalsIgnoreCase("save")) {
            String _emailId       = request.getParameter("emailid");
            String _label       = request.getParameter("label");
            String _receivers     = request.getParameter("receivers");
            
            if ( StringUtils.isBlank(_label) || StringUtils.isBlank(_receivers) ) {
                commandError = true;
                commandMessage = "Unable to save/update administration data. Some fields are incorrect!";
            } else {
                if (AdminDataHelper.getInstance().areEmailsValid(_receivers)) {
                    Emails email = new Emails();
                    if (!StringUtils.isBlank(_emailId) && _emailId.length()>0)
                        email.setId(Integer.parseInt(_emailId));
                    if (!StringUtils.isBlank(_label) && _label.length()>0)
                        email.setLabel(StringUtils.trim(_label));
                    if (!StringUtils.isBlank(_receivers) && _receivers.length()>0)
                        email.setReceivers(StringUtils.trim(_receivers));

                    boolean result = AdminDataHelper.getInstance().saveOrUpdateEmail(email);
                    if (!result) {
                        commandError = true;
                        commandMessage = "Unable to save/update email data. Maybe some fields are incorrect!";                    
                    } else {
                        commandMessage = "Data updated correctly!";
                    }
                } else { 
                    commandError = true; 
                    commandMessage = "Unable to save/update email data. receivers are incorrect!";  
                
                }
                
            }
        } else if (command != null && command.equalsIgnoreCase("new")) {
           
        } else if (command != null && command.equalsIgnoreCase("delete")) {
                String _emailId       = request.getParameter("uid");
                
                boolean result = AdminDataHelper.getInstance().deleteEmail(_emailId);
                if (!result) {
                    commandError = true;
                    commandMessage = "Unable to save/update email data. Maybe some fields are incorrect!";                    
                } else {
                    commandMessage = "Data updated correctly!";
                }
        } 
        
    }
    
    String username = request.getParameter("asuser");
    String password = request.getParameter("aspassword");
    
    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
        Admins validUser = isValidUser(username, password);
        if (validUser != null) {
            userAuthenticated = true;
            session.setAttribute("auth", "1");
            session.setAttribute("user", validUser);
        } else {
            loginError = true;
            userAuthenticated = false;
            msgError = "Username or password not valid!";
        }
    } else {
        userAuthenticated = checkUserAuth(session);
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Pundit - Annotation Server Administration</title>
        <link rel="stylesheet" href="../css/bootstrap.css" />
        <link rel="stylesheet" href="../css/admin.css" />
        <% if (action.equalsIgnoreCase("users")) { %>
        <link rel="stylesheet" href="../css/validationEngine.jquery.css"/>
        <% } %>
    </head>
    <body>
        <div class="navbar navbar-fixed-top">
            <div class="navbar-inner">
                <div class="container">
                    <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </a>
                    <img class="brand" src="../images/punditlogo.png" width="141" alt="Pundit Logo" style="display:block;padding-top:3px" />
                    <% if (userAuthenticated) { %>
                    <%@ include file="inc_navbar.jsp" %>
                    <% } %>
                </div>
            </div>            
        </div>
        
        <% if (!userAuthenticated) { %>
        <%@include file="inc_login.jsp" %>
        <% } else { %>
        <div id="main_content" style="text-align:center">
            <%
                if (action.equalsIgnoreCase("home")) {
            %>
            <%@include file="inc_home.jsp" %>
            <%                                                        
                } else if (action.equalsIgnoreCase("annotations")) {
            %>
            <%@include file="inc_annotations.jsp" %>
            <%        
                } else if (action.equalsIgnoreCase("notebooks")) {
            %>
            <%@include file="inc_notebooks.jsp" %>
            <%        
                } else if (action.equalsIgnoreCase("users")) {
            %>
            <%@include file="inc_users.jsp" %>
            <%
                } else if (action.equalsIgnoreCase("emails")) {
            %>
            <%@include file="inc_emails.jsp" %>
            <%
                }
            %>
        </div>
        <% } %>
    </body>
    <% if (action.equalsIgnoreCase("home") || action.equalsIgnoreCase("logout") || action.equalsIgnoreCase("users")) { %>
    <script src="../js/jquery-1.8.2.min.js" type="text/javascript"></script>
    <% } 
       if (action.equalsIgnoreCase("users")) { %>
        <script src="../js/jquery.validationEngine-en.js" type="text/javascript"></script>
        <script src="../js/jquery.validationEngine.js" type="text/javascript"></script>
        <script>
            $(document).ready(function(){
                $("#usersform").validationEngine();
            });
    </script>
    <% } %>
    <% if (!userAuthenticated && (action.equalsIgnoreCase("home") || action.equalsIgnoreCase("logout"))) { %>
    <script>
        $(document).ready(function() {
           $("#asuser").focus(); 
        });
    </script>
    <% } %>
</html>
