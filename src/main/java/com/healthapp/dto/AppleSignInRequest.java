package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request DTO for Sign in with Apple")
public class AppleSignInRequest {

    @NotBlank(message = "Apple identity token is required")
    @Schema(description = "Apple identity token (JWT) from the client", example = "eyJraWQiOiJlWGF1Q...")
    private String idToken;

    @Pattern(regexp = "ios|android|web", message = "Platform must be 'ios', 'android', or 'web'")
    @Schema(description = "Platform from which the request is made", example = "ios", allowableValues = {"ios", "android", "web"})
    private String platform;

    @Schema(description = "User email (only provided on first login by Apple)")
    private String email;

    @Schema(description = "User first name (only provided on first login by Apple)")
    private String firstName;

    @Schema(description = "User last name (only provided on first login by Apple)")
    private String lastName;

    public AppleSignInRequest() {}

    public AppleSignInRequest(String idToken, String platform, String email, String firstName, String lastName) {
        this.idToken = idToken;
        this.platform = platform;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
