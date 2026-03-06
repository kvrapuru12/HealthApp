package com.healthapp.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Verifies Apple Sign In identity tokens (JWT) using Apple's JWKS.
 * Validates signature (ES256), issuer, audience, and expiration.
 */
@Service
public class AppleTokenVerifierService {

    private static final Logger logger = LoggerFactory.getLogger(AppleTokenVerifierService.class);
    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final long JWKS_CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    @Value("${apple.client.id:}")
    private String appleClientId;

    @Value("${apple.client.id.ios:}")
    private String appleClientIdIos;

    @Value("${apple.client.id.android:}")
    private String appleClientIdAndroid;

    @Value("${apple.client.id.web:}")
    private String appleClientIdWeb;

    @Value("${apple.client.id.allow-expo-go-audience:false}")
    private boolean allowExpoGoAudience;

    @Value("${apple.client.id.expo-go.audience:host.exp.Exponent}")
    private String expoGoAudience;

    @Value("${apple.client.id.allowed-audiences:}")
    private String additionalAllowedAudiences;

    private final RestTemplate restTemplate = new RestTemplate();

    private volatile JWKSet cachedJwkSet;
    private volatile long cachedJwkSetTime;

    /**
     * Verifies the Apple identity token and returns the claims if valid.
     *
     * @param identityToken The Apple identity token (JWT)
     * @param platform      Optional platform ("ios", "android", "web") to select audience
     * @return Map of claims (sub, email if present) or null if verification fails
     */
    public Map<String, Object> verifyToken(String identityToken, String platform) {
        try {
            JWKSet jwkSet = getAppleJwkSet();
            if (jwkSet == null) {
                logger.error("Failed to load Apple JWKS");
                return null;
            }

            String expectedAudience = resolveClientId(platform);
            if (expectedAudience == null || expectedAudience.isEmpty()) {
                logger.warn("No Apple client ID configured for platform: {}", platform);
            }

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                    Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256), jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claimsSet = jwtProcessor.process(identityToken, null);

            // Validate expiration
            if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(new Date())) {
                logger.warn("Apple token has expired");
                return null;
            }

            // Validate issuer
            String iss = claimsSet.getIssuer();
            if (iss == null || !APPLE_ISSUER.equals(iss)) {
                logger.warn("Invalid Apple token issuer: {}", iss);
                return null;
            }

            // Validate audience if client ID is configured
            if (expectedAudience != null && !expectedAudience.isEmpty()) {
                String aud = claimsSet.getAudience() != null && !claimsSet.getAudience().isEmpty()
                        ? claimsSet.getAudience().get(0) : null;
                if (aud == null || !isAllowedAudience(aud, expectedAudience, platform)) {
                    logger.warn("Token audience '{}' does not match expected '{}' for platform '{}'",
                            aud, expectedAudience, platform);
                    return null;
                }
            }

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("sub", claimsSet.getSubject());
            if (claimsSet.getClaim("email") != null) {
                result.put("email", claimsSet.getClaim("email"));
            }
            return result;

        } catch (Exception e) {
            logger.error("Apple token verification failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verifies the Apple identity token (no platform-specific audience).
     */
    public Map<String, Object> verifyToken(String identityToken) {
        return verifyToken(identityToken, null);
    }

    private String resolveClientId(String platform) {
        if (platform != null) {
            switch (platform.toLowerCase()) {
                case "ios":
                    if (appleClientIdIos != null && !appleClientIdIos.isEmpty()) return appleClientIdIos;
                    break;
                case "android":
                    if (appleClientIdAndroid != null && !appleClientIdAndroid.isEmpty()) return appleClientIdAndroid;
                    break;
                case "web":
                    if (appleClientIdWeb != null && !appleClientIdWeb.isEmpty()) return appleClientIdWeb;
                    break;
                default:
                    break;
            }
        }
        return appleClientId != null && !appleClientId.isEmpty() ? appleClientId : null;
    }

    private boolean isAllowedAudience(String aud, String expectedAudience, String platform) {
        if (expectedAudience.equals(aud)) {
            return true;
        }

        Set<String> allowedAudiences = parseAdditionalAllowedAudiences();
        if (allowedAudiences.contains(aud)) {
            return true;
        }

        return allowExpoGoAudience
                && "ios".equalsIgnoreCase(platform)
                && expoGoAudience != null
                && expoGoAudience.equals(aud);
    }

    private Set<String> parseAdditionalAllowedAudiences() {
        if (additionalAllowedAudiences == null || additionalAllowedAudiences.isBlank()) {
            return Set.of();
        }

        Set<String> allowed = new HashSet<>();
        Arrays.stream(additionalAllowedAudiences.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(allowed::add);
        return allowed;
    }

    private synchronized JWKSet getAppleJwkSet() {
        long now = System.currentTimeMillis();
        if (cachedJwkSet != null && (now - cachedJwkSetTime) < JWKS_CACHE_TTL_MS) {
            return cachedJwkSet;
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(APPLE_KEYS_URL, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedJwkSet = JWKSet.parse(response.getBody());
                cachedJwkSetTime = now;
                return cachedJwkSet;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch Apple JWKS: {}", e.getMessage());
        }
        return cachedJwkSet; // return stale if available
    }
}
