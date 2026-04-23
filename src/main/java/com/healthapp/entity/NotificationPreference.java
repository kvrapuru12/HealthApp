package com.healthapp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "food_reminder_enabled", nullable = false)
    private Boolean foodReminderEnabled = true;

    @Column(name = "activity_reminder_enabled", nullable = false)
    private Boolean activityReminderEnabled = true;

    @Column(name = "cycle_phase_reminder_enabled", nullable = false)
    private Boolean cyclePhaseReminderEnabled = true;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(20, 0);

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public Boolean getFoodReminderEnabled() {
        return foodReminderEnabled;
    }

    public void setFoodReminderEnabled(Boolean foodReminderEnabled) {
        this.foodReminderEnabled = foodReminderEnabled;
    }

    public Boolean getActivityReminderEnabled() {
        return activityReminderEnabled;
    }

    public void setActivityReminderEnabled(Boolean activityReminderEnabled) {
        this.activityReminderEnabled = activityReminderEnabled;
    }

    public Boolean getCyclePhaseReminderEnabled() {
        return cyclePhaseReminderEnabled;
    }

    public void setCyclePhaseReminderEnabled(Boolean cyclePhaseReminderEnabled) {
        this.cyclePhaseReminderEnabled = cyclePhaseReminderEnabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
