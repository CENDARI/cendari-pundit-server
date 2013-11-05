<div class="nav-collapse">
    <ul class="nav">
        <li <% if (action.equalsIgnoreCase("home")) out.print("class='active'"); %>><a href="index.jsp?a=home" title="Home page"><i class="icon-home icon-white"></i>&nbsp;Home</a></li>
        <li <% if (action.equalsIgnoreCase("notebooks")) out.print("class='active'"); %>><a href="index.jsp?a=notebooks" title="Notebooks administration"><i class="icon-book icon-white"></i>&nbsp;Notebooks</a></li>
        <li <% if (action.equalsIgnoreCase("annotations")) out.print("class='active'"); %>><a href="index.jsp?a=annotations" title="Annotations administration"><i class="icon-pencil icon-white"></i>&nbsp;Annotations</a></li>
        <li <% if (action.equalsIgnoreCase("users")) out.print("class='active'"); %>><a href="index.jsp?a=users" title="Users administration"><i class="icon-user icon-white"></i>&nbsp;Users</a></li>
        <li <% if (action.equalsIgnoreCase("emails")) out.print("class='active'"); %>><a href="index.jsp?a=emails" title="Contact us receivers"><i class="icon-user icon-white"></i>&nbsp;"Contact us" receivers</a></li>
        <li><a href="index.jsp?a=logout" title="Logout"><i class="icon-cog icon-white"></i>&nbsp;Logout</a></li>
    </ul>    
</div>
