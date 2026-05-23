package com.tars.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Utility class for writing standardized JSON responses to HTTP clients.
 * <p>
 * This class simplifies the process of sending JSON responses from servlets by:
 * <ul>
 *   <li>Providing convenience methods for success and error responses</li>
 *   <li>Automatically setting content type and character encoding</li>
 *   <li>Handling JSON serialization with proper date/time formatting</li>
 *   <li>Wrapping responses in {@link Msg} objects for consistency</li>
 * </ul>
 * </p>
 * <p>
 * <b>JSON Serialization:</b> Uses Jackson ObjectMapper configured with:
 * <ul>
 *   <li>JavaTimeModule for LocalDateTime/Timestamp support</li>
 *   <li>Dates serialized as ISO-8601 strings (not timestamps)</li>
 *   <li>UTF-8 character encoding</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage Examples:</b>
 * <pre>{@code
 * // In a Servlet's doGet/doPost method:
 * 
 * // Success with message
 * RespUtils.writeSuccess(response, "Position created");
 * 
 * // Success with data
 * List<Position> positions = positionService.getAll();
 * RespUtils.writeSuccess(response, positions);
 * 
 * // Success with data and custom message
 * RespUtils.writeSuccess(response, user, "Login successful");
 * 
 * // Error with message (returns 400 Bad Request)
 * RespUtils.writeError(response, "Invalid input");
 * 
 * // Error with custom status code
 * RespUtils.writeError(response, "Not found", HttpServletResponse.SC_NOT_FOUND);
 * 
 * // Custom response with specific status
 * Msg<List<Position>> msg = Msg.success(positions, "Found 5 positions");
 * RespUtils.writeJson(response, msg, HttpServletResponse.SC_OK);
 * }</pre>
 * </p>
 * <p>
 * <b>HTTP Status Codes:</b>
 * <ul>
 *   <li>Success methods: Always use 200 OK</li>
 *   <li>Error methods: Default to 400 Bad Request, customizable</li>
 *   <li>Custom method: Accepts any valid HTTP status code</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/4
 * @see Msg
 * @see ObjectMapper
 * @see HttpServletResponse
 */
@Slf4j
public class RespUtils {

    /**
     * Shared ObjectMapper instance configured for Java 8 time types.
     * <p>
     * Configuration:
     * <ul>
     *   <li>Registered JavaTimeModule for LocalDateTime/Timestamp serialization</li>
     *   <li>Disabled WRITE_DATES_AS_TIMESTAMPS (dates as ISO-8601 strings)</li>
     *   <li>Thread-safe: ObjectMapper is immutable after configuration</li>
     * </ul>
     * </p>
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
     * Writes a success response with message only (no data).
     * <p>
     * Sets HTTP status to 200 OK and returns a JSON response with success=true.
     * </p>
     *
     * @param resp    HttpServletResponse to write the response to
     * @param message Success message to include in the response
     * @throws IOException if writing to response fails
     * @see Msg#success(String)
     */
    public static void writeSuccess(HttpServletResponse resp, String message) throws IOException {
        writeJson(resp, Msg.success(message), HttpServletResponse.SC_OK);
    }

    /**
     * Writes a success response with data payload.
     * <p>
     * Sets HTTP status to 200 OK and serializes the provided data into the response.
     * Uses default message "success".
     * </p>
     *
     * @param resp HttpServletResponse to write the response to
     * @param data Data payload to serialize (can be Object, List, Map, etc.)
     * @param <T>  Type of the data payload
     * @throws IOException if writing to response fails
     * @see Msg#success(Object)
     */
    public static <T> void writeSuccess(HttpServletResponse resp, T data) throws IOException {
        writeJson(resp, Msg.success(data), HttpServletResponse.SC_OK);
    }

