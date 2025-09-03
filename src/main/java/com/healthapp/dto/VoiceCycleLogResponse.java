package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response for voice cycle logging")
public class VoiceCycleLogResponse {
    
    @Schema(description = "Success message", example = "Cycle logged")
    private String message;
    
    @Schema(description = "Period start date", example = "2025-09-02")
    private LocalDate periodStartDate;
    
    @Schema(description = "Estimated next period date", example = "2025-09-30")
    private LocalDate estimatedNextPeriod;
    
    // Constructors
    public VoiceCycleLogResponse() {}
    
    public VoiceCycleLogResponse(String message, LocalDate periodStartDate, LocalDate estimatedNextPeriod) {
        this.message = message;
        this.periodStartDate = periodStartDate;
        this.estimatedNextPeriod = estimatedNextPeriod;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }
    
    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }
    
    public LocalDate getEstimatedNextPeriod() {
        return estimatedNextPeriod;
    }
    
    public void setEstimatedNextPeriod(LocalDate estimatedNextPeriod) {
        this.estimatedNextPeriod = estimatedNextPeriod;
    }
}
