package com.healthapp.dto;

import com.healthapp.entity.MoodEntry;
import com.healthapp.entity.User;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.lang.Long;

@Schema(description = "Request DTO for creating a new mood entry")
public class MoodCreateRequest {
    
    @Schema(
        description = "Numeric ID of the user creating the mood entry. Must match authenticated user unless role=admin.",
        example = "2",
        required = true
    )
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(
        description = "Timestamp when the mood was logged. Must not be more than 10 minutes in the future.",
        example = "2025-08-12T07:30:00Z",
        required = true
    )
    @NotNull(message = "Logged at timestamp is required")
    private LocalDateTime loggedAt;
    
    @Schema(
        description = "The mood/emotional state being logged",
        example = "HAPPY",
        required = true
    )
    @NotNull(message = "Mood is required")
    private MoodEntry.Mood mood;
    
    @Schema(
        description = "Intensity level of the mood (1-10 scale). Optional field.",
        example = "6",
        minimum = "1",
        maximum = "10"
    )
    @Min(value = 1, message = "Intensity must be between 1 and 10")
    @Max(value = 10, message = "Intensity must be between 1 and 10")
    private Integer intensity;
    
    @Schema(
        description = "Optional note about the mood (max 200 characters)",
        example = "Feeling great after morning workout!",
        maxLength = 200
    )
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Default constructor
    public MoodCreateRequest() {}
    
    // Constructor with required fields
    public MoodCreateRequest(Long userId, LocalDateTime loggedAt, MoodEntry.Mood mood) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.mood = mood;
    }
    
    // Constructor with all fields
    public MoodCreateRequest(Long userId, LocalDateTime loggedAt, MoodEntry.Mood mood, Integer intensity, String note) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.mood = mood;
        this.intensity = intensity;
        this.note = note;
    }
    
    // Method to convert DTO to Entity
    public MoodEntry toEntity(User user) {
        MoodEntry moodEntry = new MoodEntry();
        moodEntry.setUser(user);
        moodEntry.setLoggedAt(this.loggedAt);
        moodEntry.setMood(this.mood);
        moodEntry.setIntensity(this.intensity);
        moodEntry.setNote(this.note);
        return moodEntry;
    }
    
    // Getters and Setters
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
}
