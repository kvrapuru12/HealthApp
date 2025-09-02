package com.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VoiceActivityLogRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Voice text is required")
    @Size(min = 5, max = 1000, message = "Voice text must be between 5 and 1000 characters")
    private String voiceText;

    public VoiceActivityLogRequest() {}

    public VoiceActivityLogRequest(Long userId, String voiceText) {
        this.userId = userId;
        this.voiceText = voiceText;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getVoiceText() {
        return voiceText;
    }

    public void setVoiceText(String voiceText) {
        this.voiceText = voiceText;
    }
}
