package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortionGramEstimatorTest {

    private final PortionGramEstimator estimator = new PortionGramEstimator();

    @Test
    void resolvesBananaCountToRealisticGrams() {
        double grams = estimator.resolveEffectiveGrams("banana", 1.0, "medium", 1.0);
        assertEquals(120.0, grams, 0.1);
    }

    @Test
    void resolvesExplicitGramQuantity() {
        double grams = estimator.resolveEffectiveGrams("chicken breast", 150.0, "grams", 1.0);
        assertEquals(150.0, grams, 0.1);
    }

    @Test
    void resolvesCupOfQuinoa() {
        double grams = estimator.resolveEffectiveGrams("cooked quinoa", 1.0, "cup", 1.0);
        assertEquals(185.0, grams, 0.1);
    }

    @Test
    void resolvesTwoEggs() {
        double grams = estimator.resolveEffectiveGrams("boiled egg", 2.0, "pieces", 2.0);
        assertEquals(100.0, grams, 0.1);
    }

    @Test
    void resolvesTablespoonButter() {
        double grams = estimator.resolveEffectiveGrams("butter", 1.0, "tablespoon", 1.0);
        assertEquals(14.0, grams, 0.1);
    }

    @Test
    void resolvesTwoSlicesToast() {
        double grams = estimator.resolveEffectiveGrams("whole wheat toast", 2.0, "slices", 2.0);
        assertEquals(60.0, grams, 0.1);
    }

    @Test
    void resolvesThreeCookies() {
        double grams = estimator.resolveEffectiveGrams("cookies", 3.0, "pieces", null);
        assertEquals(96.0, grams, 0.1);
    }
}
