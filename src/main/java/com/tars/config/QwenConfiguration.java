package com.tars.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.regex.Pattern;

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

    private QwenConfiguration(String apiKey, String baseUrl, ModelOption qwen, ModelOption qwenLong, ModelOption qwenVector, Weight weight) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.qwen = qwen;
        this.qwenLong = qwenLong;
        this.qwenVector = qwenVector;
        this.weight = weight;
    }

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
     * Initialize configuration for testing purposes (without ServletContext)
     *
     * @param webAppRootPath Path to test resources directory
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

    private static JsonNode loadConfigJson(ObjectMapper mapper, String webAppRootPath) {
        try {
            String externalConfigPath = webAppRootPath + File.separator + EXTERNAL_CONFIG_DIR + CONFIG_FILE;
            File externalConfigFile = new File(externalConfigPath);

            if (externalConfigFile.exists()) {
                log.info("Loading configuration from external file: {}", externalConfigFile.getAbsolutePath());
                return mapper.readTree(externalConfigFile);
            }

            InputStream resourceStream = QwenConfiguration.class.getClassLoader()
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

    private static ModelOption createDefaultModelOption(String modelName) {
        return ModelOption.builder()
                .model(modelName)
                .build();
    }

    private static Weight createDefaultWeight() {
        return Weight.builder()
                .skills(0.33f)
                .experience(0.33f)
                .softSkills(0.33f)
                .build();
    }
    private static void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            log.warn("API key format may be invalid: {}", maskValue(apiKey));
        }
    }

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

    private static String extractStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    private static int extractIntValue(JsonNode node, String fieldName, int defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asInt() : defaultValue;
    }

    private static float extractFloatValue(JsonNode node, String fieldName, float defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? (float) fieldNode.asDouble() : defaultValue;
    }

    private static double extractDoubleValue(JsonNode node, String fieldName, double defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asDouble() : defaultValue;
    }

    private static String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private record ConfigData(String apiKey, String baseUrl, ModelOption qwen, ModelOption qwenLong, ModelOption qwenVector, Weight weight) {
    }
}
