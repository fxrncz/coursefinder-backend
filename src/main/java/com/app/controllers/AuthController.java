package com.app.controllers;

import com.app.models.User;
import com.app.repositories.UserRepository;
import com.app.repositories.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.app.security.PasswordUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    // Using PBKDF2-based hashing utility (no external security deps required)

    // Test endpoint to check users in database
    @GetMapping("/test-users")
    public ResponseEntity<Map<String, Object>> testUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<User> users = userRepository.findAll();
            System.out.println("Total users in database: " + users.size());
            
            for (User user : users) {
                System.out.println("User: " + user.getUsername() + ", Email: " + user.getEmail() + ", ID: " + user.getId());
            }
            
            response.put("success", true);
            response.put("message", "Database connection working");
            response.put("userCount", users.size());
            response.put("users", users.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail()
            )).collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Test users error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Database error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Start registration: store pending and send code (no user created yet)
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            System.out.println("Registration attempt for username: " + username + ", email: " + email);
            
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if username already exists
            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create user immediately (no email verification)
            String hashedPassword = PasswordUtil.hashPassword(password);
            User newUser = new User(username, email, hashedPassword);
            User savedUser = userRepository.save(newUser);

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", savedUser.getId());
            userResponse.put("username", savedUser.getUsername());
            userResponse.put("email", savedUser.getEmail());
            userResponse.put("age", savedUser.getAge());
            userResponse.put("gender", savedUser.getGender());
            userResponse.put("createdAt", savedUser.getCreatedAt());

            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("user", userResponse);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");
            
            System.out.println("Login attempt for email: " + email);
            System.out.println("Request body: " + request);
            
            // Validate input
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(email);
            
            if (userOptional.isEmpty()) {
                System.out.println("User not found for email: " + email);
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOptional.get();
            System.out.println("User found: " + user.getUsername() + ", ID: " + user.getId());
            
            // Check if user data is valid
            if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
                System.out.println("User data is null - username: " + user.getUsername() + ", email: " + user.getEmail() + ", password: " + (user.getPassword() != null ? "not null" : "null"));
                response.put("success", false);
                response.put("message", "User data corrupted");
                return ResponseEntity.status(500).body(response);
            }
            
            boolean isHashedFormat = user.getPassword() != null && user.getPassword().contains(":");

            boolean passwordOk;
            if (isHashedFormat) {
                // Verify hashed password
                passwordOk = PasswordUtil.verifyPassword(password, user.getPassword());
            } else {
                // Legacy plaintext password in DB: compare directly and upgrade to hashed
                passwordOk = password.equals(user.getPassword());
                if (passwordOk) {
                    String upgradedHash = PasswordUtil.hashPassword(password);
                    user.setPassword(upgradedHash);
                    userRepository.save(user);
                }
            }

            if (!passwordOk) {
                System.out.println("Password mismatch for user: " + user.getUsername());
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("Login successful for user: " + user.getUsername());
            response.put("success", true);
            response.put("message", "Login successful");
            
            // Create user response map safely
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("username", user.getUsername());
            userResponse.put("email", user.getEmail());
            userResponse.put("age", user.getAge());
            userResponse.put("gender", user.getGender());
            userResponse.put("createdAt", user.getCreatedAt());
            
            response.put("user", userResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Login failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return ResponseEntity.status(500).body(response);
        }
    }

    // Update user profile endpoint
    @PutMapping("/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = Long.parseLong(request.get("id").toString());
            String username = (String) request.get("username");
            String email = (String) request.get("email");
            String newPassword = (String) request.get("newPassword");
            Integer age = request.get("age") != null ? Integer.parseInt(request.get("age").toString()) : null;
            String gender = (String) request.get("gender");
            
            System.out.println("Update profile attempt for user ID: " + userId);
            
            // Find user by ID
            Optional<User> userOptional = userRepository.findById(userId);
            
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOptional.get();
            
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if username already exists (excluding current user)
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email already exists (excluding current user)
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update user fields
            user.setUsername(username);
            user.setEmail(email);
            user.setAge(age);
            user.setGender(gender);
            
            // Update password if provided (store hashed)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                String hashed = PasswordUtil.hashPassword(newPassword);
                user.setPassword(hashed);
            }
            
            User updatedUser = userRepository.save(user);
            
            System.out.println("User profile updated successfully: " + updatedUser.getUsername());
            
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            
            // Create user response map safely (allowing null values)
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", updatedUser.getId());
            userResponse.put("username", updatedUser.getUsername());
            userResponse.put("email", updatedUser.getEmail());
            userResponse.put("age", updatedUser.getAge());
            userResponse.put("gender", updatedUser.getGender());
            userResponse.put("createdAt", updatedUser.getCreatedAt());
            
            response.put("user", userResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Update profile error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Update failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Delete user account endpoint
    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = Long.parseLong(request.get("id").toString());
            
            System.out.println("Delete account attempt for user ID: " + userId);
            
            // Find user by ID
            Optional<User> userOptional = userRepository.findById(userId);
            
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOptional.get();
            String username = user.getUsername();
            
            // Delete user's test results first to maintain referential integrity and privacy
            try {
                testResultRepository.deleteByUserId(userId);
            } catch (Exception ignore) {
                // Proceed even if no results or soft failure; user deletion will continue
            }

            // Delete user
            userRepository.delete(user);
            
            System.out.println("User account deleted successfully: " + username);
            
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Delete account error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 