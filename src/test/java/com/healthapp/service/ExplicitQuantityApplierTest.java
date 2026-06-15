package com.healthapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExplicitQuantityApplierTest {

    @Mock
    private PortionGramEstimator portionGramEstimator;

    private ExplicitQuantityApplier applier;

    @BeforeEach
    void setUp() {
        applier = new ExplicitQuantityApplier(portionGramEstimator);
    }

    @Test
    void splitsCompositeWithExplicitMultiItemVoice() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("breakfast smoothie");
        composite.setEstimatedGrams(510.0);
        AiFoodVoiceParsingService.IngredientData cashews = new AiFoodVoiceParsingService.IngredientData();
        cashews.setName("cashews");
        cashews.setEstimatedGrams(10.0);
        AiFoodVoiceParsingService.IngredientData oats = new AiFoodVoiceParsingService.IngredientData();
        oats.setName("oats");
        oats.setEstimatedGrams(50.0);
        composite.getIngredients().add(cashews);
        composite.getIngredients().add(oats);
        dataList.addCompositeMeal(composite);

        applier.apply(dataList, "10g cashews, 50g oats for breakfast");

        assertEquals(0, dataList.getCompositeMeals().size());
        assertEquals(2, dataList.getFoodItems().size());
        assertTrue(dataList.getFoodItems().stream().allMatch(AiFoodVoiceParsingService.ParsedFoodData::isUserSpecifiedGrams));
    }

    @Test
    void skipsCountOverrideWhenAiSetPortionUnit() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData item = new AiFoodVoiceParsingService.ParsedFoodData();
        item.setFoodName("apple");
        item.setQuantity(1.0);
        item.setUnit("medium");
        item.setEstimatedGrams(182.0);
        dataList.addFoodItem(item);

        applier.apply(dataList, "one apple");

        assertEquals(182, dataList.getFoodItems().get(0).getEstimatedGrams(), 0.1);
        assertEquals("medium", dataList.getFoodItems().get(0).getUnit());
    }
}
