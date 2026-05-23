
package com.tars.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class for mapping HTTP request parameters to Java beans with automatic type conversion.
 * <p>
 * This class provides reflection-based utilities to simplify the process of populating Java objects
 * from HttpServletRequest parameters. It handles:
 * <ul>
 *   <li>Automatic type conversion for common types (String, Integer, Long, Double, Float, Boolean)</li>
 *   <li>Date and time parsing (Timestamp, LocalDateTime, Date) using standard formats</li>
 *   <li>Collection types (List, Set) from multi-value parameters</li>
 *   <li>Custom parameter name mapping for mismatched field names</li>
 *   <li>Merging source bean properties into target bean (non-null only)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Supported Date Formats:</b>
 * <ul>
 *   <li>Date-Time: {@code "yyyy-MM-dd HH:mm:ss"} (e.g., "2026-05-23 14:30:00")</li>
 *   <li>Date Only: {@code "yyyy-MM-dd"} (e.g., "2026-05-23")</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage Examples:</b>
 * <pre>{@code
 * // Basic usage - auto-matching parameter names
 * Position position = BeanUtils.mapFromReq(request, Position.class);
 * 
 * // Custom parameter name mapping
 * Map<String, String> mapping = Map.of("user_id", "userId", "pos_id", "positionId");
 * Application app = BeanUtils.mapFromReq(request, Application.class, mapping);
 * 
 * // Populate existing bean (preserves pre-set fields)
 * TAProfile profile = new TAProfile();
 * profile.setUserId(currentUserId);
 * BeanUtils.populateFromReq(request, profile);
 * 
 * // Merge two beans (only non-null properties)
 * BeanUtils.merge(updatedProfile, existingProfile, "id", "createAt");
 * }</pre>
 * </p>
 * <p>
 * <b>Error Handling:</b> All methods throw {@link RuntimeException} on failure with detailed
 * error messages logged via SLF4J. Type conversion errors will cause the entire operation to fail.
 * </p>
 *
 * @author Jflame 477996850
 * @version 1.0.0
 * @since 2026/4/2
 * @see HttpServletRequest
 * @see PropertyDescriptor
 */
