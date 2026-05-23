package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Technical Assistant (TA) candidate's detailed profile.
 * <p>
 * This entity stores comprehensive information about TA applicants, including:
 * <ul>
 *   <li>Personal information (name, gender, age, contact details)</li>
 *   <li>Academic background (college, major, degree, year, grade)</li>
 *   <li>Technical skills list</li>
 *   <li>Resume file information</li>
 *   <li>AI-generated portrait for position matching</li>
 *   <li>Availability constraints (max weekly workload)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Grade Calculation:</b>
 * The {@code grade} field is automatically calculated based on {@code degree} and {@code year}:
 * <ul>
 *   <li>Bachelor: grade = year (e.g., year 2 → grade 2)</li>
 *   <li>Master: grade = year + 10 (e.g., year 1 → grade 11)</li>
 *   <li>PhD: grade = year + 20 (e.g., year 3 → grade 23)</li>
 * </ul>
 * This unified grading system allows comparing students across different degree levels.
 * </p>
 * <p>
 * <b>Resume Storage:</b>
 * Resume files are stored as Markdown (.md) format in the {@code resumes/} directory.
 * The {@code resumePath} field stores the relative path, while {@code resumeName} stores
 * the original filename for display purposes.
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 * @see User
 * @see Portrait
 * @see Application
 */
@Data
public class TAProfile {

    /** Unique identifier for the TA profile (UUID) */
    private String id;

    /** ID of the associated User account (must have role=1 for TA) */
    private String userId;

    /** Full name of the TA candidate */
    private String name;

    /** Gender (e.g., "Male", "Female", "Other") */
    private String gender;

    /** Age in years */
    private int age;

    /** College or school affiliation (e.g., "School of Computer Science") */
    private String college;

    /** Major or field of study (e.g., "Software Engineering", "Computer Science") */
    private String major;

    /**
     * Degree level.
     * <ul>
     *   <li>BACHELOR - Undergraduate student</li>
     *   <li>MASTER - Graduate student</li>
     *   <li>PHD - Doctoral student</li>
     * </ul>
     */
    private String degree; // BACHELOR, MASTER, PHD

    /** Academic year within current degree program (e.g., 1, 2, 3, 4) */
    private int year;

    /**
     * Unified grade value calculated from degree and year.
     * <p>
     * Formula: grade = year + offset (Bachelor=0, Master=10, PhD=20)
     * </p>
     *
     * @see #setDegree(String)
     * @see #setYear(int)
     */
    private int grade;

    /** List of technical skills (e.g., ["Java", "Python", "Spring Boot"]) */
    private List<String> skills = new ArrayList<>();

    /** Email address for contact and notifications */
    private String email;

    /** Phone number for urgent communications */
    private String phone;

    /** Original resume filename (for display purposes) */
    private String resumeName;

    /**
     * Relative path to the resume file in Markdown format.
     * <p>
     * Example: {@code "resumes/resume_john_abc123.md"}
     * </p>
     *
     * @see com.tars.ai.PortraitGenerator
     */
    private String resumePath;

    /** Timestamp when this profile was created */
    private Timestamp createAt;

    /** Timestamp when this profile was last updated */
    private Timestamp updateAt;

    /** ID of the AI-generated portrait for this TA (used for position matching) */
    private String portraitId;

    /** Maximum weekly workload the TA can commit to (in hours, default: 20.0) */
    private float maxWeeklyWorkload = 20.0f;

    /**
     * Default constructor that initializes:
     * <ul>
     *   <li>Unique UUID-based ID</li>
     *   <li>Creation timestamp (current time)</li>
     *   <li>Update timestamp (current time)</li>
     *   <li>Empty skills list</li>
     * </ul>
     */
    public TAProfile() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }

    /**
     * Sets the degree level and automatically recalculates the grade.
     * <p>
     * This setter ensures that the grade field stays synchronized with degree changes.
     * </p>
     *
     * @param degree Degree level (BACHELOR, MASTER, or PHD)
     * @see #setGrade()
     */
    public void setDegree(String degree) {
        this.degree = degree;
        setGrade();
    }

    /**
     * Sets the academic year and automatically recalculates the grade.
     * <p>
     * This setter ensures that the grade field stays synchronized with year changes.
     * </p>
     *
     * @param year Academic year (typically 1-4 for Bachelor, 1-3 for Master/PhD)
     * @see #setGrade()
     */
    public void setYear(int year) {
        this.year = year;
        setGrade();
    }

    /**
     * Calculates and sets the unified grade value based on degree and year.
     * <p>
     * Grade calculation formula:
     * <pre>
     * Bachelor: grade = year + 0  (range: 1-4)
     * Master:   grade = year + 10 (range: 11-13)
     * PhD:      grade = year + 20 (range: 21-24)
     * </pre>
     * If degree is null or empty, defaults to Bachelor (offset=0).
     * </p>
     */
    private void setGrade() {
        if (degree == null || degree.trim().isEmpty()) {
            this.grade = year;
            return;
        }
        
        int offset = switch (degree.toUpperCase()) {
            case "MASTER" -> 10;
            case "PHD" -> 20;
            default -> 0;
        };
        this.grade = year + offset;
    }
}
