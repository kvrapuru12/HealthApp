package com.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VoiceFoodLogRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Voice text is required")
    @Size(min = 3, max = 1000, message = "Voice text must be between 3 and 1000 characters")
    private String voiceText;
    
    // Constructors
    public VoiceFoodLogRequest() {}
    
    public VoiceFoodLogRequest(Long userId, String voiceText) {
        this.userId = userId;
        this.voiceText = voiceText;
    }
    
    // Getters and Setters
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
