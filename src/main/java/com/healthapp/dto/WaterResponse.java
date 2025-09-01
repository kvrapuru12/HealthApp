package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.WaterEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Water consumption entry response")
public class WaterResponse {
    
    @Schema(description = "Unique identifier for the water entry", example = "221")
    private Long id;
    
    @Schema(description = "ID of the user who logged the water consumption", example = "12")
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the water was consumed", example = "2025-08-14T08:00:00Z")
    private LocalDateTime loggedAt;
    
    @Schema(description = "Amount of water consumed in milliliters", example = "350")
    private Integer amount;
    
    @Schema(description = "Optional note about the water consumption", example = "Post-workout hydration")
    private String note;
    
    @Schema(description = "Status of the water entry", example = "active")
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was created", example = "2025-08-14T08:01:00Z")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was last updated", example = "2025-08-14T08:01:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WaterResponse() {}
    
    public WaterResponse(WaterEntry waterEntry) {
        this.id = waterEntry.getId();
        this.userId = waterEntry.getUserId();
        this.loggedAt = waterEntry.getLoggedAt();
        this.amount = waterEntry.getAmount();
        this.note = waterEntry.getNote();
        this.status = waterEntry.getStatus().name().toLowerCase();
        this.createdAt = waterEntry.getCreatedAt();
        this.updatedAt = waterEntry.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
