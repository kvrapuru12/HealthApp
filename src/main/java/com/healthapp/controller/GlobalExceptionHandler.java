package com.healthapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        // Collect all field errors
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage()));
        
        // Collect all global errors (if any)
        Map<String, String> globalErrors = new HashMap<>();
        ex.getBindingResult().getGlobalErrors().forEach(error -> 
            globalErrors.put("global", error.getDefaultMessage()));
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("message", "Please fix the following validation errors");
        response.put("totalErrors", fieldErrors.size() + globalErrors.size());
        response.put("fieldErrors", fieldErrors);
        if (!globalErrors.isEmpty()) {
            response.put("globalErrors", globalErrors);
        }
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Business logic error");
        response.put("message", ex.getMessage());
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the exception with full stack trace
        System.err.println("=== GLOBAL EXCEPTION HANDLER ===");
        System.err.println("Exception Type: " + ex.getClass().getName());
        System.err.println("Exception Message: " + ex.getMessage());
        ex.printStackTrace(System.err);
        if (ex.getCause() != null) {
            System.err.println("Caused by: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage());
            ex.getCause().printStackTrace(System.err);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal server error");
        response.put("message", "An unexpected error occurred. Please try again later.");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 