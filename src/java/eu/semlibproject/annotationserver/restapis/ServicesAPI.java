/*
 * Copyright (c) 2013 Net7 SRL, <http://www.netseven.it/>
 * 
 * This file is part of Pundit: Annonation Server.
 * 
 * Pundit: Annonation Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Pundit: Annonation Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Pundit: Annonation Server.  If not, see <http ://www.gnu.org/licenses/>.
 *
 * See LICENSE.TXT or visit <http://thepund.it> for the full text of the license.
 */
/**
 * File: Services.java Created: 05-dec-2011
 */
package eu.semlibproject.annotationserver.restapis;

import eu.semlibproject.annotationserver.SemlibConstants;
import eu.semlibproject.annotationserver.hibernate.Emails;

import eu.semlibproject.annotationserver.managers.ConfigManager;
import eu.semlibproject.annotationserver.managers.HibernateManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Main class that implements all REST APIs under the namespace
 * "[our-webapps]/services/"
 *
 * @author Michele Nucci
 */
@Path("/services")
public class ServicesAPI extends APIHelper {

    private Logger logger = Logger.getLogger(ServicesAPI.class.getName());

    /**
     * Default constructor
     *
     * @param req the servlet's request passed by Jersy
     * @param servletContext the servlet context
     */
    public ServicesAPI(@Context HttpServletRequest req, @Context ServletContext servletContext) {
        super(req, servletContext);
    }

