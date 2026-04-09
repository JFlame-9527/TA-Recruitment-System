package com.tars.service;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.MOProfile;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.mo.ApplicationDTO;
import com.tars.entity.dto.mo.PosBriefDTO;
import com.tars.entity.dto.mo.PosDetailDTO;
import com.tars.entity.dto.mo.ProfileDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for MOService
 * Tests position management, application review, and manager operations
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class MOServiceTest {

    private MOService moService;

    @Before
    public void setUp() {
        moService = new MOService();
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
        String[] files = {"position.json", "application.json", "taprofile.json"};
        for (String file : files) {
            java.io.File testFile = new java.io.File("data", file);
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    // ==================== CREATE POSITION TESTS ====================

    @Test
    public void testCreatePositionSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Senior Developer");
        position.setDescription("Looking for experienced developer");
        position.setRequiredNum(5);
        position.setPostUserId("manager1");
        position.setStatus(0);

        // Act
        boolean created = moService.createPosition(position);

        // Assert
        assertTrue(created);
        assertNotNull(position.getId());
        assertNotNull(position.getCreateAt());
        assertEquals(0, position.getAppliedNum());
        assertEquals(0, position.getOfferedNum());
        assertEquals(0, position.getRejectedNum());
    }

    @Test
    public void testCreateMultiplePositions() {
        // Arrange
        Position pos1 = new Position();
        pos1.setTitle("Position 1");
        pos1.setPostUserId("mgr1");

        Position pos2 = new Position();
        pos2.setTitle("Position 2");
        pos2.setPostUserId("mgr1");

        // Act
        boolean created1 = moService.createPosition(pos1);
        boolean created2 = moService.createPosition(pos2);

        // Assert
        assertTrue(created1);
        assertTrue(created2);
        assertNotEquals(pos1.getId(), pos2.getId());
    }

    @Test
    public void testCreatePositionWithMinimalData() {
        // Arrange
        Position position = new Position();
        position.setTitle("Basic Role");
        position.setPostUserId("manager");

        // Act
        boolean created = moService.createPosition(position);

        // Assert
        assertTrue(created);
        assertNotNull(position.getId());
    }

    // ==================== UPDATE POSITION TESTS ====================

    @Test
    public void testUpdatePositionSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Original Title");
        position.setDescription("Original desc");
        position.setPostUserId("mgr");
        moService.createPosition(position);

        // Act
        position.setTitle("Updated Title");
        position.setDescription("Updated desc");
        position.setRequiredNum(10);
        boolean updated = moService.updatePosition(position);

        // Assert
        assertTrue(updated);
        Position retrieved = getPositionById(position.getId());
        assertEquals("Updated Title", retrieved.getTitle());
        assertEquals("Updated desc", retrieved.getDescription());
        assertEquals(10, retrieved.getRequiredNum());
        assertNotNull(retrieved.getUpdateAt());
    }

    @Test
    public void testUpdatePositionUpdatesTimestamp() throws InterruptedException {
        // Arrange
        Position position = new Position();
        position.setTitle("Timestamp Test");
        position.setPostUserId("mgr");
        moService.createPosition(position);

        var originalUpdateAt = position.getUpdateAt();
        Thread.sleep(10);

        // Act
        position.setTitle("Modified");
        moService.updatePosition(position);

        // Assert
        Position retrieved = getPositionById(position.getId());
        assertNotNull(retrieved.getUpdateAt());
        if (originalUpdateAt != null) {
            assertTrue(retrieved.getUpdateAt().after(originalUpdateAt));
        }
    }

    // ==================== GET POSITION LIST TESTS ====================

    @Test
    public void testGetPositionListReturnsManagerPositions() {
        // Arrange
        Position pos1 = new Position();
        pos1.setTitle("My Job 1");
        pos1.setPostUserId("mymanager");
        savePosition(pos1);

        Position pos2 = new Position();
        pos2.setTitle("My Job 2");
        pos2.setPostUserId("mymanager");
        savePosition(pos2);

        Position pos3 = new Position();
        pos3.setTitle("Other Job");
        pos3.setPostUserId("othermanager");
        savePosition(pos3);

        // Act
        List<PosBriefDTO> positions = moService.getPositionList("mymanager", 1);

        // Assert
        assertEquals(2, positions.size());
        assertTrue(positions.stream().anyMatch(p -> p.getTitle().equals("My Job 1")));
        assertTrue(positions.stream().anyMatch(p -> p.getTitle().equals("My Job 2")));
        assertFalse(positions.stream().anyMatch(p -> p.getTitle().equals("Other Job")));
    }

    @Test
    public void testGetPositionListEmptyForManager() {
        // Act
        List<PosBriefDTO> positions = moService.getPositionList("nopositions", 1);

        // Assert
        assertNotNull(positions);
        assertTrue(positions.isEmpty());
    }

    @Test
    public void testGetPositionListPagination() {
        // Arrange - Create 12 positions for same manager
        for (int i = 0; i < 12; i++) {
            Position pos = new Position();
            pos.setTitle("Job " + i);
            pos.setPostUserId("pager");
            savePosition(pos);
        }

        // Act
        List<PosBriefDTO> page1 = moService.getPositionList("pager", 1);
        List<PosBriefDTO> page2 = moService.getPositionList("pager", 2);

        // Assert
        assertEquals(9, page1.size()); // Default page size is 9
        assertEquals(3, page2.size());
    }

    @Test
    public void testGetPositionListWithNullUserId() {
        // Act
        List<PosBriefDTO> positions = moService.getPositionList(null, 1);

        // Assert
        assertNotNull(positions);
        assertTrue(positions.isEmpty());
    }

    @Test
    public void testGetPositionListWithEmptyUserId() {
        // Act
        List<PosBriefDTO> positions = moService.getPositionList("", 1);

        // Assert
        assertNotNull(positions);
        assertTrue(positions.isEmpty());
    }

    // ==================== GET POSITION PAGES TESTS ====================

    @Test
    public void testGetPositionPagesCalculation() {
        // Arrange - Create 20 positions
        for (int i = 0; i < 20; i++) {
            Position pos = new Position();
            pos.setTitle("Position " + i);
            pos.setPostUserId("countmgr");
            savePosition(pos);
        }

        // Act
        long pages = moService.getPositionPages("countmgr");

        // Assert
        assertEquals(3, pages); // 20 / 9 = 2.22 -> 3 pages
    }

    @Test
    public void testGetPositionPagesZeroForNoPositions() {
        // Act
        long pages = moService.getPositionPages("empty");

        // Assert
        assertEquals(0, pages);
    }

    // ==================== GET POSITION DETAIL TESTS ====================

    @Test
    public void testGetPositionDetailExists() {
        // Arrange
        Position position = new Position();
        position.setTitle("Detailed Position");
        position.setDescription("Full description here");
        position.setRequiredNum(3);
        position.setPostUserId("detailmgr");
        savePosition(position);

        // Act
        PosDetailDTO detail = moService.getPosition(position.getId());

        // Assert
        assertNotNull(detail);
        assertEquals("Detailed Position", detail.getTitle());
        assertEquals("Full description here", detail.getDescription());
        assertEquals(3, detail.getRequiredNum());
    }

    @Test
    public void testGetPositionDetailNotExists() {
        // Act
        PosDetailDTO detail = moService.getPosition("nonexistent");

        // Assert
        assertNull(detail);
    }

    @Test
    public void testGetPositionDetailWithNullId() {
        // Act
        PosDetailDTO detail = moService.getPosition(null);

        // Assert
        assertNull(detail);
    }

    // ==================== VERIFY POSITION OWNER TESTS ====================

    @Test
    public void testVerifyPositionOwnerReturnsTrue() {
        // Arrange
        Position position = new Position();
        position.setTitle("Owned Position");
        position.setPostUserId("owner123");
        savePosition(position);

        // Act
        boolean isOwner = moService.verifyPositionOwner(position.getId(), "owner123");

        // Assert
        assertTrue(isOwner);
    }

    @Test
    public void testVerifyPositionOwnerReturnsFalse() {
        // Arrange
        Position position = new Position();
        position.setTitle("Others Position");
        position.setPostUserId("realowner");
        savePosition(position);

        // Act
        boolean isOwner = moService.verifyPositionOwner(position.getId(), "imposter");

        // Assert
        assertFalse(isOwner);
    }

    @Test
    public void testVerifyPositionOwnerNonExistentPosition() {
        // Act
        boolean isOwner = moService.verifyPositionOwner("nonexistent", "user");

        // Assert
        assertFalse(isOwner);
    }

    @Test
    public void testVerifyPositionOwnerWithNullParams() {
        // Act & Assert
        assertFalse(moService.verifyPositionOwner(null, "user"));
        assertFalse(moService.verifyPositionOwner("pos", null));
        assertFalse(moService.verifyPositionOwner(null, null));
    }

    // ==================== GET APPLICATION LIST TESTS ====================

    @Test
    public void testGetAppListReturnsApplications() {
        // Arrange
        Position position = new Position();
        position.setTitle("Job with Apps");
        position.setPostUserId("appmgr");
        savePosition(position);

        TAProfile profile1 = new TAProfile();
        profile1.setUserId("applicant1");
        profile1.setName("Applicant One");
        saveTAProfile(profile1);

        TAProfile profile2 = new TAProfile();
        profile2.setUserId("applicant2");
        profile2.setName("Applicant Two");
        saveTAProfile(profile2);

        Application app1 = new Application();
        app1.setUserId("applicant1");
        app1.setPositionId(position.getId());
        app1.setStatus(0);
        saveApplication(app1);

        Application app2 = new Application();
        app2.setUserId("applicant2");
        app2.setPositionId(position.getId());
        app2.setStatus(0);
        saveApplication(app2);

        // Act
        List<ApplicationDTO> apps = moService.getAppList(position.getId(), 1);

        // Assert
        assertEquals(2, apps.size());
    }

    @Test
    public void testGetAppListFiltersWithdrawnApplications() {
        // Arrange
        Position position = new Position();
        position.setTitle("Filtered Job");
        position.setPostUserId("filtermgr");
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("withdrawer");
        profile.setName("Withdrawn User");
        saveTAProfile(profile);

        Application app1 = new Application();
        app1.setUserId("withdrawer");
        app1.setPositionId(position.getId());
        app1.setStatus(0); // Applied
        saveApplication(app1);

        Application app2 = new Application();
        app2.setUserId("withdrawer");
        app2.setPositionId(position.getId());
        app2.setStatus(3); // Withdrawn
        saveApplication(app2);

        // Act
        List<ApplicationDTO> apps = moService.getAppList(position.getId(), 1);

        // Assert
        assertEquals(1, apps.size()); // Only non-withdrawn
        assertEquals(0, apps.get(0).getStatus());
    }

    @Test
    public void testGetAppListEmptyPosition() {
        // Arrange
        Position position = new Position();
        position.setTitle("Empty Job");
        position.setPostUserId("emptymgr");
        savePosition(position);

        // Act
        List<ApplicationDTO> apps = moService.getAppList(position.getId(), 1);

        // Assert
        assertNotNull(apps);
        assertTrue(apps.isEmpty());
    }

    @Test
    public void testGetAppListPagination() {
        // Arrange
        Position position = new Position();
        position.setTitle("Paginated Job");
        position.setPostUserId("pagemgr");
        savePosition(position);

        // Create 15 applicants and applications
        for (int i = 0; i < 15; i++) {
            TAProfile profile = new TAProfile();
            profile.setUserId("app" + i);
            profile.setName("Applicant " + i);
            saveTAProfile(profile);

            Application app = new Application();
            app.setUserId("app" + i);
            app.setPositionId(position.getId());
            app.setStatus(0);
            saveApplication(app);
        }

        // Act
        List<ApplicationDTO> page1 = moService.getAppList(position.getId(), 1);
        List<ApplicationDTO> page2 = moService.getAppList(position.getId(), 2);

        // Assert
        assertEquals(10, page1.size()); // Default page size is 10
        assertEquals(5, page2.size());
    }

    // ==================== GET APP PAGES TESTS ====================

    @Test
    public void testGetAppPagesCalculation() {
        // Arrange
        Position position = new Position();
        position.setTitle("Counting Job");
        position.setPostUserId("countmgr2");
        savePosition(position);

        for (int i = 0; i < 25; i++) {
            TAProfile profile = new TAProfile();
            profile.setUserId("capp" + i);
            profile.setName("App " + i);
            saveTAProfile(profile);

            Application app = new Application();
            app.setUserId("capp" + i);
            app.setPositionId(position.getId());
            app.setStatus(0);
            saveApplication(app);
        }

        // Act
        long pages = moService.getAppPages(position.getId());

        // Assert
        assertEquals(3, pages); // 25 / 10 = 2.5 -> 3 pages
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    public void testGetProfileWithApplicationFeedback() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("profileuser");
        profile.setName("Profile User");
        profile.setEmail("profile@test.com");
        saveTAProfile(profile);

        Application application = new Application();
        application.setUserId("profileuser");
        application.setFeedback("Great candidate!");
        saveApplication(application);

        // Act
        ProfileDTO dto = moService.getProfile(profile.getId(), application.getId());

        // Assert
        assertNotNull(dto);
        assertEquals("Profile User", dto.getName());
        assertEquals("profile@test.com", dto.getEmail());
        assertEquals("Great candidate!", dto.getFeedback());
    }

    @Test
    public void testGetProfileWithoutApplication() {
        // Arrange
        TAProfile profile = new TAProfile();
        profile.setUserId("solo");
        profile.setName("Solo User");
        saveTAProfile(profile);

        // Act
        ProfileDTO dto = moService.getProfile(profile.getId(), null);

        // Assert
        assertNotNull(dto);
        assertEquals("Solo User", dto.getName());
        assertEquals("", dto.getFeedback()); // Empty feedback when no application
    }

    @Test
    public void testGetProfileNotExists() {
        // Act
        ProfileDTO dto = moService.getProfile("nonexistent", null);

        // Assert
        assertNull(dto);
    }

    // ==================== OFFER APPLICATION TESTS ====================

    @Test
    public void testOfferApplicationSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Offer Job");
        position.setPostUserId("offermgr");
        position.setRequiredNum(2);
        position.setOfferedNum(0);
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("offered");
        profile.setName("Offered User");
        saveTAProfile(profile);

        Application app = new Application();
        app.setUserId("offered");
        app.setPositionId(position.getId());
        app.setStatus(0); // Applied
        saveApplication(app);

        // Act
        boolean offered = moService.offerApplication(app.getId(), "Congratulations!");

        // Assert
        assertTrue(offered);

        Application updated = getApplicationById(app.getId());
        assertEquals(1, updated.getStatus()); // Status 1 = offered
        assertEquals("Congratulations!", updated.getFeedback());

        Position posUpdated = getPositionById(position.getId());
        assertEquals(1, posUpdated.getOfferedNum());
    }

    @Test
    public void testOfferApplicationAutoClosesPosition() {
        // Arrange
        Position position = new Position();
        position.setTitle("Closing Job");
        position.setPostUserId("closemgr");
        position.setRequiredNum(1);
        position.setOfferedNum(0);
        position.setStatus(0); // Open
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("lucky");
        profile.setName("Lucky Candidate");
        saveTAProfile(profile);

        Application app = new Application();
        app.setUserId("lucky");
        app.setPositionId(position.getId());
        app.setStatus(0);
        saveApplication(app);

        // Act
        moService.offerApplication(app.getId(), "You're hired!");

        // Assert
        Position updated = getPositionById(position.getId());
        assertEquals(1, updated.getOfferedNum());
        assertEquals(1, updated.getStatus()); // Status 1 = closed (filled)
    }

    @Test
    public void testOfferApplicationAlreadyProcessed() {
        // Arrange
        Position position = new Position();
        position.setTitle("Processed Job");
        position.setPostUserId("procmgr");
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("processed");
        profile.setName("Processed");
        saveTAProfile(profile);

        Application app = new Application();
        app.setUserId("processed");
        app.setPositionId(position.getId());
        app.setStatus(1); // Already offered
        saveApplication(app);

        // Act
        boolean offered = moService.offerApplication(app.getId(), "Again?");

        // Assert
        assertFalse(offered); // Should fail
    }

    @Test
    public void testOfferApplicationNonExistent() {
        // Act
        boolean offered = moService.offerApplication("nonexistent", "feedback");

        // Assert
        assertFalse(offered);
    }

    // ==================== REJECT APPLICATION TESTS ====================

    @Test
    public void testRejectApplicationSuccess() {
        // Arrange
        Position position = new Position();
        position.setTitle("Reject Job");
        position.setPostUserId("rejectmgr");
        position.setRejectedNum(0);
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("rejected");
        profile.setName("Rejected User");
        saveTAProfile(profile);

        Application app = new Application();
        app.setUserId("rejected");
        app.setPositionId(position.getId());
        app.setStatus(0); // Applied
        saveApplication(app);

        // Act
        boolean rejected = moService.rejectApplication(app.getId(), "Not a fit");

        // Assert
        assertTrue(rejected);

        Application updated = getApplicationById(app.getId());
        assertEquals(2, updated.getStatus()); // Status 2 = rejected
        assertEquals("Not a fit", updated.getFeedback());

        Position posUpdated = getPositionById(position.getId());
        assertEquals(1, posUpdated.getRejectedNum());
    }

    @Test
    public void testRejectApplicationAlreadyProcessed() {
        // Arrange
        Position position = new Position();
        position.setTitle("Already Rejected");
        position.setPostUserId("alrmgr");
        savePosition(position);

        TAProfile profile = new TAProfile();
        profile.setUserId("alreadyrej");
        profile.setName("Already");
        saveTAProfile(profile);

        Application app = new Application();
        app.setUserId("alreadyrej");
        app.setPositionId(position.getId());
        app.setStatus(2); // Already rejected
        saveApplication(app);

        // Act
        boolean rejected = moService.rejectApplication(app.getId(), "Again?");

        // Assert
        assertFalse(rejected);
    }

    @Test
    public void testRejectApplicationNonExistent() {
        // Act
        boolean rejected = moService.rejectApplication("nonexistent", "feedback");

        // Assert
        assertFalse(rejected);
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    public void testFullApplicationReviewWorkflow() {
        // 1. Manager creates position
        Position position = new Position();
        position.setTitle("Full Workflow Job");
        position.setPostUserId("workflowmgr");
        position.setRequiredNum(2);
        moService.createPosition(position);

        // 2. Verify ownership
        assertTrue(moService.verifyPositionOwner(position.getId(), "workflowmgr"));

        // 3. Create applicants
        for (int i = 0; i < 3; i++) {
            TAProfile profile = new TAProfile();
            profile.setUserId("wfapp" + i);
            profile.setName("Applicant " + i);
            saveTAProfile(profile);

            Application app = new Application();
            app.setUserId("wfapp" + i);
            app.setPositionId(position.getId());
            app.setStatus(0);
            saveApplication(app);
        }

        // 4. Get application list
        List<ApplicationDTO> apps = moService.getAppList(position.getId(), 1);
        assertEquals(3, apps.size());

        // 5. Offer first applicant
        assertTrue(moService.offerApplication(apps.get(0).getAppId(), "Offered"));

        // 6. Reject second applicant
        assertTrue(moService.rejectApplication(apps.get(1).getAppId(), "Rejected"));

        // 7. Verify counts
        Position updated = getPositionById(position.getId());
        assertEquals(1, updated.getOfferedNum());
        assertEquals(1, updated.getRejectedNum());
    }

    @Test
    public void testPositionLifecycleManagement() {
        // 1. Create position
        Position position = new Position();
        position.setTitle("Lifecycle Position");
        position.setPostUserId("lifemgr");
        position.setRequiredNum(1);
        moService.createPosition(position);

        // 2. Verify initial state
        assertEquals(0, position.getStatus()); // Open
        assertEquals(0, position.getAppliedNum());

        // 3. Update position details
        position.setTitle("Updated Lifecycle Position");
        position.setDescription("New description");
        moService.updatePosition(position);

        // 4. Retrieve and verify
        PosDetailDTO detail = moService.getPosition(position.getId());
        assertEquals("Updated Lifecycle Position", detail.getTitle());
        assertEquals("New description", detail.getDescription());

        // 5. Get position list
        List<PosBriefDTO> positions = moService.getPositionList("lifemgr", 1);
        assertEquals(1, positions.size());
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

    private void saveTAProfile(TAProfile profile) {
        try {
            var repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
            repo.saveEntity(profile);
        } catch (Exception e) {
            fail("Failed to save TA profile: " + e.getMessage());
        }
    }
}
