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
        
        Parse the input text and return a JSON object with this shape:
        {
            "compositeMeals": [],
            "foodItems": []
        }
        
        compositeMeals — use when the user describes ONE dish or drink made from many ingredients consumed together as a single thing:
        - Examples: a smoothie or shake listing ingredients; a blended bowl; one soup/stew with listed vegetables; one salad bowl where components are not separate orders.
        - Return exactly ONE entry per such dish. Pick a short, natural display name (e.g. "Berry chia breakfast smoothie").
        - approximateTotalGrams: your best estimate of total grams for everything in that dish (sum ingredient weights).
        - nutrition: estimated macros **per 100g** for that combined mixture (not per ingredient).
        - note: briefly list main ingredients from the user text (for transparency).
        - Do NOT also list those ingredients again in foodItems (no duplication).
        
        foodItems — use when foods are **distinct** items people usually log separately:
        - Examples: "chicken burger and chips" → TWO entries (burger, chips). "Pizza and soda" → two entries.
        - Each entry has foodName, quantity, unit, mealType, loggedAt, note, nutrition (per 100g for that food).
        - Use foodItems for separate beverages unless the user clearly blended everything into one drink.
        
        If unsure whether something is one blended dish vs separate items, prefer **foodItems** (safer).
        
        Shared rules:
        - Convert time references to ISO 8601 (YYYY-MM-DDTHH:mm:ssZ) using CURRENT DATE AND TIME:
          "this morning" → today 08:00:00; "yesterday evening" → yesterday 18:00:00; "last night" → yesterday 21:00:00;
          "today"/"now" → use CURRENT DATE AND TIME.
        - If no time given, use CURRENT DATE AND TIME.
        - Meal type: breakfast / lunch / dinner / snack from context.
        - Nutrition: reasonable database-style values; composite mixture is an estimate for the blend.
        - Every compositeMeals entry MUST include approximateTotalGrams (positive number).
        - Return ONLY valid JSON, no markdown or extra text.
        - loggedAt must follow ISO 8601 and align with CURRENT DATE AND TIME when inferring dates.
        
        Example A — distinct items:
        Input: "chicken burger and chips for lunch"
        Output: {
            "compositeMeals": [],
            "foodItems": [
                {
                    "foodName": "chicken burger",
                    "quantity": 1,
                    "unit": "serving",
                    "mealType": "lunch",
                    "loggedAt": "2024-01-15T12:30:00Z",
                    "note": "chicken burger",
                    "nutrition": {"caloriesPer100g": 250, "proteinPer100g": 15, "carbsPer100g": 22, "fatPer100g": 12, "fiberPer100g": 2}
                },
                {
                    "foodName": "chips",
                    "quantity": 1,
                    "unit": "serving",
                    "mealType": "lunch",
                    "loggedAt": "2024-01-15T12:30:00Z",
                    "note": "fried potato chips",
                    "nutrition": {"caloriesPer100g": 536, "proteinPer100g": 7, "carbsPer100g": 53, "fatPer100g": 35, "fiberPer100g": 4.8}
                }
            ]
        }
        
        Example B — multi-ingredient smoothie as ONE composite meal:
        Input: "breakfast smoothie: chia, almonds, banana, berries, oats"
        Output: {
            "compositeMeals": [
                {
                    "displayName": "Chia almond berry breakfast smoothie",
                    "approximateTotalGrams": 380,
                    "mealType": "breakfast",
                    "loggedAt": "2024-01-15T08:00:00Z",
                    "note": "chia seeds, almonds, banana, berries, oats",
                    "nutrition": {"caloriesPer100g": 165, "proteinPer100g": 6, "carbsPer100g": 20, "fatPer100g": 7, "fiberPer100g": 5}
                }
            ],
            "foodItems": []
        }
        
        Example C — breakfast with distinct foods:
        Input: "I ate 2 boiled eggs and 1 coffee for breakfast then I had chicken salad for lunch"
        Output: {
            "compositeMeals": [],
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

    /**
     * Models often wrap JSON in markdown fences or add a short preamble; extract a parseable JSON object.
     */
    static String extractJsonObject(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstLineBreak = s.indexOf('\n');
            if (firstLineBreak > 0) {
                s = s.substring(firstLineBreak + 1);
            } else {
                s = s.replaceFirst("^```(?:json)?", "").trim();
            }
            int closingFence = s.lastIndexOf("```");
            if (closingFence >= 0) {
                s = s.substring(0, closingFence).trim();
            }
        }
        s = s.trim();
        if (s.startsWith("{")) {
            return s;
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

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
                    .maxTokens(3500)
                    .temperature(0.1)
                    .build();

            var completion = openAiService.createChatCompletion(request);
            String response = completion.getChoices().get(0).getMessage().getContent();

            logger.info("Raw AI response: '{}'", response);

            // Parse the JSON response
            logger.info("Attempting to parse JSON from AI response");
            String jsonPayload = extractJsonObject(response);
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);
            
            ParsedFoodDataList dataList = new ParsedFoodDataList();

            JsonNode compositeNode = jsonNode.get("compositeMeals");
            if (compositeNode != null && compositeNode.isArray()) {
                for (JsonNode compositeMealNode : compositeNode) {
                    ParsedFoodData data = parseCompositeMealNode(compositeMealNode);
                    if (data != null) {
                        dataList.addCompositeMeal(data);
                    }
                }
            }

            JsonNode foodItemsNode = jsonNode.get("foodItems");
            if (foodItemsNode != null && foodItemsNode.isArray()) {
                for (JsonNode foodItemNode : foodItemsNode) {
                    ParsedFoodData data = parseStandardFoodItemNode(foodItemNode);
                    if (data != null) {
                        dataList.addFoodItem(data);
                    }
                }
            }

            logger.info("Successfully parsed {} composite meal(s), {} separate food item(s): {}",
                    dataList.getCompositeMeals().size(), dataList.getFoodItems().size(), dataList);
            return dataList;

        } catch (Exception e) {
            logger.error("Error parsing food voice text: {}", e.getMessage(), e);
            logger.error("Full exception details: ", e);
            throw new RuntimeException("Unable to parse food information from voice text: " + e.getMessage(), e);
        }
    }

    private LocalDateTime parseLoggedAtFromNode(JsonNode node) {
        if (!node.has("loggedAt") || node.get("loggedAt").isNull()) {
            return LocalDateTime.now();
        }
        String loggedAtText = node.get("loggedAt").asText();
        try {
            if (loggedAtText.endsWith("Z")) {
                loggedAtText = loggedAtText.substring(0, loggedAtText.length() - 1);
            }
            return LocalDateTime.parse(loggedAtText);
        } catch (Exception e) {
            logger.warn("Failed to parse loggedAt '{}', using current time", loggedAtText);
            return LocalDateTime.now();
        }
    }

    private static Double readNutritionDouble(JsonNode nutritionNode, String field) {
        if (nutritionNode == null || !nutritionNode.has(field)) {
            return null;
        }
        JsonNode n = nutritionNode.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asDouble();
        }
        if (n.isTextual()) {
            try {
                return Double.parseDouble(n.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private NutritionData parseNutritionFromNode(JsonNode foodItemNode) {
        if (!foodItemNode.has("nutrition") || foodItemNode.get("nutrition").isNull()) {
            return null;
        }
        JsonNode nutritionNode = foodItemNode.get("nutrition");
        if (!nutritionNode.isObject()) {
            return null;
        }
        Double calories = readNutritionDouble(nutritionNode, "caloriesPer100g");
        Double protein = readNutritionDouble(nutritionNode, "proteinPer100g");
        Double carbs = readNutritionDouble(nutritionNode, "carbsPer100g");
        Double fat = readNutritionDouble(nutritionNode, "fatPer100g");
        Double fiber = readNutritionDouble(nutritionNode, "fiberPer100g");
        if (calories == null || protein == null || carbs == null || fat == null || fiber == null) {
            logger.warn("Skipping AI nutrition: missing or non-numeric macro field(s)");
            return null;
        }
        NutritionData nutrition = new NutritionData();
        nutrition.setCaloriesPer100g(calories);
        nutrition.setProteinPer100g(protein);
        nutrition.setCarbsPer100g(carbs);
        nutrition.setFatPer100g(fat);
        nutrition.setFiberPer100g(fiber);
        return nutrition;
    }

    private ParsedFoodData parseStandardFoodItemNode(JsonNode foodItemNode) {
        if (!foodItemNode.hasNonNull("foodName")) {
            logger.warn("Skipping food item with missing foodName");
            return null;
        }
        ParsedFoodData data = new ParsedFoodData();
        data.setFoodName(foodItemNode.get("foodName").asText());
        data.setQuantity(foodItemNode.has("quantity") && !foodItemNode.get("quantity").isNull()
                ? foodItemNode.get("quantity").asDouble() : 1.0);
        data.setUnit(foodItemNode.has("unit") && !foodItemNode.get("unit").isNull()
                ? foodItemNode.get("unit").asText() : "serving");
        data.setMealType(foodItemNode.has("mealType") && !foodItemNode.get("mealType").isNull()
                ? foodItemNode.get("mealType").asText() : "snack");
        data.setLoggedAt(parseLoggedAtFromNode(foodItemNode));
        if (foodItemNode.has("note") && !foodItemNode.get("note").isNull()) {
            data.setNote(foodItemNode.get("note").asText());
        }
        data.setNutrition(parseNutritionFromNode(foodItemNode));
        return data;
    }

    private ParsedFoodData parseCompositeMealNode(JsonNode node) {
        if (!node.hasNonNull("displayName")) {
            logger.warn("Skipping composite meal with missing displayName");
            return null;
        }
        ParsedFoodData data = new ParsedFoodData();
        data.setFoodName(node.get("displayName").asText());
        double grams = 350.0;
        if (node.has("approximateTotalGrams") && !node.get("approximateTotalGrams").isNull()) {
            grams = node.get("approximateTotalGrams").asDouble();
        }
        if (grams < 1.0) {
            grams = 350.0;
        }
        data.setQuantity(grams);
        data.setUnit("grams");
        data.setMealType(node.has("mealType") && !node.get("mealType").isNull()
                ? node.get("mealType").asText() : "snack");
        data.setLoggedAt(parseLoggedAtFromNode(node));
        if (node.has("note") && !node.get("note").isNull()) {
            data.setNote(node.get("note").asText());
        }
        data.setNutrition(parseNutritionFromNode(node));
        return data;
    }

    public static class ParsedFoodDataList {
        private List<ParsedFoodData> compositeMeals;
        private List<ParsedFoodData> foodItems;

        public ParsedFoodDataList() {
            this.compositeMeals = new ArrayList<>();
            this.foodItems = new ArrayList<>();
        }

        public List<ParsedFoodData> getCompositeMeals() {
            return compositeMeals;
        }

        public void setCompositeMeals(List<ParsedFoodData> compositeMeals) {
            this.compositeMeals = compositeMeals;
        }

        public void addCompositeMeal(ParsedFoodData compositeMeal) {
            this.compositeMeals.add(compositeMeal);
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
            return String.format("ParsedFoodDataList{compositeMeals=%s, foodItems=%s}", compositeMeals, foodItems);
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
