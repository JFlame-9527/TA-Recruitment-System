package com.tars.entity.dto.ta;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for TA profile display and editing in TA portal.
 * <p>
 * This DTO provides a simplified view of {@link com.tars.entity.bean.TAProfile} for
 * TA users to view and update their own profile information. It excludes internal
 * fields like {@code userId}, {@code grade}, {@code portraitId}, and timestamps
 * that are not relevant for user-facing operations.
 * </p>
 * <p>
 * <b>Usage:</b> Used in TA profile page ({@code /views/ta/profile.jsp}) for displaying
 * and updating personal information, academic background, skills, and resume.
 * </p>
 *
 * @author QiheSun
 * @version 1.0.0
 * @since 2026/3/20
 * @see com.tars.entity.bean.TAProfile
 * @see com.tars.controller.TAServlet
 */
@Data
public class ProfileDTO {

    /** Unique TA profile identifier */
    private String id;

    /** Full name of the TA candidate */
    private String name;

    /** Gender */
    private String gender;

    /** Age as string for flexible formatting */
    private String age;

    /** College or school affiliation */
    private String college;

    /** Major or field of study */
    private String major;

    /** Degree level (BACHELOR, MASTER, PHD) */
    private String degree; // BACHELOR, MASTER, PHD

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
}
