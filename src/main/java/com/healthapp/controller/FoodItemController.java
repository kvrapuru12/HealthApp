package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.FoodItemCreateRequest;
import com.healthapp.dto.FoodItemCreateResponse;
import com.healthapp.dto.FoodItemPaginatedResponse;
import com.healthapp.dto.FoodItemResponse;
import com.healthapp.dto.FoodItemUpdateRequest;
import com.healthapp.service.FoodItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/foods")
@Tag(name = "Food Item Management", description = "APIs for managing food items and their nutritional information")
@CrossOrigin(origins = "*")
public class FoodItemController {
    
    private static final Logger logger = LoggerFactory.getLogger(FoodItemController.class);
    
    @Autowired
    private FoodItemService foodItemService;
    
    @GetMapping
    @Operation(summary = "List food items")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food items retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodItemPaginatedResponse> getFoodItems(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            // Validate sortBy parameter
            if (!sortBy.equals("name") && !sortBy.equals("createdAt")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate sortDir parameter
            if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate visibility parameter
            if (visibility != null && !visibility.equalsIgnoreCase("private") && !visibility.equalsIgnoreCase("public")) {
                return ResponseEntity.badRequest().build();
            }
            
            FoodItemPaginatedResponse response = foodItemService.getFoodItems(
                    authenticatedUserId, search, visibility, page, limit, sortBy, sortDir);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving food items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food item retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Food item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodItemResponse> getFoodItemById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            return foodItemService.getFoodItemById(id, authenticatedUserId, isAdmin)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Error retrieving food item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10)
    @Operation(summary = "Create a new food item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Food item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodItemCreateResponse> createFoodItem(@Valid @RequestBody FoodItemCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            FoodItemCreateResponse response = foodItemService.createFoodItem(request, authenticatedUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating food item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 20)
    @Operation(summary = "Update a food item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Food item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner")
    })
    public ResponseEntity<Map<String, Object>> updateFoodItem(
            @PathVariable Long id, @Valid @RequestBody FoodItemUpdateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            Map<String, Object> response = foodItemService.updateFoodItem(id, request, authenticatedUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating food item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10)
    @Operation(summary = "Delete a food item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food item deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Food item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner")
    })
    public ResponseEntity<Map<String, String>> deleteFoodItem(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            Map<String, String> response = foodItemService.deleteFoodItem(id, authenticatedUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error deleting food item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
