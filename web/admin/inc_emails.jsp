<%@page import="eu.semlibproject.annotationserver.AdminDataHelper"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Emails"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@include file="inc_msgbox.jsp" %>
<div style="height:5px"></div>

<%
    String emailId = request.getParameter("uid");
    String c = request.getParameter("c");
    if (StringUtils.isBlank(emailId) || c.equals("delete")) {
%>
<div class="boxed left mrgtop20">
<form class="left" name="mr" action="index.jsp?a=users&c=edit" method="post">
<table class="report" style="width:100%">
    <tr>       
        <th class="w30 borderB borderT"></th>
        <th class="w60 borderB borderT">Label</th>
        <th class="w250 borderB borderT">Receivers</th>
        
    </tr>
    <% 
    List<Emails> lista_email= AdminDataHelper.getInstance().getEmails();
    for(Emails email : lista_email){
        
        String id = email.getId().toString();
        String label = email.getLabel();
        String receivers  = email.getReceivers();
        String  addClass="borderB";      
    %>
    <tr class="lightGray">
        <td class="center <%= addClass %>">
            <a href="index.jsp?a=emails&c=edit&uid=<%= id %>"><i class="icon-pencil"></i></a>
            <a href="index.jsp?a=emails&c=delete&uid=<%= id %>"><i class="icon-remove"></i></a>
        </td>
        <td class="<%= addClass %>"><strong><%= label  %></strong></td>
        <td class="<%= addClass %>"><%= receivers %></td>
    </tr>
    <% } %>
   
</table>
    </form>
    <form class="left" name="mr" action="index.jsp?a=emails&c=new&uid=999999" method="post">
    <input class="button-primary" style="margin-top:8px" type="submit" name="add" value="Add receivers" />
    </form>
</div>
<%
    } else {
     
     Emails email  = AdminDataHelper.getInstance().getEmailWithId(emailId);
     if(email != null){ 
%>
<div class="boxed left mrgtop20">
<form name="mr" id="usersform" action="index.jsp?a=emails&c=save" method="post">
    <input type="hidden" name="emailid" value="<%= emailId %>" />
    <h1 style="font-size:16px;margin:0;padding:0">Edit Administrator Data</h1>
    <p>
        <label>Label:</label><br/>
        <input type="text" name="label" class="w250 validate[required]" value="<%=email.getLabel() %>"/>
    </p>
    <p>
        <label>Receivers: (insert separated by ';')</label><br/>
        <input type="text" name="receivers" class="w250 validate[required, minSize[6]]" value="<%=email.getReceivers() %>"/>
    </p>
     <input class="button-primary" style="margin-top:8px" type="submit" name="submit" value="Update Data" />
</form>
 </div>   
 <% } else {%>
 <div class="boxed left mrgtop20">
 <form name="mr" id="usersform" action="index.jsp?a=emails&c=save" method="post">
    <input type="hidden" name="emailid" value="" />
    <h1 style="font-size:16px;margin:0;padding:0">Edit Administrator Data</h1>
    <p>
        <label>Label:</label><br/>
        <input type="text" name="label" class="w250 validate[required]" value=""/>
    </p>
    <p>
        <label>Receivers: (insert separated by ';')</label><br/>
        <input type="text" name="receivers" class="w250 validate[required, minSize[6]]" value=""/>
    </p>
     <input class="button-primary" style="margin-top:8px" type="submit" name="submit" value="Update Data" />
</form>
 <% } %>
 <% } %>
</div>