package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.*;
import com.healthapp.service.MenstrualCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/cycles")
@Tag(name = "Menstrual Cycle Tracking", description = "APIs for tracking menstrual cycles and phases")
@CrossOrigin(origins = "*")
public class MenstrualCycleController {
    
    private static final Logger logger = LoggerFactory.getLogger(MenstrualCycleController.class);
    
    @Autowired
    private MenstrualCycleService menstrualCycleService;
    
    @PostMapping
    @RateLimit(value = 20)
    @Operation(summary = "Create a new menstrual cycle entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cycle created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MenstrualCycleCreateResponse> createCycle(@Valid @RequestBody MenstrualCycleCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        MenstrualCycleCreateResponse response = menstrualCycleService.createCycle(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @RateLimit(value = 30)
    @Operation(summary = "Get cycle details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cycle details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Cycle not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MenstrualCycleResponse> getCycle(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        MenstrualCycleResponse response = menstrualCycleService.getCycle(id, authenticatedUserId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @RateLimit(value = 30)
    @Operation(summary = "Get paginated list of cycles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cycles retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MenstrualCyclePaginatedResponse> getCycles(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "From date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Page number (default: 1)") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Items per page (default: 20, max: 100)") @RequestParam(defaultValue = "20") Integer limit) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        MenstrualCyclePaginatedResponse response = menstrualCycleService.getCycles(userId, from, to, page, limit, authenticatedUserId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}")
    @RateLimit(value = 20)
    @Operation(summary = "Update cycle details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cycle updated successfully"),
        @ApiResponse(responseCode = "404", description = "Cycle not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> updateCycle(@PathVariable Long id, @Valid @RequestBody MenstrualCycleUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        menstrualCycleService.updateCycle(id, request, authenticatedUserId);
        return ResponseEntity.ok(Map.of("message", "Cycle updated successfully"));
    }
    
    @DeleteMapping("/{id}")
    @RateLimit(value = 10)
    @Operation(summary = "Soft delete cycle")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cycle deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Cycle not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> deleteCycle(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        menstrualCycleService.deleteCycle(id, authenticatedUserId);
        return ResponseEntity.ok(Map.of("message", "Cycle deleted successfully"));
    }
    
    @GetMapping("/phase")
    @RateLimit(value = 30)
    @Operation(summary = "Get current cycle phase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Current phase retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "No cycle data found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CyclePhaseResponse> getCurrentPhase() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        CyclePhaseResponse response = menstrualCycleService.getCurrentPhase(authenticatedUserId);
        return ResponseEntity.ok(response);
    }
}
