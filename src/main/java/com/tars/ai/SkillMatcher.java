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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill matcher using Qwen AI to calculate match rates between skill sets
 * <p>
 * Workflow:
 * 1. Receive multiple skill lists and target skills
 * 2. Use qwen-max to intelligently calculate match rates
 * 3. Return match rates as List<Float> (0.000 - 1.000)
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 */
@Slf4j
public class SkillMatcher {

    private final String apiKey;
    private final ModelOption modelOption;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a Senior Technical Recruiter and Engineering Manager. Your task is to evaluate candidate resumes against a job description with a focus on "Technical Affinity" and "Transferable Skills".
            
            ### Task:
            Calculate ONE comprehensive match score for EACH candidate based on how well their ENTIRE skill set matches ALL required skills combined.
            
            ### Evaluation Logic (Crucial):
            1.  **Beyond Exact Matching**: Do not simply compare strings. You must evaluate the *conceptual proximity* of skills.
                -   *Example*: A candidate with "Redis" expertise should receive partial credit for a "Database/SQL" requirement, as they understand data persistence, caching strategies, and data structures.
                -   *Example*: "Vue.js" is highly transferable to "React" requirements (both are modern component-based JS frameworks).
                -   *Example*: "C++" implies strong memory management skills relevant to "Rust" or "Go" roles.
            2.  **Scoring Rubric**:
                -   **1.000 (Perfect Match)**: Has all required skills OR highly equivalent alternatives (e.g., has "PyTorch" when "TensorFlow" is required).
                -   **0.700 - 0.990 (Strong Potential)**: Missing 1 core skill but has a very related skill (e.g., has "MongoDB" but job requires "MySQL"). The learning curve is expected to be short.
                -   **0.400 - 0.690 (Partial Match)**: Has foundational skills but lacks specific tools (e.g., has "Java" but missing "Spring Boot").
                -   **0.000 - 0.390 (Weak Match)**: Skills are in completely different domains (e.g., "Graphic Design" vs "Backend Engineering").
            3.  **Context Awareness**:
                -   If a candidate lists "Spring Boot", assume they know "Spring" and "Java".
                -   If a candidate lists "Docker", assume basic familiarity with "Linux" and "Networking".
            
            ### IMPORTANT OUTPUT RULES:
            - Return EXACTLY ONE score per candidate (NOT one score per skill)
            - If there are 3 candidates, return exactly 3 scores: [score1, score2, score3]
            - If there is 1 candidate, return exactly 1 score: [score1]
            - Each score represents the candidate's OVERALL compatibility with ALL required skills
            
            ### Output Format:
            -   Return ONLY a valid JSON array of floating-point numbers.
            -   Range: 0.000 to 1.000.
            -   Precision: 3 decimal places.
            -   Do NOT output explanations, markdown code blocks (``json), or extra text.
            
            Example Output: [1.000, 0.850, 0.400]
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Analyze the following candidates against the required skills.
            
            ### Required Skills:
            %s
            
            ### Candidate Skill Sets:
            %s
            
