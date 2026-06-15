package com.healthapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nutrition.lookup")
public class NutritionLookupProperties {

    private boolean enabled = true;
    private String provider = "usda";
    private double confidenceThreshold = 0.75;
    private int cacheTtlDays = 90;
    private Usda usda = new Usda();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getCacheTtlDays() {
        return cacheTtlDays;
    }

    public void setCacheTtlDays(int cacheTtlDays) {
        this.cacheTtlDays = cacheTtlDays;
    }

    public Usda getUsda() {
        return usda;
    }

    public void setUsda(Usda usda) {
        this.usda = usda;
    }

    public static class Usda {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
