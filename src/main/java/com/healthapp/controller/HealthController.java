package com.healthapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/simple")
    public ResponseEntity<String> simpleHealth() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Test with a simple query
                statement.execute("SELECT 1");
                
                response.put("database", "connected");
                response.put("databaseUrl", connection.getMetaData().getURL());
                response.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
                response.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            }
            
            response.put("status", "healthy");
            response.put("timestamp", System.currentTimeMillis());
            response.put("memory", getMemoryInfo());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test database connectivity
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
            
            response.put("status", "ready");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "not_ready");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "alive");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> memory = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        memory.put("totalMemory", runtime.totalMemory());
        memory.put("freeMemory", runtime.freeMemory());
        memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memory.put("maxMemory", runtime.maxMemory());
        
        return memory;
    }
} 