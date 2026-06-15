package com.healthapp.service;

import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import com.healthapp.service.nutrition.RecommendedPortionCatalog.PortionRecommendation;
import org.springframework.stereotype.Component;

/**
 * Applies catalog single-portion defaults when the user did not state grams or explicit counts.
 */
@Component
public class RecommendedPortionApplicator {

    private static final double MIN_PLAUSIBLE_GRAMS = 5.0;

    public void applyDefaults(AiFoodVoiceParsingService.ParsedFoodData data) {
        if (data == null || data.isUserSpecifiedGrams()) {
            return;
        }
        if (needsDefaultPortion(data)) {
            PortionRecommendation rec = RecommendedPortionCatalog.singlePortion(data.getFoodName());
            data.setQuantity(rec.quantity());
            data.setUnit(rec.unit());
            if (data.getEstimatedGrams() == null || data.getEstimatedGrams() < MIN_PLAUSIBLE_GRAMS) {
                data.setEstimatedGrams(rec.totalGrams());
            }
        }
        normalizeIngredientPortions(data);
    }

    private boolean needsDefaultPortion(AiFoodVoiceParsingService.ParsedFoodData data) {
        Double grams = data.getEstimatedGrams();
        String unit = data.getUnit() != null ? data.getUnit().toLowerCase().trim() : "";
        if (grams != null && grams >= MIN_PLAUSIBLE_GRAMS && !isGenericUnit(unit)) {
            return false;
        }
        return isGenericUnit(unit) || grams == null || grams < MIN_PLAUSIBLE_GRAMS;
    }

    private void normalizeIngredientPortions(AiFoodVoiceParsingService.ParsedFoodData data) {
        if (data.getIngredients() == null || data.getIngredients().isEmpty()) {
            return;
        }
        double total = 0;
        for (AiFoodVoiceParsingService.IngredientData ingredient : data.getIngredients()) {
            if (ingredient.getEstimatedGrams() < MIN_PLAUSIBLE_GRAMS) {
                ingredient.setEstimatedGrams(
                        RecommendedPortionCatalog.ingredientPortionGrams(ingredient.getName()));
            }
            total += ingredient.getEstimatedGrams();
        }
        if (total > 0) {
            data.setEstimatedGrams(total);
            data.setQuantity(total);
            data.setUnit("grams");
        }
    }

    private static boolean isGenericUnit(String unit) {
        return unit.isBlank() || "serving".equals(unit) || "servings".equals(unit)
                || "grams".equals(unit) || "g".equals(unit);
    }
}
