package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a job position in the TA Recruitment System.
 * <p>
 * This entity stores all information about a technical assistant position, including:
 * <ul>
 *   <li>Basic information (title, description, module)</li>
 *   <li>Requirements (skills, grade range, workload, duration)</li>
 *   <li>Statistics (required/offered/applied/rejected counts)</li>
 *   <li>Lifecycle dates (start, end, deadline, posting date)</li>
 *   <li>Status tracking (opened, filled, closed, withdrawn)</li>
 *   <li>AI-generated portrait for candidate matching</li>
 * </ul>
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>{@code 0} - OPENED: Position is accepting applications</li>
 *   <li>{@code 1} - FILLED: All required positions have been offered</li>
 *   <li>{@code 2} - CLOSED: Position is no longer accepting applications</li>
 *   <li>{@code 3} - WITHDRAWN: Position has been cancelled by MO</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 * @see Application
 * @see com.tars.ai.PortraitGenerator
 */
@Data
public class Position {

    /** Unique identifier for the position (UUID) */
    private String id;

    /** Job title (e.g., "Java Backend TA", "Frontend Development TA") */
    private String title;

    /** Detailed job description including responsibilities and requirements */
    private String description;

    /** Module code identifier (e.g., "CS101", "SE202") */
    private String moduleCode;

    /** Full module name (e.g., "Introduction to Computer Science") */
    private String moduleName;

    /** List of required technical skills (e.g., ["Java", "Spring Boot", "MySQL"]) */
    private List<String> skills;

    /** ID of the Module Owner (MO) who posted this position */
    private String postUserId;

    /** Weekly workload in hours (e.g., 10.5 hours per week) */
    private float weeklyWorkload;

    /** Duration of the position in weeks (e.g., 12 weeks for a semester) */
    private int duration;

    /** Minimum grade requirement (-1 means no minimum restriction) */
    private int minGrade = -1;

    /** Maximum grade restriction (default: Integer.MAX_VALUE, no upper limit) */
    private int maxGrade = Integer.MAX_VALUE;

    /** Number of TA positions required for this role */
    private int requiredNum;

    /** Number of offers extended to candidates */
    private int offeredNum;

    /** Number of applications received */
    private int appliedNum;

    /** Number of applications rejected */
    private int rejectedNum;

    /** Position start date (when the TA begins work) */
    private Timestamp startDate;

    /** Position end date (when the TA contract ends) */
    private Timestamp endDate;

    /** Date when the position was posted/opened for applications */
    private Timestamp postDate;

    /** Application deadline (last date to apply) */
    private Timestamp deadline;

    /** Timestamp when this position record was created */
    private Timestamp createAt;

    /** Timestamp when this position was last updated */
    private Timestamp updateAt;

    /**
     * Position status indicator.
     * <ul>
     *   <li>0 = Opened (accepting applications)</li>
     *   <li>1 = Filled (all positions offered)</li>
     *   <li>2 = Closed (no longer accepting)</li>
     *   <li>3 = Withdrawn (cancelled by MO)</li>
     * </ul>
     */
    private int status; // 0-opened, 1-filled, 2-closed, 3-withdrawn

    /** ID of the AI-generated portrait for this position (used for candidate matching) */
    private String portraitId;

    /**
     * Default constructor that initializes:
     * <ul>
     *   <li>Unique UUID-based ID</li>
     *   <li>Creation timestamp (current time)</li>
     *   <li>Update timestamp (current time)</li>
     * </ul>
     */
    public Position() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
