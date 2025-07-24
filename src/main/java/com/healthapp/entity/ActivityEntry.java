package com.healthapp.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_entries")
@EntityListeners(AuditingEntityListener.class)
public class ActivityEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "activity_name", nullable = false)
    private String activityName;
    
    @Column(name = "calories_burned")
    private Integer caloriesBurned;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "activity_type")
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;
    
    @Column(name = "intensity_level")
    @Enumerated(EnumType.STRING)
    private IntensityLevel intensityLevel;
    
    @Column(name = "activity_date")
    private LocalDate activityDate;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "distance_km")
    private Double distanceKm;
    
    @Column(name = "steps")
    private Integer steps;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public ActivityEntry() {}
    
    public ActivityEntry(User user, String activityName, Integer caloriesBurned, Integer durationMinutes) {
        this.user = user;
        this.activityName = activityName;
        this.caloriesBurned = caloriesBurned;
        this.durationMinutes = durationMinutes;
        this.activityDate = LocalDate.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getActivityName() {
        return activityName;
    }
    
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
    
    public Integer getCaloriesBurned() {
        return caloriesBurned;
    }
    
    public void setCaloriesBurned(Integer caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public ActivityType getActivityType() {
        return activityType;
    }
    
    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }
    
    public IntensityLevel getIntensityLevel() {
        return intensityLevel;
    }
    
    public void setIntensityLevel(IntensityLevel intensityLevel) {
        this.intensityLevel = intensityLevel;
    }
    
    public LocalDate getActivityDate() {
        return activityDate;
    }
    
    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public Integer getSteps() {
        return steps;
    }
    
    public void setSteps(Integer steps) {
        this.steps = steps;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
    
    public enum ActivityType {
        RUNNING, WALKING, CYCLING, SWIMMING, WEIGHT_TRAINING, 
        YOGA, PILATES, DANCING, HIKING, TENNIS, BASKETBALL, 
        SOCCER, GOLF, SKIING, ROWING, ELLIPTICAL, STAIR_CLIMBER
    }
    
    public enum IntensityLevel {
        LOW, MODERATE, HIGH, VERY_HIGH
    }
} 