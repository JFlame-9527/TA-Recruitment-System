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
 * Skill extractor using Qwen AI to analyze resumes and extract technical/professional skills
 * <p>
 * Workflow:
 * 1. Upload file via OpenAI-compatible API -> get fileId
 * 2. Process file with DashScope SDK (qwen-long model) -> extract skills
 * 3. Parse response to List<String>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
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

    public SkillExtractor() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.apiKey = config.getApiKey();
        this.modelOption = config.getQwenLong();
        this.objectMapper = new ObjectMapper();
        this.fileParser = new FileParser();

        log.info("SkillExtractor initialized with model: {}", modelOption.getModel());
    }

    /**
     * Extract skills from uploaded resume file
     *
     * @param filePart Multipart file from HTTP request
     * @return List of extracted skills
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

            if (result == null || result.getOutput() == null ||
                    result.getOutput().getChoices() == null ||
                    result.getOutput().getChoices().isEmpty()) {
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
     * Step 3: Parse AI response to List<String>
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
                    new TypeReference<List<String>>() {}
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
     * Extract JSON array from text using regex
     */
    private String extractJsonArray(String text) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        return matcher.find() ? "[" + matcher.group(1) + "]" : null;
    }

    /**
     * Fallback: manual parsing if JSON fails
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
