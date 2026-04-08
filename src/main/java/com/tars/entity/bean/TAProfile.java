package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 */
@Data
public class TAProfile {

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

    public TAProfile() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
