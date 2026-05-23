package com.tars.entity.dto.admin;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Data Transfer Object for displaying detailed user information in admin panel.
 * <p>
 * This DTO extends the basic {@link com.tars.entity.bean.User} information by including
 * the associated profile ID, providing a complete view for administrative operations.
 * </p>
 * <p>
 * <b>Usage:</b> Used in admin user management pages to display and manage all user accounts
 * across different roles (Admin, TA, MO).
 * </p>
 * <p>
 * <b>Role Values:</b>
 * <ul>
 *   <li>0 = Administrator</li>
 *   <li>1 = Technical Assistant (TA)</li>
 *   <li>2 = Module Owner (MO)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Status Values:</b>
 * <ul>
 *   <li>0 = Available (active account)</li>
 *   <li>1 = Frozen (disabled account)</li>
 * </ul>
 * </p>
 *
 * @author wangyue
 * @version 1.0.0
 * @since 2026/4/6
 * @see com.tars.entity.bean.User
 * @see com.tars.controller.AdminServlet
 */
@Data
public class UserDetailDTO {

    /** Unique user identifier */
    private String userId;

    /** Login username */
    private String name;

    /**
     * Encrypted password (MD5 hash).
     * <p>
     * <b>Security Note:</b> This field should not be exposed in API responses
     * unless specifically needed for administrative purposes.
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
    private int role; // 1-TA, 2-MO

    /**
     * Account status indicator.
     * <ul>
     *   <li>0 = Available (active)</li>
     *   <li>1 = Frozen (disabled)</li>
     * </ul>
     */
    private int status; // 0-available, 1-frozen

    /** Timestamp when the user account was created */
    private Timestamp createAt;

    /** Timestamp when the user account was last updated */
    private Timestamp updateAt;

    /** Timestamp of the most recent successful login */
    private Timestamp lastLoginAt;

    /**
     * Associated profile ID.
     * <ul>
     *   <li>For TA users: References {@link com.tars.entity.bean.TAProfile} ID</li>
     *   <li>For MO users: References {@link com.tars.entity.bean.MOProfile} ID</li>
     *   <li>For Admin users: May be null</li>
     * </ul>
     */
    private String proId;
}
