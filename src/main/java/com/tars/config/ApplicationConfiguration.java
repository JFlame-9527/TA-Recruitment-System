package com.tars.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tars.entity.bean.User;
import com.tars.repository.JsonRepository;
import com.tars.util.FileUtils;
import com.tars.util.TestDataGenerator;
import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;

/**
 * Application configuration manager that handles environment settings and data initialization.
 * <p>
 * This singleton class is responsible for:
 * <ul>
 *   <li>Loading and parsing configuration from {@code config.json}</li>
 *   <li>Configuring logging levels based on environment (dev/prod/test)</li>
 *   <li>Initializing static paths for {@link JsonRepository} and {@link FileUtils}</li>
 *   <li>Managing data directory lifecycle (clean, create, integrity check)</li>
 *   <li>Creating admin account on fresh installation</li>
 *   <li>Generating test data in development mode</li>
 * </ul>
 * </p>
 * <p>
 * Configuration loading priority:
 * <ol>
 *   <li>External file: {@code <webRoot>/config/config.json}</li>
 *   <li>Classpath resource: {@code config.json} in resources directory</li>
 * </ol>
 * </p>
 * <p>
 * Data initialization modes:
 * <ul>
 *   <li><b>Clean Mode</b> ({@code cleanData=true}): Clears all data and recreates admin account</li>
 *   <li><b>Normal Mode</b> ({@code cleanData=false}): Checks data integrity, ensures admin exists</li>
 *   <li><b>Dev Mode with Generation</b>: In dev/test environment with {@code generateData=true}, generates sample data</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This class uses double-checked locking for thread-safe singleton initialization.
 * The {@link #initialize(ServletContext)} method should be called once during application startup
 * (typically in a {@link jakarta.servlet.ServletContextListener}).
 * </p>
 *
 * @author Jflame
 * @version 4.0.0
 * @since 2026/5/1
 * @see QwenConfiguration
 * @see JsonRepository
 * @see FileUtils
 */
@Getter
@Slf4j
public class ApplicationConfiguration {
    private static final String CONFIG_FILE = "config.json";
    private static final String EXTERNAL_CONFIG_DIR = "config/";
    private static final String RESOURCE_CONFIG_PATH = CONFIG_FILE;

    private static volatile ApplicationConfiguration instance;

    private final String environment;
    private final boolean cleanData;
    private final boolean generateData;

    private final String dataDir;
    private final String fileDir;

    private static String webRootPath;

    /**
     * Constructs a new ApplicationConfiguration instance.
     *
     * @param environment  Application environment (dev, prod, or test)
     * @param cleanData    Whether to clean data directory on startup
     * @param generateData Whether to generate test data (only in dev/test mode)
     * @param dataDir      Directory name for storing JSON data files
     * @param fileDir      Directory name for storing uploaded files
     */
    private ApplicationConfiguration(String environment, boolean cleanData, boolean generateData,
                                     String dataDir, String fileDir) {
        this.environment = environment;
        this.cleanData = cleanData;
        this.generateData = generateData;
        this.dataDir = dataDir;
        this.fileDir = fileDir;
    }

