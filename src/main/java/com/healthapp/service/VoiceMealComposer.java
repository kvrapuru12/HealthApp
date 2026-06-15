package com.healthapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Voice-first post-parse rules: split drinks, merge same-occasion plate items into composites.
 */
@Component
public class VoiceMealComposer {

    private static final Logger logger = LoggerFactory.getLogger(VoiceMealComposer.class);

    private final PortionGramEstimator portionGramEstimator;

    @Autowired
    public VoiceMealComposer(PortionGramEstimator portionGramEstimator) {
        this.portionGramEstimator = portionGramEstimator;
    }

    private static final Pattern MEAL_OCCASION = Pattern.compile(
            "\\b(for\\s+)?(breakfast|lunch|dinner)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LATER_SEGMENT = Pattern.compile(
            "\\b(then|later|after that|also had|second meal)\\b", Pattern.CASE_INSENSITIVE);

    public void applyVoiceMealRules(AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        if (dataList == null || voiceText == null) {
            return;
        }
        splitBeveragesFromFoodItems(dataList);
        promoteCompoundFoodItemsToComposites(dataList);
        maybeMergePlateItems(dataList, voiceText);
        ensureCompositeIngredients(dataList);
    }

    /**
     * Single parsed rows like "latte with muffin" become composites with per-ingredient USDA blending.
     */
    private void promoteCompoundFoodItemsToComposites(AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        List<AiFoodVoiceParsingService.ParsedFoodData> candidates = new ArrayList<>(dataList.getFoodItems());
        for (AiFoodVoiceParsingService.ParsedFoodData item : candidates) {
            if (isBeverage(item.getFoodName())) {
                continue;
            }
            List<String> parts = splitCompoundFoodName(item.getFoodName());
            if (parts.size() < 2) {
                continue;
            }
            AiFoodVoiceParsingService.ParsedFoodData composite = buildCompositeFromParts(item, parts);
            dataList.getFoodItems().remove(item);
            dataList.getCompositeMeals().add(composite);
            logger.info("Promoted compound food item '{}' to composite ({} ingredients)", item.getFoodName(), parts.size());
        }
    }

    private AiFoodVoiceParsingService.ParsedFoodData buildCompositeFromParts(
            AiFoodVoiceParsingService.ParsedFoodData source, List<String> parts) {
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName(source.getFoodName());
        composite.setMealType(source.getMealType());
        composite.setLoggedAt(source.getLoggedAt());
        composite.setNote(source.getNote());
        composite.setUnit("grams");
        double totalGrams = 0;
        for (String part : parts) {
            double grams = minimumGramsForIngredient(part.trim());
            AiFoodVoiceParsingService.IngredientData ing = new AiFoodVoiceParsingService.IngredientData();
            ing.setName(part.trim());
            ing.setEstimatedGrams(grams);
            ing.setFdcSearchTerm(part.trim());
            composite.getIngredients().add(ing);
            totalGrams += grams;
        }
        composite.setQuantity(totalGrams);
        composite.setEstimatedGrams(totalGrams);
        return composite;
    }

