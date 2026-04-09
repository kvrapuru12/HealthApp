package com.healthapp.dto;

/**
 * Access + refresh pair returned from login, social sign-in, and refresh.
 */
public record IssuedAuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {}
