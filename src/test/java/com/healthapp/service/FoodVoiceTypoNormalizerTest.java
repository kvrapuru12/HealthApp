package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodVoiceTypoNormalizerTest {

    @Test
    void normalizesCommonFoodTypos() {
        String input = "had a bannana and 2 scrabled eggs for brekfast";
        String normalized = FoodVoiceTypoNormalizer.normalize(input);
        assertTrue(normalized.contains("banana"));
        assertTrue(normalized.contains("scrambled"));
        assertTrue(normalized.contains("breakfast"));
    }

    @Test
    void normalizesRepeatedTypos() {
        assertEquals("banana banana", FoodVoiceTypoNormalizer.normalize("bannana bannana"));
    }
}
