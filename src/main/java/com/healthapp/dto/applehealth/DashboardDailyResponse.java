package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Single-day dashboard payload (steps and sleep)")
public class DashboardDailyResponse {

    @JsonProperty("localDate")
    private String localDate;

    @JsonProperty("timeZone")
    private String timeZone;

    @JsonProperty("schemaVersion")
    private int schemaVersion = 2;

    @JsonProperty("generatedAt")
    private OffsetDateTime generatedAt;

    private DashboardDailyStepsSection steps = new DashboardDailyStepsSection();

    private DashboardDailySleepSection sleep = new DashboardDailySleepSection();

    public String getLocalDate() {
        return localDate;
    }

    public void setLocalDate(String localDate) {
        this.localDate = localDate;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public DashboardDailyStepsSection getSteps() {
        return steps;
    }

    public void setSteps(DashboardDailyStepsSection steps) {
        this.steps = steps;
    }

    public DashboardDailySleepSection getSleep() {
        return sleep;
    }

    public void setSleep(DashboardDailySleepSection sleep) {
        this.sleep = sleep;
    }
}
