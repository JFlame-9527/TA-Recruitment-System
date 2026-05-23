package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Transfer Object for displaying detailed position information in TA portal.
 * <p>
 * This DTO provides comprehensive position details for TA users viewing a specific
 * position, including requirements, statistics, and the TA's application status
 * if they have applied.
 * </p>
 * <p>
 * <b>Usage:</b> Used in TA position detail page ({@code /views/ta/positionDetail.jsp})
 * to display full job description, requirements, and application information.
 * </p>
 * <p>
 * <b>Position Status Values:</b>
 * <ul>
 *   <li>0 = Opened (accepting applications)</li>
 *   <li>1 = Filled (all positions offered)</li>
 *   <li>2 = Closed (no longer accepting)</li>
 *   <li>3 = Withdrawn (cancelled by MO)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Application Status Values:</b>
 * <ul>
 *   <li>-1 = Not applied (TA can apply)</li>
 *   <li>0 = Applied (awaiting review)</li>
 *   <li>1 = Offered (offer extended)</li>
 *   <li>2 = Rejected (application declined)</li>
 *   <li>3 = Withdrawn (TA withdrew application)</li>
 * </ul>
 * </p>
 *
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/4/8
 * @see com.tars.entity.bean.Position
 * @see com.tars.entity.bean.Application
 * @see com.tars.controller.TAServlet
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

    /** Minimum grade requirement (-1 means no minimum) */
    private int minGrade;

    /** Maximum grade restriction */
    private int maxGrade;

    /** Number of TA positions required */
    private int requiredNum;

    /** Number of offers extended */
    private int offeredNum;

    /** Position start date */
    private Timestamp startDate;

    /** Position end date */
    private Timestamp endDate;

    /** Application deadline */
    private Timestamp deadline;

    /** Date when the position was posted */
    private Timestamp postDate;

    /**
     * Position status indicator.
     * <ul>
     *   <li>0 = Opened</li>
     *   <li>1 = Filled</li>
     *   <li>2 = Closed</li>
     *   <li>3 = Withdrawn</li>
     * </ul>
     */
    private int posStatus; // 0-opened, 1-filled, 2-closed, 3-withdrawn

    /**
     * Application status from TA's perspective.
     * <ul>
     *   <li>-1 = Not applied</li>
     *   <li>0 = Applied</li>
     *   <li>1 = Offered</li>
     *   <li>2 = Rejected</li>
     *   <li>3 = Withdrawn</li>
     * </ul>
     */
    private int appStatus; // (-1)-not applied, 0-applied, 1-offered, 2-rejected, 3-withdrawn

    /** Application ID (null if not applied) */
    private String appId;

    /** Feedback from MO (only present if application was reviewed) */
    private String feedback;

    /** Timestamp when the TA applied (null if not applied) */
    private Timestamp applyAt;
}
