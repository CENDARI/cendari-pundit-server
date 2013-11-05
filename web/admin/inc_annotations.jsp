<%@page import="java.rmi.server.UID"%>
<%@page import="sun.swing.UIAction"%>
<%@page import="eu.semlibproject.annotationserver.repository.OntologyHelper"%>
<%@page import="eu.semlibproject.annotationserver.models.Annotation"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="eu.semlibproject.annotationserver.MediaType"%>
<%@page import="eu.semlibproject.annotationserver.managers.RepositoryManager"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Notebooks"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Annotations"%>
<%@page import="java.util.List"%>
<%@page import="eu.semlibproject.annotationserver.AdminDataHelper"%>
<%@include file="inc_msgbox.jsp" %>
<%
    int itemsForPage = 15;
    int totNumOfAnnotations = 0;
    
    String searchingParameter     = null;
    List<Annotations> annotations = null;
    List<String> aIDs             = null;
    AdminDataHelper.SearchingMode sMode = AdminDataHelper.SearchingMode.ALL;
    
        if (command != null && command.equalsIgnoreCase("search")) {        
            searchingParameter = request.getParameter("svalue");
            if (StringUtils.isNotBlank(searchingParameter)) {
                searchingParameter = searchingParameter.toLowerCase();
                String mode = request.getParameter("smode");
                
                if (mode.equalsIgnoreCase("id")) {
                    sMode = AdminDataHelper.SearchingMode.ID;
                } else if (mode.equalsIgnoreCase("cdate")) {
                    sMode = AdminDataHelper.SearchingMode.DATE;
                } else if (mode.equalsIgnoreCase("uid")) {
                    sMode = AdminDataHelper.SearchingMode.UID;
                } else if (mode.equalsIgnoreCase("uname")) {
                    sMode = AdminDataHelper.SearchingMode.UNAME;
                } else if (StringUtils.isNotBlank(uid)) {
                    sMode = AdminDataHelper.SearchingMode.NID;
                    searchingParameter = uid;
                } else {
                    sMode = AdminDataHelper.SearchingMode.ALL;
                }
            }
        } else if (StringUtils.isNotBlank(uid)) {
            sMode = AdminDataHelper.SearchingMode.NID;
            searchingParameter = uid;
        } else {
            sMode = AdminDataHelper.SearchingMode.ALL;
        }       
%>
<div class="boxed left mrgtop20">
<form class="left" action="index.jsp?a=annotations&c=search" method="post">
    <label>Search for Annotations by</label> 
        <select name="smode">
            <option value="id" <% if (sMode == AdminDataHelper.SearchingMode.ID) { %>selected<% } %> >ID</option>            
            <option value="uid" <% if (sMode == AdminDataHelper.SearchingMode.UID) { %>selected<% } %> >User ID</option>
            <option value="uname" <% if (sMode == AdminDataHelper.SearchingMode.UNAME) { %>selected<% } %> >User's Name</option>
        </select>: 
        <input type="text" name="svalue" id="svalue" style="width:200px" value="<%= (searchingParameter != null) ? searchingParameter : "" %>"/> 
        <input class="button-primary" type="submit" name="submit" value="Search" />
