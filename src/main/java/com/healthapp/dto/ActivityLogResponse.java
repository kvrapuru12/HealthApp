package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.ActivityLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ActivityLogResponse {
    
    private Long id;
    
    private Long userId;
    
    private Long activityId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime loggedAt;
    
    private Integer durationMinutes;
    
    private BigDecimal caloriesBurned;
    
    private String note;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ActivityLogResponse() {}
    
    public ActivityLogResponse(ActivityLog activityLog) {
        this.id = activityLog.getId();
        this.userId = activityLog.getUserId();
        this.activityId = activityLog.getActivityId();
        this.loggedAt = activityLog.getLoggedAt();
        this.durationMinutes = activityLog.getDurationMinutes();
        this.caloriesBurned = activityLog.getCaloriesBurned();
        this.note = activityLog.getNote();
        this.status = activityLog.getStatus().name().toLowerCase();
        this.createdAt = activityLog.getCreatedAt();
        this.updatedAt = activityLog.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public BigDecimal getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(BigDecimal caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
