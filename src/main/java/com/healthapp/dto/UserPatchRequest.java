package com.healthapp.dto;

import com.healthapp.entity.User;
import java.time.LocalDate;

public class UserPatchRequest {
    
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String password;
    private LocalDate dob;
    private User.Gender gender;
    private User.ActivityLevel activityLevel;
    private Integer dailyCalorieIntakeTarget;
    private Integer dailyCalorieBurnTarget;
    private Double weight;
    private HeightInput height;
    private User.AccountStatus accountStatus;
    
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
    public UserPatchRequest() {}
    
    // Method to apply patch to existing user entity
    public void applyToUser(User user) {
        if (this.firstName != null) {
            user.setFirstName(this.firstName);
        }
        if (this.lastName != null) {
            user.setLastName(this.lastName);
        }
        if (this.phoneNumber != null) {
            user.setPhoneNumber(this.phoneNumber);
        }
        if (this.email != null) {
            user.setEmail(this.email);
        }
        if (this.password != null) {
            user.setPassword(this.password);
        }
        if (this.dob != null) {
            user.setDob(this.dob);
        }
        if (this.gender != null) {
            user.setGender(this.gender);
        }
        if (this.activityLevel != null) {
            user.setActivityLevel(this.activityLevel);
        }
        if (this.dailyCalorieIntakeTarget != null) {
            user.setDailyCalorieIntakeTarget(this.dailyCalorieIntakeTarget);
        }
        if (this.dailyCalorieBurnTarget != null) {
            user.setDailyCalorieBurnTarget(this.dailyCalorieBurnTarget);
        }
        if (this.weight != null) {
            user.setWeight(this.weight);
        }
        if (this.height != null) {
            user.setHeight(this.height.toCentimeters());
        }
        if (this.accountStatus != null) {
            user.setAccountStatus(this.accountStatus);
        }
        if (this.targetFat != null) {
            user.setTargetFat(this.targetFat);
        }
        if (this.targetProtein != null) {
            user.setTargetProtein(this.targetProtein);
        }
        if (this.targetCarbs != null) {
            user.setTargetCarbs(this.targetCarbs);
        }
        if (this.targetSleepHours != null) {
            user.setTargetSleepHours(this.targetSleepHours);
        }
        if (this.targetWaterLitres != null) {
            user.setTargetWaterLitres(this.targetWaterLitres);
        }
        if (this.targetSteps != null) {
            user.setTargetSteps(this.targetSteps);
        }
        if (this.targetWeight != null) {
            user.setTargetWeight(this.targetWeight);
        }
        if (this.lastPeriodDate != null) {
            user.setLastPeriodDate(this.lastPeriodDate);
        }
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
    
    public HeightInput getHeight() {
        return height;
    }
    
    public void setHeight(HeightInput height) {
        this.height = height;
    }
    
    public User.AccountStatus getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(User.AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
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