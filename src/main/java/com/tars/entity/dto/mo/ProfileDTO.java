package com.tars.entity.dto.mo;

import lombok.Data;

import java.util.List;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/5
 */
@Data
public class ProfileDTO {

    private String userId;

    private String appId;

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

    private String feedback;
}
