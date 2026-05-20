package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private int age;

    private String college;

    private String major;

    private String degree; // BACHELOR, MASTER, PHD

    private int year;

    private int grade;

    private List<String> skills = new ArrayList<>();

    private String email;

    private String phone;

    private String resumeName;

    private String resumePath;

    private Timestamp createAt;

    private Timestamp updateAt;

    private String portraitId;

    private float maxWeeklyWorkload = 20.0f;

    public TAProfile() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }

    public void setDegree(String degree) {
        this.degree = degree;
        setGrade();
    }

    public void setYear(int year) {
        this.year = year;
        setGrade();
    }

    private void setGrade() {
        if (degree == null || degree.trim().isEmpty()) {
            this.grade = year;
            return;
        }
        
        int offset = switch (degree.toUpperCase()) {
            case "BACHELOR" -> 0;
            case "MASTER" -> 10;
            case "PHD" -> 20;
            default -> 0;
        };
        this.grade = year + offset;
    }
}
