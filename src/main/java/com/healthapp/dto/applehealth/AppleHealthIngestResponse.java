package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Result of Apple Health ingest")
public class AppleHealthIngestResponse {

    private int accepted;
    private int unchanged;
    private int rejected;

    @JsonProperty("affectedLocalDates")
    private List<String> affectedLocalDates = new ArrayList<>();

    private List<AppleHealthIngestSampleResult> results = new ArrayList<>();

    @JsonProperty("serverIngestSchemaVersion")
    private int serverIngestSchemaVersion = AppleHealthIngestRequest.SUPPORTED_SCHEMA_VERSION;

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public void setUnchanged(int unchanged) {
        this.unchanged = unchanged;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public List<String> getAffectedLocalDates() {
        return affectedLocalDates;
    }

    public void setAffectedLocalDates(List<String> affectedLocalDates) {
        this.affectedLocalDates = affectedLocalDates;
    }

    public List<AppleHealthIngestSampleResult> getResults() {
        return results;
    }

    public void setResults(List<AppleHealthIngestSampleResult> results) {
        this.results = results;
    }

    public int getServerIngestSchemaVersion() {
        return serverIngestSchemaVersion;
    }

    public void setServerIngestSchemaVersion(int serverIngestSchemaVersion) {
        this.serverIngestSchemaVersion = serverIngestSchemaVersion;
    }
}
