package com.healthapp.dto;

import com.healthapp.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserResponse {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String username;
    private LocalDate dob;
    private User.Gender gender;
    private User.ActivityLevel activityLevel;
    private Integer dailyCalorieIntakeTarget;
    private Integer dailyCalorieBurnTarget;
    private Double weight;
    private Double height; // Stored in centimeters
    private HeightInput heightCm;
    private HeightInput heightFeet;
    private User.UserRole role;
    private User.AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Target fields
    private Double targetFat;
    private Double targetProtein;
    private Double targetCarbs;
    private Double targetSleepHours;
    private Double targetWaterLitres;
    private Integer targetSteps;
    private Double targetWeight;
    private LocalDate lastPeriodDate;
    
    // Default constructor
    public UserResponse() {}
    
    // Constructor from User entity
    public UserResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.dob = user.getDob();
        this.gender = user.getGender();
        this.activityLevel = user.getActivityLevel();
        this.dailyCalorieIntakeTarget = user.getDailyCalorieIntakeTarget();
        this.dailyCalorieBurnTarget = user.getDailyCalorieBurnTarget();
        this.weight = user.getWeight();
        this.height = user.getHeight();
        // Convert to both formats for response
        if (user.getHeight() != null) {
            this.heightCm = HeightInput.fromCentimeters(user.getHeight(), HeightInput.HeightUnit.CM);
            this.heightFeet = HeightInput.fromCentimeters(user.getHeight(), HeightInput.HeightUnit.FEET);
        }
        this.role = user.getRole();
        this.accountStatus = user.getAccountStatus();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.targetFat = user.getTargetFat();
        this.targetProtein = user.getTargetProtein();
        this.targetCarbs = user.getTargetCarbs();
        this.targetSleepHours = user.getTargetSleepHours();
        this.targetWaterLitres = user.getTargetWaterLitres();
        this.targetSteps = user.getTargetSteps();
        this.targetWeight = user.getTargetWeight();
        this.lastPeriodDate = user.getLastPeriodDate();
    }
    
    // Static factory method
    public static UserResponse fromUser(User user) {
        return new UserResponse(user);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public HeightInput getHeightCm() {
        return heightCm;
    }
    
    public void setHeightCm(HeightInput heightCm) {
        this.heightCm = heightCm;
    }
    
    public HeightInput getHeightFeet() {
        return heightFeet;
    }
    
    public void setHeightFeet(HeightInput heightFeet) {
        this.heightFeet = heightFeet;
    }
    
    public User.UserRole getRole() {
        return role;
    }
    
    public void setRole(User.UserRole role) {
        this.role = role;
    }
    
    public User.AccountStatus getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(User.AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Target fields getters and setters
    public Double getTargetFat() {
        return targetFat;
    }
    
    public void setTargetFat(Double targetFat) {
        this.targetFat = targetFat;
    }
    
    public Double getTargetProtein() {
        return targetProtein;
    }
    
    public void setTargetProtein(Double targetProtein) {
        this.targetProtein = targetProtein;
    }
    
    public Double getTargetCarbs() {
        return targetCarbs;
    }
    
    public void setTargetCarbs(Double targetCarbs) {
        this.targetCarbs = targetCarbs;
    }
    
    public Double getTargetSleepHours() {
        return targetSleepHours;
    }
    
    public void setTargetSleepHours(Double targetSleepHours) {
        this.targetSleepHours = targetSleepHours;
    }
    
    public Double getTargetWaterLitres() {
        return targetWaterLitres;
    }
    
    public void setTargetWaterLitres(Double targetWaterLitres) {
        this.targetWaterLitres = targetWaterLitres;
    }
    
    public Integer getTargetSteps() {
        return targetSteps;
    }
    
    public void setTargetSteps(Integer targetSteps) {
        this.targetSteps = targetSteps;
    }
    
    public Double getTargetWeight() {
        return targetWeight;
    }
    
    public void setTargetWeight(Double targetWeight) {
        this.targetWeight = targetWeight;
    }
    
    public LocalDate getLastPeriodDate() {
        return lastPeriodDate;
    }
    
    public void setLastPeriodDate(LocalDate lastPeriodDate) {
        this.lastPeriodDate = lastPeriodDate;
    }
} 