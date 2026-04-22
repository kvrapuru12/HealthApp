package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

@Schema(description = "Request to update a water entry")
public class WaterUpdateRequest {
    
    @Schema(description = "When the water was consumed (optional)", example = "2025-08-14T08:00:00Z")
    private OffsetDateTime loggedAt;
    
    @Min(value = 10, message = "Amount must be at least 10 ml")
    @Max(value = 5000, message = "Amount cannot exceed 5000 ml")
    @Schema(description = "Amount of water consumed in milliliters (optional)", example = "400", minimum = "10", maximum = "5000")
    private Integer amount;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Schema(description = "Optional note about the water consumption", example = "Adjusted after logging another sip", maxLength = 200)
    private String note;
    
    // Constructors
    public WaterUpdateRequest() {}
    
    public WaterUpdateRequest(OffsetDateTime loggedAt, Integer amount, String note) {
        this.loggedAt = loggedAt;
        this.amount = amount;
        this.note = note;
    }
    
    // Getters and Setters
    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
