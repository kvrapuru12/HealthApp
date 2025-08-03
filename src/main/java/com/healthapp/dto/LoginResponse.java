package com.healthapp.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String message;
    
    // Default constructor
    public LoginResponse() {}
    
    // Constructor
    public LoginResponse(String token, Long userId, String username, String role, String message) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.message = message;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
} 