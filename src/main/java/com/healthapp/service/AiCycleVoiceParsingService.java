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
import java.time.LocalDate;

@Service
public class AiCycleVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiCycleVoiceParsingService.class);

    private static final String SYSTEM_PROMPT = """
        You are an AI assistant that parses natural language descriptions of menstrual cycle events into structured data.
        
        Parse the input text and return a JSON object with:
        periodStartDate (YYYY-MM-DD), cycleLength, periodDuration, isCycleRegular.
        
        Rules:
        1. Extract period start date from natural language (yesterday, today, last week, etc.)
        2. Defaults: cycleLength=28, periodDuration=5, isCycleRegular=true
        3. Return ONLY valid JSON matching the schema
        
        """
        + AiPromptGuidelines.SHARED_INFERENCE_PRINCIPLES
        + "\n"
        + AiPromptGuidelines.CYCLE_DEFAULT_ASSUMPTION_RULES;

    @Autowired(required = false)
    private OpenAiChatClient openAiChatClient;

    @Autowired
    private OpenAiModelProperties modelProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode cycleVoiceSchema;

    public ParsedCycleData parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing cycle voice text: {}", voiceText);

            if (openAiChatClient == null || !openAiChatClient.isAvailable()) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            String response = openAiChatClient.createStructuredCompletion(
                    modelProperties.getVoiceCycleModel(),
                    SYSTEM_PROMPT,
                    voiceText,
                    loadCycleVoiceSchema(),
                    "cycle_voice_parse",
                    500
            );

            logger.info("Raw AI response: '{}'", response);
            JsonNode jsonNode = objectMapper.readTree(AiFoodVoiceParsingService.extractJsonObject(response));

            ParsedCycleData data = new ParsedCycleData();

            String periodStartDateText = jsonNode.get("periodStartDate").asText();
            try {
                data.setPeriodStartDate(LocalDate.parse(periodStartDateText));
            } catch (Exception e) {
                logger.warn("Failed to parse periodStartDate '{}', using current date", periodStartDateText);
                data.setPeriodStartDate(LocalDate.now());
            }

            data.setCycleLength(jsonNode.get("cycleLength").asInt());
            data.setPeriodDuration(jsonNode.get("periodDuration").asInt());
            data.setIsCycleRegular(jsonNode.get("isCycleRegular").asBoolean());

            logger.info("Successfully parsed cycle: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error parsing cycle voice text: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to parse cycle information from voice text: " + e.getMessage(), e);
        }
    }

    private JsonNode loadCycleVoiceSchema() {
        if (cycleVoiceSchema != null) {
            return cycleVoiceSchema;
        }
        try (InputStream in = new ClassPathResource("ai/cycle-voice-schema.json").getInputStream()) {
            cycleVoiceSchema = objectMapper.readTree(in);
            return cycleVoiceSchema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cycle voice schema", e);
        }
    }

    public static class ParsedCycleData {
        private LocalDate periodStartDate;
        private Integer cycleLength = 28;
        private Integer periodDuration = 5;
        private Boolean isCycleRegular = true;

        public ParsedCycleData() {}

        public LocalDate getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
        public Integer getCycleLength() { return cycleLength; }
        public void setCycleLength(Integer cycleLength) { this.cycleLength = cycleLength; }
        public Integer getPeriodDuration() { return periodDuration; }
        public void setPeriodDuration(Integer periodDuration) { this.periodDuration = periodDuration; }
        public Boolean getIsCycleRegular() { return isCycleRegular; }
        public void setIsCycleRegular(Boolean isCycleRegular) { this.isCycleRegular = isCycleRegular; }

        @Override
        public String toString() {
            return String.format("ParsedCycleData{periodStartDate=%s, cycleLength=%d, periodDuration=%d, isCycleRegular=%s}",
                    periodStartDate, cycleLength, periodDuration, isCycleRegular);
        }
    }
}
