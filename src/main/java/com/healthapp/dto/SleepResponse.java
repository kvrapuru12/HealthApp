package com.healthapp.dto;

import com.healthapp.entity.SleepEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response DTO for sleep entry data")
public class SleepResponse {
    
    @Schema(description = "Unique identifier for the sleep entry", example = "101")
    private Long id;
    
    @Schema(description = "ID of the user who logged the sleep entry", example = "12")
    private Long userId;
    
    @Schema(description = "Timestamp when the sleep was logged", example = "2025-08-14T06:30:00Z")
    private LocalDateTime loggedAt;
    
    @Schema(description = "Hours of sleep", example = "7.5")
    private BigDecimal hours;
    
    @Schema(description = "Optional note about the sleep", example = "Late bedtime")
    private String note;
    
    @Schema(description = "Current status of the sleep entry", example = "active")
    private String status;
    
    @Schema(description = "Timestamp when the entry was created", example = "2025-08-14T06:31:00Z")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the entry was last updated", example = "2025-08-14T06:31:00Z")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public SleepResponse() {}
    
    // Constructor from entity
    public SleepResponse(SleepEntry sleepEntry) {
        this.id = sleepEntry.getId();
        this.userId = sleepEntry.getUser().getId();
        this.loggedAt = sleepEntry.getLoggedAt();
        this.hours = sleepEntry.getHours();
        this.note = sleepEntry.getNote();
        this.status = sleepEntry.getStatus().name().toLowerCase();
        this.createdAt = sleepEntry.getCreatedAt();
        this.updatedAt = sleepEntry.getUpdatedAt();
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
    
    public BigDecimal getHours() {
        return hours;
    }
    
    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
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