    /**
     * Implement a simple proxy
     *
     * @param requestedURL the requested URL
     * @param req the HttpServletRequest
     * @return
     */
    @GET
    @Path("proxy")
    public Response proxy(@QueryParam(SemlibConstants.URL_PARAM) String requestedURL, @Context HttpServletRequest req) {

        BufferedReader in = null;

        try {
            URL url = new URL(requestedURL);
            URLConnection urlConnection = url.openConnection();

            int proxyConnectionTimeout = ConfigManager.getInstance().getProxyAPITimeout();

            // Set base properties
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(proxyConnectionTimeout * 1000); // set max response timeout 15 sec

            String acceptedDataFormat = req.getHeader(SemlibConstants.HTTP_HEADER_ACCEPT);
            if (StringUtils.isNotBlank(acceptedDataFormat)) {
                urlConnection.addRequestProperty(SemlibConstants.HTTP_HEADER_ACCEPT, acceptedDataFormat);
            }

            // Open the connection
            urlConnection.connect();

            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;

                int statusCode = httpConnection.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP || statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    // Follow the redirect
                    String newLocation = httpConnection.getHeaderField(SemlibConstants.HTTP_HEADER_LOCATION);
                    httpConnection.disconnect();

                    if (StringUtils.isNotBlank(newLocation)) {
                        return this.proxy(newLocation, req);
                    } else {
                        return Response.status(statusCode).build();
                    }
                } else if (statusCode == HttpURLConnection.HTTP_OK) {

                    // Send the response
                    StringBuilder sbf = new StringBuilder();

                    // Check if the contentType is supported
                    boolean contentTypeSupported = false;
                    String contentType = httpConnection.getHeaderField(SemlibConstants.HTTP_HEADER_CONTENT_TYPE);
                    List<String> supportedMimeTypes = ConfigManager.getInstance().getProxySupportedMimeTypes();
                    if (contentType != null) {
                        for (String cMime : supportedMimeTypes) {
                            if (contentType.equals(cMime) || contentType.contains(cMime)) {
                                contentTypeSupported = true;
                                break;
                            }
                        }
                    }

                    if (!contentTypeSupported) {
                        httpConnection.disconnect();
                        return Response.status(Status.NOT_ACCEPTABLE).build();
                    }

                    String contentEncoding = httpConnection.getContentEncoding();
                    if (StringUtils.isBlank(contentEncoding)) {
                        contentEncoding = "UTF-8";
                    }

                    InputStreamReader inStrem = new InputStreamReader((InputStream) httpConnection.getContent(), Charset.forName(contentEncoding));
                    in = new BufferedReader(inStrem);

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        sbf.append(inputLine);
                        sbf.append("\r\n");
                    }

                    in.close();
                    httpConnection.disconnect();

                    return Response.status(statusCode).header(SemlibConstants.HTTP_HEADER_CONTENT_TYPE, contentType).entity(sbf.toString()).build();

                } else {
                    httpConnection.disconnect();
                    return Response.status(statusCode).build();
                }
            }

            return Response.status(Status.BAD_REQUEST).build();

        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
        }
    }

    /**
     * Get all user favorites items
     *
     * @return a Response contains the favorites items for the current logged
     * user<br/><br/> HTTP Response Status Code: <ul> <li>"200 Ok" in case of
     * success</li> <li>"203 Forbidden" if this API is called whit no logged
     * user</li> <li>"204 No Content" if there are no favorites items for the
     * current logged user</li> <li>"500 Internal Server Error" for internal
     * errors</li> </ul>
     */
    @GET
    @Path("favorites")
    public Response getFavorites() {
        return super.getGenericDataByKey("favorites");
    }

    /**
     * Set the favorites items for the current logged User
     *
     * @param data data representing the favorites items for the current logged
     * User
     * @return HTTP Response Status Code:<br/> <ul> <li>"200 Ok" in case of
     * success</li> <li>"203 Forbidden" if this API is called whit no logged
     * user</li> <li>"400 Bad Request" if the data or the request is not
     * valid</li> <li>"500 Internal Server Error" for internal errors</li> </ul>
     */
    @POST
    @Path("favorites")
    public Response setFavorites(String data) {
        return super.storeGenericDataByKey("favorites", data);
    }

    /**
     * Given a Key, return generic data associated with the specified Key for
     * the current logged user.
     *
     * @param key the key
     * @return a Response contains the favorites items for the current logged
     * user<br/><br/> HTTP Response Status Code: <ul> <li>"200 Ok" in case of
     * success</li> <li>"203 Forbidden" if this API is called whit no logged
     * user</li> <li>"204 No Content" if there are no favorites items for the
     * current logged user</li> <li>"500 Internal Server Error" for internal
     * errors</li> </ul>
     */
    @GET
    @Path("preferences/{key}")
    public Response getPreferences(@PathParam("key") String key) {
        return super.getGenericDataByKey(key);
    }

    /**
     * Given a Key, associate generic data to it for the current logged user.
     *
     * @param key the key
     * @param data payload data
     *
     * @return HTTP Response Status Code:<br/> <ul> <li>"200 Ok" in case of
     * success</li> <li>"203 Forbidden" if this API is called whit no logged
     * user</li> <li>"400 Bad Request" if the data or the request is not
     * valid</li> <li>"500 Internal Server Error" for internal errors</li> </ul>
     */
    @POST
    @Path("preferences/{key}")
    public Response setPreferences(@PathParam("key") String key, String data) {
        return super.storeGenericDataByKey(key, data);
    }

    /**
     * Given a Subject, Text, Name, Email and Identifier it creates an email
     * and send it to all receivers listed into 'identifier' field stored into
     * DB table 'emails'
     *
     *  @param subjetc the email subject
     *  @param text the email text
     *  @param name the sender name 
     *  @param email the sender email 
     *  @param identifier the identifier list for receivers
     *
     * @return HTTP Response Status Code:<br/> <ul> <li>"200 Ok" in case of
     * success</li> <li>"203 Forbidden" if this API is called whit no logged
     * user</li> <li>"400 Bad Request" if the data or the request is not
     * valid</li> <li>"500 Internal Server Error" for internal errors</li> </ul>
     */
    @POST
    @Path("email")
    public Response sendEmail(@FormParam("subject") String subject,
            @FormParam("text") String text,
            @FormParam("name") String name,
            @FormParam("email") String email,
            @FormParam("identifier") String identifier,
            @Context HttpServletRequest req) {
        //User currentLoggedUser = this.getCurrentLoggedUser();
        /*if (currentLoggedUser == null || StringUtils.isBlank(currentLoggedUser.getUserID())) {
         return Response.status(Status.FORBIDDEN).build();
         } */


        String ip=null;
       
        
        if (subject == null && StringUtils.isNotBlank(subject)) {
            logger.log(Level.SEVERE, "No SUBJECT", req);
            return Response.status(Status.BAD_REQUEST).build();
        }

        if (text == null && StringUtils.isNotBlank(text)) {
            logger.log(Level.SEVERE, "No TEXT message", req);
            return Response.status(Status.BAD_REQUEST).build();
        }

        if (identifier == null && StringUtils.isNotBlank(identifier)) {
            logger.log(Level.SEVERE, "No list IDENTIFIER", req);
            return Response.status(Status.BAD_REQUEST).build();
        }


        Session hSession = null;
        try {
            ip = req.getRemoteAddr();
            hSession = HibernateManager.getSessionFactory().getCurrentSession();
            hSession.beginTransaction();
            Query query = hSession.createQuery("from Emails as e where e.label LIKE :list");
            query.setParameter("list", identifier);
            Emails email_from_DB = (Emails) query.uniqueResult();

            if (email_from_DB != null) {
                Transport transport = null;
                createAndSendMessage(transport, subject, text, name, email, email_from_DB,ip);
            } else {
                logger.log(Level.WARNING, "There are no receivers for identifier: {0}", identifier);
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error on DB access for retrieve emails", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (hSession != null && hSession.getTransaction() != null && hSession.getTransaction().isActive()) {
                hSession.getTransaction().commit();
            } else {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }

        return Response.status(Status.OK).build();
    }

    private void createAndSendMessage(Transport transport, String subject, String text, String nameFrom, String emailFrom, Emails emails, String ip) { // Assuming you are sending email from localhost
        String host = ConfigManager.getInstance().getSmtpMailhost();
        String port = ConfigManager.getInstance().getSmtpMailport();
        String password = ConfigManager.getInstance().getSmtpMailpassword();
        String auth = ConfigManager.getInstance().getSmtpMailauth();
        String sender = ConfigManager.getInstance().getSmtpMailuser();
        
        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.user", sender);
        properties.setProperty("mail.smtp.password", password);
        properties.setProperty("mail.smtp.auth", auth);
        properties.setProperty("mail.smtp.port", port);
        
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(properties);



        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        if (!StringUtils.isBlank(sender)) {
            try {
                message.setFrom(new InternetAddress(sender));

            } catch (AddressException ex) {
                logger.log(Level.SEVERE, "Sender address is not a valid mail address" + sender, ex);
            } catch (MessagingException ex) {
                logger.log(Level.SEVERE, "Can't create message for Sender: " + sender + " due to", ex);
            }

        }


        List<Address> addresses = new ArrayList<Address>();

        String receivers = emails.getReceivers();
        receivers = StringUtils.trim(receivers);

        for (String receiver : receivers.split(";")) {
            if (!StringUtils.isBlank(receiver)) {
                try {
                    addresses.add(new InternetAddress(receiver));
                } catch (AddressException ex) {
                    logger.log(Level.SEVERE, "Receiver address is not a valid mail address" + receiver, ex);
                } catch (MessagingException ex) {
                    logger.log(Level.SEVERE, "Can't create message for Receiver: " + receiver + " due to", ex);
                }
            }
        }
        Address[] add = new Address[addresses.size()];
        addresses.toArray(add);
        try {
            message.addRecipients(Message.RecipientType.TO, add);

            // Set Subject: header field
            message.setSubject("[Pundit Contact Form]: "+subject);

            // Now set the actual message
            message.setText("Subject: "+subject+" \n"
                          + "From: "+nameFrom+" (email: "+emailFrom+") \n"
                          + "Date: "+ new Date()+"\n"
                          + "IP: "+ip+"\n"
                          + "Text: \n"+text);

        } catch (MessagingException ex) {
            logger.log(Level.SEVERE, "Can't create message for Recipient: " + add + " due to", ex);
        }
        // Send messageù
       
       
        try {
            logger.log(Level.INFO, "Trying to send message to receivers: {0}", Arrays.toString(message.getAllRecipients()));
            transport = session.getTransport("smtp");
            transport.connect(host, sender, password);
            transport.sendMessage(message, message.getAllRecipients());
            logger.log(Level.INFO, "Sent messages to all receivers {0}", Arrays.toString(message.getAllRecipients()));
        } catch (NoSuchProviderException nspe) {
            logger.log(Level.SEVERE, "Cannot find smtp provider", nspe);
        } catch (MessagingException me) {
            logger.log(Level.SEVERE, "Cannot set transport layer ", me);
        } finally {
            try {
                 
                transport.close();
            } catch (MessagingException ex) {
               
            } catch (NullPointerException ne) {
            }

        }


    }
}
