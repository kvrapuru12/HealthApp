package com.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NotificationDeviceRegisterRequest {

    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID cannot exceed 255 characters")
    private String deviceId;

    @NotBlank(message = "Expo push token is required")
    @Size(max = 255, message = "Expo push token cannot exceed 255 characters")
    private String expoPushToken;

    @NotBlank(message = "Platform is required")
    @Size(max = 30, message = "Platform cannot exceed 30 characters")
    private String platform;

    @Size(max = 100, message = "App version cannot exceed 100 characters")
    private String appVersion;

    @Size(max = 100, message = "Build number cannot exceed 100 characters")
    private String buildNumber;

    @NotBlank(message = "Timezone is required")
    @Size(max = 100, message = "Timezone cannot exceed 100 characters")
    private String timezone;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getExpoPushToken() {
        return expoPushToken;
    }

    public void setExpoPushToken(String expoPushToken) {
        this.expoPushToken = expoPushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
