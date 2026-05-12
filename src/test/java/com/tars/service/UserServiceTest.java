package com.tars.service;

import com.tars.config.ApplicationConfiguration;
import com.tars.entity.bean.User;
import com.tars.entity.dto.user.UserDTO;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Test class for UserService
 * Tests authentication, registration, and user management
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class UserServiceTest {

    private static UserService userService;
    private static final String TEST_DATA_DIR = "test-data";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Set test data directory
        com.tars.repository.JsonRepository.setDataDir(TEST_DATA_DIR);

        // Create user service instance
        userService = new UserService();

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
        cleanTestDataDirectory();
    }

    /**
     * Test saving a new user
     */
    @Test
    public void testSaveUser() throws IOException {
        User user = createUser("user-save-1", "test_user", "password123", 1);

        boolean saved = userService.saveUser(user);

        assertTrue("User should be saved", saved);

        User found = findUserById(user.getId());
        assertNotNull("User should exist in repository", found);
        assertEquals("Username should match", "test_user", found.getName());
        assertEquals("Password should be encrypted", DigestUtils.md5Hex("password123"), found.getPassword());
        assertEquals("Role should match", 1, found.getRole());
    }

    /**
     * Test updating an existing user
     */
    @Test
    public void testUpdateUser() throws IOException {
        User user = createUser("user-update-1", "old_name", "password", 1);
        saveUserToRepo(user);

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setName("new_name");
        updatedUser.setRole(2);
        updatedUser.setStatus(1);

        boolean updated = userService.updateUser(updatedUser);

        assertTrue("User should be updated", updated);

        User found = findUserById(user.getId());
        assertNotNull("User should exist", found);
        assertEquals("Name should be updated", "new_name", found.getName());
        assertEquals("Role should be updated", 2, found.getRole());
        assertEquals("Status should be updated", 1, found.getStatus());
        assertNotNull("UpdateAt should be set", found.getUpdateAt());
    }

    /**
     * Test updating user with null ID
     */
    @Test
    public void testUpdateUserWithNullId() {
        User user = new User();
        user.setName("test_user");

        boolean updated = userService.updateUser(user);

        assertFalse("Should reject update with null ID", updated);
    }

    /**
     * Test updating non-existent user
     */
    @Test
    public void testUpdateNonExistentUser() {
        User user = new User();
        user.setId("non-existent-id");
        user.setName("test_user");

        boolean updated = userService.updateUser(user);

        assertFalse("Should reject update of non-existent user", updated);
    }

    /**
     * Test updating user with partial fields
     */
    @Test
    public void testUpdateUserWithPartialFields() throws IOException {
        User user = createUser("user-partial-1", "original_name", "password", 1);
        user.setStatus(0);
        saveUserToRepo(user);
        
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setPassword("new_password");
        // Note: role and status are primitive int, will be updated to default value 0
        // name is null, should not change
        
        boolean updated = userService.updateUser(updatedUser);
        
        assertTrue("User should be updated", updated);
        
        User found = findUserById(user.getId());
        assertNotNull("User should exist", found);
        assertEquals("Name should remain unchanged", "original_name", found.getName());
        assertEquals("Password should be updated", "new_password", found.getPassword());
        // role and status are primitive types, will be set to 0 (default) when not explicitly set
        assertEquals("Role will be reset to 0 (primitive int default)", 0, found.getRole());
        assertEquals("Status will be reset to 0 (primitive int default)", 0, found.getStatus());
    }

    /**
     * Test checking if user exists by username
     */
    @Test
    public void testCheckUserExist() throws IOException {
        User user = createUser("user-exist-1", "existing_user", "password", 1);
        saveUserToRepo(user);

        boolean exists = userService.checkUserExist("existing_user");

        assertTrue("User should exist", exists);

        boolean notExists = userService.checkUserExist("non_existing_user");

        assertFalse("User should not exist", notExists);
    }

    /**
     * Test login with correct credentials
     */
    @Test
    public void testLoginWithCorrectCredentials() throws IOException {
        String password = "login_password";
        User user = createUser("user-login-1", "login_user", password, 1);
        saveUserToRepo(user);

        UserDTO dto = userService.login("login_user", DigestUtils.md5Hex(password));

        assertNotNull("Login should succeed", dto);
        assertEquals("User ID should match", user.getId(), dto.getId());
        assertEquals("Username should match", "login_user", dto.getName());
        assertEquals("Role should match", 1, dto.getRole());

        // Verify lastLoginAt was updated
        User found = findUserById(user.getId());
        assertNotNull("Last login time should be updated", found.getLastLoginAt());
    }

    /**
     * Test login with incorrect password
     */
    @Test
    public void testLoginWithIncorrectPassword() throws IOException {
        User user = createUser("user-login-2", "login_user2", "correct_password", 1);
        saveUserToRepo(user);

        UserDTO dto = userService.login("login_user2", "wrong_password");

        assertNull("Login should fail with wrong password", dto);
    }

    /**
     * Test login with non-existent username
     */
    @Test
    public void testLoginWithNonExistentUser() {
        UserDTO dto = userService.login("non_existent_user", "password");

        assertNull("Login should fail for non-existent user", dto);
    }

    /**
     * Test getting user by ID
     */
    @Test
    public void testGetUserById() throws IOException {
        User user = createUser("user-get-1", "get_user", "password", 1);
        saveUserToRepo(user);

        User found = userService.getUserById(user.getId());

        assertNotNull("User should be found", found);
        assertEquals("User ID should match", user.getId(), found.getId());
        assertEquals("Username should match", "get_user", found.getName());
    }

    /**
     * Test getting non-existent user by ID
     */
    @Test
    public void testGetNonExistentUserById() {
        User found = userService.getUserById("non-existent-id");

        assertNull("Should return null for non-existent user", found);
    }

    /**
     * Test encrypting password
     */
    @Test
    public void testEncryptPassword() {
        String password = "test_password_123";
        String encrypted = userService.encryptPassword(password);

        assertNotNull("Encrypted password should not be null", encrypted);
        assertEquals("Should match MD5 hash", DigestUtils.md5Hex(password), encrypted);
        assertNotEquals("Should not be plain text", password, encrypted);
        assertEquals("MD5 hash length should be 32", 32, encrypted.length());
    }

    /**
     * Test encrypting empty password
     */
    @Test
    public void testEncryptEmptyPassword() {
        String password = "";
        String encrypted = userService.encryptPassword(password);

        assertNotNull("Encrypted password should not be null", encrypted);
        assertEquals("Should match MD5 hash of empty string", DigestUtils.md5Hex(""), encrypted);
    }

    /**
     * Test mergeUser with all fields
     */
    @Test
    public void testMergeUserWithAllFields() throws IOException {
        User existing = createUser("user-merge-1", "old_name", "old_password", 1);
        existing.setStatus(0);
        saveUserToRepo(existing);

        User source = new User();
        source.setId(existing.getId()); // Set the ID to identify which user to update
        source.setName("new_name");
        source.setPassword("new_password");
        source.setRole(2);
        source.setStatus(1);

        boolean updated = userService.updateUser(source);

        assertTrue("User should be merged and updated", updated);

        User found = findUserById(existing.getId());
        assertEquals("Name should be updated", "new_name", found.getName());
        assertEquals("Password should be updated", "new_password", found.getPassword());
        assertEquals("Role should be updated", 2, found.getRole());
        assertEquals("Status should be updated", 1, found.getStatus());
    }

    /**
     * Test mergeUser with null name (should preserve existing)
     */
    @Test
    public void testMergeUserWithNullName() throws IOException {
        User existing = createUser("user-merge-2", "keep_name", "password", 1);
        saveUserToRepo(existing);

        User source = new User();
        source.setId(existing.getId());
        source.setName(null); // Should not change name
        source.setPassword("new_password");

        boolean updated = userService.updateUser(source);

        assertTrue("User should be updated", updated);

        User found = findUserById(existing.getId());
        assertEquals("Name should remain unchanged", "keep_name", found.getName());
        assertEquals("Password should be updated", "new_password", found.getPassword());
    }

    /**
     * Test multiple users with different roles
     */
    @Test
    public void testMultipleUsersWithDifferentRoles() throws IOException {
        User admin = createUser("user-admin", "admin_user", "password", 0);
        User ta = createUser("user-ta", "ta_user", "password", 1);
        User mo = createUser("user-mo", "mo_user", "password", 2);

        saveUserToRepo(admin);
        saveUserToRepo(ta);
        saveUserToRepo(mo);

        User foundAdmin = userService.getUserById("user-admin");
        User foundTA = userService.getUserById("user-ta");
        User foundMO = userService.getUserById("user-mo");

        assertNotNull("Admin should exist", foundAdmin);
        assertEquals("Admin role should be 0", 0, foundAdmin.getRole());

        assertNotNull("TA should exist", foundTA);
        assertEquals("TA role should be 1", 1, foundTA.getRole());

        assertNotNull("MO should exist", foundMO);
        assertEquals("MO role should be 2", 2, foundMO.getRole());
    }

    /**
     * Test login updates lastLoginAt timestamp
     */
    @Test
    public void testLoginUpdatesLastLoginAt() throws IOException {
        User user = createUser("user-timestamp", "timestamp_user", "password", 1);
        saveUserToRepo(user);

        // Initial lastLoginAt should be null or old
        User beforeLogin = findUserById(user.getId());
        Timestamp beforeTimestamp = beforeLogin.getLastLoginAt();

        // Perform login
        userService.login("timestamp_user", DigestUtils.md5Hex("password"));

        // Verify lastLoginAt was updated
        User afterLogin = findUserById(user.getId());
        Timestamp afterTimestamp = afterLogin.getLastLoginAt();

        assertNotNull("LastLoginAt should be set after login", afterTimestamp);
        if (beforeTimestamp != null) {
            assertTrue("LastLoginAt should be updated", afterTimestamp.after(beforeTimestamp));
        }
    }

    // Helper methods

    private User createUser(String id, String name, String password, int role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(DigestUtils.md5Hex(password));
        user.setRole(role);
        user.setStatus(0);
        user.setCreateAt(Timestamp.valueOf(LocalDateTime.now()));
        return user;
    }

    private void saveUserToRepo(User user) throws IOException {
        com.tars.repository.JsonRepository<User> repo = new com.tars.repository.JsonRepository<>(User.class);
        repo.saveEntity(user);
    }

    private User findUserById(String id) throws IOException {
        com.tars.repository.JsonRepository<User> repo = new com.tars.repository.JsonRepository<>(User.class);
        return repo.getEntityById(id);
    }

    private static void cleanTestDataDirectory() {
        File dir = new File(TEST_DATA_DIR);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
        // Recreate the directory to ensure it exists
        dir.mkdirs();
    }

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
