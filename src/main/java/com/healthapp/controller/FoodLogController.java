package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.FoodLogCreateRequest;
import com.healthapp.dto.FoodLogCreateResponse;
import com.healthapp.dto.FoodLogPaginatedResponse;
import com.healthapp.dto.FoodLogResponse;
import com.healthapp.dto.FoodLogUpdateRequest;
import com.healthapp.service.FoodLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/food-logs")
@Tag(name = "Food Logging Management", description = "APIs for logging food consumption and tracking nutritional intake")
@CrossOrigin(origins = "*")
public class FoodLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(FoodLogController.class);
    
    @Autowired
    private FoodLogService foodLogService;
    
    @GetMapping
    @Operation(summary = "List food logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food logs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodLogPaginatedResponse> getFoodLogs(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "from", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            @RequestParam(value = "sortBy", defaultValue = "loggedAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            // Validate sortBy parameter
            if (!sortBy.equals("loggedAt") && !sortBy.equals("createdAt")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate sortDir parameter
            if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate mealType parameter
            if (mealType != null && !mealType.equalsIgnoreCase("breakfast") && 
                !mealType.equalsIgnoreCase("lunch") && !mealType.equalsIgnoreCase("dinner") && 
                !mealType.equalsIgnoreCase("snack")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Regular users can only see their own entries
            if (!isAdmin && userId != null && !userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // If no userId specified and not admin, use authenticated user's ID
            if (userId == null && !isAdmin) {
                userId = authenticatedUserId;
            }
            
            FoodLogPaginatedResponse response = foodLogService.getFoodLogs(
                    userId, from, to, mealType, page, limit, sortBy, sortDir, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving food logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get food log by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food log retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Food log not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodLogResponse> getFoodLogById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            return foodLogService.getFoodLogById(id, authenticatedUserId, isAdmin)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Error retrieving food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 20)
    @Operation(summary = "Create a new food log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Food log created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FoodLogCreateResponse> createFoodLog(@Valid @RequestBody FoodLogCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            FoodLogCreateResponse response = foodLogService.createFoodLog(request, authenticatedUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 30)
    @Operation(summary = "Update a food log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food log updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Food log not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner")
    })
    public ResponseEntity<Map<String, Object>> updateFoodLog(
            @PathVariable Long id, @Valid @RequestBody FoodLogUpdateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            Map<String, Object> response = foodLogService.updateFoodLog(id, request, authenticatedUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10)
    @Operation(summary = "Delete a food log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food log deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Food log not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner")
    })
    public ResponseEntity<Map<String, String>> deleteFoodLog(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            Map<String, String> response = foodLogService.deleteFoodLog(id, authenticatedUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error deleting food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
