package com.tars.util;

import com.tars.config.ApplicationConfiguration;
import jakarta.servlet.http.Part;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.*;

/**
 * Test class for FileUtils
 * Tests file upload, download, validation, and security features
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class FileUtilsTest {

    private static final String TEST_UPLOAD_DIR = "test-uploads";
    private static final String TEST_SUBDIR = "resumes";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Set test upload directory
        FileUtils.setFileDir(TEST_UPLOAD_DIR);

        // Create test upload directory
        File dir = new File(TEST_UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @AfterClass
    public static void tearDown() {
        // Clean up test upload directory
        cleanTestDirectory();
    }

    @Before
    public void beforeEach() {
        // Clean upload directory before each test
        cleanTestDirectory();
        File dir = new File(TEST_UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Test saving a valid PDF file
     */
    @Test
    public void testSavePdfFile() throws Exception {
        File testFile = createTempPdfFile("test.pdf", 1024);
        Part part = createRealPart(testFile, "test.pdf");

        String relativePath = FileUtils.savePdfFile(part, TEST_SUBDIR);

        assertNotNull("Relative path should not be null", relativePath);
        assertTrue("Path should contain subdirectory", relativePath.contains(TEST_SUBDIR));
        assertTrue("Path should end with .pdf", relativePath.endsWith(".pdf"));

        // Verify file was created
        File savedFile = new File(TEST_UPLOAD_DIR, relativePath.replace("/", File.separator));
        assertTrue("File should exist", savedFile.exists());

        // Clean up
        testFile.delete();
    }

    /**
     * Test saving file with null part
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSavePdfFileWithNullPart() throws Exception {
        FileUtils.savePdfFile(null, TEST_SUBDIR);
    }

    /**
     * Test saving file with empty part (zero size)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSavePdfFileWithEmptyPart() throws Exception {
        File testFile = createTempPdfFile("empty.pdf", 0);
        Part part = createRealPart(testFile, "empty.pdf");

        try {
            FileUtils.savePdfFile(part, TEST_SUBDIR);
        } finally {
            testFile.delete();
        }
    }

    /**
     * Test saving file with non-PDF extension
     */
    @Test(expected = SecurityException.class)
    public void testSaveNonPdfFile() throws Exception {
        File testFile = createTempFile("test.txt", "text/plain", 1024);
        Part part = createRealPart(testFile, "test.txt");

        try {
            FileUtils.savePdfFile(part, TEST_SUBDIR);
        } finally {
            testFile.delete();
        }
    }

    /**
     * Test saving file with invalid content type
     */
    @Test(expected = SecurityException.class)
    public void testSavePdfWithInvalidContentType() throws Exception {
        File testFile = createTempFile("test.pdf", "text/plain", 1024);
        Part part = createRealPartWithContentType(testFile, "test.pdf", "text/plain");

        try {
            FileUtils.savePdfFile(part, TEST_SUBDIR);
        } finally {
            testFile.delete();
        }
    }

    /**
     * Test getting file part from request - manual test without mocking
     */
    @Test
    public void testGetFilePartManual() {
        // This method requires HttpServletRequest which is hard to test without mocking
        // We'll test the logic indirectly by verifying the method exists and handles null
        Part result = FileUtils.getFilePart(null, "resume");
        assertNull("Should return null for null request", result);
    }

    /**
     * Test deleting an existing file
     */
    @Test
    public void testDeleteFile() throws Exception {
        File testFile = createTempPdfFile("test.pdf", 1024);
        Part part = createRealPart(testFile, "test.pdf");
        String relativePath = FileUtils.savePdfFile(part, TEST_SUBDIR);

        boolean deleted = FileUtils.deleteFile(relativePath);

        assertTrue("File should be deleted", deleted);
        assertFalse("File should not exist after deletion",
                FileUtils.fileExists(relativePath));

        // Clean up
        testFile.delete();
    }

    /**
     * Test deleting non-existent file
     */
    @Test
    public void testDeleteNonExistentFile() {
        boolean deleted = FileUtils.deleteFile("nonexistent/path/file.pdf");

        assertFalse("Should return false for non-existent file", deleted);
    }

    /**
     * Test deleting file with path traversal attempt
     */
    @Test
    public void testDeleteWithPathTraversal() {
        boolean deleted = FileUtils.deleteFile("../etc/passwd");

        assertFalse("Should reject path traversal attempts", deleted);
    }

    /**
     * Test checking if file exists
     */
    @Test
    public void testFileExists() throws Exception {
        File testFile = createTempPdfFile("test.pdf", 1024);
        Part part = createRealPart(testFile, "test.pdf");
        String relativePath = FileUtils.savePdfFile(part, TEST_SUBDIR);

        assertTrue("File should exist", FileUtils.fileExists(relativePath));

        // Clean up
        testFile.delete();
    }

    /**
     * Test checking if non-existent file exists
     */
    @Test
    public void testFileNotExists() {
        assertFalse("Non-existent file should return false",
                FileUtils.fileExists("nonexistent/file.pdf"));
    }

    /**
     * Test file existence check with path traversal
     */
    @Test
    public void testFileExistsWithPathTraversal() {
        assertFalse("Should reject path traversal in existence check",
                FileUtils.fileExists("../etc/passwd"));
    }

    /**
     * Test getting file URL
     */
    @Test
    public void testGetFileUrl() {
        String contextPath = "/app";
        String relativePath = "resumes/test.pdf";

        String url = FileUtils.getFileUrl(contextPath, relativePath);

        assertNotNull("URL should not be null", url);
        assertTrue("URL should contain context path", url.contains(contextPath));
        assertTrue("URL should contain file path", url.contains(relativePath));
    }

    /**
     * Test getting file URL with null path
     */
    @Test
    public void testGetFileUrlWithNullPath() {
        String url = FileUtils.getFileUrl("/app", null);

        assertNull("Should return null for null path", url);
    }

    /**
     * Test getting file URL with path traversal
     */
    @Test
    public void testGetFileUrlWithPathTraversal() {
        String url = FileUtils.getFileUrl("/app", "../etc/passwd");

        assertNull("Should reject path traversal in URL", url);
    }

    /**
     * Test sanitizing valid path
     */
    @Test
    public void testSanitizeValidPath() {
        String path = "resumes/test-file_123.pdf";

        String sanitized = FileUtils.sanitizePath(path);

        assertNotNull("Sanitized path should not be null", sanitized);
        assertEquals("Path should remain unchanged", path, sanitized);
    }

    /**
     * Test sanitizing path with backslashes
     */
    @Test
    public void testSanitizePathWithBackslashes() {
        String path = "resumes\\test\\file.pdf";

        String sanitized = FileUtils.sanitizePath(path);

        assertNotNull("Sanitized path should not be null", sanitized);
        assertTrue("Should convert backslashes to forward slashes",
                sanitized.contains("/"));
        assertFalse("Should not contain backslashes", sanitized.contains("\\"));
    }

    /**
     * Test sanitizing path with path traversal
     */
    @Test
    public void testSanitizePathWithTraversal() {
        String path = "../etc/passwd";

        String sanitized = FileUtils.sanitizePath(path);

        assertNull("Should reject path traversal", sanitized);
    }

    /**
     * Test sanitizing path with invalid characters
     */
    @Test
    public void testSanitizePathWithInvalidChars() {
        String path = "resumes/test<script>.pdf";

        String sanitized = FileUtils.sanitizePath(path);

        assertNull("Should reject invalid characters", sanitized);
    }

    /**
     * Test sanitizing null path
     */
    @Test
    public void testSanitizeNullPath() {
        String sanitized = FileUtils.sanitizePath(null);

        assertNull("Should return null for null input", sanitized);
    }

    /**
     * Test sanitizing empty path
     */
    @Test
    public void testSanitizeEmptyPath() {
        String sanitized = FileUtils.sanitizePath("");

        assertNull("Should return null for empty input", sanitized);
    }

    /**
     * Test getting file from relative path
     */
    @Test
    public void testGetFileFromRelativePath() throws Exception {
        File testFile = createTempPdfFile("test.pdf", 1024);
        Part part = createRealPart(testFile, "test.pdf");
        String relativePath = FileUtils.savePdfFile(part, TEST_SUBDIR);

        File file = FileUtils.getFileFromRelativePath(relativePath);

        assertNotNull("File should not be null", file);
        assertTrue("File should exist", file.exists());
        assertTrue("Should be a file", file.isFile());

        // Clean up
        testFile.delete();
    }

    /**
     * Test getting file with path traversal
     */
    @Test
    public void testGetFileWithPathTraversal() {
        File file = FileUtils.getFileFromRelativePath("../etc/passwd");

        assertNull("Should reject path traversal", file);
    }

    /**
     * Test getting file with null path
     */
    @Test
    public void testGetFileWithNullPath() {
        File file = FileUtils.getFileFromRelativePath(null);

        assertNull("Should return null for null path", file);
    }

    /**
     * Helper method to create temporary PDF file
     */
    private File createTempPdfFile(String fileName, int size) throws IOException {
        return createTempFile(fileName, "application/pdf", size);
    }

    /**
     * Helper method to create temporary file
     */
    private File createTempFile(String fileName, String contentType, int size) throws IOException {
        File tempFile = File.createTempFile("test_", "_" + fileName);

        if (size > 0) {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] data = new byte[size];
                // Fill with dummy PDF header
                if (fileName.endsWith(".pdf")) {
                    System.arraycopy("%PDF-1.4".getBytes(), 0, data, 0, Math.min(8, size));
                }
                fos.write(data);
            }
        }

        return tempFile;
    }

    /**
     * Helper method to create real Part implementation
     */
    private Part createRealPart(File file, String fileName) throws IOException {
        return new SimplePart(file, fileName, "application/pdf");
    }

    /**
     * Helper method to create real Part with custom content type
     */
    private Part createRealPartWithContentType(File file, String fileName, String contentType) throws IOException {
        return new SimplePart(file, fileName, contentType);
    }

    /**
     * Simple Part implementation for testing
     */
    private static class SimplePart implements Part {
        private final File file;
        private final String fileName;
        private final String contentType;

        public SimplePart(File file, String fileName, String contentType) throws IOException {
            this.file = file;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
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
            return file.length();
        }

        @Override
        public void write(String fileName) throws IOException {
            // Not needed for tests
        }

        @Override
        public void delete() throws IOException {
            file.delete();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public java.util.Collection<String> getHeaders(String name) {
            return null;
        }

        @Override
        public java.util.Collection<String> getHeaderNames() {
            return null;
        }
    }

    /**
     * Helper method to clean test directory
     */
    private static void cleanTestDirectory() {
        File dir = new File(TEST_UPLOAD_DIR);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
    }

    /**
     * Helper method to delete directory recursively
     */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
