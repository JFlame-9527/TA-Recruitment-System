package com.tars.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper for standardized API responses.
 * <p>
 * This class provides a consistent structure for all HTTP responses in the application,
 * encapsulating success status, message, and optional data payload. It is used in
 * conjunction with {@link RespUtils} to send JSON responses from servlets.
 * </p>
 * <p>
 * <b>Response Structure:</b>
 * <pre>{@code
 * {
 *   "success": true/false,
 *   "message": "Operation result description",
 *   "data": {...} // Optional payload (can be any type)
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Usage Examples:</b>
 * <pre>{@code
 * // Success with message only
 * return Msg.success("Position created successfully");
 * 
 * // Success with data
 * List<Position> positions = positionService.getAll();
 * return Msg.success(positions);
 * 
 * // Success with data and custom message
 * return Msg.success(user, "Login successful");
 * 
 * // Error with message
 * return Msg.error("Invalid credentials");
 * 
 * // Error with data and message
 * return Msg.error(validationErrors, "Validation failed");
 * }</pre>
 * </p>
 * <p>
 * <b>Type Safety:</b> The generic type parameter {@code <T>} allows type-safe data payloads
 * while maintaining flexibility for different response types.
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/4
 * @param <T> The type of data payload (can be Object, List, Map, or any custom type)
 * @see RespUtils
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Msg<T> {

    /** Indicates whether the operation was successful */
    private boolean success;

    /** Human-readable message describing the operation result */
    private String message;

    /** Optional data payload (null for message-only responses) */
    private T data;

    /**
     * Creates a success response with message only (no data).
     * <p>
     * Use this for operations that don't return data, such as:
     * <ul>
     *   <li>Delete operations</li>
     *   <li>Status updates</li>
     *   <li>Simple confirmations</li>
     * </ul>
     * </p>
     *
     * @param message Success message to display
     * @param <T>     Generic type (inferred from context)
     * @return Success response with null data
     */
    public static <T> Msg<T> success(String message) {
        return new Msg<>(true, message, null);
    }

    /**
     * Creates a success response with both data and custom message.
     * <p>
     * Use this when you need to return data along with a descriptive message.
     * </p>
     *
     * @param data    Response data payload
     * @param message Success message to display
     * @param <T>     Type of the data payload
     * @return Success response with data and message
     */
    public static <T> Msg<T> success(T data, String message) {
        return new Msg<>(true, message, data);
    }

    /**
     * Creates a success response with data only (default message: "success").
     * <p>
     * This is a convenience method for common cases where a simple success message
     * is sufficient.
     * </p>
     *
     * @param data Response data payload
     * @param <T>  Type of the data payload
     * @return Success response with data and default message
     */
    public static <T> Msg<T> success(T data) {
        return new Msg<>(true, "success", data);
    }

    /**
     * Creates an error response with message only (no data).
     * <p>
     * Use this for error scenarios that don't require additional error details.
     * </p>
     *
     * @param message Error message describing what went wrong
     * @param <T>     Generic type (inferred from context)
     * @return Error response with null data
     */
    public static <T> Msg<T> error(String message) {
        return new Msg<>(false, message, null);
    }

    /**
     * Creates an error response with both data and message.
     * <p>
     * Use this when you need to provide error details along with the error message,
     * such as validation errors or structured error information.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Map<String, String> errors = new HashMap<>();
     * errors.put("email", "Invalid email format");
     * errors.put("password", "Password too short");
     * return Msg.error(errors, "Validation failed");
     * }</pre>
     * </p>
     *
     * @param data    Error details or validation errors
     * @param message Error message describing what went wrong
     * @param <T>     Type of the error data
     * @return Error response with error details
     */
    public static <T> Msg<T> error(T data, String message) {
        return new Msg<>(false, message, data);
    }
}
