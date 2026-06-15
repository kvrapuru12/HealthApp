package com.healthapp.service.nutrition;

import com.healthapp.service.AiFoodVoiceParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeFoodNutritionResolverTest {

    @Mock
    private NutritionLookupService nutritionLookupService;

    private CompositeFoodNutritionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CompositeFoodNutritionResolver(nutritionLookupService, new FoodNutritionFallback());
    }

    @Test
    void aiIngredientCompositeUsesUsdaBlend() {
        NutritionProfile blend = new NutritionProfile(
                160, 18, 12, 7, 2, NutritionSource.USDA, 0.85, 12345);
        when(nutritionLookupService.blendIngredients(anyList())).thenReturn(java.util.Optional.of(blend));

        AiFoodVoiceParsingService.ParsedFoodData parsed = salmonQuinoaBroccoliMeal();
        parsed.setNutrition(llmNutrition(165, 18, 12, 7, 2));

        resolver.resolve(parsed);

        verify(nutritionLookupService).blendIngredients(anyList());
        assertEquals(NutritionSource.USDA, parsed.getNutritionSource());
        assertEquals(160.0, parsed.getNutrition().getCaloriesPer100g(), 0.1);
    }

    @Test
    void vagueCompositeWithoutIngredientsUsesLlmEstimate() {
        AiFoodVoiceParsingService.ParsedFoodData parsed = new AiFoodVoiceParsingService.ParsedFoodData();
        parsed.setFoodName("grilled salmon with quinoa and broccoli");
        parsed.setEstimatedGrams(435.0);
        parsed.setNutrition(llmNutrition(165, 18, 12, 7, 2));

        resolver.resolve(parsed);

        verify(nutritionLookupService, never()).blendIngredients(anyList());
        assertEquals(NutritionSource.LLM, parsed.getNutritionSource());
    }

    @Test
    void explicitCompositeUsesIngredientBlend() {
        NutritionProfile blend = new NutritionProfile(
                160, 18, 12, 7, 2, NutritionSource.USDA, 0.85, 12345);
        when(nutritionLookupService.blendIngredients(anyList())).thenReturn(java.util.Optional.of(blend));

        AiFoodVoiceParsingService.ParsedFoodData parsed = salmonQuinoaBroccoliMeal();
        parsed.setUserSpecifiedGrams(true);

        resolver.resolve(parsed);

        assertNotNull(parsed.getNutrition());
        assertEquals(NutritionSource.USDA, parsed.getNutritionSource());
        assertEquals(160.0, parsed.getNutrition().getCaloriesPer100g(), 0.1);
    }

    @Test
    void rejectsAllFallbackBlendForExplicitComposite() {
        NutritionProfile blend = new NutritionProfile(
                88, 3, 11, 3, 1, NutritionSource.FALLBACK_HARDCODED, 0.5, null);
        when(nutritionLookupService.blendIngredients(anyList())).thenReturn(java.util.Optional.of(blend));

        AiFoodVoiceParsingService.ParsedFoodData parsed = salmonQuinoaBroccoliMeal();
        parsed.setUserSpecifiedGrams(true);
        parsed.setNutrition(llmNutrition(165, 18, 12, 7, 2));

        resolver.resolve(parsed);

        assertEquals(NutritionSource.LLM, parsed.getNutritionSource());
        assertEquals(165.0, parsed.getNutrition().getCaloriesPer100g(), 0.1);
    }

    @Test
    void multiIngredientCompositeDetected() {
        AiFoodVoiceParsingService.ParsedFoodData parsed = salmonQuinoaBroccoliMeal();
        assertTrue(NutritionValidator.isMultiIngredientComposite(parsed));
    }

    private static AiFoodVoiceParsingService.NutritionData llmNutrition(
            double cal, double protein, double carbs, double fat, double fiber) {
        AiFoodVoiceParsingService.NutritionData nutrition = new AiFoodVoiceParsingService.NutritionData();
        nutrition.setCaloriesPer100g(cal);
        nutrition.setProteinPer100g(protein);
        nutrition.setCarbsPer100g(carbs);
        nutrition.setFatPer100g(fat);
        nutrition.setFiberPer100g(fiber);
        return nutrition;
    }

    private static AiFoodVoiceParsingService.ParsedFoodData salmonQuinoaBroccoliMeal() {
        AiFoodVoiceParsingService.ParsedFoodData parsed = new AiFoodVoiceParsingService.ParsedFoodData();
        parsed.setFoodName("grilled salmon with quinoa and broccoli");
        parsed.setEstimatedGrams(435.0);
        parsed.getIngredients().add(ingredient("grilled salmon", 200, "salmon, cooked"));
        parsed.getIngredients().add(ingredient("cooked quinoa", 185, "quinoa, cooked"));
        parsed.getIngredients().add(ingredient("steamed broccoli", 45, "broccoli, cooked"));
        parsed.getIngredients().add(ingredient("olive oil", 5, "olive oil"));
        return parsed;
    }

    private static AiFoodVoiceParsingService.IngredientData ingredient(String name, double grams, String term) {
        AiFoodVoiceParsingService.IngredientData data = new AiFoodVoiceParsingService.IngredientData();
        data.setName(name);
        data.setEstimatedGrams(grams);
        data.setFdcSearchTerm(term);
        return data;
    }
}
