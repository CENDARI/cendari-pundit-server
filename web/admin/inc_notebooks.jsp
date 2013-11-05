<%@page import="eu.semlibproject.annotationserver.repository.OntologyHelper"%>
<%@page import="eu.semlibproject.annotationserver.models.Notebook"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="eu.semlibproject.annotationserver.MediaType"%>
<%@page import="eu.semlibproject.annotationserver.managers.RepositoryManager"%>
<%@page import="eu.semlibproject.annotationserver.AdminDataHelper"%>
<%@page import="eu.semlibproject.annotationserver.hibernate.Notebooks"%>
<%@page import="java.util.List"%>
<%@include file="inc_msgbox.jsp" %>
<%
    int itemsForPage = 15;
    int totNumOfNotebooks = 0;           
    
    String searchingParameter = null;
    List<Notebooks> notebooks = null;
    List<String> nids         = null;
    AdminDataHelper.SearchingMode sMode = AdminDataHelper.SearchingMode.ALL;
    
    if (command != null && command.equalsIgnoreCase("search")) {        
        searchingParameter = request.getParameter("svalue");
        if (StringUtils.isNotBlank(searchingParameter)) {
            searchingParameter = searchingParameter.toLowerCase();
            String mode = request.getParameter("smode");
            if (mode.equalsIgnoreCase("id")) {
                sMode = AdminDataHelper.SearchingMode.ID;
            } else if (mode.equalsIgnoreCase("name")) {
                sMode = AdminDataHelper.SearchingMode.NAME;
            } else if (mode.equalsIgnoreCase("uid")) {
                sMode = AdminDataHelper.SearchingMode.UID;
            } else if (mode.equalsIgnoreCase("uname")) {
                sMode = AdminDataHelper.SearchingMode.UNAME;
            } else {
                sMode = AdminDataHelper.SearchingMode.ALL;
            }
        }
    }
%>
<div class="boxed left mrgtop20">
<form class="left" action="index.jsp?a=notebooks&c=search" method="post">
    <label>Search for Notebooks by</label> 
        <select name="smode">
            <option value="id" <% if (sMode == AdminDataHelper.SearchingMode.ID) { %>selected<% } %> >ID</option>
            <option value="name" <% if (sMode == AdminDataHelper.SearchingMode.NAME) { %>selected<% } %> >Name</option>
            <option value="uid" <% if (sMode == AdminDataHelper.SearchingMode.UID) { %>selected<% } %> >User ID</option>
            <option value="uname" <% if (sMode == AdminDataHelper.SearchingMode.UNAME) { %>selected<% } %> >User's Name</option>
        </select>: 
        <input type="text" name="svalue" id="svalue" style="width:200px" value="<%= (searchingParameter != null) ? searchingParameter : "" %>"/> 
        <input class="button-primary" type="submit" name="submit" value="Search" />
</form>
    
