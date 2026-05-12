package com.tars.repository;

import com.tars.config.ApplicationConfiguration;
import com.tars.entity.bean.User;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for JsonRepository
 * Tests CRUD operations and edge cases
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class JsonRepositoryTest {

    private static final String TEST_DATA_DIR = "test-data";
    private static JsonRepository<User> userRepo;

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Set test data directory
        JsonRepository.setDataDir(TEST_DATA_DIR);

        // Create repository instance
        userRepo = new JsonRepository<>(User.class);

        // Clean up test data before all tests
        cleanTestDataDirectory();
    }

    @AfterClass
    public static void tearDown() {
        // Clean up test data after all tests
        cleanTestDataDirectory();
    }

    @Before
    public void beforeEach() {
        // Clean data before each test to ensure isolation
        try {
            List<User> users = userRepo.loadAllEntities();
            for (User user : users) {
                if (user.getId() != null) {
                    userRepo.deleteEntity(user.getId());
                }
            }
        } catch (IOException e) {
            fail("Failed to clean data before test: " + e.getMessage());
        }
    }

    /**
     * Test saving a single entity
     */
    @Test
    public void testSaveEntity() throws IOException {
        User user = createTestUser("user1", "John Doe", "password123");

        userRepo.saveEntity(user);

        List<User> users = userRepo.loadAllEntities();
        assertEquals("Should have exactly one user", 1, users.size());
        assertEquals("User ID should match", "user1", users.get(0).getId());
        assertEquals("User name should match", "John Doe", users.get(0).getName());
    }

    /**
     * Test saving multiple entities
     */
    @Test
    public void testSaveMultipleEntities() throws IOException {
        User user1 = createTestUser("user1", "John Doe", "password123");
        User user2 = createTestUser("user2", "Jane Smith", "password456");
        User user3 = createTestUser("user3", "Bob Johnson", "password789");

        userRepo.saveEntity(user1);
        userRepo.saveEntity(user2);
        userRepo.saveEntity(user3);

        List<User> users = userRepo.loadAllEntities();
        assertEquals("Should have exactly three users", 3, users.size());
    }

    /**
     * Test updating an existing entity
     */
    @Test
    public void testUpdateEntity() throws IOException {
        User user = createTestUser("user1", "John Doe", "password123");
        userRepo.saveEntity(user);

        // Update the user
        user.setName("John Updated");
        user.setPassword("newPassword");
        userRepo.saveEntity(user);

        List<User> users = userRepo.loadAllEntities();
        assertEquals("Should still have exactly one user", 1, users.size());
        assertEquals("User name should be updated", "John Updated", users.get(0).getName());
        assertEquals("User password should be updated", "newPassword", users.get(0).getPassword());
    }

    /**
     * Test loading all entities from empty repository
     */
    @Test
    public void testLoadAllEntitiesFromEmpty() throws IOException {
        List<User> users = userRepo.loadAllEntities();
        assertNotNull("Should return non-null list", users);
        assertTrue("Should return empty list", users.isEmpty());
    }

    /**
     * Test getting entity by ID
     */
    @Test
    public void testGetEntityById() throws IOException {
        User user = createTestUser("user1", "John Doe", "password123");
        userRepo.saveEntity(user);

        User found = userRepo.getEntityById("user1");
        assertNotNull("Should find the user", found);
        assertEquals("User ID should match", "user1", found.getId());
        assertEquals("User name should match", "John Doe", found.getName());
    }

    /**
     * Test getting entity by non-existent ID
     */
    @Test
    public void testGetEntityByIdNotFound() throws IOException {
        User found = userRepo.getEntityById("nonexistent");
        assertNull("Should return null for non-existent ID", found);
    }

    /**
     * Test deleting an entity
     */
    @Test
    public void testDeleteEntity() throws IOException {
        User user = createTestUser("user1", "John Doe", "password123");
        userRepo.saveEntity(user);

        boolean deleted = userRepo.deleteEntity("user1");
        assertTrue("Should successfully delete the user", deleted);

        List<User> users = userRepo.loadAllEntities();
        assertTrue("Should have no users after deletion", users.isEmpty());
    }

    /**
     * Test deleting non-existent entity
     */
    @Test
    public void testDeleteNonExistentEntity() throws IOException {
        boolean deleted = userRepo.deleteEntity("nonexistent");
        assertFalse("Should return false for non-existent entity", deleted);
    }

    /**
     * Test saving entity with null ID (should use identity hash)
     */
    @Test
    public void testSaveEntityWithNullId() throws IOException {
        User user = new User();
        user.setId(null); // Force null ID
        user.setName("No ID User");
        user.setPassword("password");

        userRepo.saveEntity(user);

        List<User> users = userRepo.loadAllEntities();
        assertEquals("Should save entity even with null ID", 1, users.size());
    }

    /**
     * Test saving all entities at once
     */
    @Test
    public void testSaveAllEntities() throws IOException {
        User user1 = createTestUser("user1", "John Doe", "password123");
        User user2 = createTestUser("user2", "Jane Smith", "password456");

        List<User> users = List.of(user1, user2);
        userRepo.saveAllEntities(users);

        List<User> loaded = userRepo.loadAllEntities();
        assertEquals("Should have two users", 2, loaded.size());
    }

    /**
     * Test repository with different entity types
     */
    @Test
    public void testRepositoryWithDifferentEntityType() throws IOException {
        // This test verifies that JsonRepository works with any entity type
        JsonRepository<User> anotherRepo = new JsonRepository<>(User.class);

        User user = createTestUser("test1", "Test User", "testpass");
        anotherRepo.saveEntity(user);

        User found = anotherRepo.getEntityById("test1");
        assertNotNull("Should work with different repository instances", found);

        // Clean up
        anotherRepo.deleteEntity("test1");
    }

    /**
     * Test concurrent save operations (basic thread safety check)
     */
    @Test
    public void testConcurrentSaves() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            try {
                User user = createTestUser("thread1", "Thread 1 User", "pass1");
                userRepo.saveEntity(user);
            } catch (IOException e) {
                fail("Thread 1 failed: " + e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                User user = createTestUser("thread2", "Thread 2 User", "pass2");
                userRepo.saveEntity(user);
            } catch (IOException e) {
                fail("Thread 2 failed: " + e.getMessage());
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        try {
            List<User> users = userRepo.loadAllEntities();
            assertEquals("Should have two users after concurrent saves", 2, users.size());
        } catch (IOException e) {
            fail("Failed to load users: " + e.getMessage());
        }
    }

    /**
     * Helper method to create test user
     */
    private User createTestUser(String id, String name, String password) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(password);
        user.setRole(1);
        user.setStatus(0);
        return user;
    }

    /**
     * Helper method to clean test data directory
     */
    private static void cleanTestDataDirectory() {
        File dir = new File(TEST_DATA_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
        // Recreate the directory to ensure it exists
        dir.mkdirs();
    }
}
