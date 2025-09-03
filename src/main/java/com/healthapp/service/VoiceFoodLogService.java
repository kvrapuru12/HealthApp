package com.healthapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.dto.*;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.FoodLog;
import com.healthapp.entity.User;
import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public VoiceFoodLogResponse processVoiceFoodLog(VoiceFoodLogRequest request, Long authenticatedUserId) {
        try {
            // Validate user access
            if (!request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create food logs for themselves");
            }
            
            // Validate user exists
            User user = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Parse voice text using AI
            List<ParsedFoodItem> parsedItems = parseVoiceText(request.getVoiceText());
            
            if (parsedItems.isEmpty()) {
                return new VoiceFoodLogResponse("Could not extract food items from input. Please try rephrasing.", 
                                             new ArrayList<>());
            }
            
            // Process each parsed item
            List<VoiceFoodLogResponse.LoggedFoodItem> loggedItems = new ArrayList<>();
            
            for (ParsedFoodItem parsedItem : parsedItems) {
                try {
                    // Find or create food item
                    FoodItem foodItem = findOrCreateFoodItem(parsedItem, authenticatedUserId);
                    
                    // Create food log
                    FoodLogCreateRequest logRequest = new FoodLogCreateRequest();
                    logRequest.setUserId(authenticatedUserId);
                    logRequest.setFoodItemId(foodItem.getId());
                    logRequest.setLoggedAt(parsedItem.getLoggedAt());
                    logRequest.setMealType(parsedItem.getMealType());
                    logRequest.setQuantity(parsedItem.getQuantity());
                    logRequest.setUnit(parsedItem.getUnit());
                    logRequest.setNote("Created from voice input: " + request.getVoiceText());
                    
                    FoodLogCreateResponse logResponse = foodLogService.createFoodLog(logRequest, authenticatedUserId);
                    
                    // Add to response
                    loggedItems.add(new VoiceFoodLogResponse.LoggedFoodItem(
                            foodItem.getName(),
                            parsedItem.getQuantity(),
                            parsedItem.getMealType(),
                            logResponse.getCalories(),
                            logResponse.getProtein(),
                            logResponse.getCarbs(),
                            logResponse.getFat(),
                            logResponse.getFiber()
                    ));
                    
                } catch (Exception e) {
                    logger.warn("Failed to process food item: {}", parsedItem.getFoodName(), e);
                    // Continue with other items
                }
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
    
    private List<ParsedFoodItem> parseVoiceText(String voiceText) {
        try {
            String prompt = buildParsingPrompt(voiceText);
            String response = callOpenAI(prompt);
            return parseAIResponse(response);
        } catch (Exception e) {
            logger.error("Error parsing voice text: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private String buildParsingPrompt(String voiceText) {
        return String.format("""
            You are a nutrition assistant. Parse the following sentence into JSON format with food items.
            
            Each item must include:
            - foodName (normalized food name)
            - quantity (numeric value)
            - unit (grams, pieces, glass, cup, tablespoon, teaspoon, etc.)
            - mealType (breakfast/lunch/dinner/snack)
            - loggedAt (ISO 8601 datetime, use current time if not specified)
            
            Return ONLY a valid JSON array. Example format:
            [
              {
                "foodName": "boiled egg",
                "quantity": 2,
                "unit": "pieces",
                "mealType": "breakfast",
                "loggedAt": "2025-09-03T08:00:00Z"
              }
            ]
            
            Text to parse: "%s"
            """, voiceText);
    }
    
    private String callOpenAI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 500);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, request, Map.class);
            
            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            
            throw new RuntimeException("Invalid response from OpenAI");
            
        } catch (Exception e) {
            logger.error("Error calling OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call AI service: " + e.getMessage());
        }
    }
    
    private List<ParsedFoodItem> parseAIResponse(String aiResponse) {
        try {
            // Clean the response - remove any markdown formatting
            String cleanResponse = aiResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            List<Map<String, Object>> items = objectMapper.readValue(cleanResponse, new TypeReference<List<Map<String, Object>>>() {});
            
            return items.stream()
                    .map(this::mapToParsedFoodItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
        } catch (JsonProcessingException e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private ParsedFoodItem mapToParsedFoodItem(Map<String, Object> item) {
        try {
            String foodName = (String) item.get("foodName");
            Double quantity = parseQuantity(item.get("quantity"));
            String unit = (String) item.get("unit");
            String mealType = (String) item.get("mealType");
            LocalDateTime loggedAt = parseDateTime(item.get("loggedAt"));
            
            if (foodName == null || quantity == null || unit == null || mealType == null) {
                return null;
            }
            
            return new ParsedFoodItem(foodName, quantity, unit, mealType, loggedAt);
            
        } catch (Exception e) {
            logger.warn("Error mapping parsed food item: {}", e.getMessage());
            return null;
        }
    }
    
    private Double parseQuantity(Object quantity) {
        if (quantity instanceof Number) {
            return ((Number) quantity).doubleValue();
        } else if (quantity instanceof String) {
            try {
                return Double.parseDouble((String) quantity);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private LocalDateTime parseDateTime(Object dateTime) {
        if (dateTime instanceof String) {
            try {
                return LocalDateTime.parse(((String) dateTime).replace("Z", ""));
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }
    
    private FoodItem findOrCreateFoodItem(ParsedFoodItem parsedItem, Long userId) {
        // First, try to find existing food item
        String normalizedName = normalizeFoodName(parsedItem.getFoodName());
        
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
        FoodItemCreateRequest createRequest = estimateFoodMacros(parsedItem, userId);
        FoodItemCreateResponse response = foodItemService.createFoodItem(createRequest, userId);
        
        return foodItemRepository.findById(response.getId())
                .orElseThrow(() -> new RuntimeException("Failed to create food item"));
    }
    
    private String normalizeFoodName(String foodName) {
        return foodName.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private FoodItemCreateRequest estimateFoodMacros(ParsedFoodItem parsedItem, Long userId) {
        // Use AI to estimate macros for the food item
        String prompt = String.format("""
            Estimate the nutritional values for this food item. Return ONLY a JSON object with:
            - caloriesPerUnit (integer)
            - proteinPerUnit (double, grams)
            - carbsPerUnit (double, grams)
            - fatPerUnit (double, grams)
            - fiberPerUnit (double, grams)
            
            Food: %s
            Unit: %s
            
            Example response:
            {
              "caloriesPerUnit": 70,
              "proteinPerUnit": 6.3,
              "carbsPerUnit": 0.6,
              "fatPerUnit": 5.3,
              "fiberPerUnit": 0.0
            }
            """, parsedItem.getFoodName(), parsedItem.getUnit());
        
        try {
            String response = callOpenAI(prompt);
            Map<String, Object> macros = objectMapper.readValue(response, Map.class);
            
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedItem.getFoodName());
            createRequest.setCategory("general");
            createRequest.setDefaultUnit(parsedItem.getUnit());
            createRequest.setQuantityPerUnit(1.0);
            createRequest.setCaloriesPerUnit(((Number) macros.get("caloriesPerUnit")).intValue());
            createRequest.setProteinPerUnit(((Number) macros.get("proteinPerUnit")).doubleValue());
            createRequest.setCarbsPerUnit(((Number) macros.get("carbsPerUnit")).doubleValue());
            createRequest.setFatPerUnit(((Number) macros.get("fatPerUnit")).doubleValue());
            createRequest.setFiberPerUnit(((Number) macros.get("fiberPerUnit")).doubleValue());
            createRequest.setVisibility("private");
            
            return createRequest;
            
        } catch (Exception e) {
            logger.warn("Failed to estimate macros for {}, using defaults", parsedItem.getFoodName());
            
            // Fallback to default values
            FoodItemCreateRequest createRequest = new FoodItemCreateRequest();
            createRequest.setName(parsedItem.getFoodName());
            createRequest.setCategory("general");
            createRequest.setDefaultUnit(parsedItem.getUnit());
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
