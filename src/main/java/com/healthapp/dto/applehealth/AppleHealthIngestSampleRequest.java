package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "One Apple Health quantity sample (MVP: STEPS only)")
public class AppleHealthIngestSampleRequest {

    @NotBlank
    @Schema(description = "Metric type", example = "STEPS", allowableValues = {"STEPS"})
    private String metric;

    @NotBlank
    @JsonProperty("externalSampleId")
    @Schema(description = "Stable HealthKit / sync identifier", example = "HKQuantityTypeIdentifierStepCount:ABC-UUID")
    private String externalSampleId;

    @NotNull
    @Schema(description = "Interval start (ISO-8601 with offset)", example = "2026-04-15T07:00:00Z")
    private OffsetDateTime start;

    @NotNull
    @Schema(description = "Interval end (ISO-8601 with offset)", example = "2026-04-16T07:00:00Z")
    private OffsetDateTime end;

    @NotNull
    @Min(0)
    @Schema(description = "Step count for this sample", example = "8432")
    private Integer value;

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

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
