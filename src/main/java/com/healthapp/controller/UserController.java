package com.healthapp.controller;

import com.healthapp.annotation.RateLimit;
import com.healthapp.entity.User;
import com.healthapp.dto.UserCreateRequest;
import com.healthapp.dto.UserPatchRequest;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs for managing users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ValidationService validationService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(value = 10, timeUnit = "MINUTES")
    @Operation(
        summary = "Get all users with pagination and filtering", 
        description = "Retrieve a paginated list of users with optional filtering and sorting. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Users retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Access denied - Admin role required"
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Too many requests - Rate limit exceeded"
        )
    })
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) User.AccountStatus status,
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(required = false) String search
    ) {
        Page<User> usersPage;
        
        // Apply filters based on provided parameters
        if (status != null && role != null) {
            usersPage = userService.getUsersByStatusAndRole(status, role, page, size, sortBy, sortDir);
        } else if (status != null) {
            usersPage = userService.getUsersByStatus(status, page, size, sortBy, sortDir);
        } else if (role != null) {
            usersPage = userService.getUsersByRole(role, page, size, sortBy, sortDir);
        } else if (search != null && !search.trim().isEmpty()) {
            usersPage = userService.searchUsersByName(search.trim(), page, size, sortBy, sortDir);
        } else {
            usersPage = userService.getAllUsersPaginated(page, size, sortBy, sortDir);
        }
        
        // Convert to UserResponse DTOs
        Page<UserResponse> userResponses = usersPage.map(UserResponse::fromUser);
        
        return ResponseEntity.ok(userResponses);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @RateLimit(value = 30, timeUnit = "MINUTES")
    @Operation(
        summary = "Get user by ID", 
        description = "Retrieve a specific user by their ID. Users can only access their own profile unless they have ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid user ID format"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Access denied - User can only access their own profile or ADMIN role required"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found or account inactive"
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Too many requests - Rate limit exceeded"
        )
    })
    public ResponseEntity<?> getUserById(@PathVariable @Min(1) @Max(Long.MAX_VALUE) Long id) {
        try {
            // Log the access attempt for audit purposes
            logger.info("User access attempt for ID: {}", id);
            
            return userService.getUserById(id)
                    .filter(user -> {
                        // Check if user account is active (unless admin)
                        boolean isActive = user.getAccountStatus() == User.AccountStatus.ACTIVE;
                        if (!isActive) {
                            logger.warn("Attempted to access inactive user account: {}", id);
                        }
                        return isActive;
                    })
                    .map(UserResponse::fromUser)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID provided: {}", id);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid user ID",
                "message", "User ID must be a positive number",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving user with ID: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Unable to retrieve user at this time",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
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
    

    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Patch user", 
        description = "Partially update an existing user's information with validation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User patched successfully",
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
                    name = "PATCH Validation Errors Example",
                    value = """
                    {
                        "error": "Validation failed",
                        "message": "Please fix the following validation errors",
                        "totalErrors": 2,
                        "fieldErrors": {
                            "email": "Email already exists",
                            "weight": "Weight must be between 30 and 300 kg"
                        },
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> patchUser(@PathVariable Long id, @RequestBody UserPatchRequest patchRequest) {
        try {
            // Custom validation to collect all errors at once
            Map<String, String> validationErrors = validationService.validateUserPatch(patchRequest, id);
            
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Validation failed");
                response.put("message", "Please fix the following validation errors");
                response.put("totalErrors", validationErrors.size());
                response.put("fieldErrors", validationErrors);
                response.put("timestamp", java.time.LocalDateTime.now().toString());
                return ResponseEntity.badRequest().body(response);
            }
            
            User patchedUser = userService.patchUser(id, patchRequest);
            return ResponseEntity.ok(UserResponse.fromUser(patchedUser));
        } catch (RuntimeException e) {
            // Enhanced error response for business logic errors
            return ResponseEntity.badRequest().body(Map.of(
                "error", "User patch failed",
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
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @RateLimit(value = 5, timeUnit = "MINUTES")
    @Operation(
        summary = "Delete user account", 
        description = "Soft delete a user account. Users can only delete their own account unless they have ADMIN role. This marks the account as deleted but preserves data for audit purposes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User account deleted successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "message": "User account deleted successfully",
                        "userId": 123,
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid user ID or deletion not allowed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Deletion Error Example",
                    value = """
                    {
                        "error": "Deletion not allowed",
                        "message": "Cannot delete user with active data. Please deactivate the account instead.",
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Access denied - User can only delete their own account or ADMIN role required"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found or already deleted"
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Too many requests - Rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> deleteUser(@PathVariable @Min(1) @Max(Long.MAX_VALUE) Long id) {
        try {
            // Log the deletion attempt for audit purposes
            logger.info("User deletion attempt for ID: {}", id);
            
            // Check if user exists and is active before deletion
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Additional validation for deletion
            if (user.getAccountStatus() == User.AccountStatus.DELETED) {
                logger.warn("Attempted to delete already deleted user: {}", id);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User already deleted",
                    "message", "This user account has already been deleted",
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
            }
            
            // Perform the deletion
            userService.deleteUser(id);
            
            // Return success response
            return ResponseEntity.ok(Map.of(
                "message", "User account deleted successfully",
                "userId", id,
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID provided for deletion: {}", id);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid user ID",
                "message", "User ID must be a positive number",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (RuntimeException e) {
            logger.warn("User deletion failed for ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Deletion failed",
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during user deletion for ID: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Unable to delete user at this time. Please try again later.",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
} 