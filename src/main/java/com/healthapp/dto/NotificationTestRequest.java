package com.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NotificationTestRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title cannot exceed 120 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(max = 300, message = "Body cannot exceed 300 characters")
    private String body;

    @Size(max = 50, message = "Type cannot exceed 50 characters")
    private String type;

    @Size(max = 100, message = "Target screen cannot exceed 100 characters")
    private String targetScreen;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetScreen() {
        return targetScreen;
    }

    public void setTargetScreen(String targetScreen) {
        this.targetScreen = targetScreen;
    }
}
