package com.tars.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tars.config.ModelOption;
import com.tars.config.QwenConfiguration;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill extractor using Qwen AI to analyze resumes and extract technical/professional skills.
 * <p>
 * This class provides an automated solution for extracting relevant skills from resume documents.
 * It leverages the Qwen-long model through DashScope SDK to intelligently identify and normalize
 * technical skills, programming languages, frameworks, tools, and soft skills.
 * </p>
 * <p>
 * Workflow:
 * <ol>
 *   <li>Upload file via OpenAI-compatible API to obtain fileId</li>
 *   <li>Process file with DashScope SDK (qwen-long model) to extract skills</li>
 *   <li>Parse JSON response to List&lt;String&gt;</li>
 *   <li>Apply fallback parsing if JSON parsing fails</li>
 * </ol>
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Automatic skill name normalization to industry-standard terminology</li>
 *   <li>Duplicate removal and vague term filtering</li>
 *   <li>Robust error handling with fallback extraction mechanism</li>
 *   <li>Support for multiple file formats (PDF, DOC, DOCX, TXT)</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see FileParser
 * @see QwenConfiguration
 */
@Slf4j
public class SkillExtractor {

    private final String apiKey;

    private final ModelOption modelOption;

    private final ObjectMapper objectMapper;

    private final FileParser fileParser;

    private static final String SYSTEM_PROMPT = """
            You are an expert HR analyst specializing in technical skill extraction from resumes.
            
            Task: Analyze the resume and extract all relevant professional and technical skills.
            
            Extraction Rules:
            1. Extract hard skills: programming languages, frameworks, tools, databases, cloud platforms
            2. Extract soft skills only if explicitly demonstrated with evidence
            3. Normalize skill names to standard industry terminology
            4. Remove duplicates and vague terms
            5. Keep skill names concise (1-4 words)
            
            Output Format:
            Return ONLY a valid JSON array of strings. No explanations or additional text.
            Example: ["Python", "Machine Learning", "AWS", "Docker", "Agile"]
            """;

