package com.tars.entity.dto.admin;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Transfer Object for displaying TA profile information in admin panel.
 * <p>
 * This DTO provides a simplified view of {@link com.tars.entity.bean.TAProfile} for
 * administrative purposes, excluding sensitive fields like {@code portraitId} and
 * {@code maxWeeklyWorkload} that are not needed for admin overview.
 * </p>
 * <p>
 * <b>Usage:</b> Used in admin TA management pages to display candidate profiles,
 * review applications, and manage TA accounts.
 * </p>
 * <p>
 * <b>Differences from TAProfile:</b>
 * <ul>
 *   <li>Excludes {@code grade} (calculated field, not needed for display)</li>
 *   <li>Excludes {@code portraitId} (internal AI matching reference)</li>
 *   <li>Excludes {@code maxWeeklyWorkload} (not relevant for admin view)</li>
 *   <li>Changes {@code age} from int to String for flexible display formatting</li>
 * </ul>
 * </p>
 *
 * @author wangyue
 * @version 1.0.0
 * @since 2026/4/6
 * @see com.tars.entity.bean.TAProfile
 * @see com.tars.controller.AdminServlet
 */
@Data
public class TAProDTO {

    /** Unique TA profile identifier */
    private String id;

    /** ID of the associated User account */
    private String userId;

    /** Full name of the TA candidate */
    private String name;

    /** Gender */
    private String gender;

    /** Age as string for flexible formatting (e.g., "21", "N/A") */
    private String age;

    /** College or school affiliation */
    private String college;

    /** Major or field of study */
    private String major;

    /** Degree level (BACHELOR, MASTER, PHD) */
    private String degree;

    /** Academic year within current degree program */
    private int year;

    /** List of technical skills */
    private List<String> skills;

    /** Email address */
    private String email;

    /** Phone number */
    private String phone;

    /** Original resume filename */
    private String resumeName;

    /** Relative path to the resume file */
    private String resumePath;

    /** Timestamp when the profile was created */
    private Timestamp createAt;

    /** Timestamp when the profile was last updated */
    private Timestamp updateAt;
}
