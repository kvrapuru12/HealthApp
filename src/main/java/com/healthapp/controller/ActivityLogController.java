package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.ActivityLogCreateRequest;
import com.healthapp.dto.ActivityLogCreateResponse;
import com.healthapp.dto.ActivityLogPaginatedResponse;
import com.healthapp.dto.ActivityLogResponse;
import com.healthapp.dto.ActivityLogUpdateRequest;
import com.healthapp.service.ActivityLogService;
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
@RequestMapping("/activity-logs")
@Tag(name = "Activity Log Management", description = "APIs for managing activity log entries")
@CrossOrigin(origins = "*")
public class ActivityLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogController.class);
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @GetMapping
    @Operation(summary = "List activity logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity logs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<ActivityLogPaginatedResponse> getActivityLogs(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "from", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
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
            
            ActivityLogPaginatedResponse response = activityLogService.getActivityLogs(
                    userId, from, to, page, limit, sortBy, sortDir, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error retrieving activity logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get activity log by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Activity log not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<ActivityLogResponse> getActivityLogById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var activityLog = activityLogService.getActivityLogById(id, authenticatedUserId, isAdmin);
            
            return activityLog.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Error retrieving activity log with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Create activity log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Activity log created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<ActivityLogCreateResponse> createActivityLog(@Valid @RequestBody ActivityLogCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            ActivityLogCreateResponse response = activityLogService.createActivityLog(request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error creating activity log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Update activity log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity log updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Activity log not found")
    })
    public ResponseEntity<Map<String, Object>> updateActivityLog(
            @PathVariable Long id, 
            @Valid @RequestBody ActivityLogUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            Map<String, Object> response = activityLogService.updateActivityLog(id, request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error updating activity log with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Delete activity log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity log deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Activity log not found")
    })
    public ResponseEntity<Map<String, String>> deleteActivityLog(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            Map<String, String> response = activityLogService.deleteActivityLog(id, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            logger.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error deleting activity log with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
