package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response DTO for creating a new sleep entry")
public class SleepCreateResponse {
    
    @Schema(description = "Unique identifier for the created sleep entry", example = "101")
    private Long id;
    
    @Schema(description = "Timestamp when the entry was created", example = "2025-08-14T06:31:00Z")
    private LocalDateTime createdAt;
    
    // Default constructor
    public SleepCreateResponse() {}
    
    // Constructor with all fields
    public SleepCreateResponse(Long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
