package com.healthapp.service;

import java.util.Locale;
import java.util.Map;

/**
 * Lightweight typo correction for common voice-food phrases before LLM parsing.
 */
public final class FoodVoiceTypoNormalizer {

    private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
            Map.entry("bannana", "banana"),
            Map.entry("bananna", "banana"),
            Map.entry("scrabled", "scrambled"),
            Map.entry("scrambeled", "scrambled"),
            Map.entry("brekfast", "breakfast"),
            Map.entry("chiken", "chicken"),
            Map.entry("peanu butter", "peanut butter"),
            Map.entry("peanut buter", "peanut butter"),
            Map.entry("tabel spoon", "tablespoon"),
            Map.entry("tabel spoons", "tablespoons"),
            Map.entry("cachenuts", "cashews"),
            Map.entry("oasts", "oats"),
            Map.entry("ble berries", "blueberries")
    );

    private FoodVoiceTypoNormalizer() {}

    public static String normalize(String voiceText) {
        if (voiceText == null || voiceText.isBlank()) {
            return voiceText;
        }
        String result = voiceText;
        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            result = replaceIgnoreCase(result, entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        String lower = text.toLowerCase(Locale.ROOT);
        String targetLower = target.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        int from = 0;
        int idx;
        while ((idx = lower.indexOf(targetLower, from)) >= 0) {
            sb.append(text, from, idx).append(replacement);
            from = idx + target.length();
        }
        sb.append(text.substring(from));
        return sb.toString();
    }
}
