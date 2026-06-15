package com.healthapp.service;

import com.healthapp.service.nutrition.NutritionConfidence;
import com.healthapp.service.nutrition.NutritionSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplicitMacroApplierTest {

    private ExplicitMacroApplier applier;

    @BeforeEach
    void setUp() {
        applier = new ExplicitMacroApplier();
    }

    @Test
    void appliesStatedMacrosToSingleItem() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData muffin = new AiFoodVoiceParsingService.ParsedFoodData();
        muffin.setFoodName("blueberry muffin");
        muffin.setEstimatedGrams(110.0);
        dataList.addCompositeMeal(muffin);

        applier.apply(dataList, "blueberry muffin with 300cal , P=13, C=79, F=30");

        AiFoodVoiceParsingService.ParsedFoodData result = dataList.getCompositeMeals().get(0);
        assertTrue(result.isUserSpecifiedMacros());
        assertEquals(NutritionSource.USER_STATED, result.getNutritionSource());
        assertEquals(NutritionConfidence.MEDIUM, result.getNutritionConfidence());
        assertEquals(300.0, result.getNutrition().getCaloriesPer100g() * 110.0 / 100.0, 1.0);
        assertEquals(13.0, result.getNutrition().getProteinPer100g() * 110.0 / 100.0, 0.2);
        assertEquals(79.0, result.getNutrition().getCarbsPer100g() * 110.0 / 100.0, 0.2);
        assertEquals(30.0, result.getNutrition().getFatPer100g() * 110.0 / 100.0, 0.2);
    }

    @Test
    void ignoresWhenMultipleSeparateItems() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        dataList.addFoodItem(new AiFoodVoiceParsingService.ParsedFoodData());
        dataList.addFoodItem(new AiFoodVoiceParsingService.ParsedFoodData());

        applier.apply(dataList, "latte 300cal P=13 C=79 F=30");

        assertFalse(dataList.getFoodItems().get(0).isUserSpecifiedMacros());
    }

    @Test
    void appliesToCompositeWhenSeparateItemsAlsoParsed() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("chicken and rice plate");
        composite.setEstimatedGrams(400.0);
        dataList.addCompositeMeal(composite);
        dataList.addFoodItem(new AiFoodVoiceParsingService.ParsedFoodData());

        applier.apply(dataList, "chicken and rice plate 500cal P=40 C=50 F=10");

        assertTrue(composite.isUserSpecifiedMacros());
        assertEquals(NutritionSource.USER_STATED, composite.getNutritionSource());
    }
}
