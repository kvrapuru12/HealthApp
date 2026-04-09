package com.healthapp.service;

import com.healthapp.dto.IssuedAuthTokens;
import com.healthapp.dto.RefreshRotationResult;
import com.healthapp.entity.RefreshToken;
import com.healthapp.entity.User;
import com.healthapp.repository.RefreshTokenRepository;
import com.healthapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int OPAQUE_TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtAccessTokenService jwtAccessTokenService,
            @Value("${jwt.refresh-token-expiration-days:30}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtAccessTokenService = jwtAccessTokenService;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public IssuedAuthTokens issueNewSession(User user) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String plainRefresh = generateOpaqueToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(plainRefresh));
        entity.setExpiresAt(now.plusDays(refreshTokenTtlDays));
        entity.setCreatedAt(now);
        refreshTokenRepository.save(entity);
        String access = jwtAccessTokenService.createAccessToken(user);
        return new IssuedAuthTokens(access, plainRefresh, jwtAccessTokenService.getAccessTokenTtlSeconds());
    }

    /**
     * Validates refresh token, revokes it (rotation), and issues a new access + refresh pair.
     */
    @Transactional
    public Optional<RefreshRotationResult> rotateRefreshToken(String plainRefreshToken) {
        if (plainRefreshToken == null || plainRefreshToken.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(plainRefreshToken.trim());
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken row = opt.get();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (row.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        User user = userRepository.findById(row.getUser().getId()).orElse(null);
        if (user == null || user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            return Optional.empty();
        }
        row.setRevokedAt(now);
        refreshTokenRepository.save(row);
        IssuedAuthTokens tokens = issueNewSession(user);
        return Optional.of(new RefreshRotationResult(user, tokens));
    }

    @Transactional
    public void revokeAllActiveForUserId(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, LocalDateTime.now(ZoneOffset.UTC));
    }

    private static String generateOpaqueToken() {
        byte[] buf = new byte[OPAQUE_TOKEN_BYTES];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
