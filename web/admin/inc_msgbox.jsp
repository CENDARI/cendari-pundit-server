<div class="left">
<% if (commandError && commandMessage != null) { %>
<div class="berror">
    <strong>ERROR: </strong>
    <%= commandMessage %>
</div>
<% } else if (!commandError && commandMessage != null) { %>
<div class="bok">
    <%= commandMessage %>
</div>
<% } %>
</div>