    /**
     * Writes a success response with both data and custom message.
     * <p>
     * Sets HTTP status to 200 OK and includes both data and message in the response.
     * </p>
     *
     * @param resp    HttpServletResponse to write the response to
     * @param data    Data payload to serialize
     * @param message Custom success message
     * @param <T>     Type of the data payload
     * @throws IOException if writing to response fails
     * @see Msg#success(Object, String)
     */
    public static <T> void writeSuccess(HttpServletResponse resp, T data, String message) throws IOException {
        writeJson(resp, Msg.success(data, message), HttpServletResponse.SC_OK);
    }

    /**
     * Writes an error response with message only (default status: 400 Bad Request).
     * <p>
     * Sets HTTP status to 400 and returns a JSON response with success=false.
     * Use this for client-side errors like validation failures or invalid input.
     * </p>
     *
     * @param resp    HttpServletResponse to write the response to
     * @param message Error message describing the issue
     * @throws IOException if writing to response fails
     * @see Msg#error(String)
     */
    public static void writeError(HttpServletResponse resp, String message) throws IOException {
        writeJson(resp, Msg.error(message), HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Writes an error response with custom HTTP status code.
     * <p>
     * Allows specifying any HTTP status code for the error response.
     * Common codes:
     * <ul>
     *   <li>400 - Bad Request (validation errors)</li>
     *   <li>401 - Unauthorized (authentication required)</li>
     *   <li>403 - Forbidden (insufficient permissions)</li>
     *   <li>404 - Not Found (resource doesn't exist)</li>
     *   <li>500 - Internal Server Error (unexpected exceptions)</li>
     * </ul>
     * </p>
     *
     * @param resp       HttpServletResponse to write the response to
     * @param message    Error message describing the issue
     * @param statusCode HTTP status code to set
     * @throws IOException if writing to response fails
     * @see HttpServletResponse#SC_BAD_REQUEST
     * @see HttpServletResponse#SC_UNAUTHORIZED
     * @see HttpServletResponse#SC_FORBIDDEN
     * @see HttpServletResponse#SC_NOT_FOUND
     * @see HttpServletResponse#SC_INTERNAL_SERVER_ERROR
     */
    public static void writeError(HttpServletResponse resp, String message, int statusCode) throws IOException {
        writeJson(resp, Msg.error(message), statusCode);
    }

    /**
     * Writes an error response with error details, message, and custom status code.
     * <p>
     * Use this when you need to provide structured error information along with
     * the error message and specific HTTP status code.
     * </p>
     *
     * @param resp       HttpServletResponse to write the response to
     * @param data       Error details or validation errors
     * @param message    Error message describing the issue
     * @param statusCode HTTP status code to set
     * @param <T>        Type of the error data
     * @throws IOException if writing to response fails
     * @see Msg#error(Object, String)
     */
    public static <T> void writeError(HttpServletResponse resp, T data, String message, int statusCode) throws IOException {
        writeJson(resp, Msg.error(data, message), statusCode);
    }

    /**
     * Writes a custom JSON response with specified Msg object and HTTP status code.
     * <p>
     * This is the core method used by all other write methods. It handles:
     * <ol>
     *   <li>Setting content type to application/json with UTF-8 encoding</li>
     *   <li>Setting the HTTP status code</li>
     *   <li>Serializing the Msg object to JSON using Jackson</li>
     *   <li>Writing the JSON string to the response writer</li>
     *   <li>Flushing the writer to ensure immediate delivery</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> This method should be called only once per request. Calling it
     * multiple times will result in IllegalStateException.
     * </p>
     *
     * @param resp       HttpServletResponse to write the response to
     * @param response   Msg object containing success status, message, and optional data
     * @param statusCode HTTP status code to set
     * @param <T>        Type of the data payload in the Msg object
     * @throws IOException if writing to response fails or JSON serialization fails
     * @see #writeSuccess(HttpServletResponse, String)
     * @see #writeError(HttpServletResponse, String)
     */
    public static <T> void writeJson(HttpServletResponse resp, Msg<T> response, int statusCode) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(statusCode);
        String json = objectMapper.writeValueAsString(response);
        resp.getWriter().write(json);
        resp.getWriter().flush();
    }
}