<%    
    boolean searchDone = false;
    if (sMode == AdminDataHelper.SearchingMode.NAME) {
        int limit  = itemsForPage;
        int offset = (requestedPage - 1) * itemsForPage;
        
        try {
            nids = RepositoryManager.getInstance().getCurrentRDFRepository().getIDsOfNotebooksWithName(searchingParameter, false, limit, offset);
            totNumOfNotebooks = nids.size();
            searchDone = true;
        } catch (Exception e) {
            sMode = AdminDataHelper.SearchingMode.ALL;
        }
    } else if (sMode == AdminDataHelper.SearchingMode.UNAME) {
        int limit  = itemsForPage;
        int offset = (requestedPage - 1) * itemsForPage;
        
        try {
            nids = RepositoryManager.getInstance().getCurrentRDFRepository().getIDsOfNotebooksWithOwnerName(searchingParameter, false, limit, offset);
            totNumOfNotebooks = nids.size();
            searchDone = true;
        } catch (Exception e) {
            sMode = AdminDataHelper.SearchingMode.ALL;
        }
    } 
    
    if (!searchDone) {
        // Make a standard pre-search
        totNumOfNotebooks = AdminDataHelper.getInstance().getNumOfNotebooks(sMode, searchingParameter);
    }
                    
    if (totNumOfNotebooks > 0) {
        notebooks = AdminDataHelper.getInstance().getNotebooks(false, requestedPage, itemsForPage, sMode, searchingParameter, nids);    
%>
<form class="mrgtop10" name="mr" action="index.jsp?a=notebooks&c=delete" method="post">
<table class="report">
    <tr>       
        <th class="w30 borderB borderT"></th>
        <th class="w80 borderB borderT">Notebook ID</th>
        <th class="w250 borderB borderT">Notebook Name</th>
        <th class="w80 borderB borderT">Owner ID</th>
        <th class="w250 borderB borderT">Owner Name</th>
        <th class="borderB borderT">Public</th>
    </tr>
    <% for (int i = 0; i < notebooks.size(); i++) {  
        Notebooks notebook = notebooks.get(i);
        String id = notebook.getId();         
        String authorName   = null;
        String notebookname = null;
            
        String notebookMetadata = RepositoryManager.getInstance().getCurrentRDFRepository().getNotebookMetadata(id, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(notebookMetadata)) {
            JSONObject jsonData = new JSONObject(notebookMetadata);
            JSONObject cmetadata = jsonData.getJSONObject(Notebook.getURIFromID(id));                        
            
            if (cmetadata.has(OntologyHelper.URI_DCELEMENT_CREATOR)) {
                JSONObject data = cmetadata.getJSONArray(OntologyHelper.URI_DCELEMENT_CREATOR).getJSONObject(0);
                authorName = data.getString("value");                
                if (authorName.length() > 28) {
                    authorName = authorName.substring(0, 28) + "...";
                }
            }
            
            if (cmetadata.has(OntologyHelper.URI_RDFS_LABEL)) {
                JSONObject data = cmetadata.getJSONArray(OntologyHelper.URI_RDFS_LABEL).getJSONObject(0);
                notebookname = data.getString("value");
                if (notebookname.length() > 28) {
                    notebookname = notebookname.substring(0, 28) + "...";
                }
            }
        }
        
        String addClass = "";
        if (i == notebooks.size()-1) {
            addClass="borderB";
        }        
    %>
    <tr <% if (i%2 == 0) { %>class="lightGray"<% } %>>
        <td class="center <%= addClass %>"><input type="checkbox" name="ids" value="<%= id %>" /></td>
        <td class="<%= addClass %>"><strong><a class="nl" href="index.jsp?a=annotations&uid=<%= id %>"><%= id.toUpperCase() %></a></strong></td>
        <td class="<%= addClass %>"><%= notebookname %></td>
        <td class="<%= addClass %>"><%= notebook.getOwnerid().toUpperCase() %></td>
        <td class="<%= addClass %>"><%= authorName %></td>
        <td class="<%= addClass %>"><%= (notebook.isPublic_()) ? "YES" : "NO" %></td>
    </tr>
    <% } %>
</table>
    <input class="button-primary" style="margin-top:8px" type="submit" name="submit" value="Delete Notebooks" />
    <br/>
    <div id="pagination">
    <%
       if (requestedPage > 1) {
    %>    
    <a class="pageNavi" href="index.jsp?a=notebooks&p=<%=(requestedPage-1) %>"><< Previous Page</a>    
    <% }        
       if (totNumOfNotebooks > 2 && (requestedPage * itemsForPage) < totNumOfNotebooks) {
    %>
    <div id="pagi_right" style="float:right">
        <a class="pageNavi" href="index.jsp?a=notebooks&p=<%=(requestedPage+1) %>">Next Page >></a>
    </div>
    <% } %>
    <div class="clear"></div>
    </div>    
</form>
</div>
<div class="boxed left">
<form class="left" name="sr" action="index.jsp?a=notebooks&c=delete" method="post">
    <label>Delete Notebook with ID: </label><input type="text" name="id" id="id" />&nbsp;&nbsp;<input class="button-primary" type="submit" name="submit" value="Delete" />
</form>
</div>
<% } else { %>
    <div class="center" style="margin-top:20px">
        <strong>
        <%        
            if (command != null && command.equalsIgnoreCase("search")) { %>
                No Notebooks available using the specified searching parameters!
            <% } else { %>
                No Notebooks available at the moment!
            <% } %>
        </strong>
    </div>
</div>
<% } %>