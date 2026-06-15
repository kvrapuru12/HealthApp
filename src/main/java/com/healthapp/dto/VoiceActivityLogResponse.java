package com.healthapp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoiceActivityLogResponse {

    /**
     * Voice activity logging may create multiple activity logs from one utterance
     * (e.g. "cycled 40 minutes then weights 25 minutes"). {@link #activityLog} always
     * mirrors the first entry; clients should prefer {@link #activityLogs} when present.
     */
    private String message;
    /** First activity for backward compatibility. */
    private ActivityLogSummary activityLog;
    private List<ActivityLogSummary> activityLogs = new ArrayList<>();

    public VoiceActivityLogResponse() {}

    public VoiceActivityLogResponse(String message, ActivityLogSummary activityLog) {
        this.message = message;
        this.activityLog = activityLog;
        if (activityLog != null) {
            this.activityLogs = new ArrayList<>(List.of(activityLog));
        }
    }

    public VoiceActivityLogResponse(String message, List<ActivityLogSummary> activityLogs) {
        this.message = message;
        this.activityLogs = activityLogs != null ? activityLogs : new ArrayList<>();
        this.activityLog = this.activityLogs.isEmpty() ? null : this.activityLogs.get(0);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ActivityLogSummary getActivityLog() {
        return activityLog;
    }

    public void setActivityLog(ActivityLogSummary activityLog) {
        this.activityLog = activityLog;
    }

    public List<ActivityLogSummary> getActivityLogs() {
        return activityLogs;
    }

    public void setActivityLogs(List<ActivityLogSummary> activityLogs) {
        this.activityLogs = activityLogs != null ? activityLogs : new ArrayList<>();
        this.activityLog = this.activityLogs.isEmpty() ? null : this.activityLogs.get(0);
    }

    public static class ActivityLogSummary {
        private Long id;
        private String activity;
        private Integer durationMinutes;
        private Double caloriesBurned;
        private LocalDateTime loggedAt;
        private String note;

        public ActivityLogSummary() {}

        public ActivityLogSummary(Long id, String activity, Integer durationMinutes, 
                                Double caloriesBurned, LocalDateTime loggedAt, String note) {
            this.id = id;
            this.activity = activity;
            this.durationMinutes = durationMinutes;
            this.caloriesBurned = caloriesBurned;
            this.loggedAt = loggedAt;
            this.note = note;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Double getCaloriesBurned() {
            return caloriesBurned;
        }

        public void setCaloriesBurned(Double caloriesBurned) {
            this.caloriesBurned = caloriesBurned;
        }

        public LocalDateTime getLoggedAt() {
            return loggedAt;
        }

        public void setLoggedAt(LocalDateTime loggedAt) {
            this.loggedAt = loggedAt;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
