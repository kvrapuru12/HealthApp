package com.healthapp.dto;

import com.healthapp.entity.MenstrualCycle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Response for menstrual cycle details")
public class MenstrualCycleResponse {
    
    @Schema(description = "Cycle ID", example = "401")
    private Long id;
    
    @Schema(description = "User ID", example = "12")
    private Long userId;
    
    @Schema(description = "Period start date", example = "2025-09-01")
    private LocalDate periodStartDate;
    
    @Schema(description = "Cycle length in days", example = "28")
    private Integer cycleLength;
    
    @Schema(description = "Period duration in days", example = "5")
    private Integer periodDuration;
    
    @Schema(description = "Whether the cycle is regular", example = "true")
    private Boolean isCycleRegular;
    
    @Schema(description = "Cycle status", example = "ACTIVE")
    private String status;
    
    @Schema(description = "Creation timestamp", example = "2025-09-03T09:00:00Z")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp", example = "2025-09-03T09:00:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public MenstrualCycleResponse() {}
    
    public MenstrualCycleResponse(MenstrualCycle cycle) {
        this.id = cycle.getId();
        this.userId = cycle.getUserId();
        this.periodStartDate = cycle.getPeriodStartDate();
        this.cycleLength = cycle.getCycleLength();
        this.periodDuration = cycle.getPeriodDuration();
        this.isCycleRegular = cycle.getIsCycleRegular();
        this.status = cycle.getStatus().name();
        this.createdAt = cycle.getCreatedAt();
        this.updatedAt = cycle.getUpdatedAt();
    }
    
    public MenstrualCycleResponse(Long id, Long userId, LocalDate periodStartDate, Integer cycleLength,
                                Integer periodDuration, Boolean isCycleRegular, String status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.periodStartDate = periodStartDate;
        this.cycleLength = cycleLength;
        this.periodDuration = periodDuration;
        this.isCycleRegular = isCycleRegular;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
    
    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }
    
    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }
    
    public Integer getCycleLength() {
        return cycleLength;
    }
    
    public void setCycleLength(Integer cycleLength) {
        this.cycleLength = cycleLength;
    }
    
    public Integer getPeriodDuration() {
        return periodDuration;
    }
    
    public void setPeriodDuration(Integer periodDuration) {
        this.periodDuration = periodDuration;
    }
    
    public Boolean getIsCycleRegular() {
        return isCycleRegular;
    }
    
    public void setIsCycleRegular(Boolean isCycleRegular) {
        this.isCycleRegular = isCycleRegular;
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
