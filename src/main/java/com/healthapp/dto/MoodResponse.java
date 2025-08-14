package com.healthapp.dto;

import com.healthapp.entity.MoodEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.lang.Long;


@Schema(description = "Response DTO for mood entry data")
public class MoodResponse {
    
    @Schema(description = "Auto-generated numeric ID", example = "1")
    private Long id;
    

    
    @Schema(description = "Numeric ID of the user who created the mood entry", example = "2")
    private Long userId;
    
    @Schema(description = "Timestamp when the mood was logged", example = "2025-08-12T07:30:00Z")
    private LocalDateTime loggedAt;
    
    @Schema(description = "The logged mood/emotional state", example = "HAPPY")
    private MoodEntry.Mood mood;
    
    @Schema(description = "Intensity level of the mood (1-10 scale)", example = "6")
    private Integer intensity;
    
    @Schema(description = "Optional note about the mood", example = "Feeling great after morning workout!")
    private String note;
    
    @Schema(description = "Current status of the mood entry", example = "ACTIVE")
    private MoodEntry.Status status;
    
    @Schema(description = "Timestamp when the entry was created", example = "2025-08-12T07:30:00Z")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the entry was last updated", example = "2025-08-12T07:30:00Z")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public MoodResponse() {}
    
    // Constructor from entity
    public MoodResponse(MoodEntry moodEntry) {
        this.id = moodEntry.getId();
        this.userId = moodEntry.getUser().getId();
        this.loggedAt = moodEntry.getLoggedAt();
        this.mood = moodEntry.getMood();
        this.intensity = moodEntry.getIntensity();
        this.note = moodEntry.getNote();
        this.status = moodEntry.getStatus();
        this.createdAt = moodEntry.getCreatedAt();
        this.updatedAt = moodEntry.getUpdatedAt();
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
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
    
    public MoodEntry.Mood getMood() {
        return mood;
    }
    
    public void setMood(MoodEntry.Mood mood) {
        this.mood = mood;
    }
    
    public Integer getIntensity() {
        return intensity;
    }
    
    public void setIntensity(Integer intensity) {
        this.intensity = intensity;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public MoodEntry.Status getStatus() {
        return status;
    }
    
    public void setStatus(MoodEntry.Status status) {
        this.status = status;
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
