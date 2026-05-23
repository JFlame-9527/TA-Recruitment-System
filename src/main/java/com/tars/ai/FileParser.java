package com.tars.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.files.FileObject;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FilePurpose;
import com.tars.config.QwenConfiguration;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File parser responsible for uploading files and extracting file IDs.
 * <p>
 * This class provides functionality to upload resume files (PDF, DOC, DOCX, TXT, MD)
 * to the OpenAI-compatible API service and retrieve file IDs for further AI processing.
 * It supports both multipart file uploads from HTTP requests and existing files on disk.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>File validation for supported formats</li>
 *   <li>Automatic retry mechanism with exponential backoff for rate limiting</li>
 *   <li>Temporary file management with automatic cleanup</li>
 *   <li>Integration with Qwen AI service via OpenAI-compatible API</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/21
 * @see QwenConfiguration
 * @see com.openai.client.OpenAIClient
 */
@Slf4j
public class FileParser {

    private final OpenAIClient openAIClient;

    /**
     * Constructs a new FileParser instance.
     * <p>
     * Initializes the OpenAI client using configuration from {@link QwenConfiguration}.
     * The client is configured with the API key and base URL from the singleton configuration instance.
     * </p>
     *
     * @throws RuntimeException if configuration initialization fails
     */
    public FileParser() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        String apiKey = config.getApiKey();
        String baseUrl = config.getBaseUrl();

        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        log.info("FileParser initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Extracts file ID from uploaded multipart file.
     * <p>
     * This method validates the file part, uploads it to the AI service,
     * and returns the file ID for subsequent processing.
     * </p>
     *
     * @param filePart Multipart file from HTTP request
     * @return fileId string for AI processing
     * @throws IllegalArgumentException if file part is null, empty, or has unsupported format
     * @throws RuntimeException if file upload fails
     * @see #validateFilePart(Part)
     * @see #uploadFile(Part)
     */
    public String extractFileId(Part filePart) {
        validateFilePart(filePart);

        String fileName = filePart.getSubmittedFileName();
        log.info("Uploading file: {}", fileName);

        try {
            String fileId = uploadFile(filePart);
            log.info("File uploaded successfully, fileId: {}", fileId);
            return fileId;

        } catch (Exception e) {
            log.error("File upload failed for: {}", fileName, e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts file ID from an existing file on disk.
     * <p>
     * This method validates the file, uploads it to the AI service with automatic retry
     * mechanism for handling rate limits (HTTP 429), and returns the file ID.
     * </p>
     * <p>
     * Retry strategy:
     * <ul>
     *   <li>Maximum 5 retries</li>
     *   <li>Exponential backoff: 3s, 6s, 12s, 24s, 48s</li>
     *   <li>Only retries on rate limit errors (429)</li>
     * </ul>
     * </p>
     *
     * @param file File object representing the existing file
     * @return fileId string for AI processing
     * @throws IllegalArgumentException if file is null, doesn't exist, or has unsupported format
     * @throws RuntimeException if file upload fails after maximum retries
     * @see #validateFile(File)
     * @see #uploadFile(File)
     */
    public String extractFileId(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + (file != null ? file.getAbsolutePath() : "null"));
        }
        
        validateFile(file);
        
        String fileName = file.getName();
        log.info("Uploading file: {}", fileName);

        int maxRetries = 5;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String fileId = uploadFile(file);
                log.info("File uploaded successfully, fileId: {}", fileId);
                return fileId;
            } catch (Exception e) {
                retryCount++;
                
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    if (retryCount < maxRetries) {
                        long waitTime = (long) (3000 * Math.pow(2, retryCount));
                        log.warn("Rate limit exceeded. Retry {}/{} after {}ms...", retryCount, maxRetries, waitTime);
                        
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Upload interrupted", ie);
                        }
                    } else {
                        log.error("Max retries exceeded for file: {}", fileName, e);
                        throw new RuntimeException("Failed to upload file after " + maxRetries + " retries: " + e.getMessage(), e);
                    }
                } else {
                    log.error("File upload failed for: {}", fileName, e);
                    throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("Failed to upload file: Max retries exceeded");
    }

    /**
     * Validates the multipart file part.
     * <p>
     * Checks the following conditions:
     * <ul>
     *   <li>File part is not null</li>
     *   <li>File size is greater than 0</li>
     *   <li>File extension is one of: PDF, DOC, DOCX, TXT</li>
     * </ul>
     * </p>
     *
     * @param filePart the multipart file part to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFilePart(Part filePart) {
        if (filePart == null) {
            throw new IllegalArgumentException("File part cannot be null");
        }
        if (filePart.getSize() == 0) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase().matches(".*\\.(pdf|doc|docx|txt)$")) {
            throw new IllegalArgumentException("Only PDF, DOC, DOCX, TXT files are supported");
        }
    }

    /**
     * Validates the file object.
     * <p>
     * Checks the following conditions:
     * <ul>
     *   <li>File is not null</li>
     *   <li>File exists on disk</li>
     *   <li>Path represents a regular file (not directory)</li>
     *   <li>File size is greater than 0</li>
     *   <li>File extension is one of: PDF, DOC, DOCX, TXT, MD</li>
     * </ul>
     * </p>
     *
     * @param file the file object to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String fileName = file.getName();
        if (!fileName.toLowerCase().matches(".*\\.(pdf|doc|docx|txt|md)$")) {
            throw new IllegalArgumentException("Only PDF, DOC, DOCX, TXT, MD files are supported");
        }
    }

    /**
     * Uploads a multipart file using OpenAI-compatible API and returns the file ID.
     * <p>
     * Process:
     * <ol>
     *   <li>Creates a temporary file</li>
     *   <li>Copies the multipart file content to the temporary file</li>
     *   <li>Uploads to AI service with purpose "file-extract"</li>
     *   <li>Returns the file ID from the response</li>
     *   <li>Cleans up the temporary file</li>
     * </ol>
     * </p>
     *
     * @param filePart the multipart file to upload
     * @return the file ID returned by the AI service
     * @throws IOException if file operations fail
     * @throws RuntimeException if upload fails or no file ID is returned
     */
    private String uploadFile(Part filePart) throws IOException {
        Path tempFile = Files.createTempFile("resume_", "_" + filePart.getSubmittedFileName());

        try {
            Files.copy(filePart.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            FileCreateParams params = FileCreateParams.builder()
                    .file(tempFile)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();

            FileObject fileObject = openAIClient.files().create(params);

            if (fileObject == null || fileObject.id() == null) {
                throw new RuntimeException("Upload failed: no fileId returned");
            }

            return fileObject.id();

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Uploads a file from disk using OpenAI-compatible API and returns the file ID.
     * <p>
     * Process:
     * <ol>
     *   <li>Creates a temporary file</li>
     *   <li>Copies the source file content to the temporary file</li>
     *   <li>Uploads to AI service with purpose "file-extract"</li>
     *   <li>Returns the file ID from the response</li>
     *   <li>Cleans up the temporary file</li>
     * </ol>
     * </p>
     *
     * @param file the file to upload
     * @return the file ID returned by the AI service
     * @throws IOException if file operations fail
     * @throws RuntimeException if upload fails or no file ID is returned
     */
    private String uploadFile(File file) throws IOException {
        Path tempFile = Files.createTempFile("resume_", "_" + file.getName());

        try {
            Files.copy(file.toPath(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            FileCreateParams params = FileCreateParams.builder()
                    .file(tempFile)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();

            FileObject fileObject = openAIClient.files().create(params);

            if (fileObject == null || fileObject.id() == null) {
                throw new RuntimeException("Upload failed: no fileId returned");
            }

            return fileObject.id();

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
