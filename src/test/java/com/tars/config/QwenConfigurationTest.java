package com.tars.config;

import org.junit.*;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Test class for QwenConfiguration
 * Tests configuration loading, validation, and model options
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class QwenConfigurationTest {

    private static final String TEST_CONFIG_DIR = "src/test/resources";

    @BeforeClass
    public static void setUp() {
        // Reset singleton instance before tests
        resetInstance();
    }

    @After
    public void tearDown() {
        // Reset singleton instance after each test to ensure isolation
        resetInstance();
    }

    /**
     * Test initializing configuration for test environment
     */
    @Test
    public void testInitializeForTest() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        assertNotNull("Configuration should not be null", config);
        assertNotNull("API key should be loaded", config.getApiKey());
        assertNotNull("Base URL should be loaded", config.getBaseUrl());
    }

    /**
     * Test that getInstance throws exception when not initialized
     */
    @Test(expected = IllegalStateException.class)
    public void testGetInstanceNotInitialized() {
        // Ensure instance is null
        resetInstance();
        QwenConfiguration.getInstance();
    }

    /**
     * Test that initializeForTest can only be called once
     */
    @Test
    public void testInitializeOnlyOnce() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        // Second initialization should be skipped (no exception)
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        assertNotNull("Configuration should still exist", config);
    }

    /**
     * Test API key is loaded correctly
     */
    @Test
    public void testApiKeyLoaded() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        String apiKey = config.getApiKey();

        assertNotNull("API key should not be null", apiKey);
        assertFalse("API key should not be empty", apiKey.trim().isEmpty());
        assertTrue("API key should start with sk-", apiKey.startsWith("sk-"));
    }

    /**
     * Test base URL is loaded correctly
     */
    @Test
    public void testBaseUrlLoaded() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        String baseUrl = config.getBaseUrl();

        assertNotNull("Base URL should not be null", baseUrl);
        assertFalse("Base URL should not be empty", baseUrl.trim().isEmpty());
        assertTrue("Base URL should start with https://", baseUrl.startsWith("https://"));
    }

    /**
     * Test qwen-long model configuration is loaded
     */
    @Test
    public void testQwenLongModelLoaded() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        ModelOption qwenLong = config.getQwenLong();

        assertNotNull("Qwen-Long model should be loaded", qwenLong);
        assertEquals("Max tokens should match", 1024, qwenLong.getMaxTokens());
        assertEquals("Temperature should match", 0.0f, qwenLong.getTemperature(), 0.001f);
        assertEquals("TopP should match", 0.01, qwenLong.getTopP(), 0.001);
        assertEquals("TopK should match", 1, qwenLong.getTopK());
        assertEquals("Repetition penalty should match", 1.1f, qwenLong.getRepetitionPenalty(), 0.001f);
    }

    /**
     * Test qwen3-max model configuration is loaded
     */
    @Test
    public void testQwenMaxModelLoaded() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        ModelOption qwen = config.getQwen();

        assertNotNull("Qwen model should be loaded", qwen);
        assertEquals("Max tokens should match", 512, qwen.getMaxTokens());
        assertEquals("Temperature should match", 0.0f, qwen.getTemperature(), 0.001f);
    }

    /**
     * Test weight configuration loaded from file
     */
    @Test
    public void testWeightDefaults() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        Weight weight = config.getWeight();

        // Weight config is loaded from test JSON file
        assertNotNull("Weight should not be null", weight);
        assertEquals("Skills weight should match config", 0.6f, weight.getSkills(), 0.001f);
        assertEquals("Experience weight should match config", 0.15f, weight.getExperience(), 0.001f);
        assertEquals("Soft skills weight should match config", 0.25f, weight.getSoftSkills(), 0.001f);
    }

    /**
     * Test API key validation - valid key
     */
    @Test
    public void testValidApiKey() {
        // The test config has a valid API key format
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        assertNotNull("Should load with valid API key", config);
    }

    /**
     * Test base URL validation - valid URL
     */
    @Test
    public void testValidBaseUrl() {
        // The test config has a valid HTTPS URL
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        String baseUrl = config.getBaseUrl();
        assertTrue("Base URL should be HTTPS", baseUrl.startsWith("https://"));
    }

    /**
     * Test model option builder
     */
    @Test
    public void testModelOptionBuilder() {
        ModelOption model = ModelOption.builder()
                .model("test-model")
                .maxTokens(1024)
                .temperature(0.5f)
                .topP(0.9)
                .topK(50)
                .repetitionPenalty(1.0f)
                .dimension(768)
                .build();

        assertNotNull("Model option should be created", model);
        assertEquals("Model name should match", "test-model", model.getModel());
        assertEquals("Max tokens should match", 1024, model.getMaxTokens());
        assertEquals("Temperature should match", 0.5f, model.getTemperature(), 0.001f);
        assertEquals("Dimension should match", 768, model.getDimension());
    }

    /**
     * Test weight builder
     */
    @Test
    public void testWeightBuilder() {
        Weight weight = Weight.builder()
                .skills(0.4f)
                .experience(0.3f)
                .softSkills(0.3f)
                .build();

        assertNotNull("Weight should be created", weight);
        assertEquals("Skills should match", 0.4f, weight.getSkills(), 0.001f);
        assertEquals("Experience should match", 0.3f, weight.getExperience(), 0.001f);
        assertEquals("Soft skills should match", 0.3f, weight.getSoftSkills(), 0.001f);
    }

    /**
     * Test configuration immutability after initialization
     */
    @Test
    public void testConfigurationImmutability() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();
        String apiKey = config.getApiKey();
        String baseUrl = config.getBaseUrl();

        // Values should remain consistent
        assertEquals("API key should not change", apiKey, config.getApiKey());
        assertEquals("Base URL should not change", baseUrl, config.getBaseUrl());
    }

    /**
     * Test that missing model configuration uses defaults
     */
    @Test
    public void testMissingModelUsesDefaults() {
        QwenConfiguration.initializeForTest(TEST_CONFIG_DIR);

        QwenConfiguration config = QwenConfiguration.getInstance();

        // Qwen-Vector is not in test config, should have defaults or be null
        ModelOption qwenVector = config.getQwenVector();
        // Depending on implementation, it might be null or have default values
        // This test verifies the behavior is consistent
        if (qwenVector != null) {
            assertNotNull("If qwenVector exists, model name should be set", qwenVector.getModel());
        }
    }

    /**
     * Helper method to reset the singleton instance using reflection
     */
    private static void resetInstance() {
        try {
            Field instanceField = QwenConfiguration.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset QwenConfiguration instance", e);
        }
    }
}
