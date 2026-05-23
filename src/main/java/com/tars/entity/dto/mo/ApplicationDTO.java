package com.tars.entity.dto.mo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Data Transfer Object for displaying TA application information in MO portal.
 * <p>
 * This DTO provides essential applicant information for Module Owners to review
 * TA applications, including academic background and application status.
 * </p>
 * <p>
 * <b>Usage:</b> Used in MO application review page to display a list of applicants
 * for a specific position, allowing MOs to quickly assess candidates.
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>0 = Applied (awaiting review)</li>
 *   <li>1 = Offered (offer extended)</li>
 *   <li>2 = Rejected (application declined)</li>
 * </ul>
 * </p>
 *
 * @author 477996850
 * @version 1.0.0
 * @since 2026/4/5
 * @see com.tars.entity.bean.Application
 * @see com.tars.entity.bean.TAProfile
 * @see com.tars.controller.MOServlet
 */
@Data
public class ApplicationDTO {

    /** Application identifier */
    private String appId;

    /** TA profile identifier */
    private String proId;

    /** Applicant's full name */
    private String name;

    /** Applicant's college affiliation */
    private String college;

    /** Applicant's major */
    private String major;

    /** Degree level (BACHELOR, MASTER, PHD) */
    private String degree;

    /** Academic year */
    private int year;

    /** Timestamp when the application was submitted */
    private Timestamp applyAt;

    /**
     * Application status indicator.
     * <ul>
     *   <li>0 = Applied</li>
     *   <li>1 = Offered</li>
     *   <li>2 = Rejected</li>
     * </ul>
     */
    private int status; // 0-applied, 1-offered, 2-rejected
}
