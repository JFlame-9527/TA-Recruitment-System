package com.tars.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Secure file upload and management utilities for handling PDF documents.
 * <p>
 * This class provides comprehensive file handling capabilities with a strong focus
 * on security, including:
 * <ul>
 *   <li>Secure PDF file upload with multiple validation layers</li>
 *   <li>Path traversal attack prevention</li>
 *   <li>File size and content type validation</li>
 *   <li>Safe file deletion with directory containment checks</li>
 *   <li>Secure file streaming for download/view operations</li>
 *   <li>Path sanitization and normalization</li>
 * </ul>
 * </p>
 * <p>
 * <b>Security Features:</b>
 * <ol>
 *   <li><b>Extension Validation</b>: Only .pdf files are accepted</li>
 *   <li><b>Content Type Check</b>: Validates MIME type is application/pdf</li>
 *   <li><b>UUID Filename</b>: Generates unique filenames to prevent overwrites</li>
 *   <li><b>Path Traversal Prevention</b>: Rejects paths containing ".."</li>
 *   <li><b>Directory Containment</b>: Ensures files stay within uploads directory</li>
 *   <li><b>Size Limit</b>: Enforces 10MB maximum file size</li>
 *   <li><b>Null Byte Injection</b>: Removes null bytes from paths</li>
 *   <li><b>MIME Sniffing Protection</b>: Sets X-Content-Type-Options header</li>
 * </ol>
 * </p>
 * <p>
 * <b>Storage Structure:</b>
 * <pre>
 * uploads/
 * ├── resumes/
 * │   ├── uuid1.pdf
 * │   └── uuid2.pdf
 * └── photos/
 *     └── uuid3.pdf
 * </pre>
 * Files are stored with UUID-based names in subdirectories under the configured upload directory.
 * Database stores relative paths like "resumes/uuid1.pdf".
 * </p>
 * <p>
 * <b>Configuration:</b> Set the upload directory via {@code setFileDir(String)} during
 * application initialization. Default behavior requires explicit configuration.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * // In a Servlet handling file upload:
 * Part filePart = FileUtils.getFilePart(request, "resume");
 * if (filePart != null) {
 *     try {
 *         String relativePath = FileUtils.savePdfFile(filePart, "resumes");
 *         // Store relativePath in database
 *         taProfile.setResumePath(relativePath);
 *         
 *         // Generate web-accessible URL
 *         String fileUrl = FileUtils.getFileUrl(request.getContextPath(), relativePath);
 *         
 *     } catch (SecurityException e) {
 *         RespUtils.writeError(response, "Invalid file: " + e.getMessage());
 *     }
 * }
 * 
 * // To serve file for viewing/downloading:
 * FileUtils.serveFile(request, response, relativePath);
 * 
 * // To delete file:
 * FileUtils.deleteFile(relativePath);
 * }</pre>
 * </p>
 *
 * @author Jflame
 * @version 2.0.0
 * @since 2026/4/2
 * @see Part
 * @see Files
 * @see Path
 */
@Slf4j
public class FileUtils {

    // Allowed file extension
    private static final String ALLOWED_EXTENSION = ".pdf";

