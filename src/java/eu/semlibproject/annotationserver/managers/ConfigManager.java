
package eu.semlibproject.annotationserver.managers;

import eu.semlibproject.annotationserver.SemlibConstantsConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;

/**
 * This is a singleton  class used  to manage the configuration 
 * of the Annotation Server. This class are based on the values
 * parsed from the /WEB-INF/configuration.properties
 * 
 * @author Michele Nucci
 */
public class ConfigManager {
   
    private static ConfigManager instance = null;
    
    // The Annotation Server version
    private String annotationServerVersion = null;
    
    // The main servlet context and the related context path
    private ServletContext servletContext = null;
    private String servletContextPath     = null;
    
    // Shared Flags 
    private boolean contextParametersParsed  = false;
    private boolean anonymousUserInitialized = false;
    
    // Repository configuration value
    private String reporitoryURL        = null;
    private String repositoryPort       = null;
    private String repositoryID         = null;
    private boolean repositoryUseAuth   = false;
    private String repositoryUsername   = null;
    private String repositoryPassword   = null;
    
    // RDBMS configuration value
    private String rdbmsDialect         = "org.hibernate.dialect.MySQLDialect";
    private String rdbmsDriver          = "com.mysql.jdbc.Driver";
    private String rdbmsConnectionUrl   = "jdbc:mysql://localhost:3306/semlibStorage?useUnicode=true&amp;characterEncoding=utf8&amp;autoReconnect=true";
    private String rdbmsUsername        = "semlibAs";
    private String rdbmsPassword        = "";
    
    
    //mail smtp host
    private String mailSmtpHost = "localhost";
    private String mailSmtpPort="";
    private String mailSmtpPassword="";
    private String mailSmtpAuth="";
            
    
    // HASH used in resource ID creation
    private String hashAlgorithm = "CRC32";
    
    // Default status for the newly created album
    private boolean newNotebooksPublicAsDefault = true;
    
    // Max connection timeout for proxy API (in seconds)
    private int proxyAPITimeout = 20;
    
    // Supported MIME-TYPES for proxy API
    private List<String> proxyAPIsupportedMimeTypes;
    
    // Used to store the name of the RDF repository
    private String currentRDFRepositoryClass = "eu.semlibproject.annotationserver.repository.SesameRepository";
    
    // Used to store the name of the relational data repository 
    private String currentDataRepositoryClass = "eu.semlibproject.annotationserver.repository.MySqlRepository";
    
    // Used to store the name of the class that implements the main permission manager
    private String currentPermissionsManagerClass = "eu.semlibproject.annotationserver.security.DefaultPermissionManager";
    
    // Used to store the path to the loginform (if the authentication is enabled)
    private String authenticationFormPath = null;      
        
    // The Logger
    private Logger logger = Logger.getLogger(ConfigManager.class.getName());
    private String mailSmtpUser;
    
    
    /**
     * Default constructor
     */
    private ConfigManager() { }
    
    
    /**
     * Get the ConfigManager shared instance
     * 
     * @return a ConfigManager shared instance
     */
    public static synchronized ConfigManager getInstance() {
        
        if (instance == null) {
            instance =  new ConfigManager();
            
            instance.parseMainContextParameters();            
        }
        
        return instance;
    }
    
    
    /**
     * Get the ConfigManager shared instance and initialize the servletContext
     * 
     * @param context   the main servlet contex
     * @return          a ConfigManager shared instance
     */
    public static synchronized void initInstance(ServletContext context) {

        if (instance == null) {
            instance =  new ConfigManager();
            
            if (context != null) {
                instance.setMainServletContext(context);
            }
            
            instance.parseMainContextParameters();            
        }
    }
    
    
    public String getVersion() {
        return this.annotationServerVersion;
    }
    
    
    public String getHASHAlgorithmName() {
        return hashAlgorithm;
    }
    
    
    /**
     * Get the class name that implement alls methods about the RDF Repository
     * 
     * @return a String that contains the name of the class 
     */
    public String getCurrentRDFRepositoryClassName() {
        return currentRDFRepositoryClass;
    }
    

    /**
     * Get the class name that implements all methods about the relational data Repository
     * 
     * @return a String that contains the name of the class 
     */
    public String getCurrentDataRepositoryClassName() {
        return currentDataRepositoryClass;
    }
    
    
    /**
     * Get the class name that implements all methods used by the Security Manager
     * 
     * @return a String that contains the name of the class 
     */    
    public String getCurrentSecurityManagerClassName() {
        return currentPermissionsManagerClass;
    }
       
