package com.healthapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "menstrual_cycles")
public class MenstrualCycle {
    
    public enum Status {
        ACTIVE, DELETED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Column(name = "period_start_date", nullable = false)
    @NotNull(message = "Period start date is required")
    @PastOrPresent(message = "Period start date must be today or in the past")
    private LocalDate periodStartDate;
    
    @Column(name = "cycle_length")
    @Min(value = 21, message = "Cycle length must be at least 21 days")
    @Max(value = 40, message = "Cycle length must be at most 40 days")
    private Integer cycleLength = 28;
    
    @Column(name = "period_duration")
    @Min(value = 1, message = "Period duration must be at least 1 day")
    @Max(value = 10, message = "Period duration must be at most 10 days")
    private Integer periodDuration = 5;
    
    @Column(name = "is_cycle_regular")
    private Boolean isCycleRegular = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public MenstrualCycle() {}
    
    public MenstrualCycle(Long userId, LocalDate periodStartDate) {
        this.userId = userId;
        this.periodStartDate = periodStartDate;
    }
    
    public MenstrualCycle(Long userId, LocalDate periodStartDate, Integer cycleLength, 
                         Integer periodDuration, Boolean isCycleRegular) {
        this.userId = userId;
        this.periodStartDate = periodStartDate;
        this.cycleLength = cycleLength;
        this.periodDuration = periodDuration;
        this.isCycleRegular = isCycleRegular;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
