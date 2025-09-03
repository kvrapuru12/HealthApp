package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AiVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiVoiceParsingService.class);
                    private static final String SYSTEM_PROMPT = """
                        You are an AI assistant that parses natural language descriptions of physical activities into structured data.
                        
                        Parse the input text and return a JSON object with the following structure:
                        {
                            "activityName": "string - the name of the activity",
                            "durationMinutes": "number - duration in minutes",
                            "loggedAt": "ISO 8601 datetime string (YYYY-MM-DDTHH:mm:ss format)",
                            "note": "string - optional additional context"
                        }
                        
                        Rules:
                        1. Extract the activity name clearly (e.g., "brisk walk", "yoga", "swimming")
                        2. Convert time references to ISO 8601 format (YYYY-MM-DDTHH:mm:ss):
                           - "this morning" → today's date at 08:00:00
                           - "yesterday evening" → yesterday's date at 18:00:00
                           - "last night" → yesterday's date at 21:00:00
                           - "today" → today's date at current time (use actual current time)
                           - "now" → current date and time
                        3. If no specific time is mentioned, use current time
                        4. Keep notes concise and relevant
                        5. Return ONLY valid JSON, no additional text
                        6. IMPORTANT: loggedAt must be in ISO 8601 format (YYYY-MM-DDTHH:mm:ss)
                        """;

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    public ParsedActivityData parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing voice text: {}", voiceText);

            if (openAiService == null) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage("system", SYSTEM_PROMPT),
                            new ChatMessage("user", voiceText)
                    ))
                    .maxTokens(500)
                    .temperature(0.1)
                    .build();

            var completion = openAiService.createChatCompletion(request);
            String response = completion.getChoices().get(0).getMessage().getContent();

            logger.info("Raw AI response: '{}'", response);

                                    // Parse the JSON response
                        logger.info("Attempting to parse JSON from AI response");
                        JsonNode jsonNode = objectMapper.readTree(response);
                        
                        ParsedActivityData data = new ParsedActivityData();
                        data.setActivityName(jsonNode.get("activityName").asText());
                        data.setDurationMinutes(jsonNode.get("durationMinutes").asInt());
                        
                        // Parse loggedAt with fallback to current time if parsing fails
                        String loggedAtText = jsonNode.get("loggedAt").asText();
                        try {
                            data.setLoggedAt(LocalDateTime.parse(loggedAtText));
                        } catch (Exception e) {
                            logger.warn("Failed to parse loggedAt '{}', using current time", loggedAtText);
                            data.setLoggedAt(LocalDateTime.now());
                        }
                        
                        if (jsonNode.has("note") && !jsonNode.get("note").isNull()) {
                            data.setNote(jsonNode.get("note").asText());
                        }

            logger.info("Successfully parsed activity: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error parsing voice text: {}", e.getMessage(), e);
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse activity information from voice text: " + e.getMessage(), e);
        }
    }

    public static class ParsedActivityData {
        private String activityName;
        private Integer durationMinutes;
        private LocalDateTime loggedAt;
        private String note;

        public ParsedActivityData() {}

        public String getActivityName() {
            return activityName;
        }

        public void setActivityName(String activityName) {
            this.activityName = activityName;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public LocalDateTime getLoggedAt() {
            return loggedAt;
        }

        public void setLoggedAt(LocalDateTime loggedAt) {
            this.loggedAt = loggedAt;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        @Override
        public String toString() {
            return String.format("ParsedActivityData{activityName='%s', durationMinutes=%d, loggedAt=%s, note='%s'}",
                    activityName, durationMinutes, loggedAt, note);
        }
    }
}
