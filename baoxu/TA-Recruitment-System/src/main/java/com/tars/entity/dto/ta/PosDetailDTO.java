package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/4/8
 */
@Data
public class PosDetailDTO {

    private String posId;

    private String title;

    private String description;

    private String moduleCode;

    private String moduleName;

    private List<String> skills;

    private float weeklyWorkload;

    private int duration;

    private int requiredNum;

    private int offeredNum;

    private Timestamp startDate;

    private Timestamp endDate;

    private Timestamp deadline;

    private Timestamp postDate;

    private int posStatus; // 0-opened, 1-filled, 2-closed, 3-withdrawn

    private int appStatus; // (-1)-not applied, 0-applied, 1-offered, 2-rejected, 3-withdrawn

    private String appId;

    private String feedback;

    private Timestamp applyAt;
}
