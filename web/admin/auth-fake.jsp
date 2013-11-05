<%-- 
    Auth-Fake per supporto test
--%>
<%@page import="eu.semlibproject.annotationserver.SemlibConstants"%>
<%@page import="eu.semlibproject.annotationserver.managers.CookiesManager"%>
<%@page import="eu.semlibproject.annotationserver.managers.TokenManager"%>
<%@page import="eu.semlibproject.annotationserver.managers.UtilsManager"%>
<%@page import="eu.semlibproject.annotationserver.models.User"%>
<%@page import="org.openid4java.discovery.Identifier"%>
<%!
    private static final User _createFakeUser(final String userId) {
        System.out.println("\n->_createFakeUser("+userId+")");
        Identifier verified = new Identifier() {
            public String getIdentifier(){
                return userId;
            }
        };
        System.out.println("  verified          : "+verified);
        User user = new User(verified);
        System.out.println("  user              : "+user);
        // Compute the user ID
        String openIDIdentifier = new String(userId);
        System.out.println("  openIDIdentifier  : "+openIDIdentifier);
        String openIDHash = UtilsManager.getInstance().CRC32(openIDIdentifier);                
        user.setID(openIDHash);
        // ExchangeValues
        user.setFirstName("_FAKE_");
        user.setLastName(userId);
        user.setFullName(user.getFirstName()+" "+user.getLastName());
        user.setScreenName(user.getFullName());
        user.setEmail(userId+"@fake.mail");
        // Auth
        //user.setAuthenticated(true);
        //user.setAccessToken(userIdHash + "_" + userId);
        //TokenManager.getInstance().generateAndAddNewToken(user);                
        return user;
    }
%>
<%
    String returnPath = "/login.jsp";
    String userId     = request.getParameter("userId");

    User user = null;

    boolean authFake = true;
    if (authFake) {
        user = _createFakeUser(userId);
        
        // Auth
        user.setAuthenticated(true);
        String accessToken = TokenManager.getInstance().generateAndAddNewToken(user);
        Cookie cookie = CookiesManager.getInstance().generateNewASCookie(accessToken);
        response.addCookie(cookie);
        request.setAttribute(SemlibConstants.OPENID_USERLOGGED, "1");
        request.setAttribute(SemlibConstants.OPENID_USERID, user.getUserID());
            
        //String urlForRedirect = resp.encodeRedirectURL(returnPath.toString());            
        //resp.sendRedirect(urlForRedirect);
        getServletContext().getRequestDispatcher(returnPath).forward(request, response);
        return;
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>Auth Fake</h1>
    </body>
</html>
