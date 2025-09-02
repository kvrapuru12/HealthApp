package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.ActivityCreateRequest;
import com.healthapp.dto.ActivityCreateResponse;
import com.healthapp.dto.ActivityPaginatedResponse;
import com.healthapp.dto.ActivityResponse;
import com.healthapp.dto.ActivityUpdateRequest;
import com.healthapp.service.ActivityService;
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
@RequestMapping("/activities")
@Tag(name = "Activity Management", description = "APIs for managing reusable activities")
@CrossOrigin(origins = "*")
public class ActivityController {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);
    
    @Autowired
    private ActivityService activityService;
    
    @GetMapping
    @Operation(summary = "List activities")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activities retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ActivityPaginatedResponse> getActivities(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            ActivityPaginatedResponse response = activityService.getActivities(
                    search, visibility, page, limit, sortBy, sortDir, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving activities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Activity not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var activity = activityService.getActivityById(id, authenticatedUserId, isAdmin);
            
            return activity.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Error retrieving activity with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Create activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Activity created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<ActivityCreateResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            ActivityCreateResponse response = activityService.createActivity(request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error creating activity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Update activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<Map<String, Object>> updateActivity(
            @PathVariable Long id, 
            @Valid @RequestBody ActivityUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            Map<String, Object> response = activityService.updateActivity(id, request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error updating activity with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Delete activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<Map<String, String>> deleteActivity(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            Map<String, String> response = activityService.deleteActivity(id, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error deleting activity with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
