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
    void splitsMultiIngredientCompositeWhenVoiceListsMultipleItems() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("steak with mashed potatoes and green beans");
        AiFoodVoiceParsingService.IngredientData steak = new AiFoodVoiceParsingService.IngredientData();
        steak.setName("steak");
        steak.setEstimatedGrams(227.0);
        AiFoodVoiceParsingService.IngredientData potatoes = new AiFoodVoiceParsingService.IngredientData();
        potatoes.setName("mashed potatoes");
        potatoes.setEstimatedGrams(60.0);
        AiFoodVoiceParsingService.IngredientData beans = new AiFoodVoiceParsingService.IngredientData();
        beans.setName("green beans");
        beans.setEstimatedGrams(48.0);
        composite.getIngredients().add(steak);
        composite.getIngredients().add(potatoes);
        composite.getIngredients().add(beans);
        dataList.addCompositeMeal(composite);

        applier.apply(dataList, "8 oz steak, half cup mashed potatoes, and green beans");

        assertEquals(0, dataList.getCompositeMeals().size());
        assertEquals(3, dataList.getFoodItems().size());
        AiFoodVoiceParsingService.ParsedFoodData steakItem = dataList.getFoodItems().stream()
                .filter(i -> i.getFoodName().toLowerCase().contains("steak"))
                .findFirst()
                .orElseThrow();
        assertEquals(226.8, steakItem.getEstimatedGrams(), 0.5);
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

    @Test
    void keepsThaliCompositeWhenVoiceListsPlateItems() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        AiFoodVoiceParsingService.ParsedFoodData composite = new AiFoodVoiceParsingService.ParsedFoodData();
        composite.setFoodName("Indian thali with dal, rice, roti, and paneer curry");
        AiFoodVoiceParsingService.IngredientData dal = new AiFoodVoiceParsingService.IngredientData();
        dal.setName("dal");
        dal.setEstimatedGrams(80.0);
        AiFoodVoiceParsingService.IngredientData rice = new AiFoodVoiceParsingService.IngredientData();
        rice.setName("cooked rice");
        rice.setEstimatedGrams(100.0);
        composite.getIngredients().add(dal);
        composite.getIngredients().add(rice);
        dataList.addCompositeMeal(composite);

        applier.apply(dataList, "Indian thali with dal rice roti and paneer curry");

        assertEquals(1, dataList.getCompositeMeals().size());
        assertEquals(0, dataList.getFoodItems().size());
    }
}
