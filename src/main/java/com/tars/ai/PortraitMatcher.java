package com.tars.ai;

import com.tars.config.QwenConfiguration;
import com.tars.config.Weight;
import com.tars.entity.bean.Portrait;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PortraitMatcher {

    private final Weight weight;

    public PortraitMatcher() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.weight = config.getWeight();
        log.info("PortraitMatcher initialized with weights: skills={}, experience={}, softSkills={}",
                weight.getSkills(), weight.getExperience(), weight.getSoftSkills());
    }

    /**
     * Calculate recommendation score between TA portrait and position portrait
     * 
     * @param portrait1 First portrait
     * @param portrait2 Second portrait
     * @return Recommendation score (0-1 range, higher is better match)
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
     * Calculate cosine similarity between two vectors
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity in range [-1, 1]
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
