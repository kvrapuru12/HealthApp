package com.healthapp.controller;

import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.LoginResponse;
import com.healthapp.entity.User;
import com.healthapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for testing")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
            
            // Generate simple token (for testing purposes)
            String token = generateSimpleToken(user);
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setRole(user.getRole().name());
            response.setMessage("Login successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Login failed",
                "message", e.getMessage()
            ));
        }
    }
    
    private String generateSimpleToken(User user) {
        // Simple token generation for testing
        // In production, use proper JWT library
        return "Bearer " + user.getId() + "_" + user.getRole() + "_" + System.currentTimeMillis();
    }
} 