package com.healthapp.service;

import com.healthapp.dto.FoodLogCreateRequest;
import com.healthapp.dto.FoodLogCreateResponse;
import com.healthapp.dto.FoodLogPaginatedResponse;
import com.healthapp.dto.FoodLogResponse;
import com.healthapp.dto.FoodLogUpdateRequest;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.FoodLog;

import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.FoodLogRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FoodLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(FoodLogService.class);
    
    @Autowired
    private FoodLogRepository foodLogRepository;
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Optional<FoodLogResponse> getFoodLogById(Long id, Long authenticatedUserId, boolean isAdmin) {
        FoodLog foodLog = foodLogRepository.findByIdAndStatus(id, FoodLog.FoodLogStatus.ACTIVE).orElse(null);
        
        if (foodLog == null) {
            return Optional.empty();
        }
        
        // Check access: user can see their own logs or admin can see all
        if (!isAdmin && !foodLog.getUserId().equals(authenticatedUserId)) {
            return Optional.empty();
        }
        
        // Get food item name
        FoodItem foodItem = foodItemRepository.findById(foodLog.getFoodItemId()).orElse(null);
        String foodItemName = foodItem != null ? foodItem.getName() : "Unknown Food";
        
        return Optional.of(new FoodLogResponse(foodLog, foodItemName));
    }
    
    public FoodLogPaginatedResponse getFoodLogs(Long userId, LocalDateTime from, LocalDateTime to,
                                              String mealType, Integer page, Integer limit, 
                                              String sortBy, String sortDir, Long authenticatedUserId, boolean isAdmin) {
        
        // Validate and normalize parameters
        page = Math.max(1, page);
        limit = Math.min(100, Math.max(1, limit));
        
        // Set default date range if not provided
        if (from == null) {
            from = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0); // Default to last 30 days, start of day
        }
        if (to == null) {
            to = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999); // End of current day
        }
        
        // Validate date range
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
        
        // Validate sortBy parameter
        if (!sortBy.equals("loggedAt") && !sortBy.equals("createdAt")) {
            sortBy = "loggedAt";
        }
        
        // Validate sortDir parameter
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            sortDir = "desc";
        }
        
        // Create pageable with sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        // Determine which user's logs to fetch
        Long targetUserId = (isAdmin && userId != null) ? userId : authenticatedUserId;
        
        // Parse meal type if provided
        FoodLog.MealType mealTypeEnum = null;
        if (mealType != null) {
            try {
                mealTypeEnum = FoodLog.MealType.valueOf(mealType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid meal type: " + mealType);
            }
        }
        
        // Execute query
        Page<FoodLog> foodLogsPage = foodLogRepository.findByUserIdWithFilters(
                targetUserId, from, to, mealTypeEnum, FoodLog.FoodLogStatus.ACTIVE, pageable);
        
        // Convert to response DTOs and calculate totals
        List<FoodLogResponse> items = foodLogsPage.getContent().stream()
                .map(foodLog -> {
                    FoodItem foodItem = foodItemRepository.findById(foodLog.getFoodItemId()).orElse(null);
                    String foodItemName = foodItem != null ? foodItem.getName() : "Unknown Food";
                    return new FoodLogResponse(foodLog, foodItemName);
                })
                .toList();
        
        // Calculate totals
        Double totalCalories = items.stream().mapToDouble(f -> f.getCalories() != null ? f.getCalories() : 0.0).sum();
        Double totalProtein = items.stream().mapToDouble(f -> f.getProtein() != null ? f.getProtein() : 0.0).sum();
        Double totalCarbs = items.stream().mapToDouble(f -> f.getCarbs() != null ? f.getCarbs() : 0.0).sum();
        Double totalFat = items.stream().mapToDouble(f -> f.getFat() != null ? f.getFat() : 0.0).sum();
        Double totalFiber = items.stream().mapToDouble(f -> f.getFiber() != null ? f.getFiber() : 0.0).sum();
        
        return new FoodLogPaginatedResponse(items, page, limit, foodLogsPage.getTotalElements(), 
                                          totalCalories, totalProtein, totalCarbs, totalFat, totalFiber);
    }
    
    public FoodLogCreateResponse createFoodLog(FoodLogCreateRequest request, Long authenticatedUserId) {
        try {
            // Validate user access
            if (!request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create food logs for themselves");
            }
            
            // Validate user exists
            userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Validate food item exists and is accessible
            FoodItem foodItem = foodItemRepository.findByIdAndStatus(request.getFoodItemId(), FoodItem.FoodStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Food item not found"));
            
            // Check if user can access this food item
            if (!foodItem.getCreatedBy().equals(authenticatedUserId) && 
                foodItem.getVisibility() != FoodItem.FoodVisibility.PUBLIC) {
                throw new IllegalArgumentException("Food item not accessible");
            }
            
            // Validate loggedAt time (not more than 10 minutes in the future)
            if (request.getLoggedAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
                throw new IllegalArgumentException("Logged time cannot be more than 10 minutes in the future");
            }
            
            // Parse meal type if provided
            FoodLog.MealType mealTypeEnum = null;
            if (request.getMealType() != null) {
                try {
                    mealTypeEnum = FoodLog.MealType.valueOf(request.getMealType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid meal type: " + request.getMealType());
                }
            }
            
            // Determine unit (use food item's default if not provided)
            String unit = request.getUnit() != null ? request.getUnit() : foodItem.getDefaultUnit();
            
            // Calculate macros
            double scale = request.getQuantity() / foodItem.getQuantityPerUnit();
            Double calories = foodItem.getCaloriesPerUnit() != null ? scale * foodItem.getCaloriesPerUnit() : null;
            Double protein = foodItem.getProteinPerUnit() != null ? scale * foodItem.getProteinPerUnit() : null;
            Double carbs = foodItem.getCarbsPerUnit() != null ? scale * foodItem.getCarbsPerUnit() : null;
            Double fat = foodItem.getFatPerUnit() != null ? scale * foodItem.getFatPerUnit() : null;
            Double fiber = foodItem.getFiberPerUnit() != null ? scale * foodItem.getFiberPerUnit() : null;
            
            // Create food log
            FoodLog foodLog = new FoodLog();
            foodLog.setUserId(request.getUserId());
            foodLog.setFoodItemId(request.getFoodItemId());
            foodLog.setLoggedAt(request.getLoggedAt());
            foodLog.setMealType(mealTypeEnum);
            foodLog.setQuantity(request.getQuantity());
            foodLog.setUnit(unit);
            foodLog.setCalories(calories);
            foodLog.setProtein(protein);
            foodLog.setCarbs(carbs);
            foodLog.setFat(fat);
            foodLog.setFiber(fiber);
            foodLog.setNote(request.getNote());
            
            FoodLog savedFoodLog = foodLogRepository.save(foodLog);
            
            logger.info("Created food log: {} for user: {}", foodItem.getName(), authenticatedUserId);
            
            return new FoodLogCreateResponse(savedFoodLog.getId(), calories, protein, carbs, fat, fiber, savedFoodLog.getCreatedAt());
            
        } catch (Exception e) {
            logger.error("Error creating food log: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, Object> updateFoodLog(Long id, FoodLogUpdateRequest request, Long authenticatedUserId) {
        try {
            FoodLog foodLog = foodLogRepository.findByIdAndStatus(id, FoodLog.FoodLogStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Food log not found"));
            
            // Check ownership
            if (!foodLog.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("You can only update your own food logs");
            }
            
            // Validate loggedAt time if provided
            if (request.getLoggedAt() != null && request.getLoggedAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
                throw new IllegalArgumentException("Logged time cannot be more than 10 minutes in the future");
            }
            
            // Parse meal type if provided
            FoodLog.MealType mealTypeEnum = null;
            if (request.getMealType() != null) {
                try {
                    mealTypeEnum = FoodLog.MealType.valueOf(request.getMealType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid meal type: " + request.getMealType());
                }
            }
            
            // Update fields if provided
            if (request.getLoggedAt() != null) {
                foodLog.setLoggedAt(request.getLoggedAt());
            }
            if (mealTypeEnum != null) {
                foodLog.setMealType(mealTypeEnum);
            }
            if (request.getNote() != null) {
                foodLog.setNote(request.getNote());
            }
            
            // Handle quantity and unit changes (requires recalculation)
            boolean needsRecalculation = false;
            if (request.getQuantity() != null) {
                foodLog.setQuantity(request.getQuantity());
                needsRecalculation = true;
            }
            if (request.getUnit() != null) {
                foodLog.setUnit(request.getUnit());
                needsRecalculation = true;
            }
            
            // Recalculate macros if needed
            if (needsRecalculation) {
                FoodItem foodItem = foodItemRepository.findById(foodLog.getFoodItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Food item not found"));
                
                double scale = foodLog.getQuantity() / foodItem.getQuantityPerUnit();
                foodLog.setCalories(foodItem.getCaloriesPerUnit() != null ? scale * foodItem.getCaloriesPerUnit() : null);
                foodLog.setProtein(foodItem.getProteinPerUnit() != null ? scale * foodItem.getProteinPerUnit() : null);
                foodLog.setCarbs(foodItem.getCarbsPerUnit() != null ? scale * foodItem.getCarbsPerUnit() : null);
                foodLog.setFat(foodItem.getFatPerUnit() != null ? scale * foodItem.getFatPerUnit() : null);
                foodLog.setFiber(foodItem.getFiberPerUnit() != null ? scale * foodItem.getFiberPerUnit() : null);
            }
            
            FoodLog updatedFoodLog = foodLogRepository.save(foodLog);
            
            logger.info("Updated food log: {} for user: {}", updatedFoodLog.getId(), authenticatedUserId);
            
            return Map.of(
                "message", "updated",
                "updatedAt", updatedFoodLog.getUpdatedAt()
            );
            
        } catch (Exception e) {
            logger.error("Error updating food log: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, String> deleteFoodLog(Long id, Long authenticatedUserId) {
        try {
            FoodLog foodLog = foodLogRepository.findByIdAndStatus(id, FoodLog.FoodLogStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Food log not found"));
            
            // Check ownership
            if (!foodLog.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("You can only delete your own food logs");
            }
            
            // Soft delete
            foodLog.setStatus(FoodLog.FoodLogStatus.DELETED);
            foodLogRepository.save(foodLog);
            
            logger.info("Deleted food log: {} for user: {}", foodLog.getId(), authenticatedUserId);
            
            return Map.of("message", "deleted");
            
        } catch (Exception e) {
            logger.error("Error deleting food log: {}", e.getMessage(), e);
            throw e;
        }
    }
}
