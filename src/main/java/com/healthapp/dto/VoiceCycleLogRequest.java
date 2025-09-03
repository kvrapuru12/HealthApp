package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to log menstrual cycle via voice input")
public class VoiceCycleLogRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "User ID", example = "12")
    private Long userId;
    
    @NotBlank(message = "Voice text is required")
    @Schema(description = "Voice input text", example = "My period started yesterday")
    private String voiceText;
    
    // Constructors
    public VoiceCycleLogRequest() {}
    
    public VoiceCycleLogRequest(Long userId, String voiceText) {
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
