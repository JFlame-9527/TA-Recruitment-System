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
public class Position {

    private String id;

    private String title;

    private String description;

    private String moduleCode;

    private String moduleName;

    private List<String> skills;

    private String postUserId;

    private float weeklyWorkload;

    private int duration;

    private int requiredNum;

    private int offeredNum;

    private int appliedNum;

    private int rejectedNum;

    private Timestamp startDate;

    private Timestamp endDate;

    private Timestamp postDate;

    private Timestamp deadline;

    private Timestamp createAt;

    private Timestamp updateAt;

    private int status; // 0-opened, 1-filled, 2-closed, 3-withdrawn

    public Position() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
