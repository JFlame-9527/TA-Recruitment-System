package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/3/23
 */
@Data
public class Application {

    private String id;

    private String positionId;

    private String userId;

    private Timestamp applyAt;

    private int status; // 0-applied, 1-offered, 2-rejected, 3-withdrawn

    private String feedback;

    public Application() {
        this.id = UUID.randomUUID().toString();
        this.applyAt = Timestamp.valueOf(LocalDateTime.now());
    }

    public Application(int status) {
        this.status = status;
    }
}
