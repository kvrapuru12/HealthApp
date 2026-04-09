package com.healthapp.dto;

import com.healthapp.entity.User;

public record RefreshRotationResult(User user, IssuedAuthTokens tokens) {}
