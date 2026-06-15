package com.healthapp.service.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodNutritionFallbackTest {

    private final FoodNutritionFallback fallback = new FoodNutritionFallback();

    @Test
    void unknownFoodReturnsEmpty() {
        assertTrue(fallback.resolveKnown("mystery superfood xyz").isEmpty());
    }

    @Test
    void oatsAndChiaUseNamedMacros() {
        assertEquals(389, fallback.resolveKnown("rolled oats").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(486, fallback.resolveKnown("chia seeds").orElseThrow().getCaloriesPer100g(), 0.1);
    }

    @Test
    void boiledEggUsesEggMacrosNotOil() {
        NutritionProfile profile = fallback.resolveKnown("boiled egg for breakfast").orElseThrow();
        assertEquals(155, profile.getCaloriesPer100g(), 0.1);
        assertEquals(13.0, profile.getProteinPer100g(), 0.1);
    }

    @Test
    void oliveOilStillUsesOilMacros() {
        NutritionProfile profile = fallback.resolveKnown("olive oil").orElseThrow();
        assertEquals(884, profile.getCaloriesPer100g(), 0.1);
    }

    @Test
    void twoBoiledEggsPhraseUsesEggMacros() {
        NutritionProfile profile = fallback.resolveKnown("two boiled eggs").orElseThrow();
        assertEquals(155, profile.getCaloriesPer100g(), 0.1);
    }
}
