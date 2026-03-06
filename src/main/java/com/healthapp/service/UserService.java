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
import java.util.regex.Pattern;

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
    
    @Transactional(readOnly = true)
    public Optional<User> getUserByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserByAppleId(String appleId) {
        return userRepository.findByAppleId(appleId);
    }
    
    @Transactional
    public User findOrCreateUserByGoogleInfo(String googleId, String email, String firstName, String lastName) {
        // First, try to find by Google ID
        Optional<User> userOpt = getUserByGoogleId(googleId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Update Google ID if it wasn't set
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user = userRepository.save(user);
            }
            return user;
        }
        
        // Try to find by email (existing user might be signing in with Google for first time)
        userOpt = getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Link Google ID to existing account
            user.setGoogleId(googleId);
            return userRepository.save(user);
        }
        
        // Create new user
        // Generate username from email: take part before @, normalize to alphanumeric + underscore only
        String localPart = email != null ? email.split("@")[0] : "";
        String baseUsername = normalizeUsernameFromEmailLocalPart(localPart);
        // Ensure username is unique (base truncated to 16 so base + suffix fits in 20 chars)
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }
        if (username.length() > 20) {
            username = username.substring(0, 20);
        }
        
        User newUser = new User();
        newUser.setGoogleId(googleId);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setFirstName(firstName != null ? firstName : "");
        newUser.setLastName(lastName);
        // Set a random password (won't be used for Google sign-in)
        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        newUser.setRole(User.UserRole.USER);
        newUser.setAccountStatus(User.AccountStatus.ACTIVE);
        // Set default activity level (required field - user can update later)
        newUser.setActivityLevel(User.ActivityLevel.MODERATE);
        
        return userRepository.save(newUser);
    }
    
    @Transactional
    public User findOrCreateUserByAppleInfo(String appleId, String email, String firstName, String lastName) {
        // First, try to find by Apple ID
        Optional<User> userOpt = getUserByAppleId(appleId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getAppleId() == null) {
                user.setAppleId(appleId);
                user = userRepository.save(user);
            }
            // Optionally update name/email if provided and currently null
            boolean updated = false;
            if (firstName != null && !firstName.isEmpty() && (user.getFirstName() == null || user.getFirstName().isEmpty())) {
                user.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && user.getLastName() == null) {
                user.setLastName(lastName);
                updated = true;
            }
            if (email != null && !email.isEmpty() && user.getEmail() == null) {
                user.setEmail(email);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            return user;
        }
        
        // Try to find by email (existing user signing in with Apple for first time)
        if (email != null && !email.trim().isEmpty()) {
            userOpt = getUserByEmail(email.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAppleId(appleId);
                return userRepository.save(user);
            }
        }
        
        // Create new user - use placeholder email if Apple did not provide one
        String resolvedEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        if (resolvedEmail == null) {
            String sanitizedSub = appleId != null ? appleId.replaceAll("[^a-zA-Z0-9]", "") : "";
            resolvedEmail = "apple_" + (sanitizedSub.length() > 50 ? sanitizedSub.substring(0, 50) : sanitizedSub) + "@privaterelay.appleid.com";
        }
        
        String localPart = resolvedEmail.split("@")[0];
        String baseUsername = normalizeUsernameFromEmailLocalPart(localPart);
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }
        if (username.length() > 20) {
            username = username.substring(0, 20);
        }
        
        User newUser = new User();
        newUser.setAppleId(appleId);
        newUser.setEmail(resolvedEmail);
        newUser.setUsername(username);
        newUser.setFirstName(firstName != null ? firstName : "");
        newUser.setLastName(lastName);
        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        newUser.setRole(User.UserRole.USER);
        newUser.setAccountStatus(User.AccountStatus.ACTIVE);
        newUser.setActivityLevel(User.ActivityLevel.MODERATE);
        
        return userRepository.save(newUser);
    }
    
    /**
     * Normalize email local part (part before @) to a username that only contains
     * alphanumeric characters and underscores, matching DB constraint chk_username.
     * Truncated to 16 chars so that base + numeric suffix fits in username length 20.
     */
    private static final Pattern USERNAME_ALLOWED = Pattern.compile("[^a-zA-Z0-9_]");
    
    private String normalizeUsernameFromEmailLocalPart(String localPart) {
        if (localPart == null) {
            localPart = "";
        }
        String normalized = USERNAME_ALLOWED.matcher(localPart.trim().toLowerCase()).replaceAll("");
        if (normalized.isEmpty()) {
            normalized = "user" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        }
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }
    
    @Transactional
    public User createUser(User user) {
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
            
            // Encode password before saving
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            User savedUser = userRepository.save(user);
            return savedUser;
        } catch (Exception e) {
            logger.error("User creation failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Transactional
    public User updateUser(Long id, User userDetails) {
        
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
        
    }
    
    @Transactional(readOnly = true)
    private boolean hasActiveData(User user) {
        // Check if user has any active food entries, activity entries, etc.
        // This is a placeholder - implement based on your business logic
        // For now, we'll allow deletion but log it
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
        
        return userRepository.findById(id)
                .map(user -> {
                    // Apply patch changes
                    patchRequest.applyToUser(user);
                    
                    // Encode password if it was updated
                    if (patchRequest.getPassword() != null) {
                        user.setPassword(passwordEncoder.encode(patchRequest.getPassword()));
                    }
                    
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Change user password
     * @param userId The ID of the user changing password
     * @param currentPassword The current password for verification
     * @param newPassword The new password to set
     * @throws RuntimeException if user not found, current password is incorrect, or account is not active
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if account is active
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new RuntimeException("Cannot change password for inactive account");
        }
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password is different from current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }
        
        // Encode and set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
    }
} 