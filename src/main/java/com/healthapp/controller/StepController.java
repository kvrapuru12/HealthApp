package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.StepCreateRequest;
import com.healthapp.dto.StepCreateResponse;
import com.healthapp.dto.StepPaginatedResponse;
import com.healthapp.dto.StepResponse;
import com.healthapp.dto.StepUpdateRequest;
import com.healthapp.service.StepEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/steps")
@Tag(name = "Step Tracking Management", description = "APIs for managing step entries and tracking daily step counts")
@CrossOrigin(origins = "*")
public class StepController {
    
    private static final Logger logger = LoggerFactory.getLogger(StepController.class);
    
    @Autowired
    private StepEntryService stepEntryService;
    
    @GetMapping
    @Operation(
        summary = "List step entries", 
        description = "Get paginated step entries with filtering options. Admin users can filter by userId query parameter."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Step entries retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<StepPaginatedResponse> getStepEntries(
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
            
            // Regular users can only see their own entries
            if (!isAdmin && userId != null && !userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // If no userId specified and not admin, use authenticated user's ID
            if (userId == null && !isAdmin) {
                userId = authenticatedUserId;
            }
            
            StepPaginatedResponse response = stepEntryService.getStepEntries(
                    userId, from, to, page, limit, sortBy, sortDir);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving step entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get step entry by ID", 
        description = "Retrieve a specific step entry by its numeric ID. Accessible to the entry owner or admin users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Step entry retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Step entry not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<StepResponse> getStepEntryById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var stepEntryOpt = stepEntryService.getStepEntryById(id);
            if (stepEntryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var stepEntry = stepEntryOpt.get();
            // Check if user can access this entry
            if (isAdmin || stepEntry.getUserId().equals(authenticatedUserId)) {
                return ResponseEntity.ok(stepEntry);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
                    
        } catch (Exception e) {
            logger.error("Error retrieving step entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(
        summary = "Create step entry", 
        description = "Create a new step entry with validation including duplicate checking within ±5 minutes and future timestamp validation (max 10 minutes ahead)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Step entry created successfully",
            content = @Content(schema = @Schema(implementation = StepCreateResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "User ID mismatch"),
        @ApiResponse(responseCode = "409", description = "Duplicate entry detected")
    })
    public ResponseEntity<StepCreateResponse> createStepEntry(
            @Valid @RequestBody StepCreateRequest request) {
        
        try {
            logger.info("Creating step entry for user: {}, stepCount: {}, loggedAt: {}", 
                request.getUserId(), request.getStepCount(), request.getLoggedAt());
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            logger.debug("Authenticated user ID: {}, isAdmin: {}", authenticatedUserId, isAdmin);
            
            StepResponse stepResponse = stepEntryService.createStepEntry(request, authenticatedUserId, isAdmin);
            
            StepCreateResponse createResponse = new StepCreateResponse(stepResponse.getId(), stepResponse.getCreatedAt());
            
            logger.info("Created step entry with ID: {} for user: {}", stepResponse.getId(), request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createResponse);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in step entry creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in step entry creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating step entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Partial update step entry", 
        description = "Partially update an existing step entry. Only the entry owner or admin can update."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Step entry updated successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot update step entry of another user"),
        @ApiResponse(responseCode = "404", description = "Step entry not found")
    })
    public ResponseEntity<Map<String, Object>> updateStepEntry(
            @PathVariable Long id, 
            @Valid @RequestBody StepUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            stepEntryService.updateStepEntry(id, request, authenticatedUserId, isAdmin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "updated");
            response.put("updatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in step entry update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in step entry update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating step entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Soft delete step entry", 
        description = "Soft delete a step entry (mark as deleted). Only the entry owner or admin can delete."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Step entry soft deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot delete step entry of another user"),
        @ApiResponse(responseCode = "404", description = "Step entry not found")
    })
    public ResponseEntity<Map<String, String>> deleteStepEntry(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            stepEntryService.softDeleteStepEntry(id, authenticatedUserId, isAdmin);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "deleted");
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in step entry deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in step entry deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting step entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
