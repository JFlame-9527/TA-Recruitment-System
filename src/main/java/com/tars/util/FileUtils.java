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
 * File upload and management utilities
 *
 * @author Jflame
 * @version 2.0.0
 * @since 2026/4/2
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

    @Getter
    @Setter
    private static String fileDir;


    /**
     * Securely save uploaded PDF file to upload directory.
     *
     * @param part The uploaded file part from multipart request
     * @param subDir Subdirectory under uploads folder (e.g., "resumes", "photos")
     * @return Relative path for database storage (e.g., "resumes/uuid.pdf")
     * @throws Exception if validation fails or IO error occurs
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
        log.info("File saved successfully to: {}", relativePath);

        return relativePath;
    }

    /**
     * Get uploaded file part from request by field name.
     *
     * @param req HttpServletRequest containing the multipart request
     * @param fieldName The form field name (e.g., "resume", "photo")
     * @return The Part object if found with content, null otherwise
     */
    public static Part getFilePart(HttpServletRequest req, String fieldName) {
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
     * Delete uploaded file from the upload directory.
     *
     * @param relativePath The relative path stored in database
     * @return true if deleted successfully, false otherwise
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
     * Check if file exists in the upload directory.
     *
     * @param relativePath The relative path stored in database
     * @return true if file exists, false otherwise
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
     * Get the web-accessible URL for an uploaded file.
     *
     * @param contextPath The web application context path (from req.getContextPath())
     * @param relativePath The relative path stored in database (e.g., "resumes/uuid.pdf")
     * @return Web-accessible URL (e.g., "/app/upload/resumes/uuid.pdf"), or null if invalid
     */
    public static String getFileUrl(String contextPath, String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        // Prevent path traversal in URL construction
        if (relativePath.contains("..")) {
            log.warn("Attempted path traversal in URL construction: {}", relativePath);
            return null;
        }

        // Normalize the path (convert backslashes to forward slashes)
        String normalizedPath = relativePath.replace("\\", "/");

        // Ensure it doesn't start with slash
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return contextPath + "/" + getFileDir() + "/" + normalizedPath;
    }

    /**
     * Stream a file to HTTP response for secure download/view.
     *
     * @param req HttpServletRequest (used for determining download mode)
     * @param resp HttpServletResponse (used for streaming file content)
     * @param relativePath The relative path stored in database
     * @throws IOException if IO error occurs
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
     * Validate and sanitize file path for storage and access.
     *
     * @param path The path to validate
     * @return Sanitized path or null if invalid
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
     * Get File object from relative path stored in database.
     *
     * @param relativePath The relative path stored in database (e.g., "resumes/uuid.pdf")
     * @return File object if exists, null otherwise
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