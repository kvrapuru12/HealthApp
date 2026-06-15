package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendedPortionApplicatorTest {

    private final RecommendedPortionApplicator applicator = new RecommendedPortionApplicator();

    @Test
    void appliesDefaultWhenOnlyFoodNameGiven() {
        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("banana");
        data.setQuantity(1.0);
        data.setUnit("serving");

        applicator.applyDefaults(data);

        assertEquals("medium", data.getUnit());
        assertEquals(120.0, data.getEstimatedGrams());
    }

    @Test
    void doesNotOverrideUserSpecifiedGrams() {
        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("banana");
        data.setQuantity(1.0);
        data.setUnit("serving");
        data.setEstimatedGrams(90.0);
        data.setUserSpecifiedGrams(true);

        applicator.applyDefaults(data);

        assertEquals(90.0, data.getEstimatedGrams());
    }

    @Test
    void normalizesCompositeIngredientGrams() {
        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("salmon plate");
        AiFoodVoiceParsingService.IngredientData salmon = new AiFoodVoiceParsingService.IngredientData();
        salmon.setName("grilled salmon");
        salmon.setEstimatedGrams(1);
        AiFoodVoiceParsingService.IngredientData quinoa = new AiFoodVoiceParsingService.IngredientData();
        quinoa.setName("cooked quinoa");
        quinoa.setEstimatedGrams(1);
        data.getIngredients().add(salmon);
        data.getIngredients().add(quinoa);

        applicator.applyDefaults(data);

        assertEquals(270.0, data.getEstimatedGrams());
    }
}
