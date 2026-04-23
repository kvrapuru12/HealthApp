package com.healthapp.dto;

public class NotificationPreferencesResponse {

    private Boolean foodReminderEnabled;
    private Boolean activityReminderEnabled;
    private Boolean cyclePhaseReminderEnabled;
    private String reminderTime;
    private String quietHoursStart;
    private String quietHoursEnd;
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
