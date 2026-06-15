package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.OpenAiModelProperties;
import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiFoodVoiceParsingService {

    private static final Logger logger = LoggerFactory.getLogger(AiFoodVoiceParsingService.class);

    @Autowired(required = false)
    private OpenAiChatClient openAiChatClient;

    @Autowired
    private OpenAiModelProperties modelProperties;

    @Autowired
    private MealComplexityClassifier mealComplexityClassifier;

    @Autowired
    private PortionGramEstimator portionGramEstimator;

    @Autowired
    private VoiceMealComposer voiceMealComposer;

    @Autowired
    private ExplicitQuantityApplier explicitQuantityApplier;

    @Autowired
    private ExplicitMacroApplier explicitMacroApplier;

    @Autowired
    private RecommendedPortionApplicator recommendedPortionApplicator;

    @Autowired
    private PortionSanityCorrector portionSanityCorrector;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int SIMPLE_PARSE_MAX_TOKENS = 3500;
    private static final int COMPLEX_PARSE_MAX_TOKENS = 5000;

    private JsonNode foodVoiceSchema;

    private String getSystemPrompt() {
        String currentDateTime = LocalDateTime.now().toString();
        String assumptionRules = AiPromptGuidelines.SHARED_INFERENCE_PRINCIPLES
                + "\n        "
                + AiPromptGuidelines.FOOD_PORTION_ASSUMPTION_RULES
                + "\n        "
                + RecommendedPortionCatalog.promptReferenceLines();
        return String.format("""
        You are an AI assistant that parses natural language descriptions of food consumption into structured data.
        
        CURRENT DATE AND TIME: %s
        
        Parse the input text and return JSON with compositeMeals and foodItems arrays.
        
        CRITICAL PORTION RULES:
        - estimatedGrams is REQUIRED for every foodItems entry and represents total edible weight consumed in grams.
        - NEVER set estimatedGrams to the count (1 banana ≠ 1 gram). Always convert counts to realistic grams.
        - When the user does NOT mention quantity or grams, assume ONE typical single portion (quantity=1 with the best unit).
        - Example: "had a banana" → quantity=1, unit=medium, estimatedGrams=120.
        - Example: "1 medium banana" → quantity=1, unit=medium, estimatedGrams=120.
        - Example: "2 boiled eggs" → quantity=2, unit=pieces, estimatedGrams=100 (2 × ~50g).
        - Example: "150g chicken breast" → quantity=150, unit=grams, estimatedGrams=150.
        - Example: "1 cup cooked quinoa" → quantity=1, unit=cup, estimatedGrams=185.
        - Example: "black coffee" → quantity=1, unit=cup, estimatedGrams=250.
        - For foodItems (single simple foods), include nutrition per 100g (caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, fiberPer100g) as a USDA-quality estimate.
        - For compositeMeals, include nutrition per 100g AND a complete ingredients[] array for USDA blending.
        
        VOICE-FIRST MEAL RULES (users speak in flowing sentences, not comma lists):
        - One utterance describing one eating occasion → prefer ONE compositeMeal with ingredients[] for the plate.
        - Example: "salmon with quinoa and broccoli for dinner" → compositeMeal with 3+ ingredients, not separate foodItems.
        - Example: "two slices toast with butter and peanut butter" → compositeMeal with ingredients: toast, butter, peanut butter.
        - Beverages (lassi, juice, coffee, smoothie) → separate foodItems with cup/glass unit when mentioned alongside a meal.
        - fdcSearchTerm: use USDA-friendly English (e.g. "banana, raw", "salmon, cooked", "quinoa, cooked").
        
        compositeMeals — one plate or blended dish from ingredients:
        - REQUIRED ingredients array: each with name, estimatedGrams, fdcSearchTerm.
        - approximateTotalGrams = sum of ingredient grams.
        
        foodItems — single distinct foods OR beverages not part of the main plate.
        
        %s
        
        %s
        
        Shared rules:
        - Replace <CURRENT> in examples with ISO 8601 (YYYY-MM-DDTHH:mm:ss) from CURRENT DATE AND TIME.
        - If no time given, use CURRENT DATE AND TIME.
        - Meal type: breakfast / lunch / dinner / snack from context.
        - Notes: use Stated:/Assumed: labels per existing guidelines.
        """, currentDateTime, assumptionRules, AiPromptGuidelines.FOOD_VOICE_FEW_SHOT_EXAMPLES);
    }

    public ParsedFoodDataList parseVoiceText(String voiceText) {
        try {
            String normalizedVoice = FoodVoiceTypoNormalizer.normalize(voiceText);
            logger.debug("Parsing food voice text: {}", normalizedVoice);

            if (openAiChatClient == null || !openAiChatClient.isAvailable()) {
                throw new RuntimeException("OpenAI service is not available. Please configure OpenAI API key.");
            }

            MealComplexity complexity = mealComplexityClassifier.classify(normalizedVoice);
            String model = resolveModel(normalizedVoice, complexity);
            int maxTokens = complexity == MealComplexity.COMPLEX ? COMPLEX_PARSE_MAX_TOKENS : SIMPLE_PARSE_MAX_TOKENS;
            String response = openAiChatClient.createStructuredCompletion(
                    model,
                    getSystemPrompt(),
                    normalizedVoice,
                    loadFoodVoiceSchema(),
                    "food_voice_parse",
                    maxTokens
            );

            logger.debug("Raw AI response: '{}'", response);
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

            normalizeAndMergeParsedResults(dataList, normalizedVoice);

            logger.info("Successfully parsed {} composite meal(s), {} separate food item(s)",
                    dataList.getCompositeMeals().size(), dataList.getFoodItems().size());
            return dataList;

        } catch (Exception e) {
            logger.error("Error parsing food voice text: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to parse food information from voice text: " + e.getMessage(), e);
        }
    }

    private String resolveModel(String voiceText, MealComplexity complexity) {
        if (modelProperties.isComplexRoutingEnabled() && complexity == MealComplexity.COMPLEX) {
            return modelProperties.getFoodComplexModel();
        }
        return modelProperties.getFoodSimpleModel();
    }

    private JsonNode loadFoodVoiceSchema() {
        if (foodVoiceSchema != null) {
            return foodVoiceSchema;
        }
        try (InputStream in = new ClassPathResource("ai/food-voice-schema.json").getInputStream()) {
            foodVoiceSchema = objectMapper.readTree(in);
            return foodVoiceSchema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load food voice schema", e);
        }
    }

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

    private List<IngredientData> parseIngredients(JsonNode node) {
        List<IngredientData> ingredients = new ArrayList<>();
        if (!node.has("ingredients") || !node.get("ingredients").isArray()) {
            return ingredients;
        }
        for (JsonNode ing : node.get("ingredients")) {
            if (!ing.hasNonNull("name")) {
                continue;
            }
            IngredientData data = new IngredientData();
            data.setName(ing.get("name").asText());
            data.setEstimatedGrams(ing.has("estimatedGrams") ? ing.get("estimatedGrams").asDouble() : 0);
            data.setFdcSearchTerm(ing.has("fdcSearchTerm") ? ing.get("fdcSearchTerm").asText() : data.getName());
            ingredients.add(data);
        }
        return ingredients;
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
        data.setEstimatedGrams(readEstimatedGrams(foodItemNode));
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
        data.setEstimatedGrams(grams);
        data.setMealType(node.has("mealType") && !node.get("mealType").isNull()
                ? node.get("mealType").asText() : "snack");
        data.setLoggedAt(parseLoggedAtFromNode(node));
        if (node.has("note") && !node.get("note").isNull()) {
            data.setNote(node.get("note").asText());
        }
        data.setNutrition(parseNutritionFromNode(node));
        data.setIngredients(parseIngredients(node));
        return data;
    }

    private void normalizeAndMergeParsedResults(ParsedFoodDataList dataList, String voiceText) {
        explicitQuantityApplier.apply(dataList, voiceText);
        boolean explicitMulti = ExplicitPortionParser.hasExplicitMultiItemBreakdown(voiceText);
        for (ParsedFoodData composite : dataList.getCompositeMeals()) {
            normalizePortions(composite);
        }
        for (ParsedFoodData item : dataList.getFoodItems()) {
            normalizePortions(item);
        }
        portionSanityCorrector.apply(dataList);
        if (!explicitMulti) {
            voiceMealComposer.applyVoiceMealRules(dataList, voiceText);
        } else {
            logger.info("Skipping composite merge rules — user stated explicit per-item quantities");
        }
        explicitMacroApplier.apply(dataList, voiceText);
    }

    private void normalizePortions(ParsedFoodData data) {
        if (data.isUserSpecifiedGrams() && data.getEstimatedGrams() != null && data.getEstimatedGrams() > 0) {
            data.setQuantity(data.getEstimatedGrams());
            data.setUnit("grams");
            normalizeIngredientPortions(data);
            return;
        }
        recommendedPortionApplicator.applyDefaults(data);
        double grams = portionGramEstimator.resolveEffectiveGrams(
                data.getFoodName(), data.getQuantity(), data.getUnit(), data.getEstimatedGrams());
        data.setEstimatedGrams(grams);
        if (PortionGramEstimator.isMassUnit(data.getUnit() != null ? data.getUnit().toLowerCase() : "")) {
            data.setQuantity(grams);
            data.setUnit("grams");
        }
        normalizeIngredientPortions(data);
    }

    private void normalizeIngredientPortions(ParsedFoodData data) {
        if (data.getIngredients() == null || data.getIngredients().isEmpty()) {
            return;
        }
        double total = 0;
        String mealContext = data.getFoodName();
        for (IngredientData ingredient : data.getIngredients()) {
            if (ingredient.getEstimatedGrams() < 5) {
                ingredient.setEstimatedGrams(
                        RecommendedPortionCatalog.ingredientPortionGrams(ingredient.getName(), mealContext));
            }
            total += ingredient.getEstimatedGrams();
        }
        if (total > 0 && !data.isUserSpecifiedGrams()) {
            data.setEstimatedGrams(total);
            data.setQuantity(total);
            data.setUnit("grams");
        }
    }

    private Double readEstimatedGrams(JsonNode node) {
        if (node.has("estimatedGrams") && !node.get("estimatedGrams").isNull()) {
            double grams = node.get("estimatedGrams").asDouble();
            if (grams > 0) {
                return grams;
            }
        }
        if (node.has("approximateTotalGrams") && !node.get("approximateTotalGrams").isNull()) {
            return node.get("approximateTotalGrams").asDouble();
        }
        return null;
    }

    public static class ParsedFoodDataList {
        private List<ParsedFoodData> compositeMeals = new ArrayList<>();
        private List<ParsedFoodData> foodItems = new ArrayList<>();

        public List<ParsedFoodData> getCompositeMeals() {
            return compositeMeals;
        }

        public void addCompositeMeal(ParsedFoodData compositeMeal) {
            this.compositeMeals.add(compositeMeal);
        }

        public List<ParsedFoodData> getFoodItems() {
            return foodItems;
        }

        public void addFoodItem(ParsedFoodData foodItem) {
            this.foodItems.add(foodItem);
        }
    }

    public static class ParsedFoodData {
        private String foodName;
        private Double quantity;
        private String unit;
        private Double estimatedGrams;
        private String mealType;
        private LocalDateTime loggedAt;
        private String note;
        private NutritionData nutrition;
        private List<IngredientData> ingredients = new ArrayList<>();
        private com.healthapp.service.nutrition.NutritionSource nutritionSource;
        private com.healthapp.service.nutrition.NutritionConfidence nutritionConfidence;
        private Integer fdcId;
        private boolean userSpecifiedGrams;
        private boolean userSpecifiedMacros;

        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Double getEstimatedGrams() { return estimatedGrams; }
        public void setEstimatedGrams(Double estimatedGrams) { this.estimatedGrams = estimatedGrams; }
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        public LocalDateTime getLoggedAt() { return loggedAt; }
        public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public NutritionData getNutrition() { return nutrition; }
        public void setNutrition(NutritionData nutrition) { this.nutrition = nutrition; }
        public List<IngredientData> getIngredients() { return ingredients; }
        public void setIngredients(List<IngredientData> ingredients) { this.ingredients = ingredients; }
        public com.healthapp.service.nutrition.NutritionSource getNutritionSource() { return nutritionSource; }
        public void setNutritionSource(com.healthapp.service.nutrition.NutritionSource nutritionSource) { this.nutritionSource = nutritionSource; }
        public com.healthapp.service.nutrition.NutritionConfidence getNutritionConfidence() { return nutritionConfidence; }
        public void setNutritionConfidence(com.healthapp.service.nutrition.NutritionConfidence nutritionConfidence) { this.nutritionConfidence = nutritionConfidence; }
        public Integer getFdcId() { return fdcId; }
        public void setFdcId(Integer fdcId) { this.fdcId = fdcId; }
        public boolean isUserSpecifiedGrams() { return userSpecifiedGrams; }
        public void setUserSpecifiedGrams(boolean userSpecifiedGrams) { this.userSpecifiedGrams = userSpecifiedGrams; }
        public boolean isUserSpecifiedMacros() { return userSpecifiedMacros; }
        public void setUserSpecifiedMacros(boolean userSpecifiedMacros) { this.userSpecifiedMacros = userSpecifiedMacros; }
    }

    public static class IngredientData {
        private String name;
        private double estimatedGrams;
        private String fdcSearchTerm;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getEstimatedGrams() { return estimatedGrams; }
        public void setEstimatedGrams(double estimatedGrams) { this.estimatedGrams = estimatedGrams; }
        public String getFdcSearchTerm() { return fdcSearchTerm; }
        public void setFdcSearchTerm(String fdcSearchTerm) { this.fdcSearchTerm = fdcSearchTerm; }
    }

    public static class NutritionData {
        private double caloriesPer100g;
        private double proteinPer100g;
        private double carbsPer100g;
        private double fatPer100g;
        private double fiberPer100g;

        public double getCaloriesPer100g() { return caloriesPer100g; }
        public void setCaloriesPer100g(double caloriesPer100g) { this.caloriesPer100g = caloriesPer100g; }
        public double getProteinPer100g() { return proteinPer100g; }
        public void setProteinPer100g(double proteinPer100g) { this.proteinPer100g = proteinPer100g; }
        public double getCarbsPer100g() { return carbsPer100g; }
        public void setCarbsPer100g(double carbsPer100g) { this.carbsPer100g = carbsPer100g; }
        public double getFatPer100g() { return fatPer100g; }
        public void setFatPer100g(double fatPer100g) { this.fatPer100g = fatPer100g; }
        public double getFiberPer100g() { return fiberPer100g; }
        public void setFiberPer100g(double fiberPer100g) { this.fiberPer100g = fiberPer100g; }
    }
}