    /**
     * Returns the singleton instance of ApplicationConfiguration.
     * <p>
     * Uses double-checked locking for thread-safe lazy initialization.
     * </p>
     *
     * @return The singleton ApplicationConfiguration instance
     * @throws IllegalStateException if {@link #initialize(ServletContext)} has not been called yet
     * @see #initialize(ServletContext)
     */
    public static ApplicationConfiguration getInstance() {
        if (instance == null) {
            synchronized (ApplicationConfiguration.class) {
                if (instance == null) {
                    throw new IllegalStateException(
                            "ApplicationConfig not initialized. Call initialize() first.");
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the application configuration during web application startup.
     * <p>
     * This method should be called once from a {@link jakarta.servlet.ServletContextListener}
     * or similar initialization hook. It performs the following steps:
     * <ol>
     *   <li>Retrieves the web application root path from ServletContext</li>
     *   <li>Loads and validates configuration from config.json</li>
     *   <li>Configures logging level based on environment</li>
     *   <li>Creates necessary directories (data, upload)</li>
     *   <li>Initializes static paths in {@link JsonRepository} and {@link FileUtils}</li>
     *   <li>Executes data operations (clean, create admin, generate test data)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> If called multiple times, subsequent calls will log a warning and skip initialization.
     * </p>
     *
     * @param servletContext The ServletContext providing access to web application resources
     * @throws RuntimeException if initialization fails due to configuration errors or I/O issues
     * @see #initializeForTest(String)
     */
    public static synchronized void initialize(ServletContext servletContext) {
        if (instance != null) {
            log.warn("ApplicationConfig already initialized, skipping");
            return;
        }

        try {
            webRootPath = servletContext.getRealPath("");
            log.info("WebAppRootPath: {}", webRootPath);

            initializeInternal(webRootPath);

            initializeStaticPaths();

            executeDataOperations();
        } catch (Exception e) {
            log.error("Failed to initialize ApplicationConfig", e);
            throw new RuntimeException("Failed to initialize ApplicationConfig: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes the application configuration for testing purposes without ServletContext.
     * <p>
     * This method is designed for unit tests and integration tests where a full web container
     * is not available. It accepts a test resources directory path instead of deriving it
     * from ServletContext.
     * </p>
     * <p>
     * <b>Usage in tests:</b>
     * <pre>{@code
     * @Before
     * public void setUp() {
     *     String testResourcesPath = "target/test-classes";
     *     ApplicationConfiguration.initializeForTest(testResourcesPath);
     * }
     * }</pre>
     * </p>
     *
     * @param testWebRootPath Path to test resources directory (e.g., "target/test-classes")
     * @throws RuntimeException if initialization fails
     * @see #initialize(ServletContext)
     */
    public static synchronized void initializeForTest(String testWebRootPath) {
        if (instance != null) {
            log.warn("ApplicationConfig already initialized, skipping");
            return;
        }

        try {
            webRootPath = testWebRootPath;
            initializeInternal(webRootPath);
            initializeStaticPaths();
            executeDataOperations();
        } catch (Exception e) {
            log.error("Failed to initialize ApplicationConfig for tests", e);
            throw new RuntimeException("Failed to initialize ApplicationConfig: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes static paths for utility classes and creates necessary directories.
     * <p>
     * This method:
     * <ol>
     *   <li>Constructs absolute paths for data and file directories</li>
     *   <li>Creates directories if they don't exist</li>
     *   <li>Sets the paths in {@link JsonRepository} and {@link FileUtils}</li>
     *   <li>Logs the configured paths for verification</li>
     * </ol>
     * </p>
     * <p>
     * <b>Path structure:</b>
     * <pre>
     * &lt;webRoot&gt;/
     * ├── data/          ← JSON data files (user.json, position.json, etc.)
     * └── upload/        ← Uploaded files (resumes, documents, etc.)
     * </pre>
     * </p>
     */
    private static void initializeStaticPaths() {
        log.info("Initializing static paths for utility classes...");

        String dataPath = webRootPath + File.separator + instance.dataDir + File.separator;
        String filePath = webRootPath + File.separator + instance.fileDir + File.separator;

        ensureDirectoryExists(dataPath, "Data directory");
        ensureDirectoryExists(filePath, "File upload directory");

        JsonRepository.setDataDir(dataPath);
        log.info("JsonRepository data dir: {}", JsonRepository.getDataDir());
        
        FileUtils.setFileDir(filePath);
        log.info("FileUtils upload dir: {}", FileUtils.getFileDir());

        log.info("Static paths initialized successfully");
        log.info("Web root path: {}", webRootPath);
        log.info("Data will be stored in: {}", dataPath);
        log.info("Files will be stored in: {}", filePath);
        
        log.info("Static paths initialized successfully");
    }

    /**
     * Ensures a directory exists, creating it if necessary.
     *
     * @param dirPath     Absolute path of the directory to check/create
     * @param description Human-readable description for logging purposes
     * @throws RuntimeException if directory creation fails
     */
    private static void ensureDirectoryExists(String dirPath, String description) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("{} created: {}", description, dirPath);
            } else {
                log.error("Failed to create {}: {}", description, dirPath);
                throw new RuntimeException("Failed to create directory: " + dirPath);
            }
        } else {
            log.debug("{} already exists: {}", description, dirPath);
        }
    }

    /**
     * Returns the absolute path to the data directory.
     *
     * @return Absolute path string for data storage (e.g., "/var/www/app/data/")
     * @see #dataDir
     */
    public String getDataPath() {
        return webRootPath + File.separator + dataDir;
    }
    
    /**
     * Returns the absolute path to the file upload directory.
     *
     * @return Absolute path string for file uploads (e.g., "/var/www/app/upload/")
     * @see #fileDir
     */
    public String getFilePath() {
        return webRootPath + File.separator + fileDir;
    }

    /**
     * Internal initialization logic shared by both production and test initialization.
     *
     * @param webAppRootPath Root path of the web application
     */
    private static void initializeInternal(String webAppRootPath) {
        ConfigData configData = loadConfigData(webAppRootPath);

        validateEnvironment(configData.environment());

        configureLoggingLevel(configData.environment());

        instance = new ApplicationConfiguration(
                configData.environment(),
                configData.cleanData(),
                configData.generateData(),
                configData.dataDir(),
                configData.fileDir()
        );

        log.info("ApplicationConfig initialized successfully");
        log.info("Environment: {}", instance.environment);
        log.info("Clean Data: {}", instance.cleanData);
        log.info("Generate Data: {}", instance.generateData);
        log.info("Data Dir: {}", instance.dataDir);
        log.info("File Dir: {}", instance.fileDir);
    }

    /**
     * Configures the logging level based on the application environment.
     * <p>
     * Logging level strategy:
     * <ul>
     *   <li><b>Production</b> ({@code environment=prod}): Sets root logger to INFO level to reduce log volume</li>
     *   <li><b>Development/Test</b> (any other value): Sets root logger to DEBUG level for detailed diagnostics</li>
     * </ul>
     * </p>
     * <p>
     * This method directly manipulates the Logback LoggerContext to apply the appropriate level.
     * Note: Uses System.out.println instead of log to avoid circular dependency during initialization.
     * </p>
     *
     * @param environment The application environment string from configuration
     * @see Level#INFO
     * @see Level#DEBUG
     */
    private static void configureLoggingLevel(String environment) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        if ("prod".equalsIgnoreCase(environment)) {
            rootLogger.setLevel(Level.INFO);
            System.out.println("Production mode: Logging level set to INFO");
        } else {
            // Dev, Test, etc.
            rootLogger.setLevel(Level.DEBUG);
            System.out.println("Development mode: Logging level set to DEBUG");
        }
    }

    /**
     * Loads configuration data from config.json file.
     * <p>
     * Loading priority:
     * <ol>
     *   <li>External file: {@code <webRoot>/config/config.json}</li>
     *   <li>Classpath resource: {@code config.json} in classpath root</li>
     * </ol>
     * </p>
     * <p>
     * Expected JSON structure:
     * <pre>{@code
     * {
     *   "environment": "dev",
     *   "active": {
     *     "cleanData": false,
     *     "generateData": true
     *   },
     *   "paths": {
     *     "data": "data",
     *     "file": "upload"
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param webAppRootPath Root path for locating external configuration file
     * @return Parsed configuration data as a ConfigData record
     * @throws RuntimeException if configuration file cannot be loaded or parsed
     * @see #loadConfigJson(ObjectMapper, String)
     * @see ConfigData
     */
    private static ConfigData loadConfigData(String webAppRootPath) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode configNode = loadConfigJson(mapper, webAppRootPath);

            if (configNode == null) {
                throw new RuntimeException("Configuration file not found");
            }

            String environment = extractStringValue(configNode, "environment", "dev");
            
            JsonNode activeNode = configNode.get("active");
            boolean cleanData = false;
            boolean generateData = false;
            
            if (activeNode != null && !activeNode.isNull()) {
                cleanData = extractBooleanValue(activeNode, "cleanData", false);
                generateData = extractBooleanValue(activeNode, "generateData", false);
            }

            JsonNode pathsNode = configNode.get("paths");
            String dataDir = "data";
            String fileDir = "upload";
            
            if (pathsNode != null && !pathsNode.isNull()) {
                dataDir = extractStringValue(pathsNode, "data", "data");
                fileDir = extractStringValue(pathsNode, "file", "upload");
            }

            return new ConfigData(environment, cleanData, generateData, dataDir, fileDir);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the configuration JSON from external file or classpath resource.
     *
     * @param mapper         Jackson ObjectMapper for JSON parsing
     * @param webAppRootPath Root path for external configuration file location
     * @return JsonNode representing the configuration, or null if not found
     * @see #EXTERNAL_CONFIG_DIR
     * @see #RESOURCE_CONFIG_PATH
     */
    private static JsonNode loadConfigJson(ObjectMapper mapper, String webAppRootPath) {
        try {
            String externalConfigPath = webAppRootPath + File.separator + EXTERNAL_CONFIG_DIR + CONFIG_FILE;
            File externalConfigFile = new File(externalConfigPath);

            if (externalConfigFile.exists()) {
                log.info("Loading configuration from external file: {}", externalConfigFile.getAbsolutePath());
                return mapper.readTree(externalConfigFile);
            }

            InputStream resourceStream = ApplicationConfiguration.class.getClassLoader()
                    .getResourceAsStream(RESOURCE_CONFIG_PATH);

            if (resourceStream != null) {
                log.info("Loading configuration from classpath resource: {}", RESOURCE_CONFIG_PATH);
                JsonNode node = mapper.readTree(resourceStream);
                resourceStream.close();
                return node;
            }

            log.error("Configuration file not found in {} or classpath: {}",
                    externalConfigPath, RESOURCE_CONFIG_PATH);
            return null;

        } catch (Exception e) {
            log.error("Error loading configuration file", e);
            return null;
        }
    }

    /**
     * Executes data operations based on configuration settings.
     * <p>
     * Operation modes:
     * <ul>
     *   <li><b>Clean Mode</b>: Deletes all data files, recreates admin account, optionally generates test data</li>
     *   <li><b>Normal Mode</b>: Performs data integrity check to ensure admin account exists</li>
     * </ul>
     * </p>
     * <p>
     * Test data generation only occurs when ALL conditions are met:
     * <ul>
     *   <li>{@code cleanData = true}</li>
     *   <li>{@code generateData = true}</li>
     *   <li>Environment is "dev" or "test"</li>
     * </ul>
     * </p>
     *
     * @throws RuntimeException if any data operation fails
     * @see #cleanDataDirectory()
     * @see #createAdminAccount()
     * @see #ensureDataIntegrity()
     * @see #generateTestData()
     */
    private static void executeDataOperations() {
        log.info("Executing data operations...");

        try {
            if (instance.cleanData) {
                // Clean mode: clear all data and recreate admin
                cleanDataDirectory();
                createAdminAccount();
                log.info("Data directory cleaned and admin account created");

                if (("dev".equalsIgnoreCase(instance.environment) || "test".equalsIgnoreCase(instance.environment)) && instance.generateData) {
                    log.info("Development mode with data generation enabled");
                    generateTestData();
                }
            } else {
                // Normal mode: check data integrity
                ensureDataIntegrity();
            }
        } catch (Exception e) {
            log.error("Failed to execute data operations", e);
            throw new RuntimeException("Data operations failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures data integrity on normal startup (non-clean mode).
     * <p>
     * Integrity checks:
     * <ol>
     *   <li>If data directory doesn't exist → creates it and initializes admin account</li>
     *   <li>If data directory exists but user.json is missing → creates admin account</li>
     *   <li>If user.json exists but admin account is missing → throws exception (possible tampering)</li>
     *   <li>If everything is intact → logs success message</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security Note:</b> Missing admin account in an existing data directory triggers a critical error
     * because it may indicate data tampering or corruption. In such cases, administrators should either:
     * <ul>
     *   <li>Restore from backup</li>
     *   <li>Set {@code cleanData=true} to reset the system</li>
     * </ul>
     * </p>
     *
     * @throws RuntimeException if admin account is missing from existing data (integrity violation)
     * @see #isAdminAccountExists()
     * @see #createAdminAccount()
     */
    private static void ensureDataIntegrity() {
        File dataDir = new File(instance.getDataPath());
        
        // Check if data directory exists
        if (!dataDir.exists()) {
            log.info("Data directory does not exist, creating initial setup...");
            dataDir.mkdirs();
            createAdminAccount();
            log.info("Initial setup completed with admin account");
            return;
        }

        // Data directory exists, check if it's empty (no user files)
        File userFile = new File(dataDir, "user.json");
        if (!userFile.exists()) {
            log.info("Data directory exists but is empty, creating initial setup...");
            createAdminAccount();
            log.info("Initial setup completed with admin account");
            return;
        }

        // User file exists, check if admin account exists
        if (!isAdminAccountExists()) {
            log.error("CRITICAL: Admin account not found in existing data directory!");
            log.error("This may indicate data tampering or corruption.");
            log.error("Please restore from backup or set cleanData=true to reset.");
            throw new RuntimeException("Admin account missing - data integrity check failed");
        }

        log.info("Data integrity check passed - admin account exists");
    }

    /**
     * Checks if an admin account exists in the user repository.
     * <p>
     * An admin account is identified by:
     * <ul>
     *   <li>{@code role = 0} (admin role)</li>
     *   <li>{@code name = "admin"}</li>
     * </ul>
     * </p>
     *
     * @return true if admin account exists, false otherwise
     * @see User#getRole()
     * @see User#getName()
     */
    private static boolean isAdminAccountExists() {
        try {
            JsonRepository<User> userRepo = new JsonRepository<>(User.class);
            java.util.List<User> users = userRepo.loadAllEntities();
            
            if (users == null || users.isEmpty()) {
                return false;
            }

            // Check if any user has role=0 (admin) and name="admin"
            return users.stream()
                    .anyMatch(user -> user != null 
                            && user.getRole() == 0 
                            && "admin".equals(user.getName()));
        } catch (Exception e) {
            log.error("Failed to check admin account existence", e);
            return false;
        }
    }

    /**
     * Cleans all data and upload directories by deleting all files.
     * <p>
     * This method removes all files from:
     * <ul>
     *   <li>Data directory (user.json, position.json, profile.json, etc.)</li>
     *   <li>Upload directory (resumes, documents, etc.)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Warning:</b> This operation is irreversible. All data will be permanently deleted.
     * Use only in development/testing environments or when intentional data reset is required.
     * </p>
     *
     * @see #cleanDirectory(String)
     */
    private static void cleanDataDirectory() {
        cleanDirectory(instance.getDataPath());

        cleanDirectory(instance.getFilePath());
        
        log.info("All directories cleaned successfully");
    }
    
    /**
     * Deletes all files in a specified directory.
     * <p>
     * Only deletes regular files, not subdirectories. Logs warnings for files that fail to delete.
     * </p>
     *
     * @param dirPath Absolute path of the directory to clean
     * @see #cleanDataDirectory()
     */
    private static void cleanDirectory(String dirPath) {
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            log.debug("Directory does not exist, skipping cleanup: {}", dirPath);
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.debug("Deleted file: {}", file.getName());
                    } else {
                        log.warn("Failed to delete file: {}", file.getName());
                    }
                }
            }
        }
        
        log.info("Directory cleaned: {}", dirPath);
    }

    /**
     * Creates the default admin account.
     * <p>
     * Admin account details:
     * <ul>
     *   <li><b>Name:</b> admin</li>
     *   <li><b>Password:</b> admin123 (MD5 hashed)</li>
     *   <li><b>Role:</b> 0 (administrator)</li>
     *   <li><b>Status:</b> 0 (active)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Security Note:</b> The default password should be changed immediately after first login
     * in production environments.
     * </p>
     *
     * @throws RuntimeException if admin account creation fails
     * @see DigestUtils#md5Hex(String)
     * @see User
     */
    private static void createAdminAccount() {
        try {
            JsonRepository<User> userRepo = new JsonRepository<>(User.class);
            
            User admin = new User();
            admin.setName("admin");
            admin.setPassword(DigestUtils.md5Hex("admin123"));
            admin.setRole(0);
            admin.setStatus(0);
            userRepo.saveEntity(admin);
            
            log.info("Admin account created - ID: {}, Name: admin", admin.getId());
        } catch (Exception e) {
            log.error("Failed to create admin account", e);
            throw new RuntimeException("Admin account creation failed", e);
        }
    }

    /**
     * Generates test data using the TestDataGenerator utility.
     * <p>
     * This method delegates to {@link TestDataGenerator#run()} which creates:
     * <ul>
     *   <li>Sample user accounts (TA, MO roles)</li>
     *   <li>Position listings with various requirements</li>
     *   <li>TA profiles with skills and experience</li>
     *   <li>MO profiles for module owners</li>
     *   <li>Application records linking candidates to positions</li>
     * </ul>
     * </p>
     * <p>
     * Test data generation is typically used in development and testing environments
     * to populate the system with realistic sample data for demonstration purposes.
     * </p>
     *
     * @see TestDataGenerator#run()
     */
    private static void generateTestData() {
        log.info("Starting AI-enhanced test data generation...");
        TestDataGenerator.run();
        log.info("Test data generation completed");
    }

    /**
     * Validates the environment configuration value.
     * <p>
     * Acceptable values (case-insensitive):
     * <ul>
     *   <li>{@code dev} - Development environment</li>
     *   <li>{@code prod} - Production environment</li>
     *   <li>{@code test} - Testing environment</li>
     * </ul>
     * </p>
     * <p>
     * Unknown values trigger a warning but do not prevent initialization.
     * Null or empty values throw an IllegalArgumentException.
     * </p>
     *
     * @param environment Environment string to validate
     * @throws IllegalArgumentException if environment is null or empty
     */
    private static void validateEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }
        
        if (!"dev".equalsIgnoreCase(environment) && !"prod".equalsIgnoreCase(environment) && !"test".equalsIgnoreCase(environment)) {
            log.warn("Unknown environment: {}. Expected 'dev', 'test' or 'prod'", environment);
        }
    }

    /**
     * Extracts a string value from a JSON node with fallback to default value.
     *
     * @param node         Parent JSON node
     * @param fieldName    Field name to extract
     * @param defaultValue Default value if field is null or missing
     * @return Extracted string value or default value
     */
    private static String extractStringValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : defaultValue;
    }

    /**
     * Extracts a boolean value from a JSON node with fallback to default value.
     *
     * @param node         Parent JSON node
     * @param fieldName    Field name to extract
     * @param defaultValue Default value if field is null or missing
     * @return Extracted boolean value or default value
     */
    private static boolean extractBooleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asBoolean() : defaultValue;
    }

    /**
     * Immutable record holding configuration data parsed from config.json.
     *
     * @param environment  Application environment (dev, prod, test)
     * @param cleanData    Flag indicating whether to clean data on startup
     * @param generateData Flag indicating whether to generate test data
     * @param dataDir      Directory name for JSON data storage
     * @param fileDir      Directory name for file uploads
     */
    private record ConfigData(String environment, boolean cleanData, boolean generateData,
                              String dataDir, String fileDir) {
    }
}
