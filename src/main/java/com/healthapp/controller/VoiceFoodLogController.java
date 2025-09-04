package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.VoiceFoodLogRequest;
import com.healthapp.dto.VoiceFoodLogResponse;
import com.healthapp.service.VoiceFoodLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/ai/food-log")
@Tag(name = "AI Voice-to-Food Logging", description = "APIs for logging food consumption using natural language voice input")
@CrossOrigin(origins = "*")
public class VoiceFoodLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceFoodLogController.class);
    
    private final VoiceFoodLogService voiceFoodLogService;
    
    public VoiceFoodLogController(VoiceFoodLogService voiceFoodLogService) {
        this.voiceFoodLogService = voiceFoodLogService;
    }
    
    @PostMapping("/from-voice")
    @RateLimit(value = 10) // Lower rate limit for AI operations
    @Operation(summary = "Create food logs from voice input using AI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Food logs created successfully from voice input"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or AI processing failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "503", description = "AI service not available")
    })
    public ResponseEntity<?> createFoodLogFromVoice(@Valid @RequestBody VoiceFoodLogRequest request) {
        try {
            // Check if AI service is available
            if (voiceFoodLogService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "AI voice parsing service is not available. Please configure OpenAI API key."));
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            VoiceFoodLogResponse response = voiceFoodLogService.processVoiceFoodLog(request, authenticatedUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new VoiceFoodLogResponse("Invalid request: " + e.getMessage(), new ArrayList<>())
            );
        } catch (Exception e) {
            logger.error("Error processing voice food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new VoiceFoodLogResponse("Failed to process voice input. Please try again.", new ArrayList<>())
            );
        }
    }
}
