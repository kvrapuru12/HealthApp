package com.healthapp.service;

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

        AiFoodVoiceParsingService.ParsedFoodDataList parsedList = new AiFoodVoiceParsingService.ParsedFoodDataList();
        parsedList.addFoodItem(parsed);

        FoodItem foodItem = new FoodItem();
        foodItem.setId(5L);
        foodItem.setName("eggs");
        foodItem.setStatus(FoodItem.FoodStatus.ACTIVE);

        FoodLogCreateResponse response = new FoodLogCreateResponse();
        response.setCalories(150.0);
        response.setProtein(12.0);
        response.setCarbs(1.0);
        response.setFat(10.0);
        response.setFiber(0.0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(aiFoodVoiceParsingService.parseVoiceText("2 eggs")).thenReturn(parsedList);
        when(foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(eq("eggs"), eq(FoodItem.FoodStatus.ACTIVE), eq(10L)))
                .thenReturn(Optional.of(foodItem));
        when(foodLogService.createFoodLog(any(), eq(10L))).thenReturn(response);

        VoiceFoodLogResponse result = voiceFoodLogService.processVoiceFoodLog(request, 10L);

        assertEquals(1, result.getLogs().size());
        assertEquals("Food log created from voice input", result.getMessage());
        assertEquals("eggs", result.getLogs().get(0).getFood());
    }
}
