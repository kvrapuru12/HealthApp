package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.VoiceActivityLogRequest;
import com.healthapp.dto.VoiceActivityLogResponse;
import com.healthapp.service.VoiceActivityLogService;
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
@RequestMapping("/ai/activity-log")
@Tag(name = "AI Voice-to-Activity Logging", description = "APIs for logging activities using natural language voice input")
@CrossOrigin(origins = "*")
public class VoiceActivityController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceActivityController.class);

    @Autowired(required = false)
    private VoiceActivityLogService voiceActivityLogService;

    @PostMapping("/from-voice")
    @RateLimit(value = 20, timeUnit = "MINUTES")
    @Operation(summary = "Log activity from voice/text input", 
               description = "Parse natural language input and automatically log an activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Activity logged successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or unable to parse input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "503", description = "AI service not available")
    })
    public ResponseEntity<?> logActivityFromVoice(@Valid @RequestBody VoiceActivityLogRequest request) {
        try {
            // Check if AI service is available
            if (voiceActivityLogService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "AI voice parsing service is not available. Please configure OpenAI API key."));
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            VoiceActivityLogResponse response = voiceActivityLogService.processVoiceActivityLog(
                    request.getUserId(), request.getVoiceText(), authenticatedUserId, isAdmin);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error processing voice activity log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Unable to extract activity info from input. Please try rephrasing."));
        } catch (Exception e) {
            logger.error("Unexpected error processing voice activity log: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }
}
