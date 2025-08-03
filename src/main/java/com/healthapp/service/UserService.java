package com.healthapp.service;

import com.healthapp.dto.UserPatchRequest;
import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<User> getAllUsersPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(User.AccountStatus status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findByAccountStatus(status, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(User.UserRole role, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findByRole(role, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatusAndRole(User.AccountStatus status, User.UserRole role, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findByAccountStatusAndRole(status, role, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<User> searchUsersByName(String searchTerm, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            searchTerm, searchTerm, pageable);
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
        logger.info("Attempting to delete user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Attempted to delete non-existent user with ID: {}", id);
                    return new RuntimeException("User not found");
                });
        
        // Check if user is already deleted
        if (user.getAccountStatus() == User.AccountStatus.DELETED) {
            logger.warn("Attempted to delete already deleted user with ID: {}", id);
            throw new RuntimeException("User is already deleted");
        }
        
        // Check if user has active data that should prevent deletion
        if (hasActiveData(user)) {
            logger.warn("Attempted to delete user with active data, ID: {}", id);
            throw new RuntimeException("Cannot delete user with active data. Please deactivate the account instead.");
        }
        
        // Soft delete - mark as deleted instead of hard delete
        user.setAccountStatus(User.AccountStatus.DELETED);
        userRepository.save(user);
        
        logger.info("User successfully soft deleted with ID: {}", id);
    }
    
    @Transactional(readOnly = true)
    private boolean hasActiveData(User user) {
        // Check if user has any active food entries, activity entries, etc.
        // This is a placeholder - implement based on your business logic
        // For now, we'll allow deletion but log it
        logger.info("Checking active data for user ID: {}", user.getId());
        return false; // Placeholder - implement actual logic
    }
    
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public User patchUser(Long id, UserPatchRequest patchRequest) {
        logger.info("Patching user with ID: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    // Apply patch changes
                    patchRequest.applyToUser(user);
                    
                    // Encode password if it was updated
                    if (patchRequest.getPassword() != null) {
                        user.setPassword(passwordEncoder.encode(patchRequest.getPassword()));
                    }
                    
                    logger.info("User patched successfully for ID: {}", id);
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
} 