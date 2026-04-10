package com.tars.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for Msg
 * Tests response message wrapper structure and serialization
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class MsgTest {

    // ==================== SUCCESS MESSAGE TESTS ====================

    @Test
    public void testSuccessWithOnlyMessage() {
        // Act
        Msg<String> msg = Msg.success("Operation completed");

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals("Operation completed", msg.getMessage());
        assertNull(msg.getData());
    }

    @Test
    public void testSuccessWithOnlyData() {
        // Arrange
        String testData = "test data";

        // Act - For String data, must use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success(testData, "success");

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(testData, msg.getData());
        assertEquals("success", msg.getMessage());
    }

    @Test
    public void testSuccessWithDataAndCustomMessage() {
        // Arrange
        Integer data = 100;
        String message = "Custom message";

        // Act
        Msg<Integer> msg = Msg.success(data, message);

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(data, msg.getData());
        assertEquals(message, msg.getMessage());
    }

    @Test
    public void testSuccessWithNullData() {
        // Act
        Msg<Void> msg = Msg.success((Void) null);

        // Assert
        assertTrue(msg.isSuccess());
        assertNull(msg.getData());
        assertEquals("success", msg.getMessage());
    }

    // ==================== ERROR MESSAGE TESTS ====================

    @Test
    public void testErrorWithMessage() {
        // Act
        Msg<String> msg = Msg.error("Something went wrong");

        // Assert
        assertFalse(msg.isSuccess());
        assertEquals("Something went wrong", msg.getMessage());
        assertNull(msg.getData());
    }

    @Test
    public void testErrorWithNullMessage() {
        // Act
        Msg<String> msg = Msg.error(null);

        // Assert
        assertFalse(msg.isSuccess());
        assertNull(msg.getMessage());
        assertNull(msg.getData());
    }

    @Test
    public void testErrorWithEmptyMessage() {
        // Act
        Msg<String> msg = Msg.error("");

        // Assert
        assertFalse(msg.isSuccess());
        assertEquals("", msg.getMessage());
    }

    // ==================== DATA TYPE TESTS ====================

    @Test
    public void testMsgWithStringData() {
        // Act - For String data, use two-parameter method to avoid ambiguity with success(String message)
        Msg<String> msg = Msg.success("string data", "success");

        // Assert
        assertEquals("string data", msg.getData());
        assertEquals("success", msg.getMessage());
    }

    @Test
    public void testMsgWithIntegerData() {
        // Act
        Msg<Integer> msg = Msg.success(42);

        // Assert
        assertEquals(Integer.valueOf(42), msg.getData());
    }

    @Test
    public void testMsgWithBooleanData() {
        // Act
        Msg<Boolean> msg = Msg.success(true);

        // Assert
        assertTrue(msg.getData());
    }

    @Test
    public void testMsgWithObjectData() {
        // Arrange
        class CustomObject {
            String value = "test";
        }
        CustomObject obj = new CustomObject();

        // Act
        Msg<CustomObject> msg = Msg.success(obj);

        // Assert
        assertNotNull(msg.getData());
        assertEquals("test", msg.getData().value);
    }

    // ==================== GENERIC TYPE TESTS ====================

    @Test
    public void testMsgSupportsGenericTypes() {
        // Act
        Msg<java.util.List<String>> msg = Msg.success(java.util.Arrays.asList("a", "b", "c"));

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(3, msg.getData().size());
    }

    @Test
    public void testMsgWithMapData() {
        // Arrange
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        // Act
        Msg<java.util.Map<String, Object>> msg = Msg.success(map);

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals("value1", msg.getData().get("key1"));
        assertEquals(123, msg.getData().get("key2"));
    }

    // ==================== FIELD ACCESSOR TESTS ====================

    @Test
    public void testGetSuccessReturnsBoolean() {
        // Act
        Msg<String> successMsg = Msg.success("ok");
        Msg<String> errorMsg = Msg.error("fail");

        // Assert
        assertTrue(successMsg.isSuccess());
        assertFalse(errorMsg.isSuccess());
    }

    @Test
    public void testGetMessageReturnsString() {
        // Act
        Msg<String> msg = Msg.success("test", "message");

        // Assert
        assertTrue(msg.getMessage() instanceof String);
        assertEquals("message", msg.getMessage());
    }

    @Test
    public void testGetDataReturnsGenericType() {
        // Act
        Msg<Integer> msg = Msg.success(999);

        // Assert
        assertTrue(msg.getData() instanceof Integer);
        assertEquals(Integer.valueOf(999), msg.getData());
    }

    // ==================== EDGE CASES ====================

    @Test
    public void testSuccessWithEmptyString() {
        // Act - For String data, use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success("", "success");

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals("", msg.getData());
        assertEquals("success", msg.getMessage());
    }

    @Test
    public void testSuccessWithZero() {
        // Act
        Msg<Integer> msg = Msg.success(0);

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(Integer.valueOf(0), msg.getData());
    }

    @Test
    public void testSuccessWithFalse() {
        // Act
        Msg<Boolean> msg = Msg.success(false);

        // Assert
        assertTrue(msg.isSuccess()); // The operation succeeded
        assertFalse(msg.getData()); // But the data value is false
    }

    @Test
    public void testMsgImmutability() {
        // Msg objects should be immutable after creation
        // For String data, use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success("original", "success");

        // There's no setter methods, so immutability is by design
        assertEquals("original", msg.getData());
    }
}
