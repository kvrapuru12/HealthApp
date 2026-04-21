package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "One Apple Health sample (STEPS quantity or SLEEP category segment)")
public class AppleHealthIngestSampleRequest {

    @NotBlank
    @Schema(description = "Metric type", example = "STEPS", allowableValues = {"STEPS", "SLEEP"})
    private String metric;

    @NotBlank
    @JsonProperty("externalSampleId")
    @Schema(description = "Stable HealthKit / sync identifier (e.g. HKQuantityTypeIdentifierStepCount:... or HKCategoryTypeIdentifierSleepAnalysis:...)", example = "HKCategoryTypeIdentifierSleepAnalysis:ABC-UUID")
    private String externalSampleId;

    @NotNull
    @Schema(description = "Interval start (ISO-8601 with offset)", example = "2026-04-15T07:00:00Z")
    private OffsetDateTime start;

    @NotNull
    @Schema(description = "Interval end (ISO-8601 with offset)", example = "2026-04-16T07:00:00Z")
    private OffsetDateTime end;

    @Schema(description = "Client-declared local calendar date in anchorTimeZone (required for schema v2; STEPS: must match start date, SLEEP: must match end / wake date)", example = "2026-04-17")
    private LocalDate localDate;

    @Min(0)
    @Schema(description = "Step count when metric is STEPS; ignored for SLEEP", example = "8432")
    private Integer value;

    @JsonProperty("sleepStage")
    @Schema(description = "Normalized sleep stage when metric is SLEEP (e.g. CORE, DEEP, REM, AWAKE, IN_BED, ASLEEP, ASLEEP_UNSPECIFIED)", example = "CORE")
    private String sleepStage;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getExternalSampleId() {
        return externalSampleId;
    }

    public void setExternalSampleId(String externalSampleId) {
        this.externalSampleId = externalSampleId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getSleepStage() {
        return sleepStage;
    }

    public void setSleepStage(String sleepStage) {
        this.sleepStage = sleepStage;
    }
}
