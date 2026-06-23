package com.healthapp.service.nutrition;

import com.healthapp.config.NutritionLookupProperties;
import com.healthapp.entity.FoodNutritionCache;
import com.healthapp.repository.FoodNutritionCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NutritionLookupServiceTest {

    @Mock
    private NutritionLookupProperties properties;

    @Mock
    private FoodNutritionCacheRepository cacheRepository;

    @Mock
    private UsdaFoodDataClient usdaClient;

    private final FoodNutritionFallback foodNutritionFallback = new FoodNutritionFallback();

    private NutritionLookupService nutritionLookupService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(properties.getProvider()).thenReturn("usda");
        nutritionLookupService = new NutritionLookupService(
                properties, cacheRepository, usdaClient, foodNutritionFallback);
    }

    @Test
    void lookup_returnsCachedProfile() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getCacheTtlDays()).thenReturn(90);
        FoodNutritionCache cache = new FoodNutritionCache();
        cache.setNormalizedName("avocado");
        cache.setSource(NutritionSource.USDA.name());
        cache.setCaloriesPer100g(160.0);
        cache.setProteinPer100g(2.0);
        cache.setCarbsPer100g(8.5);
        cache.setFatPer100g(14.7);
        cache.setFiberPer100g(6.7);
        cache.setConfidence(0.9);
        cache.setFdcId(123);
        cache.setUpdatedAt(LocalDateTime.now());
        when(cacheRepository.findByNormalizedName("avocado")).thenReturn(Optional.of(cache));

        Optional<NutritionProfile> result = nutritionLookupService.lookup("avocado");
        assertTrue(result.isPresent());
        assertEquals(160.0, result.get().getCaloriesPer100g());
        verify(usdaClient, never()).searchFoods(anyString(), anyInt());
    }

    @Test
    void scoreMatch_exactName() {
        assertEquals(1.0, NutritionLookupService.scoreMatch("avocado", "Avocado"));
    }

    @Test
    void scoreMatch_doesNotOverScoreShortTokenInsideLongQuery() {
        assertTrue(NutritionLookupService.scoreMatch("chicken rice bowl", "rice, white, cooked") < 0.85);
    }

    @Test
    void lookup_cachesUsdaMiss() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProvider()).thenReturn("usda");
        when(properties.getConfidenceThreshold()).thenReturn(0.75);
        when(cacheRepository.findByNormalizedName("unknown food")).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(true);
        when(usdaClient.searchFoods(anyString(), eq(5))).thenReturn(UsdaFoodDataClient.UsdaSearchResponse.empty());
        when(cacheRepository.save(any(FoodNutritionCache.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<NutritionProfile> result = nutritionLookupService.lookup("unknown food");
        assertTrue(result.isEmpty());
        verify(cacheRepository).save(argThat(cache ->
                NutritionLookupService.NEGATIVE_CACHE_SOURCE.equals(cache.getSource())));
        verify(usdaClient, times(1)).searchFoods(anyString(), eq(5));
    }

    @Test
    void blendIngredients_weightedAverage() {
        when(properties.isEnabled()).thenReturn(true);

        NutritionProfile berries = new NutritionProfile(50, 1, 12, 0.5, 2, NutritionSource.USDA, 0.9, 1);
        NutritionProfile banana = new NutritionProfile(89, 1.1, 23, 0.3, 2.6, NutritionSource.USDA, 0.9, 2);

        when(cacheRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(true);
        when(usdaClient.searchFoods(anyString(), eq(5)))
                .thenReturn(new UsdaFoodDataClient.UsdaSearchResponse(
                        List.of(new UsdaFoodDataClient.UsdaSearchResult(1, "berries raw")), false))
                .thenReturn(new UsdaFoodDataClient.UsdaSearchResponse(
                        List.of(new UsdaFoodDataClient.UsdaSearchResult(2, "banana raw")), false));
        when(usdaClient.getFoodDetails(eq(1), anyDouble())).thenReturn(UsdaFoodDataClient.UsdaDetailResponse.success(berries));
        when(usdaClient.getFoodDetails(eq(2), anyDouble())).thenReturn(UsdaFoodDataClient.UsdaDetailResponse.success(banana));

        var portions = List.of(
                new NutritionLookupService.IngredientPortion("berries", 50, "berries raw"),
                new NutritionLookupService.IngredientPortion("banana", 120, "banana raw")
        );
        Optional<NutritionProfile> blended = nutritionLookupService.blendIngredients(portions);
        assertTrue(blended.isPresent());
        assertTrue(blended.get().getCaloriesPer100g() > 50);
        assertTrue(blended.get().getCaloriesPer100g() < 120);
    }

    @Test
    void blendIngredients_usesFallbackWhenUsdaMisses() {
        when(properties.isEnabled()).thenReturn(true);
        when(cacheRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(false);

        var portions = List.of(
                new NutritionLookupService.IngredientPortion("whole wheat toast", 60, "toast"),
                new NutritionLookupService.IngredientPortion("butter", 14, "butter"),
                new NutritionLookupService.IngredientPortion("peanut butter", 16, "peanut butter")
        );
        Optional<NutritionProfile> blended = nutritionLookupService.blendIngredients(portions);
        assertTrue(blended.isPresent());
        double totalCal = blended.get().getCaloriesPer100g() * (60 + 14 + 16) / 100.0;
        assertTrue(totalCal >= 250, "toast breakfast total cal should be substantial: " + totalCal);
    }

    @Test
    void buildSearchTerm_appleAndCarbonara() {
        assertEquals("apple raw", NutritionLookupService.buildSearchTerm("one apple"));
        assertEquals("pasta carbonara", NutritionLookupService.buildSearchTerm("pasta carbonara with garlic bread"));
    }

    @Test
    void lookup_usesFallbackWhenUsdaUnavailable() {
        when(properties.isEnabled()).thenReturn(true);
        when(cacheRepository.findByNormalizedName("banana")).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(false);
        when(cacheRepository.save(any(FoodNutritionCache.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<NutritionProfile> result = nutritionLookupService.lookup("banana");
        assertTrue(result.isPresent());
        assertEquals(89.0, result.get().getCaloriesPer100g(), 0.1);
        assertEquals(NutritionSource.FALLBACK_HARDCODED, result.get().getSource());
        verify(cacheRepository).save(any(FoodNutritionCache.class));
    }

    @Test
    void lookup_doesNotNegativeCacheOnTransientUsdaFailure() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProvider()).thenReturn("usda");
        when(properties.getConfidenceThreshold()).thenReturn(0.75);
        when(cacheRepository.findByNormalizedName("unknown food xyz")).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(true);
        when(usdaClient.searchFoods(anyString(), eq(5)))
                .thenReturn(UsdaFoodDataClient.UsdaSearchResponse.transientError());

        Optional<NutritionProfile> result = nutritionLookupService.lookup("unknown food xyz");
        assertTrue(result.isEmpty());
        verify(cacheRepository, never()).save(any(FoodNutritionCache.class));
    }

    @Test
    void blendIngredients_decomposesSandwichIntoComponents() {
        when(properties.isEnabled()).thenReturn(true);
        when(cacheRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(false);

        var portions = List.of(
                new NutritionLookupService.IngredientPortion("chicken sandwich", 220, "chicken sandwich")
        );
        Optional<NutritionProfile> blended = nutritionLookupService.blendIngredients(portions);
        assertTrue(blended.isPresent());
        assertTrue(blended.get().getCarbsPer100g() > 0);
    }

    @Test
    void blendIngredients_abortsWhenIngredientUnresolved() {
        when(properties.isEnabled()).thenReturn(true);
        when(cacheRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(usdaClient.isAvailable()).thenReturn(false);

        var portions = List.of(
                new NutritionLookupService.IngredientPortion("mystery garnish", 100, "unknown garnish xyz")
        );
        assertTrue(nutritionLookupService.blendIngredients(portions).isEmpty());
    }

    @Test
    void fallback_oatMilkLatteAndCarbonara() {
        NutritionProfile latte = foodNutritionFallback.resolveKnown("latte with oat milk").orElseThrow();
        assertEquals(65.0, latte.getCaloriesPer100g(), 0.1);
        NutritionProfile carbonara = foodNutritionFallback.resolveKnown("pasta carbonara").orElseThrow();
        assertEquals(280.0, carbonara.getCaloriesPer100g(), 0.1);
    }
}
