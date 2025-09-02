package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WeightUpdateRequest {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime loggedAt;
    
    @DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    private BigDecimal weight;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Constructors
    public WeightUpdateRequest() {}
    
    public WeightUpdateRequest(LocalDateTime loggedAt, BigDecimal weight, String note) {
        this.loggedAt = loggedAt;
        this.weight = weight;
        this.note = note;
    }
    
    // Getters and Setters
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
