package com.healthapp.service;

import com.healthapp.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtAccessTokenService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtAccessTokenService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration-seconds:3600}") long accessTokenTtlSeconds) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = sha256Digest(jwtSecret);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    private static byte[] sha256Digest(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenTtlSeconds * 1000L);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates signature and expiry; does not check user status in DB.
     */
    public Optional<AccessTokenClaims> parseValidAccessToken(String compactJwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(compactJwt)
                    .getBody();
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            if (role == null) {
                return Optional.empty();
            }
            return Optional.of(new AccessTokenClaims(userId, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static boolean looksLikeJwt(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        int dots = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '.') {
                dots++;
            }
        }
        return dots == 2;
    }

    public record AccessTokenClaims(Long userId, String role) {}
}
