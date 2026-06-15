package com.healthapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts user-stated portions from voice text (grams, ml, oz, tbsp, cups).
 * Used to build separate food items with exact weights instead of vague composites.
 */
public final class ExplicitPortionParser {

    private ExplicitPortionParser() {}

    public record ExplicitPortion(double grams, String foodName) {}

    public record ExplicitCountPortion(double quantity, String unit, String foodName) {}

    private static final Pattern EMBEDDED_WITH_QUANTITY = Pattern.compile(
            ".*\\bwith\\s+(\\d+(?:\\.\\d+)?)\\s*(grams?|g|ml|milliliters?|oz|ounces?|tablespoons?|tbsp|teaspoons?|tsp|cups?)\\s+(?:of\\s+)?(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LEADING_QUANTITY = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?)\\s*(grams?|g|ml|milliliters?|oz|ounces?|tablespoons?|tbsp|teaspoons?|tsp|cups?)\\s+(?:of\\s+)?(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRAILING_GRAMS = Pattern.compile(
            "^\\s*(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(grams?|g)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COUNT_WITH_GRAMS = Pattern.compile(
            "^\\s*(?:one|two|three|four|five|six|seven|eight|nine|ten|\\d+)\\s+(?:medium|small|large\\s+)?(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(grams?|g)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COUNT_WITH_UNIT = Pattern.compile(
            "^\\s*(?:(\\d+(?:\\.\\d+)?)|one|two|three|four|five|six|seven|eight|nine|ten|a|an)\\s+"
                    + "(glass|glasses|cup|cups|slice|slices|piece|pieces|bowl|bowls)\\s+(?:of\\s+)?(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COUNT_PIECES = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?|one|two|three|four|five|six|seven|eight|nine|ten)\\s+"
                    + "(?:(medium|small|large)\\s+)?(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Returns portions only when the user clearly stated measurable amounts.
     */
    public static List<ExplicitPortion> parse(String voiceText) {
        List<ExplicitPortion> portions = new ArrayList<>();
        if (voiceText == null || voiceText.isBlank()) {
            return portions;
        }
        for (String segment : splitSegments(voiceText)) {
            parseSegment(segment).ifPresent(portions::add);
        }
        return portions;
    }

    public static boolean hasExplicitPortions(String voiceText) {
        return !parse(voiceText).isEmpty();
    }

    /**
     * Parses count-based segments like "3 cookies" or "a glass of milk".
     */
    public static List<ExplicitCountPortion> parseCountPortions(String voiceText) {
        List<ExplicitCountPortion> portions = new ArrayList<>();
        if (voiceText == null || voiceText.isBlank()) {
            return portions;
        }
        for (String segment : splitSegments(voiceText)) {
            parseCountSegment(segment).ifPresent(portions::add);
        }
        return portions;
    }

    public static boolean hasExplicitMultiItemBreakdown(String voiceText) {
        return parse(voiceText).size() >= 2 || parseCountPortions(voiceText).size() >= 2;
    }

    static List<String> splitSegments(String voiceText) {
        List<String> segments = new ArrayList<>();
        String normalized = normalizeUnitTypos(voiceText).replaceAll("(?i)\\s*,\\s*and\\s+", ", ");
        for (String part : normalized.split("\\s*,\\s*")) {
            for (String sub : part.split("(?i)\\s+and\\s+")) {
                String trimmed = sub.trim();
                if (!trimmed.isEmpty()) {
                    segments.add(trimmed);
                }
            }
        }
        return segments;
    }

    static Optional<ExplicitPortion> parseSegment(String segment) {
        String normalized = normalizeUnitTypos(segment);
        Matcher trailing = TRAILING_GRAMS.matcher(normalized);
        if (trailing.matches()) {
            return Optional.of(new ExplicitPortion(
                    Double.parseDouble(trailing.group(2)),
                    cleanFoodName(trailing.group(1))));
        }
        Matcher countGrams = COUNT_WITH_GRAMS.matcher(normalized);
        if (countGrams.matches()) {
            return Optional.of(new ExplicitPortion(
                    Double.parseDouble(countGrams.group(2)),
                    cleanFoodName(countGrams.group(1))));
        }
        Matcher leading = LEADING_QUANTITY.matcher(normalized);
        if (leading.matches()) {
            double amount = Double.parseDouble(leading.group(1));
            String unit = leading.group(2).toLowerCase(Locale.ROOT);
            String food = cleanFoodName(leading.group(3));
            return Optional.of(new ExplicitPortion(toGrams(amount, unit, food), food));
        }
        Matcher embedded = EMBEDDED_WITH_QUANTITY.matcher(normalized);
        if (embedded.matches()) {
            double amount = Double.parseDouble(embedded.group(1));
            String unit = embedded.group(2).toLowerCase(Locale.ROOT);
            String food = cleanFoodName(embedded.group(3));
            return Optional.of(new ExplicitPortion(toGrams(amount, unit, food), food));
        }
        return Optional.empty();
    }

    static String normalizeUnitTypos(String segment) {
        if (segment == null) {
            return "";
        }
        return segment
                .replaceAll("(?i)\\btabel\\s+spoons?\\b", "tablespoon")
                .replaceAll("(?i)\\btable\\s+spoons?\\b", "tablespoon")
                .replaceAll("(?i)\\bcachenuts\\b", "cashews")
                .replaceAll("(?i)\\boasts\\b", "oats")
                .replaceAll("(?i)\\bble\\s+berries\\b", "blueberries");
    }

    static Optional<ExplicitCountPortion> parseCountSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return Optional.empty();
        }
        if (parseSegment(segment).isPresent()) {
            return Optional.empty();
        }
        Matcher withUnit = COUNT_WITH_UNIT.matcher(segment);
        if (withUnit.matches()) {
            double qty = parseCountToken(withUnit.group(1));
            String unit = normalizeCountUnit(withUnit.group(2));
            String food = cleanFoodName(withUnit.group(3));
            if (!food.isEmpty()) {
                return Optional.of(new ExplicitCountPortion(qty, unit, food));
            }
        }
        Matcher pieces = COUNT_PIECES.matcher(segment);
        if (pieces.matches()) {
            double qty = parseCountToken(pieces.group(1));
            String food = cleanFoodName(pieces.group(3));
            if (!food.isEmpty() && !isVagueCountFood(food)) {
                return Optional.of(new ExplicitCountPortion(qty, "pieces", food));
            }
        }
        return Optional.empty();
    }

    private static boolean isVagueCountFood(String food) {
        String n = food.toLowerCase(Locale.ROOT);
        return n.equals("snacks") || n.equals("snack") || n.contains("some snack");
    }

    private static String normalizeCountUnit(String unit) {
        String u = unit.toLowerCase(Locale.ROOT);
        if (u.startsWith("glass")) {
            return "glass";
        }
        if (u.startsWith("cup")) {
            return "cup";
        }
        if (u.startsWith("slice")) {
            return "slices";
        }
        if (u.startsWith("piece")) {
            return "pieces";
        }
        if (u.startsWith("bowl")) {
            return "cup";
        }
        return u;
    }

    static double parseCountToken(String token) {
        if (token == null || token.isBlank()) {
            return 1.0;
        }
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "a", "an", "one" -> 1.0;
            case "two" -> 2.0;
            case "three" -> 3.0;
            case "four" -> 4.0;
            case "five" -> 5.0;
            case "six" -> 6.0;
            case "seven" -> 7.0;
            case "eight" -> 8.0;
            case "nine" -> 9.0;
            case "ten" -> 10.0;
            default -> Double.parseDouble(token);
        };
    }

    static double toGrams(double amount, String unit, String foodName) {
        return switch (unit) {
            case "g", "gram", "grams" -> amount;
            case "ml", "milliliter", "milliliters" -> amount;
            case "oz", "ounce", "ounces" -> amount * 28.35;
            case "tbsp", "tablespoon", "tablespoons" -> amount * 15.0;
            case "tsp", "teaspoon", "teaspoons" -> amount * 5.0;
            case "cup", "cups" -> amount * cupGrams(foodName);
            default -> amount;
        };
    }

    private static double cupGrams(String foodName) {
        String n = foodName != null ? foodName.toLowerCase(Locale.ROOT) : "";
        if (n.contains("quinoa") || n.contains("rice")) {
            return 185.0;
        }
        if (n.contains("broccoli") || n.contains("vegetable")) {
            return 150.0;
        }
        return 200.0;
    }

    static String cleanFoodName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replaceAll("(?i)^(a|an|the|my|some)\\s+", "")
                .replaceAll("(?i)\\s+for\\s+(breakfast|lunch|dinner|snack)\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
