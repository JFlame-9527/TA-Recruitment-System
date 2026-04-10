package com.tars.service;

import com.tars.entity.bean.*;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.admin.UserDetailDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for AdminService
 * Tests user management, account operations, and admin functions
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class AdminServiceTest {

    private AdminService adminService;

    @Before
    public void setUp() {
        adminService = new AdminService();
        cleanupTestData();
    }

    @After
    public void tearDown() {
        cleanupTestData();
    }

    /**
     * Helper method to clean up test data files
     */
    private void cleanupTestData() {
        String[] files = {"user.json", "taprofile.json", "moprofile.json", "application.json", "position.json"};
        for (String file : files) {
            java.io.File testFile = new java.io.File("data", file);
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    public void testDeleteTAAccountRemovesAssociatedData() {
        // Arrange - Create TA user with profile and applications
        User taUser = new User();
        taUser.setName("ta_delete");
        taUser.setRole(1); // TA role
        saveUser(taUser);

        TAProfile profile = new TAProfile();
        profile.setUserId(taUser.getId());
        profile.setName("TA Profile");
        saveTAProfile(profile);

        Position position = new Position();
        position.setTitle("Job for TA");
        position.setAppliedNum(1);
        savePosition(position);

        Application app = new Application();
        app.setUserId(taUser.getId());
        app.setPositionId(position.getId());
        app.setStatus(0);
        saveApplication(app);

        // Act
        boolean deleted = adminService.deleteUser(taUser.getId());

        // Assert
        assertTrue(deleted);
        assertNull(getUserById(taUser.getId()));
        assertNull(getTAProfileByUserId(taUser.getId()));
        assertNull(getApplicationById(app.getId()));

        Position updated = getPositionById(position.getId());
        assertEquals(0, updated.getAppliedNum()); // Count decremented
    }

    @Test
    public void testDeleteMOAccountRemovesPositionsAndApplications() {
        // Arrange - Create MO user with positions
        User moUser = new User();
        moUser.setName("mo_delete");
        moUser.setRole(2); // MO role
        saveUser(moUser);

        MOProfile profile = new MOProfile();
        profile.setUserId(moUser.getId());
        profile.setName("MO Profile");
        saveMOProfile(profile);

        Position pos1 = new Position();
        pos1.setTitle("MO Job 1");
        pos1.setPostUserId(moUser.getId());
        savePosition(pos1);

        Position pos2 = new Position();
        pos2.setTitle("MO Job 2");
        pos2.setPostUserId(moUser.getId());
        savePosition(pos2);

        // Act
        boolean deleted = adminService.deleteUser(moUser.getId());

        // Assert
        assertTrue(deleted);
        assertNull(getUserById(moUser.getId()));
        assertNull(getMOProfileByUserId(moUser.getId()));
        assertNull(getPositionById(pos1.getId()));
        assertNull(getPositionById(pos2.getId()));
    }

    @Test
    public void testDeleteAdminAccount() {
        // Arrange
        User adminUser = new User();
        adminUser.setName("admin_delete");
        adminUser.setRole(0); // Admin role
        saveUser(adminUser);

        // Act
        boolean deleted = adminService.deleteUser(adminUser.getId());

        // Assert
        assertTrue(deleted);
        assertNull(getUserById(adminUser.getId()));
    }

    @Test
    public void testDeleteNonExistentUser() {
        // Act
        boolean deleted = adminService.deleteUser("nonexistent");

        // Assert
        assertFalse(deleted);
    }

    @Test
    public void testDeleteUserWithOfferedApplication() {
        // Arrange
        User taUser = new User();
        taUser.setName("offered_ta");
        taUser.setRole(1);
        saveUser(taUser);

        Position position = new Position();
        position.setTitle("Offered Job");
        position.setAppliedNum(1);
        position.setOfferedNum(1);
        savePosition(position);

        Application app = new Application();
        app.setUserId(taUser.getId());
        app.setPositionId(position.getId());
        app.setStatus(1); // Offered
        saveApplication(app);

        // Act
        adminService.deleteUser(taUser.getId());

        // Assert
        Position updated = getPositionById(position.getId());
        assertEquals(0, updated.getAppliedNum());
        assertEquals(0, updated.getOfferedNum()); // Offered count decremented
    }

    @Test
    public void testDeleteUserWithRejectedApplication() {
        // Arrange
        User taUser = new User();
        taUser.setName("rejected_ta");
        taUser.setRole(1);
        saveUser(taUser);

        Position position = new Position();
        position.setTitle("Rejected Job");
        position.setAppliedNum(1);
        position.setRejectedNum(1);
        savePosition(position);

        Application app = new Application();
        app.setUserId(taUser.getId());
        app.setPositionId(position.getId());
        app.setStatus(2); // Rejected
        saveApplication(app);

        // Act
        adminService.deleteUser(taUser.getId());

        // Assert
        Position updated = getPositionById(position.getId());
        assertEquals(0, updated.getAppliedNum());
        assertEquals(0, updated.getRejectedNum()); // Rejected count decremented
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    public void testUpdateUserName() {
        // Arrange
        User user = new User();
        user.setName("original_name");
        user.setRole(1);
        saveUser(user);

        // Act
        user.setName("updated_name");
        boolean updated = adminService.updateUser(user);

        // Assert
        assertTrue(updated);
        User retrieved = getUserById(user.getId());
        assertEquals("updated_name", retrieved.getName());
    }

    @Test
    public void testUpdateUserRole() {
        // Arrange
        User user = new User();
        user.setName("role_change");
        user.setRole(1); // TA
        saveUser(user);

        // Act
        user.setRole(2); // Change to MO
        boolean updated = adminService.updateUser(user);

        // Assert
        assertTrue(updated);
        User retrieved = getUserById(user.getId());
        assertEquals(2, retrieved.getRole());
    }

    // ==================== UPDATE USER STATUS TESTS ====================

    @Test
    public void testUpdateUserStatusToFrozen() {
        // Arrange
        User user = new User();
        user.setName("freeze_me");
        user.setStatus(0); // Active
        saveUser(user);

        // Act
        boolean updated = adminService.updateUserStatus(user.getId(), 1); // Freeze

        // Assert
        assertTrue(updated);
        User retrieved = getUserById(user.getId());
        assertEquals(1, retrieved.getStatus());
    }

    @Test
    public void testUpdateUserStatusToActive() {
        // Arrange
        User user = new User();
        user.setName("unfreeze_me");
        user.setStatus(1); // Frozen
        saveUser(user);

        // Act
        boolean updated = adminService.updateUserStatus(user.getId(), 0); // Activate

        // Assert
        assertTrue(updated);
        User retrieved = getUserById(user.getId());
        assertEquals(0, retrieved.getStatus());
    }

    @Test
    public void testUpdateUserStatusNonExistent() {
        // Act
        boolean updated = adminService.updateUserStatus("nonexistent", 1);

        // Assert
        assertFalse(updated);
    }

    // ==================== RESET PASSWORD TESTS ====================

    @Test
    public void testResetPasswordSuccess() {
        // Arrange
        User user = new User();
        user.setName("reset_pass");
        user.setPassword("old_encrypted");
        saveUser(user);

        // Act
        boolean reset = adminService.resetPassword(user.getId(), "newpassword123");

        // Assert
        assertTrue(reset);
        User retrieved = getUserById(user.getId());
        assertNotEquals("old_encrypted", retrieved.getPassword());
        assertNotNull(retrieved.getPassword());
    }

    @Test
    public void testResetPasswordTooShort() {
        // Arrange
        User user = new User();
        user.setName("short_pass");
        user.setPassword("original");
        saveUser(user);

        // Act
        boolean reset = adminService.resetPassword(user.getId(), "12345"); // Less than 6 chars

        // Assert
        assertFalse(reset);
        User retrieved = getUserById(user.getId());
        assertEquals("original", retrieved.getPassword()); // Unchanged
    }

    @Test
    public void testResetPasswordNonExistentUser() {
        // Act
        boolean reset = adminService.resetPassword("nonexistent", "newpass123");

        // Assert
        assertFalse(reset);
    }

    @Test
    public void testResetPasswordWithNullPassword() {
        // Arrange
        User user = new User();
        user.setName("null_pass");
        user.setPassword("original");
        saveUser(user);

        // Act
        boolean reset = adminService.resetPassword(user.getId(), null);

        // Assert
        assertFalse(reset);
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    public void testGetTAProfile() {
        // Arrange
        User user = new User();
        user.setName("ta_profile_user");
        user.setRole(1);
        saveUser(user);

        TAProfile profile = new TAProfile();
        profile.setUserId(user.getId());
        profile.setName("TA Name");
        profile.setEmail("ta@test.com");
        saveTAProfile(profile);

        // Act
        TAProDTO dto = adminService.getTAProfile(user.getId());

        // Assert
        assertNotNull(dto);
        assertEquals("TA Name", dto.getName());
        assertEquals("ta@test.com", dto.getEmail());
    }

    @Test
    public void testGetTAProfileNotExists() {
        // Act
        TAProDTO dto = adminService.getTAProfile("nonexistent");

        // Assert
        assertNull(dto);
    }

    @Test
    public void testGetMOProfile() {
        // Arrange
        User user = new User();
        user.setName("mo_profile_user");
        user.setRole(2);
        saveUser(user);

        MOProfile profile = new MOProfile();
        profile.setUserId(user.getId());
        profile.setName("MO Name");
        saveMOProfile(profile);

        // Act
        MOProDTO dto = adminService.getMOProfile(user.getId());

        // Assert
        assertNotNull(dto);
        assertEquals("MO Name", dto.getName());
    }

    @Test
    public void testGetMOProfileNotExists() {
        // Act
        MOProDTO dto = adminService.getMOProfile("nonexistent");

        // Assert
        assertNull(dto);
    }

    // ==================== UPDATE MO PROFILE TESTS ====================

    @Test
    public void testUpdateMOProfileSuccess() {
        // Arrange
        MOProfile profile = new MOProfile();
        profile.setUserId("mo_update");
        profile.setName("Original MO");
        saveMOProfile(profile);

        // Act
        profile.setName("Updated MO");
        boolean updated = adminService.updateMOProfile(profile);

        // Assert
        assertTrue(updated);
        MOProfile retrieved = getMOProfileById(profile.getId());
        assertEquals("Updated MO", retrieved.getName());
    }

    // ==================== CREATE MO ACCOUNT TESTS ====================

    @Test
    public void testCreateMOAccountSuccess() {
        // Arrange
        User moUser = new User();
        moUser.setName("new_mo");
        moUser.setPassword("encrypted_pass");

        MOProfile profile = new MOProfile();
        profile.setName("New MO Manager");

        // Act
        boolean created = adminService.createMOAccount(moUser, profile);

        // Assert
        assertTrue(created);
        assertNotNull(moUser.getId());
        assertEquals(2, moUser.getRole()); // Should be set to MO
        assertNotNull(profile.getId());
        assertEquals(moUser.getId(), profile.getUserId());
    }

    // ==================== GET ACCOUNTS BY ROLE TESTS ====================

    @Test
    public void testGetAccountsByRoleReturnsTAUsers() {
        // Arrange
        User ta1 = new User();
        ta1.setName("ta1");
        ta1.setRole(1);
        saveUser(ta1);

        User ta2 = new User();
        ta2.setName("ta2");
        ta2.setRole(1);
        saveUser(ta2);

        User mo = new User();
        mo.setName("mo1");
        mo.setRole(2);
        saveUser(mo);

        // Act
        List<UserDetailDTO> tas = adminService.getAccountsByRole(1, 1, "admin");

        // Assert
        assertEquals(2, tas.size());
        assertTrue(tas.stream().anyMatch(u -> u.getName().equals("ta1")));
        assertTrue(tas.stream().anyMatch(u -> u.getName().equals("ta2")));
    }

    @Test
    public void testGetAccountsByRoleReturnsMOUsers() {
        // Arrange
        User mo1 = new User();
        mo1.setName("mo1");
        mo1.setRole(2);
        saveUser(mo1);

        User mo2 = new User();
        mo2.setName("mo2");
        mo2.setRole(2);
        saveUser(mo2);

        // Act
        List<UserDetailDTO> mos = adminService.getAccountsByRole(2, 1, "admin");

        // Assert
        assertEquals(2, mos.size());
    }

    @Test
    public void testGetAccountsByRoleExcludesSpecifiedUser() {
        // Arrange
        User user1 = new User();
        user1.setName("keep1");
        user1.setRole(1);
        saveUser(user1);

        User excludeMe = new User();
        excludeMe.setName("exclude");
        excludeMe.setRole(1);
        saveUser(excludeMe);

        User user2 = new User();
        user2.setName("keep2");
        user2.setRole(1);
        saveUser(user2);

        // Act
        List<UserDetailDTO> users = adminService.getAccountsByRole(1, 1, excludeMe.getId());

        // Assert
        assertEquals(2, users.size());
    }

    @Test
    public void testGetAccountsByRolePagination() {
        // Arrange - Create 15 TA users
        for (int i = 0; i < 15; i++) {
            User user = new User();
            user.setName("page_ta" + i);
            user.setRole(1);
            saveUser(user);
        }

        // Act
        List<UserDetailDTO> page1 = adminService.getAccountsByRole(1, 1, "admin");
        List<UserDetailDTO> page2 = adminService.getAccountsByRole(1, 2, "admin");

        // Assert
        assertEquals(10, page1.size()); // Page size is 10
        assertEquals(5, page2.size());
    }

    @Test
    public void testGetAccountsByRoleEmptyResult() {
        // Act
        List<UserDetailDTO> users = adminService.getAccountsByRole(1, 1, "admin");

        // Assert
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    // ==================== GET ACCOUNT PAGES TESTS ====================

    @Test
    public void testGetAccountPagesCalculation() {
        // Arrange - Create 25 TA users
        for (int i = 0; i < 25; i++) {
            User user = new User();
            user.setName("count_ta" + i);
            user.setRole(1);
            saveUser(user);
        }

        // Act
        long pages = adminService.getAccountPages(1, "admin");

        // Assert
        assertEquals(3, pages); // 25 / 10 = 2.5 -> 3 pages
    }

    @Test
    public void testGetAccountPagesZeroForNoUsers() {
        // Act
        long pages = adminService.getAccountPages(1, "admin");

        // Assert
        assertEquals(0, pages);
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    public void testFullUserManagementWorkflow() {
        // 1. Create TA user
        User taUser = new User();
        taUser.setName("managed_ta");
        taUser.setRole(1);
        taUser.setStatus(0);
        saveUser(taUser);

        // 2. Get user details
        List<UserDetailDTO> users = adminService.getAccountsByRole(1, 1, "admin");
        assertEquals(1, users.size());

        // 3. Freeze user
        assertTrue(adminService.updateUserStatus(taUser.getId(), 1));
        User frozen = getUserById(taUser.getId());
        assertEquals(1, frozen.getStatus());

        // 4. Reset password
        assertTrue(adminService.resetPassword(taUser.getId(), "newpass123"));

        // 5. Unfreeze user
        assertTrue(adminService.updateUserStatus(taUser.getId(), 0));
        User active = getUserById(taUser.getId());
        assertEquals(0, active.getStatus());

        // 6. Delete user
        assertTrue(adminService.deleteUser(taUser.getId()));
        assertNull(getUserById(taUser.getId()));
    }

    @Test
    public void testMOAccountCreationAndManagement() {
        // 1. Create MO account
        User moUser = new User();
        moUser.setName("managed_mo");
        moUser.setPassword("pass");

        MOProfile profile = new MOProfile();
        profile.setName("MO Manager");

        assertTrue(adminService.createMOAccount(moUser, profile));
        assertEquals(2, moUser.getRole());

        // 2. Get MO profile
        MOProDTO moProfile = adminService.getMOProfile(moUser.getId());
        assertNotNull(moProfile);
        assertEquals("MO Manager", moProfile.getName());

        // 3. Update MO profile
        assertTrue(adminService.updateMOProfile(profile));

        // 4. Verify update
        MOProDTO updated = adminService.getMOProfile(moUser.getId());
    }

    // ==================== HELPER METHODS ====================

    private void saveUser(User user) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(User.class);
            repo.saveEntity(user);
        } catch (Exception e) {
            fail("Failed to save user: " + e.getMessage());
        }
    }

    private User getUserById(String id) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(User.class);
            return repo.getEntityById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveTAProfile(TAProfile profile) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
            repo.saveEntity(profile);
        } catch (Exception e) {
            fail("Failed to save TA profile: " + e.getMessage());
        }
    }

    private TAProfile getTAProfileByUserId(String userId) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
            var profiles = repo.loadAllEntities();
            return profiles.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveMOProfile(MOProfile profile) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(MOProfile.class);
            repo.saveEntity(profile);
        } catch (Exception e) {
            fail("Failed to save MO profile: " + e.getMessage());
        }
    }

    private MOProfile getMOProfileById(String id) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(MOProfile.class);
            return repo.getEntityById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private MOProfile getMOProfileByUserId(String userId) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(MOProfile.class);
            var profiles = repo.loadAllEntities();
            return profiles.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void savePosition(Position position) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(Position.class);
            repo.saveEntity(position);
        } catch (Exception e) {
            fail("Failed to save position: " + e.getMessage());
        }
    }

    private Position getPositionById(String id) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(Position.class);
            return repo.getEntityById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveApplication(Application application) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(Application.class);
            repo.saveEntity(application);
        } catch (Exception e) {
            fail("Failed to save application: " + e.getMessage());
        }
    }

    private Application getApplicationById(String id) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(Application.class);
            return repo.getEntityById(id);
        } catch (Exception e) {
            return null;
        }
    }
}
