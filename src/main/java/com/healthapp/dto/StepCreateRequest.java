package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

@Schema(description = "Request to create a new step entry")
public class StepCreateRequest {
    
    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    @Schema(description = "ID of the user logging the steps", example = "12")
    private Long userId;
    
    @NotNull(message = "Logged at time is required")
    @PastOrPresent(message = "Logged at time cannot be in the future")
    @Schema(description = "When the steps were logged", example = "2025-08-14T07:00:00Z")
    private OffsetDateTime loggedAt;
    
    @NotNull(message = "Step count is required")
    @Min(value = 0, message = "Step count must be at least 0")
    @Max(value = 100000, message = "Step count cannot exceed 100,000")
    @Schema(description = "Number of steps logged", example = "8200", minimum = "0", maximum = "100000")
    private Integer stepCount;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Schema(description = "Optional note about the steps", example = "Morning walk + commute", maxLength = 200)
    private String note;
    
    // Constructors
    public StepCreateRequest() {}
    
    public StepCreateRequest(Long userId, OffsetDateTime loggedAt, Integer stepCount, String note) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.stepCount = stepCount;
        this.note = note;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
