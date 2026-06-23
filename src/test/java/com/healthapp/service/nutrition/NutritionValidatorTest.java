package com.healthapp.service.nutrition;

import com.healthapp.service.AiFoodVoiceParsingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NutritionValidatorTest {

    @Test
    void rejectsSolidFoodWithCoffeeLevelCalories() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(2, 0.3, 0, 0, 0);
        assertNull(NutritionValidator.validateNutritionData("avocado", data, 150.0));
    }

    @Test
    void acceptsAvocadoMacros() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(160, 2, 8.5, 14.7, 6.7);
        assertNotNull(NutritionValidator.validateNutritionData("avocado", data, 150.0));
    }

    @Test
    void acceptsBlackCoffee() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(2, 0.3, 0, 0, 0);
        assertNotNull(NutritionValidator.validateNutritionData("black coffee", data, 250.0));
    }

    @Test
    void rejectsOneGramPortionForSolidFood() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(160, 2, 8.5, 14.7, 6.7);
        assertNull(NutritionValidator.validateNutritionData("avocado", data, 1.0));
    }

    @Test
    void rejectsImplausibleBananaCaloriesPer100g() {
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition("banana", 421));
    }

    @Test
    void rejectsImplausibleChickenBreastCaloriesPer100g() {
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition("grilled chicken breast", 100));
    }

    @Test
    void acceptsRedWineDespiteLowMacroRatio() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(85, 0.1, 2.6, 0, 0);
        assertNotNull(NutritionValidator.validateNutritionData("red wine", data, 150.0));
    }

    @Test
    void acceptsColaSoftDrink() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(42, 0, 10.6, 0, 0);
        assertNotNull(NutritionValidator.validateNutritionData("coke", data, 330.0));
    }

    @Test
    void rejectsLowCalorieCakeUsdaMatch() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(42, 0, 10.6, 0, 0);
        assertNull(NutritionValidator.validateNutritionData("slice of chocolate cake", data, 80.0));
    }

    @Test
    void rejectsImplausibleStoredCakeCalories() {
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition("chocolate cake", 42));
    }

    @Test
    void rejectsOliveOilMacrosForBoiledEgg() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(884, 0, 0, 100, 0);
        assertNull(NutritionValidator.validateNutritionData("boiled egg", data, 100.0));
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition("boiled egg", 884));
    }

    @Test
    void acceptsBoiledEggMacros() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(155, 13, 1.1, 11, 0);
        assertNotNull(NutritionValidator.validateNutritionData("boiled egg", data, 100.0));
        assertFalse(NutritionValidator.hasImplausibleStoredNutrition("boiled egg", 155));
    }

    @Test
    void rejectsHighProteinAppleMacros() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(121, 9.1, 18, 5.5, 2);
        assertNull(NutritionValidator.validateNutritionData("apple", data, 150.0));
    }

    @Test
    void validateForPersist_acceptsProteinShakeWithBananaBlend() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(83.5, 7, 10, 2, 1.2);
        assertNotNull(NutritionValidator.validateForPersist(
                "protein shake with banana and milk", data, 395.0));
    }

    @Test
    void validateForPersist_acceptsBlueberryMuffin() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(380, 5, 50, 18, 2);
        assertNotNull(NutritionValidator.validateForPersist("Blueberry muffin", data, 110.0));
    }

    @Test
    void validateForPersist_acceptsMangoLassi() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(83, 3, 14, 2, 0.5);
        assertNotNull(NutritionValidator.validateForPersist("mango lassi", data, 250.0));
    }

    @Test
    void validateForPersist_acceptsThaiIcedTea() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(80, 1, 18, 2, 0);
        assertNotNull(NutritionValidator.validateForPersist("thai iced tea", data, 300.0));
    }

    @Test
    void validateForPersist_acceptsSalmonQuinoaBroccoliPlate() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(165, 18, 12, 7, 2);
        assertNotNull(NutritionValidator.validateForPersist(
                "Grilled salmon with cooked quinoa and steamed broccoli with olive oil drizzle",
                data, 450.0));
    }

    @Test
    void acceptsThaiIcedTeaUnderStrictRulesAfterBeverageFix() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(80, 1, 18, 2, 0);
        assertNotNull(NutritionValidator.validateNutritionData("thai iced tea", data, 300.0));
    }

    @Test
    void compositeBlendWithBroccoliNameIsPlausible() {
        AiFoodVoiceParsingService.NutritionData blend = nutrition(160, 18, 12, 7, 2);
        assertFalse(NutritionValidator.hasImplausibleCompositeNutrition(blend));
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition(
                "grilled salmon with quinoa and broccoli", 160));
    }

    @Test
    void rejectsImplausibleStoredDietCokeCalories() {
        assertTrue(NutritionValidator.hasImplausibleStoredNutrition("diet coke", 42));
    }

    @Test
    void acceptsDietCokeZeroCalories() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(0, 0, 0, 0, 0);
        assertNotNull(NutritionValidator.validateNutritionData("diet coke", data, 330.0));
    }

    @Test
    void acceptsSpinachBelowThirtyCaloriesPer100g() {
        AiFoodVoiceParsingService.NutritionData data = nutrition(23, 2.9, 3.6, 0.4, 2.2);
        assertNotNull(NutritionValidator.validateNutritionData("spinach", data, 80.0));
        assertNotNull(NutritionValidator.validateNutritionData("raw spinach", data, 80.0));
    }

    private static AiFoodVoiceParsingService.NutritionData nutrition(
            double cal, double protein, double carbs, double fat, double fiber) {
        AiFoodVoiceParsingService.NutritionData data = new AiFoodVoiceParsingService.NutritionData();
        data.setCaloriesPer100g(cal);
        data.setProteinPer100g(protein);
        data.setCarbsPer100g(carbs);
        data.setFatPer100g(fat);
        data.setFiberPer100g(fiber);
        return data;
    }
}
