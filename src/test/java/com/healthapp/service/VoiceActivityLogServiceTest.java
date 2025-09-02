package com.healthapp.service;

import com.healthapp.dto.ActivityCreateRequest;
import com.healthapp.dto.ActivityCreateResponse;
import com.healthapp.dto.ActivityLogCreateRequest;
import com.healthapp.dto.ActivityLogCreateResponse;
import com.healthapp.dto.VoiceActivityLogResponse;
import com.healthapp.entity.Activity;
import com.healthapp.entity.ActivityLog;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityLogRepository;
import com.healthapp.repository.ActivityRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceActivityLogServiceTest {

    @Mock
    private AiVoiceParsingService aiVoiceParsingService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private VoiceActivityLogService voiceActivityLogService;

    private User testUser;
    private Activity testActivity;
    private ActivityLog testActivityLog;
    private AiVoiceParsingService.ParsedActivityData parsedData;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Brisk walk");
        testActivity.setVisibility(Activity.Visibility.PRIVATE);
        testActivity.setCreatedBy(testUser);

        testActivityLog = new ActivityLog();
        testActivityLog.setId(1L);
        testActivityLog.setUser(testUser);
        testActivityLog.setActivity(testActivity);
        testActivityLog.setDurationMinutes(30);
        testActivityLog.setCaloriesBurned(new BigDecimal("135.0"));
        testActivityLog.setLoggedAt(LocalDateTime.now());
        testActivityLog.setNote("after breakfast");

        parsedData = new AiVoiceParsingService.ParsedActivityData();
        parsedData.setActivityName("Brisk walk");
        parsedData.setDurationMinutes(30);
        parsedData.setLoggedAt(LocalDateTime.now());
        parsedData.setNote("after breakfast");
    }

    @Test
    void processVoiceActivityLog_Success() {
        // Arrange
        Long userId = 1L;
        String voiceText = "I did a 30-minute brisk walk this morning after breakfast";
        Long authenticatedUserId = 1L;
        boolean isAdmin = false;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(aiVoiceParsingService.parseVoiceText(voiceText)).thenReturn(parsedData);
        when(activityRepository.findByCreatedByAndNameAndStatus(userId, "Brisk walk", Activity.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(activityService.createActivity(any(ActivityCreateRequest.class), eq(userId), eq(false)))
                .thenReturn(new ActivityCreateResponse(1L, LocalDateTime.now()));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityLogService.createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin)))
                .thenReturn(new ActivityLogCreateResponse(1L, LocalDateTime.now(), new BigDecimal("135.0")));
        when(activityLogRepository.findById(1L)).thenReturn(Optional.of(testActivityLog));

        // Act
        VoiceActivityLogResponse response = voiceActivityLogService.processVoiceActivityLog(
                userId, voiceText, authenticatedUserId, isAdmin);

        // Assert
        assertNotNull(response);
        assertEquals("Activity logged successfully", response.getMessage());
        assertNotNull(response.getActivityLog());
        assertEquals(1L, response.getActivityLog().getId());
        assertEquals("Brisk walk", response.getActivityLog().getActivity());
        assertEquals(30, response.getActivityLog().getDurationMinutes());
        assertEquals(135.0, response.getActivityLog().getCaloriesBurned());
        assertEquals("after breakfast", response.getActivityLog().getNote());

        verify(aiVoiceParsingService).parseVoiceText(voiceText);
        verify(activityService).createActivity(any(ActivityCreateRequest.class), eq(userId), eq(false));
        verify(activityLogService).createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin));
    }

    @Test
    void processVoiceActivityLog_ExistingActivityFound() {
        // Arrange
        Long userId = 1L;
        String voiceText = "I did a 30-minute brisk walk this morning after breakfast";
        Long authenticatedUserId = 1L;
        boolean isAdmin = false;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(aiVoiceParsingService.parseVoiceText(voiceText)).thenReturn(parsedData);
        when(activityRepository.findByCreatedByAndNameAndStatus(userId, "Brisk walk", Activity.Status.ACTIVE))
                .thenReturn(Optional.of(testActivity));
        when(activityLogService.createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin)))
                .thenReturn(new ActivityLogCreateResponse(1L, LocalDateTime.now(), new BigDecimal("135.0")));
        when(activityLogRepository.findById(1L)).thenReturn(Optional.of(testActivityLog));

        // Act
        VoiceActivityLogResponse response = voiceActivityLogService.processVoiceActivityLog(
                userId, voiceText, authenticatedUserId, isAdmin);

        // Assert
        assertNotNull(response);
        assertEquals("Activity logged successfully", response.getMessage());

        verify(aiVoiceParsingService).parseVoiceText(voiceText);
        verify(activityService, never()).createActivity(any(), any(), anyBoolean());
        verify(activityLogService).createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin));
    }

    @Test
    void processVoiceActivityLog_UserNotFound() {
        // Arrange
        Long userId = 1L;
        String voiceText = "I did a 30-minute brisk walk this morning after breakfast";
        Long authenticatedUserId = 1L;
        boolean isAdmin = false;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            voiceActivityLogService.processVoiceActivityLog(userId, voiceText, authenticatedUserId, isAdmin);
        });

        verify(aiVoiceParsingService, never()).parseVoiceText(any());
        verify(activityService, never()).createActivity(any(), any(), anyBoolean());
        verify(activityLogService, never()).createActivityLog(any(), any(), anyBoolean());
    }

    @Test
    void processVoiceActivityLog_AccessDenied() {
        // Arrange
        Long userId = 2L;
        String voiceText = "I did a 30-minute brisk walk this morning after breakfast";
        Long authenticatedUserId = 1L;
        boolean isAdmin = false;

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            voiceActivityLogService.processVoiceActivityLog(userId, voiceText, authenticatedUserId, isAdmin);
        });

        verify(userRepository, never()).findById(any());
        verify(aiVoiceParsingService, never()).parseVoiceText(any());
        verify(activityService, never()).createActivity(any(), any(), anyBoolean());
        verify(activityLogService, never()).createActivityLog(any(), any(), anyBoolean());
    }

    @Test
    void processVoiceActivityLog_AdminAccessAllowed() {
        // Arrange
        Long userId = 2L;
        String voiceText = "I did a 30-minute brisk walk this morning after breakfast";
        Long authenticatedUserId = 1L;
        boolean isAdmin = true;

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(otherUser));
        when(aiVoiceParsingService.parseVoiceText(voiceText)).thenReturn(parsedData);
        when(activityRepository.findByCreatedByAndNameAndStatus(userId, "Brisk walk", Activity.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(activityService.createActivity(any(ActivityCreateRequest.class), eq(userId), eq(false)))
                .thenReturn(new ActivityCreateResponse(1L, LocalDateTime.now()));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityLogService.createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin)))
                .thenReturn(new ActivityLogCreateResponse(1L, LocalDateTime.now(), new BigDecimal("135.0")));
        when(activityLogRepository.findById(1L)).thenReturn(Optional.of(testActivityLog));

        // Act
        VoiceActivityLogResponse response = voiceActivityLogService.processVoiceActivityLog(
                userId, voiceText, authenticatedUserId, isAdmin);

        // Assert
        assertNotNull(response);
        assertEquals("Activity logged successfully", response.getMessage());

        verify(aiVoiceParsingService).parseVoiceText(voiceText);
        verify(activityService).createActivity(any(ActivityCreateRequest.class), eq(userId), eq(false));
        verify(activityLogService).createActivityLog(any(ActivityLogCreateRequest.class), eq(authenticatedUserId), eq(isAdmin));
    }
}
