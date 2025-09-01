package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "Request to update an existing step entry")
public class StepUpdateRequest {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @PastOrPresent(message = "Logged at time cannot be in the future")
    @Schema(description = "When the steps were logged", example = "2025-08-14T07:00:00Z")
    private LocalDateTime loggedAt;
    
    @Min(value = 0, message = "Step count must be at least 0")
    @Max(value = 100000, message = "Step count cannot exceed 100,000")
    @Schema(description = "Number of steps logged", example = "9000", minimum = "0", maximum = "100000")
    private Integer stepCount;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Schema(description = "Optional note about the steps", example = "Updated after evening walk", maxLength = 200)
    private String note;
    
    // Constructors
    public StepUpdateRequest() {}
    
    public StepUpdateRequest(LocalDateTime loggedAt, Integer stepCount, String note) {
        this.loggedAt = loggedAt;
        this.stepCount = stepCount;
        this.note = note;
    }
    
    // Getters and Setters
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
