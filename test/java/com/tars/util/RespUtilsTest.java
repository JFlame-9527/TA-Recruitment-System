package com.tars.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test class for RespUtils
 * Tests JSON response generation and HTTP response handling
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class RespUtilsTest {

    // ==================== OBJECT MAPPER TESTS ====================

    @Test
    public void testGetObjectMapperReturnsInstance() {
        // Act
        var mapper = RespUtils.getObjectMapper();

        // Assert
        assertNotNull(mapper);
    }

    @Test
    public void testGetObjectMapperReturnsSameInstance() {
        // Act
        var mapper1 = RespUtils.getObjectMapper();
        var mapper2 = RespUtils.getObjectMapper();

        // Assert
        assertSame(mapper1, mapper2); // Should be singleton
    }

    @Test
    public void testObjectMapperSerializesLocalDateTime() throws IOException {
        // Arrange
        var mapper = RespUtils.getObjectMapper();
        var testObj = new java.util.HashMap<String, Object>();
        testObj.put("timestamp", java.time.LocalDateTime.now());

        // Act
        String json = mapper.writeValueAsString(testObj);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("timestamp"));
        // Should not be serialized as timestamp array
        assertFalse(json.contains("[["));
    }

    // ==================== MSG WRAPPER TESTS ====================

    @Test
    public void testMsgSuccessWithMessage() {
        // Act
        Msg<String> msg = Msg.success("Operation successful");

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals("Operation successful", msg.getMessage());
        assertNull(msg.getData());
    }

    @Test
    public void testMsgSuccessWithData() {
        // Arrange
        String testData = "test data";

        // Act - For String data, use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success(testData, "success");

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(testData, msg.getData());
        assertEquals("success", msg.getMessage());
    }

    @Test
    public void testMsgSuccessWithDataAndMessage() {
        // Arrange
        Integer testData = 42;
        String message = "Custom success message";

        // Act
        Msg<Integer> msg = Msg.success(testData, message);

        // Assert
        assertTrue(msg.isSuccess());
        assertEquals(testData, msg.getData());
        assertEquals(message, msg.getMessage());
    }

    @Test
    public void testMsgErrorWithMessage() {
        // Act
        Msg<String> msg = Msg.error("Something went wrong");

        // Assert
        assertFalse(msg.isSuccess());
        assertEquals("Something went wrong", msg.getMessage());
        assertNull(msg.getData());
    }

    @Test
    public void testMsgStructureFields() {
        // Act
        Msg<String> successMsg = Msg.success("data", "ok");
        Msg<String> errorMsg = Msg.error("error");

        // Assert
        assertNotNull(successMsg.getMessage());

        assertNotNull(errorMsg.getMessage());
    }

    // ==================== WRITE SUCCESS TESTS (Mock) ====================

    @Test
    public void testWriteSuccessGeneratesValidJson() throws IOException {
        // This test validates the JSON structure without actual HTTP response
        // In real scenario, this would write to HttpServletResponse

        // Arrange
        String testData = "test";
        // For String data, use two-parameter method to avoid ambiguity
        Msg<String> expected = Msg.success(testData, "success");

        // Act - Manually serialize to verify structure
        String json = RespUtils.getObjectMapper().writeValueAsString(expected);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"success\""));
        assertTrue(json.contains("\"data\":\"test\""));
    }

    @Test
    public void testWriteSuccessWithComplexData() throws IOException {
        // Arrange
        java.util.Map<String, Object> complexData = new java.util.HashMap<>();
        complexData.put("id", "123");
        complexData.put("name", "John");
        complexData.put("active", true);

        Msg<java.util.Map<String, Object>> msg = Msg.success(complexData, "Created");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"Created\""));
        assertTrue(json.contains("\"id\":\"123\""));
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"active\":true"));
    }

    @Test
    public void testWriteErrorGeneratesValidJson() throws IOException {
        // Arrange
        Msg<String> error = Msg.error("Invalid input");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(error);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"message\":\"Invalid input\""));
    }

    @Test
    public void testWriteErrorWithNullMessage() throws IOException {
        // Arrange
        Msg<String> error = Msg.error(null);

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(error);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"success\":false"));
    }

    // ==================== EDGE CASES ====================

    @Test
    public void testMsgWithNullData() throws IOException {
        // Arrange
        Msg<Void> msg = Msg.success(null, "No data");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"No data\""));
    }

    @Test
    public void testMsgWithEmptyString() throws IOException {
        // Arrange
        Msg<String> msg = Msg.success("", "Empty");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"data\":\"\""));
    }

    @Test
    public void testMsgWithSpecialCharacters() throws IOException {
        // Arrange
        String specialData = "Data with \"quotes\" and \\backslash";
        // For String data, use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success(specialData, "success");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        // Jackson should properly escape special characters
        assertTrue(json.contains("quotes"));
    }

    @Test
    public void testMsgWithUnicodeCharacters() throws IOException {
        // Arrange
        String unicodeData = "中文データ🎉";
        // For String data, use two-parameter method to avoid ambiguity
        Msg<String> msg = Msg.success(unicodeData, "success");

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains(unicodeData));
    }

    @Test
    public void testMsgWithNumericData() throws IOException {
        // Arrange & Act
        Msg<Integer> intMsg = Msg.success(42);
        String intJson = RespUtils.getObjectMapper().writeValueAsString(intMsg);

        Msg<Double> doubleMsg = Msg.success(3.14);
        String doubleJson = RespUtils.getObjectMapper().writeValueAsString(doubleMsg);

        // Assert
        assertTrue(intJson.contains("\"data\":42"));
        assertTrue(doubleJson.contains("\"data\":3.14"));
    }

    @Test
    public void testMsgWithBooleanData() throws IOException {
        // Arrange & Act
        Msg<Boolean> trueMsg = Msg.success(true);
        String trueJson = RespUtils.getObjectMapper().writeValueAsString(trueMsg);

        Msg<Boolean> falseMsg = Msg.success(false);
        String falseJson = RespUtils.getObjectMapper().writeValueAsString(falseMsg);

        // Assert
        assertTrue(trueJson.contains("\"data\":true"));
        assertTrue(falseJson.contains("\"data\":false"));
    }

    @Test
    public void testMsgWithListData() throws IOException {
        // Arrange
        java.util.List<String> list = java.util.Arrays.asList("item1", "item2", "item3");
        Msg<java.util.List<String>> msg = Msg.success(list);

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"data\":["));
        assertTrue(json.contains("\"item1\""));
        assertTrue(json.contains("\"item2\""));
        assertTrue(json.contains("\"item3\""));
    }

    @Test
    public void testMsgWithNestedObject() throws IOException {
        // Arrange
        java.util.Map<String, Object> nested = new java.util.HashMap<>();
        nested.put("level1", "value1");

        java.util.Map<String, Object> inner = new java.util.HashMap<>();
        inner.put("level2", "value2");
        nested.put("nested", inner);

        Msg<java.util.Map<String, Object>> msg = Msg.success(nested);

        // Act
        String json = RespUtils.getObjectMapper().writeValueAsString(msg);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"level1\":\"value1\""));
        assertTrue(json.contains("\"level2\":\"value2\""));
    }
}
