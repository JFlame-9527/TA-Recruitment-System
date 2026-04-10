package com.tars.entity.dto.mo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/30
 */
@Data
public class PosBriefDTO {

    private String posId;

    private String title;

    private String moduleCode;

    private String moduleName;

    private int vacancyNum;

    private int pendingNum;

    private Timestamp postDate;

    private Timestamp deadline;

    private int status; // 0-opened, 1-filled, 2-closed, 3-withdrawn
}
