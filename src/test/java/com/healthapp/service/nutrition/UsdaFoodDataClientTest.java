package com.healthapp.service.nutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsdaFoodDataClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractMacros_prefersKcalEnergy() throws Exception {
        String json = """
                {
                  "foodNutrients": [
                    {"nutrient": {"name": "Energy"}, "amount": 418},
                    {"nutrient": {"name": "Energy (kcal)"}, "amount": 100},
                    {"nutrient": {"name": "Protein"}, "amount": 5},
                    {"nutrient": {"name": "Carbohydrate, by difference"}, "amount": 10},
                    {"nutrient": {"name": "Total lipid (fat)"}, "amount": 3},
                    {"nutrient": {"name": "Fiber, total dietary"}, "amount": 1}
                  ]
                }
                """;
        NutritionProfile profile = UsdaFoodDataClient.extractMacros(
                objectMapper.readTree(json), 42, 0.9);
        assertNotNull(profile);
        assertEquals(100.0, profile.getCaloriesPer100g(), 0.1);
        assertEquals(42, profile.getFdcId());
        assertEquals(NutritionSource.USDA, profile.getSource());
    }

    @Test
    void extractMacros_convertsKjWhenKcalMissing() throws Exception {
        String json = """
                {
                  "foodNutrients": [
                    {"nutrient": {"name": "Energy (kJ)"}, "amount": 418.4},
                    {"nutrient": {"name": "Protein"}, "amount": 5}
                  ]
                }
                """;
        NutritionProfile profile = UsdaFoodDataClient.extractMacros(
                objectMapper.readTree(json), 7, 0.8);
        assertNotNull(profile);
        assertEquals(100.0, profile.getCaloriesPer100g(), 1.0);
    }

    @Test
    void extractMacros_returnsNullWithoutEnergy() throws Exception {
        String json = """
                {
                  "foodNutrients": [
                    {"nutrient": {"name": "Protein"}, "amount": 5}
                  ]
                }
                """;
        assertNull(UsdaFoodDataClient.extractMacros(objectMapper.readTree(json), 1, 0.5));
    }
}
