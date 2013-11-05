<%@page import="eu.semlibproject.annotationserver.managers.ConfigManager"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Admins"%>
<%
    Admins admin = (Admins)session.getAttribute("user");
    
    String firstName = admin.getFirstname();
    String lastName  = admin.getLastname();
    
    String name = (StringUtils.isNotBlank(firstName) ? firstName : "") + (StringUtils.isNotBlank(lastName) ? " " + lastName : "");
    if (StringUtils.isEmpty(name)) {
        name = admin.getUsername();
    }
%>
<div class="boxed center mgrtop20">
    <h1 style="font-size:14px">Welcome to the Annotation Server administration area!</h1>
    <table class="left" style="width:100%">        
        <tr >
            <td class="borderT lightGray"><strong>Current logget user:</strong></td>
            <td class="borderT lightGray"> <%= name %></td>
        </tr>
        <tr>
            <td class="borderB"><strong>Annotation Server version:</strong></td>
            <td class="borderB"> <%= ConfigManager.getInstance().getVersion() %></td>
        </tr>
    </table>
</div>
