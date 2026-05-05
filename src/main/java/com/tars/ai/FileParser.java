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
 * File parser responsible for uploading files and extracting file IDs
 * <p>
 * Responsibilities:
 * 1. Validate file parts
 * 2. Upload files via OpenAI-compatible API
 * 3. Return fileId for further processing
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/21
 */
@Slf4j
public class FileParser {

    private final OpenAIClient openAIClient;

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
     * Extract fileId from uploaded file
     *
     * @param filePart Multipart file from HTTP request
     * @return fileId string for AI processing
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
     * Extract fileId from existing file on disk
     *
     * @param file File object representing the existing file
     * @return fileId string for AI processing
     */
    public String extractFileId(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + (file != null ? file.getAbsolutePath() : "null"));
        }
        
        validateFile(file);
        
        String fileName = file.getName();
        log.info("Uploading file: {}", fileName);

        int maxRetries = 3;
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
                        long waitTime = 3000;
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
     * Validate file part
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
     * Validate file
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
     * Upload file using OpenAI-compatible API and return fileId
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
     * Upload file using OpenAI-compatible API and return fileId
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
