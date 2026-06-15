package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthapp.config.OpenAiModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

/**
 * OpenAI Chat Completions client with Structured Outputs (json_schema) support.
 * Uses RestTemplate for reliable schema-based responses across gpt-4o+ models.
 */
@Service
public class OpenAiChatClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiChatClient.class);
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final OpenAiModelProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public OpenAiChatClient(OpenAiModelProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public boolean isAvailable() {
        return properties.hasValidApiKey();
    }

    private static final String RETRY_APPENDIX =
            "\n\nPrevious attempt failed validation. Return ONLY valid JSON matching the required schema. No markdown.";

    /**
     * Calls OpenAI with json_schema structured output. Retries on failure while keeping the full system prompt.
     */
    public String createStructuredCompletion(String model, String systemPrompt, String userText,
                                             JsonNode jsonSchema, String schemaName, int maxTokens) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
        }
        int maxRetries = Math.max(0, properties.getVoiceMaxRetries());
        RuntimeException lastError = null;
        String retryPrompt = systemPrompt;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String response = callChatCompletions(
                        model, retryPrompt, userText, jsonSchema, schemaName, maxTokens);
                logger.debug("OpenAI structured response (model={}, attempt={}): {}", model, attempt, response);
                return response;
            } catch (RuntimeException e) {
                lastError = e;
                logger.warn("OpenAI structured completion failed (model={}, attempt={}): {}", model, attempt, e.getMessage());
                if (attempt >= maxRetries) {
                    break;
                }
                if (isRateLimited(e)) {
                    sleepBackoff(attempt);
                }
                retryPrompt = systemPrompt + RETRY_APPENDIX;
            }
        }
        throw lastError != null ? lastError : new RuntimeException("OpenAI structured completion failed");
    }

    private static boolean isRateLimited(RuntimeException e) {
        String message = e.getMessage();
        return message != null && (message.contains("429") || message.toLowerCase(Locale.ROOT).contains("rate limit"));
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(500L * (1L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * JSON-object mode fallback when no schema is provided.
     */
    public String createJsonCompletion(String model, String systemPrompt, String userText, int maxTokens) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
        }
        return callChatCompletions(model, systemPrompt, userText, null, null, maxTokens);
    }

    private String callChatCompletions(String model, String systemPrompt, String userText,
                                       JsonNode jsonSchema, String schemaName, int maxTokens) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", properties.getVoiceTemperature());

            ArrayNode messages = body.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", userText);

            if (jsonSchema != null) {
                ObjectNode responseFormat = body.putObject("response_format");
                responseFormat.put("type", "json_schema");
                ObjectNode jsonSchemaWrapper = responseFormat.putObject("json_schema");
                jsonSchemaWrapper.put("name", schemaName != null ? schemaName : "response");
                jsonSchemaWrapper.put("strict", true);
                jsonSchemaWrapper.set("schema", jsonSchema);
            } else {
                ObjectNode responseFormat = body.putObject("response_format");
                responseFormat.put("type", "json_object");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey().trim());

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(CHAT_COMPLETIONS_URL, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode usage = root.get("usage");
            if (usage != null) {
                logger.info("OpenAI usage model={} prompt_tokens={} completion_tokens={}",
                        model,
                        usage.path("prompt_tokens").asInt(),
                        usage.path("completion_tokens").asInt());
            }
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("OpenAI returned no choices");
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI returned empty content");
            }
            return content;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }
}
