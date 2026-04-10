package com.tars.entity.bean;

import lombok.Data;

import java.util.UUID;

/**
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/23
 */
@Data
public class MOProfile {

    private String id;

    private String userId;

    private String name;

    private String college;

    private String email;

    private String phone;

    public MOProfile() {
        this.id = UUID.randomUUID().toString();
    }
}
