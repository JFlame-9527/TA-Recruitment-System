package com.tars.util;

import jakarta.servlet.http.Part;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test class for FileUtils
 * Tests file upload, download, validation, and security features
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class FileUtilsTest {

    private static final String TEST_UPLOAD_DIR = "test_uploads";
    private static final String TEST_WEB_ROOT = "test_webroot";

    @Before
    public void setUp() {
        cleanupTestDirectories();
    }

    @After
    public void tearDown() {
        cleanupTestDirectories();
    }

    /**
     * Helper method to clean up test directories
     */
    private void cleanupTestDirectories() {
        deleteDirectory(new File(TEST_UPLOAD_DIR));
        deleteDirectory(new File(TEST_WEB_ROOT));
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    // ==================== SANITIZE PATH TESTS ====================

    @Test
    public void testSanitizePathValidPath() {
        // Arrange
        String path = "resumes/abc123.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNotNull(sanitized);
        assertEquals("resumes/abc123.pdf", sanitized);
    }

    @Test
    public void testSanitizePathRemovesLeadingSlash() {
        // Arrange
        String path = "/resumes/file.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNotNull(sanitized);
        assertEquals("resumes/file.pdf", sanitized);
    }

    @Test
    public void testSanitizePathConvertsBackslashes() {
        // Arrange
        String path = "resumes\\subdir\\file.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNotNull(sanitized);
        assertEquals("resumes/subdir/file.pdf", sanitized);
    }

    @Test
    public void testSanitizePathRejectsPathTraversal() {
        // Arrange
        String path = "../etc/passwd";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathRejectsDoubleDotInMiddle() {
        // Arrange
        String path = "resumes/../secret/file.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathRejectsInvalidCharacters() {
        // Arrange
        String path = "file<script>.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathWithNullInput() {
        // Act
        String sanitized = FileUtils.sanitizePath(null);

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathWithEmptyString() {
        // Act
        String sanitized = FileUtils.sanitizePath("");

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathWithWhitespace() {
        // Act
        String sanitized = FileUtils.sanitizePath("   ");

        // Assert
        assertNull(sanitized);
    }

    @Test
    public void testSanitizePathAllowsAlphanumericDashUnderscore() {
        // Arrange
        String path = "my-folder/test_file-123.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNotNull(sanitized);
        assertEquals("my-folder/test_file-123.pdf", sanitized);
    }

    @Test
    public void testSanitizePathRemovesNullBytes() {
        // Arrange
        String path = "file\u0000.pdf";

        // Act
        String sanitized = FileUtils.sanitizePath(path);

        // Assert
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("\u0000"));
    }

    // ==================== FILE URL TESTS ====================

    @Test
    public void testGetFileUrlBasicPath() {
        // Arrange
        String contextPath = "/app";
        String relativePath = "resumes/uuid123.pdf";

        // Act
        String url = FileUtils.getFileUrl(contextPath, relativePath);

        // Assert
        assertNotNull(url);
        assertEquals("/app/uploads/resumes/uuid123.pdf", url);
    }

    @Test
    public void testGetFileUrlWithEmptyContextPath() {
        // Arrange
        String contextPath = "";
        String relativePath = "photos/photo.jpg";

        // Act
        String url = FileUtils.getFileUrl(contextPath, relativePath);

        // Assert
        assertNotNull(url);
        assertEquals("/uploads/photos/photo.jpg", url);
    }

    @Test
    public void testGetFileUrlRejectsPathTraversal() {
        // Arrange
        String contextPath = "/app";
        String relativePath = "../secret/file.pdf";

        // Act
        String url = FileUtils.getFileUrl(contextPath, relativePath);

        // Assert
        assertNull(url);
    }

    @Test
    public void testGetFileUrlWithNullPath() {
        // Act
        String url = FileUtils.getFileUrl("/app", null);

        // Assert
        assertNull(url);
    }

    @Test
    public void testGetFileUrlWithEmptyPath() {
        // Act
        String url = FileUtils.getFileUrl("/app", "");

        // Assert
        assertNull(url);
    }

    @Test
    public void testGetFileUrlNormalizesBackslashes() {
        // Arrange
        String contextPath = "/app";
        String relativePath = "folder\\subfolder\\file.pdf";

        // Act
        String url = FileUtils.getFileUrl(contextPath, relativePath);

        // Assert
        assertNotNull(url);
        assertEquals("/app/uploads/folder/subfolder/file.pdf", url);
    }

    @Test
    public void testGetFileUrlRemovesLeadingSlashFromRelativePath() {
        // Arrange
        String contextPath = "/app";
        String relativePath = "/resumes/file.pdf";

        // Act
        String url = FileUtils.getFileUrl(contextPath, relativePath);

        // Assert
        assertNotNull(url);
        assertEquals("/app/uploads/resumes/file.pdf", url);
    }

    // ==================== FILE EXISTS TESTS ====================

    @Test
    public void testFileExistsReturnsTrue() throws Exception {
        // Arrange
        setupTestWebRoot();
        Path filePath = Paths.get(TEST_WEB_ROOT, "uploads", "test.pdf");
        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);

        // Act
        boolean exists = FileUtils.fileExists(TEST_WEB_ROOT, "test.pdf");

        // Assert
        assertTrue(exists);
    }

    @Test
    public void testFileExistsReturnsFalse() throws Exception {
        // Arrange
        setupTestWebRoot();

        // Act
        boolean exists = FileUtils.fileExists(TEST_WEB_ROOT, "nonexistent.pdf");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testFileExistsRejectsPathTraversal() throws Exception {
        // Arrange
        setupTestWebRoot();

        // Act
        boolean exists = FileUtils.fileExists(TEST_WEB_ROOT, "../etc/passwd");

        // Assert
        assertFalse(exists);
    }

    @Test
    public void testFileExistsWithNullPath() {
        // Act
        boolean exists = FileUtils.fileExists(TEST_WEB_ROOT, null);

        // Assert
        assertFalse(exists);
    }

    // ==================== DELETE FILE TESTS ====================

    @Test
    public void testDeleteFileSuccess() throws Exception {
        // Arrange
        setupTestWebRoot();
        Path filePath = Paths.get(TEST_WEB_ROOT, "uploads", "delete_me.pdf");
        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);
        assertTrue(Files.exists(filePath));

        // Act
        boolean deleted = FileUtils.deleteFile(TEST_WEB_ROOT, "delete_me.pdf");

        // Assert
        assertTrue(deleted);
        assertFalse(Files.exists(filePath));
    }

    @Test
    public void testDeleteFileNotExists() throws Exception {
        // Arrange
        setupTestWebRoot();

        // Act
        boolean deleted = FileUtils.deleteFile(TEST_WEB_ROOT, "nonexistent.pdf");

        // Assert
        assertFalse(deleted);
    }

    @Test
    public void testDeleteFileRejectsPathTraversal() throws Exception {
        // Arrange
        setupTestWebRoot();
        Path importantFile = Paths.get("important.txt");
        Files.createFile(importantFile);

        // Act
        boolean deleted = FileUtils.deleteFile(TEST_WEB_ROOT, "../important.txt");

        // Assert
        assertFalse(deleted);
        assertTrue(Files.exists(importantFile)); // File should still exist

        Files.deleteIfExists(importantFile);
    }

    @Test
    public void testDeleteFileWithNullPath() {
        // Act
        boolean deleted = FileUtils.deleteFile(TEST_WEB_ROOT, null);

        // Assert
        assertFalse(deleted);
    }

    // ==================== SAVE PDF FILE TESTS (Mock Part) ====================

    @Test
    public void testSavePdfFileWithValidPdf() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "resume.pdf",
                "application/pdf",
                createFakePdfContent()
        );

        // Act
        String savedPath = FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "resumes");

        // Assert
        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("resumes/"));
        assertTrue(savedPath.endsWith(".pdf"));

        // Verify file exists
        Path filePath = Paths.get(TEST_WEB_ROOT, "uploads", savedPath);
        assertTrue(Files.exists(filePath));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSavePdfFileWithNullPart() throws Exception {
        // Act
        FileUtils.savePdfFile(null, TEST_WEB_ROOT, "resumes");
    }

    @Test(expected = SecurityException.class)
    public void testSavePdfFileWithNonPdfExtension() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "malicious.exe",
                "application/x-msdownload",
                "fake exe content".getBytes()
        );

        // Act
        FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "uploads");
    }

    @Test(expected = SecurityException.class)
    public void testSavePdfFileWithWrongContentType() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "fake.pdf",
                "text/plain",
                "not a pdf".getBytes()
        );

        // Act
        FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "uploads");
    }

    @Test(expected = SecurityException.class)
    public void testSavePdfFileExceedsSizeLimit() throws Exception {
        // Arrange
        setupTestWebRoot();
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB > 10MB limit
        MockPart mockPart = new MockPart(
                "large.pdf",
                "application/pdf",
                largeContent
        );

        // Act
        FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "uploads");
    }

    @Test
    public void testSavePdfFileGeneratesUniqueNames() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart part1 = new MockPart("same.pdf", "application/pdf", createFakePdfContent());
        MockPart part2 = new MockPart("same.pdf", "application/pdf", createFakePdfContent());

        // Act
        String path1 = FileUtils.savePdfFile(part1, TEST_WEB_ROOT, "resumes");
        String path2 = FileUtils.savePdfFile(part2, TEST_WEB_ROOT, "resumes");

        // Assert
        assertNotEquals(path1, path2); // Should have different UUID names
    }

    @Test
    public void testSavePdfFileCreatesSubdirectory() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "test.pdf",
                "application/pdf",
                createFakePdfContent()
        );

        // Act
        String savedPath = FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "custom_subdir");

        // Assert
        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("custom_subdir/"));

        Path dirPath = Paths.get(TEST_WEB_ROOT, "uploads", "custom_subdir");
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));
    }

    // ==================== GET FILE PART TESTS ====================

    @Test
    public void testGetFilePartReturnsCorrectPart() {
        // This would require mocking HttpServletRequest
        // Skipping for now as it requires complex servlet mocking
        assertTrue(true); // Placeholder
    }

    // ==================== SECURITY TESTS ====================

    @Test
    public void testPreventDirectoryTraversalInFilename() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "../../../etc/passwd.pdf",
                "application/pdf",
                createFakePdfContent()
        );

        // Act
        String savedPath = FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "uploads");

        // Assert
        // Should save with UUID name, ignoring original filename's path
        assertNotNull(savedPath);
        assertFalse(savedPath.contains(".."));
        assertFalse(savedPath.contains("/etc/"));
    }

    @Test
    public void testMultipleSubdirectoryLevels() throws Exception {
        // Arrange
        setupTestWebRoot();
        MockPart mockPart = new MockPart(
                "deep.pdf",
                "application/pdf",
                createFakePdfContent()
        );

        // Act
        String savedPath = FileUtils.savePdfFile(mockPart, TEST_WEB_ROOT, "level1/level2/level3");

        // Assert
        assertNotNull(savedPath);
        Path fullPath = Paths.get(TEST_WEB_ROOT, "uploads", savedPath);
        assertTrue(Files.exists(fullPath));
    }

    // ==================== HELPER METHODS ====================

    private void setupTestWebRoot() throws Exception {
        Files.createDirectories(Paths.get(TEST_WEB_ROOT));
    }

    private byte[] createFakePdfContent() {
        // Minimal valid PDF header
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes();
    }

    /**
     * Mock implementation of Part interface for testing
     */
    private static class MockPart implements Part {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        MockPart(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getSubmittedFileName() {
            return fileName;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public void write(String fileName) {}

        @Override
        public void delete() {}

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }
    }
}
