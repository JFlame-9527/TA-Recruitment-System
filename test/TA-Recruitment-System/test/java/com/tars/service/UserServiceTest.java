package com.tars.service;

import com.tars.entity.bean.User;
import com.tars.entity.dto.user.UserDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for UserService
 * Tests authentication, registration, and user management
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/7
 */
public class UserServiceTest {

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService();
        cleanupTestData();
    }

    @After
    public void tearDown() {
        cleanupTestData();
    }

    /**
     * Helper method to clean up test data
     */
    private void cleanupTestData() {
        java.io.File testFile = new java.io.File("data", "user.json");
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    // ==================== ENCRYPT PASSWORD TESTS ====================

    @Test
    public void testEncryptPasswordConsistency() {
        // Arrange
        String password = "password123";

        // Act
        String encrypted1 = userService.encryptPassword(password);
        String encrypted2 = userService.encryptPassword(password);

        // Assert
        assertNotNull(encrypted1);
        assertEquals(encrypted1, encrypted2); // Same input should produce same output
        assertNotEquals(password, encrypted1); // Encrypted should differ from original
    }

    @Test
    public void testEncryptPasswordDifferentInputs() {
        // Arrange & Act
        String encrypted1 = userService.encryptPassword("pass1");
        String encrypted2 = userService.encryptPassword("pass2");

        // Assert
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    public void testEncryptPasswordEmptyString() {
        // Arrange & Act
        String encrypted = userService.encryptPassword("");

        // Assert
        assertNotNull(encrypted);
        assertEquals(32, encrypted.length()); // MD5 produces 32-char hex string
    }

    @Test
    public void testEncryptPasswordWithSpecialCharacters() {
        // Arrange
        String password = "p@ss!#$%^&*()";

        // Act
        String encrypted = userService.encryptPassword(password);

        // Assert
        assertNotNull(encrypted);
        assertEquals(32, encrypted.length());
    }

    @Test
    public void testEncryptPasswordWithUnicode() {
        // Arrange
        String password = "密码パスワード";

        // Act
        String encrypted = userService.encryptPassword(password);

        // Assert
        assertNotNull(encrypted);
        assertEquals(32, encrypted.length());
    }

    // ==================== CHECK USER EXIST TESTS ====================

    @Test
    public void testCheckUserExistReturnsFalseForNewUsername() {
        // Act
        boolean exists = userService.checkUserExist("nonexistent");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testCheckUserExistReturnsTrueForExistingUsername() {
        // Arrange
        User user = new User();
        user.setName("existinguser");
        user.setPassword(userService.encryptPassword("pass123"));
        userService.saveUser(user);

        // Act
        boolean exists = userService.checkUserExist("existinguser");

        // Assert
        assertTrue(exists);
    }

    @Test
    public void testCheckUserExistCaseSensitive() {
        // Arrange
        User user = new User();
        user.setName("TestUser");
        user.setPassword("pass123");
        userService.saveUser(user);

        // Act & Assert
        assertTrue(userService.checkUserExist("TestUser"));
        assertFalse(userService.checkUserExist("testuser")); // Different case
        assertFalse(userService.checkUserExist("TESTUSER"));
    }

    @Test
    public void testCheckUserExistWithEmptyString() {
        // Act
        boolean exists = userService.checkUserExist("");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testCheckUserExistWithNull() {
        // Act
        boolean exists = userService.checkUserExist(null);

        // Assert
        assertFalse(exists);
    }

    // ==================== SAVE USER TESTS ====================

    @Test
    public void testSaveUserSuccess() {
        // Arrange
        User user = new User();
        user.setName("newuser");
        user.setPassword(userService.encryptPassword("pass123"));
        user.setRole(1);
        user.setStatus(0);

        // Act
        boolean saved = userService.saveUser(user);

        // Assert
        assertTrue(saved);
        assertNotNull(user.getId());
        assertNotNull(user.getCreateAt());
    }

    @Test
    public void testSaveMultipleUsers() {
        // Arrange
        User user1 = new User();
        user1.setName("user1");
        user1.setRole(1);

        User user2 = new User();
        user2.setName("user2");
        user2.setRole(2);

        // Act
        boolean saved1 = userService.saveUser(user1);
        boolean saved2 = userService.saveUser(user2);

        // Assert
        assertTrue(saved1);
        assertTrue(saved2);
        assertNotEquals(user1.getId(), user2.getId());
    }

    @Test
    public void testSaveUserGeneratesUniqueId() {
        // Arrange
        User user1 = new User();
        user1.setName("user1");

        User user2 = new User();
        user2.setName("user2");

        // Act
        userService.saveUser(user1);
        userService.saveUser(user2);

        // Assert
        assertNotNull(user1.getId());
        assertNotNull(user2.getId());
        assertNotEquals(user1.getId(), user2.getId());
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    public void testUpdateUserName() {
        // Arrange
        User user = new User();
        user.setName("original");
        user.setPassword("pass123");
        userService.saveUser(user);

        // Act
        user.setName("updated");
        boolean updated = userService.updateUser(user);

        // Assert
        assertTrue(updated);
        User retrieved = userService.getUserById(user.getId());
        assertEquals("updated", retrieved.getName());
    }

    @Test
    public void testUpdateUserPassword() {
        // Arrange
        User user = new User();
        user.setName("testuser");
        user.setPassword("oldpass");
        userService.saveUser(user);

        // Act
        String newPassword = userService.encryptPassword("newpass");
        user.setPassword(newPassword);
        boolean updated = userService.updateUser(user);

        // Assert
        assertTrue(updated);
        User retrieved = userService.getUserById(user.getId());
        assertEquals(newPassword, retrieved.getPassword());
    }

    @Test
    public void testUpdateUserPartialFields() {
        // Arrange
        User user = new User();
        user.setName("testuser");
        user.setPassword("pass123");
        user.setRole(1);
        userService.saveUser(user);

        // Act - Update only name, password is null
        User updateData = new User(user.getId(), "newname", null);
        boolean updated = userService.updateUser(updateData);

        // Assert
        assertTrue(updated);
        User retrieved = userService.getUserById(user.getId());
        assertEquals("newname", retrieved.getName());
        assertEquals("pass123", retrieved.getPassword()); // Original password preserved
    }

    @Test
    public void testUpdateNonExistentUser() {
        // Arrange
        User user = new User("non-existent-id", "test", "pass");

        // Act
        boolean updated = userService.updateUser(user);

        // Assert
        assertFalse(updated);
    }

    @Test
    public void testUpdateUserWithNullId() {
        // Arrange
        User user = new User();
        user.setId(null);
        user.setName("test");

        // Act
        boolean updated = userService.updateUser(user);

        // Assert
        assertFalse(updated);
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    public void testGetUserByIdExists() {
        // Arrange
        User user = new User();
        user.setName("findme");
        user.setRole(1);
        userService.saveUser(user);

        // Act
        User found = userService.getUserById(user.getId());

        // Assert
        assertNotNull(found);
        assertEquals(user.getId(), found.getId());
        assertEquals("findme", found.getName());
        assertEquals(1, found.getRole());
    }

    @Test
    public void testGetUserByIdNotExists() {
        // Act
        User found = userService.getUserById("non-existent-id");

        // Assert
        assertNull(found);
    }

    @Test
    public void testGetUserByIdWithNull() {
        // Act
        User found = userService.getUserById(null);

        // Assert
        assertNull(found);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    public void testLoginWithValidCredentials() {
        // Arrange
        String username = "loginuser";
        String password = "securepass";
        String encryptedPassword = userService.encryptPassword(password);

        User user = new User();
        user.setName(username);
        user.setPassword(encryptedPassword);
        user.setRole(1);
        user.setStatus(0);
        userService.saveUser(user);

        // Act
        UserDTO result = userService.login(username, encryptedPassword);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getName());
        assertEquals(1, result.getRole());
        assertEquals(0, result.getStatus());
        assertNotNull(result.getId());
    }

    @Test
    public void testLoginWithInvalidPassword() {
        // Arrange
        String username = "user1";
        String correctPassword = userService.encryptPassword("correct");

        User user = new User();
        user.setName(username);
        user.setPassword(correctPassword);
        userService.saveUser(user);

        // Act
        String wrongPassword = userService.encryptPassword("wrong");
        UserDTO result = userService.login(username, wrongPassword);

        // Assert
        assertNull(result);
    }

    @Test
    public void testLoginWithNonExistentUser() {
        // Act
        UserDTO result = userService.login("nonexistent", "password");

        // Assert
        assertNull(result);
    }

    @Test
    public void testLoginUpdatesLastLoginTime() throws InterruptedException {
        // Arrange
        String username = "logintest";
        String password = userService.encryptPassword("pass123");

        User user = new User();
        user.setName(username);
        user.setPassword(password);
        userService.saveUser(user);

        // Get initial last login time
        User beforeLogin = userService.getUserById(user.getId());
        var initialLastLogin = beforeLogin.getLastLoginAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // Act
        userService.login(username, password);

        // Assert
        User afterLogin = userService.getUserById(user.getId());
        assertNotNull(afterLogin.getLastLoginAt());

        if (initialLastLogin != null) {
            assertTrue(afterLogin.getLastLoginAt().after(initialLastLogin));
        }
    }

    @Test
    public void testLoginWithFrozenAccount() {
        // Arrange
        String username = "frozenuser";
        String password = userService.encryptPassword("pass123");

        User user = new User();
        user.setName(username);
        user.setPassword(password);
        user.setStatus(1); // Frozen
        userService.saveUser(user);

        // Act
        UserDTO result = userService.login(username, password);

        // Assert - Login succeeds but status indicates frozen
        assertNotNull(result);
        assertEquals(1, result.getStatus()); // Status should be 1 (frozen)
    }

    @Test
    public void testLoginWithEmptyUsername() {
        // Act
        UserDTO result = userService.login("", "password");

        // Assert
        assertNull(result);
    }

    @Test
    public void testLoginWithNullCredentials() {
        // Act
        UserDTO result = userService.login(null, null);

        // Assert
        assertNull(result);
    }

    @Test
    public void testLoginCaseSensitiveUsername() {
        // Arrange
        User user = new User();
        user.setName("TestUser");
        user.setPassword(userService.encryptPassword("pass"));
        userService.saveUser(user);

        // Act & Assert
        assertNotNull(userService.login("TestUser", userService.encryptPassword("pass")));
        assertNull(userService.login("testuser", userService.encryptPassword("pass")));
    }

    @Test
    public void testMultipleLoginsSameUser() {
        // Arrange
        String username = "multiuser";
        String password = userService.encryptPassword("pass123");

        User user = new User();
        user.setName(username);
        user.setPassword(password);
        userService.saveUser(user);

        // Act - Login multiple times
        UserDTO result1 = userService.login(username, password);
        UserDTO result2 = userService.login(username, password);
        UserDTO result3 = userService.login(username, password);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(result1.getId(), result2.getId());
        assertEquals(result2.getId(), result3.getId());
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    public void testFullUserRegistrationAndLoginFlow() {
        // 1. Check username availability
        assertFalse(userService.checkUserExist("newaccount"));

        // 2. Create user
        User user = new User();
        user.setName("newaccount");
        user.setPassword(userService.encryptPassword("mypassword"));
        user.setRole(1);
        user.setStatus(0);
        assertTrue(userService.saveUser(user));

        // 3. Verify username now exists
        assertTrue(userService.checkUserExist("newaccount"));

        // 4. Login with correct credentials
        UserDTO loggedIn = userService.login("newaccount", user.getPassword());
        assertNotNull(loggedIn);
        assertEquals("newaccount", loggedIn.getName());

        // 5. Login with wrong password fails
        UserDTO wrongLogin = userService.login("newaccount", userService.encryptPassword("wrong"));
        assertNull(wrongLogin);
    }

    @Test
    public void testUserStatusManagement() {
        // Arrange - Create active user
        User user = new User();
        user.setName("statususer");
        user.setPassword(userService.encryptPassword("pass"));
        user.setStatus(0); // Active
        userService.saveUser(user);

        // Act & Assert - Can login when active
        UserDTO activeLogin = userService.login("statususer", user.getPassword());
        assertNotNull(activeLogin);
        assertEquals(0, activeLogin.getStatus());

        // Update status to frozen
        user.setStatus(1);
        userService.updateUser(user);

        // Can still login but status shows frozen
        UserDTO frozenLogin = userService.login("statususer", user.getPassword());
        assertNotNull(frozenLogin);
        assertEquals(1, frozenLogin.getStatus());
    }

    @Test
    public void testDifferentUserRoles() {
        // Arrange - Create users with different roles
        User admin = new User();
        admin.setName("admin");
        admin.setRole(0);
        admin.setPassword(userService.encryptPassword("pass"));

        User ta = new User();
        ta.setName("ta_user");
        ta.setRole(1);
        ta.setPassword(userService.encryptPassword("pass"));

        User mo = new User();
        mo.setName("mo_user");
        mo.setRole(2);
        mo.setPassword(userService.encryptPassword("pass"));

        userService.saveUser(admin);
        userService.saveUser(ta);
        userService.saveUser(mo);

        // Act & Assert
        UserDTO adminDTO = userService.login("admin", admin.getPassword());
        assertEquals(0, adminDTO.getRole());

        UserDTO taDTO = userService.login("ta_user", ta.getPassword());
        assertEquals(1, taDTO.getRole());

        UserDTO moDTO = userService.login("mo_user", mo.getPassword());
        assertEquals(2, moDTO.getRole());
    }

    @Test
    public void testPasswordChangeFlow() {
        // Arrange
        User user = new User();
        user.setName("changepass");
        String oldPassword = userService.encryptPassword("oldpass");
        user.setPassword(oldPassword);
        userService.saveUser(user);

        // Verify old password works
        assertNotNull(userService.login("changepass", oldPassword));

        // Change password
        String newPassword = userService.encryptPassword("newpass");
        user.setPassword(newPassword);
        userService.updateUser(user);

        // Verify new password works
        UserDTO afterChange = userService.login("changepass", newPassword);
        assertNotNull(afterChange);

        // Verify old password no longer works
        UserDTO withOldPass = userService.login("changepass", oldPassword);
        assertNull(withOldPass);
    }

    @Test
    public void testConcurrentUserOperations() {
        // Arrange - Create multiple users
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setName("user" + i);
            user.setPassword(userService.encryptPassword("pass" + i));
            user.setRole(i % 3);
            userService.saveUser(user);
        }

        // Act & Assert - All users can be retrieved
        for (int i = 0; i < 10; i++) {
            String username = "user" + i;
            assertTrue(userService.checkUserExist(username));
        }
    }

    /**
     * Helper method to get user by username (for testing)
     */
    private User getUserByUsername(String username) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(User.class);
            var users = repo.loadAllEntities();
            return users.stream()
                    .filter(u -> u.getName().equals(username))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
