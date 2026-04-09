
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
 * Utility class to map HttpServletRequest parameters to Java beans
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/2
 */
@Slf4j
public class BeanUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Map request parameters to a bean object
     * Automatically handles type conversion for common types
     *
     * @param req   HttpServletRequest containing parameters
     * @param clazz The target bean class
     * @return Instantiated and populated bean object
     * @throws RuntimeException if instantiation fails
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
     * Map request parameters to a bean object with custom parameter name mapping
     * Allows mapping from different parameter names to bean properties
     *
     * @param req              HttpServletRequest containing parameters
     * @param clazz            The target bean class
     * @param paramNameMapping Map from parameter name to property name (e.g., {"user_id" -> "userId"})
     * @return Instantiated and populated bean object
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
     * Convert string array parameter values to the target type
     */
    @SuppressWarnings("unchecked")
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
     * Populate additional fields on an existing bean object
     * Useful when some fields are already set programmatically
     *
     * @param req  HttpServletRequest containing parameters
     * @param bean Existing bean object to populate
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
     * Merge source bean into target bean
     * Only copies non-null properties from source to target
     *
     * @param source Source bean (new data)
     * @param target Target bean (existing data to be updated)
     * @param ignoreFields Fields to ignore during merge
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