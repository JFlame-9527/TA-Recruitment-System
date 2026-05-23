package com.tars.entity.bean;

import lombok.Data;

import java.util.UUID;

/**
 * Represents a Module Owner (MO) profile containing personal and contact information.
 * <p>
 * Module Owners are faculty members or staff who:
 * <ul>
 *   <li>Create and manage TA positions for their courses/modules</li>
 *   <li>Review TA applications and make hiring decisions</li>
 *   <li>Provide feedback and guidance to TAs</li>
 * </ul>
 * </p>
 * <p>
 * This profile links to a {@link User} account (via {@code userId}) with role=2 (MO).
 * </p>
 *
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/23
 * @see User
 * @see Position
 */
@Data
public class MOProfile {

    /** Unique identifier for the MO profile (UUID) */
    private String id;

    /** ID of the associated User account (must have role=2 for MO) */
    private String userId;

    /** Full name of the Module Owner */
    private String name;

    /** College or department affiliation (e.g., "School of Computer Science") */
    private String college;

    /** Email address for contact and notifications */
    private String email;

    /** Phone number for urgent communications */
    private String phone;

    /**
     * Default constructor that generates a unique UUID-based ID.
     * <p>
     * Other fields should be populated through setters or during profile creation.
     * </p>
     */
    public MOProfile() {
        this.id = UUID.randomUUID().toString();
    }
}
