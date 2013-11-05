        <div id="login">
            <% if (loginError) { %>
                <div class="berror">
                    <strong>ERROR: </strong><%= msgError %>                    
                </div>
            <% } %>
            <div class="boxed" style="padding-bottom: 50px">
            <form name="loginform" id="loginform" action="/annotationserver/admin/index.jsp" method="post">
                <p>
                    <label>Username
                        <br/>
                        <input class="input" type="text" name="asuser" id="asuser" size="20" tabindex="10" value=""/>
                    </label>
                </p>
                <p>
                    <label>Password
                        <br/>
                        <input class="input" type="password" name="aspassword" id="aspassword" size="20" tabindex="11" value=""/>
                    </label>
                </p>
                <p class="submit">
                    <input type="submit" name="pasSubmit" class="button-primary" value="Login" tabindex="12" />
                </p>
            </form>
            </div>
        </div>
