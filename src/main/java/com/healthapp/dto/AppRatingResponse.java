package com.healthapp.dto;

import com.healthapp.entity.AppRating;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response DTO for app rating data")
public class AppRatingResponse {
    
    @Schema(description = "Auto-generated primary key", example = "456")
    private Long id;
    
    @Schema(description = "Numeric ID of the user who submitted the rating", example = "123")
    private Long userId;
    
    @Schema(description = "Rating value from 1 to 5", example = "5")
    private Integer rating;
    
    @Schema(description = "Optional feedback text", example = "Great app!")
    private String feedback;
    
    @Schema(description = "Platform where the app is used (ios, android, web)", example = "ios")
    private String platform;
    
    @Schema(description = "Version of the app", example = "1.0.0")
    private String appVersion;
    
    @Schema(description = "Timestamp when the rating was created", example = "2025-12-25T10:30:00Z")
    private LocalDateTime createdAt;
    
    // Default constructor
    public AppRatingResponse() {}
    
    // Constructor from entity
    public AppRatingResponse(AppRating appRating) {
        this.id = appRating.getId();
        this.userId = appRating.getUser().getId();
        this.rating = appRating.getRating();
        this.feedback = appRating.getFeedback();
        this.platform = appRating.getPlatform();
        this.appVersion = appRating.getAppVersion();
        this.createdAt = appRating.getCreatedAt();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

