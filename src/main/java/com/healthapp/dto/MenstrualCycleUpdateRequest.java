package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Request to update a menstrual cycle entry")
public class MenstrualCycleUpdateRequest {
    
    @Schema(description = "Period start date", example = "2025-09-01")
    private LocalDate periodStartDate;
    
    @Min(value = 21, message = "Cycle length must be at least 21 days")
    @Max(value = 40, message = "Cycle length must be at most 40 days")
    @Schema(description = "Cycle length in days", example = "28")
    private Integer cycleLength;
    
    @Min(value = 1, message = "Period duration must be at least 1 day")
    @Max(value = 10, message = "Period duration must be at most 10 days")
    @Schema(description = "Period duration in days", example = "5")
    private Integer periodDuration;
    
    @Schema(description = "Whether the cycle is regular", example = "true")
    private Boolean isCycleRegular;
    
    // Constructors
    public MenstrualCycleUpdateRequest() {}
    
    public MenstrualCycleUpdateRequest(LocalDate periodStartDate, Integer cycleLength, 
                                     Integer periodDuration, Boolean isCycleRegular) {
        this.periodStartDate = periodStartDate;
        this.cycleLength = cycleLength;
        this.periodDuration = periodDuration;
        this.isCycleRegular = isCycleRegular;
    }
    
    // Getters and Setters
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
