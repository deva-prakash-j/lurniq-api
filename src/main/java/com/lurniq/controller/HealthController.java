package com.lurniq.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@Slf4j
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                
                Map<String, Object> health = Map.of(
                    "status", isValid ? "UP" : "DOWN",
                    "database", isValid ? "Connected" : "Disconnected",
                    "timestamp", System.currentTimeMillis()
                );
                
                return ResponseEntity.ok(health);
            }
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "database", "Connection failed",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(503).body(health);
        }
    }
    
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of(
            "status", "READY",
            "message", "Application is ready to serve requests"
        ));
    }
}
