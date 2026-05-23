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

/**
 * Portrait generator that creates comprehensive candidate and position profiles using AI.
 * <p>
 * This class generates structured professional portraits for both Technical Assistant (TA) candidates
 * and job positions. Each portrait consists of three dimensions:
 * <ul>
 *   <li><b>Skills</b>: Technical and professional capabilities</li>
 *   <li><b>Experience</b>: Work history and achievements summary</li>
 *   <li><b>Soft Skills</b>: Interpersonal and behavioral competencies</li>
 * </ul>
 * </p>
 * <p>
 * The generated portraits are vectorized using embedding models to enable similarity calculations
 * and candidate-position matching through {@link PortraitMatcher}.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Multi-model orchestration (qwen, qwen-long, qwen-vector)</li>
 *   <li>Resume content extraction from various file formats (PDF, DOC, DOCX, TXT, MD)</li>
 *   <li>Fallback mechanism for profile-only generation when resume processing fails</li>
 *   <li>Vector embedding generation for semantic similarity matching</li>
 *   <li>Support for both multipart uploads and existing files</li>
 * </ul>
 * </p>
 * <p>
 * Model usage:
 * <ul>
 *   <li><b>qwen-long</b>: Extracts full text content from resume files</li>
 *   <li><b>qwen</b>: Generates structured portrait data from text</li>
 *   <li><b>qwen-vector</b>: Creates vector embeddings for similarity comparison</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see Portrait
 * @see PortraitMatcher
 * @see FileParser
 * @see QwenConfiguration
 */
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

    /**
     * Constructs a new PortraitGenerator instance.
     * <p>
     * Initializes the generator with configuration from {@link QwenConfiguration},
     * sets up three model configurations (qwen, qwen-long, qwen-vector),
     * creates an {@link ObjectMapper} for JSON processing, and instantiates
     * a {@link FileParser} for file upload operations.
     * </p>
     *
     * @throws RuntimeException if configuration initialization fails
     */
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
     * Generates a portrait for a TA candidate from profile and uploaded resume.
     * <p>
     * This method orchestrates the complete portrait generation process:
     * <ol>
     *   <li>Uploads the resume file to obtain a fileId</li>
     *   <li>Extracts full text content from the resume using qwen-long</li>
     *   <li>Generates structured portrait data using qwen model</li>
     *   <li>Vectorizes all portrait dimensions using qwen-vector</li>
     * </ol>
     * </p>
     * <p>
     * <b>Fallback Strategy:</b> If resume processing fails at any step, the method automatically
     * falls back to generating a portrait using only the profile information. This ensures
     * robustness even when resume files are corrupted or unreadable.
     * </p>
     *
     * @param profile TA profile information containing name, education, and existing skills
     * @param resume  Uploaded resume file as multipart data
     * @return Vectorized portrait object with skills, experience, and soft skills vectors
     * @throws RuntimeException if portrait generation fails completely (including fallback)
     * @see #generateTAPortrait(TAProfile, String)
     * @see #generateTAPortraitFromProfileOnly(TAProfile)
     * @see #vectorizePortrait(PortraitRaw)
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

    /**
     * Generates a portrait for a TA candidate from profile and existing resume file.
     * <p>
     * This is an overloaded version that accepts a {@link File} object instead of {@link Part}.
     * It follows the same workflow and fallback strategy as {@link #generatePortrait(TAProfile, Part)}.
     * </p>
     *
     * @param profile TA profile information containing name, education, and existing skills
     * @param resume  Existing resume file on disk
     * @return Vectorized portrait object with skills, experience, and soft skills vectors
     * @throws RuntimeException if portrait generation fails completely (including fallback)
     * @see #generatePortrait(TAProfile, Part)
     */
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
     * Generates a portrait for a job position.
     * <p>
     * This method analyzes the position details and requirements to create an ideal candidate profile.
     * The process includes:
     * <ol>
     *   <li>Building a prompt with position details (title, module, description, skills, etc.)</li>
     *   <li>Calling qwen model to generate structured portrait data</li>
     *   <li>Vectorizing all portrait dimensions for similarity matching</li>
     * </ol>
     * </p>
     * <p>
     * Unlike TA portrait generation, this method does not have a fallback mechanism
     * since position data is always structured and available.
     * </p>
     *
     * @param position Position details including title, description, required skills, duration, and workload
     * @return Vectorized portrait object representing the ideal candidate profile
     * @throws RuntimeException if position portrait generation fails
     * @see #generatePositionPortrait(Position)
     * @see #vectorizePortrait(PortraitRaw)
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
     * Extracts raw text content from a resume file using qwen-long model.
     * <p>
     * This method sends three messages to the AI model:
     * <ol>
     *   <li>System message instructing to extract complete text content</li>
     *   <li>File reference message with the fileId (format: {@code fileid://<fileId>})</li>
     *   <li>User message requesting text extraction</li>
     * </ol>
     * </p>
     * <p>
     * The qwen-long model is specifically chosen for its ability to handle long documents
     * and preserve formatting, structure, and all relevant information from resumes.
     * </p>
     *
     * @param fileId the file identifier obtained from file upload
     * @return Complete text content extracted from the resume
     * @throws RuntimeException if API call fails or returns empty response
     * @see #qwenLong
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

            if (result.getOutput() == null || result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
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
     * Generates a TA portrait from profile information and resume content.
     * <p>
     * This method combines structured profile data (name, education, existing skills)
     * with unstructured resume content to create a comprehensive candidate portrait.
     * The AI model analyzes both sources to extract:
     * <ul>
     *   <li>Technical skills from resume and profile</li>
     *   <li>Experience narrative based on work history</li>
     *   <li>Soft skills inferred from project descriptions and achievements</li>
     * </ul>
     * </p>
     *
     * @param profile TA profile containing structured information
     * @param resumeContent Full text content extracted from resume
     * @return Raw portrait object with skills, experience, and soft skills as strings
     * @see #TA_SYSTEM_PROMPT
     * @see #TA_USER_PROMPT_TEMPLATE
     * @see #callQwenForPortrait(String, String, ModelOption)
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
     * Generates a TA portrait from profile information only (fallback method).
     * <p>
     * This method is used when resume processing fails. It generates a portrait
     * using only the structured profile data, with a placeholder message indicating
     * that no resume is available. The AI model infers additional information
     * from the existing skills and educational background.
     * </p>
     * <p>
     * While less comprehensive than the full method, this fallback ensures that
     * candidates can still be evaluated even without a readable resume.
     * </p>
     *
     * @param profile TA profile containing structured information
     * @return Raw portrait object with skills, experience, and soft skills as strings
     * @see #generateTAPortrait(TAProfile, String)
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
     * Generates a position portrait from job details.
     * <p>
     * This method analyzes the position requirements to create an ideal candidate profile.
     * The AI model evaluates:
     * <ul>
     *   <li>Required and preferred technical skills</li>
     *   <li>Experience level and domain expertise needed</li>
     *   <li>Soft skills derived from job responsibilities</li>
     *   <li>Project complexity and collaboration requirements</li>
     * </ul>
     * </p>
     *
     * @param position Position details including title, module, description, skills, duration, and workload
     * @return Raw portrait object representing the ideal candidate profile
     * @see #POSITION_SYSTEM_PROMPT
     * @see #POSITION_USER_PROMPT_TEMPLATE
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
     * Calls the Qwen API for portrait generation.
     * <p>
     * This is a generic method used by all portrait generation workflows. It constructs
     * a two-message conversation (system + user) and sends it to the specified model.
     * </p>
     *
     * @param systemPrompt System message defining the AI's role and task instructions
     * @param userPrompt User message containing specific data to analyze
     * @param model Model configuration to use (qwen, qwen-long, etc.)
     * @return Raw text response from the AI model
     * @throws RuntimeException if API call fails or returns empty response
     * @see GenerationParam
     * @see Message
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

            if (result.getOutput() == null || result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
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
     * Parses the AI response into a PortraitRaw object.
     * <p>
     * This method uses Jackson ObjectMapper to deserialize the JSON response
     * into a {@link PortraitRaw} object. The expected JSON format is:
     * <pre>{@code
     * {
     *   "skills": ["Java", "Spring Boot", "MySQL"],
     *   "experience": "5 years of backend development...",
     *   "softSkills": ["Communication", "Leadership"]
     * }
     * }</pre>
     * </p>
     *
     * @param response Raw JSON response from AI model
     * @return Parsed PortraitRaw object
     * @throws RuntimeException if response is empty or JSON parsing fails
     * @see PortraitRaw
     * @see ObjectMapper#readValue(String, Class)
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
     * Vectorizes all portrait dimensions using the qwen-vector embedding model.
     * <p>
     * This method converts the textual portrait data into numerical vectors:
     * <ol>
     *   <li>Formats skills list into a single string</li>
     *   <li>Generates embedding for skills string</li>
     *   <li>Generates embedding for experience narrative</li>
     *   <li>Formats and generates embedding for soft skills list</li>
     *   <li>Constructs a Portrait object with all three vectors</li>
     * </ol>
     * </p>
     * <p>
     * These vectors enable semantic similarity calculations through cosine similarity,
     * allowing the system to match candidates with positions based on conceptual
     * proximity rather than exact keyword matching.
     * </p>
     *
     * @param portrait Raw portrait object with string-based data
     * @return Vectorized Portrait object suitable for similarity matching
     * @throws RuntimeException if embedding generation fails
     * @see #generateEmbedding(String)
     * @see #formatSkillsForEmbedding(List)
     * @see #formatSoftSkillsForEmbedding(List)
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
     * Generates a vector embedding for a text string using qwen-vector model.
     * <p>
     * This method converts textual data into a high-dimensional vector representation
     * that captures semantic meaning. The embedding process:
     * <ol>
     *   <li>Validates input text (returns empty list if null or empty)</li>
     *   <li>Calls the TextEmbedding API with the text</li>
     *   <li>Converts the Double[] response to List&lt;Float&gt;</li>
     * </ol>
     * </p>
     * <p>
     * The resulting vectors can be compared using cosine similarity to determine
     * semantic relatedness between different pieces of text.
     * </p>
     *
     * @param text The text to convert into a vector embedding
     * @return List of floats representing the embedding vector, or empty list if text is empty
     * @throws RuntimeException if embedding API call fails
     * @see TextEmbedding
     * @see #qwenVector
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
     * Formats a list of skills into a single string for embedding generation.
     * <p>
     * Converts a skill list like {@code ["Java", "Spring", "MySQL"]} into
     * {@code "Technical skills: Java, Spring, MySQL"}. This formatting helps
     * the embedding model understand the context and semantic category of the terms.
     * </p>
     *
     * @param skills List of skill strings
     * @return Formatted string with "Technical skills: " prefix, or empty string if skills is null/empty
     */
    private String formatSkillsForEmbedding(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        return "Technical skills: " + String.join(", ", skills);
    }

    /**
     * Formats a list of soft skills into a single string for embedding generation.
     * <p>
     * Converts a soft skill list like {@code ["Leadership", "Communication"]} into
     * {@code "Soft skills: Leadership, Communication"}. This formatting provides
     * context to the embedding model about the nature of these skills.
     * </p>
     *
     * @param softSkills List of soft skill strings
     * @return Formatted string with "Soft skills: " prefix, or empty string if softSkills is null/empty
     */
    private String formatSoftSkillsForEmbedding(List<String> softSkills) {
        if (softSkills == null || softSkills.isEmpty()) {
            return "";
        }
        return "Soft skills: " + String.join(", ", softSkills);
    }

    /**
     * Internal data structure for holding raw portrait data during JSON parsing.
     * <p>
     * This class serves as an intermediate representation between the AI's JSON response
     * and the final vectorized {@link Portrait} object. It contains the same three dimensions
     * but in human-readable string format rather than numerical vectors.
     * </p>
     *
     * @see Portrait
     * @see #parsePortraitResponse(String)
     */
    @Data
    private static class PortraitRaw {
        private List<String> skills;
        private String experience;
        private List<String> softSkills;
    }
}
