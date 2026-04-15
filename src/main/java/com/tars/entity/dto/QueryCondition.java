package com.tars.entity.dto;

import lombok.Data;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/8
 */
@Data
public class QueryCondition {

    private String filter;

    private String order;

    private String key;

    private String search;

    private Integer page;
}
