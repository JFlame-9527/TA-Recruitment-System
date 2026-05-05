package com.tars.entity.dto.ta;

import lombok.Data;

import java.util.List;

/**
 * @author QiheSun
 * @version 1.0.0
 * @since 2026/3/20
 */
@Data
public class ProfileDTO {

    private String id;

    private String name;

    private String gender;

    private String age;

    private String college;

    private String major;

    private String degree;

    private int year;

    private List<String> skills;

    private String email;

    private String phone;

    private String resumeName;

    private String resumePath;
}
