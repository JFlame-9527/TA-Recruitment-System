package com.tars.ai;

import com.tars.config.QwenConfiguration;
import com.tars.config.Weight;
import com.tars.entity.bean.Portrait;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Portrait matcher that calculates similarity scores between candidate and position portraits.
 * <p>
 * This class implements a weighted cosine similarity algorithm to evaluate how well a TA candidate's
 * profile matches a job position's requirements. It compares three dimensions:
 * <ul>
 *   <li><b>Skills</b>: Technical and professional capabilities vector</li>
 *   <li><b>Experience</b>: Work history and achievements vector</li>
 *   <li><b>Soft Skills</b>: Interpersonal and behavioral competencies vector</li>
 * </ul>
 * </p>
 * <p>
 * The matching process:
 * <ol>
 *   <li>Calculates cosine similarity for each dimension separately</li>
 *   <li>Normalizes similarity scores from [-1, 1] to [0, 1] range</li>
 *   <li>Applies configurable weights to each dimension</li>
 *   <li>Returns a weighted composite score</li>
 * </ol>
 * </p>
 * <p>
 * Score interpretation:
 * <ul>
 *   <li><b>0.8 - 1.0</b>: Excellent match - candidate closely aligns with position requirements</li>
 *   <li><b>0.6 - 0.8</b>: Good match - candidate has relevant skills with some gaps</li>
 *   <li><b>0.4 - 0.6</b>: Moderate match - candidate has partial alignment</li>
 *   <li><b>0.0 - 0.4</b>: Weak match - significant skill or experience gaps</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see Portrait
 * @see PortraitGenerator
 * @see Weight
 * @see QwenConfiguration
 */
@Slf4j
public class PortraitMatcher {

    private final Weight weight;