    /**
     * Get the main ServletContext
     * 
     * @return the main ServletContext
     */
    public ServletContext getMainServletContext() {
        return this.servletContext;
    }
    
    
    /**
     * Set the context for this web application
     * 
     * @param servletContext the main ServletContext
     */
    public void setMainServletContext(ServletContext servletContext) {
        if (servletContext != null && !servletContext.equals(this.servletContext)) {
            this.servletContext = servletContext;                
        }
        
        if (!contextParametersParsed) {
            parseMainContextParameters();
        }
    }    
    
    
    /**
     * Set the main servlet context path
     * 
     * @param servletContextPath the servlet context path
     */
    public void setServletContextPath(String servletContextPath) {
        if (servletContextPath != null && !servletContextPath.equals(this.servletContextPath)) {
            this.servletContextPath = servletContextPath;
        }
        
        if (!contextParametersParsed) {
            parseMainContextParameters();
        }
    }
    
    
    /**
     * Parse the main context parameters
     */
    private void parseMainContextParameters() {
     
        if (servletContext != null && !contextParametersParsed) {
            
            // Try to read the annotationserver version file
            Properties props = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties");
            try {
                props.load(is);
                this.annotationServerVersion = props.getProperty("version");
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, "Unable to load the version properties file.\n" + ex.getMessage());
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            
            final String errorMsg = "See the web.xml file.";
            
            // Hash algorithm for ID creation
            String tHash = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_HASH_TYPE);
            if (tHash != null && (tHash.equalsIgnoreCase("CRC32") || tHash.equalsIgnoreCase("MD5") || tHash.equalsIgnoreCase("SAH1")) ) {
                this.hashAlgorithm = tHash.toUpperCase();
            }
            
            String proxyTimeout = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SERVICES_PROXY_TIMEOUT);
            if (StringUtils.isNotBlank(proxyTimeout) && StringUtils.isNumeric(proxyTimeout)) {                    
                this.proxyAPITimeout = Integer.parseInt(proxyTimeout);
            }
            
