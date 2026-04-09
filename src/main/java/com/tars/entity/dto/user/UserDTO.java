package com.tars.entity.dto.user;

import lombok.Data;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/24
 */
@Data
public class UserDTO{
    private String id;
    private String name;
    private int role;
    private int status;
}
