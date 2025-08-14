package com.healthapp.dto;

import com.healthapp.entity.SleepEntry;
import com.healthapp.entity.User;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request DTO for creating a new sleep entry")
public class SleepCreateRequest {
    
    @Schema(
        description = "Numeric ID of the user creating the sleep entry. Must match authenticated user unless role=admin.",
        example = "12",
        required = true
    )
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(
        description = "Timestamp when the sleep was logged. Must not be more than 10 minutes in the future.",
        example = "2025-08-14T06:30:00Z",
        required = true
    )
    @NotNull(message = "Logged at timestamp is required")
    private LocalDateTime loggedAt;
    
    @Schema(
        description = "Hours of sleep (0.0 to 24.0, max 1 decimal place)",
        example = "7.5",
        required = true,
        minimum = "0.0",
        maximum = "24.0"
    )
    @NotNull(message = "Hours is required")
    @DecimalMin(value = "0.0", message = "Hours must be at least 0.0")
    @DecimalMax(value = "24.0", message = "Hours cannot exceed 24.0")
    @Digits(integer = 2, fraction = 1, message = "Hours can have at most 1 decimal place")
    private BigDecimal hours;
    
    @Schema(
        description = "Optional note about the sleep (max 200 characters)",
        example = "Late bedtime",
        maxLength = 200
    )
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Default constructor
    public SleepCreateRequest() {}
    
    // Constructor with required fields
    public SleepCreateRequest(Long userId, LocalDateTime loggedAt, BigDecimal hours) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.hours = hours;
    }
    
    // Constructor with all fields
    public SleepCreateRequest(Long userId, LocalDateTime loggedAt, BigDecimal hours, String note) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.hours = hours;
        this.note = note;
    }
    
    // Method to convert DTO to Entity
    public SleepEntry toEntity(User user) {
        SleepEntry sleepEntry = new SleepEntry();
        sleepEntry.setUser(user);
        sleepEntry.setLoggedAt(this.loggedAt);
        sleepEntry.setHours(this.hours);
        sleepEntry.setNote(this.note);
        return sleepEntry;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
    
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
