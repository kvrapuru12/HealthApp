package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.Activity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ActivityResponse {
    
    private Long id;
    
    private String name;
    
    private String category;
    
    private BigDecimal caloriesPerMinute;
    
    private String visibility;
    
    private Long createdById;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ActivityResponse() {}
    
    public ActivityResponse(Activity activity) {
        this.id = activity.getId();
        this.name = activity.getName();
        this.category = activity.getCategory();
        this.caloriesPerMinute = activity.getCaloriesPerMinute();
        this.visibility = activity.getVisibility().name().toLowerCase();
        this.createdById = activity.getCreatedById();
        this.status = activity.getStatus().name().toLowerCase();
        this.createdAt = activity.getCreatedAt();
        this.updatedAt = activity.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public BigDecimal getCaloriesPerMinute() { return caloriesPerMinute; }
    public void setCaloriesPerMinute(BigDecimal caloriesPerMinute) { this.caloriesPerMinute = caloriesPerMinute; }
    
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
