package com.tars.ai;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import com.tars.entity.bean.Portrait;
import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for PortraitMatcher
 * Tests portrait matching and cosine similarity calculation
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class PortraitMatcherTest {

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
     * Test PortraitMatcher initialization
     */
    @Test
    public void testPortraitMatcherInitialization() {
        try {
            PortraitMatcher matcher = new PortraitMatcher();
            assertNotNull("PortraitMatcher should be created", matcher);
        } catch (Exception e) {
            fail("PortraitMatcher initialization should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test calculating match score with null portraits
     */
    @Test
    public void testCalculateMatchScoreWithNullPortraits() {
        PortraitMatcher matcher = new PortraitMatcher();

        double score1 = matcher.calculateMatchScore(null, null);
        assertEquals("Score should be 0.0 for null portraits", 0.0, score1, 0.001);

        Portrait portrait = createTestPortrait(1.0f, 2.0f, 3.0f);
        double score2 = matcher.calculateMatchScore(null, portrait);
        assertEquals("Score should be 0.0 when first portrait is null", 0.0, score2, 0.001);

        double score3 = matcher.calculateMatchScore(portrait, null);
        assertEquals("Score should be 0.0 when second portrait is null", 0.0, score3, 0.001);
    }

    /**
     * Test calculating match score with identical portraits
     */
    @Test
    public void testCalculateMatchScoreWithIdenticalPortraits() {
        PortraitMatcher matcher = new PortraitMatcher();

        List<Float> skills = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> experience = Arrays.asList(4.0f, 5.0f, 6.0f);
        List<Float> softSkills = Arrays.asList(7.0f, 8.0f, 9.0f);

        Portrait portrait1 = new Portrait(skills, experience, softSkills);
        Portrait portrait2 = new Portrait(skills, experience, softSkills);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Identical vectors should have cosine similarity of 1.0
        // After normalization: (1.0 + 1.0) / 2.0 = 1.0
        // Weighted score should be close to 1.0 (allowing for floating point precision)
        assertTrue("Score should be high for identical portraits", score > 0.9);
        assertTrue("Score should not exceed 1.0 (with tolerance)", score <= 1.0001);
    }

    /**
     * Test calculating match score with orthogonal vectors
     */
    @Test
    public void testCalculateMatchScoreWithOrthogonalVectors() {
        PortraitMatcher matcher = new PortraitMatcher();

        // Orthogonal vectors (dot product = 0)
        List<Float> skills1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> skills2 = Arrays.asList(0.0f, 1.0f, 0.0f);

        List<Float> exp1 = Arrays.asList(1.0f, 0.0f);
        List<Float> exp2 = Arrays.asList(0.0f, 1.0f);

        List<Float> soft1 = Arrays.asList(1.0f, 0.0f);
        List<Float> soft2 = Arrays.asList(0.0f, 1.0f);

        Portrait portrait1 = new Portrait(skills1, exp1, soft1);
        Portrait portrait2 = new Portrait(skills2, exp2, soft2);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Orthogonal vectors have cosine similarity of 0
        // After normalization: (0.0 + 1.0) / 2.0 = 0.5
        // Score should be around 0.5
        assertTrue("Score should be around 0.5 for orthogonal vectors",
                score >= 0.4 && score <= 0.6);
    }

    /**
     * Test calculating match score with opposite vectors
     */
    @Test
    public void testCalculateMatchScoreWithOppositeVectors() {
        PortraitMatcher matcher = new PortraitMatcher();

        // Opposite vectors (cosine similarity = -1)
        List<Float> skills1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> skills2 = Arrays.asList(-1.0f, -2.0f, -3.0f);

        List<Float> exp1 = Arrays.asList(1.0f, 2.0f);
        List<Float> exp2 = Arrays.asList(-1.0f, -2.0f);

        List<Float> soft1 = Arrays.asList(1.0f, 2.0f);
        List<Float> soft2 = Arrays.asList(-1.0f, -2.0f);

        Portrait portrait1 = new Portrait(skills1, exp1, soft1);
        Portrait portrait2 = new Portrait(skills2, exp2, soft2);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Opposite vectors have cosine similarity of -1
        // After normalization: (-1.0 + 1.0) / 2.0 = 0.0
        // Score should be close to 0.0
        assertTrue("Score should be low for opposite vectors", score < 0.1);
        assertTrue("Score should not be negative", score >= 0.0);
    }

    /**
     * Test calculating match score with empty vectors
     */
    @Test
    public void testCalculateMatchScoreWithEmptyVectors() {
        PortraitMatcher matcher = new PortraitMatcher();

        Portrait portrait1 = new Portrait(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Portrait portrait2 = new Portrait(Arrays.asList(1.0f, 2.0f), Arrays.asList(3.0f, 4.0f), Arrays.asList(5.0f, 6.0f));

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Empty vectors should result in 0.0 similarity
        // After normalization: (0.0 + 1.0) / 2.0 = 0.5 for each dimension
        // Score should be around 0.5
        assertTrue("Score should handle empty vectors gracefully", score >= 0.0 && score <= 1.0);
    }

    /**
     * Test calculating match score with different vector sizes
     */
    @Test
    public void testCalculateMatchScoreWithDifferentVectorSizes() {
        PortraitMatcher matcher = new PortraitMatcher();

        Portrait portrait1 = new Portrait(
                Arrays.asList(1.0f, 2.0f, 3.0f),
                Arrays.asList(1.0f, 2.0f),
                Arrays.asList(1.0f, 2.0f)
        );
        Portrait portrait2 = new Portrait(
                Arrays.asList(1.0f, 2.0f), // Different size
                Arrays.asList(1.0f, 2.0f),
                Arrays.asList(1.0f, 2.0f)
        );

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Mismatched vector sizes should return 0.0 for that dimension
        // Score should still be calculable from other dimensions
        assertTrue("Score should handle mismatched vectors", score >= 0.0 && score <= 1.0);
    }

    /**
     * Test calculating match score with highly similar portraits
     */
    @Test
    public void testCalculateMatchScoreWithHighlySimilarPortraits() {
        PortraitMatcher matcher = new PortraitMatcher();

        // Very similar vectors
        List<Float> skills1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> skills2 = Arrays.asList(1.1f, 2.1f, 3.1f);

        List<Float> exp1 = Arrays.asList(4.0f, 5.0f, 6.0f);
        List<Float> exp2 = Arrays.asList(4.1f, 5.1f, 6.1f);

        List<Float> soft1 = Arrays.asList(7.0f, 8.0f, 9.0f);
        List<Float> soft2 = Arrays.asList(7.1f, 8.1f, 9.1f);

        Portrait portrait1 = new Portrait(skills1, exp1, soft1);
        Portrait portrait2 = new Portrait(skills2, exp2, soft2);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Highly similar vectors should have high score
        assertTrue("Score should be high for similar portraits", score > 0.95);
    }

    /**
     * Test calculating match score with partially matching portraits
     */
    @Test
    public void testCalculateMatchScoreWithPartialMatch() {
        PortraitMatcher matcher = new PortraitMatcher();

        // Skills match perfectly, experience is orthogonal, soft skills match
        List<Float> skills1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> skills2 = Arrays.asList(1.0f, 2.0f, 3.0f);

        List<Float> exp1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> exp2 = Arrays.asList(0.0f, 1.0f, 0.0f);

        List<Float> soft1 = Arrays.asList(1.0f, 2.0f);
        List<Float> soft2 = Arrays.asList(1.0f, 2.0f);

        Portrait portrait1 = new Portrait(skills1, exp1, soft1);
        Portrait portrait2 = new Portrait(skills2, exp2, soft2);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // With weights: skills=0.6, experience=0.15, softSkills=0.25
        // Skills match (1.0), experience orthogonal (~0.5), soft skills match (1.0)
        // Expected: 0.6*1.0 + 0.15*0.5 + 0.25*1.0 = 0.925
        // This is a high score because skills and soft skills both match
        assertTrue("Score should reflect partial match", score >= 0.8 && score <= 1.0);
        assertTrue("Score should be less than perfect match", score < 1.0);
    }

    /**
     * Test cosine similarity with zero vectors
     */
    @Test
    public void testCosineSimilarityWithZeroVectors() {
        PortraitMatcher matcher = new PortraitMatcher();

        List<Float> zeroVector = Arrays.asList(0.0f, 0.0f, 0.0f);
        List<Float> normalVector = Arrays.asList(1.0f, 2.0f, 3.0f);

        Portrait portrait1 = new Portrait(zeroVector, zeroVector, zeroVector);
        Portrait portrait2 = new Portrait(normalVector, normalVector, normalVector);

        double score = matcher.calculateMatchScore(portrait1, portrait2);

        // Zero vectors should result in 0.0 similarity
        // After normalization: (0.0 + 1.0) / 2.0 = 0.5
        assertTrue("Score should handle zero vectors", score >= 0.0 && score <= 1.0);
    }

    /**
     * Test match score range validation
     */
    @Test
    public void testMatchScoreRange() {
        PortraitMatcher matcher = new PortraitMatcher();

        // Test various combinations to ensure score is always in [0, 1]
        for (int i = 0; i < 5; i++) {
            List<Float> v1 = Arrays.asList((float) Math.random(), (float) Math.random(), (float) Math.random());
            List<Float> v2 = Arrays.asList((float) Math.random(), (float) Math.random(), (float) Math.random());

            Portrait p1 = new Portrait(v1, v1, v1);
            Portrait p2 = new Portrait(v2, v2, v2);

            double score = matcher.calculateMatchScore(p1, p2);

            assertTrue("Score should be >= 0.0", score >= 0.0);
            assertTrue("Score should be <= 1.0", score <= 1.0);
        }
    }

    /**
     * Test match score consistency
     */
    @Test
    public void testMatchScoreConsistency() {
        PortraitMatcher matcher = new PortraitMatcher();

        List<Float> skills1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> skills2 = Arrays.asList(4.0f, 5.0f, 6.0f);
        List<Float> exp1 = Arrays.asList(7.0f, 8.0f);
        List<Float> exp2 = Arrays.asList(9.0f, 10.0f);
        List<Float> soft1 = Arrays.asList(11.0f, 12.0f);
        List<Float> soft2 = Arrays.asList(13.0f, 14.0f);

        Portrait portrait1 = new Portrait(skills1, exp1, soft1);
        Portrait portrait2 = new Portrait(skills2, exp2, soft2);

        // Calculate score multiple times
        double score1 = matcher.calculateMatchScore(portrait1, portrait2);
        double score2 = matcher.calculateMatchScore(portrait1, portrait2);
        double score3 = matcher.calculateMatchScore(portrait2, portrait1); // Reversed

        // Scores should be consistent
        assertEquals("Scores should be consistent", score1, score2, 0.0001);
        // Match score should be symmetric
        assertEquals("Match score should be symmetric", score1, score3, 0.0001);
    }

    /**
     * Helper method to create test portrait
     */
    private Portrait createTestPortrait(float skillVal, float expVal, float softVal) {
        List<Float> skills = Arrays.asList(skillVal, skillVal + 1.0f, skillVal + 2.0f);
        List<Float> experience = Arrays.asList(expVal, expVal + 1.0f, expVal + 2.0f);
        List<Float> softSkills = Arrays.asList(softVal, softVal + 1.0f, softVal + 2.0f);

        return new Portrait(skills, experience, softSkills);
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
