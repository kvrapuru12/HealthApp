package com.healthapp.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request DTO for creating a new app rating")
public class AppRatingCreateRequest {
    
    @Schema(
        description = "Numeric ID of the user submitting the rating. Must match authenticated user unless role=admin.",
        example = "123",
        required = true
    )
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(
        description = "Rating value from 1 to 5",
        example = "5",
        minimum = "1",
        maximum = "5",
        required = true
    )
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    
    @Schema(
        description = "Optional feedback text",
        example = "Great app!",
        required = false
    )
    private String feedback;
    
    @Schema(
        description = "Platform where the app is used (ios, android, web)",
        example = "ios",
        required = true
    )
    @NotBlank(message = "Platform is required")
    @Size(max = 10, message = "Platform must not exceed 10 characters")
    private String platform;
    
    @Schema(
        description = "Version of the app",
        example = "1.0.0",
        required = true
    )
    @NotBlank(message = "App version is required")
    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;
    
    // Default constructor
    public AppRatingCreateRequest() {}
    
    // Constructor with all fields
    public AppRatingCreateRequest(Long userId, Integer rating, String feedback, String platform, String appVersion) {
        this.userId = userId;
        this.rating = rating;
        this.feedback = feedback;
        this.platform = platform;
        this.appVersion = appVersion;
    }
    
    // Method to convert DTO to Entity (requires User entity)
    // This will be used in the service layer where User is available
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
}

