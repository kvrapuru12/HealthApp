package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.WeightEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response for weight entry data")
public class WeightResponse {
    
    @Schema(description = "Unique identifier for the weight entry", example = "431")
    private Long id;
    
    @Schema(description = "ID of the user who logged the weight measurement", example = "12")
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the weight was measured", example = "2025-08-14T07:00:00Z")
    private LocalDateTime loggedAt;
    
    @Schema(description = "Weight in kilograms", example = "62.3")
    private BigDecimal weight;
    
    @Schema(description = "Optional note about the weight measurement", example = "Morning, post-bathroom")
    private String note;
    
    @Schema(description = "Status of the weight entry", example = "active")
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was created", example = "2025-08-14T07:01:00Z")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "When the entry was last updated", example = "2025-08-14T07:01:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WeightResponse() {}
    
    public WeightResponse(WeightEntry weightEntry) {
        this.id = weightEntry.getId();
        this.userId = weightEntry.getUserId();
        this.loggedAt = weightEntry.getLoggedAt();
        this.weight = weightEntry.getWeight();
        this.note = weightEntry.getNote();
        this.status = weightEntry.getStatus().name().toLowerCase();
        this.createdAt = weightEntry.getCreatedAt();
        this.updatedAt = weightEntry.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
