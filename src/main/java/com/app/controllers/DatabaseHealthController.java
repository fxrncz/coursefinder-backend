package com.app.controllers;

import com.app.models.User;
import com.app.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/database")
public class DatabaseHealthController {

    @Autowired
    private UserRepository userRepository;

    // Test database connection
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test basic database operations
            long userCount = userRepository.countUsers();
            
            response.put("status", "SUCCESS");
            response.put("message", "Database connection is working!");
            response.put("timestamp", LocalDateTime.now());
            response.put("userCount", userCount);
            response.put("database", "PostgreSQL");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Database connection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // Create a test user
    @PostMapping("/test-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if test user already exists
            if (userRepository.existsByUsername("testuser")) {
                response.put("status", "INFO");
                response.put("message", "Test user already exists");
                response.put("user", userRepository.findByUsername("testuser").orElse(null));
                return ResponseEntity.ok(response);
            }
            
            // Create a test user with all fields
            User testUser = new User("testuser", "test@example.com", "testpassword123", 25, "Male");
            User savedUser = userRepository.save(testUser);
            
            response.put("status", "SUCCESS");
            response.put("message", "Test user created successfully");
            response.put("user", savedUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to create test user: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // Get all users
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<User> users = userRepository.findAll();
            
            response.put("status", "SUCCESS");
            response.put("message", "Retrieved all users successfully");
            response.put("userCount", users.size());
            response.put("users", users);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve users: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // Delete test user
    @DeleteMapping("/test-user")
    public ResponseEntity<Map<String, Object>> deleteTestUser() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> testUser = userRepository.findByUsername("testuser");
            
            if (testUser.isPresent()) {
                userRepository.delete(testUser.get());
                response.put("status", "SUCCESS");
                response.put("message", "Test user deleted successfully");
            } else {
                response.put("status", "INFO");
                response.put("message", "Test user not found");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to delete test user: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }
} 