package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Apple Health ingest batch (MVP: steps only, schema v1)")
public class AppleHealthIngestRequest {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    @NotNull
    @JsonProperty("clientIngestSchemaVersion")
    @Schema(description = "Client ingest schema version", example = "1")
    private Integer clientIngestSchemaVersion;

    @NotBlank
    @JsonProperty("anchorTimeZone")
    @Schema(description = "IANA time zone used to derive local calendar day from sample end", example = "America/Los_Angeles")
    private String anchorTimeZone;

    @NotEmpty
    @Valid
    private List<AppleHealthIngestSampleRequest> samples;

    public Integer getClientIngestSchemaVersion() {
        return clientIngestSchemaVersion;
    }

    public void setClientIngestSchemaVersion(Integer clientIngestSchemaVersion) {
        this.clientIngestSchemaVersion = clientIngestSchemaVersion;
    }

    public String getAnchorTimeZone() {
        return anchorTimeZone;
    }

    public void setAnchorTimeZone(String anchorTimeZone) {
        this.anchorTimeZone = anchorTimeZone;
    }

    public List<AppleHealthIngestSampleRequest> getSamples() {
        return samples;
    }

    public void setSamples(List<AppleHealthIngestSampleRequest> samples) {
        this.samples = samples;
    }
}
