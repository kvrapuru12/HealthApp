package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.CycleSyncActivityResponse;
import com.healthapp.dto.CycleSyncFoodResponse;
import com.healthapp.dto.VoiceCycleLogRequest;
import com.healthapp.dto.VoiceCycleLogResponse;
import com.healthapp.service.MenstrualCycleService;
import com.healthapp.service.VoiceCycleLogService;
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
@RequestMapping("/ai/suggestions/cycle-sync")
@Tag(name = "AI Cycle-Sync Recommendations", description = "AI-powered recommendations based on menstrual cycle phases")
@CrossOrigin(origins = "*")
public class CycleSyncAiController {
    
    private static final Logger logger = LoggerFactory.getLogger(CycleSyncAiController.class);
    
    @Autowired
    private MenstrualCycleService menstrualCycleService;
    
    @Autowired(required = false)
    private VoiceCycleLogService voiceCycleLogService;
    
    @GetMapping("/food")
    @RateLimit(value = 15)
    @Operation(summary = "Get AI food recommendations based on current cycle phase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Food recommendations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "No cycle data found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CycleSyncFoodResponse> getFoodRecommendations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        CycleSyncFoodResponse response = menstrualCycleService.getFoodRecommendations(authenticatedUserId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activity")
    @RateLimit(value = 15)
    @Operation(summary = "Get AI activity recommendations based on current cycle phase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity recommendations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "No cycle data found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CycleSyncActivityResponse> getActivityRecommendations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        
        CycleSyncActivityResponse response = menstrualCycleService.getActivityRecommendations(authenticatedUserId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/cycle-log/from-voice")
    @RateLimit(value = 10)
    @Operation(summary = "Log menstrual cycle via voice input using AI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cycle logged successfully from voice input"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or AI processing failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "503", description = "AI service not available")
    })
    public ResponseEntity<?> createCycleLogFromVoice(@Valid @RequestBody VoiceCycleLogRequest request) {
        try {
            // Check if AI service is available
            if (voiceCycleLogService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "AI voice parsing service is not available. Please configure OpenAI API key."));
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            VoiceCycleLogResponse response = voiceCycleLogService.processVoiceCycleLog(request, authenticatedUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new VoiceCycleLogResponse("Invalid request: " + e.getMessage(), null, null)
            );
        } catch (Exception e) {
            logger.error("Error processing voice cycle log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new VoiceCycleLogResponse("Failed to process voice input. Please try again.", null, null)
            );
        }
    }
}
