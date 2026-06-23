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

    @Test
    void resolvesCommonFailedFoodFallbacks() {
        assertEquals(40, fallback.resolveKnown("side salad").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(330, fallback.resolveKnown("garlic knots").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(210, fallback.resolveKnown("masala dosa").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(260, fallback.resolveKnown("coconut chutney").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(55, fallback.resolveKnown("sambar").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(180, fallback.resolveKnown("salmon avocado sushi").orElseThrow().getCaloriesPer100g(), 0.1);
    }

    @Test
    void resolvesSpinachDalRotiPaneer() {
        assertEquals(23, fallback.resolveKnown("spinach").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(116, fallback.resolveKnown("dal").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(297, fallback.resolveKnown("roti").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(265, fallback.resolveKnown("paneer curry").orElseThrow().getCaloriesPer100g(), 0.1);
        assertEquals(170, fallback.resolveKnown("Indian thali plate").orElseThrow().getCaloriesPer100g(), 0.1);
    }
}
