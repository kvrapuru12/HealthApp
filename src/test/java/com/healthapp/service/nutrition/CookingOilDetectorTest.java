package com.healthapp.service.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookingOilDetectorTest {

    @Test
    void boiledEggsDoesNotMatchOil() {
        assertFalse(CookingOilDetector.containsCookingOil("boiled eggs"));
        assertFalse(CookingOilDetector.containsCookingOil("two boiled eggs"));
    }

    @Test
    void spoiledFoodDoesNotMatchOil() {
        assertFalse(CookingOilDetector.containsCookingOil("spoiled food"));
    }

    @Test
    void oilyFishDoesNotMatchOil() {
        assertFalse(CookingOilDetector.containsCookingOil("oily fish"));
    }

    @Test
    void explicitOilsMatch() {
        assertTrue(CookingOilDetector.containsCookingOil("olive oil"));
        assertTrue(CookingOilDetector.containsCookingOil("olive oil drizzle"));
        assertTrue(CookingOilDetector.containsCookingOil("avocado oil"));
        assertTrue(CookingOilDetector.containsCookingOil("sesame oil"));
        assertTrue(CookingOilDetector.containsCookingOil("coconut oil"));
    }
}
