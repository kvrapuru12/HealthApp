package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response for creating a menstrual cycle entry")
public class MenstrualCycleCreateResponse {
    
    @Schema(description = "Cycle ID", example = "401")
    private Long id;
    
    @Schema(description = "Creation timestamp", example = "2025-09-03T09:00:00Z")
    private LocalDateTime createdAt;
    
    // Constructors
    public MenstrualCycleCreateResponse() {}
    
    public MenstrualCycleCreateResponse(Long id, LocalDateTime createdAt) {
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
