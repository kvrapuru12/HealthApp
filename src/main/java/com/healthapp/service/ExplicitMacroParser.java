package com.healthapp.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts user-stated total macros from voice text (label-style: "300 cal, P=13, C=79, F=30").
 */
public final class ExplicitMacroParser {

    private ExplicitMacroParser() {}

    public record StatedMacros(double calories, Double protein, Double carbs, Double fat, Double fiber) {
        public boolean hasMacrosBesidesCalories() {
            return protein != null || carbs != null || fat != null || fiber != null;
        }
    }

    private static final Pattern CALORIES = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:k?cal(?:ories)?|cals)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROTEIN = Pattern.compile(
            "(?:\\bP\\s*[=:]?\\s*|\\bprotein\\s+)(\\d+(?:\\.\\d+)?)\\s*g?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROTEIN_SUFFIX = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*g?\\s*protein\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern CARBS = Pattern.compile(
            "(?:\\bC\\s*[=:]?\\s*|\\bcarbs?\\s+)(\\d+(?:\\.\\d+)?)\\s*g?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern CARBS_SUFFIX = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*g?\\s*carbs?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FAT = Pattern.compile(
            "(?:\\bF\\s*[=:]?\\s*|\\bfat\\s+)(\\d+(?:\\.\\d+)?)\\s*g?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FAT_SUFFIX = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*g?\\s*fat\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FIBER = Pattern.compile(
            "(?:\\bfiber\\s+)(\\d+(?:\\.\\d+)?)\\s*g?\\b", Pattern.CASE_INSENSITIVE);

    public static Optional<StatedMacros> parse(String voiceText) {
        if (voiceText == null || voiceText.isBlank()) {
            return Optional.empty();
        }
        String text = voiceText.trim();
        Optional<Double> calories = parseCaloriesAvoidingBurnContext(text);
        if (calories.isEmpty() || calories.get() < 1) {
            return Optional.empty();
        }
        Double protein = parseMacro(PROTEIN, PROTEIN_SUFFIX, text).orElse(null);
        Double carbs = parseMacro(CARBS, CARBS_SUFFIX, text).orElse(null);
        Double fat = parseMacro(FAT, FAT_SUFFIX, text).orElse(null);
        Double fiber = parseFirstDouble(FIBER, text).orElse(null);
        return Optional.of(new StatedMacros(calories.get(), protein, carbs, fat, fiber));
    }

    public static boolean isPlausible(StatedMacros macros) {
        if (macros == null || macros.calories() < 1 || macros.calories() > 5000) {
            return false;
        }
        if (macros.protein() != null && (macros.protein() < 0 || macros.protein() > 300)) {
            return false;
        }
        if (macros.carbs() != null && (macros.carbs() < 0 || macros.carbs() > 500)) {
            return false;
        }
        if (macros.fat() != null && (macros.fat() < 0 || macros.fat() > 300)) {
            return false;
        }
        if (macros.fiber() != null && (macros.fiber() < 0 || macros.fiber() > 100)) {
            return false;
        }
        return true;
    }

    private static Optional<Double> parseMacro(Pattern leading, Pattern suffix, String text) {
        Optional<Double> value = parseFirstDouble(leading, text);
        return value.isPresent() ? value : parseFirstDouble(suffix, text);
    }

    private static Optional<Double> parseFirstDouble(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(Double.parseDouble(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static Optional<Double> parseCaloriesAvoidingBurnContext(String text) {
        Matcher matcher = CALORIES.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            String prefix = text.substring(Math.max(0, start - 30), start).toLowerCase(Locale.ROOT);
            if (prefix.matches("(?s).*\\b(burn(?:ed|t)?|burning)\\s*$")) {
                continue;
            }
            return Optional.of(Double.parseDouble(matcher.group(1)));
        }
        return Optional.empty();
    }

    static String formatSummary(StatedMacros macros) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%.0f kcal", macros.calories()));
        if (macros.protein() != null) {
            sb.append(String.format(Locale.ROOT, ", P=%.0fg", macros.protein()));
        }
        if (macros.carbs() != null) {
            sb.append(String.format(Locale.ROOT, ", C=%.0fg", macros.carbs()));
        }
        if (macros.fat() != null) {
            sb.append(String.format(Locale.ROOT, ", F=%.0fg", macros.fat()));
        }
        if (macros.fiber() != null) {
            sb.append(String.format(Locale.ROOT, ", fiber=%.0fg", macros.fiber()));
        }
        return sb.toString();
    }
}
