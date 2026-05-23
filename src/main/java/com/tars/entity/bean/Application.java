package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a TA application submitted by a user for a specific position.
 * <p>
 * This entity tracks the application lifecycle from submission to final decision,
 * including status changes and feedback from Module Owners.
 * </p>
 * <p>
 * <b>Application Status Flow:</b>
 * <pre>
 * Applied (0) → Offered (1) or Rejected (2)
 *            → Withdrawn (3) [at any time by applicant]
 * </pre>
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>{@code 0} - APPLIED: Application submitted, awaiting review</li>
 *   <li>{@code 1} - OFFERED: MO has extended an offer to the candidate</li>
 *   <li>{@code 2} - REJECTED: Application declined by MO</li>
 *   <li>{@code 3} - WITHDRAWN: Applicant withdrew their application</li>
 * </ul>
 * </p>
 *
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/3/23
 * @see Position
 * @see User
 */
@Data
public class Application {

    /** Unique identifier for the application (UUID) */
    private String id;

    /** ID of the position being applied for */
    private String positionId;

    /** ID of the user (TA candidate) submitting the application */
    private String userId;

    /** Timestamp when the application was submitted */
    private Timestamp applyAt;

    /**
     * Application status indicator.
     * <ul>
     *   <li>0 = Applied (awaiting review)</li>
     *   <li>1 = Offered (offer extended)</li>
     *   <li>2 = Rejected (application declined)</li>
     *   <li>3 = Withdrawn (applicant withdrew)</li>
     * </ul>
     */
    private int status; // 0-applied, 1-offered, 2-rejected, 3-withdrawn

    /** Feedback or comments from MO regarding the application decision */
    private String feedback;

    /**
     * Default constructor that initializes:
     * <ul>
     *   <li>Unique UUID-based ID</li>
     *   <li>Application timestamp (current time)</li>
     * </ul>
     * <p>
     * Status defaults to 0 (Applied) as this represents a new submission.
     * </p>
     */
    public Application() {
        this.id = UUID.randomUUID().toString();
        this.applyAt = Timestamp.valueOf(LocalDateTime.now());
    }

    /**
     * Constructor with explicit status initialization.
     * <p>
     * Useful for creating applications with non-default statuses
     * (e.g., during data migration or testing).
     * </p>
     *
     * @param status Initial application status (0-3)
     */
    public Application(int status) {
        this.status = status;
    }
}
