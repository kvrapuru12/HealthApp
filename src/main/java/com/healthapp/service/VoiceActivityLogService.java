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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoiceActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceActivityLogService.class);

    @Autowired(required = false)
    private AiActivityVoiceParsingService aiActivityVoiceParsingService;

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

        // Check if AI service is available
        if (aiActivityVoiceParsingService == null) {
            throw new RuntimeException("AI voice parsing service is not available. Please configure OpenAI API key.");
        }

        List<AiActivityVoiceParsingService.ParsedActivityData> parsedActivities =
                aiActivityVoiceParsingService.parseAllActivities(voiceText);

        List<VoiceActivityLogResponse.ActivityLogSummary> summaries = new ArrayList<>();
        for (AiActivityVoiceParsingService.ParsedActivityData parsedData : parsedActivities) {
            summaries.add(createActivityLogFromParsedData(parsedData, userId, authenticatedUserId, isAdmin));
        }

        String message = summaries.size() == 1
                ? "Activity logged successfully"
                : String.format("Logged %d activities from voice input", summaries.size());
        logger.info("Successfully processed {} voice activity log(s)", summaries.size());
        return new VoiceActivityLogResponse(message, summaries);
    }

    private VoiceActivityLogResponse.ActivityLogSummary createActivityLogFromParsedData(
            AiActivityVoiceParsingService.ParsedActivityData parsedData,
            Long userId, Long authenticatedUserId, boolean isAdmin) {
        Activity activity = findOrCreateActivity(parsedData.getActivityName(), userId);

        ActivityLogCreateRequest logRequest = new ActivityLogCreateRequest();
        logRequest.setUserId(userId);
        logRequest.setActivityId(activity.getId());
        LocalDateTime loggedAt = parsedData.getLoggedAt() != null ? parsedData.getLoggedAt() : LocalDateTime.now();
        if (loggedAt.isAfter(LocalDateTime.now().plusMinutes(10))) {
            logger.warn("Clamping AI activity loggedAt {} to now (future time rejected by activity log rules)", loggedAt);
            loggedAt = LocalDateTime.now();
        }
        logRequest.setLoggedAt(loggedAt);
        logRequest.setDurationMinutes(parsedData.getDurationMinutes());
        logRequest.setNote(parsedData.getNote());

        ActivityLogCreateResponse logResponse = activityLogService.createActivityLog(logRequest, authenticatedUserId, isAdmin);

        ActivityLog activityLog = activityLogRepository.findById(logResponse.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created activity log"));

        logger.info("Successfully processed voice activity log: {}", activityLog.getId());
        return new VoiceActivityLogResponse.ActivityLogSummary(
                activityLog.getId(),
                activity.getName(),
                activityLog.getDurationMinutes(),
                activityLog.getCaloriesBurned() != null ? activityLog.getCaloriesBurned().doubleValue() : null,
                activityLog.getLoggedAt(),
                activityLog.getNote()
        );
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
        activityRequest.setCategory(determineActivityCategory(activityName));
        activityRequest.setCaloriesPerMinute(
                ActivityCalorieEstimator.estimateCaloriesPerMinute(activityName, activityRequest.getCategory()));

        ActivityCreateResponse activityResponse = activityService.createActivity(activityRequest, userId, false);
        
        // Get the created activity
        Activity newActivity = activityRepository.findById(activityResponse.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created activity"));
        
        logger.info("Created new activity with ID: {}", newActivity.getId());

        return newActivity;
    }

    /**
     * Determines the appropriate category for an activity based on its name
     */
    private String determineActivityCategory(String activityName) {
        String name = activityName.toLowerCase();
        
        // Cardio activities
        if (name.contains("hiit") || name.contains("interval") || name.contains("tabata")
                || name.contains("crossfit") || name.contains("circuit")) {
            return "cardio";
        }
        if (name.contains("walk") || name.contains("run") || name.contains("jog") || 
            name.contains("cycle") || name.contains("bike") || name.contains("row") ||
            name.contains("elliptical") || name.contains("dance") || name.contains("zumba")) {
            return "cardio";
        }
        
        // Sports activities
        if (name.contains("swim") || name.contains("tennis") || name.contains("basketball") ||
            name.contains("soccer") || name.contains("football") || name.contains("golf") ||
            name.contains("ski") || name.contains("badminton")) {
            return "sports";
        }
        
        // Strength training
        if (name.contains("weight") || name.contains("strength") || name.contains("lift") ||
            name.contains("gym") || name.contains("muscle")) {
            return "strength";
        }
        
        // Flexibility activities
        if (name.contains("yoga") || name.contains("pilates") || name.contains("stretch")) {
            return "flexibility";
        }
        
        // Outdoor activities
        if (name.contains("hike") || name.contains("trail") || name.contains("climb")) {
            return "outdoor";
        }
        
        // Home activities
        if (name.contains("garden") || name.contains("clean") || name.contains("housework")) {
            return "home";
        }
        
        // Default category
        return "general";
    }
}
