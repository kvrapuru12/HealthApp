package com.healthapp.dto.applehealth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Step contribution from one source")
public class DashboardDailySourceSteps {

    private String source;
    private int steps;

    public DashboardDailySourceSteps() {
    }

    public DashboardDailySourceSteps(String source, int steps) {
        this.source = source;
        this.steps = steps;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
