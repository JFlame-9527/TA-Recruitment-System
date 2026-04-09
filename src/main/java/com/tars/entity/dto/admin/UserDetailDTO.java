package com.tars.entity.dto.admin;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author Yue Wang
 * @version 1.0.0
 * @since 2026/4/6
 */
@Data
public class UserDetailDTO {

    private String userId;

    private String name;

    private String password;

    private int role; // 1-TA, 2-MO

    private int status; // 0-available, 1-frozen

    private Timestamp createAt;

    private Timestamp updateAt;

    private Timestamp lastLoginAt;

    private String proId;
}
