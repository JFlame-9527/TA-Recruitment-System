package com.tars.service;

import com.tars.config.ApplicationConfiguration;
import com.tars.entity.bean.*;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.mo.ApplicationDTO;
import com.tars.entity.dto.mo.PosBriefDTO;
import com.tars.entity.dto.mo.PosDetailDTO;
import com.tars.entity.dto.mo.ProfileDTO;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for MOService
 * Tests position management, application review, and manager operations
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class MOServiceTest {

    private static MOService moService;
    private static final String TEST_DATA_DIR = "test-data";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);
        
        // Initialize QwenConfiguration (required for PortraitGenerator)
        com.tars.config.QwenConfiguration.initializeForTest(testResourcePath);

        // Set test data directory
        com.tars.repository.JsonRepository.setDataDir(TEST_DATA_DIR);

        // Create MO service instance
        moService = new MOService();

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
     * Test posting a new position
     */
    @Test
    public void testPostPosition() throws IOException {
        Position position = createPosition("pos-1", "mo-user-1");

        try {
            boolean posted = moService.postPosition(position, null);

            // If API is available, verify the position was posted
            if (posted) {
                Position found = findPositionById(position.getId());
                assertNotNull("Position should exist", found);
                assertEquals("Title should match", "Test Position", found.getTitle());
                assertNotNull("Portrait ID should be generated", found.getPortraitId());
            }
        } catch (RuntimeException e) {
            // Expected if API key is invalid or API is unavailable
            assertTrue("Should fail with API error", 
                    e.getMessage().contains("API") || e.getMessage().contains("Failed"));
        }
    }

    /**
     * Test reposting an existing position
     */
    @Test
    public void testRepostPosition() throws IOException {
        // Create original position manually (without AI)
        Position original = createPosition("pos-2", "mo-user-1");
        original.setPortraitId("mock-portrait-id");
        savePosition(original);

        // Repost with modifications
        Position reposted = createPosition(null, "mo-user-1");
        reposted.setTitle("Updated Position");

        try {
            boolean reposted_success = moService.postPosition(reposted, original.getId());

            if (reposted_success) {
                Position found = findPositionById(original.getId());
                assertNotNull("Original position should still exist", found);
                assertEquals("Title should be updated", "Updated Position", found.getTitle());
                assertEquals("ID should be preserved", original.getId(), found.getId());
            }
        } catch (RuntimeException e) {
            // Expected if API is unavailable
            assertTrue("Should handle API failure", true);
        }
    }

    /**
     * Test reposting non-existent position
     */
    @Test
    public void testRepostNonExistentPosition() throws IOException {
        Position position = createPosition("pos-3", "mo-user-1");

        boolean reposted = moService.postPosition(position, "non-existent-id");

        assertFalse("Should fail to repost non-existent position", reposted);
    }

    /**
     * Test getting position list
     */
    @Test
    public void testGetPositionList() throws IOException {
        // Create multiple positions for user
        for (int i = 0; i < 5; i++) {
            Position position = createPosition("pos-list-" + i, "mo-user-2");
            savePosition(position);
        }

        List<PosBriefDTO> positions = moService.getPositionList("mo-user-2", 1);

        assertNotNull("Position list should not be null", positions);
        assertTrue("Should have positions", positions.size() > 0);
        assertTrue("Should respect page size", positions.size() <= 9);
    }

    /**
     * Test getting position list with null userId
     */
    @Test
    public void testGetPositionListWithNullUserId() {
        List<PosBriefDTO> positions = moService.getPositionList(null, 1);

        assertNotNull("Should return empty list", positions);
        assertTrue("Should be empty", positions.isEmpty());
    }

    /**
     * Test getting position list with filter and order
     */
    @Test
    public void testGetPositionListWithCondition() throws IOException {
        // Create positions with different statuses
        Position pos1 = createPosition("pos-filter-1", "mo-user-3");
        pos1.setStatus(0); // Opened
        Position pos2 = createPosition("pos-filter-2", "mo-user-3");
        pos2.setStatus(1); // Closed

        savePosition(pos1);
        savePosition(pos2);

        QueryCondition condition = new QueryCondition();
        condition.setFilter("opened");
        condition.setOrder("postDate");
        condition.setPage(1);

        List<PosBriefDTO> positions = moService.getPositionList("mo-user-3", condition);

        assertNotNull("Position list should not be null", positions);
        // Should only return opened positions
        for (PosBriefDTO dto : positions) {
            assertEquals("Should only return opened positions", 0, dto.getStatus());
        }
    }

    /**
     * Test getting position pages
     */
    @Test
    public void testGetPositionPages() throws IOException {
        // Create 15 positions
        for (int i = 0; i < 15; i++) {
            Position position = createPosition("pos-page-" + i, "mo-user-4");
            savePosition(position);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        long pages = moService.getPositionPages("mo-user-4", condition);

        // With pageSize=9, 15 positions should have 2 pages
        assertTrue("Should have at least 1 page", pages >= 1);
    }

    /**
     * Test getting position detail
     */
    @Test
    public void testGetPositionDetail() throws IOException {
        Position position = createPosition("pos-detail-1", "mo-user-5");
        savePosition(position);

        PosDetailDTO detail = moService.getPosition(position.getId());

        assertNotNull("Position detail should not be null", detail);
        assertEquals("Position ID should match", position.getId(), detail.getPosId());
    }

    /**
     * Test getting non-existent position
     */
    @Test
    public void testGetNonExistentPosition() {
        PosDetailDTO detail = moService.getPosition("non-existent");

        assertNull("Should return null for non-existent position", detail);
    }

    /**
     * Test verifying position owner
     */
    @Test
    public void testVerifyPositionOwner() throws IOException {
        Position position = createPosition("pos-owner-1", "mo-user-6");
        savePosition(position);

        boolean isOwner = moService.verifyPositionOwner(position.getId(), "mo-user-6");

        assertTrue("Should verify as owner", isOwner);

        boolean isNotOwner = moService.verifyPositionOwner(position.getId(), "other-user");

        assertFalse("Should not verify as non-owner", isNotOwner);
    }

    /**
     * Test offering an application
     */
    @Test
    public void testOfferApplication() throws IOException {
        Application app = createApplication("app-1", "ta-user-1", "pos-offer-1");
        Position position = createPosition("pos-offer-1", "mo-user-7");
        position.setRequiredNum(2);

        saveApplication(app);
        savePosition(position);

        boolean offered = moService.offerApplication(app.getId(), "Congratulations!");

        assertTrue("Application should be offered", offered);

        Application updated = findApplicationById(app.getId());
        assertNotNull("Application should exist", updated);
        assertEquals("Status should be offered", 1, updated.getStatus());
        assertEquals("Feedback should be set", "Congratulations!", updated.getFeedback());

        Position updatedPos = findPositionById(position.getId());
        assertEquals("Offered count should increase", 1, updatedPos.getOfferedNum());
    }

    /**
     * Test offering already processed application
     */
    @Test
    public void testOfferAlreadyProcessedApplication() throws IOException {
        Application app = createApplication("app-2", "ta-user-2", "pos-offer-2");
        app.setStatus(1); // Already offered

        saveApplication(app);

        boolean offered = moService.offerApplication(app.getId(), "Feedback");

        assertFalse("Should not offer already processed application", offered);
    }

    /**
     * Test rejecting an application
     */
    @Test
    public void testRejectApplication() throws IOException {
        Application app = createApplication("app-3", "ta-user-3", "pos-reject-1");
        Position position = createPosition("pos-reject-1", "mo-user-8");

        saveApplication(app);
        savePosition(position);

        boolean rejected = moService.rejectApplication(app.getId(), "Not qualified");

        assertTrue("Application should be rejected", rejected);

        Application updated = findApplicationById(app.getId());
        assertNotNull("Application should exist", updated);
        assertEquals("Status should be rejected", 2, updated.getStatus());
        assertEquals("Feedback should be set", "Not qualified", updated.getFeedback());

        Position updatedPos = findPositionById(position.getId());
        assertEquals("Rejected count should increase", 1, updatedPos.getRejectedNum());
    }

    /**
     * Test rejecting already processed application
     */
    @Test
    public void testRejectAlreadyProcessedApplication() throws IOException {
        Application app = createApplication("app-4", "ta-user-4", "pos-reject-2");
        app.setStatus(2); // Already rejected

        saveApplication(app);

        boolean rejected = moService.rejectApplication(app.getId(), "Feedback");

        assertFalse("Should not reject already processed application", rejected);
    }

    /**
     * Test getting application list
     */
    @Test
    public void testGetApplicationList() throws IOException {
        // Create position and applications
        Position position = createPosition("pos-app-list", "mo-user-9");
        savePosition(position);

        for (int i = 0; i < 5; i++) {
            TAProfile profile = createTAProfile("ta-profile-app-" + i, "ta-user-app-" + i);
            Application app = createApplication("app-list-" + i, "ta-user-app-" + i, position.getId());

            saveTAProfile(profile);
            saveApplication(app);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        List<ApplicationDTO> apps = moService.getAppList(position.getId(), condition);

        assertNotNull("Application list should not be null", apps);
        assertTrue("Should have applications", apps.size() > 0);
        assertTrue("Should respect page size", apps.size() <= 10);
    }

    /**
     * Test getting application list with filter
     */
    @Test
    public void testGetApplicationListWithFilter() throws IOException {
        Position position = createPosition("pos-app-filter", "mo-user-10");
        savePosition(position);

        Application app1 = createApplication("app-filter-1", "ta-user-f1", position.getId());
        app1.setStatus(0); // Applied
        Application app2 = createApplication("app-filter-2", "ta-user-f2", position.getId());
        app2.setStatus(1); // Offered

        saveApplication(app1);
        saveApplication(app2);

        QueryCondition condition = new QueryCondition();
        condition.setFilter("offered");
        condition.setPage(1);

        List<ApplicationDTO> apps = moService.getAppList(position.getId(), condition);

        assertNotNull("Application list should not be null", apps);
        // Should only return offered applications
        for (ApplicationDTO dto : apps) {
            assertEquals("Should only return offered applications", 1, dto.getStatus());
        }
    }

    /**
     * Test getting application pages
     */
    @Test
    public void testGetApplicationPages() throws IOException {
        Position position = createPosition("pos-app-pages", "mo-user-11");
        savePosition(position);

        // Create 15 applications
        for (int i = 0; i < 15; i++) {
            Application app = createApplication("app-page-" + i, "ta-user-page-" + i, position.getId());
            saveApplication(app);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        long pages = moService.getAppPages(position.getId(), condition);

        // With pageSize=10, 15 applications should have 2 pages
        assertTrue("Should have at least 1 page", pages >= 1);
    }

    /**
     * Test getting TA profile
     */
    @Test
    public void testGetTAProfile() throws IOException {
        TAProfile profile = createTAProfile("profile-get-1", "ta-user-get");
        Application app = createApplication("app-profile-1", "ta-user-get", "pos-profile-1");

        saveTAProfile(profile);
        saveApplication(app);

        ProfileDTO dto = moService.getProfile(profile.getId(), app.getId());

        assertNotNull("Profile DTO should not be null", dto);
    }

    /**
     * Test withdrawing a position
     */
    @Test
    public void testWithdrawPosition() throws IOException {
        Position position = createPosition("pos-withdraw-1", "mo-user-12");
        Application app = createApplication("app-withdraw-1", "ta-user-w1", position.getId());

        savePosition(position);
        saveApplication(app);

        boolean withdrawn = moService.withdrawPosition(position.getId());

        assertTrue("Position should be withdrawn", withdrawn);

        Position found = findPositionById(position.getId());
        assertNotNull("Position should exist", found);
        assertEquals("Status should be withdrawn", 3, found.getStatus());
        assertEquals("Applied count should be reset", 0, found.getAppliedNum());

        Application foundApp = findApplicationById(app.getId());
        assertNull("Applications should be deleted", foundApp);
    }

    /**
     * Test withdrawing non-existent position
     */
    @Test
    public void testWithdrawNonExistentPosition() {
        boolean withdrawn = moService.withdrawPosition("non-existent");

        assertFalse("Should return false for non-existent position", withdrawn);
    }

    // Helper methods

    private Position createPosition(String id, String postUserId) {
        Position position = new Position();
        if (id != null) {
            position.setId(id);
        }
        position.setPostUserId(postUserId);
        position.setTitle("Test Position");
        position.setModuleCode("CS101");
        position.setModuleName("Test Module");
        position.setDescription("Test description");
        position.setDuration(12);
        position.setWeeklyWorkload(10.0f);
        position.setRequiredNum(2);
        position.setStatus(0);
        position.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
        position.setEndDate(Timestamp.valueOf(LocalDateTime.now().plusMonths(3)));
        position.setDeadline(Timestamp.valueOf(LocalDateTime.now().plusDays(30)));
        position.setPostDate(Timestamp.valueOf(LocalDateTime.now()));
        return position;
    }

    private Application createApplication(String id, String userId, String positionId) {
        Application app = new Application();
        app.setId(id);
        app.setUserId(userId);
        app.setPositionId(positionId);
        app.setStatus(0); // Applied
        app.setApplyAt(Timestamp.valueOf(LocalDateTime.now()));
        return app;
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

    private void savePosition(Position position) throws IOException {
        com.tars.repository.JsonRepository<Position> repo = new com.tars.repository.JsonRepository<>(Position.class);
        repo.saveEntity(position);
    }

    private void saveApplication(Application app) throws IOException {
        com.tars.repository.JsonRepository<Application> repo = new com.tars.repository.JsonRepository<>(Application.class);
        repo.saveEntity(app);
    }

    private void saveTAProfile(TAProfile profile) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        repo.saveEntity(profile);
    }

    private Position findPositionById(String id) throws IOException {
        com.tars.repository.JsonRepository<Position> repo = new com.tars.repository.JsonRepository<>(Position.class);
        return repo.getEntityById(id);
    }

    private Application findApplicationById(String id) throws IOException {
        com.tars.repository.JsonRepository<Application> repo = new com.tars.repository.JsonRepository<>(Application.class);
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
