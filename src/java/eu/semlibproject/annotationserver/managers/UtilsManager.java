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

package eu.semlibproject.annotationserver.managers;

import eu.semlibproject.annotationserver.MediaType;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Michele Nucci
 */
public class UtilsManager {
    
    private static UtilsManager instance = null;
    
    private Logger logger = Logger.getLogger(RepositoryManager.class.getName());
    
    
    private UtilsManager() { }
    
    public static synchronized UtilsManager getInstance() {
        if (instance == null) {
            instance = new UtilsManager();
        }
        
        return instance;
    }
    
    
    public String getDate(boolean XMLSchemaFormat) {        
        if (XMLSchemaFormat) {
            String date = this.getCurrentDate("yyyy-MM-dd");
            String time = this.getCurrentDate("HH:mm:ss");
        
            // Return the date in the correct format (see DateTime in XMLSChema)
            return date+"T"+time;
        } else {
            return this.getCurrentDate("yyyy-MM-dd HH:mm:ss");
        }
    }
    
    
    public String getCurrentDate(String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Date date = new Date();
        return formatter.format(date);
    }
    
    
    /**
     * Compute the CRC32 checksum of a String. This can be useful to
     * short an URL.
     * 
     * @param text  the text from which the checksum will be computed
     * @return      the CRC32 checksum
     */
    public String CRC32(String text) {
        CRC32 checksumer = new CRC32();
        checksumer.update(text.getBytes());
        String finalhash = Long.toHexString(checksumer.getValue());                        
        // correctly format the finalHash (e.g. number starting with 00 that was stripped)
        return StringUtils.leftPad(finalhash, 8, "0");
    }

    
    /**
     * Compute the MD5 hash of a given text
     * 
     * @param text  the text from which the hash will be computed
     * @return      the MD5 hash
     * 
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException 
     */
    public String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return computeHashWithDigest(text, "MD5");
    }

    /**
     * Compute the SHA1 hash of a given text
     * 
     * @param text  the text from which the hash will be computed
     * @return      the SHA1 hash
     * 
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException 
     */    
    public String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return computeHashWithDigest(text, "SHA-1");
    }
    
    /**
     * Encode a String in Base64
     * 
     * @param text  the text to encode
     * @return      the encoded text
     */
    public String base64Encode(String text) {
        if (text != null) {
            BASE64Encoder base64Encoder = new BASE64Encoder();
            return base64Encoder.encode(text.getBytes());
        }
        return null;
    }
    
    
    /**
     * Return the lenght (number of chars) of the HASH algorithm used 
     * to create annotations and notebook ID
     * 
     * @return the number of chars of the HASH algorithm used 
     *         to create annotations and notebook ID
     */
    public int getCurrentHASHLenght() {
        String hashType = ConfigManager.getInstance().getHASHAlgorithmName();
        if (hashType.equalsIgnoreCase("CRC32")) {
            return 8;
        } else if (hashType.equalsIgnoreCase("MD5") || hashType.equalsIgnoreCase("SHA1")) {
            try {
                return MessageDigest.getInstance(hashType).getDigestLength() * 2;
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(UtilsManager.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }
        } else {
            return -1;  
        }
    }
    
    
    /**
     * Check for a valid mail address
     * 
     * @param mailAddress   the string to check
     * @return <code>true</code> if mailAddress is a valid mail address, <code>false</code> otherwise
     */
    public boolean isValidMailAddress(String mailAddress) {        
        boolean valid = false;
        if (mailAddress != null) {            
            Pattern p = Pattern.compile(".+@.+\\.[a-zA-Z]+");
            Matcher m = p.matcher(mailAddress);
            valid = m.matches();
        }
        
        return valid;
    }
    
    
    /**
     * Wrap data into a JSONP callback
     * 
     * @param data      the data to wrap
     * @param callback  the callback name
     * 
     * @return          the wrapped data
     */
    public String wrapJSONPResponse(String data, String callback) {
        
        if (StringUtils.isBlank(data)) {
            data = "{}";
        }
        
        return ( callback + "(" + data + ")");       
        
    }
    
    
    /**
     * Compute the HASH of a string using a specific digest.
     * 
     * @param text      the text from which the hash will be created
     * @param digest    a digest (SHA-1, MD5 or CRC32)
     * @return          the hash of the passed String
     */
    public String computeHashWithDigest(String text, String digest) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        
        if (digest.equalsIgnoreCase("CRC32")) {
            return CRC32(text);
        } else {
            MessageDigest md;
            md = MessageDigest.getInstance(digest);
            byte[] digestBytes = new byte[md.getDigestLength()];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            digestBytes = md.digest();
            return convertToHex(digestBytes);            
        }
        
    }
    
    
    /**
     * Shortcut methods: get the correct accept value for the current request (basing on callback 
     * and Accept header) and set the correct triple format 
     * 
     * @param callback the JSONP callback function (could be null for other types of requests)
     * @param accept   the Accept header for te current request
     * 
     * @return         the correct triple format
     */
    public String getCorrectTripleFormat(String callback, String accept, String cAccepts) {                
        if (cAccepts == null) {
            cAccepts = getCorrectAcceptValue(callback, accept);
        }

        if (cAccepts.equalsIgnoreCase(MediaType.APPLICATION_JAVASCRIPT)) {
            return MediaType.APPLICATION_JSON;
        } else {
            return cAccepts;
        }
    }
    
    
    /**
     * Get the correct accept value for the current request basing on callback and Accept header
     * 
     * @param callback  the JSONP callback function (could be null for other types of requests)
     * @param accept    the Accept header for te current request
     * 
     * @return          the correct Accept value for the current request
     */
    public String getCorrectAcceptValue(String callback, String accept) {
        
        if ( (callback != null && callback.length() > 0)) {
            return MediaType.APPLICATION_JAVASCRIPT;
        } else if (accept.contains(MediaType.TEXT_HTML)) {
            return MediaType.APPLICATION_RDFXML;
        } else if (accept.contains(MediaType.WILDCARD)) {
            return MediaType.APPLICATION_JSON;
        } else {
            return accept;
        }
        
    }

    
    /**
     * Create a new instance of a specified class
     * 
     * @param fullClassName the full className
     * @return              the instance of the specified
     */
    public Object createNewInstanceOfClass(String fullClassName) {
        
        final String errorMessage = "Unable to instanciate the specified class for Repository or models. See your configuration in web.xml file.";                
        
        if (fullClassName == null) {
            logger.log(Level.SEVERE, "No class specified (Custom repository or models)!");
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
            
        try {
            Object myFinalClass = null;
                
            Class newClass = Class.forName(fullClassName);
            if (newClass == null) {
                logger.log(Level.SEVERE, "Specified custom class for repository or models not found.");
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
                
            Constructor defaultConstructor = newClass.getConstructor();
            myFinalClass = defaultConstructor.newInstance();
            if (myFinalClass == null) {
                logger.log(Level.SEVERE, errorMessage);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            } else {
                return myFinalClass;
            }
                        
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex.getMessage());
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }
    
    /**
     * Parse Limit or Offset values
     * 
     * @param limitOrOffset the limit or offset values
     * @return              the limit or offset values or <code>-1</code> in case of error
     */
    public int parseLimitOrOffset(String limitOrOffset) {
        if (limitOrOffset != null && limitOrOffset.length() > 0) {
            try {
                return Integer.parseInt(limitOrOffset);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }           
    }
    
    
    /**
     * Check if the specified URI is valid
     * 
     * @param resource  the URI to check
     * @return          <code>true</code> if the URI is valid
     */
    public boolean isValidURI(String resource) {
        try {
            URI uri = new URI(resource);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
        
    /**
     * Covert data to hexadecimal format
     * 
     * @param data  the data to convert
     * @return      a String containing data converted in hexadecimal format
     */
    private String convertToHex(byte[] data) { 
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) { 
                    buf.append((char) ('0' + halfbyte));
                }
                else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 
    
}
