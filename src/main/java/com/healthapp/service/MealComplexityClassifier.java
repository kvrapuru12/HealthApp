package com.healthapp.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class MealComplexityClassifier {

    private static final Pattern MEAL_TIME_PATTERN = Pattern.compile(
            "\\b(breakfast|lunch|dinner|snack)\\b.*\\b(breakfast|lunch|dinner|snack)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NUMERIC_QUANTITY_PATTERN = Pattern.compile(
            "\\b\\d+\\s*(pieces?|rolls?|slices?|glasses?|cups?|oz|g|grams?)\\b",
            Pattern.CASE_INSENSITIVE);

    public MealComplexity classify(String voiceText) {
        if (voiceText == null || voiceText.isBlank()) {
            return MealComplexity.SIMPLE;
        }
        String text = voiceText.toLowerCase(Locale.ROOT);
        if (text.length() > 120) {
            return MealComplexity.COMPLEX;
        }
        if (NUMERIC_QUANTITY_PATTERN.matcher(text).results().count() >= 2) {
            return MealComplexity.COMPLEX;
        }
        if (countSeparators(text) >= 3) {
            return MealComplexity.COMPLEX;
        }
        if (hasCompositeIndicator(text)) {
            return MealComplexity.COMPLEX;
        }
        if (MEAL_TIME_PATTERN.matcher(text).find()) {
            return MealComplexity.COMPLEX;
        }
        return MealComplexity.SIMPLE;
    }

    private int countSeparators(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ',' || c == ';') {
                count++;
            }
        }
        int andCount = text.split("\\band\\b", -1).length - 1;
        return count + andCount;
    }

    private boolean hasCompositeIndicator(String text) {
        return text.contains("smoothie")
                || text.contains(" with ")
                || text.contains("salad with")
                || text.contains("curry")
                || text.contains("biryani")
                || text.contains("bowl")
                || text.contains("recipe")
                || text.contains("stew")
                || text.contains("soup with")
                || text.contains("toast with")
                || text.contains("sandwich with")
                || text.contains("buffet")
                || text.contains("all you can eat")
                || text.contains("cheese board")
                || text.contains("charcuterie")
                || text.contains("pieces")
                || text.contains(" rolls")
                || text.contains("wine")
                || text.contains("whey")
                || text.contains("protein shake")
                || text.contains("scoop")
                || text.contains("post workout")
                || text.contains("post gym")
                || text.contains("bannana")
                || text.contains("scrabled");
    }
}
