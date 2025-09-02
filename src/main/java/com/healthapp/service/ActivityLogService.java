package com.healthapp.service;

import com.healthapp.dto.ActivityLogCreateRequest;
import com.healthapp.dto.ActivityLogCreateResponse;
import com.healthapp.dto.ActivityLogPaginatedResponse;
import com.healthapp.dto.ActivityLogResponse;
import com.healthapp.dto.ActivityLogUpdateRequest;
import com.healthapp.entity.Activity;
import com.healthapp.entity.ActivityLog;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityLogRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    @Autowired
    private ActivityRepository activityRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public ActivityLogCreateResponse createActivityLog(ActivityLogCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        // Validate user access
        if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("Users can only create activity logs for themselves");
        }
        
        // Validate loggedAt time
        if (request.getLoggedAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
            throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
        }
        
        // Get user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Get activity and validate access
        Activity activity = activityRepository.findByIdAndStatusAndAccessible(
                request.getActivityId(), Activity.Status.ACTIVE, authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found or not accessible"));
        
        // Create activity log
        ActivityLog activityLog = new ActivityLog(user, activity, request.getLoggedAt(), request.getDurationMinutes(), request.getNote());
        
        ActivityLog savedActivityLog = activityLogRepository.save(activityLog);
        
        logger.info("Created activity log with ID {} for user {}", savedActivityLog.getId(), authenticatedUserId);
        
        return new ActivityLogCreateResponse(savedActivityLog.getId(), savedActivityLog.getCreatedAt(), savedActivityLog.getCaloriesBurned());
    }
    
    public Optional<ActivityLogResponse> getActivityLogById(Long id, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity log ID");
        }
        
        Optional<ActivityLog> activityLogOpt = activityLogRepository.findByIdAndStatusAndUser(id, ActivityLog.Status.ACTIVE, authenticatedUserId);
        
        return activityLogOpt.map(ActivityLogResponse::new);
    }
    
    public ActivityLogPaginatedResponse getActivityLogs(Long userId, LocalDateTime from, LocalDateTime to, Integer page, Integer limit, 
                                                       String sortBy, String sortDir, Long authenticatedUserId, boolean isAdmin) {
        
        // Validate parameters
        if (page == null || page < 1) page = 1;
        if (limit == null || limit < 1) limit = 20;
        if (limit > 100) limit = 100;
        if (sortBy == null || (!sortBy.equals("loggedAt") && !sortBy.equals("createdAt"))) sortBy = "loggedAt";
        if (sortDir == null || (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc"))) sortDir = "desc";
        
        // Validate date range
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before or equal to to date");
        }
        
        // Regular users can only see their own entries
        if (!isAdmin && userId != null && !userId.equals(authenticatedUserId)) {
            throw new SecurityException("Access denied: Users can only view their own activity logs");
        }
        
        // If no userId specified and not admin, use authenticated user's ID
        if (userId == null && !isAdmin) {
            userId = authenticatedUserId;
        }
        
        // Create pageable
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        Page<ActivityLog> activityLogPage;
        
        // Build query based on date range
        if (from != null && to != null) {
            activityLogPage = activityLogRepository.findByUserAndStatusAndDateRange(userId, ActivityLog.Status.ACTIVE, from, to, pageable);
        } else if (from != null) {
            activityLogPage = activityLogRepository.findByUserAndStatusAndFromDate(userId, ActivityLog.Status.ACTIVE, from, pageable);
        } else if (to != null) {
            activityLogPage = activityLogRepository.findByUserAndStatusAndToDate(userId, ActivityLog.Status.ACTIVE, to, pageable);
        } else {
            activityLogPage = activityLogRepository.findByUserAndStatus(userId, ActivityLog.Status.ACTIVE, pageable);
        }
        
        List<ActivityLogResponse> activityLogs = activityLogPage.getContent().stream()
                .map(ActivityLogResponse::new)
                .collect(Collectors.toList());
        
        return new ActivityLogPaginatedResponse(activityLogs, page, limit, activityLogPage.getTotalElements());
    }
    
    public Map<String, Object> updateActivityLog(Long id, ActivityLogUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity log ID");
        }
        
        ActivityLog activityLog = activityLogRepository.findByIdAndStatusAndUser(id, ActivityLog.Status.ACTIVE, authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Activity log not found"));
        
        boolean updated = false;
        
        // Update fields if provided
        if (request.getLoggedAt() != null) {
            if (request.getLoggedAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
                throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
            }
            activityLog.setLoggedAt(request.getLoggedAt());
            updated = true;
        }
        
        if (request.getDurationMinutes() != null) {
            activityLog.setDurationMinutes(request.getDurationMinutes());
            
            // Recalculate calories burned if activity has calories per minute
            Activity activity = activityLog.getActivity();
            if (activity.getCaloriesPerMinute() != null) {
                BigDecimal caloriesBurned = activity.getCaloriesPerMinute()
                        .multiply(BigDecimal.valueOf(request.getDurationMinutes()));
                activityLog.setCaloriesBurned(caloriesBurned);
            }
            updated = true;
        }
        
        if (request.getNote() != null) {
            activityLog.setNote(request.getNote());
            updated = true;
        }
        
        if (updated) {
            activityLogRepository.save(activityLog);
            logger.info("Updated activity log with ID {} by user {}", id, authenticatedUserId);
        }
        
        return Map.of(
                "message", "updated",
                "updatedAt", activityLog.getUpdatedAt()
        );
    }
    
    public Map<String, String> deleteActivityLog(Long id, Long authenticatedUserId, boolean isAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid activity log ID");
        }
        
        ActivityLog activityLog = activityLogRepository.findByIdAndStatusAndUser(id, ActivityLog.Status.ACTIVE, authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Activity log not found"));
        
        activityLog.setStatus(ActivityLog.Status.DELETED);
        activityLogRepository.save(activityLog);
        
        logger.info("Deleted activity log with ID {} by user {}", id, authenticatedUserId);
        
        return Map.of("message", "deleted");
    }
}
