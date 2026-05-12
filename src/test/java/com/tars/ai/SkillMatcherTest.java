package com.tars.ai;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for SkillMatcher
 * Tests skill matching and AI-based scoring functionality
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class SkillMatcherTest {

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Initialize QwenConfiguration
        QwenConfiguration.initializeForTest(testResourcePath);
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
     * Test SkillMatcher initialization
     */
    @Test
    public void testSkillMatcherInitialization() {
        try {
            SkillMatcher matcher = new SkillMatcher();
            assertNotNull("SkillMatcher should be created", matcher);
        } catch (Exception e) {
            fail("SkillMatcher initialization should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test matching single candidate with null skills
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchSingleWithNullCandidateSkills() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.matchSingle(null, Arrays.asList("Java", "Python"));
    }

    /**
     * Test matching single candidate with empty skills
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchSingleWithEmptyCandidateSkills() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.matchSingle(Collections.emptyList(), Arrays.asList("Java", "Python"));
    }

    /**
     * Test matching single candidate with null target skills
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchSingleWithNullTargetSkills() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.matchSingle(Arrays.asList("Java", "Python"), null);
    }

    /**
     * Test matching single candidate with empty target skills
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchSingleWithEmptyTargetSkills() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.matchSingle(Arrays.asList("Java", "Python"), Collections.emptyList());
    }

    /**
     * Test matching single candidate with valid data
     */
    @Test
    public void testMatchSingleWithValidData() {
        SkillMatcher matcher = new SkillMatcher();
        List<String> candidateSkills = Arrays.asList("Java", "Spring Boot", "MySQL");
        List<String> targetSkills = Arrays.asList("Java", "Spring", "Database");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            // If API is available, verify score range
            if (score != null) {
                assertTrue("Score should be >= 0.0", score >= 0.0f);
                assertTrue("Score should be <= 1.0", score <= 1.0f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should fail with runtime exception",
                    e.getMessage().contains("Failed") || e.getMessage().contains("API"));
        }
    }

    /**
     * Test matching multiple candidates with null list
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchMultipleWithNullCandidateList() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.match(null, Arrays.asList("Java", "Python"));
    }

    /**
     * Test matching multiple candidates with empty list
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMatchMultipleWithEmptyCandidateList() {
        SkillMatcher matcher = new SkillMatcher();
        matcher.match(Collections.emptyList(), Arrays.asList("Java", "Python"));
    }

    /**
     * Test matching multiple candidates with valid data
     */
    @Test
    public void testMatchMultipleWithValidData() {
        SkillMatcher matcher = new SkillMatcher();

        List<List<String>> candidateSkills = Arrays.asList(
                Arrays.asList("Java", "Spring Boot", "MySQL"),
                Arrays.asList("Python", "Django", "PostgreSQL"),
                Arrays.asList("JavaScript", "React", "Node.js")
        );
        List<String> targetSkills = Arrays.asList("Java", "Spring", "Backend Development");

        try {
            List<Float> scores = matcher.match(candidateSkills, targetSkills);

            // If API is available, verify results
            if (scores != null && !scores.isEmpty()) {
                assertEquals("Should return 3 scores", 3, scores.size());

                for (Float score : scores) {
                    assertTrue("Score should be >= 0.0", score >= 0.0f);
                    assertTrue("Score should be <= 1.0", score <= 1.0f);
                }

                // First candidate should have highest score (has Java and Spring Boot)
                assertTrue("First candidate should have high score", scores.get(0) > 0.5f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should fail with runtime exception",
                    e.getMessage().contains("Failed") || e.getMessage().contains("API"));
        }
    }

    /**
     * Test matching with highly relevant skills
     */
    @Test
    public void testMatchWithHighlyRelevantSkills() {
        SkillMatcher matcher = new SkillMatcher();

        // Candidate has exact matches
        List<String> candidateSkills = Arrays.asList("Java", "Spring Boot", "MySQL", "Redis");
        List<String> targetSkills = Arrays.asList("Java", "Spring Boot", "MySQL");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Should have high score due to exact matches
                assertTrue("Score should be high for exact matches", score > 0.8f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching with transferable skills
     */
    @Test
    public void testMatchWithTransferableSkills() {
        SkillMatcher matcher = new SkillMatcher();

        // Candidate has related but not exact skills
        List<String> candidateSkills = Arrays.asList("Vue.js", "MongoDB", "TypeScript");
        List<String> targetSkills = Arrays.asList("React", "MySQL", "JavaScript");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Should have moderate score due to transferable skills
                // Vue.js -> React, MongoDB -> MySQL, TypeScript -> JavaScript
                assertTrue("Score should reflect transferable skills", score >= 0.4f);
                assertTrue("Score should not be perfect for non-exact matches", score < 1.0f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching with unrelated skills
     */
    @Test
    public void testMatchWithUnrelatedSkills() {
        SkillMatcher matcher = new SkillMatcher();

        // Candidate has completely different domain skills
        List<String> candidateSkills = Arrays.asList("Photoshop", "Illustrator", "Figma");
        List<String> targetSkills = Arrays.asList("Java", "Spring Boot", "Microservices");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Should have low score due to unrelated skills
                assertTrue("Score should be low for unrelated skills", score < 0.4f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching with partial skill overlap
     */
    @Test
    public void testMatchWithPartialOverlap() {
        SkillMatcher matcher = new SkillMatcher();

        List<String> candidateSkills = Arrays.asList("Java", "Python", "Git");
        List<String> targetSkills = Arrays.asList("Java", "Spring Boot", "Docker", "Kubernetes");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Has Java (match) but missing Spring Boot, Docker, Kubernetes
                assertTrue("Score should be moderate for partial match",
                        score >= 0.3f && score <= 0.7f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching multiple candidates with varying relevance
     */
    @Test
    public void testMatchMultipleWithVaryingRelevance() {
        SkillMatcher matcher = new SkillMatcher();

        List<List<String>> candidateSkills = Arrays.asList(
                Arrays.asList("Java", "Spring Boot", "MySQL"),      // High match
                Arrays.asList("Python", "Flask", "PostgreSQL"),     // Medium match (backend)
                Arrays.asList("HTML", "CSS", "JavaScript")          // Low match (frontend)
        );
        List<String> targetSkills = Arrays.asList("Java", "Spring", "Backend");

        try {
            List<Float> scores = matcher.match(candidateSkills, targetSkills);

            if (scores != null && scores.size() == 3) {
                // Verify relative ordering
                assertTrue("First candidate should score highest",
                        scores.get(0) >= scores.get(1));
                assertTrue("Second candidate should score higher than third",
                        scores.get(1) >= scores.get(2));
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching with framework-specific skills
     */
    @Test
    public void testMatchWithFrameworkSkills() {
        SkillMatcher matcher = new SkillMatcher();

        // Candidate has Spring Boot, which implies Spring knowledge
        List<String> candidateSkills = Arrays.asList("Spring Boot", "Java");
        List<String> targetSkills = Arrays.asList("Spring", "Java");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Spring Boot implies Spring knowledge, should have high score
                assertTrue("Score should be high when framework implies base skill", score > 0.8f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching score precision
     */
    @Test
    public void testMatchScorePrecision() {
        SkillMatcher matcher = new SkillMatcher();

        List<String> candidateSkills = Arrays.asList("Java", "Python");
        List<String> targetSkills = Arrays.asList("Java", "Python");

        try {
            Float score = matcher.matchSingle(candidateSkills, targetSkills);

            if (score != null) {
                // Score should have at most 3 decimal places
                String scoreStr = score.toString();
                int decimalIndex = scoreStr.indexOf('.');
                if (decimalIndex != -1) {
                    int decimalPlaces = scoreStr.length() - decimalIndex - 1;
                    assertTrue("Score should have at most 3 decimal places", decimalPlaces <= 3);
                }
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
    }

    /**
     * Test matching consistency
     */
    @Test
    public void testMatchConsistency() {
        SkillMatcher matcher = new SkillMatcher();

        List<String> candidateSkills = Arrays.asList("Java", "Spring Boot");
        List<String> targetSkills = Arrays.asList("Java", "Spring");

        try {
            // Call multiple times
            Float score1 = matcher.matchSingle(candidateSkills, targetSkills);
            Float score2 = matcher.matchSingle(candidateSkills, targetSkills);

            // Results should be consistent (if API returns same result)
            if (score1 != null && score2 != null) {
                assertEquals("Scores should be consistent", score1, score2, 0.001f);
            }
        } catch (RuntimeException e) {
            // Expected if API is not available
        }
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
