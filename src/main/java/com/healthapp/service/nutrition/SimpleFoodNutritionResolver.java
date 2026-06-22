package com.healthapp.service.nutrition;

import com.healthapp.entity.FoodItem;
import com.healthapp.service.AiFoodVoiceParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Simple single foods: trusted DB macros → USDA (with cache/fallback) → LLM → hardcoded fallback.
 */
@Component
public class SimpleFoodNutritionResolver {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFoodNutritionResolver.class);

    private final NutritionLookupService nutritionLookupService;
    private final FoodNutritionFallback foodNutritionFallback;

    @Autowired
    public SimpleFoodNutritionResolver(@Autowired(required = false) NutritionLookupService nutritionLookupService,
                                       FoodNutritionFallback foodNutritionFallback) {
        this.nutritionLookupService = nutritionLookupService;
        this.foodNutritionFallback = foodNutritionFallback;
    }

    public static boolean isSimpleFood(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        return parsedData != null
                && (parsedData.getIngredients() == null || parsedData.getIngredients().isEmpty());
    }

    public static boolean isTrustedFoodItem(FoodItem item) {
        if (item == null || item.getCaloriesPerUnit() == null || item.getCaloriesPerUnit() <= 0) {
            return false;
        }
        if (item.getFdcId() != null && item.getFdcId() > 0) {
            return true;
        }
        if (NutritionValidator.hasImplausibleStoredNutrition(item.getName(), item.getCaloriesPerUnit())) {
            return false;
        }
        return !isGenericDefaultFoodItem(item);
    }

    public void applyFoodItemToParsedData(AiFoodVoiceParsingService.ParsedFoodData parsedData, FoodItem item) {
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(item.getCaloriesPerUnit());
        nutrition.setProteinPer100g(safe(item.getProteinPerUnit()));
        nutrition.setCarbsPer100g(safe(item.getCarbsPerUnit()));
        nutrition.setFatPer100g(safe(item.getFatPerUnit()));
        nutrition.setFiberPer100g(safe(item.getFiberPerUnit()));
        parsedData.setNutrition(nutrition);
        parsedData.setNutritionSource(item.getFdcId() != null ? NutritionSource.USDA : NutritionSource.LLM);
        parsedData.setNutritionConfidence(item.getFdcId() != null
                ? NutritionConfidence.HIGH
                : NutritionConfidence.MEDIUM);
        parsedData.setFdcId(item.getFdcId());
        logger.info("nutritionSource=db food='{}' cal/100g={}", parsedData.getFoodName(), item.getCaloriesPerUnit());
    }

    public void resolve(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.isUserSpecifiedMacros()) {
            return;
        }
        clearInvalidAiNutrition(parsedData);

        if (parsedData.getNutrition() != null) {
            parsedData.setNutritionSource(NutritionSource.LLM);
            parsedData.setNutritionConfidence(NutritionConfidence.MEDIUM);
            logger.info("nutritionSource=llm skippedUsda=true food='{}' cal/100g={}",
                    parsedData.getFoodName(), parsedData.getNutrition().getCaloriesPer100g());
            return;
        }

        if (nutritionLookupService != null) {
            long t0 = System.nanoTime();
            nutritionLookupService.lookup(parsedData.getFoodName()).ifPresent(profile -> {
                if (applyProfile(parsedData, profile)) {
                    logger.info("nutritionSource=usda fdcId={} confidence={} food='{}' cal/100g={}",
                            profile.getFdcId(), profile.getConfidence(),
                            parsedData.getFoodName(), profile.getCaloriesPer100g());
                }
            });
            logger.info("perf usdaMs={} food='{}'",
                    (System.nanoTime() - t0) / 1_000_000, parsedData.getFoodName());
        }

        if (parsedData.getNutrition() == null) {
            applyFallback(parsedData);
        }

        if (parsedData.getNutrition() != null && parsedData.getNutritionSource() == null) {
            parsedData.setNutritionSource(NutritionSource.LLM);
            parsedData.setNutritionConfidence(NutritionConfidence.MEDIUM);
        }
    }

    private void clearInvalidAiNutrition(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.getNutrition() == null) {
            return;
        }
        if (NutritionValidator.validateNutritionData(
                parsedData.getFoodName(), parsedData.getNutrition(), parsedData.getEstimatedGrams()) == null) {
            parsedData.setNutrition(null);
            parsedData.setNutritionSource(null);
            parsedData.setNutritionConfidence(null);
        }
    }

    private void applyFallback(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        Optional<NutritionProfile> fallback = foodNutritionFallback.resolveKnown(parsedData.getFoodName());
        if (fallback.isEmpty()) {
            logger.warn("No named fallback for simple food '{}'", parsedData.getFoodName());
            return;
        }
        NutritionProfile profile = fallback.get();
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(profile.getCaloriesPer100g());
        nutrition.setProteinPer100g(profile.getProteinPer100g());
        nutrition.setCarbsPer100g(profile.getCarbsPer100g());
        nutrition.setFatPer100g(profile.getFatPer100g());
        nutrition.setFiberPer100g(profile.getFiberPer100g());
        AiFoodVoiceParsingService.NutritionData validated = NutritionValidator.validateForPersist(
                parsedData.getFoodName(), nutrition, parsedData.getEstimatedGrams());
        if (validated == null) {
            logger.warn("Hardcoded fallback rejected for '{}': {} cal/100g",
                    parsedData.getFoodName(), profile.getCaloriesPer100g());
            return;
        }
        parsedData.setNutrition(validated);
        parsedData.setNutritionSource(NutritionSource.FALLBACK_HARDCODED);
        parsedData.setNutritionConfidence(NutritionConfidence.LOW);
        parsedData.setFdcId(null);
        logger.info("nutritionSource=fallback_hardcoded simple food='{}' cal/100g={}",
                parsedData.getFoodName(), validated.getCaloriesPer100g());
    }

    private boolean applyProfile(AiFoodVoiceParsingService.ParsedFoodData parsedData, NutritionProfile profile) {
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(profile.getCaloriesPer100g());
        nutrition.setProteinPer100g(profile.getProteinPer100g());
        nutrition.setCarbsPer100g(profile.getCarbsPer100g());
        nutrition.setFatPer100g(profile.getFatPer100g());
        nutrition.setFiberPer100g(profile.getFiberPer100g());
        AiFoodVoiceParsingService.NutritionData validated = NutritionValidator.validateNutritionData(
                parsedData.getFoodName(), nutrition, parsedData.getEstimatedGrams());
        if (validated == null) {
            logger.warn("Rejected USDA profile for '{}': {} cal/100g",
                    parsedData.getFoodName(), profile.getCaloriesPer100g());
            return false;
        }
        parsedData.setNutrition(validated);
        parsedData.setNutritionSource(profile.getSource());
        parsedData.setNutritionConfidence(profile.toConfidenceLevel());
        parsedData.setFdcId(profile.getFdcId());
        return true;
    }

    private static boolean isGenericDefaultFoodItem(FoodItem item) {
        return item.getCaloriesPerUnit() != null && item.getCaloriesPerUnit() == 100
                && item.getProteinPerUnit() != null && item.getProteinPerUnit() == 5.0
                && item.getCarbsPerUnit() != null && item.getCarbsPerUnit() == 10.0
                && item.getFatPerUnit() != null && item.getFatPerUnit() == 3.0;
    }

    private static double safe(Double value) {
        return value != null ? value : 0.0;
    }
}
