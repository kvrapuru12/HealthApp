package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Request to create a new menstrual cycle entry")
public class MenstrualCycleCreateRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "User ID", example = "12")
    private Long userId;
    
    @NotNull(message = "Period start date is required")
    @Schema(description = "Period start date", example = "2025-09-01")
    private LocalDate periodStartDate;
    
    @Min(value = 21, message = "Cycle length must be at least 21 days")
    @Max(value = 40, message = "Cycle length must be at most 40 days")
    @Schema(description = "Cycle length in days", example = "28", defaultValue = "28")
    private Integer cycleLength = 28;
    
    @Min(value = 1, message = "Period duration must be at least 1 day")
    @Max(value = 10, message = "Period duration must be at most 10 days")
    @Schema(description = "Period duration in days", example = "5", defaultValue = "5")
    private Integer periodDuration = 5;
    
    @Schema(description = "Whether the cycle is regular", example = "true", defaultValue = "true")
    private Boolean isCycleRegular = true;
    
    // Constructors
    public MenstrualCycleCreateRequest() {}
    
    public MenstrualCycleCreateRequest(Long userId, LocalDate periodStartDate) {
        this.userId = userId;
        this.periodStartDate = periodStartDate;
    }
    
    public MenstrualCycleCreateRequest(Long userId, LocalDate periodStartDate, Integer cycleLength, 
                                     Integer periodDuration, Boolean isCycleRegular) {
        this.userId = userId;
        this.periodStartDate = periodStartDate;
        this.cycleLength = cycleLength;
        this.periodDuration = periodDuration;
        this.isCycleRegular = isCycleRegular;
    }
    
    // Getters and Setters
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
}
