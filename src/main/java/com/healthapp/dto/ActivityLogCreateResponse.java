package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ActivityLogCreateResponse {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    private BigDecimal caloriesBurned;
    
    // Constructors
    public ActivityLogCreateResponse() {}
    
    public ActivityLogCreateResponse(Long id, LocalDateTime createdAt, BigDecimal caloriesBurned) {
        this.id = id;
        this.createdAt = createdAt;
        this.caloriesBurned = caloriesBurned;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public BigDecimal getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(BigDecimal caloriesBurned) { this.caloriesBurned = caloriesBurned; }
}
