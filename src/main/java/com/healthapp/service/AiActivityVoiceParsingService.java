package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.OpenAiModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class AiActivityVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiActivityVoiceParsingService.class);

    @Autowired(required = false)
    private OpenAiChatClient openAiChatClient;

    @Autowired
    private OpenAiModelProperties modelProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode activityVoiceSchema;
    private volatile String cachedSystemPrompt;

    private static final int ACTIVITY_PARSE_MAX_TOKENS = 550;

    private String getSystemPrompt() {
        if (cachedSystemPrompt != null) {
            return cachedSystemPrompt;
        }
        cachedSystemPrompt = """
        You are an AI assistant that parses natural language descriptions of physical activities into structured data.
        
        Parse the input text and return a JSON object with an activities array (one entry per distinct activity).
        
        Rules:
        1. Extract each activity clearly (e.g., "brisk walk", "yoga", "weight training")
        2. When the user describes multiple activities (e.g. "cycled 40 minutes then did 25 minutes weights"), return multiple activities entries
        3. Convert time references to ISO 8601 using CURRENT DATETIME from the user message
        4. If no specific time is mentioned, use CURRENT DATETIME for loggedAt
        5. Each note field is required with Voice: line plus optional Stated:/Assumed:
        
        """
                + AiPromptGuidelines.SHARED_INFERENCE_PRINCIPLES
                + "\n"
                + AiPromptGuidelines.ACTIVITY_DURATION_ASSUMPTION_RULES;
        return cachedSystemPrompt;
    }

    private static String buildUserMessage(String voiceText) {
        return "CURRENT DATETIME: " + LocalDateTime.now() + "\n\n" + voiceText;
    }

    public ParsedActivityData parseVoiceText(String voiceText) {
        List<ParsedActivityData> activities = parseAllActivities(voiceText);
        if (activities.isEmpty()) {
            throw new RuntimeException("Unable to parse activity information from voice text");
        }
        return activities.get(0);
    }

    public List<ParsedActivityData> parseAllActivities(String voiceText) {
        return parseAllActivities(voiceText, true);
    }

    private List<ParsedActivityData> parseAllActivities(String voiceText, boolean allowCompoundSplit) {
        long startNs = System.nanoTime();
        try {
            logger.debug("Parsing activity voice text: {}", voiceText);

            if (openAiChatClient == null || !openAiChatClient.isAvailable()) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            long openAiStartNs = System.nanoTime();
            String response = openAiChatClient.createStructuredCompletion(
                    modelProperties.getVoiceActivityModel(),
                    getSystemPrompt(),
                    buildUserMessage(voiceText),
                    loadActivityVoiceSchema(),
                    "activity_voice_parse",
                    ACTIVITY_PARSE_MAX_TOKENS
            );
            long openAiMs = (System.nanoTime() - openAiStartNs) / 1_000_000;

            logger.debug("Raw AI response: '{}'", response);
            JsonNode jsonNode = objectMapper.readTree(AiFoodVoiceParsingService.extractJsonObject(response));

            List<ParsedActivityData> results = new ArrayList<>();
            JsonNode activitiesNode = jsonNode.get("activities");
            if (activitiesNode != null && activitiesNode.isArray()) {
                for (JsonNode activityNode : activitiesNode) {
                    ParsedActivityData data = parseActivityNode(activityNode);
                    ensureVoiceLineInNote(data, voiceText);
                    results.add(data);
                }
            } else if (jsonNode.has("activityName")) {
                ParsedActivityData data = parseActivityNode(jsonNode);
                ensureVoiceLineInNote(data, voiceText);
                results.add(data);
            }

            if (results.isEmpty()) {
                throw new RuntimeException("No activities parsed from voice text");
            }

            if (allowCompoundSplit && results.size() == 1) {
                results = maybeSplitCompoundActivities(voiceText, results.get(0));
            }

            long totalMs = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("perf activityVoiceParse totalMs={} openAiMs={} model={} maxTokens={} activities={}",
                    totalMs, openAiMs, modelProperties.getVoiceActivityModel(), ACTIVITY_PARSE_MAX_TOKENS, results.size());
            return results;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error parsing activity voice text: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to parse activity information from voice text: " + e.getMessage(), e);
        }
    }

    private static final int MAX_COMPOUND_ACTIVITY_SEGMENTS = 3;

    private List<ParsedActivityData> maybeSplitCompoundActivities(String voiceText, ParsedActivityData single) {
        if (voiceText == null || !voiceText.toLowerCase().contains(" then ")) {
            return List.of(single);
        }
        String[] parts = voiceText.split("(?i)\\s+then\\s+");
        if (parts.length < 2) {
            return List.of(single);
        }
        int limit = Math.min(parts.length, MAX_COMPOUND_ACTIVITY_SEGMENTS);
        if (parts.length > MAX_COMPOUND_ACTIVITY_SEGMENTS) {
            logger.warn("Truncating compound activity voice text from {} to {} segments", parts.length, limit);
        }
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String trimmed = parts[i].trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        if (segments.isEmpty()) {
            return List.of(single);
        }
        if (segments.size() == 1) {
            return List.of(single);
        }
        List<CompletableFuture<List<ParsedActivityData>>> futures = segments.stream()
                .map(segment -> CompletableFuture.supplyAsync(() -> parseAllActivities(segment, false)))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of(single);
        } catch (ExecutionException e) {
            logger.warn("Parallel compound activity parse failed, using single result: {}", e.getCause().getMessage());
            return List.of(single);
        }
        List<ParsedActivityData> split = new ArrayList<>();
        for (CompletableFuture<List<ParsedActivityData>> future : futures) {
            split.addAll(future.join());
        }
        return split.isEmpty() ? List.of(single) : split;
    }

    private ParsedActivityData parseActivityNode(JsonNode activityNode) {
        ParsedActivityData data = new ParsedActivityData();
        data.setActivityName(activityNode.get("activityName").asText());
        data.setDurationMinutes(activityNode.get("durationMinutes").asInt());

        String loggedAtText = activityNode.get("loggedAt").asText();
        try {
            data.setLoggedAt(LocalDateTime.parse(loggedAtText.replace("Z", "")));
        } catch (Exception e) {
            logger.warn("Failed to parse loggedAt '{}', using current time", loggedAtText);
            data.setLoggedAt(LocalDateTime.now());
        }

        if (activityNode.has("note") && !activityNode.get("note").isNull()) {
            data.setNote(activityNode.get("note").asText());
        } else {
            data.setNote("");
        }
        return data;
    }

    private JsonNode loadActivityVoiceSchema() {
        if (activityVoiceSchema != null) {
            return activityVoiceSchema;
        }
        try (InputStream in = new ClassPathResource("ai/activity-voice-schema.json").getInputStream()) {
            activityVoiceSchema = objectMapper.readTree(in);
            return activityVoiceSchema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load activity voice schema", e);
        }
    }

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

        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public LocalDateTime getLoggedAt() { return loggedAt; }
        public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        @Override
        public String toString() {
            return String.format("ParsedActivityData{activityName='%s', durationMinutes=%d, loggedAt=%s, note='%s'}",
                    activityName, durationMinutes, loggedAt, note);
        }
    }
}
