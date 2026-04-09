package com.tars.entity.dto.admin;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Yue Wang
 * @version 1.0.0
 * @since 2026/4/6
 */
@Data
public class TAProDTO {

    private String id;

    private String userId;

    private String name;

    private String gender;

    private String age;

    private String college;

    private String major;

    private String grade;

    private List<String> skills;

    private String email;

    private String phone;

    private String resumeName;

    private String resumePath;

    private Timestamp createAt;

    private Timestamp updateAt;
}
