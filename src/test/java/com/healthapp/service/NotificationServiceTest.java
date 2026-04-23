package com.healthapp.service;

import com.healthapp.dto.NotificationDeviceRegisterRequest;
import com.healthapp.dto.NotificationPreferencesPatchRequest;
import com.healthapp.dto.NotificationTestRequest;
import com.healthapp.entity.NotificationDevice;
import com.healthapp.entity.NotificationPreference;
import com.healthapp.entity.User;
import com.healthapp.repository.NotificationDeviceRepository;
import com.healthapp.repository.NotificationPreferenceRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationServiceTest {

    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void registerDevice_createsOrUpdatesDevice() {
        NotificationDeviceRegisterRequest request = new NotificationDeviceRegisterRequest();
        request.setDeviceId("ios-device-1");
        request.setExpoPushToken("ExponentPushToken[test]");
        request.setPlatform("ios");
        request.setTimezone("America/New_York");

        NotificationDevice savedDevice = new NotificationDevice();
        savedDevice.setId(10L);
        savedDevice.setUserId(1L);
        savedDevice.setDeviceId("ios-device-1");
        savedDevice.setExpoPushToken("ExponentPushToken[test]");
        savedDevice.setPlatform("ios");
        savedDevice.setTimezone("America/New_York");
        savedDevice.setStatus(NotificationDevice.Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationDeviceRepository.findByExpoPushToken("ExponentPushToken[test]")).thenReturn(Optional.empty());
        when(notificationDeviceRepository.findByUserIdAndDeviceId(1L, "ios-device-1")).thenReturn(Optional.empty());
        when(notificationDeviceRepository.save(any(NotificationDevice.class))).thenReturn(savedDevice);
        when(notificationPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        NotificationPreference savedPreference = new NotificationPreference();
        savedPreference.setUserId(1L);
        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenReturn(savedPreference);

        var response = notificationService.registerDevice(1L, request);

        assertNotNull(response);
        assertEquals("ios-device-1", response.getDeviceId());
        verify(notificationDeviceRepository, atLeastOnce()).save(any(NotificationDevice.class));
    }

    @Test
    void updatePreferences_updatesProvidedFieldsOnly() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(1L);
        preference.setFoodReminderEnabled(true);
        preference.setActivityReminderEnabled(true);
        preference.setCyclePhaseReminderEnabled(true);
        preference.setReminderTime(LocalTime.of(20, 0));
        preference.setTimezone("UTC");

        NotificationPreferencesPatchRequest request = new NotificationPreferencesPatchRequest();
        request.setFoodReminderEnabled(false);
        request.setReminderTime("19:45");
        request.setTimezone("Asia/Kolkata");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(preference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenReturn(preference);

        var response = notificationService.updatePreferences(1L, request);

        assertNotNull(response);
        assertFalse(response.getFoodReminderEnabled());
        assertEquals("19:45:00", response.getReminderTime());
        assertEquals("Asia/Kolkata", response.getTimezone());
    }

    @Test
    void sendTestNotification_reportsActiveDeviceCount() {
        NotificationTestRequest request = new NotificationTestRequest();
        request.setTitle("Hello");
        request.setBody("Test");
        request.setType("test");

        NotificationDevice active = new NotificationDevice();
        active.setUserId(1L);
        active.setDeviceId("d1");
        active.setStatus(NotificationDevice.Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationDeviceRepository.findByUserIdAndStatus(1L, NotificationDevice.Status.ACTIVE))
                .thenReturn(List.of(active));

        var response = notificationService.sendTestNotification(1L, request);

        assertEquals(1, response.get("targetDeviceCount"));
        assertEquals("test notification accepted", response.get("message"));
    }

    @Test
    void sendTestNotification_throwsWhenUserMissing() {
        NotificationTestRequest request = new NotificationTestRequest();
        request.setTitle("Hello");
        request.setBody("Test");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> notificationService.sendTestNotification(99L, request));

        assertEquals("User not found", ex.getMessage());
        verify(notificationDeviceRepository, never()).findByUserIdAndStatus(anyLong(), any());
    }
}
