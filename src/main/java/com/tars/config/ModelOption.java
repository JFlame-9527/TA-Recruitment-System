package com.tars.config;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration class for AI model parameters.
 * <p>
 * This class encapsulates all configurable parameters for interacting with
 * Qwen AI models through the DashScope API. It supports different model types:
 * <ul>
 *   <li><b>Generation models</b> (qwen, qwen-long): For text generation and analysis</li>
 *   <li><b>Embedding models</b> (qwen-vector): For vector embedding generation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Parameter Categories:</b>
 * <ul>
 *   <li><b>Model Identity</b>: {@code model} - Specifies which AI model to use</li>
 *   <li><b>Sampling Control</b>: {@code temperature}, {@code topP}, {@code topK} - Control response randomness and diversity</li>
 *   <li><b>Output Control</b>: {@code repetitionPenalty}, {@code maxTokens} - Control output length and repetitiveness</li>
 *   <li><b>Embedding Specific</b>: {@code dimension} - Vector dimension for embedding models</li>
 * </ul>
 * </p>
 * <p>
 * <b>Example configuration in qwen_config.json:</b>
 * <pre>{@code
 * {
 *   "qwen": {
 *     "model": "qwen-max",
 *     "temperature": 0.7,
 *     "topP": 0.8,
 *     "topK": 50,
 *     "repetitionPenalty": 1.1,
 *     "maxTokens": 2000,
 *     "dimension": 0
 *   },
 *   "qwenVector": {
 *     "model": "text-embedding-v2",
 *     "temperature": 0.0,
 *     "topP": 1.0,
 *     "topK": 0,
 *     "repetitionPenalty": 1.0,
 *     "maxTokens": 0,
 *     "dimension": 1536
 *   }
 * }
 * }</pre>
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see QwenConfiguration
 * @see com.tars.ai.PortraitGenerator
 * @see com.tars.ai.SkillExtractor
 * @see com.tars.ai.SkillMatcher
 */
@Data
@Builder
public class ModelOption {

    /**
     * The AI model identifier to use for API calls.
     * <p>
     * Common model identifiers:
     * <ul>
     *   <li>{@code qwen-max} - High-performance general purpose model</li>
     *   <li>{@code qwen-plus} - Balanced performance and cost</li>
     *   <li>{@code qwen-turbo} - Fast response for simple tasks</li>
     *   <li>{@code qwen-long} - Optimized for long document processing</li>
     *   <li>{@code text-embedding-v2} - Vector embedding generation</li>
     * </ul>
     * </p>
     *
     * @see #getTemperature()
     */
    private String model;

    /**
     * Controls randomness in the model's output.
     * <p>
     * Temperature affects how deterministic or creative the model's responses are:
     * <ul>
     *   <li><b>0.0 - 0.3</b>: Very deterministic, focused, consistent (good for factual tasks)</li>
     *   <li><b>0.4 - 0.7</b>: Balanced creativity and consistency (recommended for most tasks)</li>
     *   <li><b>0.8 - 1.0</b>: More creative, diverse responses (good for brainstorming)</li>
     *   <li><b>&gt; 1.0</b>: Highly random, may produce incoherent outputs</li>
     * </ul>
     * </p>
     * <p>
     * <b>Recommended values:</b>
     * <ul>
     *   <li>Portrait generation: 0.7</li>
     *   <li>Skill extraction: 0.3</li>
     *   <li>Skill matching: 0.5</li>
     *   <li>Embedding models: 0.0 (not applicable)</li>
     * </ul>
     * </p>
     *
     * @see #getTopP()
     * @see #getTopK()
     */
    private float temperature;

    /**
     * Nucleus sampling parameter that controls diversity via probability mass threshold.
     * <p>
     * Top-P (nucleus sampling) selects tokens from the smallest set whose cumulative
     * probability exceeds the threshold. This provides more dynamic control than temperature alone.
     * </p>
     * <ul>
     *   <li><b>0.1 - 0.5</b>: Very focused, high-confidence tokens only</li>
     *   <li><b>0.6 - 0.9</b>: Balanced diversity (recommended)</li>
     *   <li><b>0.95 - 1.0</b>: Maximum diversity, considers all tokens</li>
     * </ul>
     * <p>
     * <b>Interaction with Temperature:</b> Top-P and temperature can be used together.
     * Lower Top-P makes the model more focused, while higher temperature adds creativity
     * within that focused set.
     * </p>
     *
     * @see #getTemperature()
     */
    private double topP;

    /**
     * Top-K sampling parameter that limits token selection to the K most likely options.
     * <p>
     * Top-K restricts the model to consider only the top K most probable next tokens
     * at each generation step. This prevents the model from considering very unlikely tokens.
     * </p>
     * <ul>
     *   <li><b>10 - 30</b>: Very conservative, high quality but less diverse</li>
     *   <li><b>40 - 100</b>: Balanced quality and diversity (recommended)</li>
     *   <li><b>&gt; 100</b>: More diverse, may include lower quality tokens</li>
     *   <li><b>0</b>: Disabled (consider all tokens)</li>
     * </ul>
     *
     * @see #getTopP()
     */
    private int topK;

    /**
     * Penalty applied to tokens that have already appeared in the output.
     * <p>
     * Repetition penalty reduces the likelihood of the model repeating the same
     * words or phrases. Higher values make repetition less likely.
     * </p>
     * <ul>
     *   <li><b>1.0</b>: No penalty (default)</li>
     *   <li><b>1.0 - 1.2</b>: Mild penalty, allows some repetition (recommended)</li>
     *   <li><b>1.2 - 1.5</b>: Strong penalty, significantly reduces repetition</li>
     *   <li><b>&gt; 1.5</b>: May cause unnatural phrasing to avoid repetition</li>
     * </ul>
     * <p>
     * <b>Use case:</b> Useful for longer generations where the model might get stuck
     * in repetitive loops.
     * </p>
     */
    private float repetitionPenalty;

    /**
     * Maximum number of tokens the model can generate in a single response.
     * <p>
     * This parameter limits the length of the AI's output. One token is approximately
     * 0.75 words in English or 1 character in Chinese.
     * </p>
     * <ul>
     *   <li><b>500 - 1000</b>: Short responses (skill lists, summaries)</li>
     *   <li><b>1500 - 2500</b>: Medium responses (portrait generation)</li>
     *   <li><b>3000 - 8000</b>: Long responses (detailed analysis, document extraction)</li>
     * </ul>
     * <p>
     * <b>Note:</b> Setting this too low may truncate important information.
     * Setting it too high may increase API costs and response time.
     * </p>
     *
     * @see #getModel()
     */
    private int maxTokens;

    /**
     * Dimensionality of the vector embedding for embedding models.
     * <p>
     * This parameter specifies the number of dimensions in the output vector
     * when using embedding models like {@code text-embedding-v2}.
     * </p>
     * <ul>
     *   <li><b>768</b>: Lower dimension, faster, less precise</li>
     *   <li><b>1536</b>: Standard dimension, balanced (common default)</li>
     *   <li><b>3072</b>: Higher dimension, more precise, slower</li>
     * </ul>
     * <p>
     * <b>Important:</b> This parameter is only relevant for embedding models.
     * For generation models (qwen, qwen-long), this value should be 0 or ignored.
     * All vectors being compared must have the same dimension.
     * </p>
     *
     * @see com.tars.entity.bean.Portrait
     */
    private int dimension;
}
