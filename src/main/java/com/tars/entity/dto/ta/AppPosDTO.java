package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Data Transfer Object for displaying TA's application history with position summary.
 * <p>
 * This DTO combines application information with basic position details to provide
 * TA users with a comprehensive view of their application history, including the
 * current status of each application.
 * </p>
 * <p>
 * <b>Usage:</b> Used in TA application history page to display a list of all positions
 * the TA has applied to, along with application status and timeline.
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>0 = Applied (awaiting MO review)</li>
 *   <li>1 = Offered (MO extended an offer)</li>
 *   <li>2 = Rejected (application declined)</li>
 *   <li>3 = Withdrawn (TA withdrew application)</li>
 * </ul>
 * </p>
 *
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/4/18
 * @see com.tars.entity.bean.Application
 * @see com.tars.entity.bean.Position
 * @see com.tars.controller.TAServlet
 */
@Data
public class AppPosDTO {

    /** Application identifier */
    private String appId;

    /** Position identifier */
    private String posId;

    /** Position title */
    private String title;

    /** Module code (e.g., "CS101") */
    private String moduleCode;

    /** Module name (e.g., "Introduction to Computer Science") */
    private String moduleName;

    /** Number of offers extended for this position */
    private int offeredNum;

    /** Total number of TA positions required */
    private int requiredNum;

    /**
     * Application status indicator.
     * <ul>
     *   <li>0 = Applied</li>
     *   <li>1 = Offered</li>
     *   <li>2 = Rejected</li>
     *   <li>3 = Withdrawn</li>
     * </ul>
     */
    private int status; // 0-applied, 1-offered, 2-rejected, 3-withdrawn

    /** Timestamp when the application was submitted */
    private Timestamp applyAt;
}
