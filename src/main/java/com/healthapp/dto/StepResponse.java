package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.StepEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Step entry response with full details")
public class StepResponse {
    
    @Schema(description = "Unique identifier for the step entry", example = "311")
    private Long id;
    
    @Schema(description = "ID of the user who logged the steps", example = "12")
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the steps were logged", example = "2025-08-14T07:00:00Z")
    private LocalDateTime loggedAt;
    
    @Schema(description = "Number of steps logged", example = "8200")
    private Integer stepCount;
    
    @Schema(description = "Optional note about the steps", example = "Morning walk + commute")
    private String note;
    
    @Schema(description = "Status of the step entry", example = "active")
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was created", example = "2025-08-14T07:01:00Z")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was last updated", example = "2025-08-14T07:01:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public StepResponse() {}
    
    public StepResponse(StepEntry stepEntry) {
        this.id = stepEntry.getId();
        this.userId = stepEntry.getUserId();
        this.loggedAt = stepEntry.getLoggedAt();
        this.stepCount = stepEntry.getStepCount();
        this.note = stepEntry.getNote();
        this.status = stepEntry.getStatus().name().toLowerCase();
        this.createdAt = stepEntry.getCreatedAt();
        this.updatedAt = stepEntry.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