</form>    
<%
    boolean searchDone = false;
    if (sMode == AdminDataHelper.SearchingMode.UID) {
        int limit  = itemsForPage;
        int offset = (requestedPage - 1) * itemsForPage;
            
        try {
            aIDs = RepositoryManager.getInstance().getCurrentRDFRepository().getIDsOfAnnotationsWithID(searchingParameter, false, limit, offset);
            totNumOfAnnotations = aIDs.size();
            searchDone = true;
        } catch (Exception e) {
            sMode = AdminDataHelper.SearchingMode.ALL;
        }
    } else if (sMode == AdminDataHelper.SearchingMode.UNAME) {
        int limit  = itemsForPage;
        int offset = (requestedPage - 1) * itemsForPage;
            
        try {
            aIDs = RepositoryManager.getInstance().getCurrentRDFRepository().getIDsofAnnotationsWithOwnerName(searchingParameter, false, limit, offset);
            totNumOfAnnotations = aIDs.size();
            searchDone = true;
        } catch (Exception e) {
            sMode = AdminDataHelper.SearchingMode.ALL;
        }
    }
    
    if (!searchDone) {
        totNumOfAnnotations = AdminDataHelper.getInstance().getNumOfAnnotations(sMode, searchingParameter);
    }    
    
    if (totNumOfAnnotations > 0) {
        annotations = AdminDataHelper.getInstance().getAnnotations(false, requestedPage, itemsForPage, sMode, searchingParameter, aIDs); 
%>
<form class="mrgtop10" name="mr" action="index.jsp?a=annotations&c=delete" method="post">
<%
    if (StringUtils.isNotBlank(uid)) {
%>
        <h1 class="adminTitle">List of Annotations in Notebook: <%= uid.toUpperCase() %></h1>
<%        
    }
%>
<table class="report">
    <tr>       
        <th class="w30 borderB borderT"></th>
        <th class="w90 borderB borderT">Annotation ID</th>
        <th class="w140 borderB borderT">Created</th>
        <th class="w340 borderB borderT">Creator</th>
        <th class="w80 borderB borderT">In Notebook</th>
    </tr>
    <% for (int i = 0; i < annotations.size(); i++) {  
        Annotations ann = annotations.get(i);
        String id = ann.getAnnotationid(); 
        String creatorName  = "";
        String creationDate = "";
        Notebooks notebook = annotations.get(i).getNotebooks();
        
        String annotationMetadata = RepositoryManager.getInstance().getCurrentRDFRepository().getAnnotationMetadata(id, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(annotationMetadata)) {
            JSONObject jsonData = new JSONObject(annotationMetadata);
            JSONObject aMetadata = jsonData.getJSONObject(Annotation.getURIFromID(id));
                                               
            if (aMetadata.has(OntologyHelper.URI_DCELEMENT_CREATOR)) {
                JSONObject data = aMetadata.getJSONArray(OntologyHelper.URI_DCELEMENT_CREATOR).getJSONObject(0);
                creatorName = data.getString("value");                
                if (creatorName.length() > 36) {
                    creatorName = creatorName.substring(0, 36) + "...";
                }                                        
            }
            
            if (aMetadata.has(OntologyHelper.URI_DC_CREATED)) {
                JSONObject data = aMetadata.getJSONArray(OntologyHelper.URI_DC_CREATED).getJSONObject(0);
                creationDate = data.getString("value");
                if (StringUtils.isNotBlank(creationDate)) {
                    creationDate = creationDate.replaceAll("T", " ");
                }                
            }            
        }                
        
        String addClass = "";
        if (i == annotations.size()-1) {
            addClass="borderB";
        }
    %>
    <tr <% if (i%2 == 0) { %>class="lightGray"<% } %>>
        <td class="center <%= addClass %>"><input type="checkbox" name="ids" value="<%= id %>" /></td>
        <td class="<%= addClass %>"><strong><%= id.toUpperCase() %></strong></td>
        <td class="<%= addClass %>"><%= creationDate %></td>
        <td class="<%= addClass %>"><%= creatorName %></td>
        <td class="<%= addClass %>"><%= notebook.getId().toUpperCase() %></td>    
    </tr>
    <% } %>
</table>
    <input class="button-primary" style="margin-top:8px" type="submit" name="submit" value="Delete Annotations" />
    <br/>
    <div id="pagination">
    <%
       if (requestedPage > 1) {
    %>    
    <a class="pageNavi" href="index.jsp?a=annotations&p=<%=(requestedPage-1) %>"><< Previous Page</a>    
    <% }
        
       if (totNumOfAnnotations > 2 && (requestedPage * itemsForPage) < totNumOfAnnotations) {
    %>
    <div id="pagi_right" style="float:right">
        <a class="pageNavi" href="index.jsp?a=annotations&p=<%=(requestedPage+1) %>">Next Page >></a>
    </div>
    <% } %>
    <div class="clear"></div>
    </div>    
</form>
</div>
<div class="boxed left">
    <form class="left" name="sr" action="index.jsp?a=annotations&c=delete" method="post">
    <label>Delete Annotation with ID: </label><input type="text" name="id" id="id" />&nbsp;&nbsp;<input class="button-primary" type="submit" name="submit" value="Delete" />
</form>
</div>
<% } else { %>
    <div class="center" style="margin-top:20px">
        <strong>
        <%        
            if (command != null && command.equalsIgnoreCase("search")) { %>
                No Annotations available using the specified searching parameters!
            <% } else if (StringUtils.isBlank(uid)) { %>
                No Annotations available at the moment!
            <% } else { %>
                No Annotations available in Notebook <%= uid.toUpperCase() %>!
            <% } %>
        </strong>
    </div>
</div>    
<% } %>