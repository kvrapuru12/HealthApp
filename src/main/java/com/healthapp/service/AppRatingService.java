package com.healthapp.service;

import com.healthapp.dto.AppRatingCreateRequest;
import com.healthapp.dto.AppRatingResponse;
import com.healthapp.entity.AppRating;
import com.healthapp.entity.User;
import com.healthapp.repository.AppRatingRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppRatingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppRatingService.class);
    
    private static final List<String> VALID_PLATFORMS = Arrays.asList("ios", "android", "web");
    
    @Autowired
    private AppRatingRepository appRatingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public AppRatingResponse createAppRating(AppRatingCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        try {
            // Validate user ID matches authenticated user (unless admin)
            if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
                logger.error("User ID mismatch - request: {}, authenticated: {}", request.getUserId(), authenticatedUserId);
                throw new SecurityException("User ID must match authenticated user");
            }
            
            // Find the user
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> {
                        logger.error("User not found with ID: {}", request.getUserId());
                        return new IllegalArgumentException("User not found");
                    });
            
            // Validate platform
            String platform = request.getPlatform().toLowerCase();
            if (!VALID_PLATFORMS.contains(platform)) {
                logger.error("Invalid platform: {}", platform);
                throw new IllegalArgumentException("Platform must be one of: ios, android, web");
            }
            
            // Create and save the app rating
            AppRating appRating = new AppRating();
            appRating.setUser(user);
            appRating.setRating(request.getRating());
            appRating.setFeedback(request.getFeedback());
            appRating.setPlatform(platform);
            appRating.setAppVersion(request.getAppVersion());
            
            AppRating savedAppRating = appRatingRepository.save(appRating);
            appRatingRepository.flush(); // Force flush to get generated ID
            
            // Refresh the entity to ensure createdAt is populated by auditing
            Long savedId = savedAppRating.getId();
            savedAppRating = appRatingRepository.findById(savedId)
                    .orElseThrow(() -> {
                        logger.error("Failed to retrieve saved app rating with ID: {}", savedId);
                        return new IllegalStateException("Failed to retrieve saved app rating");
                    });
            
            // Double-check createdAt - if still null, set it manually
            if (savedAppRating.getCreatedAt() == null) {
                logger.warn("createdAt is null after save, setting manually");
                savedAppRating.setCreatedAt(java.time.LocalDateTime.now());
                savedAppRating = appRatingRepository.save(savedAppRating);
            }
            
            // Create response manually to avoid lazy loading issues with User entity
            AppRatingResponse response = new AppRatingResponse();
            response.setId(savedAppRating.getId());
            response.setUserId(request.getUserId()); // Use userId from request instead of entity
            response.setRating(savedAppRating.getRating());
            response.setFeedback(savedAppRating.getFeedback());
            response.setPlatform(savedAppRating.getPlatform());
            response.setAppVersion(savedAppRating.getAppVersion());
            response.setCreatedAt(savedAppRating.getCreatedAt());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to create app rating: {}", e.getMessage(), e);
            throw e; // Re-throw to be handled by controller
        }
    }
    
    @Transactional(readOnly = true)
    public List<AppRatingResponse> getAppRatingsByUserId(Long userId) {
        List<AppRating> appRatings = appRatingRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return appRatings.stream()
                .map(rating -> {
                    // Create response manually to avoid lazy loading issues
                    AppRatingResponse response = new AppRatingResponse();
                    response.setId(rating.getId());
                    response.setUserId(userId); // Use the parameter instead of accessing entity
                    response.setRating(rating.getRating());
                    response.setFeedback(rating.getFeedback());
                    response.setPlatform(rating.getPlatform());
                    response.setAppVersion(rating.getAppVersion());
                    response.setCreatedAt(rating.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AppRatingResponse> getAppRatingsByPlatform(String platform) {
        String platformLower = platform.toLowerCase();
        if (!VALID_PLATFORMS.contains(platformLower)) {
            throw new IllegalArgumentException("Platform must be one of: ios, android, web");
        }
        
        List<AppRating> appRatings = appRatingRepository.findByPlatform(platformLower);
        return appRatings.stream()
                .map(rating -> {
                    // Create response manually to avoid lazy loading issues
                    AppRatingResponse response = new AppRatingResponse();
                    response.setId(rating.getId());
                    // Need to fetch user ID - use a query join or fetch user
                    try {
                        response.setUserId(rating.getUser().getId());
                    } catch (Exception e) {
                        logger.error("Error accessing user ID for rating {}", rating.getId(), e);
                        throw new IllegalStateException("Failed to access user information");
                    }
                    response.setRating(rating.getRating());
                    response.setFeedback(rating.getFeedback());
                    response.setPlatform(rating.getPlatform());
                    response.setAppVersion(rating.getAppVersion());
                    response.setCreatedAt(rating.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }
}

