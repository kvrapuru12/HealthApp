package com.healthapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * When users state explicit grams/ml/oz per item, replace vague composites with separate food rows.
 */
@Component
public class ExplicitQuantityApplier {

    private static final Logger logger = LoggerFactory.getLogger(ExplicitQuantityApplier.class);

    private final PortionGramEstimator portionGramEstimator;

    public ExplicitQuantityApplier(PortionGramEstimator portionGramEstimator) {
        this.portionGramEstimator = portionGramEstimator;
    }

    /**
     * If voice text contains 2+ explicit portions, rebuild the parse result as separate food items.
     * Single explicit portion enriches the lone item when present.
     */
    public void apply(AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        List<ExplicitPortionParser.ExplicitPortion> portions = ExplicitPortionParser.parse(voiceText);
        List<ExplicitPortionParser.ExplicitCountPortion> countPortions =
                ExplicitPortionParser.parseCountPortions(voiceText);

        if (portions.isEmpty() && countPortions.isEmpty()) {
            if (shouldSplitCompositeIngredients(dataList, voiceText)) {
                AiFoodVoiceParsingService.ParsedFoodData composite = dataList.getCompositeMeals().get(0);
                rebuildFromCompositeIngredients(dataList, composite, voiceText);
                logger.info("Explicit quantities: split composite '{}' into {} separate food item(s) from AI ingredients",
                        composite.getFoodName(), dataList.getFoodItems().size());
            }
            return;
        }

        if (portions.size() + countPortions.size() >= 2) {
            rebuildAsSeparateItems(dataList, portions, countPortions, voiceText);
            logger.info("Explicit quantities: rebuilt {} separate food item(s) from voice text",
                    portions.size() + countPortions.size());
            return;
        }

        if (shouldSplitCompositeIngredients(dataList, voiceText)) {
            AiFoodVoiceParsingService.ParsedFoodData composite = dataList.getCompositeMeals().get(0);
            rebuildFromCompositeIngredients(dataList, composite, voiceText);
            applyExplicitPortionsToItems(dataList, portions);
            logger.info("Explicit quantities: split composite '{}' into {} separate food item(s) from AI ingredients",
                    composite.getFoodName(), dataList.getFoodItems().size());
            return;
        }

        if (!countPortions.isEmpty()) {
            ExplicitPortionParser.ExplicitCountPortion count = countPortions.get(0);
            if (dataList.getFoodItems().size() == 1 && dataList.getCompositeMeals().isEmpty()) {
                AiFoodVoiceParsingService.ParsedFoodData item = dataList.getFoodItems().get(0);
                if (shouldSkipCountOverride(item)) {
                    return;
                }
                applyCountToItem(item, count);
                logger.info("Explicit count applied to single item: {} {} {}",
                        count.quantity(), count.unit(), count.foodName());
                return;
            }
            if (dataList.getCompositeMeals().size() == 1 && dataList.getFoodItems().isEmpty()) {
                return;
            }
        }

        if (portions.isEmpty()) {
            return;
        }

        ExplicitPortionParser.ExplicitPortion single = portions.get(0);
        if (dataList.getFoodItems().size() == 1 && dataList.getCompositeMeals().isEmpty()) {
            AiFoodVoiceParsingService.ParsedFoodData item = dataList.getFoodItems().get(0);
            applyPortionToItem(item, single);
            logger.info("Explicit quantity applied to single item: {}g {}", single.grams(), single.foodName());
            return;
        }

        if (dataList.getCompositeMeals().size() == 1 && dataList.getFoodItems().isEmpty()) {
            AiFoodVoiceParsingService.ParsedFoodData composite = dataList.getCompositeMeals().get(0);
            if (composite.getIngredients() != null && composite.getIngredients().size() >= 2) {
                rebuildFromCompositeIngredients(dataList, composite, voiceText);
                applyExplicitPortionsToItems(dataList, portions);
                logger.info("Explicit quantities: split multi-ingredient composite '{}' into {} food item(s)",
                        composite.getFoodName(), dataList.getFoodItems().size());
                return;
            }
            applyPortionToItem(composite, single);
            enrichCompositeIngredients(composite, portions);
            logger.info("Explicit quantity applied to composite: {}g {}", single.grams(), single.foodName());
            return;
        }

        if (dataList.getFoodItems().isEmpty() && dataList.getCompositeMeals().isEmpty()) {
            dataList.addFoodItem(buildItem(single, voiceText));
        }
    }

