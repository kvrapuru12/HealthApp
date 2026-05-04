package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.VoiceFoodLogRequest;
import com.healthapp.dto.VoiceFoodLogResponse;
import com.healthapp.exception.VoiceFoodLogException;
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
        @ApiResponse(responseCode = "400", description = "Invalid request (body includes message and errorCode INVALID_REQUEST)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "422", description = "No foods parsed or could not create logs (errorCode NO_FOOD_PARSED or NO_LOGS_CREATED)"),
        @ApiResponse(responseCode = "502", description = "AI parse failed (errorCode AI_PARSE_FAILED)"),
        @ApiResponse(responseCode = "503", description = "AI service not configured (errorCode AI_SERVICE_UNAVAILABLE)"),
        @ApiResponse(responseCode = "500", description = "Unexpected failure (errorCode INTERNAL_ERROR or FOOD_ITEM_SAVE_FAILED)")
    })
    public ResponseEntity<?> createFoodLogFromVoice(@Valid @RequestBody VoiceFoodLogRequest request) {
        try {
            if (voiceFoodLogService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(VoiceFoodLogResponse.error(
                                "Voice food logging is not available right now. Please try again later or add food manually.",
                                "AI_SERVICE_UNAVAILABLE"));
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            
            VoiceFoodLogResponse response = voiceFoodLogService.processVoiceFoodLog(request, authenticatedUserId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (VoiceFoodLogException e) {
            logger.warn("Voice food log failed [{}]: {}", e.getErrorCode(), e.getUserMessage());
            return ResponseEntity.status(e.getHttpStatus())
                    .body(VoiceFoodLogResponse.error(e.getUserMessage(), e.getErrorCode()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid voice food request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(VoiceFoodLogResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            logger.error("Error processing voice food log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    VoiceFoodLogResponse.error(
                            "Something went wrong while saving your food. Please try again or add manually.",
                            "INTERNAL_ERROR"));
        }
    }
}
