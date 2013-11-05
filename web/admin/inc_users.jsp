<%@page import="eu.semlibproject.annotationserver.AdminDataHelper"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Admins"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@include file="inc_msgbox.jsp" %>
<%
    String userId = request.getParameter("uid");
    
    int itemsForPage   = 15;
    int totNumOfAdmins = -1;
    List<Admins> administrators = null;
%>

<div style="height:5px"></div>

<%
    if (StringUtils.isBlank(userId)) {
        
        totNumOfAdmins = AdminDataHelper.getInstance().getNumOfAdminUsers();
        if (totNumOfAdmins > 0) {
            administrators = AdminDataHelper.getInstance().getAdminsUsers(false, requestedPage, itemsForPage);
%>
<div class="boxed left mrgtop20">
<form class="left" name="mr" action="index.jsp?a=users&c=edit" method="post">
<table class="report" style="width:100%">
    <tr>       
        <th class="w30 borderB borderT"></th>
        <th class="w60 borderB borderT">User ID</th>
        <th class="w250 borderB borderT">Username</th>
        <th class="w250 borderB borderT">Name</th>
    </tr>
    <% for (int i = 0; i < administrators.size(); i++) {  
        Admins admin = administrators.get(i);
        String id = admin.getId().toString();
        String firstName = admin.getFirstname();
        String lastName  = admin.getLastname();
        String name = ((firstName == null) ? "" : firstName) + " " + ((lastName == null) ? "" : lastName);
                
        String addClass = "";
        if (i == administrators.size()-1) {
            addClass="borderB";
        }
    %>
    <tr <% if (i%2 == 0) { %>class="lightGray"<% } %>>
        <td class="center <%= addClass %>"><a href="index.jsp?a=users&c=edit&uid=<%= id %>"><i class="icon-pencil"></i></a></td>
        <td class="<%= addClass %>"><strong><%= id.toUpperCase() %></strong></td>
        <td class="<%= addClass %>"><%= admin.getUsername() %></td>
        <td class="<%= addClass %>"><%= name %></td>
    </tr>
    <% } %>
</table>
    <br/>
    <div id="pagination">
    <%
       if (requestedPage > 1) {
    %>    
    <a class="pageNavi" href="index.jsp?a=users&p=<%=(requestedPage-1) %>"><< Previous Page</a>    
    <% }
        
       if (totNumOfAdmins > 2 && (requestedPage * itemsForPage) < totNumOfAdmins) {
    %>
    <div id="pagi_right" style="float:right">
        <a class="pageNavi" href="index.jsp?a=users&p=<%=(requestedPage+1) %>">Next Page >></a>
    </div>
    <% } %>
    <div class="clear"></div>
    </div>
    
</form>
</div>


<% 
      } else { 
%>            
         <strong>No Notebooks available at the moment!</strong>
<%    }
    } else {
        Admins cUser = AdminDataHelper.getInstance().getAdminUser(userId);
        if (cUser != null) {
%>
<div class="boxed left mrgtop20">

<form name="mr" id="usersform" action="index.jsp?a=users&c=save" method="post">
    <h1 style="font-size:16px;margin:0;padding:0">Edit Administrator Data</h1>
    <input type="hidden" name="userid" value="<%= userId %>" />
    <input type="hidden" name="check" value="<%= cUser.getPassword() %>" />
    <p>
        <label>Username:</label><br/>
        <input type="text" name="username" class="w250 validate[required]" value="<%= cUser.getUsername() %>"/>
    </p>
    <p>
        <label>Password:</label><br/>
        <input type="password" name="password" class="w250 validate[required, minSize[6]]" value="000000"/>
    </p>
    <p>
        <label>First Name:</label><br/>
        <input type="text" name="firstName" class="w250" value="<%= cUser.getFirstname() %>" />
    </p>
    <p>
        <label>Last Name:</label><br/>
        <input type="text" name="lastName" class="w250" value="<%= cUser.getLastname() %>"/>
    </p>
    <p>
        <label>E-mail:</label><br/>
        <input type="text" name="email" class="w250" value="<%= cUser.getEmail() %>"/>
    </p>
    <p>
        <label>OpenID:</label><br/>
        <input type="text" name="openid" class="w250" value="<%= cUser.getOpenid() %>" />
    </p>
    <input class="button-primary" style="margin-top:8px" type="submit" name="submit" value="Update Data" />
</form>
</div>
<%  } else { %>
<strong>Unable to get data about the selected administrator!</strong>
<% } } %>