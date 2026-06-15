package com.healthapp.service.nutrition;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Token-based cooking-oil detection. Avoids false positives from substrings like "oil" in "boiled".
 */
public final class CookingOilDetector {

    private static final Set<String> EXPLICIT_OIL_PHRASES = Set.of(
            "olive oil", "avocado oil", "sesame oil", "coconut oil", "vegetable oil",
            "canola oil", "sunflower oil", "cooking oil", "oil drizzle", "grapeseed oil",
            "peanut oil", "corn oil", "flaxseed oil", "walnut oil"
    );

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,;]+");

    private CookingOilDetector() {}

    public static boolean containsCookingOil(String foodName) {
        if (foodName == null || foodName.isBlank()) {
            return false;
        }
        String n = foodName.toLowerCase(Locale.ROOT).trim();
        if (n.contains("broccoli") || n.contains("boiled") || n.contains("spoiled")
                || n.contains("oily fish") || n.contains("fish oil")) {
            return false;
        }
        for (String phrase : EXPLICIT_OIL_PHRASES) {
            if (n.contains(phrase)) {
                return true;
            }
        }
        if (n.endsWith(" oil") || n.equals("oil")) {
            return true;
        }
        String[] tokens = TOKEN_SPLIT.split(n);
        for (String token : tokens) {
            if ("oil".equals(token)) {
                return true;
            }
        }
        return false;
    }
}
