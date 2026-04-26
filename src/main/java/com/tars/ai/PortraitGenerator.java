package com.tars.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tars.config.ModelOption;
import com.tars.config.QwenConfiguration;
import com.tars.entity.bean.Portrait;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import jakarta.servlet.http.Part;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class PortraitGenerator {

    private final String apiKey;
    private final ModelOption qwen;
    private final ModelOption qwenLong;
    private final ModelOption qwenVector;
    private final ObjectMapper objectMapper;
    private final FileParser fileParser;

    private static final String TA_SYSTEM_PROMPT = """
            You are an expert Technical Recruiter specializing in creating comprehensive candidate profiles.
            
            Task: Analyze the candidate's resume and profile information to generate a structured professional portrait.
            
            Extraction Rules:
            1. **Skills**: Extract ALL technical skills mentioned in the resume. Include programming languages, frameworks, tools, databases, cloud platforms, and methodologies.
            2. **Experience**: Create a concise narrative summary (2-4 sentences) of the candidate's work experience, focusing on:
               - Years of relevant experience
               - Key projects or achievements
               - Domain expertise
               - Leadership or collaboration experience
            3. **Soft Skills**: Infer soft skills from:
               - Project descriptions (e.g., "led team" → Leadership)
               - Collaboration mentions (e.g., "worked with cross-functional teams" → Communication)
               - Problem-solving contexts (e.g., "optimized performance" → Analytical Thinking)
               - Only include soft skills that have clear evidence
            
            Output Format:
            Return ONLY a valid JSON object with exactly these three fields:
            {
              "skills": ["skill1", "skill2", ...],
              "experience": "narrative string describing work experience",
              "softSkills": ["soft-skill1", "soft-skill2", ...]
            }
            
            Do NOT output explanations, markdown code blocks, or extra text.
            """;

    private static final String TA_USER_PROMPT_TEMPLATE = """
            Generate a professional portrait for this Technical Assistant candidate.
            
            ### Candidate Profile:
            - Name: %s
            - Education: %s, %s, %s degree
            - Existing Skills: %s
            
            ### Resume Content:
            %s
            
            Provide the portrait as a JSON object with skills, experience, and soft-skills.
            """;

    private static final String POSITION_SYSTEM_PROMPT = """
            You are a Senior Hiring Manager creating an ideal candidate profile for a technical position.
            
            Task: Analyze the job description and requirements to generate a portrait of the ideal candidate.
            
            Analysis Rules:
            1. **Skills**: Identify ALL required and preferred technical skills from the job description. Include:
               - Core programming languages and frameworks
               - Required tools and platforms
               - Methodologies and best practices
               - Mark essential skills vs nice-to-have when possible
            2. **Experience**: Describe the ideal candidate's background (2-4 sentences):
               - Required years of experience level
               - Type of projects they should have worked on
               - Industry or domain experience needed
               - Expected responsibilities and scope
            3. **Soft Skills**: Derive soft skills from job requirements:
               - Team collaboration needs (e.g., "work with stakeholders" → Communication)
               - Project complexity (e.g., "manage multiple projects" → Time Management)
               - Learning requirements (e.g., "adapt to new technologies" → Adaptability)
               - Leadership expectations if any
            
            Output Format:
            Return ONLY a valid JSON object with exactly these three fields:
            {
              "skills": ["skill1", "skill2", ...],
              "experience": "narrative string describing ideal experience",
              "softSkills": ["soft-skill1", "soft-skill2", ...]
            }
            
            Do NOT output explanations, markdown code blocks, or extra text.
            """;

    private static final String POSITION_USER_PROMPT_TEMPLATE = """
            Generate an ideal candidate portrait for this position.
            
            ### Position Details:
            - Title: %s
            - Module: %s (%s)
            - Description: %s
            - Required Skills: %s
            - Duration: %d weeks
            - Weekly Workload: %.1f hours
            
            Provide the portrait as a JSON object with skills, experience, and soft-skills.
            """;

    public PortraitGenerator() {
        QwenConfiguration config = QwenConfiguration.getInstance();
        this.apiKey = config.getApiKey();
        this.qwen = config.getQwen();
        this.qwenLong = config.getQwenLong();
        this.qwenVector = config.getQwenVector();
        this.objectMapper = new ObjectMapper();
        this.fileParser = new FileParser();

        log.info("PortraitGenerator initialized with models: qwen={}, qwen-long={}, qwen-vector={}",
                qwen.getModel(), qwenLong.getModel(), qwenVector.getModel());
    }

    /**
     * Generate portrait for TA candidate from profile and resume
     * Falls back to profile-only data if resume processing fails
     *
     * @param profile TA profile information
     * @param resume  Uploaded resume file
     * @return Vectorized portrait object
     */
    public Portrait generatePortrait(TAProfile profile, Part resume) {
        log.info("Generating portrait for TA: {}", profile.getName());

        try {
            String fileId = fileParser.extractFileId(resume);
            log.info("FileId obtained: {}", fileId);

            String resumeContent = extractResumeContent(fileId);
            log.debug("Resume content extracted, length: {}", resumeContent.length());

            PortraitRaw portrait = generateTAPortrait(profile, resumeContent);
            Portrait vectorizedPortrait = vectorizePortrait(portrait);

            log.info("TA portrait generated and vectorized successfully");
            return vectorizedPortrait;

        } catch (Exception e) {
            log.warn("Resume processing failed for {}, falling back to profile-only data", profile.getName(), e);
            try {
                PortraitRaw portrait = generateTAPortraitFromProfileOnly(profile);
                Portrait vectorizedPortrait = vectorizePortrait(portrait);
                log.info("TA portrait generated from profile only");
                return vectorizedPortrait;
            } catch (Exception ex) {
                log.error("Portrait generation failed completely for {}", profile.getName(), ex);
                throw new RuntimeException("Failed to generate TA portrait: " + ex.getMessage(), ex);
            }
        }
    }

    public Portrait generatePortrait(TAProfile profile, File resume) {
        log.info("Generating portrait for TA: {}", profile.getName());

        try {
            String fileId = fileParser.extractFileId(resume);
            log.info("FileId obtained: {}", fileId);

            String resumeContent = extractResumeContent(fileId);
            log.debug("Resume content extracted, length: {}", resumeContent.length());

            PortraitRaw portrait = generateTAPortrait(profile, resumeContent);
            Portrait vectorizedPortrait = vectorizePortrait(portrait);

            log.info("TA portrait generated and vectorized successfully");
            return vectorizedPortrait;

        } catch (Exception e) {
            log.warn("Resume processing failed for {}, falling back to profile-only data", profile.getName(), e);
            try {
                PortraitRaw portrait = generateTAPortraitFromProfileOnly(profile);
                Portrait vectorizedPortrait = vectorizePortrait(portrait);
                log.info("TA portrait generated from profile only");
                return vectorizedPortrait;
            } catch (Exception ex) {
                log.error("Portrait generation failed completely for {}", profile.getName(), ex);
                throw new RuntimeException("Failed to generate TA portrait: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Generate portrait for Position
     *
     * @param position Position details
     * @return Vectorized portrait object
     */
    public Portrait generatePortrait(Position position) {
        log.info("Generating portrait for position: {}", position.getTitle());

        try {
            PortraitRaw portrait = generatePositionPortrait(position);
            Portrait vectorizedPortrait = vectorizePortrait(portrait);

            log.info("Position portrait generated and vectorized successfully");
            return vectorizedPortrait;

        } catch (Exception e) {
            log.error("Position portrait generation failed for {}", position.getTitle(), e);
            throw new RuntimeException("Failed to generate position portrait: " + e.getMessage(), e);
        }
    }

    /**
     * Extract raw resume content using qwen-long
     */
    private String extractResumeContent(String fileId) {
        try {
            Generation generation = new Generation();

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("Extract and return the complete text content from this resume. Preserve all relevant information including work experience, education, skills, and projects.")
                    .build();

            Message fileMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("fileid://" + fileId)
                    .build();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content("Please extract all text content from this resume.")
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(qwenLong.getModel())
                    .messages(List.of(systemMessage, fileMessage, userMessage))
                    .temperature(qwenLong.getTemperature())
                    .topP(qwenLong.getTopP())
                    .topK(qwenLong.getTopK())
                    .repetitionPenalty(qwenLong.getRepetitionPenalty())
                    .maxTokens(qwenLong.getMaxTokens())
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
            throw new RuntimeException("Failed to extract resume content: " + e.getMessage(), e);
        }
    }

    /**
     * Generate TA portrait from profile and resume content
     */
    private PortraitRaw generateTAPortrait(TAProfile profile, String resumeContent) {
        String prompt = String.format(TA_USER_PROMPT_TEMPLATE,
                profile.getName(),
                profile.getCollege(),
                profile.getMajor(),
                profile.getDegree(),
                profile.getSkills() != null ? String.join(", ", profile.getSkills()) : "None",
                resumeContent);

        String response = callQwenForPortrait(TA_SYSTEM_PROMPT, prompt, qwen);
        return parsePortraitResponse(response);
    }

    /**
     * Generate TA portrait from profile only (fallback)
     */
    private PortraitRaw generateTAPortraitFromProfileOnly(TAProfile profile) {
        String prompt = String.format(TA_USER_PROMPT_TEMPLATE,
                profile.getName(),
                profile.getCollege(),
                profile.getMajor(),
                profile.getDegree(),
                profile.getSkills() != null ? String.join(", ", profile.getSkills()) : "None",
                "No resume available. Use only the profile information provided above.");

        String response = callQwenForPortrait(TA_SYSTEM_PROMPT, prompt, qwen);
        return parsePortraitResponse(response);
    }

    /**
     * Generate Position portrait
     */
    private PortraitRaw generatePositionPortrait(Position position) {
        String prompt = String.format(POSITION_USER_PROMPT_TEMPLATE,
                position.getTitle(),
                position.getModuleCode(),
                position.getModuleName(),
                position.getDescription(),
                position.getSkills() != null ? String.join(", ", position.getSkills()) : "None",
                position.getDuration(),
                position.getWeeklyWorkload());

        String response = callQwenForPortrait(POSITION_SYSTEM_PROMPT, prompt, qwen);
        return parsePortraitResponse(response);
    }

    /**
     * Call Qwen API for portrait generation
     */
    private String callQwenForPortrait(String systemPrompt, String userPrompt, ModelOption model) {
        try {
            Generation generation = new Generation();

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userPrompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model.getModel())
                    .messages(List.of(systemMessage, userMessage))
                    .temperature(model.getTemperature())
                    .topP(model.getTopP())
                    .topK(model.getTopK())
                    .repetitionPenalty(model.getRepetitionPenalty())
                    .maxTokens(model.getMaxTokens())
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
     * Parse AI response to PortraitData object
     */
    private PortraitRaw parsePortraitResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Empty response from AI");
        }

        try {
            return objectMapper.readValue(response, PortraitRaw.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse portrait response: {}", response, e);
            throw new RuntimeException("Failed to parse portrait: " + e.getMessage(), e);
        }
    }

    /**
     * Vectorize portrait fields using qwenVector embedding model
     */
    private Portrait vectorizePortrait(PortraitRaw portrait) {
        try {
            List<Float> skillsVector = generateEmbedding(formatSkillsForEmbedding(portrait.getSkills()));
            List<Float> experienceVector = generateEmbedding(portrait.getExperience());
            List<Float> softSkillsVector = generateEmbedding(formatSoftSkillsForEmbedding(portrait.getSoftSkills()));

            return new Portrait(skillsVector, experienceVector, softSkillsVector);

        } catch (Exception e) {
            log.error("Failed to vectorize portrait", e);
            throw new RuntimeException("Failed to vectorize portrait: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embedding for a text string
     */
    private List<Float> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            TextEmbedding textEmbedding = new TextEmbedding();

            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(qwenVector.getModel())
                    .texts(Collections.singletonList(text))
                    .dimension(qwenVector.getDimension())
                    .build();

            TextEmbeddingResult result = textEmbedding.call(param);

            if (result == null || result.getOutput() == null ||
                    result.getOutput().getEmbeddings() == null ||
                    result.getOutput().getEmbeddings().isEmpty()) {
                throw new RuntimeException("Empty embedding response");
            }

            List<Double> doubleVector = result.getOutput().getEmbeddings().getFirst().getEmbedding();

            List<Float> floatVector = new ArrayList<>();
            for (Double value : doubleVector) {
                floatVector.add(value.floatValue());
            }

            return floatVector;

        } catch (NoApiKeyException e) {
            throw new RuntimeException("Embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Format skills array for embedding
     */
    private String formatSkillsForEmbedding(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        return "Technical skills: " + String.join(", ", skills);
    }

    /**
     * Format soft skills array for embedding
     */
    private String formatSoftSkillsForEmbedding(List<String> softSkills) {
        if (softSkills == null || softSkills.isEmpty()) {
            return "";
        }
        return "Soft skills: " + String.join(", ", softSkills);
    }

    /**
     * Raw portrait data structure for JSON parsing
     */
    @Data
    private static class PortraitRaw {
        private List<String> skills;
        private String experience;
        private List<String> softSkills;
    }
}
