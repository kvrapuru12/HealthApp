package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request DTO for Google Sign-In")
public class GoogleSignInRequest {
    
    @NotBlank(message = "Google ID token is required")
    @Schema(description = "Google ID token from the client", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2NzM1MjA0MzUiLCJ0eXAiOiJKV1QifQ...")
    private String idToken;
    
    @NotBlank(message = "Platform is required")
    @Pattern(regexp = "ios|android", message = "Platform must be 'ios' or 'android'")
    @Schema(description = "Platform from which the request is made", example = "ios", allowableValues = {"ios", "android"}, required = true)
    private String platform;
    
    public GoogleSignInRequest() {}
    
    public GoogleSignInRequest(String idToken, String platform) {
        this.idToken = idToken;
        this.platform = platform;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
}

