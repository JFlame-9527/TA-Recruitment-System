package com.tars.config;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 */
@Data
@SuperBuilder
public class ModelOption {
    private String model;

    private float temperature;

    private double topP;

    private int topK;

    private float repetitionPenalty;

    private int maxTokens;
}
