package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Flags when manual app totals and Apple Health totals disagree (steps or sleep section)")
public class DashboardDailyConflictFlags {

    @JsonProperty("manualVsAppleMismatch")
    @Schema(description = "True when both manual and Apple Health have data for the day but the values differ")
    private boolean manualVsAppleMismatch;

    @JsonProperty("manualIgnoredForDisplay")
    @Schema(description = "True when the merged display value follows Apple Health and ignores manual for that metric")
    private boolean manualIgnoredForDisplay;

    public DashboardDailyConflictFlags() {
    }

    public DashboardDailyConflictFlags(boolean manualVsAppleMismatch, boolean manualIgnoredForDisplay) {
        this.manualVsAppleMismatch = manualVsAppleMismatch;
        this.manualIgnoredForDisplay = manualIgnoredForDisplay;
    }

    public boolean isManualVsAppleMismatch() {
        return manualVsAppleMismatch;
    }

    public void setManualVsAppleMismatch(boolean manualVsAppleMismatch) {
        this.manualVsAppleMismatch = manualVsAppleMismatch;
    }

    public boolean isManualIgnoredForDisplay() {
        return manualIgnoredForDisplay;
    }

    public void setManualIgnoredForDisplay(boolean manualIgnoredForDisplay) {
        this.manualIgnoredForDisplay = manualIgnoredForDisplay;
    }
}
