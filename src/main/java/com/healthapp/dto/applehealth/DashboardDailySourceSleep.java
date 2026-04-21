package com.healthapp.dto.applehealth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Sleep hours contribution from one source for one calendar day")
public class DashboardDailySourceSleep {

    @JsonProperty("source")
    private String source;

    @JsonProperty("hours")
    private BigDecimal hours;

    public DashboardDailySourceSleep() {
    }

    public DashboardDailySourceSleep(String source, BigDecimal hours) {
        this.source = source;
        this.hours = hours;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
}
