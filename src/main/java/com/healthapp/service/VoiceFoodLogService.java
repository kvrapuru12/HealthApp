package com.healthapp.service;

import com.healthapp.dto.*;
import com.healthapp.entity.FoodItem;

import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class VoiceFoodLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceFoodLogService.class);
    
    @Autowired
    private FoodItemService foodItemService;
    
    @Autowired
    private FoodLogService foodLogService;
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private AiFoodVoiceParsingService aiFoodVoiceParsingService;
    
    public VoiceFoodLogResponse processVoiceFoodLog(VoiceFoodLogRequest request, Long authenticatedUserId) {
        try {
            // Validate user access
            if (!request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create food logs for themselves");
            }
            
            // Validate user exists
            userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if AI service is available
            if (aiFoodVoiceParsingService == null) {
                throw new RuntimeException("AI voice parsing service is not available. Please configure OpenAI API key.");
            }
            
            // Parse voice text using AI
            AiFoodVoiceParsingService.ParsedFoodData parsedData = aiFoodVoiceParsingService.parseVoiceText(request.getVoiceText());
            
            // Process the parsed data
            List<VoiceFoodLogResponse.LoggedFoodItem> loggedItems = new ArrayList<>();
            
            try {
                // Find or create food item
                FoodItem foodItem = findOrCreateFoodItem(parsedData, authenticatedUserId);
                
                // Create food log
                FoodLogCreateRequest logRequest = new FoodLogCreateRequest();
                logRequest.setUserId(authenticatedUserId);
                logRequest.setFoodItemId(foodItem.getId());
                logRequest.setLoggedAt(parsedData.getLoggedAt());
                logRequest.setMealType(parsedData.getMealType());
                logRequest.setQuantity(parsedData.getQuantity());
                logRequest.setUnit(parsedData.getUnit());
                logRequest.setNote(parsedData.getNote() != null ? parsedData.getNote() : "Created from voice input: " + request.getVoiceText());
                
                FoodLogCreateResponse logResponse = foodLogService.createFoodLog(logRequest, authenticatedUserId);
                
                // Add to response
                loggedItems.add(new VoiceFoodLogResponse.LoggedFoodItem(
                        foodItem.getName(),
                        parsedData.getQuantity(),
                        parsedData.getMealType(),
                        logResponse.getCalories(),
                        logResponse.getProtein(),
                        logResponse.getCarbs(),
                        logResponse.getFat(),
                        logResponse.getFiber()
                ));
                
            } catch (Exception e) {
                logger.warn("Failed to process food item: {}", parsedData.getFoodName(), e);
                throw new RuntimeException("Failed to process food item: " + e.getMessage());
            }
            
            if (loggedItems.isEmpty()) {
                return new VoiceFoodLogResponse("Failed to create any food logs. Please try again.", 
                                             new ArrayList<>());
            }
            
            return new VoiceFoodLogResponse("Food logs created from voice input", loggedItems);
            
        } catch (Exception e) {
            logger.error("Error processing voice food log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process voice input: " + e.getMessage());
        }
    }
    
    private FoodItem findOrCreateFoodItem(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        // First, try to find existing food item
        String normalizedName = normalizeFoodName(parsedData.getFoodName());
        
        Optional<FoodItem> existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndCreatedBy(
                normalizedName, FoodItem.FoodStatus.ACTIVE, userId);
        
        if (existingItem.isPresent()) {
            return existingItem.get();
        }
        
        // Check for public food items
        existingItem = foodItemRepository.findByNameIgnoreCaseAndStatusAndVisibility(
                normalizedName, FoodItem.FoodStatus.ACTIVE, FoodItem.FoodVisibility.PUBLIC);
        
        if (existingItem.isPresent()) {
            return existingItem.get();
        }
        
        // Create new food item with estimated macros
        FoodItemCreateRequest createRequest = estimateFoodMacros(parsedData, userId);
        FoodItemCreateResponse response = foodItemService.createFoodItem(createRequest, userId);
        
        return foodItemRepository.findById(response.getId())
                .orElseThrow(() -> new RuntimeException("Failed to create food item"));
    }
    
    private String normalizeFoodName(String foodName) {
        return foodName.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private FoodItemCreateRequest estimateFoodMacros(AiFoodVoiceParsingService.ParsedFoodData parsedData, Long userId) {
        // Use AI to estimate macros for the food item
        try {
            // For now, use default values since we don't have direct OpenAI access in this service
            // In a real implementation, you might want to call the AI service here
            
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedData.getFoodName());
            createRequest.setCategory(parsedData.getMealType()); // Use meal type as category
            createRequest.setDefaultUnit(parsedData.getUnit());
            createRequest.setQuantityPerUnit(1.0);
            createRequest.setCaloriesPerUnit(100);
            createRequest.setProteinPerUnit(5.0);
            createRequest.setCarbsPerUnit(10.0);
            createRequest.setFatPerUnit(3.0);
            createRequest.setFiberPerUnit(1.0);
            createRequest.setVisibility("private");
            
            return createRequest;
            
        } catch (Exception e) {
            logger.warn("Failed to estimate macros for {}, using defaults", parsedData.getFoodName());
            
            // Fallback to default values
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedData.getFoodName());
            createRequest.setCategory(parsedData.getMealType()); // Use meal type as category
            createRequest.setDefaultUnit(parsedData.getUnit());
            createRequest.setQuantityPerUnit(1.0);
            createRequest.setCaloriesPerUnit(100);
            createRequest.setProteinPerUnit(5.0);
            createRequest.setCarbsPerUnit(10.0);
            createRequest.setFatPerUnit(3.0);
            createRequest.setFiberPerUnit(1.0);
            createRequest.setVisibility("private");
            
            return createRequest;
        }
    }
}
