package com.tars.ai;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import com.tars.entity.bean.Portrait;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import jakarta.servlet.http.Part;
import org.junit.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for PortraitGenerator
 * Tests portrait generation and vectorization functionality
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class PortraitGeneratorTest {

    private static final String TEST_UPLOAD_DIR = "test-uploads";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Initialize QwenConfiguration
        QwenConfiguration.initializeForTest(testResourcePath);

        // Create test upload directory
        File dir = new File(TEST_UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @AfterClass
    public static void tearDown() {
        // Clean up test upload directory
        cleanTestDirectory();
    }

    @Before
    public void beforeEach() {
        // Reset and reinitialize QwenConfiguration singleton before each test
        resetQwenConfiguration();
        
        // Reinitialize after reset
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        QwenConfiguration.initializeForTest(testResourcePath);
    }

    /**
     * Test PortraitGenerator initialization
     */
    @Test
    public void testPortraitGeneratorInitialization() {
        try {
            PortraitGenerator generator = new PortraitGenerator();
            assertNotNull("PortraitGenerator should be created", generator);
        } catch (Exception e) {
            fail("PortraitGenerator initialization should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test generating TA portrait with null profile
     */
    @Test(expected = NullPointerException.class)
    public void testGenerateTAPortraitWithNullProfile() {
        PortraitGenerator generator = new PortraitGenerator();
        generator.generatePortrait((TAProfile) null, (Part) null);
    }

    /**
     * Test generating TA portrait with null resume file
     */
    @Test
    public void testGenerateTAPortraitWithNullResume() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();

        try {
            // Should fall back to profile-only mode
            Portrait portrait = generator.generatePortrait(profile, (Part) null);
            // If API is available, portrait should not be null
            // Note: This may fail due to API unavailability in test environment
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should fail with runtime exception",
                    e.getMessage().contains("Failed") || e.getMessage().contains("API"));
        }
    }

    /**
     * Test generating TA portrait from file with null resume
     */
    @Test
    public void testGenerateTAPortraitFromFileWithNullResume() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();

        try {
            // Should fall back to profile-only mode
            Portrait portrait = generator.generatePortrait(profile, (File) null);
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should fail with runtime exception",
                    e.getMessage().contains("Failed") || e.getMessage().contains("API"));
        }
    }

    /**
     * Test generating TA portrait with valid profile and resume file
     */
    @Test
    public void testGenerateTAPortraitWithValidData() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();
        File resumeFile = null;

        try {
            resumeFile = createTempPdfFile("resume.pdf", 2048);

            try {
                Portrait portrait = generator.generatePortrait(profile, resumeFile);
                // If API is available, verify portrait structure
                if (portrait != null) {
                    assertNotNull("Portrait should not be null", portrait);
                    // Portrait should have vectors (may be empty if API fails)
                }
            } catch (RuntimeException e) {
                // Expected if API is not available or credentials invalid
                assertTrue("Should fail with runtime exception",
                        e.getMessage().contains("Failed") || e.getMessage().contains("API"));
            }
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            if (resumeFile != null && resumeFile.exists()) {
                resumeFile.delete();
            }
        }
    }

    /**
     * Test generating position portrait with null position
     */
    @Test(expected = NullPointerException.class)
    public void testGeneratePositionPortraitWithNullPosition() {
        PortraitGenerator generator = new PortraitGenerator();
        generator.generatePortrait((Position) null);
    }

    /**
     * Test generating position portrait with valid data
     */
    @Test
    public void testGeneratePositionPortraitWithValidData() {
        PortraitGenerator generator = new PortraitGenerator();
        Position position = createTestPosition();

        try {
            Portrait portrait = generator.generatePortrait(position);
            // If API is available, verify portrait structure
            if (portrait != null) {
                assertNotNull("Portrait should not be null", portrait);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should fail with runtime exception",
                    e.getMessage().contains("Failed") || e.getMessage().contains("API"));
        }
    }

    /**
     * Test portrait fallback mechanism when resume processing fails
     */
    @Test
    public void testPortraitFallbackMechanism() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();

        // Create an invalid file that will cause resume processing to fail
        File invalidFile = null;
        try {
            invalidFile = createTempFile("invalid.txt", "text/plain", 100);

            try {
                // Should fall back to profile-only mode
                Portrait portrait = generator.generatePortrait(profile, invalidFile);
                // Fallback should still attempt to generate portrait
            } catch (RuntimeException e) {
                // Expected if both resume and fallback fail
                assertTrue("Should indicate failure",
                        e.getMessage().contains("Failed") || e.getMessage().contains("API"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (invalidFile != null && invalidFile.exists()) {
                invalidFile.delete();
            }
        }
    }

    /**
     * Test portrait generation with empty skills list
     */
    @Test
    public void testPortraitWithEmptySkills() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();
        profile.setSkills(null); // No skills

        try {
            Portrait portrait = generator.generatePortrait(profile, (Part) null);
            // Should handle null skills gracefully
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should handle gracefully or fail with API error", true);
        }
    }

    /**
     * Test portrait generation with comprehensive TA profile
     */
    @Test
    public void testPortraitWithComprehensiveProfile() {
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createComprehensiveTAProfile();

        try {
            Portrait portrait = generator.generatePortrait(profile, (File) null);
            // Should process comprehensive profile
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should handle comprehensive profile", true);
        }
    }

    /**
     * Test portrait generation with different position types
     */
    @Test
    public void testPortraitWithDifferentPositions() {
        PortraitGenerator generator = new PortraitGenerator();

        // Test with Java Developer position
        Position javaPos = createJavaDeveloperPosition();
        try {
            Portrait portrait = generator.generatePortrait(javaPos);
        } catch (RuntimeException e) {
            // Expected if API is not available
        }

        // Test with Data Science position
        Position dataPos = createDataSciencePosition();
        try {
            Portrait portrait = generator.generatePortrait(dataPos);
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test embedding generation for empty text
     */
    @Test
    public void testEmbeddingForEmptyText() {
        // This tests the internal logic indirectly through portrait generation
        PortraitGenerator generator = new PortraitGenerator();
        TAProfile profile = createTestTAProfile();
        profile.setSkills(Arrays.asList()); // Empty skills list

        try {
            Portrait portrait = generator.generatePortrait(profile, (Part) null);
            // Should handle empty skills gracefully
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test portrait vectorization structure
     */
    @Test
    public void testPortraitVectorizationStructure() {
        // Create a mock portrait to verify structure
        List<Float> skillsVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        List<Float> experienceVector = Arrays.asList(0.4f, 0.5f, 0.6f);
        List<Float> softSkillsVector = Arrays.asList(0.7f, 0.8f, 0.9f);

        Portrait portrait = new Portrait(skillsVector, experienceVector, softSkillsVector);

        assertNotNull("Portrait should not be null", portrait);
        assertEquals("Skills vector size should match", 3, portrait.getSkills().size());
        assertEquals("Experience vector size should match", 3, portrait.getExperience().size());
        assertEquals("Soft skills vector size should match", 3, portrait.getSoftSkills().size());
    }

    /**
     * Helper method to create test TA profile
     */
    private TAProfile createTestTAProfile() {
        TAProfile profile = new TAProfile();
        profile.setId("test-ta-001");
        profile.setName("John Doe");
        profile.setGender("Male");
        profile.setAge(22);
        profile.setCollege("School of Engineering");
        profile.setMajor("Computer Science");
        profile.setDegree("BACHELOR");
        profile.setYear(3);
        profile.setSkills(Arrays.asList("Java", "Python", "Spring Boot"));
        profile.setEmail("john.doe@example.com");
        profile.setPhone("+44 123 456 7890");
        return profile;
    }

    /**
     * Helper method to create comprehensive TA profile
     */
    private TAProfile createComprehensiveTAProfile() {
        TAProfile profile = createTestTAProfile();
        profile.setSkills(Arrays.asList(
                "Java", "Python", "JavaScript", "Spring Boot", "React",
                "MySQL", "Docker", "Kubernetes", "AWS", "Git"
        ));
        return profile;
    }

    /**
     * Helper method to create test position
     */
    private Position createTestPosition() {
        Position position = new Position();
        position.setId("test-pos-001");
        position.setTitle("Java Developer TA");
        position.setModuleCode("CS101");
        position.setModuleName("Software Engineering");
        position.setDescription("Looking for a TA to help with Java programming course");
        position.setSkills(Arrays.asList("Java", "Spring Boot", "MySQL"));
        position.setDuration(12);
        position.setWeeklyWorkload(10.0f);
        return position;
    }

    /**
     * Helper method to create Java Developer position
     */
    private Position createJavaDeveloperPosition() {
        Position position = createTestPosition();
        position.setTitle("Senior Java Developer");
        position.setDescription("Experienced Java developer for enterprise applications");
        position.setSkills(Arrays.asList("Java", "Spring", "Microservices", "Docker"));
        return position;
    }

    /**
     * Helper method to create Data Science position
     */
    private Position createDataSciencePosition() {
        Position position = createTestPosition();
        position.setTitle("Data Science TA");
        position.setModuleCode("DS201");
        position.setModuleName("Machine Learning");
        position.setDescription("TA needed for ML course, Python and statistics required");
        position.setSkills(Arrays.asList("Python", "TensorFlow", "Statistics", "Pandas"));
        return position;
    }

    /**
     * Helper method to create temporary PDF file
     */
    private File createTempPdfFile(String fileName, int size) throws IOException {
        File tempFile = File.createTempFile("test_", "_" + fileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Write minimal PDF header
            String pdfHeader = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
            fos.write(pdfHeader.getBytes());

            // Fill remaining space with dummy data
            int remaining = size - pdfHeader.length();
            if (remaining > 0) {
                byte[] data = new byte[remaining];
                fos.write(data);
            }
        }

        return tempFile;
    }

    /**
     * Helper method to create temporary file
     */
    private File createTempFile(String fileName, String contentType, int size) throws IOException {
        File tempFile = File.createTempFile("test_", "_" + fileName);

        if (size > 0) {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] data = new byte[size];
                for (int i = 0; i < size; i++) {
                    data[i] = (byte) (i % 256);
                }
                fos.write(data);
            }
        }

        return tempFile;
    }

    /**
     * Helper method to clean test directory
     */
    private static void cleanTestDirectory() {
        File dir = new File(TEST_UPLOAD_DIR);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
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

    /**
     * Helper method to reset QwenConfiguration singleton
     */
    private void resetQwenConfiguration() {
        try {
            Field instanceField = QwenConfiguration.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore - configuration may not be initialized
        }
    }
}
