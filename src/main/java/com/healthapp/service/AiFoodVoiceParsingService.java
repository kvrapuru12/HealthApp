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
import java.util.ArrayList;

@Service
public class AiFoodVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiFoodVoiceParsingService.class);
    
    private String getSystemPrompt() {
        String currentDateTime = LocalDateTime.now().toString();
        return String.format("""
        You are an AI assistant that parses natural language descriptions of food consumption into structured data.
        
        CURRENT DATE AND TIME: %s
        
        Parse the input text and return a JSON object with the following structure:
        {
            "foodItems": [
                {
                    "foodName": "string - the name of the food item",
                    "quantity": "number - quantity consumed",
                    "unit": "string - unit of measurement (grams, pieces, glass, cup, tablespoon, teaspoon, etc.)",
                    "mealType": "string - meal type (breakfast/lunch/dinner/snack)",
                    "loggedAt": "ISO 8601 datetime string (YYYY-MM-DDTHH:mm:ssZ format)",
                    "note": "string - optional additional context",
                    "nutrition": {
                        "caloriesPer100g": "number - calories per 100g",
                        "proteinPer100g": "number - protein in grams per 100g",
                        "carbsPer100g": "number - carbohydrates in grams per 100g",
                        "fatPer100g": "number - fat in grams per 100g",
                        "fiberPer100g": "number - fiber in grams per 100g"
                    }
                }
            ]
        }
        
        Rules:
        1. Extract ALL food items mentioned in the input text
        2. Each food item should be a separate entry in the foodItems array
        3. Extract the food name clearly (e.g., "boiled egg", "orange juice", "chicken curry")
        4. Convert time references to ISO 8601 format (YYYY-MM-DDTHH:mm:ss) using the CURRENT DATE AND TIME above:
           - "this morning" → today's date at 08:00:00
           - "yesterday evening" → yesterday's date at 18:00:00
           - "last night" → yesterday's date at 21:00:00
           - "today" → today's date at current time (use the CURRENT DATE AND TIME provided)
           - "now" → current date and time (use the CURRENT DATE AND TIME provided)
        5. If no specific time is mentioned, use the CURRENT DATE AND TIME provided above
        6. Determine meal type based on context and timing:
           - morning/breakfast time → "breakfast"
           - afternoon/lunch time → "lunch"
           - evening/dinner time → "dinner"
           - between meals → "snack"
        7. For multiple foods mentioned together, infer meal context:
           - If foods are mentioned with "for breakfast" → all are breakfast
           - If foods are mentioned with "for lunch" → all are lunch
           - If foods span multiple meals, assign appropriate meal type to each
        8. Provide accurate nutritional information per 100g for each food item
        9. Use ACCURATE nutritional values based on USDA/standard food databases - do not guess or estimate
        9a. For South Indian foods: idly ~130-150 cal/100g, dosa ~200-250 cal/100g, rice ~130 cal/100g
        9b. For common foods: bread ~250 cal/100g, chicken ~165 cal/100g, fish ~200 cal/100g
        10. ALWAYS provide a meaningful note - include cooking method, preparation style, or any relevant context from the input
        11. Return ONLY valid JSON, no additional text
        12. IMPORTANT: loggedAt must be in ISO 8601 format (YYYY-MM-DDTHH:mm:ssZ) and must use the CURRENT DATE AND TIME provided above
        
        Examples:
        Input: "I ate 2 boiled eggs and 1 coffee for breakfast then I had chicken salad for lunch"
        Output: {
            "foodItems": [
                {
                    "foodName": "boiled egg",
                    "quantity": 2,
                    "unit": "pieces",
                    "mealType": "breakfast",
                    "loggedAt": "2024-01-15T08:00:00Z",
                    "note": "boiled eggs for breakfast",
                    "nutrition": {"caloriesPer100g": 155, "proteinPer100g": 13, "carbsPer100g": 1.1, "fatPer100g": 11, "fiberPer100g": 0}
                },
                {
                    "foodName": "coffee",
                    "quantity": 1,
                    "unit": "cup",
                    "mealType": "breakfast",
                    "loggedAt": "2024-01-15T08:00:00Z",
                    "note": "coffee for breakfast",
                    "nutrition": {"caloriesPer100g": 2, "proteinPer100g": 0.3, "carbsPer100g": 0, "fatPer100g": 0, "fiberPer100g": 0}
                },
                {
                    "foodName": "chicken salad",
                    "quantity": 1,
                    "unit": "serving",
                    "mealType": "lunch",
                    "loggedAt": "2024-01-15T12:00:00Z",
                    "note": "chicken salad for lunch",
                    "nutrition": {"caloriesPer100g": 120, "proteinPer100g": 15, "carbsPer100g": 8, "fatPer100g": 3, "fiberPer100g": 2}
                }
            ]
        }
        """, currentDateTime);
    }

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    public ParsedFoodDataList parseVoiceText(String voiceText) {
        try {
            logger.info("Parsing food voice text: {}", voiceText);

            if (openAiService == null) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage("system", getSystemPrompt()),
                            new ChatMessage("user", voiceText)
                    ))
                    .maxTokens(1000) // Increased for multiple food items
                    .temperature(0.1)
                    .build();

            var completion = openAiService.createChatCompletion(request);
            String response = completion.getChoices().get(0).getMessage().getContent();

            logger.info("Raw AI response: '{}'", response);

            // Parse the JSON response
            logger.info("Attempting to parse JSON from AI response");
            JsonNode jsonNode = objectMapper.readTree(response);
            
            ParsedFoodDataList dataList = new ParsedFoodDataList();
            
            // Parse foodItems array
            JsonNode foodItemsNode = jsonNode.get("foodItems");
            if (foodItemsNode != null && foodItemsNode.isArray()) {
                for (JsonNode foodItemNode : foodItemsNode) {
                    ParsedFoodData data = new ParsedFoodData();
                    data.setFoodName(foodItemNode.get("foodName").asText());
                    data.setQuantity(foodItemNode.get("quantity").asDouble());
                    data.setUnit(foodItemNode.get("unit").asText());
                    data.setMealType(foodItemNode.get("mealType").asText());
                    
                    // Parse loggedAt with fallback to current time if parsing fails
                    String loggedAtText = foodItemNode.get("loggedAt").asText();
                    try {
                        // Handle both formats: YYYY-MM-DDTHH:mm:ssZ and YYYY-MM-DDTHH:mm:ss
                        if (loggedAtText.endsWith("Z")) {
                            loggedAtText = loggedAtText.substring(0, loggedAtText.length() - 1);
                        }
                        data.setLoggedAt(LocalDateTime.parse(loggedAtText));
                    } catch (Exception e) {
                        logger.warn("Failed to parse loggedAt '{}', using current time", loggedAtText);
                        data.setLoggedAt(LocalDateTime.now());
                    }
                    
                    if (foodItemNode.has("note") && !foodItemNode.get("note").isNull()) {
                        data.setNote(foodItemNode.get("note").asText());
                    }
                    
                    // Parse nutrition data
                    if (foodItemNode.has("nutrition") && !foodItemNode.get("nutrition").isNull()) {
                        JsonNode nutritionNode = foodItemNode.get("nutrition");
                        NutritionData nutrition = new NutritionData();
                        nutrition.setCaloriesPer100g(nutritionNode.get("caloriesPer100g").asDouble());
                        nutrition.setProteinPer100g(nutritionNode.get("proteinPer100g").asDouble());
                        nutrition.setCarbsPer100g(nutritionNode.get("carbsPer100g").asDouble());
                        nutrition.setFatPer100g(nutritionNode.get("fatPer100g").asDouble());
                        nutrition.setFiberPer100g(nutritionNode.get("fiberPer100g").asDouble());
                        data.setNutrition(nutrition);
                    }
                    
                    dataList.addFoodItem(data);
                }
            }

            logger.info("Successfully parsed {} food items: {}", dataList.getFoodItems().size(), dataList);
            return dataList;

        } catch (Exception e) {
            logger.error("Error parsing food voice text: {}", e.getMessage(), e);
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse food information from voice text: " + e.getMessage(), e);
        }
    }

    public static class ParsedFoodDataList {
        private List<ParsedFoodData> foodItems;

        public ParsedFoodDataList() {
            this.foodItems = new ArrayList<>();
        }

        public List<ParsedFoodData> getFoodItems() {
            return foodItems;
        }

        public void setFoodItems(List<ParsedFoodData> foodItems) {
            this.foodItems = foodItems;
        }

        public void addFoodItem(ParsedFoodData foodItem) {
            this.foodItems.add(foodItem);
        }

        @Override
        public String toString() {
            return String.format("ParsedFoodDataList{foodItems=%s}", foodItems);
        }
    }

    public static class ParsedFoodData {
        private String foodName;
        private Double quantity;
        private String unit;
        private String mealType;
        private LocalDateTime loggedAt;
        private String note;
        private NutritionData nutrition;

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

        public NutritionData getNutrition() {
            return nutrition;
        }

        public void setNutrition(NutritionData nutrition) {
            this.nutrition = nutrition;
        }

        @Override
        public String toString() {
            return String.format("ParsedFoodData{foodName='%s', quantity=%.2f, unit='%s', mealType='%s', loggedAt=%s, note='%s', nutrition=%s}",
                    foodName, quantity, unit, mealType, loggedAt, note, nutrition);
        }
    }
    
    public static class NutritionData {
        private double caloriesPer100g;
        private double proteinPer100g;
        private double carbsPer100g;
        private double fatPer100g;
        private double fiberPer100g;

        public NutritionData() {}

        public double getCaloriesPer100g() {
            return caloriesPer100g;
        }

        public void setCaloriesPer100g(double caloriesPer100g) {
            this.caloriesPer100g = caloriesPer100g;
        }

        public double getProteinPer100g() {
            return proteinPer100g;
        }

        public void setProteinPer100g(double proteinPer100g) {
            this.proteinPer100g = proteinPer100g;
        }

        public double getCarbsPer100g() {
            return carbsPer100g;
        }

        public void setCarbsPer100g(double carbsPer100g) {
            this.carbsPer100g = carbsPer100g;
        }

        public double getFatPer100g() {
            return fatPer100g;
        }

        public void setFatPer100g(double fatPer100g) {
            this.fatPer100g = fatPer100g;
        }

        public double getFiberPer100g() {
            return fiberPer100g;
        }

        public void setFiberPer100g(double fiberPer100g) {
            this.fiberPer100g = fiberPer100g;
        }

        @Override
        public String toString() {
            return String.format("NutritionData{calories=%.1f, protein=%.1f, carbs=%.1f, fat=%.1f, fiber=%.1f}",
                    caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, fiberPer100g);
        }
    }
}
