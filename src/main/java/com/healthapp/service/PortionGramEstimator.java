package com.healthapp.service;

import com.healthapp.service.nutrition.NutritionValidator;
import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.springframework.stereotype.Component;

/**
 * Estimates edible weight in grams from quantity + unit + food name.
 * Corrects common AI mistakes (e.g. quantity=1 treated as 1 gram for "1 banana").
 */
@Component
public class PortionGramEstimator {

    private static final double MIN_PLAUSIBLE_GRAMS = 5.0;

    public double resolveEffectiveGrams(String foodName, Double quantity, String unit, Double estimatedGrams) {
        double qty = quantity != null && quantity > 0 ? quantity : 1.0;
        String normalizedUnit = unit != null ? unit.toLowerCase().trim() : "serving";

        if (isMassUnit(normalizedUnit) && qty >= MIN_PLAUSIBLE_GRAMS) {
            return qty;
        }

        double fromQuantity = RecommendedPortionCatalog.gramsPerUnit(foodName, normalizedUnit, qty);
        double fromEstimate = estimatedGrams != null && estimatedGrams > 0 ? estimatedGrams : 0;

        if (fromEstimate >= MIN_PLAUSIBLE_GRAMS && !isImplausiblyLowForUnit(fromEstimate, fromQuantity, normalizedUnit)) {
            return fromEstimate;
        }

        if (fromQuantity >= MIN_PLAUSIBLE_GRAMS) {
            return fromQuantity;
        }

        if (fromEstimate > 0 && !NutritionValidator.isLikelySpice(foodName)) {
            return applyMinimumPlausibleGrams(foodName, fromEstimate);
        }

        return applyMinimumPlausibleGrams(foodName, Math.max(fromQuantity, fromEstimate));
    }

    public double applyMinimumPlausibleGrams(String foodName, double grams) {
        double minimum = RecommendedPortionCatalog.minimumPlausibleGrams(foodName);
        return Math.max(grams, minimum);
    }

    public double gramsFromQuantity(String foodName, double quantity, String unit) {
        return RecommendedPortionCatalog.gramsPerUnit(foodName, unit, quantity);
    }

    public double weightPerUnit(String unit, String foodName) {
        return RecommendedPortionCatalog.weightPerUnit(unit, foodName);
    }

    private boolean isImplausiblyLowForUnit(double estimated, double fromQuantity, String unit) {
        if (fromQuantity < MIN_PLAUSIBLE_GRAMS) {
            return false;
        }
        return estimated < MIN_PLAUSIBLE_GRAMS && !isMassUnit(unit);
    }

    static boolean isMassUnit(String unit) {
        return "gram".equals(unit) || "grams".equals(unit) || "g".equals(unit)
                || "kg".equals(unit) || "kilogram".equals(unit) || "kilograms".equals(unit);
    }
}
