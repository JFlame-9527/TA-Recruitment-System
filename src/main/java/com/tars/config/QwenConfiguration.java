package com.tars.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Configuration manager for Qwen AI service integration.
 * <p>
 * This singleton class loads and manages all configuration required for interacting
 * with Alibaba's Qwen (Tongyi Qianwen) AI models through the DashScope API. It provides:
 * </p>
 * <ul>
 *   <li>API authentication credentials (API key and base URL)</li>
 *   <li>Model-specific parameters for three model types:
 *     <ul>
 *       <li><b>qwen</b>: Standard generation model for portrait and skill analysis</li>
 *       <li><b>qwen-long</b>: Long-context model for resume content extraction</li>
 *       <li><b>qwen-vector</b>: Embedding model for vector similarity matching</li>
 *     </ul>
 *   </li>
 *   <li>Portrait matching weight configuration</li>
 * </ul>
 * <p>
 * <b>Configuration Loading Priority:</b>
 * <ol>
 *   <li>External file: {@code <webRoot>/config/qwen_config.json}</li>
 *   <li>Classpath resource: {@code qwen_config.json} in resources directory</li>
 * </ol>
 * </p>
 * <p>
 * <b>Security Features:</b>
 * <ul>
 *   <li>API key format validation using regex pattern {@code ^sk-[a-zA-Z0-9]{32,}$}</li>
 *   <li>Base URL validation to ensure proper HTTP/HTTPS format</li>
 *   <li>HTTPS recommendation warning for production use</li>
 *   <li>API key masking in logs to prevent credential exposure</li>
 * </ul>
 * </p>
 * <p>
 * <b>Fallback Strategy:</b> If specific model or weight configurations are missing from
 * the config file, the system uses sensible defaults to ensure graceful degradation.
 * </p>
 * <p>
 * <b>Thread Safety:</b> Uses double-checked locking for thread-safe singleton initialization.
 * The {@link #initialize(ServletContext)} method should be called once during application startup.
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see ModelOption
 * @see Weight
 * @see com.tars.ai.PortraitGenerator
 * @see com.tars.ai.SkillExtractor
 * @see com.tars.ai.SkillMatcher
 */
@Getter
@Slf4j
public class QwenConfiguration {
    private static final String CONFIG_FILE = "qwen_config.json";
    private static final String EXTERNAL_CONFIG_DIR = "config/";
    private static final String RESOURCE_CONFIG_PATH = CONFIG_FILE;

    private static final Pattern API_KEY_PATTERN = Pattern.compile("^sk-[a-zA-Z0-9]{32,}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w.-]+(/[\\w./-]*)?$");

    private static volatile QwenConfiguration instance;

    private final String apiKey;
    private final String baseUrl;
    private final ModelOption qwen;
    private final ModelOption qwenLong;
    private final ModelOption qwenVector;
    private final Weight weight;

    /**
     * Constructs a new QwenConfiguration instance.
     *
     * @param apiKey      API key for DashScope authentication
     * @param baseUrl     Base URL for Qwen API endpoint
     * @param qwen        Configuration for standard Qwen model
     * @param qwenLong    Configuration for Qwen-long model (long context)
     * @param qwenVector  Configuration for Qwen embedding model
     * @param weight      Portrait matching weight configuration
     */
    private QwenConfiguration(String apiKey, String baseUrl, ModelOption qwen, ModelOption qwenLong, ModelOption qwenVector, Weight weight) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.qwen = qwen;
        this.qwenLong = qwenLong;
        this.qwenVector = qwenVector;
        this.weight = weight;
    }

    /**
     * Returns the singleton instance of QwenConfiguration.
     * <p>
     * Uses double-checked locking for thread-safe lazy initialization.
     * </p>
     *
     * @return The singleton QwenConfiguration instance
     * @throws IllegalStateException if {@link #initialize(ServletContext)} has not been called yet
     * @see #initialize(ServletContext)
     */
    public static QwenConfiguration getInstance() {
        if (instance == null) {
            synchronized (QwenConfiguration.class) {
                if (instance == null) {
                    throw new IllegalStateException(
                            "QwenConfiguration not initialized. Call initialize() first.");
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the Qwen configuration during web application startup.
     * <p>
     * This method should be called once from a {@link jakarta.servlet.ServletContextListener}
     * or similar initialization hook. It performs the following steps:
     * <ol>
     *   <li>Retrieves the web application root path from ServletContext</li>
     *   <li>Loads configuration from qwen_config.json</li>
     *   <li>Validates API key format and base URL</li>
     *   <li>Creates the singleton instance with parsed configuration</li>
     *   <li>Logs configuration summary (with masked API key)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> If called multiple times, subsequent calls will log a warning and skip initialization.
     * </p>
     *
     * @param servletContext The ServletContext providing access to web application resources
     * @throws RuntimeException if initialization fails due to configuration errors or validation failures
     * @see #initializeForTest(String)
     * @see #validateApiKey(String)
     * @see #validateBaseUrl(String)
     */
    public static synchronized void initialize(ServletContext servletContext) {
        if (instance != null) {
            log.warn("QwenConfiguration already initialized, skipping");
            return;
        }

        try {
            String webAppRootPath = servletContext.getRealPath("");
            initializeInternal(webAppRootPath);
        } catch (Exception e) {
            log.error("Failed to initialize QwenConfiguration", e);
            throw new RuntimeException("Failed to initialize QwenConfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes the Qwen configuration for testing purposes without ServletContext.
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
     *     QwenConfiguration.initializeForTest(testResourcesPath);
     * }
     * }</pre>
     * </p>
     *
     * @param webAppRootPath Path to test resources directory (e.g., "target/test-classes")
     * @throws RuntimeException if initialization fails
     * @see #initialize(ServletContext)
     */
    public static synchronized void initializeForTest(String webAppRootPath) {
        if (instance != null) {
            log.warn("QwenConfiguration already initialized, skipping");
            return;
        }

        try {
            initializeInternal(webAppRootPath);
        } catch (Exception e) {
            log.error("Failed to initialize QwenConfiguration for tests", e);
            throw new RuntimeException("Failed to initialize QwenConfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Internal initialization logic shared by both production and test initialization.
     *
     * @param webAppRootPath Root path of the web application or test resources
     */
    private static void initializeInternal(String webAppRootPath) {
        ConfigData configData = loadConfigData(webAppRootPath);

        validateApiKey(configData.apiKey());
        validateBaseUrl(configData.baseUrl());

        instance = new QwenConfiguration(
                configData.apiKey(),
                configData.baseUrl(),
                configData.qwen(),
                configData.qwenLong(),
                configData.qwenVector(),
                configData.weight()
        );

        log.info("QwenConfiguration initialized successfully");
        log.info("API Key: {}", maskValue(instance.apiKey));
        log.info("Base URL: {}", instance.baseUrl);

        if (instance.qwen != null) {
            log.info("Qwen: {}", instance.qwen);
        }
        if (instance.qwenLong != null) {
            log.info("Qwen-Long: {}", instance.qwenLong);
        }
        if (instance.qwenVector != null) {
            log.info("Qwen-Vector: {}", instance.qwenVector);
        }
    }

    /**
     * Loads configuration data from qwen_config.json file.
     * <p>
     * Expected JSON structure:
     * <pre>{@code
     * {
     *   "apiKey": "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
     *   "baseUrl": "https://dashscope.aliyuncs.com/api/v1",
     *   "qwen": {
     *     "model": "qwen-max",
     *     "temperature": 0.7,
     *     "topP": 0.8,
     *     "topK": 50,
     *     "repetitionPenalty": 1.1,
     *     "maxTokens": 2000,
     *     "dimension": 0
     *   },
     *   "long": {
     *     "model": "qwen-long",
     *     "temperature": 0.3,
     *     "maxTokens": 8000
     *   },
     *   "vector": {
     *     "model": "text-embedding-v2",
     *     "dimension": 1536
     *   },
     *   "weight": {
     *     "skills": 0.5,
     *     "experience": 0.3,
     *     "softSkills": 0.2
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param webAppRootPath Root path for locating external configuration file
     * @return Parsed configuration data as a ConfigData record
     * @throws RuntimeException if configuration file cannot be loaded or parsed
     * @see #loadConfigJson(ObjectMapper, String)
     * @see #parseModelOption(JsonNode, String)
     * @see #parseWeight(JsonNode)
     */
    private static ConfigData loadConfigData(String webAppRootPath) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode configNode = loadConfigJson(mapper, webAppRootPath);

            if (configNode == null) {
                throw new RuntimeException("Configuration file not found");
            }

            String apiKey = extractStringValue(configNode, "apiKey");
            String baseUrl = extractStringValue(configNode, "baseUrl");
            ModelOption qwen = parseModelOption(configNode, "qwen");
            ModelOption qwenLong = parseModelOption(configNode, "long");
            ModelOption qwenVector = parseModelOption(configNode, "vector");
            Weight weight = parseWeight(configNode);

            return new ConfigData(apiKey, baseUrl, qwen, qwenLong, qwenVector, weight);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the configuration JSON from external file or classpath resource.
     * <p>
     * Loading priority:
     * <ol>
     *   <li>External file: {@code <webRoot>/config/qwen_config.json}</li>
     *   <li>Classpath resource: {@code qwen_config.json} in classpath root</li>
     * </ol>
     * </p>
     *
     * @param mapper         Jackson ObjectMapper for JSON parsing
     * @param webAppRootPath Root path for external configuration file location
     * @return JsonNode representing the configuration, or null if not found
     * @see #EXTERNAL_CONFIG_DIR
     * @see #RESOURCE_CONFIG_PATH
     */
    private static JsonNode loadConfigJson(ObjectMapper mapper, String webAppRootPath) {
        if (webAppRootPath != null && !webAppRootPath.isEmpty()) {
            try {
                String externalConfigPath = webAppRootPath + File.separator + EXTERNAL_CONFIG_DIR + CONFIG_FILE;
                File externalConfigFile = new File(externalConfigPath);

                if (externalConfigFile.exists()) {
                    log.info("Loading configuration from external file: {}", externalConfigFile.getAbsolutePath());
                    return mapper.readTree(externalConfigFile);
                }
                
                log.debug("External config file not found: {}", externalConfigPath);
            } catch (Exception e) {
                log.warn("Failed to load external config file: {}", e.getMessage());
            }
        } else {
            log.debug("webAppRootPath is null or empty, skipping external config");
        }

        try {
            InputStream resourceStream = QwenConfiguration.class.getClassLoader()
                    .getResourceAsStream(RESOURCE_CONFIG_PATH);

            if (resourceStream != null) {
                log.info("Loading configuration from classpath resource: {}", RESOURCE_CONFIG_PATH);
                JsonNode node = mapper.readTree(resourceStream);
                resourceStream.close();
                return node;
            }
            
            log.debug("Classpath resource not found: {}", RESOURCE_CONFIG_PATH);
        } catch (Exception e) {
            log.warn("Failed to load classpath config resource: {}", e.getMessage());
        }

        log.error("Configuration file not found in external path or classpath: {}", CONFIG_FILE);
        return null;
    }

    /**
     * Parses model-specific configuration from JSON node.
     * <p>
     * If the model configuration is missing or invalid, this method falls back to
     * default values to ensure the application can still function.
     * </p>
     *
     * @param rootNode  Root JSON node containing all configuration
     * @param modelName Name of the model section to parse ("qwen", "long", or "vector")
     * @return ModelOption object with parsed or default values
     * @see #createDefaultModelOption(String)
     */
    private static ModelOption parseModelOption(JsonNode rootNode, String modelName) {
        JsonNode modelNode = rootNode.get(modelName);
        if (modelNode == null || modelNode.isNull()) {
            log.warn("Model configuration '{}' not found, using defaults", modelName);
            return createDefaultModelOption(modelName);
        }

        try {
            return ModelOption.builder()
                    .model(extractStringValue(modelNode, "model"))
                    .maxTokens(extractIntValue(modelNode, "maxTokens", 2048))
                    .temperature(extractFloatValue(modelNode, "temperature", 0.7f))
                    .topP(extractDoubleValue(modelNode, "topP", 0.8f))
                    .topK(extractIntValue(modelNode, "topK", 50))
                    .repetitionPenalty(extractFloatValue(modelNode, "repetitionPenalty", 1.0f))
                    .dimension(extractIntValue(modelNode, "dimension", 1024))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse model configuration for '{}', using defaults", modelName, e);
            return createDefaultModelOption(modelName);
        }
    }

    /**
     * Parses portrait matching weight configuration from JSON node.
     * <p>
     * If weight configuration is missing, defaults to equal distribution (0.33 each).
     * </p>
     *
     * @param rootNode Root JSON node containing all configuration
     * @return Weight object with parsed or default values
     * @see #createDefaultWeight()
     */
    private static Weight parseWeight(JsonNode rootNode) {
        JsonNode weightNode = rootNode.get("weight");
        if (weightNode == null || weightNode.isNull()) {
            log.warn("Weight configuration '{}' not found, using defaults", "weight");
            return createDefaultWeight();
        }
        return Weight.builder()
                .skills(extractFloatValue(weightNode, "skills", 0.33f))
                .experience(extractFloatValue(weightNode, "experience", 0.33f))
                .softSkills(extractFloatValue(weightNode, "softSkills", 0.33f))
                .build();
    }

    /**
     * Creates a default ModelOption with minimal configuration.
     *
     * @param modelName Model identifier to use
     * @return Default ModelOption instance
     */
    private static ModelOption createDefaultModelOption(String modelName) {
        return ModelOption.builder()
                .model(modelName)
                .build();
    }

    /**
     * Creates a default Weight with equal distribution across all dimensions.
     *
     * @return Default Weight instance (skills=0.33, experience=0.33, softSkills=0.33)
     */
    private static Weight createDefaultWeight() {
        return Weight.builder()
                .skills(0.33f)
                .experience(0.33f)
                .softSkills(0.33f)
                .build();
    }

    /**
     * Validates the API key format.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Must not be null or empty</li>
     *   <li>Should match pattern: {@code sk-[a-zA-Z0-9]{32,}} (starts with "sk-" followed by at least 32 alphanumeric characters)</li>
     * </ul>
     * </p>
     * <p>
     * Invalid format triggers a warning but does not prevent initialization,
     * allowing for custom API key formats or future format changes.
     * </p>
     *
     * @param apiKey API key string to validate
     * @throws IllegalArgumentException if API key is null or empty
     * @see #API_KEY_PATTERN
     * @see #maskValue(String)
     */
    private static void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            log.warn("API key format may be invalid: {}", maskValue(apiKey));
        }
    }

    /**
     * Validates the base URL format.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Must not be null or empty</li>
     *   <li>Must match HTTP/HTTPS URL pattern: {@code ^https?://[\w.-]+(/[\w./-]*)?$}</li>
     *   <li>Should use HTTPS for security (warning if HTTP is used)</li>
     * </ul>
     * </p>
     *
     * @param baseUrl Base URL string to validate
     * @throws IllegalArgumentException if URL is null, empty, or has invalid format
     * @see #URL_PATTERN
     */
    private static void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        if (!URL_PATTERN.matcher(baseUrl).matches()) {
            throw new IllegalArgumentException("Invalid base URL format: " + baseUrl);
        }
        if (!baseUrl.startsWith("https://")) {
            log.warn("Base URL should use HTTPS for security: {}", baseUrl);
        }
    }

    /**
     * Extracts a string value from a JSON node.
     *
     * @param node      Parent JSON node
     * @param fieldName Field name to extract
     * @return Extracted string value, or null if field is missing or null
     */
    private static String extractStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    /**
     * Extracts an integer value from a JSON node with fallback to default value.
     *
     * @param node         Parent JSON node
     * @param fieldName    Field name to extract
     * @param defaultValue Default value if field is null or missing
     * @return Extracted integer value or default value
     */
    private static int extractIntValue(JsonNode node, String fieldName, int defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asInt() : defaultValue;
    }

    /**
     * Extracts a float value from a JSON node with fallback to default value.
     *
     * @param node         Parent JSON node
     * @param fieldName    Field name to extract
     * @param defaultValue Default value if field is null or missing
     * @return Extracted float value or default value
     */
    private static float extractFloatValue(JsonNode node, String fieldName, float defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? (float) fieldNode.asDouble() : defaultValue;
    }

    /**
     * Extracts a double value from a JSON node with fallback to default value.
     *
     * @param node         Parent JSON node
     * @param fieldName    Field name to extract
     * @param defaultValue Default value if field is null or missing
     * @return Extracted double value or default value
     */
    private static double extractDoubleValue(JsonNode node, String fieldName, double defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asDouble() : defaultValue;
    }

    /**
     * Masks sensitive values (like API keys) for safe logging.
     * <p>
     * Shows only the first 4 and last 4 characters, replacing the middle with "...".
     * For short values (≤8 characters), returns "***" to avoid revealing any information.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>{@code "sk-abc123...xyz789"} → {@code "sk-a...z789"}</li>
     *   <li>{@code "short"} → {@code "***"}</li>
     *   <li>{@code null} → {@code "***"}</li>
     * </ul>
     * </p>
     *
     * @param value The sensitive value to mask
     * @return Masked string safe for logging
     */
    private static String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    /**
     * Immutable record holding all configuration data parsed from qwen_config.json.
     *
     * @param apiKey      API key for DashScope authentication
     * @param baseUrl     Base URL for Qwen API endpoint
     * @param qwen        Configuration for standard Qwen model
     * @param qwenLong    Configuration for Qwen-long model
     * @param qwenVector  Configuration for Qwen embedding model
     * @param weight      Portrait matching weight configuration
     */
    private record ConfigData(String apiKey, String baseUrl, ModelOption qwen, ModelOption qwenLong, ModelOption qwenVector, Weight weight) {
    }
}
