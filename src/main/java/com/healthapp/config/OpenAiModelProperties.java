package com.healthapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiModelProperties {

    private final Api api = new Api();
    private int timeout = 60;
    private Model model = new Model();

    public Api getApi() {
        return api;
    }

    public String getApiKey() {
        return api.getKey();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public boolean hasValidApiKey() {
        String key = api.getKey();
        return key != null && !key.isBlank() && !"your-openai-api-key-here".equals(key.trim());
    }

    public String getVoiceFoodModel() {
        Voice voice = model.getVoice();
        return voice.getFoodDefault() != null ? voice.getFoodDefault() : "gpt-4.1-mini";
    }

    public String getVoiceActivityModel() {
        Voice voice = model.getVoice();
        return voice.getActivity() != null ? voice.getActivity() : "gpt-4.1-mini";
    }

    public String getVoiceCycleModel() {
        Voice voice = model.getVoice();
        return voice.getCycle() != null ? voice.getCycle() : "gpt-4.1-mini";
    }

    public String getCycleSyncModel() {
        return model.getCycleSync() != null ? model.getCycleSync() : "gpt-4o-mini";
    }

    public String getFoodSimpleModel() {
        FoodRouting routing = model.getVoice().getFoodRouting();
        return routing.getSimple() != null ? routing.getSimple() : getVoiceFoodModel();
    }

    public String getFoodComplexModel() {
        FoodRouting routing = model.getVoice().getFoodRouting();
        return routing.getComplex() != null ? routing.getComplex() : "gpt-4.1";
    }

    public boolean isComplexRoutingEnabled() {
        return model.getVoice().getFoodRouting().isEnabled();
    }

    public int getVoiceMaxRetries() {
        return model.getVoice().getMaxRetries();
    }

    public double getVoiceTemperature() {
        return model.getVoice().getTemperature();
    }

    public static class Model {
        private Voice voice = new Voice();
        private String cycleSync = "gpt-4o-mini";

        public Voice getVoice() {
            return voice;
        }

        public void setVoice(Voice voice) {
            this.voice = voice;
        }

        public String getCycleSync() {
            return cycleSync;
        }

        public void setCycleSync(String cycleSync) {
            this.cycleSync = cycleSync;
        }
    }

    public static class Voice {
        private String defaultModel = "gpt-4.1-mini";
        private String foodDefault = "gpt-4.1-mini";
        private String activity = "gpt-4.1-mini";
        private String cycle = "gpt-4.1-mini";
        private int maxRetries = 3;
        private double temperature = 0.1;
        private FoodRouting foodRouting = new FoodRouting();

        public String getDefault() {
            return defaultModel;
        }

        public void setDefault(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public String getFoodDefault() {
            return foodDefault;
        }

        public void setFoodDefault(String foodDefault) {
            this.foodDefault = foodDefault;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }

        public String getCycle() {
            return cycle;
        }

        public void setCycle(String cycle) {
            this.cycle = cycle;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public FoodRouting getFoodRouting() {
            return foodRouting;
        }

        public void setFoodRouting(FoodRouting foodRouting) {
            this.foodRouting = foodRouting;
        }
    }

    public static class FoodRouting {
        private String simple = "gpt-4.1-mini";
        private String complex = "gpt-4.1";
        private boolean enabled = false;

        public String getSimple() {
            return simple;
        }

        public void setSimple(String simple) {
            this.simple = simple;
        }

        public String getComplex() {
            return complex;
        }

        public void setComplex(String complex) {
            this.complex = complex;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Api {
        private String key = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