            String proxyMimeTypes = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SERVICES_PROXY_MIMETYPES);
            if (StringUtils.isNotBlank(proxyMimeTypes)) {
                proxyMimeTypes = proxyMimeTypes.replaceAll(" ", "");
                String[] mimeTypesList = proxyMimeTypes.split(",");                
                if (mimeTypesList.length > 0) {
                    if (this.proxyAPIsupportedMimeTypes == null) {
                        this.proxyAPIsupportedMimeTypes = new ArrayList<String>();
                    }
                    
                    for (int i = 0; i < mimeTypesList.length; i++) {
                        String cMimeType = mimeTypesList[i];
                        if (StringUtils.isNotBlank(cMimeType)) {
                            this.proxyAPIsupportedMimeTypes.add(cMimeType);                                    
                        }
                    }
                }
            }
            
            // Repository
            String tRepoUrl = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_URL);
            if (StringUtils.isNotBlank(tRepoUrl)) {
                this.reporitoryURL = tRepoUrl;
            } else {
                logger.log(Level.SEVERE, "The repository or the DB URL is not specified. " + errorMsg);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            
            String tRepoPort = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_PORT);
            if (StringUtils.isNotBlank(tRepoPort) && StringUtils.isNumeric(tRepoPort)) {
                this.repositoryPort = tRepoPort;
            }
            
            String tRepoID = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_ID);
            if (StringUtils.isNotBlank(tRepoID)) {
                this.repositoryID = tRepoID;
            } else {
                logger.log(Level.SEVERE, "The repository ID or the DB name is not specified. " + errorMsg);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);                
            }
            
            String tRepoAuth = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_USEAUTHENTICATION);
            if (tRepoAuth != null) {
                
                if (tRepoAuth.equalsIgnoreCase("yes")) {
                    
                    String tUsername = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_USERNAME);
                    String tPassword = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DB_PASSWORD);
                    
                    if (tUsername != null && tUsername.length() > 0 && tPassword != null && tPassword.length() > 0) {
                        this.repositoryUseAuth  = true;
                        this.repositoryUsername = tUsername;
                        this.repositoryPassword = tPassword;
                    } else {
                        logger.log(Level.SEVERE, "Username or password are not correctly specified. " + errorMsg);
                        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                    }
                    
                } else {
                    this.repositoryUseAuth = false;
                } 
                
            }            
            
            // RDF Repository class
            String tRepositoryClass = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDFREPO_CLASS);            
            if (StringUtils.isNotBlank(tRepositoryClass)) {
                this.currentRDFRepositoryClass = tRepositoryClass;
            } 
            
            // Relationa Data Repository class
            String tDataRepositoryClass = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_DATAREPO_CLASS);
            if (StringUtils.isNotBlank(tDataRepositoryClass)) {
                this.currentDataRepositoryClass = tDataRepositoryClass;
            }
        
            // RDBMS configuration
            String dbDialect = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDBMS_HDIALECT);            
            if (StringUtils.isNotBlank(dbDialect)) {                
                this.rdbmsDialect = dbDialect;                
            }
            
            String dbDriver = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDBMS_DRIVER);
            if (StringUtils.isNotBlank(dbDriver)) {
                this.rdbmsDriver = dbDriver;
            }
            
            String dbConnectionUrl = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDBMS_CONNECTION_URL);
            if (StringUtils.isNotBlank(dbConnectionUrl)) {
                this.rdbmsConnectionUrl = dbConnectionUrl;
            }
            
            String dbUsername = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDBMS_USERNAME);
            if (StringUtils.isNotBlank(dbUsername)) {
                this.rdbmsUsername = dbUsername;
            }
            
            String dbPassword = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_RDBMS_PASSWORD);
            if (StringUtils.isNotBlank(dbPassword)) {
                this.rdbmsPassword = dbPassword;
            }
                
            // Security manager class
            String securityManager = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_PERMISSION_CLASS);
            if (StringUtils.isNotBlank(securityManager)) {
                this.currentPermissionsManagerClass = securityManager;
            }
            
            String defaultNotebookStatus = servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_NOTEBOOK_DEFAULTSTATUS);
            if (StringUtils.isNotBlank(defaultNotebookStatus)) {
                if (defaultNotebookStatus.equals("1")) {
                    newNotebooksPublicAsDefault = true;
                } else if (defaultNotebookStatus.equals("0")) {
                    newNotebooksPublicAsDefault = false;
                }
            }
            String smtpH =  servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SMTP_HOST);
            if (StringUtils.isNotBlank(smtpH)){
                mailSmtpHost = smtpH;
            }
            
            String smtpP =  servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SMTP_PORT);
            if (StringUtils.isNotBlank(smtpP)){
                mailSmtpPort = smtpP;
            }
            
            String smtpPass =  servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SMTP_PASSWORD);
            if (StringUtils.isNotBlank(smtpPass)){
                mailSmtpPassword = smtpPass;
            }
            
            String smtpAuth =  servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SMTP_AUTH);
            if (StringUtils.isNotBlank(smtpAuth)){
                mailSmtpAuth = smtpAuth;
            }
            
            String smtpUser =  servletContext.getInitParameter(SemlibConstantsConfiguration.PARAM_SMTP_USER);
            if (StringUtils.isNotBlank(smtpUser)){
                mailSmtpUser = smtpUser;
            }
            logger.log(Level.INFO, "Configuration parameters parsed!");
            
            contextParametersParsed = true;
        }        
    }
        
    public int getProxyAPITimeout() {
        return this.proxyAPITimeout;
    }
    
    public List<String> getProxySupportedMimeTypes() {
        return this.proxyAPIsupportedMimeTypes;
    }
    
    public String getRepositoryUrl() {
        return this.reporitoryURL;
    }
    
    public String getRepositoryPort() {
        return this.repositoryPort;
    }
    
    public String getRepositoryID() {
        return this.repositoryID;
    }
    
    public String getUsername() {
        return this.repositoryUsername;
    }
    
    public String getPassword() {
        return this.repositoryPassword;
    }
    
    public String getRDBMSDialect() {
        return this.rdbmsDialect;
    }
    
    public String getRDBMSDriver() {
        return this.rdbmsDriver;
    }
    
    public String getRDBMSConnectionUrl() {
        return this.rdbmsConnectionUrl;
    }
    
    public String getRDBMSUsername() {
        return this.rdbmsUsername;
    }
    
    public String getRDBMSPassword() {
        return this.rdbmsPassword;
    }
    
    
    
    public boolean useAuthenticationForRepository() {
        return this.repositoryUseAuth;
    }
    
    public String getAuthenticationFormPath() {
        return this.authenticationFormPath;
    }
        
    public String getServletContextPath() {
        return this.servletContextPath;
    }
    
    public boolean isDefaultNotebookStatusPublic() {
        return this.newNotebooksPublicAsDefault;
    }
    
    public boolean isAnonymousUserInitialized() {
        return this.anonymousUserInitialized;
    }
    
    public void setAuthenticationFormPath(String formPath) {
        this.authenticationFormPath = formPath;
    }
    
    public void setAnonymousUserInitialized(boolean initialized) {
        this.anonymousUserInitialized = initialized;
    }
    
    public void setRDBMSParameters(String dialect, String driver, String url, String user, String password) {
        this.rdbmsDialect = dialect;
        this.rdbmsDriver = driver;
        this.rdbmsConnectionUrl = url;
        this.rdbmsUsername = user;
        this.rdbmsPassword = password;
    }

    public String getSmtpMailhost() {
        return this.mailSmtpHost;
    }
    public String getSmtpMailport() {
        return this.mailSmtpPort;
    }

    public String getSmtpMailpassword() {
        return this.mailSmtpPassword;
    }

    public String getSmtpMailauth() {
        return this.mailSmtpAuth;
    }
    
     public String getSmtpMailuser() {
        return this.mailSmtpUser;
    }
        
}
