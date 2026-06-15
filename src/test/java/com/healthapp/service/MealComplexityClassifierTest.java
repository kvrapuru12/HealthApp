package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MealComplexityClassifierTest {

    private final MealComplexityClassifier classifier = new MealComplexityClassifier();

    @Test
    void classify_simpleUtterance() {
        assertEquals(MealComplexity.SIMPLE, classifier.classify("I ate 1 medium avocado"));
    }

    @Test
    void classify_complexSmoothie() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("breakfast smoothie with chia, almonds, banana, berries, oats and yogurt"));
    }

    @Test
    void classify_toastWithToppingsAsComplex() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("two slices toast with butter and peanut butter for breakfast"));
    }

    @Test
    void classify_typoBreakfastAsComplex() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("had a bannana and 2 scrabled eggs for brekfast"));
    }

    @Test
    void classify_buffetSushiAsComplex() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("all you can eat sushi dinner about 12 pieces salmon avocado and 4 tuna rolls"));
    }

    @Test
    void classify_wineCheeseBoardAsComplex() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("two glasses of red wine with cheese board"));
    }

    @Test
    void classify_postWorkoutWheyAsComplex() {
        assertEquals(MealComplexity.COMPLEX,
                classifier.classify("after my workout I had a scoop of whey protein mixed with water and a banana"));
    }
}
