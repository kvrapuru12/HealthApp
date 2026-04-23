package com.healthapp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NotificationPreferencesPatchRequest {

    private Boolean foodReminderEnabled;
    private Boolean activityReminderEnabled;
    private Boolean cyclePhaseReminderEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "reminderTime must be HH:mm or HH:mm:ss")
    private String reminderTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "quietHoursStart must be HH:mm or HH:mm:ss")
    private String quietHoursStart;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "quietHoursEnd must be HH:mm or HH:mm:ss")
    private String quietHoursEnd;

    @Size(max = 100, message = "Timezone cannot exceed 100 characters")
    private String timezone;

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

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
