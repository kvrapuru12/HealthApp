package com.healthapp.service;

import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Raises AI-estimated portions that are far below typical single servings when the user did not
 * state an explicit quantity.
 */
@Component
public class PortionSanityCorrector {

    private static final Logger logger = LoggerFactory.getLogger(PortionSanityCorrector.class);
    private static final double MIN_FRACTION_OF_TYPICAL = 0.75;

    public void apply(AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        if (dataList == null) {
            return;
        }
        for (AiFoodVoiceParsingService.ParsedFoodData composite : dataList.getCompositeMeals()) {
            correctComposite(composite);
        }
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getFoodItems()) {
            correctItem(item, null);
        }
    }

    private void correctComposite(AiFoodVoiceParsingService.ParsedFoodData composite) {
        if (composite.getIngredients() == null || composite.getIngredients().isEmpty()) {
            correctItem(composite, null);
            return;
        }
        String mealContext = composite.getFoodName();
        double ingredientTotal = 0;
        for (AiFoodVoiceParsingService.IngredientData ingredient : composite.getIngredients()) {
            double corrected = correctGrams(
                    ingredient.getName(), ingredient.getEstimatedGrams(), mealContext, composite.isUserSpecifiedGrams());
            if (corrected != ingredient.getEstimatedGrams()) {
                logger.info("Portion sanity: raised '{}' from {}g to {}g",
                        ingredient.getName(), ingredient.getEstimatedGrams(), corrected);
                ingredient.setEstimatedGrams(corrected);
            }
            ingredientTotal += ingredient.getEstimatedGrams();
        }
        if (!composite.isUserSpecifiedGrams() && ingredientTotal > 0) {
            composite.setEstimatedGrams(ingredientTotal);
            composite.setQuantity(ingredientTotal);
            composite.setUnit("grams");
        }
    }

    private void correctItem(AiFoodVoiceParsingService.ParsedFoodData item, String mealContext) {
        if (item.isUserSpecifiedGrams()) {
            return;
        }
        double corrected = correctGrams(item.getFoodName(), item.getEstimatedGrams(), mealContext, false);
        if (corrected != item.getEstimatedGrams()) {
            logger.info("Portion sanity: raised '{}' from {}g to {}g",
                    item.getFoodName(), item.getEstimatedGrams(), corrected);
            item.setEstimatedGrams(corrected);
            if ("grams".equalsIgnoreCase(item.getUnit()) || "g".equalsIgnoreCase(item.getUnit())) {
                item.setQuantity(corrected);
            }
        }
    }

    private double correctGrams(String foodName, double currentGrams, String mealContext, boolean userSpecified) {
        if (userSpecified || foodName == null || foodName.isBlank()) {
            return currentGrams;
        }
        double typical = RecommendedPortionCatalog.typicalMinimumServingGrams(foodName, mealContext);
        if (typical <= 0) {
            typical = RecommendedPortionCatalog.ingredientPortionGrams(foodName, mealContext);
        }
        if (typical <= 0) {
            return currentGrams;
        }
        double threshold = typical * MIN_FRACTION_OF_TYPICAL;
        if (currentGrams > 0 && currentGrams < threshold) {
            return typical;
        }
        if (currentGrams <= 0) {
            return typical;
        }
        return currentGrams;
    }
}
