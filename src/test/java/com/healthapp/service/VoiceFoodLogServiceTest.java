package com.healthapp.service;

import com.healthapp.config.AiFoodProperties;
import com.healthapp.dto.FoodLogCreateResponse;
import com.healthapp.dto.VoiceFoodLogRequest;
import com.healthapp.dto.VoiceFoodLogResponse;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.User;
import com.healthapp.exception.VoiceFoodLogException;
import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceFoodLogServiceTest {

    @Mock
    private FoodItemService foodItemService;
    @Mock
    private FoodLogService foodLogService;
    @Mock
    private FoodItemRepository foodItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiFoodVoiceParsingService aiFoodVoiceParsingService;
    @Mock
    private AiFoodProperties aiFoodProperties;
    @Mock
    private PortionGramEstimator portionGramEstimator;
    @Mock
    private com.healthapp.service.nutrition.SimpleFoodNutritionResolver simpleFoodNutritionResolver;
    @Mock
    private com.healthapp.service.nutrition.CompositeFoodNutritionResolver compositeFoodNutritionResolver;

    @InjectMocks
    private VoiceFoodLogService voiceFoodLogService;

    @Test
    void processVoiceFoodLog_throwsWhenUserMismatch() {
        VoiceFoodLogRequest request = new VoiceFoodLogRequest(10L, "eggs and toast");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> voiceFoodLogService.processVoiceFoodLog(request, 11L));

        assertTrue(ex.getMessage().contains("own account"));
    }

    @Test
    void processVoiceFoodLog_throwsWhenNothingParsed() {
        VoiceFoodLogRequest request = new VoiceFoodLogRequest(10L, "something");
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(aiFoodVoiceParsingService.parseVoiceText("something"))
                .thenReturn(new AiFoodVoiceParsingService.ParsedFoodDataList());

        VoiceFoodLogException ex = assertThrows(VoiceFoodLogException.class,
                () -> voiceFoodLogService.processVoiceFoodLog(request, 10L));

        assertEquals("NO_FOOD_PARSED", ex.getErrorCode());
    }

    @Test
    void processVoiceFoodLog_createsLogFromParsedItem() {
        VoiceFoodLogRequest request = new VoiceFoodLogRequest(10L, "2 eggs");

        AiFoodVoiceParsingService.ParsedFoodData parsed = new AiFoodVoiceParsingService.ParsedFoodData();
        parsed.setFoodName("Eggs");
        parsed.setQuantity(2.0);
        parsed.setUnit("pieces");
        parsed.setMealType("breakfast");
        parsed.setLoggedAt(LocalDateTime.now().minusMinutes(1));
        parsed.setEstimatedGrams(100.0);

        AiFoodVoiceParsingService.ParsedFoodDataList parsedList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        parsedList.addFoodItem(parsed);

        FoodItem foodItem = new FoodItem();
        foodItem.setId(5L);
        foodItem.setName("eggs");
        foodItem.setCreatedBy(10L);
        foodItem.setCaloriesPerUnit(155);
        foodItem.setStatus(FoodItem.FoodStatus.ACTIVE);

        FoodLogCreateResponse response = new FoodLogCreateResponse();
        response.setCalories(150.0);
        response.setProtein(12.0);
        response.setCarbs(1.0);
        response.setFat(10.0);
        response.setFiber(0.0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(aiFoodProperties.isShowConfidence()).thenReturn(false);
        when(portionGramEstimator.resolveEffectiveGrams(any(), any(), any(), any())).thenReturn(100.0);
        when(aiFoodVoiceParsingService.parseVoiceText("2 eggs")).thenReturn(parsedList);
        when(foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(eq("eggs"), eq(FoodItem.FoodStatus.ACTIVE), eq(10L)))
                .thenReturn(Optional.of(foodItem));
        when(foodLogService.createFoodLog(any(), eq(10L))).thenReturn(response);

        VoiceFoodLogResponse result = voiceFoodLogService.processVoiceFoodLog(request, 10L);

        assertEquals(1, result.getLogs().size());
        assertEquals("Food log created from voice input", result.getMessage());
        assertEquals("eggs", result.getLogs().get(0).getFood());
    }

    @Test
    void processVoiceFoodLog_preservesOrderForMultipleItems() {
        VoiceFoodLogRequest request = new VoiceFoodLogRequest(10L, "eggs and toast");

        AiFoodVoiceParsingService.ParsedFoodData eggs = new AiFoodVoiceParsingService.ParsedFoodData();
        eggs.setFoodName("Eggs");
        eggs.setQuantity(2.0);
        eggs.setUnit("pieces");
        eggs.setMealType("breakfast");
        eggs.setLoggedAt(LocalDateTime.now().minusMinutes(1));
        eggs.setEstimatedGrams(100.0);

        AiFoodVoiceParsingService.ParsedFoodData toast = new AiFoodVoiceParsingService.ParsedFoodData();
        toast.setFoodName("Toast");
        toast.setQuantity(1.0);
        toast.setUnit("slice");
        toast.setMealType("breakfast");
        toast.setLoggedAt(LocalDateTime.now().minusMinutes(1));
        toast.setEstimatedGrams(30.0);

        AiFoodVoiceParsingService.ParsedFoodDataList parsedList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        parsedList.addFoodItem(eggs);
        parsedList.addFoodItem(toast);

        FoodItem eggsItem = new FoodItem();
        eggsItem.setId(5L);
        eggsItem.setName("eggs");
        eggsItem.setCreatedBy(10L);
        eggsItem.setCaloriesPerUnit(155);
        eggsItem.setStatus(FoodItem.FoodStatus.ACTIVE);

        FoodItem toastItem = new FoodItem();
        toastItem.setId(6L);
        toastItem.setName("toast");
        toastItem.setCreatedBy(10L);
        toastItem.setCaloriesPerUnit(80);
        toastItem.setStatus(FoodItem.FoodStatus.ACTIVE);

        FoodLogCreateResponse eggsResponse = new FoodLogCreateResponse();
        eggsResponse.setCalories(150.0);
        eggsResponse.setProtein(12.0);
        eggsResponse.setCarbs(1.0);
        eggsResponse.setFat(10.0);
        eggsResponse.setFiber(0.0);

        FoodLogCreateResponse toastResponse = new FoodLogCreateResponse();
        toastResponse.setCalories(80.0);
        toastResponse.setProtein(3.0);
        toastResponse.setCarbs(15.0);
        toastResponse.setFat(1.0);
        toastResponse.setFiber(2.0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(aiFoodProperties.isShowConfidence()).thenReturn(false);
        when(portionGramEstimator.resolveEffectiveGrams(any(), any(), any(), any())).thenReturn(100.0, 30.0);
        when(aiFoodVoiceParsingService.parseVoiceText("eggs and toast")).thenReturn(parsedList);
        when(foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(eq("eggs"), eq(FoodItem.FoodStatus.ACTIVE), eq(10L)))
                .thenReturn(Optional.of(eggsItem));
        when(foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(eq("toast"), eq(FoodItem.FoodStatus.ACTIVE), eq(10L)))
                .thenReturn(Optional.of(toastItem));
        when(foodLogService.createFoodLog(any(), eq(10L))).thenReturn(eggsResponse, toastResponse);

        VoiceFoodLogResponse result = voiceFoodLogService.processVoiceFoodLog(request, 10L);

        assertEquals(2, result.getLogs().size());
        assertEquals("eggs", result.getLogs().get(0).getFood());
        assertEquals("toast", result.getLogs().get(1).getFood());
        assertEquals("Created 2 food logs from voice input", result.getMessage());
    }
}