            Provide the match scores as a JSON array.
            """;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[([^]]*)]");

    public SkillMatcher() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.apiKey = config.getApiKey();
        this.modelOption = config.getQwen();
        this.objectMapper = new ObjectMapper();

        log.info("SkillMatcher initialized with model: {}", modelOption.getModel());
    }

    /**
     * Calculate match rate for a single candidate against target skills
     * 
     * @param candidateSkills List of skills for one candidate
     * @param targetSkills List of required/target skills to match against
     * @return Match rate (0.000 - 1.000)
     */
    public Float matchSingle(List<String> candidateSkills, List<String> targetSkills) {
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            throw new IllegalArgumentException("Candidate skills cannot be null or empty");
        }
        
        List<List<String>> candidateList = List.of(candidateSkills);
        List<Float> results = match(candidateList, targetSkills);
        
        return results.isEmpty() ? 0.0f : results.getFirst();
    }

    /**
     * Calculate match rates for multiple candidates against target skills
     * 
     * @param candidateSkills List of skill lists, each representing one candidate's skills
     * @param targetSkills List of required/target skills to match against
     * @return List of match rates (0.000 - 1.000) for each candidate
     */
    public List<Float> match(List<List<String>> candidateSkills, List<String> targetSkills) {
        validateInput(candidateSkills, targetSkills);

        log.info("Starting skill matching for {} candidates against {} target skills",
                candidateSkills.size(), targetSkills.size());

        try {
            // Build prompt with skill data
            String prompt = buildPrompt(candidateSkills, targetSkills);
            log.debug("Prompt: {}", prompt);

            // Call Qwen-Max API
            String response = callQwenMax(prompt);
            log.debug("AI response: {}", response);

            // Parse response to List<Float>
            List<Float> matchRates = parseResponse(response);
            log.info("Match rates calculated: {}", matchRates);

            return matchRates;

        } catch (Exception e) {
            log.error("Skill matching failed", e);
            throw new RuntimeException("Failed to calculate match rates: " + e.getMessage(), e);
        }
    }

    /**
     * Validate input parameters
     */
    private void validateInput(List<List<String>> candidateSkills, List<String> targetSkills) {
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            throw new IllegalArgumentException("Candidate skills cannot be null or empty");
        }
        if (targetSkills == null || targetSkills.isEmpty()) {
            throw new IllegalArgumentException("Target skills cannot be null or empty");
        }

        for (int i = 0; i < candidateSkills.size(); i++) {
            List<String> skills = candidateSkills.get(i);
            if (skills == null || skills.isEmpty()) {
                throw new IllegalArgumentException("Candidate " + i + " has null or empty skills");
            }
        }
    }

    /**
     * Build prompt with skill data
     */
    private String buildPrompt(List<List<String>> candidateSkills, List<String> targetSkills) {

        // Format target skills
        String targetSkillsStr = String.join(", ", targetSkills);

        // Format candidate skills
        StringBuilder candidatesSb = new StringBuilder();
        for (int i = 0; i < candidateSkills.size(); i++) {
            candidatesSb.append(String.format("Candidate %d: %s\n",
                    i + 1,
                    String.join(", ", candidateSkills.get(i))));
        }

        return String.format(USER_PROMPT_TEMPLATE, targetSkillsStr, candidatesSb.toString().trim());
    }

    /**
     * Call Qwen-Max API to calculate match rates
     */
    private String callQwenMax(String userPrompt) {
        try {
            Generation generation = new Generation();

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(SYSTEM_PROMPT)
                    .build();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userPrompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(modelOption.getModel())
                    .messages(List.of(systemMessage, userMessage))
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
            throw new RuntimeException("Qwen API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse AI response to List<Float>
     */
    private List<Float> parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Empty response from AI");
        }

        try {
            // Extract JSON array
            String jsonStr = extractJsonArray(response);
            if (jsonStr == null) {
                jsonStr = response.trim();
            }

            // Parse to List<Double> first (Jackson doesn't directly support Float)
            List<Double> doubles = objectMapper.readValue(
                    jsonStr,
                    new TypeReference<>() {}
            );

            if (doubles == null || doubles.isEmpty()) {
                throw new RuntimeException("No match rates parsed from response");
            }

            // Convert to List<Float> and validate range
            List<Float> matchRates = new ArrayList<>();
            for (Double value : doubles) {
                float rate = value.floatValue();

                // Validate range [0.00, 1.00]
                if (rate < 0.0f || rate > 1.0f) {
                    log.warn("Match rate {} out of range, clamping to [0.000, 1.000]", rate);
                    rate = Math.max(0.0f, Math.min(1.0f, rate));
                }

                // Round to 3 decimal places
                rate = Math.round(rate * 1000.0f) / 1000.0f;

                matchRates.add(rate);
            }

            return matchRates;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse match rates from response: {}", response, e);
            throw new RuntimeException("Failed to parse match rates: " + e.getMessage(), e);
        }
    }

    /**
     * Extract JSON array from text using regex
     */
    private String extractJsonArray(String text) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        return matcher.find() ? "[" + matcher.group(1) + "]" : null;
    }
}
