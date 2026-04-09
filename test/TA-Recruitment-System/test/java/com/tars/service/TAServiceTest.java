package com.tars.service;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.ta.AppPosDTO;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for TAService
 * Tests applicant profile management, job applications, and position browsing
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/7
 */
public class TAServiceTest {

    private TAService taService;

    @Before
    public void setUp() {
        taService = new TAService();
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
        String[] files = {"taprofile.json", "application.json", "position.json"};
        for (String file : files) {
            java.io.File testFile = new java.io.File("data", file);
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    // ==================== PROFILE CREATION TESTS ====================

    @Test
    public void testCreateProfileSuccess() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("user123");
        profile.setName("John Doe");
        profile.setEmail("john@example.com");
        profile.setPhone("1234567890");

        // Act
        boolean created = taService.createProfile(profile);

        // Assert
        assertTrue(created);
        assertNotNull(profile.getId());
        assertNotNull(profile.getCreateAt());
    }

    @Test
    public void testCreateMultipleProfiles() {
        // Arrange
        TAProfile profile1 = new TAProfile();
        profile1.setUserId("user1");
        profile1.setName("User One");

        TAProfile profile2 = new TAProfile();
        profile2.setUserId("user2");
        profile2.setName("User Two");

        // Act
        boolean created1 = taService.createProfile(profile1);
        boolean created2 = taService.createProfile(profile2);

        // Assert
        assertTrue(created1);
        assertTrue(created2);
        assertNotEquals(profile1.getId(), profile2.getId());
    }

    @Test
    public void testCreateProfileWithMinimalData() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("minimal");
        profile.setName("Minimal User");
        // Other fields left null

        // Act
        boolean created = taService.createProfile(profile);

        // Assert
        assertTrue(created);
        assertNotNull(profile.getId());
    }

    // ==================== CHECK PROFILE EXIST TESTS ====================

    @Test
    public void testCheckProfileExistReturnsTrue() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("existuser");
        profile.setName("Test User");
        taService.createProfile(profile);

        // Act
        boolean exists = taService.checkProfileExist("existuser");

