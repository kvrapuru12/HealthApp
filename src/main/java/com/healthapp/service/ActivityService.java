package com.healthapp.service;

import com.healthapp.dto.ActivityCreateRequest;
import com.healthapp.dto.ActivityCreateResponse;
import com.healthapp.dto.ActivityPaginatedResponse;
import com.healthapp.dto.ActivityResponse;
import com.healthapp.dto.ActivityUpdateRequest;
import com.healthapp.entity.Activity;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);
    
    @Autowired
    private ActivityRepository activityRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public ActivityCreateResponse createActivity(ActivityCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        // Check if activity name already exists for this user
        Optional<Activity> existingActivity = activityRepository.findByCreatedByAndNameAndStatus(
                authenticatedUserId, request.getName(), Activity.Status.ACTIVE);
        if (existingActivity.isPresent()) {
            throw new IllegalArgumentException("Activity with name '" + request.getName() + "' already exists");
        }
        
        // Get user
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Create activity
        Activity activity = new Activity(
                request.getName(),
                request.getCategory(),
                request.getCaloriesPerMinute(),
                request.getVisibility(),
                user
        );
        
        Activity savedActivity = activityRepository.save(activity);
        
        logger.info("Created activity with ID {} for user {}", savedActivity.getId(), authenticatedUserId);
        
        return new ActivityCreateResponse(savedActivity.getId(), savedActivity.getCreatedAt());
    }
    
    public Optional<ActivityResponse> getActivityById(Long id, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity ID");
        }
        
        Optional<Activity> activityOpt = activityRepository.findByIdAndStatusAndAccessible(id, Activity.Status.ACTIVE, authenticatedUserId);
        
        return activityOpt.map(ActivityResponse::new);
    }
    
    public ActivityPaginatedResponse getActivities(String search, String visibility, Integer page, Integer limit, 
                                                 String sortBy, String sortDir, Long authenticatedUserId, boolean isAdmin) {
        
        // Validate parameters
        if (page == null || page < 1) page = 1;
        if (limit == null || limit < 1) limit = 20;
        if (limit > 100) limit = 100;
        if (sortBy == null || (!sortBy.equals("name") && !sortBy.equals("createdAt"))) sortBy = "createdAt";
        if (sortDir == null || (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc"))) sortDir = "desc";
        
        // Create pageable
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        Page<Activity> activityPage;
        
        // Build query based on parameters
        if (search != null && !search.trim().isEmpty()) {
            if (visibility != null && !visibility.trim().isEmpty()) {
                Activity.Visibility vis = Activity.Visibility.valueOf(visibility.toUpperCase());
                activityPage = activityRepository.findByStatusAndVisibilityAndSearch(Activity.Status.ACTIVE, vis, search.trim(), pageable);
            } else {
                activityPage = activityRepository.findByStatusAndSearch(Activity.Status.ACTIVE, search.trim(), pageable);
            }
        } else if (visibility != null && !visibility.trim().isEmpty()) {
            Activity.Visibility vis = Activity.Visibility.valueOf(visibility.toUpperCase());
            activityPage = activityRepository.findByStatusAndVisibility(Activity.Status.ACTIVE, vis, pageable);
        } else {
            // Default: show user's activities and public activities
            activityPage = activityRepository.findByUserOrPublicAndStatus(authenticatedUserId, Activity.Status.ACTIVE, pageable);
        }
        
        List<ActivityResponse> activities = activityPage.getContent().stream()
                .map(ActivityResponse::new)
                .collect(Collectors.toList());
        
        return new ActivityPaginatedResponse(activities, page, limit, activityPage.getTotalElements());
    }
    
    public Map<String, Object> updateActivity(Long id, ActivityUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity ID");
        }
        
        Activity activity = activityRepository.findByIdAndStatus(id, Activity.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        
        // Check ownership
        if (!isAdmin && !activity.getCreatedBy().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Access denied: You can only update your own activities");
        }
        
        boolean updated = false;
        
        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            // Check for name uniqueness
            Optional<Activity> existingActivity = activityRepository.findByCreatedByAndNameAndIdNotAndStatus(
                    authenticatedUserId, request.getName(), id, Activity.Status.ACTIVE);
            if (existingActivity.isPresent()) {
                throw new IllegalArgumentException("Activity with name '" + request.getName() + "' already exists");
            }
            activity.setName(request.getName().trim());
            updated = true;
        }
        
        if (request.getCategory() != null) {
            activity.setCategory(request.getCategory().trim());
            updated = true;
        }
        
        if (request.getCaloriesPerMinute() != null) {
            activity.setCaloriesPerMinute(request.getCaloriesPerMinute());
            updated = true;
        }
        
        if (request.getVisibility() != null) {
            activity.setVisibility(request.getVisibility());
            updated = true;
        }
        
        if (updated) {
            activityRepository.save(activity);
            logger.info("Updated activity with ID {} by user {}", id, authenticatedUserId);
        }
        
        return Map.of(
                "message", "updated",
                "updatedAt", activity.getUpdatedAt()
        );
    }
    
    public Map<String, String> deleteActivity(Long id, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity ID");
        }
        
        Activity activity = activityRepository.findByIdAndStatus(id, Activity.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        
        // Check ownership
        if (!isAdmin && !activity.getCreatedBy().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Access denied: You can only delete your own activities");
        }
        
        activity.setStatus(Activity.Status.DELETED);
        activityRepository.save(activity);
        
        logger.info("Deleted activity with ID {} by user {}", id, authenticatedUserId);
        
        return Map.of("message", "deleted");
    }
}
