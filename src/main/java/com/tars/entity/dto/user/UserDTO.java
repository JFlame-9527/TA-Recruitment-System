package com.tars.entity.dto.user;

import lombok.Data;

/**
 * Data Transfer Object for user authentication and session management.
 * <p>
 * This lightweight DTO contains essential user information needed after successful
 * login, excluding sensitive fields like password. It is typically stored in the
 * HTTP session to maintain user state across requests.
 * </p>
 * <p>
 * <b>Usage:</b> Created during login process and stored in session for subsequent
 * request authentication and authorization checks.
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
 * <p>
 * <b>Security Note:</b> This DTO intentionally excludes the password field to prevent
 * accidental exposure of credentials in session data or logs.
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/24
 * @see com.tars.entity.bean.User
 * @see com.tars.controller.UserServlet
 * @see com.tars.service.UserService
 */
@Data
public class UserDTO {

    /** Unique user identifier */
    private String id;

    /** Login username */
    private String name;

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
}
