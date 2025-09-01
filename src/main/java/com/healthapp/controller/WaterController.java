package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.WaterCreateRequest;
import com.healthapp.dto.WaterCreateResponse;
import com.healthapp.dto.WaterPaginatedResponse;
import com.healthapp.dto.WaterResponse;
import com.healthapp.dto.WaterUpdateRequest;
import com.healthapp.service.WaterEntryService;
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
@RequestMapping("/water")
@Tag(name = "Water Tracking Management", description = "APIs for managing water consumption entries and tracking daily water intake")
@CrossOrigin(origins = "*")
public class WaterController {
    
    private static final Logger logger = LoggerFactory.getLogger(WaterController.class);
    
    @Autowired
    private WaterEntryService waterEntryService;
    
    @GetMapping
    @Operation(
        summary = "List water entries", 
        description = "Get paginated water entries with filtering options. Admin users can filter by userId query parameter."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Water entries retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WaterPaginatedResponse> getWaterEntries(
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
            
            WaterPaginatedResponse response = waterEntryService.getWaterEntries(
                    userId, from, to, page, limit, sortBy, sortDir);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving water entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get water entry by ID", 
        description = "Retrieve a specific water entry by its numeric ID. Accessible to the entry owner or admin users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Water entry retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Water entry not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<WaterResponse> getWaterEntryById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var waterEntryOpt = waterEntryService.getWaterEntryById(id);
            if (waterEntryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var waterEntry = waterEntryOpt.get();
            // Check if user can access this entry
            if (isAdmin || waterEntry.getUserId().equals(authenticatedUserId)) {
                return ResponseEntity.ok(waterEntry);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
                    
        } catch (Exception e) {
            logger.error("Error retrieving water entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(
        summary = "Create water entry", 
        description = "Create a new water entry with validation including duplicate checking within ±5 minutes and future timestamp validation (max 10 minutes ahead)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Water entry created successfully",
            content = @Content(schema = @Schema(implementation = WaterCreateResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "User ID mismatch"),
        @ApiResponse(responseCode = "409", description = "Duplicate entry detected")
    })
    public ResponseEntity<WaterCreateResponse> createWaterEntry(
            @Valid @RequestBody WaterCreateRequest request) {
        
        try {
            logger.info("Creating water entry for user: {}, amount: {}, loggedAt: {}", 
                request.getUserId(), request.getAmount(), request.getLoggedAt());
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            logger.debug("Authenticated user ID: {}, isAdmin: {}", authenticatedUserId, isAdmin);
            
            WaterResponse waterResponse = waterEntryService.createWaterEntry(request, authenticatedUserId, isAdmin);
            
            WaterCreateResponse createResponse = new WaterCreateResponse(waterResponse.getId(), waterResponse.getCreatedAt());
            
            logger.info("Created water entry with ID: {} for user: {}", waterResponse.getId(), request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createResponse);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in water entry creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in water entry creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating water entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Partial update water entry", 
        description = "Partially update an existing water entry. Only the entry owner or admin can update."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Water entry updated successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot update water entry of another user"),
        @ApiResponse(responseCode = "404", description = "Water entry not found")
    })
    public ResponseEntity<Map<String, Object>> updateWaterEntry(
            @PathVariable Long id, 
            @Valid @RequestBody WaterUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            waterEntryService.updateWaterEntry(id, request, authenticatedUserId, isAdmin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "updated");
            response.put("updatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in water entry update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in water entry update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating water entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Soft delete water entry", 
        description = "Soft delete a water entry (mark as deleted). Only the entry owner or admin can delete."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Water entry soft deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot delete water entry of another user"),
        @ApiResponse(responseCode = "404", description = "Water entry not found")
    })
    public ResponseEntity<Map<String, String>> deleteWaterEntry(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            waterEntryService.softDeleteWaterEntry(id, authenticatedUserId, isAdmin);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "deleted");
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in water entry deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in water entry deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting water entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
