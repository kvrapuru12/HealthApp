package com.healthapp.dto;

import com.healthapp.entity.User;
import java.time.LocalDate;

public class UserCreateRequest {
    
    private String firstName;
    
    private String lastName;
    
    private String phoneNumber;
    
    private String email;
    
    private String username;
    
    private String password;
    
    private LocalDate dob;
    
    private User.Gender gender;
    
    private User.ActivityLevel activityLevel;
    
    private Integer dailyCalorieIntakeTarget;
    
    private Integer dailyCalorieBurnTarget;
    
    private Double weight;
    
    private Double height;
    
    private User.UserRole role;
    
    // Default constructor
    public UserCreateRequest() {}
    
    // Constructor with required fields
    public UserCreateRequest(String firstName, String email, String username, String password, LocalDate dob, 
                           User.Gender gender, User.ActivityLevel activityLevel, User.UserRole role) {
        this.firstName = firstName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.dob = dob;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.role = role;
    }
    
    // Method to convert DTO to Entity
    public User toEntity() {
        User user = new User();
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setPhoneNumber(this.phoneNumber);
        user.setEmail(this.email);
        user.setUsername(this.username);
        user.setPassword(this.password);
        user.setDob(this.dob);
        user.setGender(this.gender);
        user.setActivityLevel(this.activityLevel);
        user.setDailyCalorieIntakeTarget(this.dailyCalorieIntakeTarget);
        user.setDailyCalorieBurnTarget(this.dailyCalorieBurnTarget);
        user.setWeight(this.weight);
        user.setHeight(this.height);
        user.setRole(this.role);
        return user;
    }
    
    // Getters and Setters
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
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public LocalDate getDob() {
        return dob;
    }
    
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
    
    public User.Gender getGender() {
        return gender;
    }
    
    public void setGender(User.Gender gender) {
        this.gender = gender;
    }
    
    public User.ActivityLevel getActivityLevel() {
        return activityLevel;
    }
    
    public void setActivityLevel(User.ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }
    
    public Integer getDailyCalorieIntakeTarget() {
        return dailyCalorieIntakeTarget;
    }
    
    public void setDailyCalorieIntakeTarget(Integer dailyCalorieIntakeTarget) {
        this.dailyCalorieIntakeTarget = dailyCalorieIntakeTarget;
    }
    
    public Integer getDailyCalorieBurnTarget() {
        return dailyCalorieBurnTarget;
    }
    
    public void setDailyCalorieBurnTarget(Integer dailyCalorieBurnTarget) {
        this.dailyCalorieBurnTarget = dailyCalorieBurnTarget;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public Double getHeight() {
        return height;
    }
    
    public void setHeight(Double height) {
        this.height = height;
    }
    
    public User.UserRole getRole() {
        return role;
    }
    
    public void setRole(User.UserRole role) {
        this.role = role;
    }
} 