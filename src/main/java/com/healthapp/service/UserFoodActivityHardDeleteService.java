package com.healthapp.service;

import com.healthapp.dto.UserFoodActivityHardDeleteResponse;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityLogRepository;
import com.healthapp.repository.ActivityRepository;
import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.FoodLogRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserFoodActivityHardDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(UserFoodActivityHardDeleteService.class);

    private final UserRepository userRepository;
    private final FoodLogRepository foodLogRepository;
    private final FoodItemRepository foodItemRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityRepository activityRepository;

    public UserFoodActivityHardDeleteService(
            UserRepository userRepository,
            FoodLogRepository foodLogRepository,
            FoodItemRepository foodItemRepository,
            ActivityLogRepository activityLogRepository,
            ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.foodLogRepository = foodLogRepository;
        this.foodItemRepository = foodItemRepository;
        this.activityLogRepository = activityLogRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * Physically removes all food logs and activity logs for the user, then removes food items and activities
     * they created only when no remaining logs reference those rows (so shared public templates stay if in use).
     */
    @Transactional
    public UserFoodActivityHardDeleteResponse hardDeleteFoodAndActivityDataByEmail(String email) {
        String trimmed = email == null ? "" : email.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        User user = userRepository.findByEmail(trimmed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for email"));

        Long userId = user.getId();

        int foodLogsDeleted = foodLogRepository.deleteAllByUserId(userId);
        int foodItemsDeleted = foodItemRepository.deleteOwnedFoodItemsWithNoReferencingLogs(userId);
        int activityLogsDeleted = activityLogRepository.deleteAllByUserId(userId);
        int activitiesDeleted = activityRepository.deleteOwnedActivitiesWithNoReferencingLogs(userId);

        logger.warn(
                "Hard-delete food/activity data for userId={} email={}: foodLogs={}, foodItems={}, activityLogs={}, activities={}",
                userId, trimmed, foodLogsDeleted, foodItemsDeleted, activityLogsDeleted, activitiesDeleted);

        return new UserFoodActivityHardDeleteResponse(
                trimmed, userId, foodLogsDeleted, foodItemsDeleted, activityLogsDeleted, activitiesDeleted);
    }
}
