package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WeightCreateRequest {
    
    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;
    
    @NotNull(message = "Logged at time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime loggedAt;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    private BigDecimal weight;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Constructors
    public WeightCreateRequest() {}
    
    public WeightCreateRequest(Long userId, LocalDateTime loggedAt, BigDecimal weight, String note) {
        this.userId = userId;
        this.loggedAt = loggedAt;
        this.weight = weight;
        this.note = note;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
