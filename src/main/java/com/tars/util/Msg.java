package com.tars.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Msg<T> {

    private boolean success;

    private String message;

    private T data;

    /**
     * Create a success response with message only
     */
    public static <T> Msg<T> success(String message) {
        return new Msg<>(true, message, null);
    }

    /**
     * Create a success response with data and message
     */
    public static <T> Msg<T> success(T data, String message) {
        return new Msg<>(true, message, data);
    }

    /**
     * Create a success response with data only
     */
    public static <T> Msg<T> success(T data) {
        return new Msg<>(true, "success", data);
    }

    /**
     * Create an error response with message only
     */
    public static <T> Msg<T> error(String message) {
        return new Msg<>(false, message, null);
    }

    /**
     * Create an error response with data and message
     */
    public static <T> Msg<T> error(T data, String message) {
        return new Msg<>(false, message, data);
    }
}
