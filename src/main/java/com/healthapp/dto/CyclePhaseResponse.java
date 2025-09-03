package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response for current menstrual cycle phase")
public class CyclePhaseResponse {
    
    @Schema(description = "Current phase", example = "luteal", allowableValues = {"menstrual", "follicular", "ovulatory", "luteal"})
    private String phase;
    
    @Schema(description = "Phase start date", example = "2025-09-15")
    private LocalDate phaseStartDate;
    
    @Schema(description = "Phase end date", example = "2025-09-28")
    private LocalDate phaseEndDate;
    
    @Schema(description = "Days spent in current phase", example = "3")
    private Integer daysInPhase;
    
    @Schema(description = "Current day in cycle", example = "18")
    private Integer cycleDay;
    
    @Schema(description = "Next period expected date", example = "2025-09-29")
    private LocalDate nextPeriodExpected;
    
    // Constructors
    public CyclePhaseResponse() {}
    
    public CyclePhaseResponse(String phase, LocalDate phaseStartDate, LocalDate phaseEndDate,
                            Integer daysInPhase, Integer cycleDay, LocalDate nextPeriodExpected) {
        this.phase = phase;
        this.phaseStartDate = phaseStartDate;
        this.phaseEndDate = phaseEndDate;
        this.daysInPhase = daysInPhase;
        this.cycleDay = cycleDay;
        this.nextPeriodExpected = nextPeriodExpected;
    }
    
    // Getters and Setters
    public String getPhase() {
        return phase;
    }
    
    public void setPhase(String phase) {
        this.phase = phase;
    }
    
    public LocalDate getPhaseStartDate() {
        return phaseStartDate;
    }
    
    public void setPhaseStartDate(LocalDate phaseStartDate) {
        this.phaseStartDate = phaseStartDate;
    }
    
    public LocalDate getPhaseEndDate() {
        return phaseEndDate;
    }
    
    public void setPhaseEndDate(LocalDate phaseEndDate) {
        this.phaseEndDate = phaseEndDate;
    }
    
    public Integer getDaysInPhase() {
        return daysInPhase;
    }
    
    public void setDaysInPhase(Integer daysInPhase) {
        this.daysInPhase = daysInPhase;
    }
    
    public Integer getCycleDay() {
        return cycleDay;
    }
    
    public void setCycleDay(Integer cycleDay) {
        this.cycleDay = cycleDay;
    }
    
    public LocalDate getNextPeriodExpected() {
        return nextPeriodExpected;
    }
    
    public void setNextPeriodExpected(LocalDate nextPeriodExpected) {
        this.nextPeriodExpected = nextPeriodExpected;
    }
}
