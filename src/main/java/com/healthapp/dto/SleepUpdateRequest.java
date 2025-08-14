package com.healthapp.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request DTO for updating an existing sleep entry")
public class SleepUpdateRequest {
    
    @Schema(
        description = "Hours of sleep (0.0 to 24.0, max 1 decimal place)",
        example = "8.0",
        minimum = "0.0",
        maximum = "24.0"
    )
    @DecimalMin(value = "0.0", message = "Hours must be at least 0.0")
    @DecimalMax(value = "24.0", message = "Hours cannot exceed 24.0")
    @Digits(integer = 2, fraction = 1, message = "Hours can have at most 1 decimal place")
    private BigDecimal hours;
    
    @Schema(
        description = "Optional note about the sleep (max 200 characters)",
        example = "Logged after breakfast",
        maxLength = 200
    )
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Default constructor
    public SleepUpdateRequest() {}
    
    // Constructor with all fields
    public SleepUpdateRequest(BigDecimal hours, String note) {
        this.hours = hours;
        this.note = note;
    }
    
    // Getters and Setters
    public BigDecimal getHours() {
        return hours;
    }
    
    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}