    // Allowed content types for PDF
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/x-pdf"
    );

    // Maximum file size (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Base directory for file uploads.
     * <p>
     * Must be configured via {@code setFileDir(String)} before using any file operations.
     * Recommended to set during application initialization in a ServletContextListener.
     * </p>
     * <p>
     * <b>Note:</b> This is a static field shared across all file operations.
     * Changing it affects all file handling globally.
     * </p>
     */
    @Getter
    @Setter
    private static String fileDir;


    /**
     * Securely saves an uploaded PDF file to the upload directory with comprehensive validation.
     * <p>
     * This method performs 6 layers of security checks:
     * <ol>
     *   <li>Validates part existence and non-empty content</li>
     *   <li>Checks file extension (.pdf only)</li>
     *   <li>Verifies content type (application/pdf)</li>
     *   <li>Generates UUID-based safe filename</li>
     *   <li>Validates resolved path stays within upload directory</li>
     *   <li>Enforces 10MB size limit</li>
     * </ol>
     * </p>
     * <p>
     * <b>Filename Strategy:</b> Original filename is discarded and replaced with UUID + .pdf
     * to prevent:
     * <ul>
     *   <li>Filename collisions</li>
     *   <li>Special character issues</li>
     *   <li>Path traversal attempts</li>
     *   <li>Information disclosure (original name may contain sensitive data)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Return Value:</b> Returns relative path in format "{subDir}/{uuid}.pdf" suitable
     * for database storage. Example: "resumes/a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf"
     * </p>
     *
     * @param part   The uploaded file part from multipart request
     * @param subDir Subdirectory under uploads folder (e.g., "resumes", "photos")
     * @return Relative path for database storage (e.g., "resumes/uuid.pdf")
     * @throws IllegalArgumentException if part is null, empty, or has no filename
     * @throws SecurityException        if validation fails (invalid type, size, or path)
     * @throws IOException              if file I/O operations fail
     */
    public static String savePdfFile(Part part, String subDir) throws Exception {
        // Validate part
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("No file uploaded");
        }

        String originalFileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        log.info("Processing file upload: {} (size: {} bytes, content-type: {})",
                originalFileName, part.getSize(), part.getContentType());

        // Security check 1: Validate file extension (PDF only)
        String lowerFileName = originalFileName.toLowerCase();
        if (!lowerFileName.endsWith(ALLOWED_EXTENSION)) {
            throw new SecurityException(
                    String.format("Invalid file type. Only PDF files are allowed. Got: %s", originalFileName));
        }

        // Security check 2: Validate content type
        String contentType = part.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new SecurityException(
                    String.format("Invalid content type. Expected PDF but got: %s", contentType));
        }

        // Security check 3: Generate safe unique filename using UUID
        String safeFileName = UUID.randomUUID() + ALLOWED_EXTENSION;

        // Security check 4: Build upload directory path
        String uploadDirPath = Paths.get(getFileDir(), subDir).toString();
        Path uploadDirectory = Paths.get(uploadDirPath);

        // Create directory if it doesn't exist
        if (!Files.exists(uploadDirectory)) {
            Files.createDirectories(uploadDirectory);
            log.info("Created upload directory: {}", uploadDirPath);
        }

        // Security check 5: Build and validate file path
        Path filePath = uploadDirectory.resolve(safeFileName);

        // Ensure the resolved path is within upload directory (prevent path traversal)
        if (!filePath.normalize().startsWith(uploadDirectory.normalize())) {
            throw new SecurityException("Invalid file path attempt - path traversal detected");
        }

        // Security check 6: Additional size validation
        if (part.getSize() > MAX_FILE_SIZE) {
            throw new SecurityException(
                    String.format("File size exceeds maximum allowed size of 10MB. Got: %d bytes", part.getSize()));
        }

        // Save file securely
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = subDir + "/" + safeFileName;
        relativePath = relativePath.replace("\\", "/");
        log.info("File saved successfully to: {}", relativePath);

        return relativePath;
    }

    /**
     * Retrieves the uploaded file part from a multipart request by field name.
     * <p>
     * This method iterates through all parts in the request and returns the first
     * matching part with non-zero size.
     * </p>
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     * Part resumePart = FileUtils.getFilePart(request, "resume");
     * if (resumePart != null) {
     *     String path = FileUtils.savePdfFile(resumePart, "resumes");
     * }
     * }</pre>
     * </p>
     *
     * @param req       HttpServletRequest containing the multipart request
     * @param fieldName The form field name (e.g., "resume", "photo")
     * @return The Part object if found with content, null otherwise
     */
    public static Part getFilePart(HttpServletRequest req, String fieldName) {
        if (req == null || fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        
        try {
            for (Part part : req.getParts()) {
                if (fieldName.equals(part.getName()) && part.getSize() > 0) {
                    return part;
                }
            }
        } catch (Exception e) {
            log.error("Error getting file part: {}", fieldName, e);
        }
        return null;
    }

    /**
     * Deletes a file from the upload directory with security validation.
     * <p>
     * This method performs path traversal checks and directory containment verification
     * before deleting the file to prevent unauthorized file deletion.
     * </p>
     * <p>
     * <b>Security Checks:</b>
     * <ul>
     *   <li>Rejects paths containing ".." (path traversal)</li>
     *   <li>Normalizes path and verifies it's within uploads directory</li>
     *   <li>Logs warnings for suspicious deletion attempts</li>
     * </ul>
     * </p>
     *
     * @param relativePath The relative path stored in database (e.g., "resumes/uuid.pdf")
     * @return true if deleted successfully, false if file not found or validation failed
     */
    public static boolean deleteFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }

        try {
            // Prevent path traversal attacks
            if (relativePath.contains("..")) {
                log.warn("Attempted path traversal in delete: {}", relativePath);
                return false;
            }

            Path filePath = Paths.get(getFileDir(), relativePath).normalize();

            // Verify file is within uploads directory
            if (!filePath.normalize().startsWith(Paths.get(getFileDir()).normalize())) {
                log.warn("Attempted to delete file outside uploads directory: {}", relativePath);
                return false;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", relativePath);
                return true;
            } else {
                log.warn("File not found for deletion: {}", relativePath);
                return false;
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Checks if a file exists in the upload directory.
     * <p>
     * Performs the same security validations as {@link #deleteFile(String)} to prevent
     * path traversal attacks.
     * </p>
     *
     * @param relativePath The relative path stored in database
     * @return true if file exists and passes validation, false otherwise
     */
    public static boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }

        try {
            if (relativePath.contains("..")) {
                return false;
            }

            Path filePath = Paths.get(getFileDir(), relativePath).normalize();

            if (!filePath.normalize().startsWith(Paths.get(getFileDir()).normalize())) {
                return false;
            }

            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("Error checking file existence: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Constructs a web-accessible URL for an uploaded file.
     * <p>
     * This method combines the application context path, upload directory, and relative
     * file path to create a complete URL that can be used in HTML links or redirects.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>
     * Context path: /ta-system
     * File dir: upload
     * Relative path: resumes/abc123.pdf
     * Result: /ta-system/upload/resumes/abc123.pdf
     * </pre>
     * </p>
     * <p>
     * <b>Security:</b> Rejects paths containing ".." to prevent path traversal in URLs.
     * </p>
     *
     * @param contextPath  The web application context path (from req.getContextPath())
     * @param relativePath The relative path stored in database (e.g., "resumes/uuid.pdf")
     * @return Web-accessible URL (e.g., "/app/upload/resumes/uuid.pdf"), or null if invalid
     */
    public static String getFileUrl(String contextPath, String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        if (relativePath.contains("..")) {
            log.warn("Attempted path traversal in URL construction: {}", relativePath);
            return null;
        }

        String normalizedPath = relativePath.replace("\\", "/");

        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        String fileDir = getFileDir();
        if (fileDir.endsWith("/") || fileDir.endsWith("\\")) {
            fileDir = fileDir.replaceAll("[/\\\\]+$", "");
        }

        return contextPath + "/" + fileDir + "/" + normalizedPath;
    }

    /**
     * Streams a file to HTTP response for secure download or inline viewing.
     * <p>
     * This method handles the complete file serving process:
     * <ol>
     *   <li>Validates the relative path (rejects path traversal)</li>
     *   <li>Verifies file is within uploads directory</li>
     *   <li>Checks file existence</li>
     *   <li>Detects content type automatically</li>
     *   <li>Sets appropriate headers (Content-Disposition, X-Content-Type-Options)</li>
     *   <li>Streams file content in 8KB chunks</li>
     * </ol>
     * </p>
     * <p>
     * <b>Download vs Inline:</b> Controlled by request parameter "download":
     * <ul>
     *   <li>{@code ?download=true} → Content-Disposition: attachment (forces download)</li>
     *   <li>No parameter or false → Content-Disposition: inline (browser displays if possible)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Security Headers:</b>
     * <ul>
     *   <li>X-Content-Type-Options: nosniff (prevents MIME type sniffing)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     * // In a Servlet:
     * String relativePath = request.getParameter("path");
     * FileUtils.serveFile(request, response, relativePath);
     * }</pre>
     * </p>
     *
     * @param req          HttpServletRequest (used for determining download mode)
     * @param resp         HttpServletResponse (used for streaming file content)
     * @param relativePath The relative path stored in database
     * @throws IOException if IO error occurs or file not found
     * @see #sanitizePath(String)
     */
    public static void serveFile(HttpServletRequest req, HttpServletResponse resp,
                                 String relativePath) throws IOException {

        // Validate relative path
        if (relativePath == null || relativePath.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path");
            return;
        }

        // Security check: Prevent path traversal attacks
        if (relativePath.contains("..")) {
            log.warn("Attempted path traversal attack: {}", relativePath);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // Build absolute file path
        Path filePath = Paths.get(getFileDir(), relativePath).normalize();

        // Security check: Verify file is within uploads directory
        Path uploadsDirectory = Paths.get(getFileDir()).normalize();
        if (!filePath.normalize().startsWith(uploadsDirectory)) {
            log.warn("Attempted access to file outside uploads directory: {}", relativePath);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // Check if file exists
        if (!Files.exists(filePath)) {
            log.warn("File not found: {}", relativePath);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        // Determine content type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/pdf"; // Default to PDF
        }

        // Set response headers
        resp.setContentType(contentType);
        resp.setContentLengthLong(Files.size(filePath));

        // Set Content-Disposition based on request parameter
        String download = req.getParameter("download");
        String fileName = Paths.get(relativePath).getFileName().toString();

        if ("true".equals(download)) {
            // Force download as attachment
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        } else {
            // Inline view (browser will try to display PDF)
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        }

        // Add security headers to prevent MIME type sniffing
        resp.setHeader("X-Content-Type-Options", "nosniff");

        // Stream file to response
        try (InputStream input = Files.newInputStream(filePath);
             java.io.BufferedOutputStream output = new java.io.BufferedOutputStream(resp.getOutputStream())) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Validates and sanitizes a file path for safe storage and access.
     * <p>
     * This method performs comprehensive path sanitization:
     * <ol>
     *   <li>Removes null bytes (prevents null byte injection)</li>
     *   <li>Rejects paths containing ".." (path traversal)</li>
     *   <li>Normalizes backslashes to forward slashes</li>
     *   <li>Collapses multiple consecutive slashes to single slash</li>
     *   <li>Validates characters (only alphanumeric, dash, underscore, dot, slash)</li>
     *   <li>Removes leading slash</li>
     * </ol>
     * </p>
     * <p>
     * <b>Allowed Characters:</b> {@code [a-zA-Z0-9_\-./]}
     * </p>
     * <p>
     * <b>Examples:</b>
     * <pre>
     * Input: "resumes/../etc/passwd" → Output: null (rejected)
     * Input: "resumes\\file.pdf"     → Output: "resumes/file.pdf"
     * Input: "//resumes///file.pdf"  → Output: "resumes/file.pdf"
     * Input: "/resumes/file.pdf"     → Output: "resumes/file.pdf"
     * Input: "resume<script>.pdf"    → Output: null (rejected)
     * </pre>
     * </p>
     *
     * @param path The path to validate and sanitize
     * @return Sanitized path or null if path is invalid or contains dangerous patterns
     */
    public static String sanitizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // Remove any null bytes
        path = path.replace("\0", "");

        // Reject paths with ..
        if (path.contains("..")) {
            log.warn("Path traversal attempt detected: {}", path);
            return null;
        }

        // Normalize slashes first (convert backslashes to forward slashes)
        path = path.replace("\\", "/");

        // Normalize multiple consecutive slashes to single slash
        path = path.replaceAll("/+", "/");

        // Only allow safe characters (alphanumeric, dash, underscore, dot, slash)
        if (!path.matches("[a-zA-Z0-9_\\-./]+")) {
            log.warn("Invalid characters in path: {}", path);
            return null;
        }

        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    /**
     * Retrieves a File object from a relative path stored in the database.
     * <p>
     * This method performs security validations before returning the File object
     * to prevent path traversal and unauthorized file access.
     * </p>
     * <p>
     * <b>Use Case:</b> When you need a File object for operations like:
     * <ul>
     *   <li>Checking file metadata (size, last modified)</li>
     *   <li>Passing to other APIs that require File objects</li>
     *   <li>Manual file operations not covered by other methods</li>
     * </ul>
     * </p>
     *
     * @param relativePath The relative path stored in database (e.g., "resumes/uuid.pdf")
     * @return File object if exists and passes validation, null otherwise
     */
    public static File getFileFromRelativePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        try {
            // Prevent path traversal attacks
            if (relativePath.contains("..")) {
                log.warn("Attempted path traversal in getFileFromRelativePath: {}", relativePath);
                return null;
            }

            Path filePath = Paths.get(getFileDir(), relativePath).normalize();

            // Verify file is within uploads directory
            if (!filePath.normalize().startsWith(Paths.get(getFileDir()).normalize())) {
                log.warn("Attempted to access file outside uploads directory: {}", relativePath);
                return null;
            }

            File file = filePath.toFile();
            
            if (file.exists() && file.isFile()) {
                log.debug("File found: {}", filePath);
                return file;
            } else {
                log.warn("File does not exist or is not a file: {}", filePath);
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting file from relative path: {}", relativePath, e);
            return null;
        }
    }
}