@Slf4j
public class BeanUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Maps HTTP request parameters to a new bean instance with automatic type conversion.
     * <p>
     * This method creates a new instance of the specified class and populates its properties
     * by matching request parameter names to bean property names. Only parameters that match
     * existing properties are set; others are ignored.
     * </p>
     * <p>
     * <b>Process:</b>
     * <ol>
     *   <li>Creates a new instance using the default constructor</li>
     *   <li>Introspects the bean to discover all readable/writable properties</li>
     *   <li>For each property, checks if a matching request parameter exists</li>
     *   <li>Converts the parameter value to the property's type</li>
     *   <li>Sets the converted value via the property's setter method</li>
     * </ol>
     * </p>
     * <p>
     * <b>Type Conversion:</b> Handled by {@link #convertValue(String[], Class)}. Supports:
     * <ul>
     *   <li>Primitive types: int, long, double, float, boolean</li>
     *   <li>Wrapper types: Integer, Long, Double, Float, Boolean</li>
     *   <li>Collections: List (from multi-value params), Set (from multi-value params)</li>
     *   <li>Date/Time: Timestamp, LocalDateTime, java.sql.Date, java.util.Date</li>
     * </ul>
     * </p>
     *
     * @param req   HttpServletRequest containing form parameters
     * @param clazz The target bean class (must have a public no-arg constructor)
     * @param <T>   The bean type
     * @return A new bean instance with populated properties
     * @throws RuntimeException if instantiation fails or type conversion encounters errors
     * @see #convertValue(String[], Class)
     */
    public static <T> T mapFromReq(HttpServletRequest req, Class<T> clazz) {
        try {
            T bean = clazz.getDeclaredConstructor().newInstance();

            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();

            for (PropertyDescriptor pd : propertyDescriptors) {
                String propertyName = pd.getName();

                if ("class".equals(propertyName)) {
                    continue;
                }

                if (req.getParameterMap().containsKey(propertyName)) {
                    String[] values = req.getParameterValues(propertyName);
                    Object convertedValue = convertValue(values, pd.getPropertyType());

                    if (convertedValue != null) {
                        pd.getWriteMethod().invoke(bean, convertedValue);
                    }
                }
            }

            return bean;
        } catch (InstantiationException | IllegalAccessException | IntrospectionException |
                 InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to map request parameters to bean: {}", clazz.getSimpleName(), e);
            throw new RuntimeException("Failed to map request parameters to bean", e);
        }
    }

    /**
     * Maps HTTP request parameters to a new bean instance with custom parameter name mapping.
     * <p>
     * This overload allows mapping request parameters with different names to bean properties.
     * Useful when HTML form field names don't match Java property naming conventions.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * // HTML form has: <input name="user_id"> <input name="pos_id">
     * // Java bean has: userId, positionId
     * Map<String, String> mapping = new HashMap<>();
     * mapping.put("userId", "user_id");
     * mapping.put("positionId", "pos_id");
     * 
     * MyBean bean = BeanUtils.mapFromReq(request, MyBean.class, mapping);
     * }</pre>
     * </p>
     *
     * @param req              HttpServletRequest containing form parameters
     * @param clazz            The target bean class
     * @param paramNameMapping Map from bean property name to request parameter name
     *                         (e.g., {"userId" -> "user_id"})
     * @param <T>              The bean type
     * @return A new bean instance with populated properties
     * @throws RuntimeException if instantiation fails or type conversion encounters errors
     * @see #mapFromReq(HttpServletRequest, Class)
     */
    public static <T> T mapFromReq(HttpServletRequest req, Class<T> clazz, Map<String, String> paramNameMapping) {
        try {
            T bean = clazz.getDeclaredConstructor().newInstance();

            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();

            for (PropertyDescriptor pd : propertyDescriptors) {
                String propertyName = pd.getName();

                if ("class".equals(propertyName)) {
                    continue;
                }

                String paramName = paramNameMapping.getOrDefault(propertyName, propertyName);

                if (req.getParameterMap().containsKey(paramName)) {
                    String[] values = req.getParameterValues(paramName);
                    Object convertedValue = convertValue(values, pd.getPropertyType());

                    if (convertedValue != null) {
                        pd.getWriteMethod().invoke(bean, convertedValue);
                    }
                }
            }

            return bean;
        } catch (InstantiationException | IllegalAccessException | IntrospectionException |
                 InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to map request parameters to bean: {}", clazz.getSimpleName(), e);
            throw new RuntimeException("Failed to map request parameters to bean", e);
        }
    }

    /**
     * Converts string array parameter values to the specified target type.
     * <p>
     * This internal method handles type conversion for all supported data types.
     * It takes the first value from the array for single-value types, or all values
     * for collection types (List, Set).
     * </p>
     * <p>
     * <b>Conversion Rules:</b>
     * <ul>
     *   <li>Null or empty values → returns null</li>
     *   <li>String → direct assignment</li>
     *   <li>Numeric types → uses {@code valueOf()} methods</li>
     *   <li>Boolean → uses {@code Boolean.valueOf()} (case-insensitive)</li>
     *   <li>List/Set → converts entire array to collection</li>
     *   <li>Date/Time → parses using predefined formatters</li>
     *   <li>Unsupported types → logs warning and returns raw string</li>
     * </ul>
     * </p>
     *
     * @param values     Array of string values from request parameters
     * @param targetType The desired target type
     * @return Converted object or null if input is empty
     * @throws NumberFormatException if numeric conversion fails
     * @throws IllegalArgumentException if date parsing fails
     */
    private static Object convertValue(String[] values, Class<?> targetType) {
        if (values == null || values.length == 0 || values[0] == null || values[0].trim().isEmpty()) {
            return null;
        }

        String value = values[0];

        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(value);
        } else if (targetType == Float.class || targetType == float.class) {
            return Float.valueOf(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value);
        } else if (targetType == List.class) {
            return Arrays.asList(values);
        } else if (targetType == Set.class) {
            return new HashSet<>(Arrays.asList(values));
        } else if (targetType == Timestamp.class) {
            return Timestamp.valueOf(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
        } else if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } else if (targetType == java.sql.Date.class) {
            return java.sql.Date.valueOf(String.valueOf(LocalDateTime.parse(value, DATE_FORMATTER)));
        } else if (targetType == Date.class) {
            return Timestamp.valueOf(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
        }

        log.warn("Unsupported target type: {}", targetType.getName());
        return value;
    }

    /**
     * Populates an existing bean instance with HTTP request parameters.
     * <p>
     * Unlike {@link #mapFromReq(HttpServletRequest, Class)}, this method works with
     * an already-instantiated bean, allowing you to set some properties programmatically
     * before populating the rest from request parameters.
     * </p>
     * <p>
     * <b>Use Case:</b> When certain fields should not come from user input (e.g., userId
     * from session, timestamps from server time):
     * <pre>{@code
     * TAProfile profile = new TAProfile();
     * profile.setUserId(session.getAttribute("userId").toString());
     * profile.setCreateAt(Timestamp.valueOf(LocalDateTime.now()));
     * 
     * // Only populate editable fields from form
     * BeanUtils.populateFromReq(request, profile);
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing form parameters
     * @param bean Existing bean instance to populate
     * @throws RuntimeException if property access fails
     * @see #mapFromReq(HttpServletRequest, Class)
     */
    public static void populateFromReq(HttpServletRequest req, Object bean) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();

            for (PropertyDescriptor pd : propertyDescriptors) {
                String propertyName = pd.getName();

                if ("class".equals(propertyName)) {
                    continue;
                }

                if (req.getParameterMap().containsKey(propertyName)) {
                    String[] values = req.getParameterValues(propertyName);
                    Object convertedValue = convertValue(values, pd.getPropertyType());

                    if (convertedValue != null) {
                        pd.getWriteMethod().invoke(bean, convertedValue);
                    }
                }
            }
        } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
            log.error("Failed to populate bean: {}", bean.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to populate bean", e);
        }
    }

    /**
     * Merges properties from a source bean into a target bean, copying only non-null values.
     * <p>
     * This method performs a shallow merge where:
     * <ul>
     *   <li>Only non-null properties from source are copied to target</li>
     *   <li>Empty strings (trimmed) are treated as null and skipped</li>
     *   <li>Specified fields can be excluded from merging</li>
     *   <li>Target properties retain their values if source has null</li>
     * </ul>
     * </p>
     * <p>
     * <b>Use Cases:</b>
     * <ul>
     *   <li>Partial updates: User submits only changed fields</li>
     *   <li>Patch operations: Update specific properties without replacing entire object</li>
     *   <li>Data migration: Copy valid data while preserving existing values</li>
     * </ul>
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * // User updates only email and phone
     * TAProfile updatedForm = BeanUtils.mapFromReq(request, TAProfile.class);
     * TAProfile existingProfile = taService.getById(userId);
     * 
     * // Merge only non-null fields, preserve id and timestamps
     * BeanUtils.merge(updatedForm, existingProfile, "id", "createAt", "updateAt");
     * }</pre>
     * </p>
     *
     * @param source       Source bean containing new data (null values are skipped)
     * @param target       Target bean to be updated (existing data)
     * @param ignoreFields Variable-length list of property names to exclude from merging
     * @param <T>          The bean type (source and target must be same type)
     * @throws IllegalArgumentException if source or target is null
     * @throws RuntimeException         if property access fails
     */
    public static <T> void merge(T source, T target, String... ignoreFields) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target cannot be null");
        }

        List<String> ignoreList = ignoreFields != null ?
                Arrays.asList(ignoreFields) : Collections.emptyList();

        try {
            PropertyDescriptor[] propertyDescriptors =
                    Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();

            for (PropertyDescriptor pd : propertyDescriptors) {
                String propertyName = pd.getName();

                // Skip 'class' and ignored fields
                if ("class".equals(propertyName) || ignoreList.contains(propertyName)) {
                    continue;
                }

                Method readMethod = pd.getReadMethod();
                Method writeMethod = pd.getWriteMethod();

                if (readMethod == null || writeMethod == null) {
                    continue;
                }

                // Get value from source
                Object sourceValue = readMethod.invoke(source);

                // Only copy if source value is not null
                if (sourceValue != null) {
                    // For String type, also check if it's not empty
                    if (sourceValue instanceof String &&
                            ((String) sourceValue).trim().isEmpty()) {
                        continue;
                    }

                    // Copy to target
                    writeMethod.invoke(target, sourceValue);
                    log.debug("Merged property: {} = {}", propertyName, sourceValue);
                }
            }

            log.info("Bean merge completed successfully");

        } catch (Exception e) {
            log.error("Failed to merge beans", e);
            throw new RuntimeException("Failed to merge beans", e);
        }
    }
}