        // Assert
        assertTrue(exists);
    }

    @Test
    public void testCheckProfileExistReturnsFalse() {
        // Act
        boolean exists = taService.checkProfileExist("nonexistent");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testCheckProfileExistWithNull() {
        // Act
        boolean exists = taService.checkProfileExist(null);

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testCheckProfileExistWithEmptyString() {
        // Act
        boolean exists = taService.checkProfileExist("");

        // Assert
        assertFalse(exists);
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    public void testGetProfileDTOExists() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("profileuser");
        profile.setName("Profile User");
        profile.setEmail("profile@test.com");
        taService.createProfile(profile);

        // Act
        ProfileDTO dto = taService.getProfileDTO("profileuser");

        // Assert
        assertNotNull(dto);
        assertEquals("Profile User", dto.getName());
        assertEquals("profile@test.com", dto.getEmail());
    }

    @Test
    public void testGetProfileDTONotExists() {
        // Act
        ProfileDTO dto = taService.getProfileDTO("nonexistent");

        // Assert
        assertNull(dto);
    }

    @Test
    public void testGetProfileEntityExists() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("entityuser");
        profile.setName("Entity User");
        taService.createProfile(profile);

        // Act
        TAProfile retrieved = taService.getProfile("entityuser");

        // Assert
        assertNotNull(retrieved);
        assertEquals("entityuser", retrieved.getUserId());
        assertEquals("Entity User", retrieved.getName());
    }

    @Test
    public void testGetProfileEntityNotExists() {
        // Act
        TAProfile profile = taService.getProfile("nonexistent");

        // Assert
        assertNull(profile);
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    public void testUpdateProfileSuccess() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("updateuser");
        profile.setName("Original Name");
        profile.setEmail("original@test.com");
        taService.createProfile(profile);

        // Act
        profile.setName("Updated Name");
        profile.setEmail("updated@test.com");
        boolean updated = taService.updateProfile(profile);

        // Assert
        assertTrue(updated);
        TAProfile retrieved = taService.getProfile("updateuser");
        assertEquals("Updated Name", retrieved.getName());
        assertEquals("updated@test.com", retrieved.getEmail());
    }

    @Test
    public void testUpdateProfileWithNullName() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("nullname");
        profile.setName("Valid Name");
        taService.createProfile(profile);

        // Act
        profile.setName(null);
        boolean updated = taService.updateProfile(profile);

        // Assert
        assertFalse(updated); // Should fail validation
    }

    @Test
    public void testUpdateProfileWithEmptyName() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("emptyname");
        profile.setName("Valid Name");
        taService.createProfile(profile);

        // Act
        profile.setName("");
        boolean updated = taService.updateProfile(profile);

        // Assert
        assertFalse(updated); // Should fail validation
    }

    @Test
    public void testUpdateProfileWithNullUserId() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("validuser");
        profile.setName("Test");
        taService.createProfile(profile);

        // Act
        profile.setUserId(null);
        boolean updated = taService.updateProfile(profile);

        // Assert
        assertFalse(updated); // Should fail validation
    }

    @Test
    public void testUpdateNonExistentProfile() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("nonexistent");
        profile.setName("Test");

        // Act
        boolean updated = taService.updateProfile(profile);

        // Assert
        assertFalse(updated);
    }

    @Test
    public void testUpdateProfilePreservesUnchangedFields() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("preserve");
        profile.setName("Original");
        profile.setEmail("original@test.com");
        profile.setPhone("123456");
        taService.createProfile(profile);

        // Act - Update only name
        profile.setName("New Name");
        taService.updateProfile(profile);

        // Assert
        TAProfile retrieved = taService.getProfile("preserve");
        assertEquals("New Name", retrieved.getName());
        assertEquals("original@test.com", retrieved.getEmail()); // Preserved
        assertEquals("123456", retrieved.getPhone()); // Preserved
    }

    // ==================== APPLICATION CREATION TESTS ====================

    @Test
    public void testCreateApplicationSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Software Engineer");
        position.setAppliedNum(0);
        savePosition(position);

        Application app = new Application();
        app.setUserId("applicant1");
        app.setPositionId(position.getId());
        app.setStatus(0); // Applied

        // Act
        boolean created = taService.createApplication(app);

        // Assert
        assertTrue(created);
        assertNotNull(app.getId());

        // Verify position applied count incremented
        Position updated = getPositionById(position.getId());
        assertEquals(1, updated.getAppliedNum());
    }

    @Test
    public void testCreateApplicationIncrementsCounter() {
        // Arrange
        Position position = new Position();
        position.setTitle("Developer");
        position.setAppliedNum(5);
        savePosition(position);

        Application app = new Application();
        app.setUserId("user1");
        app.setPositionId(position.getId());
        app.setStatus(0);

        // Act
        taService.createApplication(app);

        // Assert
        Position updated = getPositionById(position.getId());
        assertEquals(6, updated.getAppliedNum());
    }

    @Test
    public void testCreateMultipleApplications() {
        // Arrange
        Position position = new Position();
        position.setTitle("Analyst");
        position.setAppliedNum(0);
        savePosition(position);

        Application app1 = new Application();
        app1.setUserId("user1");
        app1.setPositionId(position.getId());
        app1.setStatus(0);

        Application app2 = new Application();
        app2.setUserId("user2");
        app2.setPositionId(position.getId());
        app2.setStatus(0);

        // Act
        taService.createApplication(app1);
        taService.createApplication(app2);

        // Assert
        Position updated = getPositionById(position.getId());
        assertEquals(2, updated.getAppliedNum());
    }

    // ==================== WITHDRAW APPLICATION TESTS ====================

    @Test
    public void testWithdrawApplicationSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Manager");
        position.setAppliedNum(1);
        savePosition(position);

        Application app = new Application();
        app.setUserId("withdrawer");
        app.setPositionId(position.getId());
        app.setStatus(0);
        saveApplication(app);

        // Act
        taService.withdrawApplication(app.getId(), "withdrawer");

        // Assert
        Application withdrawn = getApplicationById(app.getId());
        assertEquals(3, withdrawn.getStatus()); // Status 3 = withdrawn

        Position updated = getPositionById(position.getId());
        assertEquals(0, updated.getAppliedNum()); // Count decremented
    }

    @Test
    public void testWithdrawApplicationWrongUser() {
        // Arrange
        Position position = new Position();
        position.setTitle("Role");
        position.setAppliedNum(1);
        savePosition(position);

        Application app = new Application();
        app.setUserId("owner");
        app.setPositionId(position.getId());
        app.setStatus(0);
        saveApplication(app);

        int initialCount = position.getAppliedNum();

        // Act - Try to withdraw as different user
        taService.withdrawApplication(app.getId(), "someoneelse");

        // Assert - Should not withdraw
        Application retrieved = getApplicationById(app.getId());
        assertEquals(0, retrieved.getStatus()); // Status unchanged

        Position updated = getPositionById(position.getId());
        assertEquals(initialCount, updated.getAppliedNum()); // Count unchanged
    }

    @Test
    public void testWithdrawNonExistentApplication() {
        // Act - Should not throw exception
        taService.withdrawApplication("nonexistent", "user1");

        // Assert - No exception thrown (void method)
    }

    // ==================== GET APPLICATION-POSITION LIST TESTS ====================

    @Test
    public void testGetAppPosListReturnsApplications() {
        // Arrange
        Position pos1 = new Position();
        pos1.setTitle("Job 1");
        savePosition(pos1);

        Position pos2 = new Position();
        pos2.setTitle("Job 2");
        savePosition(pos2);

        Application app1 = new Application();
        app1.setUserId("myuser");
        app1.setPositionId(pos1.getId());
        app1.setStatus(0);
        saveApplication(app1);

        Application app2 = new Application();
        app2.setUserId("myuser");
        app2.setPositionId(pos2.getId());
        app2.setStatus(1);
        saveApplication(app2);

        // Act
        List<AppPosDTO> list = taService.getAppPosList("myuser", 1);

        // Assert
        assertEquals(2, list.size());
    }

    @Test
    public void testGetAppPosListFiltersByUser() {
        // Arrange
        Position position = new Position();
        position.setTitle("Shared Job");
        savePosition(position);

        Application app1 = new Application();
        app1.setUserId("user1");
        app1.setPositionId(position.getId());
        app1.setStatus(0);
        saveApplication(app1);

        Application app2 = new Application();
        app2.setUserId("user2");
        app2.setPositionId(position.getId());
        app2.setStatus(0);
        saveApplication(app2);

        // Act
        List<AppPosDTO> user1Apps = taService.getAppPosList("user1", 1);
        List<AppPosDTO> user2Apps = taService.getAppPosList("user2", 1);

        // Assert
        assertEquals(1, user1Apps.size());
        assertEquals(1, user2Apps.size());
    }

    @Test
    public void testGetAppPosListEmptyForUser() {
        // Act
        List<AppPosDTO> list = taService.getAppPosList("noapps", 1);

        // Assert
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetAppPosListWithNullUserId() {
        // Act
        List<AppPosDTO> list = taService.getAppPosList(null, 1);

        // Assert
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== POSITION LIST TESTS ====================

    @Test
    public void testGetPositionListReturnsActivePositions() {
        // Arrange
        Position pos1 = new Position();
        pos1.setTitle("Active Job");
        pos1.setStatus(0); // Active
        savePosition(pos1);

        Position pos2 = new Position();
        pos2.setTitle("Closed Job");
        pos2.setStatus(3); // Closed
        savePosition(pos2);

        // Act
        List<PosBriefDTO> positions = taService.getPositionList("viewer", 1);

        // Assert
        assertTrue(positions.stream().anyMatch(p -> p.getTitle().equals("Active Job")));
        assertFalse(positions.stream().anyMatch(p -> p.getTitle().equals("Closed Job")));
    }

    @Test
    public void testGetPositionListPagination() {
        // Arrange - Create 15 positions
        for (int i = 0; i < 15; i++) {
            Position pos = new Position();
            pos.setTitle("Position " + i);
            pos.setStatus(0);
            savePosition(pos);
        }

        // Act
        List<PosBriefDTO> page1 = taService.getPositionList("user", 1);
        List<PosBriefDTO> page2 = taService.getPositionList("user", 2);

        // Assert
        assertEquals(10, page1.size()); // Default page size is 10
        assertEquals(5, page2.size());
    }

    @Test
    public void testGetPositionPages() {
        // Arrange - Create 25 positions
        for (int i = 0; i < 25; i++) {
            Position pos = new Position();
            pos.setTitle("Job " + i);
            pos.setStatus(0);
            savePosition(pos);
        }

        // Act
        long pages = taService.getPositionPages();

        // Assert
        assertEquals(3, pages); // 25 items / 10 per page = 3 pages
    }

    // ==================== VERIFY POSITION AVAILABLE TESTS ====================

    @Test
    public void testVerifyPosAvailableReturnsTrue() {
        // Arrange
        Position position = new Position();
        position.setTitle("Available Job");
        position.setStatus(0);
        savePosition(position);

        // Act
        boolean available = taService.verifyPosAvailable(position.getId(), "newuser");

        // Assert
        assertTrue(available);
    }

    @Test
    public void testVerifyPosAvailableReturnsFalseWhenClosed() {
        // Arrange
        Position position = new Position();
        position.setTitle("Closed Job");
        position.setStatus(3); // Closed
        savePosition(position);

        // Act
        boolean available = taService.verifyPosAvailable(position.getId(), "user");

        // Assert
        assertFalse(available);
    }

    @Test
    public void testVerifyPosAvailableReturnsFalseWhenAlreadyApplied() {
        // Arrange
        Position position = new Position();
        position.setTitle("Applied Job");
        position.setStatus(0);
        savePosition(position);

        Application app = new Application();
        app.setUserId("alreadyapplied");
        app.setPositionId(position.getId());
        app.setStatus(0); // Applied
        saveApplication(app);

        // Act
        boolean available = taService.verifyPosAvailable(position.getId(), "alreadyapplied");

        // Assert
        assertFalse(available);
    }

    @Test
    public void testVerifyPosAvailableAllowsReapplyAfterWithdrawal() {
        // Arrange
        Position position = new Position();
        position.setTitle("Reapply Job");
        position.setStatus(0);
        savePosition(position);

        Application app = new Application();
        app.setUserId("reuser");
        app.setPositionId(position.getId());
        app.setStatus(3); // Withdrawn
        saveApplication(app);

        // Act
        boolean available = taService.verifyPosAvailable(position.getId(), "reuser");

        // Assert
        assertTrue(available); // Can reapply after withdrawal
    }

    // ==================== VERIFY PROFILE EXISTS TESTS ====================

    @Test
    public void testVerifyProfileExistsReturnsTrue() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("hasprofile");
        profile.setName("Profiled User");
        taService.createProfile(profile);

        // Act
        boolean exists = taService.verifyProfileExists("hasprofile");

        // Assert
        assertTrue(exists);
    }

    @Test
    public void testVerifyProfileExistsReturnsFalse() {
        // Act
        boolean exists = taService.verifyProfileExists("noprofile");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testVerifyProfileExistsWithNull() {
        // Act
        boolean exists = taService.verifyProfileExists(null);

        // Assert
        assertFalse(exists);
    }

    // ==================== HELPER METHODS ====================

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
