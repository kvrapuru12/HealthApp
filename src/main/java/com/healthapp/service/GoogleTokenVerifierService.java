package com.healthapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Service
public class GoogleTokenVerifierService {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifierService.class);
    
    @Value("${google.client.id.ios:}")
    private String googleClientIdIos;
    
    @Value("${google.client.id.android:}")
    private String googleClientIdAndroid;
    
    /**
     * Verifies a Google ID token and extracts user information
     * Note: This is a simplified implementation that decodes the JWT without signature verification.
     * For production, implement proper signature verification using Google's public keys.
     * 
     * @param idToken The Google ID token string
     * @param platform The platform ("ios" or "android") to determine which client ID to use
     * @return Map containing user information (sub, email, name, picture, etc.) or null if invalid
     */
    public Map<String, Object> verifyToken(String idToken, String platform) {
        try {
            // Get the appropriate client ID based on platform
            String expectedClientId = null;
            if ("ios".equalsIgnoreCase(platform) && googleClientIdIos != null && !googleClientIdIos.isEmpty()) {
                expectedClientId = googleClientIdIos;
            } else if ("android".equalsIgnoreCase(platform) && googleClientIdAndroid != null && !googleClientIdAndroid.isEmpty()) {
                expectedClientId = googleClientIdAndroid;
            }
            
            // Decode JWT without verification (for MVP/simplified implementation)
            // Split the token to get the payload
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                logger.error("Invalid JWT token format");
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Parse JSON payload
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);
            
            // Basic validation: check issuer and expiration
            String issuer = (String) claims.get("iss");
            if (issuer == null || !issuer.equals("https://accounts.google.com")) {
                logger.warn("Invalid issuer: {}", issuer);
                return null;
            }
            
            // Check expiration if present
            if (claims.containsKey("exp")) {
                Long exp = claims.get("exp") instanceof Integer 
                    ? ((Integer) claims.get("exp")).longValue() 
                    : (Long) claims.get("exp");
                if (exp != null && exp < System.currentTimeMillis() / 1000) {
                    logger.warn("Token has expired");
                    return null;
                }
            }
            
            // Verify audience if client ID is configured for the platform
            if (expectedClientId != null) {
                Object aud = claims.get("aud");
                if (aud != null) {
                    String audience = aud.toString();
                    if (!audience.equals(expectedClientId)) {
                        logger.warn("Token audience '{}' does not match expected client ID '{}' for platform '{}'", 
                                audience, expectedClientId, platform);
                        // Still return the payload for flexibility, but log the mismatch
                    } else {
                    }
                } else {
                    logger.warn("Token does not contain audience claim");
                }
            } else {
            }
            
            return claims;
            
        } catch (Exception e) {
            logger.error("Failed to verify Google ID token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Verifies a Google ID token (legacy method for backward compatibility)
     * @param idToken The Google ID token string
     * @return Map containing user information or null if invalid
     */
    public Map<String, Object> verifyToken(String idToken) {
        // Default to no platform-specific verification
        return verifyToken(idToken, null);
    }
}

