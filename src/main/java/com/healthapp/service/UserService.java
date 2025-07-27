package com.healthapp.service;

import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User createUser(User user) {
        logger.info("Creating user with username: {}", user.getUsername());
        
        try {
            // Check if username or email already exists
            if (userRepository.existsByUsername(user.getUsername())) {
                logger.warn("Username already exists: {}", user.getUsername());
                throw new RuntimeException("Username already exists");
            }
            if (userRepository.existsByEmail(user.getEmail())) {
                logger.warn("Email already exists: {}", user.getEmail());
                throw new RuntimeException("Email already exists");
            }
            
            logger.info("Encoding password for user: {}", user.getUsername());
            // Encode password before saving
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            logger.info("Saving user to database: {}", user.getUsername());
            User savedUser = userRepository.save(user);
            logger.info("User created successfully with ID: {}", savedUser.getId());
            
            return savedUser;
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public User updateUser(Long id, User userDetails) {
        logger.info("Updating user with ID: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(userDetails.getFirstName());
                    user.setLastName(userDetails.getLastName());
                    user.setEmail(userDetails.getEmail());
                    user.setPhoneNumber(userDetails.getPhoneNumber());
                    user.setDateOfBirth(userDetails.getDateOfBirth());
                    user.setGender(userDetails.getGender());
                    user.setHeightCm(userDetails.getHeightCm());
                    user.setWeightKg(userDetails.getWeightKg());
                    user.setAge(userDetails.getAge());
                    user.setActivityLevel(userDetails.getActivityLevel());
                    user.setDailyCalorieGoal(userDetails.getDailyCalorieGoal());
                    
                    // Only update password if provided
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }
                    
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 