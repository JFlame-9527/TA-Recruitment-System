package com.tars.entity.dto.mo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Data Transfer Object for displaying position brief information in MO portal.
 * <p>
 * This DTO provides a condensed view of positions created by a Module Owner,
 * including vacancy and pending application counts for quick status overview.
 * </p>
 * <p>
 * <b>Usage:</b> Used in MO position management page ({@code /views/mo/position.jsp})
 * to display a list of positions posted by the MO with key statistics.
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
public class PosBriefDTO {

    /** Position identifier */
    private String posId;

    /** Job title */
    private String title;

    /** Module code */
    private String moduleCode;

    /** Module name */
    private String moduleName;

    /** Number of remaining vacancies (requiredNum - offeredNum) */
    private int vacancyNum;

    /** Number of pending applications (applied but not yet decided) */
    private int pendingNum;

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
