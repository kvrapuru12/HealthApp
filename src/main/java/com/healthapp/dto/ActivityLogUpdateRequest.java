package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

public class ActivityLogUpdateRequest {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime loggedAt;
    
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 300, message = "Duration cannot exceed 300 minutes")
    private Integer durationMinutes;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Constructors
    public ActivityLogUpdateRequest() {}
    
    public ActivityLogUpdateRequest(LocalDateTime loggedAt, Integer durationMinutes, String note) {
        this.loggedAt = loggedAt;
        this.durationMinutes = durationMinutes;
        this.note = note;
    }
    
    // Getters and Setters
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
