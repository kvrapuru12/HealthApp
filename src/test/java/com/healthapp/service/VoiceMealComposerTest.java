package com.healthapp.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
}
