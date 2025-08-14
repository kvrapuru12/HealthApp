package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.MoodCreateRequest;
import com.healthapp.dto.MoodResponse;
import com.healthapp.service.MoodEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.List;


@RestController
@RequestMapping("/moods")
@Tag(name = "Mood Management", description = "APIs for managing mood entries and tracking")
@CrossOrigin(origins = "*")
public class MoodController {
    
    private static final Logger logger = LoggerFactory.getLogger(MoodController.class);
    
    @Autowired
    private MoodEntryService moodEntryService;
    
    @GetMapping
    @Operation(
        summary = "List mood entries", 
        description = "Get mood entries for the authenticated user. Admin users can filter by userId query parameter."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mood entries retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<MoodResponse>> getMoodEntries(
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            List<MoodResponse> moodEntries;
            if (userId != null && isAdmin) {
                // Admin can filter by userId
                moodEntries = moodEntryService.getMoodEntriesByUserId(userId);
            } else {
                // Regular user gets their own entries
                moodEntries = moodEntryService.getMoodEntriesByUserId(authenticatedUserId);
            }
            
            return ResponseEntity.ok(moodEntries);
            
        } catch (Exception e) {
            logger.error("Error retrieving mood entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get mood entry by ID", 
        description = "Retrieve a specific mood entry by its numeric ID. Accessible to the entry owner or admin users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mood entry retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Mood entry not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<MoodResponse> getMoodEntryById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            var moodEntryOpt = moodEntryService.getMoodEntryById(id);
            if (moodEntryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var moodEntry = moodEntryOpt.get();
            // Check if user can access this entry
            if (isAdmin || moodEntry.getUserId().equals(authenticatedUserId)) {
                return ResponseEntity.ok(moodEntry);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
                    
        } catch (Exception e) {
            logger.error("Error retrieving mood entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(
        summary = "Create mood entry", 
        description = "Create a new mood entry with validation including duplicate checking within ±5 minutes and future timestamp validation (max 10 minutes ahead)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Mood entry created successfully",
            content = @Content(schema = @Schema(implementation = MoodResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "User ID mismatch"),
        @ApiResponse(responseCode = "409", description = "Duplicate entry detected")
    })
    public ResponseEntity<MoodResponse> createMoodEntry(
            @Valid @RequestBody MoodCreateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            MoodResponse moodResponse = moodEntryService.createMoodEntry(request, authenticatedUserId, isAdmin);
            
            logger.info("Created mood entry with ID: {} for user: {}", moodResponse.getId(), request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(moodResponse);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in mood entry creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in mood entry creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating mood entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Partial update mood entry", 
        description = "Partially update an existing mood entry. Only the entry owner or admin can update."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mood entry updated successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot update mood entry of another user"),
        @ApiResponse(responseCode = "404", description = "Mood entry not found")
    })
    public ResponseEntity<MoodResponse> updateMoodEntry(
            @PathVariable Long id, 
            @Valid @RequestBody MoodCreateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            MoodResponse updatedMood = moodEntryService.updateMoodEntry(id, request, authenticatedUserId, isAdmin);
            return ResponseEntity.ok(updatedMood);
            
        } catch (SecurityException e) {
            logger.warn("Security violation in mood entry update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in mood entry update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating mood entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Soft delete mood entry", 
        description = "Soft delete a mood entry (mark as deleted). Only the entry owner or admin can delete."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mood entry soft deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Cannot delete mood entry of another user"),
        @ApiResponse(responseCode = "404", description = "Mood entry not found")
    })
    public ResponseEntity<?> deleteMoodEntry(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            moodEntryService.softDeleteMoodEntry(id, authenticatedUserId, isAdmin);
            return ResponseEntity.ok().build();
            
        } catch (SecurityException e) {
            logger.warn("Security violation in mood entry deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in mood entry deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting mood entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
