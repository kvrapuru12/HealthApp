package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Combined step view for one calendar day")
public class DashboardDailyStepsSection {

    @JsonProperty("mergePolicy")
    private String mergePolicy = "SUM_WHEN_BOTH_PRESENT";

    @JsonProperty("displayedSteps")
    private int displayedSteps;

    @JsonProperty("resolvedSource")
    private String resolvedSource = "NONE";

    @JsonProperty("bySource")
    private List<DashboardDailySourceSteps> bySource = new ArrayList<>();

    @JsonProperty("conflictFlags")
    private DashboardDailyConflictFlags conflictFlags = new DashboardDailyConflictFlags();

    public String getMergePolicy() {
        return mergePolicy;
    }

    public void setMergePolicy(String mergePolicy) {
        this.mergePolicy = mergePolicy;
    }

    public int getDisplayedSteps() {
        return displayedSteps;
    }

    public void setDisplayedSteps(int displayedSteps) {
        this.displayedSteps = displayedSteps;
    }

    public String getResolvedSource() {
        return resolvedSource;
    }

    public void setResolvedSource(String resolvedSource) {
        this.resolvedSource = resolvedSource;
    }

    public List<DashboardDailySourceSteps> getBySource() {
        return bySource;
    }

    public void setBySource(List<DashboardDailySourceSteps> bySource) {
        this.bySource = bySource;
    }

    public DashboardDailyConflictFlags getConflictFlags() {
        return conflictFlags;
    }

    public void setConflictFlags(DashboardDailyConflictFlags conflictFlags) {
        this.conflictFlags = conflictFlags;
    }
}
