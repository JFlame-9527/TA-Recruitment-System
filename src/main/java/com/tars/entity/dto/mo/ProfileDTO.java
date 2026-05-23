package com.tars.entity.dto.mo;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for displaying TA applicant profile in MO portal.
 * <p>
 * This DTO provides comprehensive TA profile information for Module Owners to
 * review during the application evaluation process. It includes all academic
 * and contact information, plus the application-specific {@code feedback} field
 * for MOs to record their decision rationale.
 * </p>
 * <p>
 * <b>Usage:</b> Used in MO application review detail page to display a complete
 * view of a TA candidate's qualifications when reviewing their application.
 * </p>
 * <p>
 * <b>Differences from TA's ProfileDTO:</b>
 * <ul>
 *   <li>Includes {@code userId} for reference</li>
 *   <li>Includes {@code appId} to link to the specific application</li>
 *   <li>Includes {@code feedback} for MO decision recording</li>
 *   <li>Excludes {@code id} (profile ID not needed for review)</li>
 * </ul>
 * </p>
 *
 * @author 477996850
 * @version 1.0.0
 * @since 2026/4/5
 * @see com.tars.entity.bean.TAProfile
 * @see com.tars.entity.bean.Application
 * @see com.tars.controller.MOServlet
 */
@Data
public class ProfileDTO {

    /** TA user identifier */
    private String userId;

    /** Application identifier linking this profile to a specific application */
    private String appId;

    /** Applicant's full name */
    private String name;

    /** Gender */
    private String gender;

    /** Age as string for flexible formatting */
    private String age;

    /** College affiliation */
    private String college;

    /** Major */
    private String major;

    /** Degree level */
    private String degree;

    /** Academic year */
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

    /**
     * Feedback from MO regarding the application decision.
     * <p>
     * This field is used by MOs to record reasons for acceptance or rejection,
     * and is visible to the TA applicant after the decision is made.
     * </p>
     */
    private String feedback;
}
