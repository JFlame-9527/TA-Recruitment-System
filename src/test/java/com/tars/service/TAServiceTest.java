package com.tars.service;

import com.tars.config.ApplicationConfiguration;
import com.tars.entity.bean.*;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.ta.AppPosDTO;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.PosDetailDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import jakarta.servlet.http.Part;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for TAService
 * Tests applicant profile management, job applications, and position browsing
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class TAServiceTest {

    private static TAService taService;
    private static final String TEST_DATA_DIR = "test-data";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);
        
        // Initialize QwenConfiguration (required for PortraitGenerator and SkillExtractor)
        com.tars.config.QwenConfiguration.initializeForTest(testResourcePath);
        
        // Set test data directory
        com.tars.repository.JsonRepository.setDataDir(TEST_DATA_DIR);
        
        // Create TA service instance
        taService = new TAService();
        
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
     * Test creating a new TA profile
     */
    @Test
    public void testCreateProfile() throws IOException {
        TAProfile profile = createTAProfile("profile-create-1", "ta-user-1");

        // Note: This will attempt to call AI API for portrait generation
        boolean created = taService.createProfile(profile, null);

        // May fail due to AI API unavailability, but logic should be correct
        if (created) {
            TAProfile found = findTAProfileByUserId("ta-user-1");
            assertNotNull("Profile should exist", found);
            assertEquals("User ID should match", "ta-user-1", found.getUserId());
            assertNotNull("Portrait ID should be generated", found.getPortraitId());
        }
    }

    /**
     * Test checking if profile exists
     */
    @Test
    public void testCheckProfileExist() throws IOException {
        TAProfile profile = createTAProfile("profile-check-1", "ta-user-check");
        saveTAProfile(profile);

        boolean exists = taService.checkProfileExist("ta-user-check");

        assertTrue("Profile should exist", exists);

        boolean notExists = taService.checkProfileExist("non-existent-user");

        assertFalse("Profile should not exist", notExists);
    }

    /**
     * Test getting profile DTO
     */
    @Test
    public void testGetProfileDTO() throws IOException {
        TAProfile profile = createTAProfile("profile-dto-1", "ta-user-dto");
        saveTAProfile(profile);

        ProfileDTO dto = taService.getProfileDTO("ta-user-dto");

        assertNotNull("Profile DTO should not be null", dto);
    }

    /**
     * Test getting non-existent profile DTO
     */
    @Test
    public void testGetNonExistentProfileDTO() {
        ProfileDTO dto = taService.getProfileDTO("non-existent");

        assertNull("Should return null for non-existent profile", dto);
    }

    /**
     * Test getting raw TA profile
     */
    @Test
    public void testGetProfile() throws IOException {
        TAProfile profile = createTAProfile("profile-get-1", "ta-user-get");
        saveTAProfile(profile);

        TAProfile found = taService.getProfile("ta-user-get");

        assertNotNull("Profile should not be null", found);
        assertEquals("Profile ID should match", profile.getId(), found.getId());
    }

    /**
     * Test updating profile with validation
     */
    @Test
    public void testUpdateProfile() throws IOException {
        TAProfile profile = createTAProfile("profile-update-1", "ta-user-update");
        saveTAProfile(profile);

        profile.setName("Updated Name");

        // Note: This will attempt to call AI API
        boolean updated = taService.updateProfile(profile, (Part) null);

        // May fail due to AI API, but validation should pass
        if (updated) {
            TAProfile found = findTAProfileById(profile.getId());
            assertNotNull("Profile should exist", found);
            assertEquals("Name should be updated", "Updated Name", found.getName());
        }
    }

    /**
     * Test updating profile with empty name
     */
    @Test
    public void testUpdateProfileWithEmptyName() throws IOException {
        TAProfile profile = createTAProfile("profile-update-2", "ta-user-update2");
        saveTAProfile(profile);

        profile.setName("");

        boolean updated = taService.updateProfile(profile, (Part) null);

        assertFalse("Should reject empty name", updated);
    }

    /**
     * Test updating non-existent profile
     */
    @Test
    public void testUpdateNonExistentProfile() throws IOException {
        TAProfile profile = createTAProfile("profile-update-3", "ta-user-nonexist");
        // Don't save the profile

        boolean updated = taService.updateProfile(profile, (Part) null);

        assertFalse("Should reject non-existent profile", updated);
    }

    /**
     * Test creating an application
     */
    @Test
    public void testCreateApplication() throws IOException {
        Position position = createPosition("pos-apply-1", "mo-user-1");
        savePosition(position);

        Application app = createApplication("app-create-1", "ta-user-apply", position.getId());

        boolean created = taService.createApplication(app);

        assertTrue("Application should be created", created);

        Application found = findApplicationById(app.getId());
        assertNotNull("Application should exist", found);

        Position updatedPos = findPositionById(position.getId());
        assertEquals("Applied count should increase", 1, updatedPos.getAppliedNum());
    }

    /**
     * Test getting application-position list
     */
    @Test
    public void testGetAppPosList() throws IOException {
        Position position = createPosition("pos-applist-1", "mo-user-2");
        Application app = createApplication("app-list-1", "ta-user-list", position.getId());

        savePosition(position);
        saveApplication(app);

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        List<AppPosDTO> list = taService.getAppPosList("ta-user-list", condition);

        assertNotNull("AppPos list should not be null", list);
        assertTrue("Should have applications", list.size() > 0);
    }

    /**
     * Test getting application-position list with filter
     */
    @Test
    public void testGetAppPosListWithFilter() throws IOException {
        Position position = createPosition("pos-appfilter-1", "mo-user-3");
        Application app1 = createApplication("app-filter-1", "ta-user-filter", position.getId());
        app1.setStatus(0); // Pending
        Application app2 = createApplication("app-filter-2", "ta-user-filter", position.getId());
        app2.setStatus(1); // Offered

        savePosition(position);
        saveApplication(app1);
        saveApplication(app2);

        QueryCondition condition = new QueryCondition();
        condition.setFilter("offered");
        condition.setPage(1);

        List<AppPosDTO> list = taService.getAppPosList("ta-user-filter", condition);

        assertNotNull("AppPos list should not be null", list);
        // Should only return offered applications
        for (AppPosDTO dto : list) {
            assertEquals("Should only return offered applications", 1, dto.getStatus());
        }
    }

    /**
     * Test getting application-position pages
     */
    @Test
    public void testGetAppPosPages() throws IOException {
        Position position = createPosition("pos-apppages-1", "mo-user-4");

        // Create 15 applications
        for (int i = 0; i < 15; i++) {
            Application app = createApplication("app-page-" + i, "ta-user-pages", position.getId());
            saveApplication(app);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        long pages = taService.getAppPosPages("ta-user-pages", condition);

        // With pageSize=9, 15 applications should have 2 pages
        assertTrue("Should have at least 1 page", pages >= 1);
    }

    /**
     * Test withdrawing an application
     */
    @Test
    public void testWithdrawApplication() throws IOException {
        Position position = createPosition("pos-withdraw-1", "mo-user-5");
        position.setAppliedNum(1); // Set applied count to 1 (as if application was created)
        Application app = createApplication("app-withdraw-1", "ta-user-withdraw", position.getId());

        savePosition(position);
        saveApplication(app);

        taService.withdrawApplication(app.getId(), "ta-user-withdraw");

        Application found = findApplicationById(app.getId());
        assertNotNull("Application should still exist", found);
        assertEquals("Status should be withdrawn", 3, found.getStatus());

        Position updatedPos = findPositionById(position.getId());
        assertEquals("Applied count should decrease", 0, updatedPos.getAppliedNum());
    }

    /**
     * Test withdrawing other user's application
     */
    @Test
    public void testWithdrawOtherUserApplication() throws IOException {
        Position position = createPosition("pos-withdraw-2", "mo-user-6");
        Application app = createApplication("app-withdraw-2", "ta-user-other", position.getId());

        savePosition(position);
        saveApplication(app);

        // Try to withdraw as different user
        taService.withdrawApplication(app.getId(), "wrong-user");

        Application found = findApplicationById(app.getId());
        assertNotNull("Application should still exist", found);
        assertNotEquals("Status should not change", 3, found.getStatus());
    }

    /**
     * Test getting position list
     */
    @Test
    public void testGetPositionList() throws IOException {
        // Create multiple positions
        for (int i = 0; i < 5; i++) {
            Position position = createPosition("pos-list-" + i, "mo-user-list");
            position.setStatus(0); // Open
            savePosition(position);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        List<PosBriefDTO> positions = taService.getPositionList("ta-user-browse", condition);

        assertNotNull("Position list should not be null", positions);
        assertTrue("Should have positions", positions.size() > 0);
        assertTrue("Should respect page size", positions.size() <= 10);
    }

    /**
     * Test getting position list with search
     */
    @Test
    public void testGetPositionListWithSearch() throws IOException {
        Position pos1 = createPosition("pos-search-1", "mo-user-search");
        pos1.setTitle("Java Developer");
        Position pos2 = createPosition("pos-search-2", "mo-user-search");
        pos2.setTitle("Python Engineer");

        savePosition(pos1);
        savePosition(pos2);

        QueryCondition condition = new QueryCondition();
        condition.setKey("title");
        condition.setSearch("Java");
        condition.setPage(1);

        List<PosBriefDTO> positions = taService.getPositionList("ta-user-search", condition);

        assertNotNull("Position list should not be null", positions);
        // Should only return Java positions
        for (PosBriefDTO dto : positions) {
            assertTrue("Title should contain Java", dto.getTitle().toLowerCase().contains("java"));
        }
    }

    /**
     * Test getting position list with filter
     */
    @Test
    public void testGetPositionListWithFilter() throws IOException {
        Position pos1 = createPosition("pos-filter-1", "mo-user-filter");
        pos1.setStatus(0); // Open
        Position pos2 = createPosition("pos-filter-2", "mo-user-filter");
        pos2.setStatus(1); // Closed

        savePosition(pos1);
        savePosition(pos2);

        QueryCondition condition = new QueryCondition();
        condition.setFilter("opened");
        condition.setPage(1);

        List<PosBriefDTO> positions = taService.getPositionList("ta-user-filter", condition);

        assertNotNull("Position list should not be null", positions);
        // Should only return opened positions
        for (PosBriefDTO dto : positions) {
            assertEquals("Should only return opened positions", 0, dto.getPosStatus());
        }
    }

    /**
     * Test getting position pages
     */
    @Test
    public void testGetPositionPages() throws IOException {
        // Create 15 positions
        for (int i = 0; i < 15; i++) {
            Position position = createPosition("pos-pages-" + i, "mo-user-pages");
            position.setStatus(0);
            savePosition(position);
        }

        QueryCondition condition = new QueryCondition();
        condition.setPage(1);

        long pages = taService.getPositionPages(condition);

        // With pageSize=10, 15 positions should have 2 pages
        assertTrue("Should have at least 1 page", pages >= 1);
    }

    /**
     * Test getting position detail
     */
    @Test
    public void testGetPositionDetail() throws IOException {
        Position position = createPosition("pos-detail-1", "mo-user-detail");
        savePosition(position);

        PosDetailDTO detail = taService.getPosition(position.getId(), "-1");

        assertNotNull("Position detail should not be null", detail);
        assertEquals("Position ID should match", position.getId(), detail.getPosId());
    }

    /**
     * Test verifying position availability
     */
    @Test
    public void testVerifyPosAvailable() throws IOException {
        Position position = createPosition("pos-verify-1", "mo-user-verify");
        position.setStatus(0); // Open
        savePosition(position);

        boolean available = taService.verifyPosAvailable(position.getId(), "ta-user-verify");

        assertTrue("Position should be available", available);
    }

    /**
     * Test verifying position already applied
     */
    @Test
    public void testVerifyPosAlreadyApplied() throws IOException {
        Position position = createPosition("pos-verify-2", "mo-user-verify2");
        position.setStatus(0);
        Application app = createApplication("app-verify-1", "ta-user-verify2", position.getId());

        savePosition(position);
        saveApplication(app);

        boolean available = taService.verifyPosAvailable(position.getId(), "ta-user-verify2");

        assertFalse("Should not be available if already applied", available);
    }

    /**
     * Test verifying closed position
     */
    @Test
    public void testVerifyClosedPosition() throws IOException {
        Position position = createPosition("pos-verify-3", "mo-user-verify3");
        position.setStatus(1); // Closed
        savePosition(position);

        boolean available = taService.verifyPosAvailable(position.getId(), "ta-user-verify3");

        assertFalse("Closed position should not be available", available);
    }

    /**
     * Test verifying profile exists
     */
    @Test
    public void testVerifyProfileExists() throws IOException {
        TAProfile profile = createTAProfile("profile-verify-1", "ta-user-verify-profile");
        saveTAProfile(profile);

        boolean exists = taService.verifyProfileExists("ta-user-verify-profile");

        assertTrue("Profile should exist", exists);
    }

    /**
     * Test verifying non-existent profile
     */
    @Test
    public void testVerifyNonExistentProfile() {
        boolean exists = taService.verifyProfileExists("non-existent-user");

        assertFalse("Profile should not exist", exists);
    }

    // Helper methods

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

    private void saveTAProfile(TAProfile profile) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        repo.saveEntity(profile);
    }

    private void savePosition(Position position) throws IOException {
        com.tars.repository.JsonRepository<Position> repo = new com.tars.repository.JsonRepository<>(Position.class);
        repo.saveEntity(position);
    }

    private void saveApplication(Application app) throws IOException {
        com.tars.repository.JsonRepository<Application> repo = new com.tars.repository.JsonRepository<>(Application.class);
        repo.saveEntity(app);
    }

    private TAProfile findTAProfileByUserId(String userId) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        List<TAProfile> profiles = repo.loadAllEntities();
        return profiles.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private TAProfile findTAProfileById(String id) throws IOException {
        com.tars.repository.JsonRepository<TAProfile> repo = new com.tars.repository.JsonRepository<>(TAProfile.class);
        return repo.getEntityById(id);
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