    private static final Pattern BEVERAGE_WORD = Pattern.compile(
            "\\b(lassi|smoothie|milkshake|juice|soda|cola|coffee|latte|espresso|beer|wine|cocktail)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TEA_WORD = Pattern.compile("\\btea\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MILK_WORD = Pattern.compile("\\bmilk\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHAKE_WORD = Pattern.compile("\\bshake\\b", Pattern.CASE_INSENSITIVE);

    static boolean isBeverage(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase(Locale.ROOT);
        if (BEVERAGE_WORD.matcher(n).find()) {
            return true;
        }
        if (TEA_WORD.matcher(n).find() && !n.contains("steak")) {
            return true;
        }
        if (MILK_WORD.matcher(n).find() && !n.contains("milkshake")) {
            return true;
        }
        return SHAKE_WORD.matcher(n).find() && !n.contains("steak") && !n.contains("milkshake");
    }

    private void splitBeveragesFromFoodItems(AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = dataList.getFoodItems();
        if (items.size() < 2) {
            return;
        }
        List<AiFoodVoiceParsingService.ParsedFoodData> plate = new ArrayList<>();
        List<AiFoodVoiceParsingService.ParsedFoodData> drinks = new ArrayList<>();
        for (AiFoodVoiceParsingService.ParsedFoodData item : items) {
            if (isBeverage(item.getFoodName())) {
                drinks.add(item);
            } else {
                plate.add(item);
            }
        }
        if (drinks.isEmpty() || plate.size() < 2) {
            return;
        }
        dataList.getFoodItems().clear();
        dataList.getFoodItems().addAll(plate);
        dataList.getFoodItems().addAll(drinks);
        logger.info("Split {} beverage(s) from plate items for separate logging", drinks.size());
    }

    private void maybeMergePlateItems(AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = new ArrayList<>();
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getFoodItems()) {
            if (!isBeverage(item.getFoodName())) {
                items.add(item);
            }
        }
        if (items.size() < 2 || !shouldMergePlate(voiceText, items)) {
            return;
        }
        AiFoodVoiceParsingService.ParsedFoodData composite = buildComposite(items, voiceText);
        dataList.getCompositeMeals().add(composite);
        dataList.getFoodItems().removeIf(i -> !isBeverage(i.getFoodName()));
        logger.info("Voice plate merge: {} items -> composite '{}'", items.size(), composite.getFoodName());
    }

    boolean shouldMergePlate(String voiceText, List<AiFoodVoiceParsingService.ParsedFoodData> plateItems) {
        if (voiceText == null || plateItems.size() < 2) {
            return false;
        }
        if (plateItems.stream().anyMatch(i -> i.getMealType() == null)) {
            return false;
        }
        String mealType = plateItems.get(0).getMealType();
        if (plateItems.stream().anyMatch(i -> !mealType.equalsIgnoreCase(i.getMealType()))) {
            return false;
        }
        if (LATER_SEGMENT.matcher(voiceText).find()) {
            return false;
        }
        String lower = voiceText.toLowerCase(Locale.ROOT);
        return lower.contains(" with ") || MEAL_OCCASION.matcher(lower).find();
    }

    private AiFoodVoiceParsingService.ParsedFoodData buildComposite(
            List<AiFoodVoiceParsingService.ParsedFoodData> items, String voiceText) {
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName(buildPlateName(items, voiceText));
        composite.setMealType(items.get(0).getMealType());
        composite.setLoggedAt(items.get(0).getLoggedAt());
        composite.setNote(items.get(0).getNote());
        composite.setUnit("grams");

        double totalGrams = 0;
        List<AiFoodVoiceParsingService.IngredientData> ingredients = new ArrayList<>();
        for (AiFoodVoiceParsingService.ParsedFoodData item : items) {
            double grams = resolveIngredientGrams(item);
            totalGrams += grams;
            AiFoodVoiceParsingService.IngredientData ing = new AiFoodVoiceParsingService.IngredientData();
            ing.setName(item.getFoodName());
            ing.setEstimatedGrams(grams);
            ing.setFdcSearchTerm(item.getFoodName());
            ingredients.add(ing);
        }
        composite.setQuantity(totalGrams);
        composite.setEstimatedGrams(totalGrams);
        composite.setIngredients(ingredients);
        return composite;
    }

    private String buildPlateName(List<AiFoodVoiceParsingService.ParsedFoodData> items, String voiceText) {
        if (items.size() == 1) {
            return items.get(0).getFoodName();
        }
        String lower = voiceText != null ? voiceText.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("breakfast")) {
            return "Breakfast plate";
        }
        if (lower.contains("lunch")) {
            return "Lunch plate";
        }
        if (lower.contains("dinner")) {
            return "Dinner plate";
        }
        return items.get(0).getFoodName() + " plate";
    }

    private void ensureCompositeIngredients(AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        for (AiFoodVoiceParsingService.ParsedFoodData composite : dataList.getCompositeMeals()) {
            if (composite.getIngredients() != null && !composite.getIngredients().isEmpty()) {
                continue;
            }
            if (composite.getEstimatedGrams() == null || composite.getEstimatedGrams() <= 0) {
                composite.setEstimatedGrams(300.0);
                composite.setQuantity(300.0);
            }
            List<String> parts = splitCompoundFoodName(composite.getFoodName());
            if (parts.size() >= 2) {
                double totalGrams = 0;
                List<AiFoodVoiceParsingService.IngredientData> splitIngredients = new ArrayList<>();
                for (String part : parts) {
                    double grams = minimumGramsForIngredient(part.trim());
                    AiFoodVoiceParsingService.IngredientData ing = new AiFoodVoiceParsingService.IngredientData();
                    ing.setName(part.trim());
                    ing.setEstimatedGrams(grams);
                    ing.setFdcSearchTerm(part.trim());
                    splitIngredients.add(ing);
                    totalGrams += grams;
                }
                composite.setEstimatedGrams(totalGrams);
                composite.setQuantity(totalGrams);
                composite.getIngredients().addAll(splitIngredients);
                logger.info("Split compound name '{}' into {} ingredients for blending ({} g total)",
                        composite.getFoodName(), parts.size(), totalGrams);
                continue;
            }
            AiFoodVoiceParsingService.IngredientData ing = new AiFoodVoiceParsingService.IngredientData();
            ing.setName(composite.getFoodName());
            ing.setEstimatedGrams(composite.getEstimatedGrams());
            ing.setFdcSearchTerm(composite.getFoodName());
            composite.getIngredients().add(ing);
        }
    }

    private double resolveIngredientGrams(AiFoodVoiceParsingService.ParsedFoodData item) {
        double grams = portionGramEstimator.resolveEffectiveGrams(
                item.getFoodName(),
                item.getQuantity(),
                item.getUnit(),
                item.getEstimatedGrams());
        return portionGramEstimator.applyMinimumPlausibleGrams(item.getFoodName(), grams);
    }

    static double minimumGramsForIngredient(String part) {
        return RecommendedPortionCatalog.ingredientPortionGrams(part);
    }

    static List<String> splitCompoundFoodName(String foodName) {
        List<String> parts = new ArrayList<>();
        if (foodName == null || foodName.isBlank()) {
            return parts;
        }
        String lower = foodName.toLowerCase(Locale.ROOT);
        boolean hasWith = lower.contains(" with ");
        int andSegments = lower.split("\\band\\b", -1).length;
        if (!hasWith && andSegments < 2) {
            return parts;
        }
        for (String segment : foodName.split("(?i)\\s+with\\s+|\\s+and\\s+")) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts.size() >= 2 ? parts : List.of();
    }
}
