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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoiceActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceActivityLogService.class);

    @Autowired
    private AiVoiceParsingService aiVoiceParsingService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public VoiceActivityLogResponse processVoiceActivityLog(Long userId, String voiceText, Long authenticatedUserId, boolean isAdmin) {
        logger.info("Processing voice activity log for user {}: {}", userId, voiceText);

        // Validate user access
        validateUserAccess(userId, authenticatedUserId, isAdmin);

        // Parse voice text using AI
        AiVoiceParsingService.ParsedActivityData parsedData = aiVoiceParsingService.parseVoiceText(voiceText);

        // Find or create activity
        Activity activity = findOrCreateActivity(parsedData.getActivityName(), userId);

        // Create activity log
        ActivityLogCreateRequest logRequest = new ActivityLogCreateRequest();
        logRequest.setUserId(userId);
        logRequest.setActivityId(activity.getId());
        logRequest.setLoggedAt(parsedData.getLoggedAt());
        logRequest.setDurationMinutes(parsedData.getDurationMinutes());
        logRequest.setNote(parsedData.getNote());

        ActivityLogCreateResponse logResponse = activityLogService.createActivityLog(logRequest, authenticatedUserId, isAdmin);

        // Get the created activity log for response
        ActivityLog activityLog = activityLogRepository.findById(logResponse.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created activity log"));

                            // Build response
                    VoiceActivityLogResponse.ActivityLogSummary summary = new VoiceActivityLogResponse.ActivityLogSummary(
                            activityLog.getId(),
                            activity.getName(),
                            activityLog.getDurationMinutes(),
                            activityLog.getCaloriesBurned() != null ? activityLog.getCaloriesBurned().doubleValue() : null,
                            activityLog.getLoggedAt(),
                            activityLog.getNote()
                    );

        logger.info("Successfully processed voice activity log: {}", activityLog.getId());
        return new VoiceActivityLogResponse("Activity logged successfully", summary);
    }

    private void validateUserAccess(Long userId, Long authenticatedUserId, boolean isAdmin) {
        if (!isAdmin && !userId.equals(authenticatedUserId)) {
            throw new SecurityException("Users can only log activities for themselves");
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
    }

    private Activity findOrCreateActivity(String activityName, Long userId) {
        logger.debug("Looking for existing activity: {}", activityName);

        // Try to find existing activity for this user
        Optional<Activity> existingActivity = activityRepository.findByCreatedByAndNameAndStatus(
                userId, activityName, Activity.Status.ACTIVE);

        if (existingActivity.isPresent()) {
            logger.debug("Found existing activity: {}", existingActivity.get().getId());
            return existingActivity.get();
        }

        // Try to find public activity with same name (using a simple search)
        // We'll search for activities with the name and check if any are public
        List<Activity> activities = activityRepository.findByCreatedByAndStatus(userId, Activity.Status.ACTIVE);
        Optional<Activity> publicActivity = activities.stream()
                .filter(a -> a.getName().equalsIgnoreCase(activityName) && a.getVisibility() == Activity.Visibility.PUBLIC)
                .findFirst();

        if (publicActivity.isPresent()) {
            logger.debug("Found public activity: {}", publicActivity.get().getId());
            return publicActivity.get();
        }

                            // Create new activity
                    logger.info("Creating new activity: {}", activityName);
                    ActivityCreateRequest activityRequest = new ActivityCreateRequest();
                    activityRequest.setName(activityName);
                    activityRequest.setVisibility(Activity.Visibility.PRIVATE);
                    // Set a default calories per minute for new activities
                    activityRequest.setCaloriesPerMinute(new BigDecimal("3.0")); // Default moderate activity

        ActivityCreateResponse activityResponse = activityService.createActivity(activityRequest, userId, false);
        
        // Get the created activity
        Activity newActivity = activityRepository.findById(activityResponse.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created activity"));
        
        logger.info("Created new activity with ID: {}", newActivity.getId());

        return newActivity;
    }
}
