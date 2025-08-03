package com.healthapp.service;

import com.healthapp.dto.UserCreateRequest;
import com.healthapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValidationService {
    
    @Autowired
    private UserRepository userRepository;
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]*$");
    
    public Map<String, String> validateUserCreation(UserCreateRequest userRequest) {
        Map<String, String> errors = new HashMap<>();
        
        // Validate firstName
        if (userRequest.getFirstName() == null || userRequest.getFirstName().trim().isEmpty()) {
            errors.put("firstName", "First name is required");
        } else if (userRequest.getFirstName().length() < 2 || userRequest.getFirstName().length() > 50) {
            errors.put("firstName", "First name must be between 2 and 50 characters");
        } else if (!NAME_PATTERN.matcher(userRequest.getFirstName()).matches()) {
            errors.put("firstName", "First name can only contain letters and spaces");
        }
        
        // Validate lastName (optional)
        if (userRequest.getLastName() != null && !userRequest.getLastName().trim().isEmpty()) {
            if (userRequest.getLastName().length() > 50) {
                errors.put("lastName", "Last name cannot exceed 50 characters");
            } else if (!NAME_PATTERN.matcher(userRequest.getLastName()).matches()) {
                errors.put("lastName", "Last name can only contain letters and spaces");
            }
        }
        
        // Validate email
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            errors.put("email", "Email is required");
        } else if (!EMAIL_PATTERN.matcher(userRequest.getEmail()).matches()) {
            errors.put("email", "Email must be a valid email address");
        } else if (userRepository != null && userRepository.existsByEmail(userRequest.getEmail())) {
            errors.put("email", "Email already exists");
        }
        
        // Validate phoneNumber (optional)
        if (userRequest.getPhoneNumber() != null && !userRequest.getPhoneNumber().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(userRequest.getPhoneNumber()).matches()) {
                errors.put("phoneNumber", "Phone number must be in international format");
            }
        }
        
        // Validate username
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            errors.put("username", "Username is required");
        } else if (userRequest.getUsername().length() < 4 || userRequest.getUsername().length() > 20) {
            errors.put("username", "Username must be between 4 and 20 characters");
        } else if (!USERNAME_PATTERN.matcher(userRequest.getUsername()).matches()) {
            errors.put("username", "Username can only contain alphanumeric characters and underscores");
        } else if (userRepository != null && userRepository.existsByUsername(userRequest.getUsername())) {
            errors.put("username", "Username already exists");
        }
        
        // Validate password
        if (userRequest.getPassword() == null || userRequest.getPassword().trim().isEmpty()) {
            errors.put("password", "Password is required");
        } else if (userRequest.getPassword().length() < 8 || userRequest.getPassword().length() > 64) {
            errors.put("password", "Password must be between 8 and 64 characters");
        } else if (!PASSWORD_PATTERN.matcher(userRequest.getPassword()).matches()) {
            errors.put("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }
        
        // Validate date of birth
        if (userRequest.getDob() == null) {
            errors.put("dob", "Date of birth is required");
        } else {
            LocalDate today = LocalDate.now();
            if (userRequest.getDob().isAfter(today)) {
                errors.put("dob", "Date of birth must be in the past");
            } else {
                Period age = Period.between(userRequest.getDob(), today);
                if (age.getYears() < 13) {
                    errors.put("dob", "User must be at least 13 years old");
                }
            }
        }
        
        // Validate gender
        if (userRequest.getGender() == null) {
            errors.put("gender", "Gender is required");
        }
        
        // Validate activity level
        if (userRequest.getActivityLevel() == null) {
            errors.put("activityLevel", "Activity level is required");
        }
        
        // Validate role
        if (userRequest.getRole() == null) {
            errors.put("role", "Role is required");
        }
        
                       // Validate daily calorie intake target (optional)
               if (userRequest.getDailyCalorieIntakeTarget() != null) {
                   if (userRequest.getDailyCalorieIntakeTarget() < 800 || userRequest.getDailyCalorieIntakeTarget() > 6000) {
                       errors.put("dailyCalorieIntakeTarget", "Daily calorie intake target must be between 800 and 6000 cal");
                   }
               }
        
                       // Validate daily calorie burn target (optional)
               if (userRequest.getDailyCalorieBurnTarget() != null) {
                   if (userRequest.getDailyCalorieBurnTarget() < 100 || userRequest.getDailyCalorieBurnTarget() > 3000) {
                       errors.put("dailyCalorieBurnTarget", "Daily calorie burn target must be between 100 and 3000 cal");
                   }
               }
        
        // Validate weight (optional)
        if (userRequest.getWeight() != null) {
            if (userRequest.getWeight() < 30 || userRequest.getWeight() > 300) {
                errors.put("weight", "Weight must be between 30 and 300 kg");
            }
        }
        
        // Validate height (optional)
        if (userRequest.getHeight() != null) {
            if (userRequest.getHeight() < 100 || userRequest.getHeight() > 250) {
                errors.put("height", "Height must be between 100 and 250 cm");
            }
        }
        
        return errors;
    }
} 