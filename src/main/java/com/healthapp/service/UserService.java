package com.healthapp.service;

import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional
    public User createUser(User user) {
        logger.info("=== STARTING USER CREATION PROCESS ===");
        logger.info("Creating user with username: {}", user.getUsername());
        
        try {
            logger.info("Step 1: Checking if username exists...");
            // Check if username or email already exists
            if (userRepository.existsByUsername(user.getUsername())) {
                logger.warn("Username already exists: {}", user.getUsername());
                throw new RuntimeException("Username already exists");
            }
            
            logger.info("Step 2: Checking if email exists...");
            if (userRepository.existsByEmail(user.getEmail())) {
                logger.warn("Email already exists: {}", user.getEmail());
                throw new RuntimeException("Email already exists");
            }
            
            logger.info("Step 3: Encoding password...");
            // Encode password before saving
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            logger.info("Step 4: Attempting to save user to database...");
            logger.info("User object before save: username={}, email={}, firstName={}", 
                       user.getUsername(), user.getEmail(), user.getFirstName());
            
            User savedUser = userRepository.save(user);
            
            logger.info("Step 5: User saved successfully!");
            logger.info("User created successfully with ID: {}", savedUser.getId());
            logger.info("=== USER CREATION COMPLETED SUCCESSFULLY ===");
            
            return savedUser;
        } catch (Exception e) {
            logger.error("=== USER CREATION FAILED ===");
            logger.error("Error type: {}", e.getClass().getSimpleName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Full stack trace:", e);
            
            // Check if it's a database connection issue
            if (e.getMessage() != null && e.getMessage().contains("JDBC")) {
                logger.error("This appears to be a JDBC/database connection issue");
            }
            
            throw e;
        }
    }
    
    @Transactional
    public User updateUser(Long id, User userDetails) {
        logger.info("Updating user with ID: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(userDetails.getFirstName());
                    user.setLastName(userDetails.getLastName());
                    user.setEmail(userDetails.getEmail());
                    user.setPhoneNumber(userDetails.getPhoneNumber());
                    user.setDob(userDetails.getDob());
                    user.setGender(userDetails.getGender());
                    user.setHeight(userDetails.getHeight());
                    user.setWeight(userDetails.getWeight());
                    user.setActivityLevel(userDetails.getActivityLevel());
                    user.setDailyCalorieIntakeTarget(userDetails.getDailyCalorieIntakeTarget());
                    
                    // Only update password if provided
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }
                    
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 