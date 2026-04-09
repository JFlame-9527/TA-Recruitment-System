package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/4/7
 */
@Data
public class PosBriefDTO {

    private String posId;

    private String title;

    private String moduleCode;

    private String moduleName;

    private float weeklyWorkload;

    private int duration;

    private int requiredNum;

    private Timestamp postDate;

    private Timestamp deadline;

    private int posStatus; // 0-opened, 1-filled, 2-closed, 3-withdrawn

    private int appStatus; // (-1)-not applied, 0-applied, 1-offered, 2-rejected, 3-withdrawn

    private String appId;
}
