package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Represents a user account in the TA Recruitment System.
 * <p>
 * This is the core authentication and authorization entity that supports three roles:
 * <ul>
 *   <li><b>Admin (role=0)</b>: System administrator with full access</li>
 *   <li><b>Technical Assistant - TA (role=1)</b>: Student applicants and hired TAs</li>
 *   <li><b>Module Owner - MO (role=2)</b>: Faculty/staff who post positions and review applications</li>
 * </ul>
 * </p>
 * <p>
 * Each user may have an associated profile:
 * <ul>
 *   <li>TA users → {@link TAProfile}</li>
 *   <li>MO users → {@link MOProfile}</li>
 *   <li>Admin users → No additional profile required</li>
 * </ul>
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>{@code 0} - Available: Account is active and can log in</li>
 *   <li>{@code 1} - Frozen: Account is disabled (e.g., violation, inactive)</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/22
 * @see TAProfile
 * @see MOProfile
 */
@Data
public class User {

    /** Unique identifier for the user (UUID) */
    private String id;

    /** Login username (must be unique across all users) */
    private String name;

    /**
     * Encrypted password (MD5 hash).
     * <p>
     * Passwords are hashed using {@link org.apache.commons.codec.digest.DigestUtils#md5Hex(String)}
     * before storage. Plain text passwords are never stored.
     * </p>
     */
    private String password;

    /**
     * User role indicator.
     * <ul>
     *   <li>0 = Administrator</li>
     *   <li>1 = Technical Assistant (TA)</li>
     *   <li>2 = Module Owner (MO)</li>
     * </ul>
     */
    private int role;

    /**
     * Account status indicator.
     * <ul>
     *   <li>0 = Available (active)</li>
     *   <li>1 = Frozen (disabled)</li>
     * </ul>
     */
    private int status;

    /** Timestamp when the user account was created */
    private Timestamp createAt;

    /** Timestamp when the user account was last updated */
    private Timestamp updateAt;

    /** Timestamp of the most recent successful login (null if never logged in) */
    private Timestamp lastLoginAt;

    /**
     * Default constructor that initializes:
     * <ul>
     *   <li>Unique UUID-based ID</li>
     *   <li>Creation timestamp (current time)</li>
     * </ul>
     * <p>
     * Other fields should be set through setters before saving.
     * </p>
     */
    public User() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
    }

    /**
     * Constructor with explicit ID, name, and password.
     * <p>
     * Useful for creating users with predefined IDs (e.g., during data migration)
     * or for testing purposes.
     * </p>
     *
     * @param id       Custom user ID (should be UUID format)
     * @param name     Username for login
     * @param password Password (should be pre-hashed with MD5)
     */
    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
}
