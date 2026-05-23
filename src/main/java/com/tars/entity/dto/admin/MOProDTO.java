package com.tars.entity.dto.admin;

import lombok.Data;

/**
 * Data Transfer Object for displaying Module Owner (MO) profile information in admin panel.
 * <p>
 * This DTO provides a simplified view of {@link com.tars.entity.bean.MOProfile} for
 * administrative purposes, containing only essential contact and identification information.
 * </p>
 * <p>
 * <b>Usage:</b> Used in admin MO management pages to display Module Owner profiles,
 * verify contact information, and manage MO accounts.
 * </p>
 * <p>
 * <b>Note:</b> This DTO is identical to {@link com.tars.entity.bean.MOProfile} in structure,
 * but serves as a dedicated transfer object to decouple the admin API from the domain model.
 * </p>
 *
 * @author wangyue
 * @version 1.0.0
 * @since 2026/4/6
 * @see com.tars.entity.bean.MOProfile
 * @see com.tars.controller.AdminServlet
 */
@Data
public class MOProDTO {

    /** Unique MO profile identifier */
    private String id;

    /** ID of the associated User account (must have role=2 for MO) */
    private String userId;

    /** Full name of the Module Owner */
    private String name;

    /** College or department affiliation */
    private String college;

    /** Email address for contact */
    private String email;

    /** Phone number for urgent communications */
    private String phone;
}
