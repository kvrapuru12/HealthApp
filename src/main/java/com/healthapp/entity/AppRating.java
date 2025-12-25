package com.healthapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_ratings", indexes = {
    @Index(name = "idx_app_ratings_user_id", columnList = "user_id"),
    @Index(name = "idx_app_ratings_created_at", columnList = "created_at"),
    @Index(name = "idx_app_ratings_platform", columnList = "platform")
})
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "App rating entity for user feedback and ratings")
public class AppRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated primary key", example = "456")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "User who submitted the rating")
    private User user;
    
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Column(nullable = false)
    @Schema(description = "Rating value from 1 to 5", example = "5")
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    @Schema(description = "Optional feedback text", example = "Great app!")
    private String feedback;
    
    @NotBlank(message = "Platform is required")
    @Size(max = 10, message = "Platform must not exceed 10 characters")
    @Column(nullable = false, length = 10)
    @Schema(description = "Platform where the app is used (ios, android, web)", example = "ios")
    private String platform;
    
    @NotBlank(message = "App version is required")
    @Size(max = 20, message = "App version must not exceed 20 characters")
    @Column(name = "app_version", nullable = false, length = 20)
    @Schema(description = "Version of the app", example = "1.0.0")
    private String appVersion;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Timestamp when the rating was created", example = "2025-12-25T10:30:00Z")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Timestamp when the rating was last updated", example = "2025-12-25T10:30:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public AppRating() {}
    
    public AppRating(User user, Integer rating, String platform, String appVersion) {
        this.user = user;
        this.rating = rating;
        this.platform = platform;
        this.appVersion = appVersion;
    }
    
    public AppRating(User user, Integer rating, String feedback, String platform, String appVersion) {
        this.user = user;
        this.rating = rating;
        this.feedback = feedback;
        this.platform = platform;
        this.appVersion = appVersion;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

