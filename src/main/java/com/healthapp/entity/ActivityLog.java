package com.healthapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_logs_user", columnList = "user_id"),
    @Index(name = "idx_activity_logs_activity", columnList = "activity_id"),
    @Index(name = "idx_activity_logs_logged_at", columnList = "logged_at"),
    @Index(name = "idx_activity_logs_status", columnList = "status"),
    @Index(name = "idx_activity_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_activity_logs_user_logged_at", columnList = "user_id, logged_at")
})
@EntityListeners(AuditingEntityListener.class)
public class ActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    @JsonIgnore
    private Activity activity;
    
    @Column(name = "logged_at", nullable = false)
    @NotNull(message = "Logged at time is required")
    private LocalDateTime loggedAt;
    
    @Column(name = "duration_minutes", nullable = false)
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 300, message = "Duration cannot exceed 300 minutes")
    private Integer durationMinutes;
    
    @Column(name = "calories_burned", precision = 6, scale = 2)
    @DecimalMin(value = "0.0", message = "Calories burned cannot be negative")
    private BigDecimal caloriesBurned;
    
    @Column(name = "note", length = 200)
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    private Status status = Status.ACTIVE;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum Status {
        ACTIVE, DELETED
    }
    
    // Constructors
    public ActivityLog() {}
    
    public ActivityLog(User user, Activity activity, LocalDateTime loggedAt, Integer durationMinutes, String note) {
        this.user = user;
        this.activity = activity;
        this.loggedAt = loggedAt;
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.status = Status.ACTIVE;
        
        // Calculate calories burned if activity has calories per minute
        if (activity.getCaloriesPerMinute() != null) {
            this.caloriesBurned = activity.getCaloriesPerMinute()
                    .multiply(BigDecimal.valueOf(durationMinutes));
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Long getUserId() { return user != null ? user.getId() : null; }
    
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }
    
    public Long getActivityId() { return activity != null ? activity.getId() : null; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public BigDecimal getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(BigDecimal caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
