package com.healthapp.service;

import com.healthapp.dto.*;
import com.healthapp.entity.FoodItem;

import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class VoiceFoodLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceFoodLogService.class);
    
    @Autowired
    private FoodItemService foodItemService;
    
    @Autowired
    private FoodLogService foodLogService;
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private AiFoodVoiceParsingService aiFoodVoiceParsingService;
    
    public VoiceFoodLogResponse processVoiceFoodLog(VoiceFoodLogRequest request, Long authenticatedUserId) {
        try {
            // Validate user access
            if (!request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create food logs for themselves");
            }
            
            // Validate user exists
            userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if AI service is available
            if (aiFoodVoiceParsingService == null) {
                throw new RuntimeException("AI voice parsing service is not available. Please configure OpenAI API key.");
            }
            
            // Parse voice text using AI
            AiFoodVoiceParsingService.ParsedFoodDataList parsedDataList = aiFoodVoiceParsingService.parseVoiceText(request.getVoiceText());
            
            // Process the parsed data
            List<VoiceFoodLogResponse.LoggedFoodItem> loggedItems = new ArrayList<>();
            
            for (AiFoodVoiceParsingService.ParsedFoodData parsedData : parsedDataList.getFoodItems()) {
                try {
                    // Find or create food item
                    FoodItem foodItem = findOrCreateFoodItem(parsedData, authenticatedUserId);
                    
                    // Create food log
                    FoodLogCreateRequest logRequest = new FoodLogCreateRequest();
                    logRequest.setUserId(authenticatedUserId);
                    logRequest.setFoodItemId(foodItem.getId());
                    logRequest.setLoggedAt(parsedData.getLoggedAt());
                    logRequest.setMealType(parsedData.getMealType().toUpperCase());
                    logRequest.setQuantity(parsedData.getQuantity());
                    logRequest.setUnit(parsedData.getUnit());
                    logRequest.setNote(parsedData.getNote() != null && !parsedData.getNote().trim().isEmpty() ? parsedData.getNote() : "Created from voice input: " + request.getVoiceText());
                    
                    FoodLogCreateResponse logResponse = foodLogService.createFoodLog(logRequest, authenticatedUserId);
                    
                    // Add to response
                    loggedItems.add(new VoiceFoodLogResponse.LoggedFoodItem(
                            foodItem.getName(),
                            parsedData.getQuantity(),
                            parsedData.getMealType().toUpperCase(),
                            logResponse.getCalories(),
                            logResponse.getProtein(),
                            logResponse.getCarbs(),
                            logResponse.getFat(),
                            logResponse.getFiber(),
                            parsedData.getLoggedAt().toString()
                    ));
                    
                } catch (Exception e) {
                    logger.warn("Failed to process food item: {}", parsedData.getFoodName(), e);
                    // Continue processing other items instead of failing completely
                }
            }
            
            if (loggedItems.isEmpty()) {
                return new VoiceFoodLogResponse("Failed to create any food logs. Please try again.", 
                                             new ArrayList<>());
            }
            
            String message = loggedItems.size() == 1 ? 
                "Food log created from voice input" : 
                String.format("Created %d food logs from voice input", loggedItems.size());
            
            return new VoiceFoodLogResponse(message, loggedItems);
            
        } catch (Exception e) {
            logger.error("Error processing voice food log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process voice input: " + e.getMessage());
        }
    }
    
    private FoodItem findOrCreateFoodItem(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        // First, try to find existing food item with exact match
        String normalizedName = normalizeFoodName(parsedData.getFoodName());
        
        Optional<FoodItem> existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(
                normalizedName, FoodItem.FoodStatus.ACTIVE, userId);
        
        if (existingItem.isPresent()) {
            logger.info("Found exact match for user's food item: {}", normalizedName);
            return existingItem.get();
        }
        
        // Check for public food items with exact match
        existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndVisibility(
                normalizedName, FoodItem.FoodStatus.ACTIVE, FoodItem.FoodVisibility.PUBLIC);
        
        if (existingItem.isPresent()) {
            logger.info("Found exact match for public food item: {}", normalizedName);
            return existingItem.get();
        }
        
        // Try fuzzy matching for public food items
        existingItem = findSimilarFoodItem(normalizedName, userId);
        if (existingItem.isPresent()) {
            logger.info("Found similar food item: {} -> {}", normalizedName, existingItem.get().getName());
            return existingItem.get();
        }
        
        // Create new food item with estimated macros
        try {
            FoodItemCreateRequest createRequest = estimateFoodMacros(parsedData, userId);
            FoodItemCreateResponse response = foodItemService.createFoodItem(createRequest, userId);
            
            FoodItem createdItem = foodItemRepository.findById(response.getId())
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve created food item"));
            
            logger.info("Successfully created new public food item: {} (ID: {})", 
                       createdItem.getName(), createdItem.getId());
            return createdItem;
            
        } catch (Exception e) {
            logger.error("Failed to create food item for '{}': {}", parsedData.getFoodName(), e.getMessage(), e);
            throw new RuntimeException("Failed to create food item '" + parsedData.getFoodName() + "': " + e.getMessage(), e);
        }
    }
    
    private Optional<FoodItem> findSimilarFoodItem(String normalizedName, Long userId) {
        // Get all available public food items
        List<FoodItem> availableItems = foodItemRepository.findAvailableFoodItems(userId);
        
        // Find the best match using simple string similarity
        FoodItem bestMatch = null;
        double bestScore = 0.0;
        
        for (FoodItem item : availableItems) {
            if (item.getVisibility() == FoodItem.FoodVisibility.PUBLIC) {
                double similarity = calculateSimilarity(normalizedName, normalizeFoodName(item.getName()));
                if (similarity > bestScore && similarity > 0.8) { // 80% similarity threshold
                    bestScore = similarity;
                    bestMatch = item;
                }
            }
        }
        
        return Optional.ofNullable(bestMatch);
    }
    
    private double calculateSimilarity(String str1, String str2) {
        if (str1.equals(str2)) return 1.0;
        
        // Simple Levenshtein distance-based similarity
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }
    
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    private String normalizeFoodName(String foodName) {
        return foodName.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /**
     * Calculate weight_per_unit based on unit type and food name.
     * This represents the weight (in grams) of one unit of the food item.
     */
    private double calculateWeightPerUnit(String unit, String foodName) {
        String normalizedUnit = unit.toLowerCase().trim();
        String normalizedFood = foodName.toLowerCase().trim();
        
        // Handle different unit types
        switch (normalizedUnit) {
            case "pieces":
            case "piece":
                return getWeightForPiece(normalizedFood);
            case "cup":
            case "cups":
                return getWeightForCup(normalizedFood);
            case "glass":
            case "glasses":
                return getWeightForGlass(normalizedFood);
            case "tablespoon":
            case "tablespoons":
            case "tbsp":
                return getWeightForTablespoon(normalizedFood);
            case "teaspoon":
            case "teaspoons":
            case "tsp":
                return getWeightForTeaspoon(normalizedFood);
            case "gram":
            case "grams":
            case "g":
                return 1.0; // 1 gram = 1 gram
            case "kilogram":
            case "kilograms":
            case "kg":
                return 1000.0; // 1 kg = 1000 grams
            case "ounce":
            case "ounces":
            case "oz":
                return 28.35; // 1 oz = 28.35 grams
            case "pound":
            case "pounds":
            case "lb":
                return 453.59; // 1 lb = 453.59 grams
            case "serving":
            case "servings":
                return getWeightForServing(normalizedFood);
            default:
                // Default fallback - assume it's a piece-based unit
                return getWeightForPiece(normalizedFood);
        }
    }
    
    private double getWeightForPiece(String foodName) {
        // Weight per piece for common foods
        if (foodName.contains("egg")) return 50.0;
        if (foodName.contains("apple")) return 150.0;
        if (foodName.contains("banana")) return 120.0;
        if (foodName.contains("orange")) return 130.0;
        if (foodName.contains("cashew") || foodName.contains("nut")) return 1.0; // per nut
        if (foodName.contains("almond")) return 1.0; // per almond
        if (foodName.contains("bread")) return 25.0; // per slice
        if (foodName.contains("hash brown")) return 300.0; // per hash brown
        return 100.0; // Default
    }
    
    private double getWeightForCup(String foodName) {
        // Weight per cup for common foods
        if (foodName.contains("coffee") || foodName.contains("tea")) return 250.0;
        if (foodName.contains("milk")) return 240.0;
        if (foodName.contains("rice")) return 200.0;
        if (foodName.contains("pasta")) return 140.0;
        if (foodName.contains("oats") || foodName.contains("oatmeal")) return 100.0;
        return 200.0; // Default
    }
    
    private double getWeightForGlass(String foodName) {
        // Weight per glass (250ml)
        if (foodName.contains("water")) return 250.0;
        if (foodName.contains("juice")) return 250.0;
        return 250.0; // Default
    }
    
    private double getWeightForTablespoon(String foodName) {
        // Weight per tablespoon (15ml)
        if (foodName.contains("oil") || foodName.contains("butter")) return 14.0;
        if (foodName.contains("peanut butter")) return 16.0;
        if (foodName.contains("honey") || foodName.contains("syrup")) return 21.0;
        return 15.0; // Default
    }
    
    private double getWeightForTeaspoon(String foodName) {
        // Weight per teaspoon (5ml)
        if (foodName.contains("sugar")) return 4.0;
        if (foodName.contains("salt")) return 6.0;
        if (foodName.contains("oil") || foodName.contains("butter")) return 4.5;
        return 5.0; // Default
    }
    
    private double getWeightForServing(String foodName) {
        // Weight per serving
        if (foodName.contains("salad")) return 200.0;
        if (foodName.contains("soup")) return 250.0;
        if (foodName.contains("curry") || foodName.contains("stew")) return 250.0;
        return 200.0; // Default
    }
    
    private FoodItemCreateRequest estimateFoodMacros(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        try {
            // Use AI-provided nutrition data if available, otherwise fallback to hardcoded values
            AiFoodVoiceParsingService.NutritionData aiNutrition = parsedData.getNutrition();
            
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedData.getFoodName());
            createRequest.setCategory(parsedData.getMealType().toUpperCase()); // Use meal type as category
            createRequest.setDefaultUnit(parsedData.getUnit());
            createRequest.setWeightPerUnit(calculateWeightPerUnit(parsedData.getUnit(), parsedData.getFoodName()));
            
            if (aiNutrition != null) {
                // Validate and use AI-provided nutrition data
                AiFoodVoiceParsingService.NutritionData validatedNutrition = validateNutritionData(aiNutrition);
                if (validatedNutrition != null) {
                    createRequest.setCaloriesPerUnit((int) Math.round(validatedNutrition.getCaloriesPer100g()));
                    createRequest.setProteinPerUnit(validatedNutrition.getProteinPer100g());
                    createRequest.setCarbsPerUnit(validatedNutrition.getCarbsPer100g());
                    createRequest.setFatPerUnit(validatedNutrition.getFatPer100g());
                    createRequest.setFiberPerUnit(validatedNutrition.getFiberPer100g());
                    
                    logger.info("Created food item '{}' with AI nutrition: {} cal, {}g protein per 100g", 
                               parsedData.getFoodName(), validatedNutrition.getCaloriesPer100g(), validatedNutrition.getProteinPer100g());
                } else {
                    // AI nutrition was rejected, use fallback
                    logger.info("AI nutrition rejected for '{}', using fallback data", parsedData.getFoodName());
                    FoodNutritionData nutritionData = getFoodNutritionData(parsedData.getFoodName());
                    createRequest.setCaloriesPerUnit((int) Math.round(nutritionData.caloriesPer100g));
                    createRequest.setProteinPerUnit(nutritionData.proteinPer100g);
                    createRequest.setCarbsPerUnit(nutritionData.carbsPer100g);
                    createRequest.setFatPerUnit(nutritionData.fatPer100g);
                    createRequest.setFiberPerUnit(nutritionData.fiberPer100g);
                    
                    logger.info("Created food item '{}' with fallback nutrition: {} cal, {}g protein per 100g", 
                               parsedData.getFoodName(), nutritionData.caloriesPer100g, nutritionData.proteinPer100g);
                }
            } else {
                // Fallback to hardcoded values
                FoodNutritionData nutritionData = getFoodNutritionData(parsedData.getFoodName());
                createRequest.setCaloriesPerUnit((int) Math.round(nutritionData.caloriesPer100g));
                createRequest.setProteinPerUnit(nutritionData.proteinPer100g);
                createRequest.setCarbsPerUnit(nutritionData.carbsPer100g);
                createRequest.setFatPerUnit(nutritionData.fatPer100g);
                createRequest.setFiberPerUnit(nutritionData.fiberPer100g);
                
                logger.info("Created food item '{}' with fallback nutrition: {} cal, {}g protein per 100g", 
                           parsedData.getFoodName(), nutritionData.caloriesPer100g, nutritionData.proteinPer100g);
            }
            
            createRequest.setVisibility("public");
            return createRequest;
            
        } catch (Exception e) {
            logger.warn("Failed to estimate macros for {}, using defaults", parsedData.getFoodName());
            
            // Fallback to default values
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedData.getFoodName());
            createRequest.setCategory(parsedData.getMealType().toUpperCase()); // Use meal type as category
            createRequest.setDefaultUnit(parsedData.getUnit());
            createRequest.setWeightPerUnit(calculateWeightPerUnit(parsedData.getUnit(), parsedData.getFoodName()));
            createRequest.setCaloriesPerUnit(100);
            createRequest.setProteinPerUnit(5.0);
            createRequest.setCarbsPerUnit(10.0);
            createRequest.setFatPerUnit(3.0);
            createRequest.setFiberPerUnit(1.0);
            createRequest.setVisibility("public");
            
            return createRequest;
        }
    }
    
    private FoodNutritionData getFoodNutritionData(String foodName) {
        String normalizedName = foodName.toLowerCase().trim();
        
        // Common food nutritional data per 100g
        switch (normalizedName) {
            case "pasta":
            case "spaghetti":
            case "macaroni":
                return new FoodNutritionData(131, 5.0, 25.0, 1.1, 1.8);
            case "rice":
            case "white rice":
                return new FoodNutritionData(130, 2.7, 28.0, 0.3, 0.4);
            case "idly":
            case "idli":
                return new FoodNutritionData(140, 3.5, 28.0, 0.5, 1.0);
            case "bread":
            case "white bread":
                return new FoodNutritionData(265, 9.0, 49.0, 3.2, 2.7);
            case "whole wheat bread":
            case "wholemeal bread":
                return new FoodNutritionData(247, 13.0, 41.0, 4.2, 6.0);
            case "chicken":
            case "chicken breast":
                return new FoodNutritionData(165, 31.0, 0.0, 3.6, 0.0);
            case "beef":
            case "ground beef":
                return new FoodNutritionData(250, 26.0, 0.0, 15.0, 0.0);
            case "salmon":
                return new FoodNutritionData(208, 25.0, 0.0, 12.0, 0.0);
            case "eggs":
            case "egg":
                return new FoodNutritionData(155, 13.0, 1.1, 11.0, 0.0);
            case "milk":
            case "whole milk":
                return new FoodNutritionData(61, 3.2, 4.8, 3.3, 0.0);
            case "cheese":
            case "cheddar cheese":
                return new FoodNutritionData(403, 25.0, 1.3, 33.0, 0.0);
            case "apple":
                return new FoodNutritionData(52, 0.3, 14.0, 0.2, 2.4);
            case "banana":
                return new FoodNutritionData(89, 1.1, 23.0, 0.3, 2.6);
            case "orange":
                return new FoodNutritionData(47, 0.9, 12.0, 0.1, 2.4);
            case "broccoli":
                return new FoodNutritionData(34, 2.8, 7.0, 0.4, 2.6);
            case "carrot":
                return new FoodNutritionData(41, 0.9, 10.0, 0.2, 2.8);
            case "potato":
            case "potatoes":
                return new FoodNutritionData(77, 2.0, 17.0, 0.1, 2.2);
            case "yogurt":
            case "greek yogurt":
                return new FoodNutritionData(59, 10.0, 3.6, 0.4, 0.0);
            case "oats":
            case "oatmeal":
                return new FoodNutritionData(389, 17.0, 66.0, 7.0, 11.0);
            case "almonds":
                return new FoodNutritionData(579, 21.0, 22.0, 50.0, 12.0);
            case "peanut butter":
                return new FoodNutritionData(588, 25.0, 20.0, 50.0, 8.0);
            case "coffee":
            case "black coffee":
                return new FoodNutritionData(2, 0.3, 0.0, 0.0, 0.0);
            case "tea":
            case "black tea":
                return new FoodNutritionData(1, 0.0, 0.0, 0.0, 0.0);
            case "water":
                return new FoodNutritionData(0, 0.0, 0.0, 0.0, 0.0);
            case "hash browns":
            case "hash brown":
                return new FoodNutritionData(326, 2.8, 41.0, 16.0, 2.0);
            case "fish salad":
            case "tuna salad":
                return new FoodNutritionData(200, 20.0, 8.0, 10.0, 2.0);
            case "chicken salad":
                return new FoodNutritionData(120, 15.0, 8.0, 3.0, 2.0);
            default:
                // Default values for unknown foods
                return new FoodNutritionData(100, 5.0, 10.0, 3.0, 1.0);
        }
    }
    
    private AiFoodVoiceParsingService.NutritionData validateNutritionData(AiFoodVoiceParsingService.NutritionData nutrition) {
        // Check if nutrition data is unrealistic (too low for most foods)
        // But allow very low values for beverages like coffee, tea, water
        if (nutrition.getCaloriesPer100g() < 1 || 
            nutrition.getProteinPer100g() < 0 || 
            nutrition.getCarbsPer100g() < 0) {
            logger.warn("AI provided unrealistic nutrition data (calories: {}, protein: {}, carbs: {}), rejecting AI data", 
                       nutrition.getCaloriesPer100g(), nutrition.getProteinPer100g(), nutrition.getCarbsPer100g());
            return null; // This will trigger fallback to hardcoded values
        }
        
        AiFoodVoiceParsingService.NutritionData validated = new AiFoodVoiceParsingService.NutritionData();
        
        // Validate calories (reasonable range: 1-1000 per 100g)
        double calories = Math.max(1, Math.min(1000, nutrition.getCaloriesPer100g()));
        validated.setCaloriesPer100g(calories);
        
        // Validate protein (reasonable range: 0-100g per 100g)
        double protein = Math.max(0, Math.min(100, nutrition.getProteinPer100g()));
        validated.setProteinPer100g(protein);
        
        // Validate carbs (reasonable range: 0-100g per 100g)
        double carbs = Math.max(0, Math.min(100, nutrition.getCarbsPer100g()));
        validated.setCarbsPer100g(carbs);
        
        // Validate fat (reasonable range: 0-100g per 100g)
        double fat = Math.max(0, Math.min(100, nutrition.getFatPer100g()));
        validated.setFatPer100g(fat);
        
        // Validate fiber (reasonable range: 0-50g per 100g)
        double fiber = Math.max(0, Math.min(50, nutrition.getFiberPer100g()));
        validated.setFiberPer100g(fiber);
        
        // Log if validation changed any values
        if (calories != nutrition.getCaloriesPer100g() || 
            protein != nutrition.getProteinPer100g() || 
            carbs != nutrition.getCarbsPer100g() || 
            fat != nutrition.getFatPer100g() || 
            fiber != nutrition.getFiberPer100g()) {
            logger.warn("Nutrition data validation adjusted values for food item");
        }
        
        return validated;
    }
    
    // Helper class for nutritional data
    private static class FoodNutritionData {
        final double caloriesPer100g;
        final double proteinPer100g;
        final double carbsPer100g;
        final double fatPer100g;
        final double fiberPer100g;
        
        FoodNutritionData(double calories, double protein, double carbs, double fat, double fiber) {
            this.caloriesPer100g = calories;
            this.proteinPer100g = protein;
            this.carbsPer100g = carbs;
            this.fatPer100g = fat;
            this.fiberPer100g = fiber;
        }
    }
}
