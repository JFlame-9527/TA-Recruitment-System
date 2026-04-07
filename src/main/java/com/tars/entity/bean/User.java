package com.tars.entity.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/22
 */
@Data
public class User {

    private String id;

    private String name;

    private String password;

    private int role;

    private int status;

    private Timestamp createAt;

    private Timestamp updateAt;

    private Timestamp lastLoginAt;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.createAt = Timestamp.valueOf(LocalDateTime.now());
    }

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
}
