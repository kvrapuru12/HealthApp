package com.healthapp.service;

import com.healthapp.service.nutrition.NutritionValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden calorie range checks using deterministic mocked macro inputs (no live OpenAI/USDA).
 */
class AiFoodParsingGoldenTest {

    @ParameterizedTest
    @CsvSource({
            "avocado,160,2,8.5,14.7,6.7,150,200,320",
            "boiled egg,155,13,1.1,11,0,100,140,180",
            "black coffee,2,0.3,0,0,0,250,0,10",
            "chicken burger,250,15,22,12,2,200,500,900"
    })
    void totalCaloriesWithinExpectedRange(String food, double calPer100g, double protein, double carbs,
                                          double fat, double fiber, double grams,
                                          double minCal, double maxCal) {
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(calPer100g);
        nutrition.setProteinPer100g(protein);
        nutrition.setCarbsPer100g(carbs);
        nutrition.setFatPer100g(fat);
        nutrition.setFiberPer100g(fiber);

        var validated = NutritionValidator.validateNutritionData(food, nutrition, grams);
        assertNotNull(validated, "validation failed for " + food);
        double total = validated.getCaloriesPer100g() * (grams / 100.0);
        assertTrue(total >= minCal && total <= maxCal,
                food + " total calories " + total + " outside [" + minCal + "," + maxCal + "]");
    }

    @ParameterizedTest
    @CsvSource({
            "walk,4,30,100,180",
            "run,10,30,250,350"
    })
    void activityBurnWithinExpectedRange(String activity, double calPerMin, int minutes,
                                         double minCal, double maxCal) {
        double burn = ActivityCalorieEstimator.estimateCaloriesPerMinute(activity, "cardio").doubleValue() * minutes;
        assertTrue(burn >= minCal && burn <= maxCal,
                activity + " burn " + burn + " outside [" + minCal + "," + maxCal + "]");
    }
}
