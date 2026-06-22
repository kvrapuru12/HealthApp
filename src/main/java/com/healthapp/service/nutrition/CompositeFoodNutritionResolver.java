package com.healthapp.service.nutrition;

import com.healthapp.service.AiFoodVoiceParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Multi-ingredient meals:
 * <ul>
 *   <li>Explicit per-ingredient weights → USDA ingredient blend (HIGH/MEDIUM when USDA hits)</li>
 *   <li>Vague composite (no stated grams) → OpenAI meal estimate (MEDIUM)</li>
 *   <li>Last resort → named hardcoded fallback only (LOW)</li>
 * </ul>
 */
@Component
public class CompositeFoodNutritionResolver {

    private static final Logger logger = LoggerFactory.getLogger(CompositeFoodNutritionResolver.class);

    private final NutritionLookupService nutritionLookupService;
    private final FoodNutritionFallback foodNutritionFallback;

    @Autowired
    public CompositeFoodNutritionResolver(NutritionLookupService nutritionLookupService,
                                          FoodNutritionFallback foodNutritionFallback) {
        this.nutritionLookupService = nutritionLookupService;
        this.foodNutritionFallback = foodNutritionFallback;
    }

    public void resolve(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.isUserSpecifiedMacros()) {
            return;
        }
        AiFoodVoiceParsingService.NutritionData aiNutrition = parsedData.getNutrition();
        clearInvalidAiNutrition(parsedData);

        boolean explicitPortions = parsedData.isUserSpecifiedGrams();
        boolean aiIngredientBlend = !explicitPortions && hasAiIngredientBreakdown(parsedData);

        if (explicitPortions || aiIngredientBlend) {
            tryIngredientBlend(parsedData);
            if (parsedData.getNutritionSource() == NutritionSource.FALLBACK_HARDCODED) {
                logger.info("Rejecting all-fallback ingredient blend for '{}'; trying LLM estimate",
                        parsedData.getFoodName());
                clearNutrition(parsedData);
            }
        } else {
            applyLlmNutrition(parsedData, aiNutrition);
        }

        if (parsedData.getNutrition() == null) {
            applyLlmNutrition(parsedData, aiNutrition);
        }

        if (parsedData.getNutrition() == null) {
            applyNamedFallback(parsedData);
        } else if (NutritionValidator.isMultiIngredientComposite(parsedData)
                && parsedData.getNutritionSource() != NutritionSource.LLM
                && NutritionValidator.hasImplausibleCompositeNutrition(parsedData.getNutrition())) {
            logger.info("Implausible composite nutrition for '{}'; trying LLM then named fallback",
                    parsedData.getFoodName());
            clearNutrition(parsedData);
            applyLlmNutrition(parsedData, aiNutrition);
            if (parsedData.getNutrition() == null) {
                applyNamedFallback(parsedData);
            }
        }
    }

    private void tryIngredientBlend(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.getIngredients().isEmpty()) {
            return;
        }
        List<NutritionLookupService.IngredientPortion> portions = parsedData.getIngredients().stream()
                .map(i -> new NutritionLookupService.IngredientPortion(
                        i.getName(), i.getEstimatedGrams(), i.getFdcSearchTerm()))
                .toList();
        nutritionLookupService.blendIngredients(portions).ifPresent(profile -> {
            if (applyProfile(parsedData, profile)) {
                logger.info("Composite USDA blend for '{}': {} cal/100g source={}",
                        parsedData.getFoodName(), profile.getCaloriesPer100g(), profile.getSource());
            }
        });
    }

    private static boolean hasAiIngredientBreakdown(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.getIngredients() == null || parsedData.getIngredients().size() < 2) {
            return false;
        }
        long withGrams = parsedData.getIngredients().stream()
                .filter(i -> i.getEstimatedGrams() > 0)
                .count();
        return withGrams >= 2;
    }

    private void applyLlmNutrition(AiFoodVoiceParsingService.ParsedFoodData parsedData,
                                   AiFoodVoiceParsingService.NutritionData aiNutrition) {
        if (aiNutrition == null) {
            return;
        }
        AiFoodVoiceParsingService.NutritionData candidate = parsedData.getNutrition() != null
                ? parsedData.getNutrition()
                : aiNutrition;
        AiFoodVoiceParsingService.NutritionData validated = NutritionValidator.validateForPersist(
                parsedData.getFoodName(), candidate, parsedData.getEstimatedGrams());
        if (validated != null) {
            parsedData.setNutrition(validated);
            parsedData.setNutritionSource(NutritionSource.LLM);
            parsedData.setNutritionConfidence(NutritionConfidence.MEDIUM);
            logger.info("nutritionSource=llm composite food='{}' cal/100g={}",
                    parsedData.getFoodName(), validated.getCaloriesPer100g());
        }
    }

    private void clearInvalidAiNutrition(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        if (parsedData.getNutrition() == null) {
            return;
        }
        boolean shouldClear;
        if (NutritionValidator.isMultiIngredientComposite(parsedData)) {
            shouldClear = NutritionValidator.hasImplausibleCompositeNutrition(parsedData.getNutrition());
        } else {
            shouldClear = NutritionValidator.validateNutritionData(
                    parsedData.getFoodName(), parsedData.getNutrition(), parsedData.getEstimatedGrams()) == null;
        }
        if (shouldClear) {
            clearNutrition(parsedData);
        }
    }

    private void clearNutrition(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        parsedData.setNutrition(null);
        parsedData.setNutritionSource(null);
        parsedData.setNutritionConfidence(null);
        parsedData.setFdcId(null);
    }

    private boolean applyProfile(AiFoodVoiceParsingService.ParsedFoodData parsedData,
                                 NutritionProfile profile) {
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(profile.getCaloriesPer100g());
        nutrition.setProteinPer100g(profile.getProteinPer100g());
        nutrition.setCarbsPer100g(profile.getCarbsPer100g());
        nutrition.setFatPer100g(profile.getFatPer100g());
        nutrition.setFiberPer100g(profile.getFiberPer100g());
        AiFoodVoiceParsingService.NutritionData validated = NutritionValidator.isMultiIngredientComposite(parsedData)
                ? NutritionValidator.validateForPersist(parsedData.getFoodName(), nutrition, parsedData.getEstimatedGrams())
                : NutritionValidator.validateNutritionData(
                        parsedData.getFoodName(), nutrition, parsedData.getEstimatedGrams());
        if (validated == null) {
            return false;
        }
        parsedData.setNutrition(validated);
        parsedData.setNutritionSource(profile.getSource());
        parsedData.setNutritionConfidence(profile.toConfidenceLevel());
        parsedData.setFdcId(profile.getFdcId());
        return true;
    }

    private void applyNamedFallback(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        Optional<NutritionProfile> fallback = foodNutritionFallback.resolveKnown(parsedData.getFoodName());
        if (fallback.isEmpty()) {
            logger.warn("No named fallback for composite '{}'", parsedData.getFoodName());
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
            logger.warn("Named fallback failed validation for composite '{}'", parsedData.getFoodName());
            return;
        }
        parsedData.setNutrition(validated);
        parsedData.setNutritionSource(NutritionSource.FALLBACK_HARDCODED);
        parsedData.setNutritionConfidence(NutritionConfidence.LOW);
        logger.info("nutritionSource=fallback_hardcoded composite food='{}' cal/100g={}",
                parsedData.getFoodName(), profile.getCaloriesPer100g());
    }
}