    /**
     * Constructs a new PortraitMatcher instance.
     * <p>
     * Initializes the matcher with weighting configuration from {@link QwenConfiguration}.
     * The weights determine the relative importance of each portrait dimension in the
     * final match score calculation.
     * </p>
     * <p>
     * Default weights (configurable via config.json):
     * <ul>
     *   <li>Skills: typically 0.5 (50%)</li>
     *   <li>Experience: typically 0.3 (30%)</li>
     *   <li>Soft Skills: typically 0.2 (20%)</li>
     * </ul>
     * </p>
     *
     * @throws RuntimeException if configuration initialization fails
     * @see Weight
     */
    public PortraitMatcher() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.weight = config.getWeight();
        log.info("PortraitMatcher initialized with weights: skills={}, experience={}, softSkills={}",
                weight.getSkills(), weight.getExperience(), weight.getSoftSkills());
    }

    /**
     * Calculates the recommendation score between a TA candidate portrait and a position portrait.
     * <p>
     * This method performs a comprehensive comparison using weighted cosine similarity across
     * three dimensions. The calculation process:
     * </p>
     * <ol>
     *   <li><b>Dimension-wise Similarity</b>: Calculates cosine similarity for skills, experience,
     *       and soft skills vectors independently</li>
     *   <li><b>Normalization</b>: Converts cosine similarity from [-1, 1] to [0, 1] range using
     *       the formula: {@code normalized = (similarity + 1.0) / 2.0}</li>
     *   <li><b>Weighted Aggregation</b>: Combines normalized scores using configured weights:
     *       {@code score = skillsNorm × w1 + expNorm × w2 + softSkillsNorm × w3}</li>
     * </ol>
     * <p>
     * <b>Error Handling:</b> If either portrait is null or any error occurs during calculation,
     * the method returns 0.0 (no match) instead of throwing an exception. This ensures robustness
     * in batch processing scenarios.
     * </p>
     * <p>
     * <b>Mathematical Foundation:</b>
     * Cosine similarity measures the cosine of the angle between two vectors in multi-dimensional space:
     * <pre>{@code
     * cos(θ) = (A · B) / (||A|| × ||B||)
     * }</pre>
     * Values range from -1 (opposite) to 1 (identical), with 0 indicating orthogonality (no correlation).
     * </p>
     *
     * @param portrait1 First portrait (typically TA candidate's profile)
     * @param portrait2 Second portrait (typically position requirements)
     * @return Match score in range [0.0, 1.0], where higher values indicate better match.
     *         Returns 0.0 if portraits are null or calculation fails.
     * @see #calculateCosineSimilarity(List, List)
     * @see Weight
     */
    public double calculateMatchScore(Portrait portrait1, Portrait portrait2) {
        if (portrait1 == null || portrait2 == null) {
            log.warn("Cannot calculate match score: null portrait provided");
            return 0.0;
        }

        try {
            // Calculate cosine similarity for each dimension
            double skillsSimilarity = calculateCosineSimilarity(portrait1.getSkills(), portrait2.getSkills());
            double experienceSimilarity = calculateCosineSimilarity(portrait1.getExperience(), portrait2.getExperience());
            double softSkillsSimilarity = calculateCosineSimilarity(portrait1.getSoftSkills(), portrait2.getSoftSkills());

            // Shift cosine similarity from [-1, 1] to [0, 1]
            double skillsNormalized = (skillsSimilarity + 1.0) / 2.0;
            double experienceNormalized = (experienceSimilarity + 1.0) / 2.0;
            double softSkillsNormalized = (softSkillsSimilarity + 1.0) / 2.0;

            // Calculate weighted score
            double score = skillsNormalized * weight.getSkills()
                    + experienceNormalized * weight.getExperience()
                    + softSkillsNormalized * weight.getSoftSkills();

            log.debug("Match score calculated - Skills: {}, Experience: {}, SoftSkills: {}, Final: {}",
                    skillsNormalized, experienceNormalized, softSkillsNormalized, score);

            return score;

        } catch (Exception e) {
            log.error("Error calculating match score", e);
            return 0.0;
        }
    }

    /**
     * Calculates cosine similarity between two vector embeddings.
     * <p>
     * Cosine similarity is a measure of similarity between two non-zero vectors that evaluates
     * the cosine of the angle between them. This metric is particularly useful for comparing
     * high-dimensional embeddings because it focuses on orientation rather than magnitude.
     * </p>
     * <p>
     * <b>Formula:</b>
     * <pre>{@code
     * cos(θ) = (A · B) / (||A|| × ||B||)
     * 
     * where:
     * A · B  = Σ(Ai × Bi)  [dot product]
     * ||A||  = √(Σ(Ai²))   [magnitude of A]
     * ||B||  = √(Σ(Bi²))   [magnitude of B]
     * }</pre>
     * </p>
     * <p>
     * <b>Validation checks:</b>
     * <ul>
     *   <li>Null or empty vectors → returns 0.0</li>
     *   <li>Vector size mismatch → returns 0.0 (logs warning)</li>
     *   <li>Zero magnitude vectors → returns 0.0 (avoid division by zero)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Output range clamping:</b> The result is clamped to [-1.0, 1.0] to handle potential
     * floating-point precision errors that might produce values slightly outside the theoretical range.
     * </p>
     *
     * @param vector1 First vector (must have same dimension as vector2)
     * @param vector2 Second vector (must have same dimension as vector1)
     * @return Cosine similarity value in range [-1.0, 1.0]:
     *         <ul>
     *           <li>1.0: Vectors are identical (same direction)</li>
     *           <li>0.0: Vectors are orthogonal (no correlation)</li>
     *           <li>-1.0: Vectors are opposite (completely different)</li>
     *         </ul>
     *         Returns 0.0 if validation fails.
     */
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            log.warn("Cannot calculate cosine similarity: null or empty vectors");
            return 0.0;
        }

        if (vector1.size() != vector2.size()) {
            log.warn("Vector size mismatch: {} vs {}", vector1.size(), vector2.size());
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0.0 || norm2 == 0.0) {
            log.warn("Zero vector encountered, returning 0 similarity");
            return 0.0;
        }

        double similarity = dotProduct / (norm1 * norm2);
        
        // Clamp value to [-1, 1] to handle floating point errors
        return Math.max(-1.0, Math.min(1.0, similarity));
    }
}
