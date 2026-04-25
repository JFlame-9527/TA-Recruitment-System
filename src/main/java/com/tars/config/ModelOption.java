package com.tars.config;

import lombok.Builder;
import lombok.Data;

/**
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 */
@Data
@Builder
public class ModelOption {

    private String model;

    private float temperature;

    private double topP;

    private int topK;

    private float repetitionPenalty;

    private int maxTokens;

    private int dimension;
}
