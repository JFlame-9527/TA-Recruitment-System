package com.tars.entity.dto.mo;

import com.tars.entity.bean.Application;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Transfer Object for displaying detailed position information in MO portal.
 * <p>
 * This DTO provides comprehensive position details for Module Owners to manage
 * their posted positions, including all statistics and requirements. Unlike the
 * TA version, this includes {@code appliedNum} and {@code rejectedNum} for
 * administrative tracking.
 * </p>
 * <p>
 * <b>Usage:</b> Used in MO position detail/edit page to display and modify
 * position information, view application statistics, and manage the position lifecycle.
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>0 = Opened (accepting applications)</li>
 *   <li>1 = Filled (all positions offered)</li>
 *   <li>2 = Closed (no longer accepting)</li>
 *   <li>3 = Withdrawn (cancelled by MO)</li>
 * </ul>
 * </p>
 *
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/30
 * @see com.tars.entity.bean.Position
 * @see com.tars.controller.MOServlet
 */
@Data
public class PosDetailDTO {

    /** Position identifier */
    private String posId;

    /** Job title */
    private String title;

    /** Detailed job description */
    private String description;

    /** Module code */
    private String moduleCode;

    /** Module name */
    private String moduleName;

    /** List of required technical skills */
    private List<String> skills;

    /** Weekly workload in hours */
    private float weeklyWorkload;

    /** Duration in weeks */
    private int duration;

    /** Minimum grade requirement */
    private int minGrade;

    /** Maximum grade restriction */
    private int maxGrade;

    /** Number of TA positions required */
    private int requiredNum;

    /** Number of offers extended */
    private int offeredNum;

    /** Total number of applications received */
    private int appliedNum;

    /** Number of applications rejected */
    private int rejectedNum;

    /** Position start date */
    private Timestamp startDate;

    /** Position end date */
    private Timestamp endDate;

    /** Date when the position was posted */
    private Timestamp postDate;

    /** Application deadline */
    private Timestamp deadline;

    /**
     * Position status indicator.
     * <ul>
     *   <li>0 = Opened</li>
     *   <li>1 = Filled</li>
     *   <li>2 = Closed</li>
     *   <li>3 = Withdrawn</li>
     * </ul>
     */
    private int status; // 0-opened, 1-filled, 2-closed, 3-withdrawn
}
