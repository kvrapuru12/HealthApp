package com.healthapp.controller;

import com.healthapp.dto.AppRatingCreateRequest;
import com.healthapp.dto.AppRatingResponse;
import com.healthapp.service.AppRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app-ratings")
public class AppRatingController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppRatingController.class);
    
    @Autowired
    private AppRatingService appRatingService;
    
    @PostMapping
    @Operation(
        summary = "Create app rating", 
        description = "Create a new app rating with user feedback. Users can only submit ratings for their own account unless they have ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "App rating created successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AppRatingResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID mismatch - cannot submit rating for another user")
    })
    public ResponseEntity<AppRatingResponse> createAppRating(
            @Valid @RequestBody AppRatingCreateRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || authentication.getPrincipal() == null) {
                logger.error("Authentication is null or principal is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long authenticatedUserId = (Long) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            AppRatingResponse appRatingResponse = appRatingService.createAppRating(
                    request, authenticatedUserId, isAdmin);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(appRatingResponse);
            
        } catch (SecurityException e) {
            logger.error("Security violation in app rating creation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request in app rating creation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating app rating: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

