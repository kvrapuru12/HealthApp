package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Flags when manual and Apple Health steps disagree")
public class DashboardDailyConflictFlags {

    @JsonProperty("manualVsAppleMismatch")
    private boolean manualVsAppleMismatch;

    @JsonProperty("manualIgnoredForDisplay")
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
