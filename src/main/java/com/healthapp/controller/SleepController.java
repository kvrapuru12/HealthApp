package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.SleepCreateRequest;
import com.healthapp.dto.SleepCreateResponse;
import com.healthapp.dto.SleepPaginatedResponse;
import com.healthapp.dto.SleepResponse;
import com.healthapp.dto.SleepUpdateRequest;
import com.healthapp.service.SleepEntryService;
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
@RequestMapping("/api/sleeps")
@Tag(name = "Sleep Management", description = "APIs for managing sleep entries and tracking")
@CrossOrigin(origins = "*")
public class SleepController {
    
    private static final Logger logger = LoggerFactory.getLogger(SleepController.class);
    
    @Autowired
    private SleepEntryService sleepEntryService;
    
    @GetMapping
    @Operation(
        summary = "List sleep entries", 
        description = "Get paginated sleep entries with filtering options. Admin users can filter by userId query parameter."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sleep entries retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SleepPaginatedResponse> getSleepEntries(
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
            
            SleepPaginatedResponse response = sleepEntryService.getSleepEntries(
                    userId, from, to, page, limit, sortBy, sortDir);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving sleep entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get sleep entry by ID", 
        description = "Retrieve a specific sleep entry by its numeric ID. Accessible to the entry owner or admin users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sleep entry retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Sleep entry not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<SleepResponse> getSleepEntryById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var sleepEntryOpt = sleepEntryService.getSleepEntryById(id);
            if (sleepEntryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var sleepEntry = sleepEntryOpt.get();
            // Check if user can access this entry
            if (isAdmin || sleepEntry.getUserId().equals(authenticatedUserId)) {
                return ResponseEntity.ok(sleepEntry);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
                    
        } catch (Exception e) {
            logger.error("Error retrieving sleep entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(
        summary = "Create sleep entry", 
        description = "Create a new sleep entry with validation including duplicate checking within ±5 minutes and future timestamp validation (max 10 minutes ahead)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Sleep entry created successfully",
            content = @Content(schema = @Schema(implementation = SleepCreateResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "User ID mismatch"),
        @ApiResponse(responseCode = "409", description = "Duplicate entry detected")
    })
    public ResponseEntity<SleepCreateResponse> createSleepEntry(
            @Valid @RequestBody SleepCreateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            SleepResponse sleepResponse = sleepEntryService.createSleepEntry(request, authenticatedUserId, isAdmin);
            
            SleepCreateResponse createResponse = new SleepCreateResponse(sleepResponse.getId(), sleepResponse.getCreatedAt());
            
            logger.info("Created sleep entry with ID: {} for user: {}", sleepResponse.getId(), request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createResponse);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in sleep entry creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in sleep entry creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating sleep entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Partial update sleep entry", 
        description = "Partially update an existing sleep entry. Only the entry owner or admin can update."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sleep entry updated successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot update sleep entry of another user"),
        @ApiResponse(responseCode = "404", description = "Sleep entry not found")
    })
    public ResponseEntity<Map<String, Object>> updateSleepEntry(
            @PathVariable Long id, 
            @Valid @RequestBody SleepUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            sleepEntryService.updateSleepEntry(id, request, authenticatedUserId, isAdmin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "updated");
            response.put("updatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in sleep entry update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in sleep entry update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating sleep entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Soft delete sleep entry", 
        description = "Soft delete a sleep entry (mark as deleted). Only the entry owner or admin can delete."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sleep entry soft deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot delete sleep entry of another user"),
        @ApiResponse(responseCode = "404", description = "Sleep entry not found")
    })
    public ResponseEntity<Map<String, String>> deleteSleepEntry(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            sleepEntryService.softDeleteSleepEntry(id, authenticatedUserId, isAdmin);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "deleted");
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in sleep entry deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in sleep entry deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting sleep entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
