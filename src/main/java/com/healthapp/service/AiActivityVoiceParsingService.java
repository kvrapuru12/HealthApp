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
import java.util.List;

@Service
public class AiActivityVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiActivityVoiceParsingService.class);
    
    private String getSystemPrompt() {
        String currentDateTime = LocalDateTime.now().toString();
        return String.format("""
        You are an AI assistant that parses natural language descriptions of physical activities into structured data.
        
        CURRENT DATE AND TIME: %s
        
        Parse the input text and return a JSON object with the following structure:
        {
            "activityName": "string - the name of the activity",
            "durationMinutes": "number - duration in minutes",
            "loggedAt": "ISO 8601 datetime string (YYYY-MM-DDTHH:mm:ss format)",
            "note": "string - see rule 4: Voice / optional Stated / optional Assumed (same labeling idea as food voice logs)"
        }
        
        Rules:
        1. Extract the activity name clearly (e.g., "brisk walk", "yoga", "swimming")
        2. Convert time references to ISO 8601 format (YYYY-MM-DDTHH:mm:ss) using the CURRENT DATE AND TIME above:
           - "this morning" → today's date at 08:00:00
           - "yesterday evening" → yesterday's date at 18:00:00
           - "last night" → yesterday's date at 21:00:00
           - "today" → today's date at current time (use the CURRENT DATE AND TIME provided)
           - "now" → current date and time (use the CURRENT DATE AND TIME provided)
        3. If no specific time is mentioned, use the CURRENT DATE AND TIME provided above when inferring loggedAt
        4. **note** field (required): always include **`Voice:`** with the user's exact words. If they explicitly stated duration and/or when the activity occurred, add **`Stated:`** with only those facts. If you inferred durationMinutes and/or loggedAt, add **`Assumed:`** with concrete numbers and datetimes (omit the whole Assumed clause if nothing was inferred). Never `Assumed: none`.
        5. Return ONLY valid JSON, no additional text
        6. IMPORTANT: loggedAt must be in ISO 8601 format (YYYY-MM-DDTHH:mm:ss) and must use the CURRENT DATE AND TIME provided above
        
        %s
        %s
        """, currentDateTime,
                AiPromptGuidelines.SHARED_INFERENCE_PRINCIPLES,
                AiPromptGuidelines.ACTIVITY_DURATION_ASSUMPTION_RULES);
    }

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    public ParsedActivityData parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing activity voice text: {}", voiceText);

            if (openAiService == null) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage("system", getSystemPrompt()),
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
            } else {
                data.setNote("");
            }
            ensureVoiceLineInNote(data, voiceText);

            logger.info("Successfully parsed activity: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error parsing activity voice text: {}", e.getMessage(), e);
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse activity information from voice text: " + e.getMessage(), e);
        }
    }

    /**
     * Food voice logs always separate the utterance from Stated/Assumed; mirror that by guaranteeing a Voice line
     * even when the model returns only a fragment or the legacy "note = raw text" shape.
     */
    static void ensureVoiceLineInNote(ParsedActivityData data, String voiceText) {
        if (voiceText == null) {
            voiceText = "";
        }
        String trimmedVoice = voiceText.trim();
        String note = data.getNote() == null ? "" : data.getNote().trim();
        if (note.contains("Voice:")) {
            return;
        }
        String voiceLine = "Voice: " + trimmedVoice;
        if (note.isEmpty()) {
            data.setNote(voiceLine);
            return;
        }
        data.setNote(voiceLine + " " + note);
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
