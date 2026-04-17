package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppleHealthIngestSampleResult {

    public enum Status {
        UPSERTED,
        UNCHANGED,
        REJECTED
    }

    @JsonProperty("externalSampleId")
    private String externalSampleId;

    private Status status;

    private String message;

    public AppleHealthIngestSampleResult() {
    }

    public AppleHealthIngestSampleResult(String externalSampleId, Status status, String message) {
        this.externalSampleId = externalSampleId;
        this.status = status;
        this.message = message;
    }

    public String getExternalSampleId() {
        return externalSampleId;
    }

    public void setExternalSampleId(String externalSampleId) {
        this.externalSampleId = externalSampleId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
