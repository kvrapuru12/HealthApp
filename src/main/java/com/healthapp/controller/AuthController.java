package com.healthapp.controller;

import com.healthapp.dto.AppleSignInRequest;
import com.healthapp.dto.ChangePasswordRequest;
import com.healthapp.dto.GoogleSignInRequest;
import com.healthapp.dto.IssuedAuthTokens;
import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.LoginResponse;
import com.healthapp.dto.RefreshRotationResult;
import com.healthapp.dto.RefreshTokenRequest;
import com.healthapp.entity.User;
import com.healthapp.service.AppleTokenVerifierService;
import com.healthapp.service.GoogleTokenVerifierService;
import com.healthapp.service.RefreshTokenService;
import com.healthapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for testing")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private GoogleTokenVerifierService googleTokenVerifierService;
    
    @Autowired
    private AppleTokenVerifierService appleTokenVerifierService;

    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Find user by username
            User user = userService.getUserByUsername(loginRequest.getUsername())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authentication failed",
                    "message", "Invalid username or password"
                ));
            }
            
            // Check password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authentication failed",
                    "message", "Invalid username or password"
                ));
            }
            
            // Check if account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Account inactive",
                    "message", "Account is not active"
                ));
            }
            
            IssuedAuthTokens tokens = refreshTokenService.issueNewSession(user);
            return ResponseEntity.ok(buildLoginResponse(user, tokens));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Login failed",
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/google")
    @Operation(
        summary = "Google Sign-In",
        description = "Authenticate user using Google ID token. Creates a new user account if one doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sign-in successful"),
        @ApiResponse(responseCode = "400", description = "Invalid Google ID token"),
        @ApiResponse(responseCode = "401", description = "Token verification failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        try {
            // Verify the Google ID token with platform-specific client ID
            Map<String, Object> tokenPayload = googleTokenVerifierService.verifyToken(
                    request.getIdToken(), request.getPlatform());
            
            if (tokenPayload == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Token verification failed",
                    "message", "Invalid or expired Google ID token"
                ));
            }
            
            // Extract user information from token payload
            String googleId = (String) tokenPayload.get("sub");
            String email = (String) tokenPayload.get("email");
            String firstName = (String) tokenPayload.get("given_name");
            String lastName = (String) tokenPayload.get("family_name");
            
            if (googleId == null || email == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid token payload",
                    "message", "Token does not contain required user information"
                ));
            }
            
            // Find or create user
            User user = userService.findOrCreateUserByGoogleInfo(googleId, email, firstName, lastName);
            
            // Check if account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Account inactive",
                    "message", "Account is not active"
                ));
            }
            
            IssuedAuthTokens tokens = refreshTokenService.issueNewSession(user);
            return ResponseEntity.ok(buildLoginResponse(user, tokens));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Google sign-in failed",
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/apple")
    @Operation(
        summary = "Sign in with Apple",
        description = "Authenticate user using Apple identity token. Creates a new user account if one doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sign-in successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or token payload"),
        @ApiResponse(responseCode = "401", description = "Token verification failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> appleSignIn(@Valid @RequestBody AppleSignInRequest request) {
        try {
            Map<String, Object> tokenPayload = appleTokenVerifierService.verifyToken(
                    request.getIdToken(), request.getPlatform());
            
            if (tokenPayload == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Token verification failed",
                    "message", "Invalid or expired Apple identity token"
                ));
            }
            
            String appleId = (String) tokenPayload.get("sub");
            if (appleId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid token payload",
                    "message", "Token does not contain subject (Apple user ID)"
                ));
            }
            
            String emailFromToken = (String) tokenPayload.get("email");
            String email = request.getEmail() != null && !request.getEmail().isBlank()
                    ? request.getEmail().trim() : emailFromToken;
            String firstName = request.getFirstName() != null && !request.getFirstName().isBlank()
                    ? request.getFirstName() : null;
            String lastName = request.getLastName() != null ? request.getLastName() : null;
            
            User user = userService.findOrCreateUserByAppleInfo(appleId, email, firstName, lastName);
            
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Account inactive",
                    "message", "Account is not active"
                ));
            }
            
            IssuedAuthTokens tokens = refreshTokenService.issueNewSession(user);
            return ResponseEntity.ok(buildLoginResponse(user, tokens));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Apple sign-in failed",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchange a valid refresh token for a new access token and rotated refresh token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        Optional<RefreshRotationResult> rotation = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        if (rotation.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or expired refresh token"
            ));
        }
        RefreshRotationResult r = rotation.get();
        return ResponseEntity.ok(buildLoginResponse(r.user(), r.tokens()));
    }
    
    @PostMapping("/change-password")
    @Operation(
        summary = "Change user password",
        description = "Change the password for the authenticated user. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or current password is incorrect"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - account is not active")
    })
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            // Get authenticated user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Authentication required"
                ));
            }
            
            Long userId;
            try {
                userId = (Long) authentication.getPrincipal();
            } catch (ClassCastException e) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid authentication token"
                ));
            }
            
            // Change password
            userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            refreshTokenService.revokeAllActiveForUserId(userId);

            return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Password change failed",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "An error occurred while changing password"
            ));
        }
    }

    private static LoginResponse buildLoginResponse(User user, IssuedAuthTokens tokens) {
        LoginResponse response = new LoginResponse();
        response.setToken(tokens.accessToken());
        response.setRefreshToken(tokens.refreshToken());
        response.setExpiresIn(tokens.expiresInSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setGender(user.getGender() != null ? user.getGender().name() : null);
        response.setProfileComplete(user.isProfileComplete());
        response.setMessage("Login successful");
        return response;
    }
} 