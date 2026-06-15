package com.healthapp.service;

import com.healthapp.service.nutrition.NutritionConfidence;
import com.healthapp.service.nutrition.NutritionSource;
import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * When users state total calories/macros in voice, override resolved nutrition for the target item.
 */
@Component
public class ExplicitMacroApplier {

    private static final Logger logger = LoggerFactory.getLogger(ExplicitMacroApplier.class);

    public void apply(AiFoodVoiceParsingService.ParsedFoodDataList dataList, String voiceText) {
        Optional<ExplicitMacroParser.StatedMacros> macrosOpt = ExplicitMacroParser.parse(voiceText);
        if (macrosOpt.isEmpty() || !ExplicitMacroParser.isPlausible(macrosOpt.get())) {
            return;
        }
        ExplicitMacroParser.StatedMacros macros = macrosOpt.get();
        List<AiFoodVoiceParsingService.ParsedFoodData> targets = collectTargets(dataList);
        if (targets.size() == 1) {
            applyToItem(targets.get(0), macros);
            logger.info("User-stated macros applied to '{}': {}",
                    targets.get(0).getFoodName(), ExplicitMacroParser.formatSummary(macros));
        } else if (dataList.getCompositeMeals().size() == 1) {
            AiFoodVoiceParsingService.ParsedFoodData composite = dataList.getCompositeMeals().get(0);
            applyToItem(composite, macros);
            logger.info("User-stated macros applied to composite '{}' ({} total parsed items): {}",
                    composite.getFoodName(), targets.size(), ExplicitMacroParser.formatSummary(macros));
        } else {
            logger.info("Stated macros ignored — ambiguous target among {} items", targets.size());
        }
    }

    private List<AiFoodVoiceParsingService.ParsedFoodData> collectTargets(
            AiFoodVoiceParsingService.ParsedFoodDataList dataList) {
        List<AiFoodVoiceParsingService.ParsedFoodData> targets = new ArrayList<>();
        targets.addAll(dataList.getCompositeMeals());
        targets.addAll(dataList.getFoodItems());
        return targets;
    }

    private void applyToItem(AiFoodVoiceParsingService.ParsedFoodData item,
                             ExplicitMacroParser.StatedMacros macros) {
        double grams = resolveServingGrams(item);
        item.setEstimatedGrams(grams);
        item.setQuantity(grams);
        item.setUnit("grams");

        double scale100 = 100.0 / grams;
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(macros.calories() * scale100);
        nutrition.setProteinPer100g(macros.protein() != null ? macros.protein() * scale100 : 0.0);
        nutrition.setCarbsPer100g(macros.carbs() != null ? macros.carbs() * scale100 : 0.0);
        nutrition.setFatPer100g(macros.fat() != null ? macros.fat() * scale100 : 0.0);
        nutrition.setFiberPer100g(macros.fiber() != null ? macros.fiber() * scale100 : 0.0);

        item.setNutrition(nutrition);
        item.setNutritionSource(NutritionSource.USER_STATED);
        item.setNutritionConfidence(NutritionConfidence.MEDIUM);
        item.setFdcId(null);
        item.setUserSpecifiedMacros(true);
        item.getIngredients().clear();

        String statedNote = "Stated nutrition from voice: " + ExplicitMacroParser.formatSummary(macros) + ".";
        if (item.getNote() == null || item.getNote().isBlank()) {
            item.setNote(statedNote);
        } else if (!item.getNote().contains("Stated nutrition from voice")) {
            item.setNote(item.getNote() + " " + statedNote);
        }
    }

    private double resolveServingGrams(AiFoodVoiceParsingService.ParsedFoodData item) {
        if (item.isUserSpecifiedGrams() && item.getEstimatedGrams() != null && item.getEstimatedGrams() > 0) {
            return item.getEstimatedGrams();
        }
        if (item.getEstimatedGrams() != null && item.getEstimatedGrams() >= 5) {
            return item.getEstimatedGrams();
        }
        return RecommendedPortionCatalog.ingredientPortionGrams(item.getFoodName(), item.getFoodName());
    }
}
