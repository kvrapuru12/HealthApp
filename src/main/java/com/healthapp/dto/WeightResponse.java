package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.WeightEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WeightResponse {
    
    private Long id;
    
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime loggedAt;
    
    private BigDecimal weight;
    
    private String note;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
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
