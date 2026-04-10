package com.tars.entity.dto.mo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author 477996850
 * @version 1.0.0
 * @since 2026/4/5
 */
@Data
public class ApplicationDTO {

    private String appId;

    private String proId;

    private String name;

    private String college;

    private String major;

    private String grade;

    private Timestamp applyAt;

    private int status; // 0-applied, 1-offered, 2-rejected
}
