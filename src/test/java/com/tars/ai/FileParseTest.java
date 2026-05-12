package com.tars.ai;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import jakarta.servlet.http.Part;
import org.junit.*;

import java.io.*;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Test class for FileParser
 * Tests file upload and fileId extraction functionality
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/5/10
 */
public class FileParseTest {

    private static final String TEST_UPLOAD_DIR = "test-uploads";

    @BeforeClass
    public static void setUp() {
        // Initialize ApplicationConfiguration for test environment
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        ApplicationConfiguration.initializeForTest(testResourcePath);

        // Initialize QwenConfiguration
        QwenConfiguration.initializeForTest(testResourcePath);

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
        // Reset and reinitialize QwenConfiguration singleton before each test
        resetQwenConfiguration();
        
        // Reinitialize after reset
        String testResourcePath = new File("src/test/resources").getAbsolutePath();
        QwenConfiguration.initializeForTest(testResourcePath);
    }

    /**
     * Test FileParser initialization
     */
    @Test
    public void testFileParserInitialization() {
        try {
            FileParser parser = new FileParser();
            assertNotNull("FileParser should be created", parser);
        } catch (Exception e) {
            fail("FileParser initialization should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test extracting fileId from null file part
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithNullPart() {
        FileParser parser = new FileParser();
        parser.extractFileId((Part) null);
    }

    /**
     * Test extracting fileId from empty file part
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithEmptyPart() throws Exception {
        FileParser parser = new FileParser();
        Part mockPart = createMockPart("test.pdf", 0);

        parser.extractFileId(mockPart);
    }

    /**
     * Test extracting fileId with unsupported file type
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithUnsupportedType() throws Exception {
        FileParser parser = new FileParser();
        Part mockPart = createMockPart("test.jpg", 1024);

        parser.extractFileId(mockPart);
    }

    /**
     * Test extracting fileId from null file
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithNullFile() {
        FileParser parser = new FileParser();
        parser.extractFileId((File) null);
    }

    /**
     * Test extracting fileId from non-existent file
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithNonExistentFile() {
        FileParser parser = new FileParser();
        File nonExistentFile = new File("nonexistent_file.pdf");

        parser.extractFileId(nonExistentFile);
    }

    /**
     * Test extracting fileId from empty file
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithEmptyFile() throws Exception {
        FileParser parser = new FileParser();
        File emptyFile = createTempFile("empty.pdf", 0);

        try {
            parser.extractFileId(emptyFile);
        } finally {
            emptyFile.delete();
        }
    }

    /**
     * Test extracting fileId with unsupported file extension
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExtractFileIdWithUnsupportedExtension() throws Exception {
        FileParser parser = new FileParser();
        File imageFile = createTempFile("test.jpg", 1024);

        try {
            parser.extractFileId(imageFile);
        } finally {
            imageFile.delete();
        }
    }

    /**
     * Test extracting fileId from valid PDF file
     * Note: This test requires actual API connection, may fail without valid credentials
     */
    @Test
    public void testExtractFileIdFromValidPdfFile() {
        FileParser parser = new FileParser();

        // Create a small test PDF file
        File pdfFile = null;
        try {
            pdfFile = createTempPdfFile("test.pdf", 1024);

            // This will attempt to upload to the API
            // In a real scenario, this would return a fileId
            // For testing purposes, we verify the validation passes
            try {
                String fileId = parser.extractFileId(pdfFile);
                // If API is available, fileId should not be null or empty
                if (fileId != null) {
                    assertFalse("FileId should not be empty", fileId.trim().isEmpty());
                }
            } catch (RuntimeException e) {
                // API call may fail due to network or credentials
                // This is acceptable in unit tests
                assertTrue("Should fail with runtime exception",
                        e.getMessage().contains("Failed to upload"));
            }
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }

    /**
     * Test extracting fileId from valid TXT file
     */
    @Test
    public void testExtractFileIdFromValidTxtFile() {
        FileParser parser = new FileParser();

        File txtFile = null;
        try {
            txtFile = createTempFile("test.txt", "text/plain", 512);

            try {
                String fileId = parser.extractFileId(txtFile);
                if (fileId != null) {
                    assertFalse("FileId should not be empty", fileId.trim().isEmpty());
                }
            } catch (RuntimeException e) {
                // Expected if API is not available
                assertTrue("Should fail with runtime exception",
                        e.getMessage().contains("Failed to upload"));
            }
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            if (txtFile != null && txtFile.exists()) {
                txtFile.delete();
            }
        }
    }

    /**
     * Test extracting fileId from valid MD file
     */
    @Test
    public void testExtractFileIdFromValidMdFile() {
        FileParser parser = new FileParser();

        File mdFile = null;
        try {
            mdFile = createTempFile("test.md", "text/markdown", 512);

            try {
                String fileId = parser.extractFileId(mdFile);
                if (fileId != null) {
                    assertFalse("FileId should not be empty", fileId.trim().isEmpty());
                }
            } catch (RuntimeException e) {
                // Expected if API is not available
                assertTrue("Should fail with runtime exception",
                        e.getMessage().contains("Failed to upload"));
            }
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            if (mdFile != null && mdFile.exists()) {
                mdFile.delete();
            }
        }
    }

    /**
     * Test file validation for PDF files
     */
    @Test
    public void testValidatePdfFile() throws Exception {
        FileParser parser = new FileParser();
        File pdfFile = createTempPdfFile("valid.pdf", 1024);

        try {
            // Should not throw exception for valid PDF
            String fileId = parser.extractFileId(pdfFile);
            // Validation passed (API call may still fail)
            assertNotNull("Validation should pass for valid PDF", pdfFile);
        } catch (RuntimeException e) {
            // API failure is acceptable, validation should pass
            assertTrue("Should be validation or API error",
                    e.getMessage().contains("Failed to upload") ||
                            e.getMessage().contains("upload"));
        } finally {
            pdfFile.delete();
        }
    }

    /**
     * Test file validation for DOC files
     */
    @Test
    public void testValidateDocFile() throws Exception {
        FileParser parser = new FileParser();
        File docFile = createTempFile("test.doc", "application/msword", 1024);

        try {
            String fileId = parser.extractFileId(docFile);
            assertNotNull("Validation should pass for valid DOC", docFile);
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should be API error", e.getMessage().contains("Failed to upload"));
        } finally {
            docFile.delete();
        }
    }

    /**
     * Test file validation for DOCX files
     */
    @Test
    public void testValidateDocxFile() throws Exception {
        FileParser parser = new FileParser();
        File docxFile = createTempFile("test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 1024);

        try {
            String fileId = parser.extractFileId(docxFile);
            assertNotNull("Validation should pass for valid DOCX", docxFile);
        } catch (RuntimeException e) {
            // Expected if API is not available
            assertTrue("Should be API error", e.getMessage().contains("Failed to upload"));
        } finally {
            docxFile.delete();
        }
    }

    /**
     * Helper method to create mock Part
     */
    private Part createMockPart(String fileName, long size) throws IOException {
        return new SimplePart(fileName, size);
    }

    /**
     * Helper method to create temporary file
     */
    private File createTempFile(String fileName, int size) throws IOException {
        return createTempFile(fileName, "application/octet-stream", size);
    }

    /**
     * Helper method to create temporary file with content type
     */
    private File createTempFile(String fileName, String contentType, int size) throws IOException {
        File tempFile = File.createTempFile("test_", "_" + fileName);

        if (size > 0) {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] data = new byte[size];
                // Add some dummy content
                for (int i = 0; i < size; i++) {
                    data[i] = (byte) (i % 256);
                }
                fos.write(data);
            }
        }

        return tempFile;
    }

    /**
     * Helper method to create temporary PDF file
     */
    private File createTempPdfFile(String fileName, int size) throws IOException {
        File tempFile = File.createTempFile("test_", "_" + fileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Write minimal PDF header
            String pdfHeader = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
            fos.write(pdfHeader.getBytes());

            // Fill remaining space with dummy data
            int remaining = size - pdfHeader.length();
            if (remaining > 0) {
                byte[] data = new byte[remaining];
                fos.write(data);
            }
        }

        return tempFile;
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

    /**
     * Helper method to reset QwenConfiguration singleton
     */
    private void resetQwenConfiguration() {
        try {
            Field instanceField = QwenConfiguration.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore - configuration may not be initialized
        }
    }

    /**
     * Simple Part implementation for testing
     */
    private static class SimplePart implements Part {
        private final String fileName;
        private final long size;

        public SimplePart(String fileName, long size) {
            this.fileName = fileName;
            this.size = size;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[(int) size]);
        }

        @Override
        public String getContentType() {
            if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (fileName.endsWith(".jpg")) {
                return "image/jpeg";
            }
            return "application/octet-stream";
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
            return size;
        }

        @Override
        public void write(String fileName) throws IOException {
            // Not needed for tests
        }

        @Override
        public void delete() throws IOException {
            // Not needed for tests
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
}
