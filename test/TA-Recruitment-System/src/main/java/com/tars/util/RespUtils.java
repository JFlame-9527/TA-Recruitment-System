package com.tars.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/4
 */
@Slf4j
public class RespUtils {
    /**
     * -- GETTER --
     *  Get ObjectMapper instance for custom serialization
     */
    @Getter
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // Register JavaTimeModule for LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Write success response with message
     */
    public static void writeSuccess(HttpServletResponse resp, String message) throws IOException {
        writeJson(resp, Msg.success(message), HttpServletResponse.SC_OK);
    }

    /**
     * Write success response with data
     */
    public static <T> void writeSuccess(HttpServletResponse resp, T data) throws IOException {
        writeJson(resp, Msg.success(data), HttpServletResponse.SC_OK);
    }

    /**
     * Write success response with data and message
     */
    public static <T> void writeSuccess(HttpServletResponse resp, T data, String message) throws IOException {
        writeJson(resp, Msg.success(data, message), HttpServletResponse.SC_OK);
    }

    /**
     * Write error response with message
     */
    public static void writeError(HttpServletResponse resp, String message) throws IOException {
        writeJson(resp, Msg.error(message), HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Write error response with status code
     */
    public static void writeError(HttpServletResponse resp, String message, int statusCode) throws IOException {
        writeJson(resp, Msg.error(message), statusCode);
    }

    /**
     * Write custom response
     */
    public static <T> void writeJson(HttpServletResponse resp, Msg<T> response, int statusCode) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(statusCode);
        String json = objectMapper.writeValueAsString(response);
        resp.getWriter().write(json);
        resp.getWriter().flush();
    }
}
