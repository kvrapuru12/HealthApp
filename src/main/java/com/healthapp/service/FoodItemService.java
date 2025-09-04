package com.healthapp.service;

import com.healthapp.dto.FoodItemCreateRequest;
import com.healthapp.dto.FoodItemCreateResponse;
import com.healthapp.dto.FoodItemPaginatedResponse;
import com.healthapp.dto.FoodItemResponse;
import com.healthapp.dto.FoodItemUpdateRequest;
import com.healthapp.entity.FoodItem;

import com.healthapp.repository.FoodItemRepository;
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


import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FoodItemService {
    
    private static final Logger logger = LoggerFactory.getLogger(FoodItemService.class);
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Optional<FoodItemResponse> getFoodItemById(Long id, Long authenticatedUserId, boolean isAdmin) {
        FoodItem foodItem = foodItemRepository.findByIdAndStatus(id, FoodItem.FoodStatus.ACTIVE).orElse(null);
        
        if (foodItem == null) {
            return Optional.empty();
        }
        
        // Check access: user can see their own items or public items
        if (!isAdmin && !foodItem.getCreatedBy().equals(authenticatedUserId) && 
            foodItem.getVisibility() != FoodItem.FoodVisibility.PUBLIC) {
            return Optional.empty();
        }
        
        return Optional.of(new FoodItemResponse(foodItem));
    }
    
    public FoodItemPaginatedResponse getFoodItems(Long authenticatedUserId, String search, String visibility,
                                                Integer page, Integer limit, String sortBy, String sortDir) {
        
        // Validate and normalize parameters
        page = Math.max(1, page);
        limit = Math.min(100, Math.max(1, limit));
        
        // Validate sortBy parameter
        if (!sortBy.equals("name") && !sortBy.equals("createdAt")) {
            sortBy = "createdAt";
        }
        
        // Validate sortDir parameter
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            sortDir = "desc";
        }
        
        // Create pageable with sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        // Execute query
        Page<FoodItem> foodItemsPage;
        if (visibility != null) {
            FoodItem.FoodVisibility visibilityEnum = FoodItem.FoodVisibility.valueOf(visibility.toUpperCase());
            foodItemsPage = foodItemRepository.findByVisibilityAndCreatedBy(
                    authenticatedUserId, visibilityEnum, FoodItem.FoodStatus.ACTIVE, pageable);
        } else {
            foodItemsPage = foodItemRepository.findBySearchCriteria(
                    authenticatedUserId, search, FoodItem.FoodStatus.ACTIVE, pageable);
        }
        
        // Convert to response DTOs
        List<FoodItemResponse> items = foodItemsPage.getContent().stream()
                .map(FoodItemResponse::new)
                .toList();
        
        return new FoodItemPaginatedResponse(items, page, limit, foodItemsPage.getTotalElements());
    }
    
    public FoodItemCreateResponse createFoodItem(FoodItemCreateRequest request, Long authenticatedUserId) {
        try {
            // Validate user exists
            userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if food item name already exists for this user
            if (foodItemRepository.existsByNameAndCreatedByAndStatusNot(
                    request.getName(), authenticatedUserId, FoodItem.FoodStatus.DELETED)) {
                throw new IllegalArgumentException("A food item with this name already exists");
            }
            
            // Create food item
            FoodItem foodItem = new FoodItem();
            foodItem.setName(request.getName());
            foodItem.setCategory(request.getCategory());
            foodItem.setDefaultUnit(request.getDefaultUnit());
            foodItem.setQuantityPerUnit(request.getQuantityPerUnit());
            foodItem.setCaloriesPerUnit(request.getCaloriesPerUnit());
            foodItem.setProteinPerUnit(request.getProteinPerUnit());
            foodItem.setCarbsPerUnit(request.getCarbsPerUnit());
            foodItem.setFatPerUnit(request.getFatPerUnit());
            foodItem.setFiberPerUnit(request.getFiberPerUnit());
            foodItem.setVisibility(FoodItem.FoodVisibility.valueOf(request.getVisibility().toUpperCase()));
            foodItem.setCreatedBy(authenticatedUserId);
            
            FoodItem savedFoodItem = foodItemRepository.save(foodItem);
            
            logger.info("Created food item: {} for user: {}", savedFoodItem.getName(), authenticatedUserId);
            
            return new FoodItemCreateResponse(savedFoodItem.getId(), savedFoodItem.getCreatedAt());
            
        } catch (Exception e) {
            logger.error("Error creating food item: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, Object> updateFoodItem(Long id, FoodItemUpdateRequest request, Long authenticatedUserId) {
        try {
            FoodItem foodItem = foodItemRepository.findByIdAndStatus(id, FoodItem.FoodStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Food item not found"));
            
            // Check ownership
            if (!foodItem.getCreatedBy().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("You can only update your own food items");
            }
            
            // Check if name is being changed and if it conflicts
            if (request.getName() != null && !request.getName().equals(foodItem.getName())) {
                if (foodItemRepository.existsByNameAndCreatedByAndStatusNot(
                        request.getName(), authenticatedUserId, FoodItem.FoodStatus.DELETED)) {
                    throw new IllegalArgumentException("A food item with this name already exists");
                }
                foodItem.setName(request.getName());
            }
            
            // Update other fields if provided
            if (request.getCategory() != null) {
                foodItem.setCategory(request.getCategory());
            }
            if (request.getDefaultUnit() != null) {
                foodItem.setDefaultUnit(request.getDefaultUnit());
            }
            if (request.getQuantityPerUnit() != null) {
                foodItem.setQuantityPerUnit(request.getQuantityPerUnit());
            }
            if (request.getCaloriesPerUnit() != null) {
                foodItem.setCaloriesPerUnit(request.getCaloriesPerUnit());
            }
            if (request.getProteinPerUnit() != null) {
                foodItem.setProteinPerUnit(request.getProteinPerUnit());
            }
            if (request.getCarbsPerUnit() != null) {
                foodItem.setCarbsPerUnit(request.getCarbsPerUnit());
            }
            if (request.getFatPerUnit() != null) {
                foodItem.setFatPerUnit(request.getFatPerUnit());
            }
            if (request.getFiberPerUnit() != null) {
                foodItem.setFiberPerUnit(request.getFiberPerUnit());
            }
            if (request.getVisibility() != null) {
                foodItem.setVisibility(FoodItem.FoodVisibility.valueOf(request.getVisibility().toUpperCase()));
            }
            
            FoodItem updatedFoodItem = foodItemRepository.save(foodItem);
            
            logger.info("Updated food item: {} for user: {}", updatedFoodItem.getName(), authenticatedUserId);
            
            return Map.of(
                "message", "updated",
                "updatedAt", updatedFoodItem.getUpdatedAt()
            );
            
        } catch (Exception e) {
            logger.error("Error updating food item: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, String> deleteFoodItem(Long id, Long authenticatedUserId) {
        try {
            FoodItem foodItem = foodItemRepository.findByIdAndStatus(id, FoodItem.FoodStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Food item not found"));
            
            // Check ownership
            if (!foodItem.getCreatedBy().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("You can only delete your own food items");
            }
            
            // Soft delete
            foodItem.setStatus(FoodItem.FoodStatus.DELETED);
            foodItemRepository.save(foodItem);
            
            logger.info("Deleted food item: {} for user: {}", foodItem.getName(), authenticatedUserId);
            
            return Map.of("message", "deleted");
            
        } catch (Exception e) {
            logger.error("Error deleting food item: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public List<FoodItem> getAvailableFoodItems(Long userId) {
        return foodItemRepository.findAvailableFoodItems(userId);
    }
}
