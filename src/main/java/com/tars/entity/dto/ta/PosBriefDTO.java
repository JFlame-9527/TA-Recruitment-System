package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Data Transfer Object for displaying position brief information in TA portal.
 * <p>
 * This DTO provides a condensed view of positions for the TA position listing page,
 * including application status to help TAs quickly identify which positions they've
 * applied to and their current status.
 * </p>
 * <p>
 * <b>Usage:</b> Used in TA positions listing page ({@code /views/ta/positions.jsp})
 * to display available positions with application status indicators.
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
 * @since 2026/4/7
 * @see com.tars.entity.bean.Position
 * @see com.tars.entity.bean.Application
 * @see com.tars.controller.TAServlet
 */
@Data
public class PosBriefDTO {

    /** Position identifier */
    private String posId;

    /** Job title */
    private String title;

    /** Module code (e.g., "CS101") */
    private String moduleCode;

    /** Module name (e.g., "Introduction to Computer Science") */
    private String moduleName;

    /** Weekly workload in hours */
    private float weeklyWorkload;

    /** Duration in weeks */
    private int duration;

    /** Number of TA positions required */
    private int requiredNum;

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
}
