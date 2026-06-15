package com.healthapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.food")
public class AiFoodProperties {

    private boolean showConfidence = true;

    public boolean isShowConfidence() {
        return showConfidence;
    }

    public void setShowConfidence(boolean showConfidence) {
        this.showConfidence = showConfidence;
    }
}
