package com.healthapp.service;

import com.healthapp.config.AiFoodProperties;
import com.healthapp.dto.*;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.FoodLog;
import com.healthapp.exception.VoiceFoodLogException;
import com.healthapp.service.nutrition.CompositeFoodNutritionResolver;
import com.healthapp.service.nutrition.NutritionConfidence;
import com.healthapp.service.nutrition.NutritionSource;
import com.healthapp.service.nutrition.NutritionValidator;
import com.healthapp.service.nutrition.SimpleFoodNutritionResolver;

import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
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

    @Autowired
    private AiFoodProperties aiFoodProperties;

    @Autowired
    private PortionGramEstimator portionGramEstimator;

    @Autowired
    private SimpleFoodNutritionResolver simpleFoodNutritionResolver;

    @Autowired
    private CompositeFoodNutritionResolver compositeFoodNutritionResolver;
    
    public VoiceFoodLogResponse processVoiceFoodLog(VoiceFoodLogRequest request, Long authenticatedUserId) {
        // Validate user access
        if (!request.getUserId().equals(authenticatedUserId)) {
            throw new IllegalArgumentException("You can only create food logs for your own account.");
        }

        userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (aiFoodVoiceParsingService == null) {
            throw new VoiceFoodLogException(
                    "AI_SERVICE_UNAVAILABLE",
                    "Voice food logging is not available right now. Please try again later or add food manually.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        AiFoodVoiceParsingService.ParsedFoodDataList parsedDataList;
        try {
            parsedDataList = aiFoodVoiceParsingService.parseVoiceText(request.getVoiceText());
        } catch (RuntimeException e) {
            logger.error("AI food voice parse failed: {}", e.getMessage(), e);
            throw new VoiceFoodLogException(
                    "AI_PARSE_FAILED",
                    "We could not interpret your food from that description. Try shorter wording, name each food clearly, or add manually.",
                    HttpStatus.BAD_GATEWAY,
                    e);
        }

        if (parsedDataList.getCompositeMeals().isEmpty() && parsedDataList.getFoodItems().isEmpty()) {
            throw new VoiceFoodLogException(
                    "NO_FOOD_PARSED",
                    "No foods were recognized from what you said. Try again with specific items (for example, \"two eggs and toast\") or add manually.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<VoiceFoodLogResponse.LoggedFoodItem> loggedItems = new ArrayList<>();

        for (AiFoodVoiceParsingService.ParsedFoodData parsedData : parsedDataList.getCompositeMeals()) {
            appendParsedFoodLog(request, authenticatedUserId, parsedData, true, loggedItems);
        }
        for (AiFoodVoiceParsingService.ParsedFoodData parsedData : parsedDataList.getFoodItems()) {
            appendParsedFoodLog(request, authenticatedUserId, parsedData, false, loggedItems);
        }

        if (loggedItems.isEmpty()) {
            throw new VoiceFoodLogException(
                    "NO_LOGS_CREATED",
                    "We understood your message but could not create any food logs. Check quantities and meal types, then try again or add manually.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        long compositeCount = loggedItems.stream().filter(VoiceFoodLogResponse.LoggedFoodItem::isCompositeMeal).count();
        long separateCount = loggedItems.size() - compositeCount;

        String message;
        if (loggedItems.size() == 1) {
            message = compositeCount == 1
                    ? "Food log created from voice input (composite meal)"
                    : "Food log created from voice input";
        } else if (compositeCount > 0 && separateCount > 0) {
            message = String.format("Created %d composite meal(s) and %d separate food log(s)", compositeCount, separateCount);
        } else if (compositeCount > 0) {
            message = String.format("Created %d composite meal(s) from voice input", compositeCount);
        } else {
            message = String.format("Created %d food logs from voice input", separateCount);
        }

        return new VoiceFoodLogResponse(message, loggedItems);
    }

    private void appendParsedFoodLog(VoiceFoodLogRequest request, Long authenticatedUserId,
            AiFoodVoiceParsingService.ParsedFoodData parsedData, boolean compositeMeal,
            List<VoiceFoodLogResponse.LoggedFoodItem> loggedItems) {
        String foodLabel = parsedData.getFoodName() != null && !parsedData.getFoodName().isBlank()
                ? parsedData.getFoodName()
                : "this item";
        try {
            Double normalizedGrams = resolveEstimatedGrams(parsedData);
            parsedData.setEstimatedGrams(normalizedGrams);
            FoodItem foodItem = findOrCreateFoodItem(parsedData, authenticatedUserId);

            FoodLogCreateRequest logRequest = new FoodLogCreateRequest();
            logRequest.setUserId(authenticatedUserId);
            logRequest.setFoodItemId(foodItem.getId());
            LocalDateTime loggedAt = parsedData.getLoggedAt() != null ? parsedData.getLoggedAt() : LocalDateTime.now();
            LocalDateTime now = LocalDateTime.now();
            if (loggedAt.isAfter(now.plusMinutes(10))) {
                logger.warn("Clamping AI loggedAt {} to now (future time rejected by food log rules)", loggedAt);
                loggedAt = now;
            }
            logRequest.setLoggedAt(loggedAt);
            String mealType = parsedData.getMealType() != null && !parsedData.getMealType().isBlank()
                    ? parsedData.getMealType().toUpperCase()
                    : "SNACK";
            logRequest.setMealType(mealType);
            logRequest.setQuantity(parsedData.getQuantity());
            String unit = parsedData.getUnit();
            if (unit != null && unit.length() > 20) {
                unit = unit.substring(0, 20);
            }
            logRequest.setUnit(unit);
            logRequest.setEstimatedGrams(normalizedGrams);
            String note = parsedData.getNote() != null && !parsedData.getNote().trim().isEmpty()
                    ? parsedData.getNote()
                    : "Created from voice input: " + request.getVoiceText();
            if (note.length() > FoodLog.NOTE_MAX_LENGTH) {
                note = note.substring(0, FoodLog.NOTE_MAX_LENGTH - 3) + "...";
            }
            logRequest.setNote(note);

            FoodLogCreateResponse logResponse = foodLogService.createFoodLog(logRequest, authenticatedUserId);

            if (shouldFlagLowCalorieReview(parsedData.getFoodName(), logResponse.getCalories())) {
                parsedData.setNutritionConfidence(NutritionConfidence.LOW);
            }

            String confidenceLabel = aiFoodProperties.isShowConfidence() && parsedData.getNutritionConfidence() != null
                    ? parsedData.getNutritionConfidence().name()
                    : null;

            loggedItems.add(new VoiceFoodLogResponse.LoggedFoodItem(
                    foodItem.getName(),
                    parsedData.getQuantity(),
                    normalizedGrams,
                    mealType,
                    compositeMeal,
                    logResponse.getCalories(),
                    logResponse.getProtein(),
                    logResponse.getCarbs(),
                    logResponse.getFat(),
                    logResponse.getFiber(),
                    loggedAt.toString(),
                    confidenceLabel
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for voice food item \"{}\": {}", foodLabel, e.getMessage());
            throw new IllegalArgumentException("Could not log \"" + foodLabel + "\": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Failed to save voice food item \"{}\": {}", foodLabel, e.getMessage(), e);
            throw new VoiceFoodLogException(
                    "FOOD_ITEM_SAVE_FAILED",
                    "Could not save \"" + foodLabel + "\". Try again or add that item manually.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }
    
    private FoodItem findOrCreateFoodItem(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        if (parsedData.getFoodName() != null && parsedData.getFoodName().length() > 100) {
            parsedData.setFoodName(parsedData.getFoodName().substring(0, 100));
        }
        String normalizedName = normalizeFoodName(parsedData.getFoodName());
        boolean simple = SimpleFoodNutritionResolver.isSimpleFood(parsedData);

        if (simple) {
            Optional<FoodItem> trusted = findTrustedFoodItem(normalizedName, userId);
            if (trusted.isPresent()
                    && hasUsdaAnchor(trusted.get())
                    && !parsedData.isUserSpecifiedMacros()
                    && !parsedData.isUserSpecifiedGrams()) {
                simpleFoodNutritionResolver.applyFoodItemToParsedData(parsedData, trusted.get());
                logger.info("Using trusted DB food item: {}", normalizedName);
                return trusted.get();
            }
            if (!parsedData.isUserSpecifiedMacros()) {
                simpleFoodNutritionResolver.resolve(parsedData);
            }
        } else if (!parsedData.isUserSpecifiedMacros()) {
            compositeFoodNutritionResolver.resolve(parsedData);
        }

        Optional<FoodItem> existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(
                normalizedName, FoodItem.FoodStatus.ACTIVE, userId);

        if (existingItem.isPresent()) {
            logger.info("Updating user's food item nutrition: {}", normalizedName);
            return updateFoodItemFromParse(existingItem.get(), parsedData, userId);
        }

        existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndVisibility(
                normalizedName, FoodItem.FoodStatus.ACTIVE, FoodItem.FoodVisibility.PUBLIC);

        if (existingItem.isPresent()
                && SimpleFoodNutritionResolver.isTrustedFoodItem(existingItem.get())
                && parsedData.getNutrition() == null
                && !parsedData.isUserSpecifiedGrams()) {
            simpleFoodNutritionResolver.applyFoodItemToParsedData(parsedData, existingItem.get());
            logger.info("Found exact match for public food item: {}", normalizedName);
            return existingItem.get();
        }

        try {
            FoodItemCreateRequest createRequest = buildFoodItemCreateRequest(parsedData, userId);
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

    private Optional<FoodItem> findTrustedFoodItem(String normalizedName, Long userId) {
        Optional<FoodItem> userItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(
                normalizedName, FoodItem.FoodStatus.ACTIVE, userId);
        if (userItem.isPresent() && SimpleFoodNutritionResolver.isTrustedFoodItem(userItem.get())) {
            return userItem;
        }
        Optional<FoodItem> publicItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndVisibility(
                normalizedName, FoodItem.FoodStatus.ACTIVE, FoodItem.FoodVisibility.PUBLIC);
        if (publicItem.isPresent() && SimpleFoodNutritionResolver.isTrustedFoodItem(publicItem.get())) {
            return publicItem;
        }
        return Optional.empty();
    }
    
    private String normalizeFoodName(String foodName) {
        return foodName.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private FoodItemCreateRequest buildFoodItemCreateRequest(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        AiFoodVoiceParsingService.NutritionData resolvedNutrition = parsedData.getNutrition();
        if (resolvedNutrition == null) {
            throw new IllegalArgumentException(
                    "Could not resolve nutrition for \"" + parsedData.getFoodName()
                            + "\". Try a clearer description or add the food manually.");
        }
        AiFoodVoiceParsingService.NutritionData validatedNutrition = NutritionValidator.validateForPersist(
                parsedData.getFoodName(), resolvedNutrition, parsedData.getEstimatedGrams());
        if (validatedNutrition == null) {
            throw new IllegalArgumentException(
                    "Nutrition data for \"" + parsedData.getFoodName() + "\" did not pass validation.");
        }

        FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
        createRequest.setName(parsedData.getFoodName());
        createRequest.setCategory(parsedData.getMealType().toUpperCase());
        createRequest.setDefaultUnit(parsedData.getUnit());
        createRequest.setWeightPerUnit(portionGramEstimator.weightPerUnit(parsedData.getUnit(), parsedData.getFoodName()));
        applyNutritionToRequest(createRequest, validatedNutrition);
        createRequest.setVisibility("public");
        if (parsedData.getFdcId() != null && parsedData.getFdcId() > 0) {
            createRequest.setFdcId(parsedData.getFdcId());
        }
        logger.info("Creating food item '{}' with {} nutrition: {} cal per 100g",
                parsedData.getFoodName(), parsedData.getNutritionSource(), validatedNutrition.getCaloriesPer100g());
        return createRequest;
    }

    private void applyNutritionToRequest(FoodItemCreateRequest createRequest, AiFoodVoiceParsingService.NutritionData nutrition) {
        createRequest.setCaloriesPerUnit((int) Math.round(nutrition.getCaloriesPer100g()));
        createRequest.setProteinPerUnit(nutrition.getProteinPer100g());
        createRequest.setCarbsPerUnit(nutrition.getCarbsPer100g());
        createRequest.setFatPerUnit(nutrition.getFatPer100g());
        createRequest.setFiberPerUnit(nutrition.getFiberPer100g());
    }

    private Double resolveEstimatedGrams(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.isUserSpecifiedGrams() && parsedData.getEstimatedGrams() != null && parsedData.getEstimatedGrams() > 0) {
            return parsedData.getEstimatedGrams();
        }
        return portionGramEstimator.resolveEffectiveGrams(
                parsedData.getFoodName(),
                parsedData.getQuantity(),
                parsedData.getUnit(),
                parsedData.getEstimatedGrams());
    }

    private boolean shouldFlagLowCalorieReview(String foodName, Double totalCalories) {
        return totalCalories != null && totalCalories < 10 && !NutritionValidator.isLikelyBeverage(foodName);
    }

    private FoodItem updateFoodItemFromParse(FoodItem item, AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        if (!item.getCreatedBy().equals(userId)) {
            return item;
        }
        if (parsedData.getNutrition() == null) {
            return item;
        }
        if (SimpleFoodNutritionResolver.isTrustedFoodItem(item) && !parsedData.isUserSpecifiedMacros()
                && !isHighConfidenceNutrition(parsedData)) {
            if (hasUsdaAnchor(item)) {
                logger.info("Keeping trusted nutrition for existing food item: {}", item.getName());
                return item;
            }
        }
        item.setCaloriesPerUnit((int) Math.round(parsedData.getNutrition().getCaloriesPer100g()));
        item.setProteinPerUnit(parsedData.getNutrition().getProteinPer100g());
        item.setCarbsPerUnit(parsedData.getNutrition().getCarbsPer100g());
        item.setFatPerUnit(parsedData.getNutrition().getFatPer100g());
        item.setFiberPerUnit(parsedData.getNutrition().getFiberPer100g());
        item.setWeightPerUnit(portionGramEstimator.weightPerUnit(parsedData.getUnit(), parsedData.getFoodName()));
        if (parsedData.getFdcId() != null) {
            item.setFdcId(parsedData.getFdcId());
        }
        return foodItemRepository.save(item);
    }

    private boolean isHighConfidenceNutrition(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        return (parsedData.getFdcId() != null && parsedData.getFdcId() > 0)
                || (parsedData.getNutritionSource() == NutritionSource.USDA
                && parsedData.getNutritionConfidence() == NutritionConfidence.HIGH);
    }

    private static boolean hasUsdaAnchor(FoodItem item) {
        return item.getFdcId() != null && item.getFdcId() > 0;
    }
}
