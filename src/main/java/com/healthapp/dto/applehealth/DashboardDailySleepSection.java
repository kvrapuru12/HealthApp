package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Combined sleep view for one calendar day (Apple asleep sum vs manual hours)")
public class DashboardDailySleepSection {

    @JsonProperty("mergePolicy")
    private String mergePolicy = "PREFER_APPLE_HEALTH_IF_PRESENT";

    @JsonProperty("displayedSleepHours")
    private BigDecimal displayedSleepHours = BigDecimal.ZERO;

    @JsonProperty("bySource")
    private List<DashboardDailySourceSleep> bySource = new ArrayList<>();

    @JsonProperty("conflictFlags")
    private DashboardDailyConflictFlags conflictFlags = new DashboardDailyConflictFlags();

    public String getMergePolicy() {
        return mergePolicy;
    }

    public void setMergePolicy(String mergePolicy) {
        this.mergePolicy = mergePolicy;
    }

    public BigDecimal getDisplayedSleepHours() {
        return displayedSleepHours;
    }

    public void setDisplayedSleepHours(BigDecimal displayedSleepHours) {
        this.displayedSleepHours = displayedSleepHours;
    }

    public List<DashboardDailySourceSleep> getBySource() {
        return bySource;
    }

    public void setBySource(List<DashboardDailySourceSleep> bySource) {
        this.bySource = bySource;
    }

    public DashboardDailyConflictFlags getConflictFlags() {
        return conflictFlags;
    }

    public void setConflictFlags(DashboardDailyConflictFlags conflictFlags) {
        this.conflictFlags = conflictFlags;
    }
}
