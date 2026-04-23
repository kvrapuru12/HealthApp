package com.healthapp.service;

import com.healthapp.dto.*;
import com.healthapp.entity.NotificationDevice;
import com.healthapp.entity.NotificationPreference;
import com.healthapp.repository.NotificationDeviceRepository;
import com.healthapp.repository.NotificationPreferenceRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationDeviceRepository notificationDeviceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository) {
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userRepository = userRepository;
    }

    public NotificationDeviceResponse registerDevice(Long userId, NotificationDeviceRegisterRequest request) {
        ensureUserExists(userId);
        validateTimezone(request.getTimezone());

        // Keep token ownership unique so one push token maps to one active user/device.
        notificationDeviceRepository.findByExpoPushToken(request.getExpoPushToken())
                .ifPresent(existingByToken -> {
                    if (!existingByToken.getUserId().equals(userId)
                            || !existingByToken.getDeviceId().equals(request.getDeviceId())) {
                        existingByToken.setStatus(NotificationDevice.Status.INACTIVE);
                        notificationDeviceRepository.save(existingByToken);
                    }
                });

        NotificationDevice device = notificationDeviceRepository.findByUserIdAndDeviceId(userId, request.getDeviceId())
                .orElseGet(NotificationDevice::new);

        device.setUserId(userId);
        device.setDeviceId(request.getDeviceId());
        device.setExpoPushToken(request.getExpoPushToken());
        device.setPlatform(request.getPlatform().trim().toLowerCase());
        device.setAppVersion(request.getAppVersion());
        device.setBuildNumber(request.getBuildNumber());
        device.setTimezone(request.getTimezone());
        device.setStatus(NotificationDevice.Status.ACTIVE);
        device.setLastSeenAt(LocalDateTime.now());

        NotificationDevice saved = notificationDeviceRepository.save(device);
        upsertTimezoneOnPreferences(userId, request.getTimezone());

        logger.info("Registered notification device {} for user {}", saved.getDeviceId(), userId);
        return toDeviceResponse(saved);
    }

    public Map<String, String> deleteDevice(Long userId, String deviceId) {
        NotificationDevice device = notificationDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Notification device not found"));

        device.setStatus(NotificationDevice.Status.INACTIVE);
        notificationDeviceRepository.save(device);

        logger.info("Deactivated notification device {} for user {}", deviceId, userId);
        return Map.of("message", "device deregistered");
    }

    /** Not read-only: getOrCreatePreference may INSERT for new users (class @Transactional applies). */
    public NotificationPreferencesResponse getPreferences(Long userId) {
        ensureUserExists(userId);
        NotificationPreference preference = getOrCreatePreference(userId);
        return toPreferenceResponse(preference);
    }

    @SuppressWarnings("null")
    public NotificationPreferencesResponse updatePreferences(Long userId, NotificationPreferencesPatchRequest request) {
        ensureUserExists(userId);
        NotificationPreference preference = getOrCreatePreference(userId);

        if (request.getFoodReminderEnabled() != null) {
            preference.setFoodReminderEnabled(request.getFoodReminderEnabled());
        }
        if (request.getActivityReminderEnabled() != null) {
            preference.setActivityReminderEnabled(request.getActivityReminderEnabled());
        }
        if (request.getCyclePhaseReminderEnabled() != null) {
            preference.setCyclePhaseReminderEnabled(request.getCyclePhaseReminderEnabled());
        }
        if (request.getReminderTime() != null) {
            preference.setReminderTime(parseTime(request.getReminderTime(), "reminderTime"));
        }
        if (request.getQuietHoursStart() != null) {
            preference.setQuietHoursStart(parseTime(request.getQuietHoursStart(), "quietHoursStart"));
        }
        if (request.getQuietHoursEnd() != null) {
            preference.setQuietHoursEnd(parseTime(request.getQuietHoursEnd(), "quietHoursEnd"));
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            validateTimezone(request.getTimezone());
            preference.setTimezone(request.getTimezone());
        }

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        logger.info("Updated notification preferences for user {}", userId);
        return toPreferenceResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sendTestNotification(Long userId, NotificationTestRequest request) {
        ensureUserExists(userId);
        List<NotificationDevice> activeDevices = notificationDeviceRepository
                .findByUserIdAndStatus(userId, NotificationDevice.Status.ACTIVE);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "test notification accepted");
        response.put("userId", userId);
        response.put("title", request.getTitle());
        response.put("body", request.getBody());
        response.put("type", request.getType() == null ? "test" : request.getType());
        response.put("targetScreen", request.getTargetScreen());
        response.put("targetDeviceCount", activeDevices.size());
        response.put("queuedAt", LocalDateTime.now());
        return response;
    }

    private NotificationPreference getOrCreatePreference(Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUserId(userId);
                    return notificationPreferenceRepository.save(preference);
                });
    }

    private void upsertTimezoneOnPreferences(Long userId, String timezone) {
        NotificationPreference preference = getOrCreatePreference(userId);
        preference.setTimezone(timezone);
        notificationPreferenceRepository.save(preference);
    }

    private void ensureUserExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            if (value.length() == 5) {
                return LocalTime.parse(value + ":00", TIME_FORMAT);
            }
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " format");
        }
    }

    private NotificationDeviceResponse toDeviceResponse(NotificationDevice device) {
        NotificationDeviceResponse response = new NotificationDeviceResponse();
        response.setId(device.getId());
        response.setDeviceId(device.getDeviceId());
        response.setExpoPushToken(device.getExpoPushToken());
        response.setPlatform(device.getPlatform());
        response.setAppVersion(device.getAppVersion());
        response.setBuildNumber(device.getBuildNumber());
        response.setTimezone(device.getTimezone());
        response.setStatus(device.getStatus().name());
        response.setLastSeenAt(device.getLastSeenAt());
        return response;
    }

    private NotificationPreferencesResponse toPreferenceResponse(NotificationPreference preference) {
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        response.setFoodReminderEnabled(preference.getFoodReminderEnabled());
        response.setActivityReminderEnabled(preference.getActivityReminderEnabled());
        response.setCyclePhaseReminderEnabled(preference.getCyclePhaseReminderEnabled());
        response.setReminderTime(preference.getReminderTime() == null ? null : preference.getReminderTime().format(TIME_FORMAT));
        response.setQuietHoursStart(preference.getQuietHoursStart() == null ? null : preference.getQuietHoursStart().format(TIME_FORMAT));
        response.setQuietHoursEnd(preference.getQuietHoursEnd() == null ? null : preference.getQuietHoursEnd().format(TIME_FORMAT));
        response.setTimezone(preference.getTimezone());
        return response;
    }
}
