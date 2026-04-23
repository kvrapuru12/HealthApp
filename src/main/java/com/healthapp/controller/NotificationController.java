package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.dto.*;
import com.healthapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Management", description = "APIs for notification device registration and reminder preferences")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/devices/register")
    @RateLimit(value = 30)
    @Operation(summary = "Register or refresh notification device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> registerDevice(@Valid @RequestBody NotificationDeviceRegisterRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            NotificationDeviceResponse response = notificationService.registerDevice(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to register notification device: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during device registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to register device"));
        }
    }

    @DeleteMapping("/devices/{deviceId}")
    @RateLimit(value = 30)
    @Operation(summary = "Deactivate notification device for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device removed successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> deleteDevice(@PathVariable String deviceId) {
        try {
            Long userId = getAuthenticatedUserId();
            Map<String, String> response = notificationService.deleteDevice(userId, deviceId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete device"));
        }
    }

    @GetMapping("/preferences")
    @RateLimit(value = 60)
    @Operation(summary = "Get reminder preferences for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getPreferences() {
        try {
            Long userId = getAuthenticatedUserId();
            NotificationPreferencesResponse response = notificationService.getPreferences(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error fetching notification preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch preferences"));
        }
    }

    @PatchMapping("/preferences")
    @RateLimit(value = 30)
    @Operation(summary = "Update reminder preferences for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> patchPreferences(@Valid @RequestBody NotificationPreferencesPatchRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            NotificationPreferencesResponse response = notificationService.updatePreferences(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating notification preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to update preferences"));
        }
    }

    @PostMapping("/test")
    @RateLimit(value = 15)
    @Operation(summary = "Queue a test notification for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test notification queued"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> sendTestNotification(@Valid @RequestBody NotificationTestRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            Map<String, Object> response = notificationService.sendTestNotification(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error while creating test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send test notification"));
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
