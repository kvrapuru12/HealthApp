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

import java.time.LocalDate;
import java.util.List;

@Service
public class AiCycleVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiCycleVoiceParsingService.class);
    
    private static final String SYSTEM_PROMPT = """
        You are an AI assistant that parses natural language descriptions of menstrual cycle events into structured data.
        
        Parse the input text and return a JSON object with the following structure:
        {
            "periodStartDate": "ISO date string (YYYY-MM-DD format)",
            "cycleLength": "number - cycle length in days (default: 28)",
            "periodDuration": "number - period duration in days (default: 5)",
            "isCycleRegular": "boolean - whether cycle is regular (default: true)"
        }
        
        Rules:
        1. Extract the period start date from natural language:
           - "yesterday" → yesterday's date
           - "today" → today's date
           - "last week" → 7 days ago
           - "2 days ago" → 2 days ago
           - "on Monday" → most recent Monday
        2. Use default values for missing information:
           - cycleLength: 28 days
           - periodDuration: 5 days
           - isCycleRegular: true
        3. Return ONLY valid JSON, no additional text
        4. IMPORTANT: periodStartDate must be in ISO format (YYYY-MM-DD)
        """;

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    public ParsedCycleData parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing cycle voice text: {}", voiceText);

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
            
            ParsedCycleData data = new ParsedCycleData();
            
            // Parse periodStartDate with fallback to current date if parsing fails
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
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse cycle information from voice text: " + e.getMessage(), e);
        }
    }

    public static class ParsedCycleData {
        private LocalDate periodStartDate;
        private Integer cycleLength = 28;
        private Integer periodDuration = 5;
        private Boolean isCycleRegular = true;

        public ParsedCycleData() {}

        public LocalDate getPeriodStartDate() {
            return periodStartDate;
        }

        public void setPeriodStartDate(LocalDate periodStartDate) {
            this.periodStartDate = periodStartDate;
        }

        public Integer getCycleLength() {
            return cycleLength;
        }

        public void setCycleLength(Integer cycleLength) {
            this.cycleLength = cycleLength;
        }

        public Integer getPeriodDuration() {
            return periodDuration;
        }

        public void setPeriodDuration(Integer periodDuration) {
            this.periodDuration = periodDuration;
        }

        public Boolean getIsCycleRegular() {
            return isCycleRegular;
        }

        public void setIsCycleRegular(Boolean isCycleRegular) {
            this.isCycleRegular = isCycleRegular;
        }

        @Override
        public String toString() {
            return String.format("ParsedCycleData{periodStartDate=%s, cycleLength=%d, periodDuration=%d, isCycleRegular=%s}",
                    periodStartDate, cycleLength, periodDuration, isCycleRegular);
        }
    }
}
