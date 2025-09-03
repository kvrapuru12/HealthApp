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
public class AiFoodVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiFoodVoiceParsingService.class);
    
    private static final String SYSTEM_PROMPT = """
        You are an AI assistant that parses natural language descriptions of food consumption into structured data.
        
        Parse the input text and return a JSON object with the following structure:
        {
            "foodName": "string - the name of the food item",
            "quantity": "number - quantity consumed",
            "unit": "string - unit of measurement (grams, pieces, glass, cup, tablespoon, teaspoon, etc.)",
            "mealType": "string - meal type (breakfast/lunch/dinner/snack)",
            "loggedAt": "ISO 8601 datetime string (YYYY-MM-DDTHH:mm:ss format)",
            "note": "string - optional additional context"
        }
        
        Rules:
        1. Extract the food name clearly (e.g., "boiled egg", "orange juice", "chicken curry")
        2. Convert time references to ISO 8601 format (YYYY-MM-DDTHH:mm:ss):
           - "this morning" → today's date at 08:00:00
           - "yesterday evening" → yesterday's date at 18:00:00
           - "last night" → yesterday's date at 21:00:00
           - "today" → today's date at current time (use actual current time)
           - "now" → current date and time
        3. If no specific time is mentioned, use current time
        4. Determine meal type based on context:
           - morning/breakfast time → "breakfast"
           - afternoon/lunch time → "lunch"
           - evening/dinner time → "dinner"
           - between meals → "snack"
        5. Keep notes concise and relevant
        6. Return ONLY valid JSON, no additional text
        7. IMPORTANT: loggedAt must be in ISO 8601 format (YYYY-MM-DDTHH:mm:ss)
        """;

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    public ParsedFoodData parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing food voice text: {}", voiceText);

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
            
            ParsedFoodData data = new ParsedFoodData();
            data.setFoodName(jsonNode.get("foodName").asText());
            data.setQuantity(jsonNode.get("quantity").asDouble());
            data.setUnit(jsonNode.get("unit").asText());
            data.setMealType(jsonNode.get("mealType").asText());
            
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

            logger.info("Successfully parsed food: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error parsing food voice text: {}", e.getMessage(), e);
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse food information from voice text: " + e.getMessage(), e);
        }
    }

    public static class ParsedFoodData {
        private String foodName;
        private Double quantity;
        private String unit;
        private String mealType;
        private LocalDateTime loggedAt;
        private String note;

        public ParsedFoodData() {}

        public String getFoodName() {
            return foodName;
        }

        public void setFoodName(String foodName) {
            this.foodName = foodName;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getMealType() {
            return mealType;
        }

        public void setMealType(String mealType) {
            this.mealType = mealType;
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
            return String.format("ParsedFoodData{foodName='%s', quantity=%.2f, unit='%s', mealType='%s', loggedAt=%s, note='%s'}",
                    foodName, quantity, unit, mealType, loggedAt, note);
        }
    }
}
