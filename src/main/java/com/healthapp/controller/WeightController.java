package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.WeightCreateRequest;
import com.healthapp.dto.WeightCreateResponse;
import com.healthapp.dto.WeightPaginatedResponse;
import com.healthapp.dto.WeightResponse;
import com.healthapp.dto.WeightUpdateRequest;
import com.healthapp.service.WeightEntryService;
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
@RequestMapping("/weights")
@Tag(name = "Weight Tracking Management", description = "APIs for managing weight measurement entries and tracking weight trends")
@CrossOrigin(origins = "*")
public class WeightController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeightController.class);
    
    @Autowired
    private WeightEntryService weightEntryService;
    
    @GetMapping
    @Operation(summary = "List weight entries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Weight entries retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WeightPaginatedResponse> getWeightEntries(
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
            
            WeightPaginatedResponse response = weightEntryService.getWeightEntries(
                    userId, from, to, page, limit, sortBy, sortDir);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving weight entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get weight entry by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Weight entry retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Weight entry not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<WeightResponse> getWeightEntryById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            // Validate id parameter
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            var weightEntry = weightEntryService.getWeightEntryById(id, authenticatedUserId, isAdmin);
            
            return weightEntry.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Error retrieving weight entry with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Create weight entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Weight entry created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<WeightCreateResponse> createWeightEntry(@Valid @RequestBody WeightCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            WeightCreateResponse response = weightEntryService.createWeightEntry(request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating weight entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Update weight entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Weight entry updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Weight entry not found")
    })
    public ResponseEntity<Map<String, Object>> updateWeightEntry(
            @PathVariable Long id, 
            @Valid @RequestBody WeightUpdateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            // Validate id parameter
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            Map<String, Object> response = weightEntryService.updateWeightEntry(id, request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating weight entry with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(summary = "Delete weight entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Weight entry deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Weight entry not found")
    })
    public ResponseEntity<Map<String, String>> deleteWeightEntry(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            
            // Validate id parameter
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            Map<String, String> response = weightEntryService.deleteWeightEntry(id, authenticatedUserId, isAdmin);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error deleting weight entry with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
