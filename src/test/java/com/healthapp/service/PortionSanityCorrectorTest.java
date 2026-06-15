package com.healthapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortionSanityCorrectorTest {

    private PortionSanityCorrector corrector;

    @BeforeEach
    void setUp() {
        corrector = new PortionSanityCorrector();
    }

    @Test
    void raisesUnderestimatedPeanutButterOnToastComposite() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("whole wheat toast with butter and peanut butter");

        AiFoodVoiceParsingService.IngredientData toast = ingredient("whole wheat toast", 60);
        AiFoodVoiceParsingService.IngredientData butter = ingredient("butter", 7);
        AiFoodVoiceParsingService.IngredientData peanut = ingredient("peanut butter", 7);
        composite.getIngredients().add(toast);
        composite.getIngredients().add(butter);
        composite.getIngredients().add(peanut);
        dataList.addCompositeMeal(composite);

        corrector.apply(dataList);

        assertEquals(32.0, peanut.getEstimatedGrams(), 0.1);
        assertEquals(10.0, butter.getEstimatedGrams(), 0.1);
        assertEquals(102.0, composite.getEstimatedGrams(), 0.1);
    }

    @Test
    void doesNotOverrideUserSpecifiedGrams() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData item = new AiFoodVoiceParsingService.ParsedFoodData();
        item.setFoodName("peanut butter");
        item.setEstimatedGrams(5.0);
        item.setUserSpecifiedGrams(true);
        dataList.addFoodItem(item);

        corrector.apply(dataList);

        assertEquals(5.0, item.getEstimatedGrams(), 0.1);
    }

    @Test
    void raisesUnderestimatedWheyProteinPowder() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("whey protein shake with banana");

        AiFoodVoiceParsingService.IngredientData powder = ingredient("whey protein powder", 10);
        AiFoodVoiceParsingService.IngredientData banana = ingredient("banana", 100);
        composite.getIngredients().add(powder);
        composite.getIngredients().add(banana);
        dataList.addCompositeMeal(composite);

        corrector.apply(dataList);

        assertEquals(30.0, powder.getEstimatedGrams(), 0.1);
    }

    private static AiFoodVoiceParsingService.IngredientData ingredient(String name, double grams) {
        AiFoodVoiceParsingService.IngredientData data = new AiFoodVoiceParsingService.IngredientData();
        data.setName(name);
        data.setEstimatedGrams(grams);
        return data;
    }
}