    private static final String USER_PROMPT = """
            Extract all skills from this resume and return as JSON array:
            ["skill1", "skill2", "skill3"]
            """;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[([^]]*)]");

    /**
     * Constructs a new SkillExtractor instance.
     * <p>
     * Initializes the extractor with configuration from {@link QwenConfiguration},
     * creates an {@link ObjectMapper} for JSON processing, and instantiates
     * a {@link FileParser} for file upload operations.
     * </p>
     *
     * @throws RuntimeException if configuration initialization fails
     */
    public SkillExtractor() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.apiKey = config.getApiKey();
        this.modelOption = config.getQwenLong();
        this.objectMapper = new ObjectMapper();
        this.fileParser = new FileParser();

        log.info("SkillExtractor initialized with model: {}", modelOption.getModel());
    }

    /**
     * Extracts skills from an uploaded resume file.
     * <p>
     * This method orchestrates the complete skill extraction process:
     * <ol>
     *   <li>Validates and uploads the file to obtain a fileId</li>
     *   <li>Sends the file to Qwen AI for analysis</li>
     *   <li>Parses the AI response into a structured list of skills</li>
     * </ol>
     * </p>
     *
     * @param filePart Multipart file from HTTP request containing the resume
     * @return List of extracted skills as strings, never null (could be empty)
     * @throws IllegalArgumentException if file part is invalid or unsupported format
     * @throws RuntimeException if skill extraction fails completely
     * @see FileParser#extractFileId(Part)
     * @see #processFile(String)
     * @see #parseResponse(String)
     */
    public List<String> extract(Part filePart) {
        String fileName = filePart.getSubmittedFileName();
        log.info("Starting skill extraction for: {}", fileName);

        try {
            String fileId = fileParser.extractFileId(filePart);
            log.info("FileId obtained: {}", fileId);

            String response = processFile(fileId);
            log.debug("AI response: {}", response);

            List<String> skills = parseResponse(response);
            log.info("Extracted {} skills from {}", skills.size(), fileName);

            return skills;

        } catch (Exception e) {
            log.error("Skill extraction failed for: {}", fileName, e);
            throw new RuntimeException("Failed to extract skills: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a file using the Qwen-long model to extract skills.
     * <p>
     * Sends three messages to the AI model:
     * <ol>
     *   <li>System message with extraction rules and guidelines</li>
     *   <li>File reference message with the fileId</li>
     *   <li>User message requesting skill extraction</li>
     * </ol>
     * </p>
     *
     * @param fileId the file identifier obtained from file upload
     * @return Raw AI response containing JSON array of skills
     * @throws RuntimeException if API call fails or returns empty response
     * @see #SYSTEM_PROMPT
     * @see #USER_PROMPT
     */
    private String processFile(String fileId) {
        try {
            Generation generation = new Generation();

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(SYSTEM_PROMPT)
                    .build();

            Message fileMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("fileid://" + fileId)
                    .build();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(USER_PROMPT)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(modelOption.getModel())
                    .messages(Arrays.asList(systemMessage, fileMessage, userMessage))
                    .temperature(modelOption.getTemperature())
                    .topP(modelOption.getTopP())
                    .topK(modelOption.getTopK())
                    .repetitionPenalty(modelOption.getRepetitionPenalty())
                    .maxTokens(modelOption.getMaxTokens())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = generation.call(param);

            if (result.getOutput() == null || result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from Qwen API");
            }

            String content = result.getOutput().getChoices().getFirst().getMessage().getContent();

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty content in response");
            }

            return content;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException("DashScope API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the AI response into a list of skill strings.
     * <p>
     * Parsing strategy:
     * <ol>
     *   <li>Attempts to extract JSON array using regex pattern matching</li>
     *   <li>Parses JSON using Jackson ObjectMapper</li>
     *   <li>Filters out null and empty values</li>
     *   <li>Falls back to manual string parsing if JSON parsing fails</li>
     * </ol>
     * </p>
     *
     * @param response Raw response string from AI model
     * @return List of extracted skills, never null (could be empty)
     * @see #extractJsonArray(String)
     * @see #fallbackParse(String)
     */
    private List<String> parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Try to extract JSON array
            String jsonStr = extractJsonArray(response);
            if (jsonStr == null) {
                jsonStr = response.trim();
            }

            List<String> skills = objectMapper.readValue(
                    jsonStr,
                    new TypeReference<>() {
                    }
            );

            // Filter out null/empty values
            if (skills != null) {
                skills.removeIf(s -> s == null || s.trim().isEmpty());
                return skills;
            }

            return new ArrayList<>();

        } catch (JsonProcessingException e) {
            log.warn("JSON parsing failed, using fallback extraction", e);
            return fallbackParse(response);
        }
    }

    /**
     * Extracts JSON array from text using regex pattern matching.
     * <p>
     * Uses the pattern {@code \[([^\]]*)\]} to find content within square brackets.
     * This handles cases where AI wraps JSON in markdown code blocks or adds explanations.
     * </p>
     *
     * @param text The text containing potential JSON array
     * @return Extracted JSON array string, or null if no match found
     * @see #JSON_ARRAY_PATTERN
     */
    private String extractJsonArray(String text) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        return matcher.find() ? "[" + matcher.group(1) + "]" : null;
    }

    /**
     * Fallback manual parsing when JSON parsing fails.
     * <p>
     * This method uses simple string manipulation to extract skills:
     * <ol>
     *   <li>Removes quotes, brackets, and other JSON syntax characters</li>
     *   <li>Splits by comma delimiter</li>
     *   <li>Trims whitespace from each skill</li>
     *   <li>Filters out strings that are too short (&lt;2 chars) or too long (&gt;100 chars)</li>
     * </ol>
     * </p>
     *
     * @param text Raw text response from AI
     * @return List of extracted skills using manual parsing
     */
    private List<String> fallbackParse(String text) {
        List<String> skills = new ArrayList<>();
        String cleaned = text.replaceAll("[\"'\\[\\]]", "").trim();

        for (String part : cleaned.split(",")) {
            String skill = part.trim();
            if (skill.length() > 1 && skill.length() < 100) {
                skills.add(skill);
            }
        }

        log.warn("Fallback extraction found {} skills", skills.size());
        return skills;
    }
}
