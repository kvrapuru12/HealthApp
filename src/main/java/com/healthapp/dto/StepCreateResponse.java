package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response after creating a new step entry")
public class StepCreateResponse {
    
    @Schema(description = "Unique identifier for the created step entry", example = "311")
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was created", example = "2025-08-14T07:01:00Z")
    private LocalDateTime createdAt;
    
    // Constructors
    public StepCreateResponse() {}
    
    public StepCreateResponse(Long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
