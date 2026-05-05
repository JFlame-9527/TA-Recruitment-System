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

import java.io.File;
import java.io.InputStream;

/**
 * Application configuration manager
 * Handles environment settings and data initialization
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/5/1
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

    private ApplicationConfiguration(String environment, boolean cleanData, boolean generateData,
                                     String dataDir, String fileDir) {
        this.environment = environment;
        this.cleanData = cleanData;
        this.generateData = generateData;
        this.dataDir = dataDir;
        this.fileDir = fileDir;
    }

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
     * Initialize configuration for testing purposes (without ServletContext)
     *
     * @param testWebRootPath Path to test resources directory
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

    public String getDataPath() {
        return webRootPath + File.separator + dataDir;
    }
    
    public String getFilePath() {
        return webRootPath + File.separator + fileDir;
    }

    private static void initializeInternal(String webAppRootPath) {
        ConfigData configData = loadConfigData(webAppRootPath);

        validateEnvironment(configData.environment());

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

    private static void executeDataOperations() {
        if (!instance.cleanData && !instance.generateData) {
            log.info("No data operations required");
            return;
        }

        log.info("Executing data operations...");

        try {
            if (instance.cleanData) {
                cleanDataDirectory();
                log.info("Data directory cleaned successfully");

                createAdminAccount();
                log.info("Admin account created successfully");

                if ("dev".equalsIgnoreCase(instance.environment) && instance.generateData) {
                    log.info("Development mode with data generation enabled");
                    generateTestData();
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute data operations", e);
            throw new RuntimeException("Data operations failed: " + e.getMessage(), e);
        }
    }

    private static void cleanDataDirectory() {
        cleanDirectory(instance.getDataPath());

        cleanDirectory(instance.getFilePath());
        
        log.info("All directories cleaned successfully");
    }
    
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

    private static void generateTestData() {
        log.info("Starting AI-enhanced test data generation...");
        TestDataGenerator.run();
        log.info("Test data generation completed");
    }

    private static void validateEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }
        
        if (!"dev".equalsIgnoreCase(environment) && !"prod".equalsIgnoreCase(environment)) {
            log.warn("Unknown environment: {}. Expected 'dev' or 'prod'", environment);
        }
    }

    private static String extractStringValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : defaultValue;
    }

    private static boolean extractBooleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asBoolean() : defaultValue;
    }

    private record ConfigData(String environment, boolean cleanData, boolean generateData,
                              String dataDir, String fileDir) {
    }
}
