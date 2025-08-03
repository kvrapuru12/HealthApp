package com.healthapp.controller;

import com.healthapp.entity.User;
import com.healthapp.dto.UserCreateRequest;
import com.healthapp.dto.UserResponse;
import com.healthapp.service.UserService;
import com.healthapp.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs for managing users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ValidationService validationService;
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromUser)
                .toList();
        return ResponseEntity.ok(userResponses);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new user", 
        description = "Create a new user account with comprehensive validation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Multiple Validation Errors Example",
                    value = """
                    {
                        "error": "Validation failed",
                        "message": "Please fix the following validation errors",
                        "totalErrors": 4,
                        "fieldErrors": {
                            "firstName": "First name is required",
                            "email": "Email must be a valid email address",
                            "phoneNumber": "Phone number must be in international format",
                            "password": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character",
                            "dob": "User must be at least 13 years old",
                            "username": "Username already exists"
                        },
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error Example",
                    value = """
                    {
                        "error": "Internal server error",
                        "message": "Unable to process request at this time. Please try again later.",
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> createUser(
        @RequestBody UserCreateRequest userRequest
    ) {
        try {
            // Custom validation to collect all errors at once
            Map<String, String> validationErrors = validationService.validateUserCreation(userRequest);
            
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Validation failed");
                response.put("message", "Please fix the following validation errors");
                response.put("totalErrors", validationErrors.size());
                response.put("fieldErrors", validationErrors);
                response.put("timestamp", java.time.LocalDateTime.now().toString());
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userRequest.toEntity();
            User savedUser = userService.createUser(user);
            return ResponseEntity.ok(UserResponse.fromUser(savedUser));
        } catch (RuntimeException e) {
            // Enhanced error response for business logic errors
            return ResponseEntity.badRequest().body(Map.of(
                "error", "User creation failed",
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            // Enhanced error response for unexpected errors
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Unable to process request at this time. Please try again later.",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserCreateRequest userRequest) {
        try {
            User user = userRequest.toEntity();
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user account")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }
} 