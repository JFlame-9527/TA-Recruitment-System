package com.tars.config;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration class for portrait matching weights.
 * <p>
 * This class defines the relative importance of each dimension when calculating
 * match scores between candidate and position portraits. The weights are used by
 * {@link com.tars.ai.PortraitMatcher} to compute a weighted composite score.
 * </p>
 * <p>
 * <b>Weight Constraints:</b>
 * <ul>
 *   <li>All weights should be non-negative values</li>
 *   <li>The sum of all weights should ideally equal 1.0 (100%)</li>
 *   <li>Typical distribution: skills=0.5, experience=0.3, softSkills=0.2</li>
 * </ul>
 * </p>
 * <p>
 * <b>Example configuration in config.json:</b>
 * <pre>{@code
 * {
 *   "weight": {
 *     "skills": 0.5,
 *     "experience": 0.3,
 *     "softSkills": 0.2
 *   }
 * }
 * }</pre>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see com.tars.ai.PortraitMatcher
 * @see QwenConfiguration#getWeight()
 */
@Data
@Builder
public class Weight {

    /**
     * Weight for technical and professional skills dimension.
     * <p>
     * This weight determines how much importance is given to skill matching
     * when calculating the overall portrait match score. Skills typically receive
     * the highest weight as they directly indicate job competency.
     * </p>
     * <p>
     * <b>Recommended range:</b> 0.4 - 0.6 (40% - 60%)
     * </p>
     *
     * @see com.tars.entity.bean.Portrait#getSkills()
     */
    private float skills;

    /**
     * Weight for work experience dimension.
     * <p>
     * This weight determines the importance of experience similarity in the
     * overall match calculation. Experience includes years of work, project types,
     * domain expertise, and career achievements.
     * </p>
     * <p>
     * <b>Recommended range:</b> 0.2 - 0.4 (20% - 40%)
     * </p>
     *
     * @see com.tars.entity.bean.Portrait#getExperience()
     */
    private float experience;

    /**
     * Weight for soft skills dimension.
     * <p>
     * This weight determines the importance of interpersonal and behavioral
     * competencies in the match calculation. Soft skills include communication,
     * leadership, teamwork, adaptability, and problem-solving abilities.
     * </p>
     * <p>
     * <b>Recommended range:</b> 0.1 - 0.3 (10% - 30%)
     * </p>
     *
     * @see com.tars.entity.bean.Portrait#getSoftSkills()
     */
    private float softSkills;
}
