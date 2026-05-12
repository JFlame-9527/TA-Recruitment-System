package com.tars.config;

import com.tars.entity.bean.User;
import com.tars.repository.JsonRepository;
import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for ApplicationConfiguration
 * Tests configuration loading, initialization, and environment settings
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class ApplicationConfigurationTest {

    private static final String TEST_CONFIG_DIR = "src/test/resources";
    private static final String TEST_DATA_DIR = "test-data-config";

    @BeforeClass
    public static void setUp() {
        // Reset singleton instance before tests
        resetInstance();
        
        // Clean up test data directory
        cleanTestDataDirectory();
    }

    @After
    public void tearDown() {
        // Reset singleton instance after each test to ensure isolation
        resetInstance();
        
        // Clean up test data after each test
        cleanTestDataDirectory();
    }

    /**
     * Test initializing configuration for test environment
     */
    @Test
    public void testInitializeForTest() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        assertNotNull("Configuration should not be null", config);
        assertEquals("Environment should be test", "test", config.getEnvironment());
        assertTrue("CleanData should be true in test config", config.isCleanData());
        assertFalse("GenerateData should be false in test config", config.isGenerateData());
        assertEquals("DataDir should be data", "data", config.getDataDir());
        assertEquals("FileDir should be upload", "upload", config.getFileDir());
    }

    /**
     * Test that getInstance throws exception when not initialized
     */
    @Test(expected = IllegalStateException.class)
    public void testGetInstanceNotInitialized() {
        // Ensure instance is null
        resetInstance();
        ApplicationConfiguration.getInstance();
    }

    /**
     * Test that initializeForTest can only be called once
     */
    @Test
    public void testInitializeOnlyOnce() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        // Second initialization should be skipped (no exception)
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        assertNotNull("Configuration should still exist", config);
    }

    /**
     * Test getDataPath returns correct path
     */
    @Test
    public void testGetDataPath() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        String dataPath = config.getDataPath();

        assertNotNull("Data path should not be null", dataPath);
        assertTrue("Data path should contain 'data' directory", dataPath.contains("data"));
    }

    /**
     * Test getFilePath returns correct path
     */
    @Test
    public void testGetFilePath() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        String filePath = config.getFilePath();

        assertNotNull("File path should not be null", filePath);
        assertTrue("File path should contain 'upload' directory", filePath.contains("upload"));
    }

    /**
     * Test configuration loads from classpath resource
     */
    @Test
    public void testLoadConfigFromClasspath() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        // Verify config was loaded successfully
        assertNotNull("Configuration should be loaded", config);
        assertNotNull("Environment should be set", config.getEnvironment());
    }

    /**
     * Test environment validation - valid environments
     */
    @Test
    public void testValidEnvironments() {
        // Test environment should work
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        assertEquals("Should load test environment", "test",
                ApplicationConfiguration.getInstance().getEnvironment());
    }

    /**
     * Test that directories are created during initialization
     */
    @Test
    public void testDirectoriesCreated() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        File dataDir = new File(config.getDataPath());
        File fileDir = new File(config.getFilePath());

        assertTrue("Data directory should exist", dataDir.exists());
        assertTrue("Data directory should be a directory", dataDir.isDirectory());
        assertTrue("File directory should exist", fileDir.exists());
        assertTrue("File directory should be a directory", fileDir.isDirectory());
    }

    /**
     * Test configuration with different paths
     */
    @Test
    public void testCustomPaths() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        // Paths should match config.json settings
        assertEquals("Data dir should match config", "data", config.getDataDir());
        assertEquals("File dir should match config", "upload", config.getFileDir());
    }

    /**
     * Test that webRootPath is set correctly
     */
    @Test
    public void testWebRootPathSet() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        // Web root path should be set to test resources directory
        String dataPath = config.getDataPath();
        assertNotNull("Data path should not be null", dataPath);
        
        // Data path should contain the test resources directory
        File expectedDir = new File(TEST_CONFIG_DIR);
        assertTrue("Data path should contain test resources directory",
                dataPath.contains(expectedDir.getPath().replace("\\", "/")) || 
                dataPath.contains(expectedDir.getAbsolutePath()));
        
        // Data path should end with data directory
        assertTrue("Data path should end with data directory",
                dataPath.endsWith("data") || dataPath.endsWith("data" + File.separator));
    }

    /**
     * Test configuration immutability after initialization
     */
    @Test
    public void testConfigurationImmutability() {
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        String environment = config.getEnvironment();
        String dataDir = config.getDataDir();

        // Values should remain consistent
        assertEquals("Environment should not change", environment, config.getEnvironment());
        assertEquals("Data dir should not change", dataDir, config.getDataDir());
    }

    /**
     * Test first-time deployment creates admin account automatically
     */
    @Test
    public void testFirstTimeDeploymentCreatesAdmin() throws Exception {
        // Use a custom test data directory that doesn't exist yet
        String testDataPath = new File(TEST_DATA_DIR).getAbsolutePath();
        
        // Ensure directory doesn't exist (simulating first deployment)
        File dataDir = new File(testDataPath);
        if (dataDir.exists()) {
            deleteRecursively(dataDir);
        }
        
        // Initialize with cleanData=false (normal mode)
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        
        // Verify admin account was created
        JsonRepository<User> userRepo = new JsonRepository<>(User.class);
        List<User> users = userRepo.loadAllEntities();
        
        assertNotNull("Users list should not be null", users);
        assertFalse("Should have at least admin user", users.isEmpty());
        
        boolean hasAdmin = users.stream()
                .anyMatch(user -> user != null && user.getRole() == 0 && "admin".equals(user.getName()));
        
        assertTrue("Admin account should be created on first deployment", hasAdmin);
    }

    /**
     * Test normal startup with existing admin account passes integrity check
     */
    @Test
    public void testNormalStartupWithExistingAdmin() throws Exception {
        // First initialization creates admin
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        
        // Verify admin exists
        JsonRepository<User> userRepo = new JsonRepository<>(User.class);
        List<User> users = userRepo.loadAllEntities();
        boolean hasAdmin = users.stream()
                .anyMatch(user -> user != null && user.getRole() == 0 && "admin".equals(user.getName()));
        assertTrue("Admin should exist after first init", hasAdmin);
        
        // Second initialization (simulating restart) should pass integrity check
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        
        // Should succeed without exception
        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        assertNotNull("Configuration should be loaded on restart", config);
    }

    /**
     * Test cleanData mode recreates admin account
     */
    @Test
    public void testCleanDataModeRecreatesAdmin() throws Exception {
        // First create some data
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        
        // Add a regular user
        User user = new User();
        user.setName("test_user");
        user.setRole(1);
        user.setStatus(0);
        JsonRepository<User> userRepo = new JsonRepository<>(User.class);
        userRepo.saveEntity(user);
        
        // Verify user exists
        List<User> users = userRepo.loadAllEntities();
        int userCount = users.size();
        assertTrue("Should have users", userCount > 0);
        
        // Reinitialize with cleanData=true (current test config)
        // This will clean all data and recreate admin
        resetInstance();
        ApplicationConfiguration.initializeForTest(TEST_CONFIG_DIR);
        
        // Verify only admin exists now (other users were cleaned)
        JsonRepository<User> newUserRepo = new JsonRepository<>(User.class);
        List<User> cleanedUsers = newUserRepo.loadAllEntities();
        
        assertNotNull("Users list should not be null", cleanedUsers);
        assertEquals("Should have only admin after clean", 1, cleanedUsers.size());
        
        boolean hasOnlyAdmin = cleanedUsers.stream()
                .allMatch(u -> u != null && u.getRole() == 0 && "admin".equals(u.getName()));
        assertTrue("Should only have admin account after cleanData", hasOnlyAdmin);
    }

    // Helper methods

    /**
     * Helper method to reset the singleton instance using reflection
     */
    private static void resetInstance() {
        try {
            Field instanceField = ApplicationConfiguration.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset ApplicationConfiguration instance", e);
        }
    }

    /**
     * Helper method to clean test data directory
     */
    private static void cleanTestDataDirectory() {
        File dir = new File(TEST_DATA_DIR);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
        dir.mkdirs();
    }

    /**
     * Helper method to delete directory recursively
     */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
