package com.tars.entity.dto.ta;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/4/18
 */
@Data
public class AppPosDTO {

    private String appId;

    private String posId;

    private String title;

    private String moduleCode;

    private String moduleName;

    private int offeredNum;

    private int requiredNum;

    private int status; // 0-applied, 1-offered, 2-rejected, 3-withdrawn

    private Timestamp applyAt;
}
