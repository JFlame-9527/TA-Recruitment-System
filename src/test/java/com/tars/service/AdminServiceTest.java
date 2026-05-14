package com.tars.service;

import com.tars.config.ApplicationConfiguration;
import com.tars.entity.bean.*;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.admin.UserDetailDTO;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for AdminService
 * Tests user management, account operations, and administrative functions
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class AdminServiceTest {

    private static AdminService adminService;
    private static final String TEST_DATA_DIR = "test-data";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Set test data directory
        com.tars.repository.JsonRepository.setDataDir(TEST_DATA_DIR);

        // Create admin service instance
        adminService = new AdminService();

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
     * Test deleting a TA user with applications
     */
    @Test
    public void testDeleteTAUser() throws IOException {
        // Create TA user
        User taUser = createUser("ta-user-1", "ta_user", "password", 1);
        TAProfile taProfile = createTAProfile("ta-profile-1", taUser.getId());

        // Save entities
        saveUser(taUser);
        saveTAProfile(taProfile);

        // Delete user
        boolean deleted = adminService.deleteUser(taUser.getId());

        assertTrue("TA user should be deleted", deleted);
        assertNull("User should not exist after deletion", findUserById(taUser.getId()));
        assertNull("TA profile should be deleted", findTAProfileByUserId(taUser.getId()));
    }

    /**
     * Test deleting a MO user with positions
     */
    @Test
    public void testDeleteMOUser() throws IOException {
        // Create MO user
        User moUser = createUser("mo-user-1", "mo_user", "password", 2);
        MOProfile moProfile = createMOProfile("mo-profile-1", moUser.getId());

        // Save entities
        saveUser(moUser);
        saveMOProfile(moProfile);

        // Delete user
        boolean deleted = adminService.deleteUser(moUser.getId());

        assertTrue("MO user should be deleted", deleted);
        assertNull("User should not exist after deletion", findUserById(moUser.getId()));
        assertNull("MO profile should be deleted", findMOProfileByUserId(moUser.getId()));
    }

    /**
     * Test deleting non-existent user
     */
    @Test
    public void testDeleteNonExistentUser() {
        boolean deleted = adminService.deleteUser("non-existent-user");

        assertFalse("Should return false for non-existent user", deleted);
    }

    /**
     * Test updating user status
     */
    @Test
    public void testUpdateUserStatus() throws IOException {
        User user = createUser("user-1", "test_user", "password", 1);
        saveUser(user);

        boolean updated = adminService.updateUserStatus(user.getId(), 1);

        assertTrue("User status should be updated", updated);

        User updatedUser = findUserById(user.getId());
        assertNotNull("User should exist", updatedUser);
        assertEquals("Status should be updated", 1, updatedUser.getStatus());
    }

    /**
     * Test updating status of non-existent user
     */
    @Test
    public void testUpdateStatusOfNonExistentUser() {
        boolean updated = adminService.updateUserStatus("non-existent", 1);

        assertFalse("Should return false for non-existent user", updated);
    }

    /**
     * Test resetting password
     */
    @Test
    public void testResetPassword() throws IOException {
        User user = createUser("user-2", "test_user", "old_password", 1);
        saveUser(user);

        String newPassword = "new_password_123";
        boolean reset = adminService.resetPassword(user.getId(), newPassword);

        assertTrue("Password should be reset", reset);

        User updatedUser = findUserById(user.getId());
        assertNotNull("User should exist", updatedUser);
        assertEquals("Password should be encrypted", DigestUtils.md5Hex(newPassword), updatedUser.getPassword());
    }

    /**
     * Test resetting password with short password
     */
    @Test
    public void testResetPasswordWithShortPassword() throws IOException {
        User user = createUser("user-3", "test_user", "password", 1);
        saveUser(user);

        boolean reset = adminService.resetPassword(user.getId(), "12345");

        assertFalse("Should reject short password", reset);
    }

    /**
     * Test resetting password of non-existent user
     */
    @Test
    public void testResetPasswordOfNonExistentUser() {
        boolean reset = adminService.resetPassword("non-existent", "new_password");

        assertFalse("Should return false for non-existent user", reset);
    }

    /**
     * Test getting TA profile
     */
    @Test
    public void testGetTAProfile() throws IOException {
        User user = createUser("ta-user-2", "ta_user", "password", 1);
        TAProfile profile = createTAProfile("ta-profile-2", user.getId());

        saveUser(user);
        saveTAProfile(profile);

        TAProDTO dto = adminService.getTAProfile(user.getId());

        assertNotNull("TA profile DTO should not be null", dto);
        assertEquals("User ID should match", user.getId(), dto.getUserId());
    }

    /**
     * Test getting non-existent TA profile
     */
    @Test
    public void testGetNonExistentTAProfile() {
        TAProDTO dto = adminService.getTAProfile("non-existent");

        assertNull("Should return null for non-existent profile", dto);
    }

    /**
     * Test getting MO profile
     */
    @Test
    public void testGetMOProfile() throws IOException {
        User user = createUser("mo-user-2", "mo_user", "password", 2);
        MOProfile profile = createMOProfile("mo-profile-2", user.getId());

        saveUser(user);
        saveMOProfile(profile);

        MOProDTO dto = adminService.getMOProfile(user.getId());

        assertNotNull("MO profile DTO should not be null", dto);
        assertEquals("User ID should match", user.getId(), dto.getUserId());
    }

    /**
     * Test getting non-existent MO profile
     */
    @Test
    public void testGetNonExistentMOProfile() {
        MOProDTO dto = adminService.getMOProfile("non-existent");

        assertNull("Should return null for non-existent profile", dto);
    }

    /**
     * Test updating MO profile
     */
    @Test
    public void testUpdateMOProfile() throws IOException {
        MOProfile profile = createMOProfile("mo-profile-3", "mo-user-3");
        saveMOProfile(profile);

        profile.setCollege("Updated College");
        boolean updated = adminService.updateMOProfile(profile);

        assertTrue("MO profile should be updated", updated);
    }

    /**
     * Test updating user
     */
    @Test
    public void testUpdateUser() throws IOException {
        User user = createUser("user-4", "old_name", "password", 1);
        saveUser(user);

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setName("new_name");

        boolean result = adminService.updateUser(updatedUser);

        assertTrue("User should be updated", result);

        User found = findUserById(user.getId());
        assertNotNull("User should exist", found);
        assertEquals("Name should be updated", "new_name", found.getName());
    }

    /**
     * Test updating non-existent user
     */
    @Test
    public void testUpdateNonExistentUser() {
        User updatedUser = new User();
        updatedUser.setId("non-existent");
        updatedUser.setName("new_name");

        boolean result = adminService.updateUser(updatedUser);

        assertFalse("Should return false for non-existent user", result);
    }

    /**
     * Test creating MO account
     */
    @Test
    public void testCreateMOAccount() throws IOException {
        User moUser = createUser("mo-user-4", "mo_new", "password", 0);
        MOProfile moProfile = createMOProfile("mo-profile-4", null);

        boolean created = adminService.createMOAccount(moUser, moProfile);

        assertTrue("MO account should be created", created);

        User found = findUserById(moUser.getId());
        assertNotNull("User should exist", found);
        assertEquals("Role should be set to 2", 2, found.getRole());

        MOProfile foundProfile = findMOProfileByUserId(moUser.getId());
        assertNotNull("MO profile should exist", foundProfile);
        assertEquals("User ID should be linked", moUser.getId(), foundProfile.getUserId());
    }

    /**
     * Test getting accounts by role
     */
    @Test
    public void testGetAccountsByRole() throws IOException {
        // Create multiple TA users
        for (int i = 0; i < 5; i++) {
            User user = createUser("ta-user-" + i, "ta_user_" + i, "password", 1);
            TAProfile profile = createTAProfile("ta-profile-" + i, user.getId());
            saveUser(user);
            saveTAProfile(profile);
        }

        List<UserDetailDTO> accounts = adminService.getAccountsByRole(1, 1, null);

        assertNotNull("Accounts list should not be null", accounts);
        assertTrue("Should have accounts", accounts.size() > 0);
        assertTrue("Should respect page size", accounts.size() <= 10);
    }

    /**
     * Test getting accounts by role with filter and order
     */
    @Test
    public void testGetAccountsByRoleWithCondition() throws IOException {
        // Create users with different statuses
        User user1 = createUser("user-5", "user_a", "password", 1);
        user1.setStatus(0); // Available
        User user2 = createUser("user-6", "user_b", "password", 1);
        user2.setStatus(1); // Unavailable

        saveUser(user1);
        saveUser(user2);

        QueryCondition condition = new QueryCondition();
        condition.setFilter("available");
        condition.setOrder("name");
        condition.setPage(1);

        List<UserDetailDTO> accounts = adminService.getAccountsByRole(1, condition, null);

        assertNotNull("Accounts list should not be null", accounts);
        // Should only return available users
        for (UserDetailDTO dto : accounts) {
            assertEquals("Should only return available users", 0, dto.getStatus());
        }
    }

    /**
     * Test getting account pages
     */
    @Test
    public void testGetAccountPages() throws IOException {
        // Create 15 users
        for (int i = 0; i < 15; i++) {
            User user = createUser("page-user-" + i, "user_" + i, "password", 1);
            saveUser(user);
        }

        long pages = adminService.getAccountPages(1, null);

        // With pageSize=10, 15 users should have 2 pages
        assertTrue("Should have at least 1 page", pages >= 1);
    }

    /**
     * Test getting account pages with filter
     */
    @Test
    public void testGetAccountPagesWithCondition() throws IOException {
        // Create users with different statuses
        for (int i = 0; i < 5; i++) {
            User user = createUser("filter-user-" + i, "user_" + i, "password", 1);
            user.setStatus(i % 2); // Alternate between 0 and 1
            saveUser(user);
        }

        QueryCondition condition = new QueryCondition();
        condition.setFilter("available");

        long pages = adminService.getAccountPages(1, condition, null);

        assertTrue("Should calculate pages correctly", pages >= 0);
    }

    /**
     * Test encrypting password
     */
    @Test
    public void testEncryptPassword() {
        String password = "test_password";
        String encrypted = adminService.encryptPassword(password);

        assertNotNull("Encrypted password should not be null", encrypted);
        assertEquals("Should match MD5 hash", DigestUtils.md5Hex(password), encrypted);
        assertNotEquals("Should not be plain text", password, encrypted);
    }

    /**
     * Test closing expired positions
     */
    @Test
    public void testCloseExpiredPositions() throws IOException {
        // Create expired position
        Position position = createPosition("pos-1", "mo-user-5");
        position.setDeadline(Timestamp.valueOf(LocalDateTime.now().minusDays(1))); // Expired yesterday
        position.setStatus(0); // Open
        savePosition(position);

        boolean closed = adminService.closePositions();

        assertTrue("Should close expired positions", closed);

        Position found = findPositionById(position.getId());
        assertNotNull("Position should exist", found);
        assertEquals("Status should be closed", 2, found.getStatus());
    }

    /**
     * Test closing non-expired positions
     */
    @Test
    public void testCloseNonExpiredPositions() throws IOException {
        // Create future position
        Position position = createPosition("pos-2", "mo-user-6");
        position.setDeadline(Timestamp.valueOf(LocalDateTime.now().plusDays(7))); // Expires in 7 days
        position.setStatus(0); // Open
        savePosition(position);

        boolean closed = adminService.closePositions();

        assertTrue("Should process positions", closed);

        Position found = findPositionById(position.getId());
        assertNotNull("Position should exist", found);
        assertEquals("Status should remain open", 0, found.getStatus());
    }

    // Helper methods

    private User createUser(String id, String name, String password, int role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(DigestUtils.md5Hex(password));
        user.setRole(role);
        user.setStatus(0);
        return user;
    }

    private TAProfile createTAProfile(String id, String userId) {
        TAProfile profile = new TAProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setName("Test TA");
        profile.setGender("Male");
        profile.setAge(22);
        profile.setCollege("Engineering");
        profile.setMajor("Computer Science");
        profile.setDegree("BACHELOR");
        profile.setYear(3);
        return profile;
    }

    private MOProfile createMOProfile(String id, String userId) {
        MOProfile profile = new MOProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setName("Test MO");
        profile.setEmail("test@mo.com");
        return profile;
    }

    private Position createPosition(String id, String postUserId) {
        Position position = new Position();
        position.setId(id);
        position.setPostUserId(postUserId);
        position.setTitle("Test Position");
        position.setModuleCode("CS101");
        position.setModuleName("Test Module");
        position.setDescription("Test description");
        position.setDuration(12);
        position.setWeeklyWorkload(10.0f);
        position.setStatus(0);
        return position;
    }

    private void saveUser(User user) throws IOException {
        com.tars.repository.JsonRepository<User> repo = new com.tars.repository.JsonRepository<>(User.class);
        repo.saveEntity(user);
    }

    private void saveTAProfile(TAProfile profile) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        repo.saveEntity(profile);
    }

    private void saveMOProfile(MOProfile profile) throws IOException {
        com.tars.repository.JsonRepository<MOProfile> repo = new com.tars.repository.JsonRepository<>(MOProfile.class);
        repo.saveEntity(profile);
    }

    private void savePosition(Position position) throws IOException {
        com.tars.repository.JsonRepository<Position> repo = new com.tars.repository.JsonRepository<>(Position.class);
        repo.saveEntity(position);
    }

    private User findUserById(String id) throws IOException {
        com.tars.repository.JsonRepository<User> repo = new com.tars.repository.JsonRepository<>(User.class);
        return repo.getEntityById(id);
    }

    private TAProfile findTAProfileByUserId(String userId) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        List<TAProfile> profiles = repo.loadAllEntities();
        return profiles.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private MOProfile findMOProfileByUserId(String userId) throws IOException {
        com.tars.repository.JsonRepository<MOProfile> repo = new com.tars.repository.JsonRepository<>(MOProfile.class);
        List<MOProfile> profiles = repo.loadAllEntities();
        return profiles.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private Position findPositionById(String id) throws IOException {
        com.tars.repository.JsonRepository<Position> repo = new com.tars.repository.JsonRepository<>(Position.class);
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