    private boolean shouldSplitCompositeIngredients(
            AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        if (dataList.getCompositeMeals().size() != 1 || !dataList.getFoodItems().isEmpty()) {
            return false;
        }
        AiFoodVoiceParsingService.ParsedFoodData composite = dataList.getCompositeMeals().get(0);
        if (composite.getIngredients() == null || composite.getIngredients().size() < 2) {
            return false;
        }
        if (shouldKeepCompositePlate(voiceText)) {
            return false;
        }
        return ExplicitPortionParser.hasExplicitMultiItemBreakdown(voiceText)
                || hasListedMultiItemVoice(voiceText);
    }

    private static boolean shouldKeepCompositePlate(String voiceText) {
        if (voiceText == null || voiceText.isBlank()) {
            return false;
        }
        String lower = voiceText.toLowerCase(Locale.ROOT);
        return lower.contains("thali") || lower.contains("smoothie") || lower.contains("milkshake");
    }

    private static boolean hasListedMultiItemVoice(String voiceText) {
        if (voiceText == null || voiceText.isBlank()) {
            return false;
        }
        List<String> segments = ExplicitPortionParser.splitSegments(voiceText);
        return segments.size() >= 2;
    }

    private void applyExplicitPortionsToItems(
            AiFoodVoiceParsingService.ParsedFoodDataList dataList,
            List<ExplicitPortionParser.ExplicitPortion> portions) {
        if (portions.isEmpty()) {
            return;
        }
        for (ExplicitPortionParser.ExplicitPortion portion : portions) {
            String target = portion.foodName().toLowerCase(Locale.ROOT);
            for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getFoodItems()) {
                String name = item.getFoodName() != null ? item.getFoodName().toLowerCase(Locale.ROOT) : "";
                if (name.contains(target) || target.contains(name)) {
                    applyPortionToItem(item, portion);
                    logger.info("Explicit quantity applied to split item: {}g {}", portion.grams(), portion.foodName());
                    break;
                }
            }
        }
    }

    private void rebuildFromCompositeIngredients(
            AiFoodVoiceParsingService.ParsedFoodDataList dataList,
            AiFoodVoiceParsingService.ParsedFoodData composite,
            String voiceText) {
        String mealType = inferMealType(dataList, voiceText);
        LocalDateTime loggedAt = inferLoggedAt(dataList);
        dataList.getCompositeMeals().clear();
        dataList.getFoodItems().clear();
        for (AiFoodVoiceParsingService.IngredientData ingredient : composite.getIngredients()) {
            AiFoodVoiceParsingService.ParsedFoodData item = new AiFoodVoiceParsingService.ParsedFoodData();
            item.setFoodName(capitalize(ingredient.getName()));
            item.setQuantity(ingredient.getEstimatedGrams());
            item.setUnit("grams");
            item.setEstimatedGrams(ingredient.getEstimatedGrams());
            item.setUserSpecifiedGrams(true);
            item.setMealType(mealType);
            item.setLoggedAt(loggedAt);
            item.setNote("Stated: " + ingredient.getEstimatedGrams() + "g " + ingredient.getName() + " from voice.");
            dataList.addFoodItem(item);
        }
    }

    private void rebuildAsSeparateItems(
            AiFoodVoiceParsingService.ParsedFoodDataList dataList,
            List<ExplicitPortionParser.ExplicitPortion> portions,
            List<ExplicitPortionParser.ExplicitCountPortion> countPortions,
            String voiceText) {
        String mealType = inferMealType(dataList, voiceText);
        LocalDateTime loggedAt = inferLoggedAt(dataList);
        dataList.getCompositeMeals().clear();
        dataList.getFoodItems().clear();
        for (ExplicitPortionParser.ExplicitPortion portion : portions) {
            AiFoodVoiceParsingService.ParsedFoodData item = buildItem(portion, voiceText);
            item.setMealType(mealType);
            item.setLoggedAt(loggedAt);
            dataList.addFoodItem(item);
        }
        for (ExplicitPortionParser.ExplicitCountPortion portion : countPortions) {
            AiFoodVoiceParsingService.ParsedFoodData item = buildCountItem(portion, voiceText);
            item.setMealType(mealType);
            item.setLoggedAt(loggedAt);
            dataList.addFoodItem(item);
        }
    }

    private AiFoodVoiceParsingService.ParsedFoodData buildItem(
            ExplicitPortionParser.ExplicitPortion portion, String voiceText) {
        AiFoodVoiceParsingService.ParsedFoodData item = new AiFoodVoiceParsingService.ParsedFoodData();
        item.setFoodName(capitalize(portion.foodName()));
        item.setQuantity(portion.grams());
        item.setUnit("grams");
        item.setEstimatedGrams(portion.grams());
        item.setUserSpecifiedGrams(true);
        item.setMealType(inferMealTypeFromVoice(voiceText));
        item.setLoggedAt(LocalDateTime.now());
        item.setNote("Stated: " + portion.grams() + "g " + portion.foodName() + " from voice.");
        return item;
    }

    private AiFoodVoiceParsingService.ParsedFoodData buildCountItem(
            ExplicitPortionParser.ExplicitCountPortion portion, String voiceText) {
        AiFoodVoiceParsingService.ParsedFoodData item = new AiFoodVoiceParsingService.ParsedFoodData();
        applyCountToItem(item, portion);
        item.setMealType(inferMealTypeFromVoice(voiceText));
        item.setLoggedAt(LocalDateTime.now());
        item.setNote("Stated: " + portion.quantity() + " " + portion.unit() + " " + portion.foodName() + " from voice.");
        return item;
    }

    private void applyCountToItem(
            AiFoodVoiceParsingService.ParsedFoodData item,
            ExplicitPortionParser.ExplicitCountPortion portion) {
        String foodName = capitalize(portion.foodName());
        item.setFoodName(foodName);
        item.setQuantity(portion.quantity());
        item.setUnit(portion.unit());
        double grams = portionGramEstimator.resolveEffectiveGrams(
                foodName, portion.quantity(), portion.unit(), null);
        item.setEstimatedGrams(grams);
        item.getIngredients().clear();
    }

    private void applyPortionToItem(
            AiFoodVoiceParsingService.ParsedFoodData item,
            ExplicitPortionParser.ExplicitPortion portion) {
        item.setFoodName(capitalize(portion.foodName()));
        item.setQuantity(portion.grams());
        item.setUnit("grams");
        item.setEstimatedGrams(portion.grams());
        item.setUserSpecifiedGrams(true);
        item.getIngredients().clear();
    }

    private void enrichCompositeIngredients(
            AiFoodVoiceParsingService.ParsedFoodData composite,
            List<ExplicitPortionParser.ExplicitPortion> portions) {
        composite.getIngredients().clear();
        double total = 0;
        for (ExplicitPortionParser.ExplicitPortion portion : portions) {
            AiFoodVoiceParsingService.IngredientData ing = new AiFoodVoiceParsingService.IngredientData();
            ing.setName(portion.foodName());
            ing.setEstimatedGrams(portion.grams());
            ing.setFdcSearchTerm(portion.foodName());
            composite.getIngredients().add(ing);
            total += portion.grams();
        }
        composite.setEstimatedGrams(total);
        composite.setQuantity(total);
        composite.setUserSpecifiedGrams(true);
    }

    private String inferMealType(AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getFoodItems()) {
            if (item.getMealType() != null) {
                return item.getMealType();
            }
        }
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getCompositeMeals()) {
            if (item.getMealType() != null) {
                return item.getMealType();
            }
        }
        return inferMealTypeFromVoice(voiceText);
    }

    private LocalDateTime inferLoggedAt(AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getFoodItems()) {
            if (item.getLoggedAt() != null) {
                return item.getLoggedAt();
            }
        }
        for (AiFoodVoiceParsingService.ParsedFoodData item : dataList.getCompositeMeals()) {
            if (item.getLoggedAt() != null) {
                return item.getLoggedAt();
            }
        }
        return LocalDateTime.now();
    }

    private static String inferMealTypeFromVoice(String voiceText) {
        if (voiceText == null) {
            return "snack";
        }
        String lower = voiceText.toLowerCase();
        if (lower.contains("breakfast")) {
            return "breakfast";
        }
        if (lower.contains("lunch")) {
            return "lunch";
        }
        if (lower.contains("dinner")) {
            return "dinner";
        }
        if (lower.contains("snack")) {
            return "snack";
        }
        return "snack";
    }

    private static boolean shouldSkipCountOverride(AiFoodVoiceParsingService.ParsedFoodData item) {
        if (item.getEstimatedGrams() == null || item.getEstimatedGrams() <= 0) {
            return false;
        }
        String unit = item.getUnit();
        return unit != null && !unit.isBlank() && !"grams".equalsIgnoreCase(unit) && !"g".equalsIgnoreCase(unit);
    }

    private static String capitalize(String food) {
        if (food == null || food.isBlank()) {
            return food;
        }
        return food.substring(0, 1).toUpperCase() + food.substring(1);
    }
}
