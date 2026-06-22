package com.healthapp.service.nutrition;

import com.healthapp.entity.FoodItem;
import com.healthapp.service.AiFoodVoiceParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleFoodNutritionResolverTest {

    @Mock
    private NutritionLookupService nutritionLookupService;

    private SimpleFoodNutritionResolver resolver;

    private final FoodNutritionFallback foodNutritionFallback = new FoodNutritionFallback();

    @BeforeEach
    void setUp() {
        resolver = new SimpleFoodNutritionResolver(nutritionLookupService, foodNutritionFallback);
    }

    @Test
    void isSimpleFood_whenNoIngredients() {
        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("apple");
        assertTrue(SimpleFoodNutritionResolver.isSimpleFood(data));
    }

    @Test
    void isTrustedFoodItem_whenFdcIdPresent() {
        FoodItem item = new FoodItem();
        item.setName("egg");
        item.setCaloriesPerUnit(155);
        item.setFdcId(123);
        assertTrue(SimpleFoodNutritionResolver.isTrustedFoodItem(item));
    }

    @Test
    void resolve_usesUsdaWhenAvailable() {
        when(nutritionLookupService.lookup("apple")).thenReturn(Optional.of(
                new NutritionProfile(52, 0.3, 14, 0.2, 2.4, NutritionSource.USDA, 0.9, 171688)));

        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("apple");
        data.setEstimatedGrams(150.0);

        resolver.resolve(data);

        assertEquals(52, data.getNutrition().getCaloriesPer100g(), 0.1);
        assertEquals(NutritionSource.USDA, data.getNutritionSource());
    }

    @Test
    void resolve_fallsBackToLlmWhenUsdaMisses() {
        when(nutritionLookupService.lookup(anyString())).thenReturn(Optional.empty());

        AiFoodVoiceParsingService.NutritionData llm = new AiFoodVoiceParsingService.NutritionData();
        llm.setCaloriesPer100g(52);
        llm.setProteinPer100g(0.3);
        llm.setCarbsPer100g(14);
        llm.setFatPer100g(0.2);
        llm.setFiberPer100g(2.4);

        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("apple");
        data.setEstimatedGrams(150.0);
        data.setNutrition(llm);

        resolver.resolve(data);

        assertEquals(NutritionSource.LLM, data.getNutritionSource());
        assertEquals(52, data.getNutrition().getCaloriesPer100g(), 0.1);
    }

    @Test
    void resolve_usesHardcodedFallbackWhenUsdaAndLlmMiss() {
        when(nutritionLookupService.lookup(anyString())).thenReturn(Optional.empty());

        AiFoodVoiceParsingService.ParsedFoodData data = new AiFoodVoiceParsingService.ParsedFoodData();
        data.setFoodName("banana");
        data.setEstimatedGrams(120.0);

        resolver.resolve(data);

        assertEquals(NutritionSource.FALLBACK_HARDCODED, data.getNutritionSource());
        assertEquals(89, data.getNutrition().getCaloriesPer100g(), 0.1);
    }

    @Test
    void rejectsGenericDefaultFoodItem() {
        FoodItem item = new FoodItem();
        item.setName("apple");
        item.setCaloriesPerUnit(100);
        item.setProteinPerUnit(5.0);
        item.setCarbsPerUnit(10.0);
        item.setFatPerUnit(3.0);
        assertFalse(SimpleFoodNutritionResolver.isTrustedFoodItem(item));
    }
}
