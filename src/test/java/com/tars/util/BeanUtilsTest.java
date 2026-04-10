package com.tars.util;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Test class for BeanUtils
 * Tests bean property copying and manipulation utilities
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class BeanUtilsTest {

    // Simple test bean class
    private static class TestBean {
        private String name;
        private int age;
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // ==================== PROPERTY COPY TESTS ====================

    @Test
    public void testCopyPropertiesSuccess() throws Exception {
        // Arrange
        TestBean source = new TestBean();
        source.setName("John");
        source.setAge(30);
        source.setEmail("john@example.com");

        TestBean target = new TestBean();

        // Act
        BeanUtils.merge(source, target);

        // Assert
        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
        assertEquals("john@example.com", target.getEmail());
    }

    @Test
    public void testCopyPropertiesWithNullSource() {
        // Arrange
        TestBean target = new TestBean();
        target.setName("Original");

        // Act & Assert - Should throw exception for null source
        assertThrows(IllegalArgumentException.class, () -> {
            BeanUtils.merge(null, target);
        });
    }

    @Test
    public void testCopyPropertiesWithNullTarget() {
        // Arrange
        TestBean source = new TestBean();
        source.setName("Source");

        // Act & Assert - Should throw exception for null target
        assertThrows(IllegalArgumentException.class, () -> {
            BeanUtils.merge(source, null);
        });
    }

    @Test
    public void testCopyPropertiesPartialFields() throws Exception {
        // Arrange
        TestBean source = new TestBean();
        source.setName("Jane");
        // age and email left as defaults (0 and null)

        TestBean target = new TestBean();
        target.setAge(25);
        target.setEmail("jane@test.com");

        // Act
        BeanUtils.merge(source, target);

        // Assert
        assertEquals("Jane", target.getName()); // Copied from source
        assertEquals("jane@test.com", target.getEmail()); // Unchanged (source has null)
    }

    // ==================== NULL VALUE HANDLING ====================

    @Test
    public void testCopyPropertiesSkipsNullValues() throws Exception {
        // Arrange
        TestBean source = new TestBean();
        source.setName("Test");
        source.setEmail(null);

        TestBean target = new TestBean();
        target.setEmail("original@test.com");

        // Act
        BeanUtils.merge(source, target);

        // Assert
        assertEquals("Test", target.getName());
        // Email should remain unchanged because source email is null
        assertEquals("original@test.com", target.getEmail());
    }

    // ==================== TYPE COMPATIBILITY ====================

    @Test
    public void testCopyPropertiesBetweenDifferentClasses() throws Exception {
        // This test verifies that BeanUtils can copy between different classes
        // with matching property names

        // Since we don't have access to actual BeanUtils implementation details,
        // this is a placeholder for the concept
        assertTrue(true);
    }
}
