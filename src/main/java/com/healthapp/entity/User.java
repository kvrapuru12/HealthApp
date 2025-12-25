package com.healthapp.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(unique = true)
    private String email;
    
    @Column(name = "google_id", unique = true)
    private String googleId;
    
    @Column(unique = true)
    private String username;
    
    private String password;
    
    @Column(name = "date_of_birth")
    private LocalDate dob;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    private ActivityLevel activityLevel;
    
    @Column(name = "daily_calorie_intake_target")
    private Integer dailyCalorieIntakeTarget;
    
    @Column(name = "daily_calorie_burn_target")
    private Integer dailyCalorieBurnTarget;
    
    @Column(name = "weight_kg")
    private Double weight;
    
    @Column(name = "height_cm")
    private Double height;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Target fields
    @Column(name = "target_fat")
    private Double targetFat;
    
    @Column(name = "target_protein")
    private Double targetProtein;
    
    @Column(name = "target_carbs")
    private Double targetCarbs;
    
    @Column(name = "target_sleep_hours")
    private Double targetSleepHours;
    
    @Column(name = "target_water_litres")
    private Double targetWaterLitres;
    
    @Column(name = "target_steps")
    private Integer targetSteps;
    
    @Column(name = "target_weight")
    private Double targetWeight;
    
    @Column(name = "last_period_date")
    private LocalDate lastPeriodDate;
    
    // Constructors
    public User() {}
    
    public User(String firstName, String email, String username, String password, LocalDate dob, Gender gender, ActivityLevel activityLevel, UserRole role) {
        this.firstName = firstName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.dob = dob;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.role = role;
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
    
    public String getGoogleId() {
        return googleId;
    }
    
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
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
    
    public Gender getGender() {
        return gender;
    }
    
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    
    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }
    
    public void setActivityLevel(ActivityLevel activityLevel) {
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
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public AccountStatus getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(AccountStatus accountStatus) {
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
    
    public enum Gender {
        FEMALE, MALE, NON_BINARY, OTHER
    }
    
    public enum ActivityLevel {
        SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
    }
    
    public enum UserRole {
        USER, ADMIN, COACH
    }
    
    public enum AccountStatus {
        ACTIVE, INACTIVE, DELETED
    }
} 