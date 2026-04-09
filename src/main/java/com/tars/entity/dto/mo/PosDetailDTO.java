package com.tars.entity.dto.mo;

import com.tars.entity.bean.Application;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/30
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

    private int appliedNum;

    private int rejectedNum;

    private Timestamp startDate;

    private Timestamp endDate;

    private Timestamp postDate;

    private Timestamp deadline;

    private int status; // 0-opened, 1-filled, 2-closed, 3-withdrawn
}
