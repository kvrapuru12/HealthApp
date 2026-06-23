package com.healthapp.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoiceMealComposerTest {

    private final VoiceMealComposer composer = new VoiceMealComposer(new PortionGramEstimator());

    @Test
    void shouldMergePlate_onAndWithoutWith() {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = List.of(
                item("grilled salmon", "dinner", 200),
                item("cooked quinoa", "dinner", 185),
                item("steamed broccoli", "dinner", 150)
        );
        assertTrue(composer.shouldMergePlate(
                "for dinner I had salmon and quinoa and broccoli", items));
    }

    @Test
    void shouldNotMerge_whenLaterSegment() {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = List.of(
                item("pizza", "dinner", 300),
                item("salad", "dinner", 150)
        );
        assertFalse(composer.shouldMergePlate("pizza then later I had salad", items));
    }

    @Test
    void splitCompoundFoodName_splitsWithAndAnd() {
        List<String> parts = VoiceMealComposer.splitCompoundFoodName(
                "grilled salmon with cooked quinoa and steamed broccoli");
        assertEquals(3, parts.size());
        assertTrue(parts.get(0).toLowerCase().contains("salmon"));
    }

    @Test
    void isBeverage_detectsLassi() {
        assertTrue(VoiceMealComposer.isBeverage("mango lassi"));
        assertTrue(VoiceMealComposer.isBeverage("a glass of milk"));
        assertFalse(VoiceMealComposer.isBeverage("chicken biryani"));
        assertFalse(VoiceMealComposer.isBeverage("steamed broccoli"));
        assertFalse(VoiceMealComposer.isBeverage("grilled steak"));
        assertTrue(VoiceMealComposer.isBeverage("chai tea"));
        assertTrue(VoiceMealComposer.isBeverage("diet coke"));
    }

    @Test
    void applyVoiceMealRules_doesNotMergeWhenCompositesAlreadyPresent() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        dataList.addCompositeMeal(item("tuna wrap", "lunch", 220));
        dataList.addFoodItem(item("crisps", "lunch", 32));
        dataList.addFoodItem(item("diet coke", "lunch", 330));

        composer.applyVoiceMealRules(dataList,
                "um so for lunch today I grabbed a tuna wrap and also a small bag of crisps and a diet coke");

        assertEquals(1, dataList.getCompositeMeals().size());
        assertEquals(2, dataList.getFoodItems().size());
        assertTrue(dataList.getFoodItems().stream().anyMatch(i -> "crisps".equals(i.getFoodName())));
        assertTrue(dataList.getFoodItems().stream().anyMatch(i -> "diet coke".equals(i.getFoodName())));
    }

    @Test
    void shouldNotMerge_plainAndWithoutMealOccasion() {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = List.of(
                item("eggs", "breakfast", 100),
                item("toast", "breakfast", 60)
        );
        assertFalse(composer.shouldMergePlate("eggs and toast", items));
    }

    @Test
    void splitCompoundFoodName_splitsFoodAndDrinkPair() {
        List<String> parts = VoiceMealComposer.splitCompoundFoodName("3 cookies and a glass of milk");
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).toLowerCase().contains("cookie"));
        assertTrue(parts.get(1).toLowerCase().contains("milk"));
    }

    @Test
    void applyVoiceMealRules_mergesPlateKeepsDrink() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        dataList.addFoodItem(item("grilled salmon", "dinner", 200));
        dataList.addFoodItem(item("cooked quinoa", "dinner", 185));
        dataList.addFoodItem(item("mango lassi", "dinner", 300));

        composer.applyVoiceMealRules(dataList, "dinner was salmon and quinoa and a mango lassi");

        assertEquals(1, dataList.getCompositeMeals().size());
        assertEquals(1, dataList.getFoodItems().size());
        assertEquals("mango lassi", dataList.getFoodItems().get(0).getFoodName());
        assertEquals(2, dataList.getCompositeMeals().get(0).getIngredients().size());
    }

    @Test
    void shouldNotMergeSplitCompositeIngredients() {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = List.of(
                itemWithUserGrams("whole wheat toast", "breakfast", 60),
                itemWithUserGrams("butter", "breakfast", 10),
                itemWithUserGrams("peanut butter", "breakfast", 32)
        );
        assertFalse(composer.shouldMergePlate(
                "two slices whole wheat toast with butter and peanut butter for breakfast", items));
    }

    @Test
    void shouldMergePlate_thaliIgnoresMixedMealTypes() {
        List<AiFoodVoiceParsingService.ParsedFoodData> items = List.of(
                item("Dal", "lunch", 150),
                item("Rice", "dinner", 200),
                item("Roti", null, 80)
        );
        assertTrue(composer.shouldMergePlate("Indian thali with dal rice roti and paneer curry", items));
    }

    @Test
    void applyVoiceMealRules_mergesSmoothieIngredientsIntoComposite() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        dataList.addFoodItem(item("Spinach", "snack", 30));
        dataList.addFoodItem(item("Banana", "snack", 120));
        dataList.addFoodItem(item("Almond milk", "snack", 240));

        composer.applyVoiceMealRules(dataList, "green smoothie with spinach banana and almond milk");

        assertEquals(1, dataList.getCompositeMeals().size());
        assertEquals(0, dataList.getFoodItems().size());
        assertEquals("green smoothie", dataList.getCompositeMeals().get(0).getFoodName());
        assertEquals(3, dataList.getCompositeMeals().get(0).getIngredients().size());
    }

    @Test
    void applyVoiceMealRules_mergesThaliIntoComposite() {
        AiFoodVoiceParsingService.ParsedFoodDataList dataList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        dataList.addFoodItem(item("Dal", "lunch", 150));
        dataList.addFoodItem(item("Rice", "lunch", 200));
        dataList.addFoodItem(item("Roti", "lunch", 80));
        dataList.addFoodItem(item("Paneer curry", "lunch", 180));

        composer.applyVoiceMealRules(dataList, "Indian thali with dal rice roti and paneer curry");

        assertEquals(1, dataList.getCompositeMeals().size());
        assertEquals(0, dataList.getFoodItems().size());
        assertEquals("Indian thali plate", dataList.getCompositeMeals().get(0).getFoodName());
        assertEquals(4, dataList.getCompositeMeals().get(0).getIngredients().size());
    }

    private static AiFoodVoiceParsingService.ParsedFoodData item(String name, String meal, double grams) {
        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName(name);
        data.setMealType(meal);
        data.setEstimatedGrams(grams);
        data.setQuantity(grams);
        data.setUnit("grams");
        data.setLoggedAt(LocalDateTime.now());
        return data;
    }

    private static AiFoodVoiceParsingService.ParsedFoodData itemWithUserGrams(String name, String meal, double grams) {
        AiFoodVoiceParsingService.ParsedFoodData data = item(name, meal, grams);
        data.setUserSpecifiedGrams(true);
        return data;
    }
}
