package com.app.controllers;

import com.app.models.User;
import com.app.models.PendingRegistration;
import com.app.repositories.UserRepository;
import com.app.repositories.TestResultRepository;
import com.app.repositories.PendingRegistrationRepository;
import com.app.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import com.app.security.PasswordUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestResultRepository testResultRepository;
    
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    
    
    @Autowired
    private EmailService emailService;

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

    // Start registration: store pending and send verification code
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
            
            // Check if username already exists in users table
            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email already exists in users table
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Clean up all expired registrations first
            pendingRegistrationRepository.deleteExpiredRegistrations(LocalDateTime.now());
            
            // Check if email already exists in pending registrations
            Optional<PendingRegistration> existingPending = pendingRegistrationRepository.findByEmail(email);
            if (existingPending.isPresent()) {
                PendingRegistration pending = existingPending.get();
                if (pending.isExpired()) {
                    // This shouldn't happen after cleanup, but just in case
                    pendingRegistrationRepository.delete(pending);
                    System.out.println("Deleted expired pending registration for: " + email + " - allowing re-registration");
                } else if (!pending.isConsumed()) {
                    // Only block if there's an active (non-expired) pending registration
                    response.put("success", false);
                    response.put("message", "Email verification already in progress. Please check your email or try again later.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Generate verification code
            String code = generateVerificationCode();
            String codeHash = sha256(code);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minutes expiration
            
            // Hash password
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            // Create pending registration
            PendingRegistration pendingRegistration = new PendingRegistration(
                username, email, hashedPassword, codeHash, expiresAt
            );
            
            System.out.println("Saving pending registration for: " + email);
            PendingRegistration savedPending = pendingRegistrationRepository.save(pendingRegistration);
            System.out.println("Pending registration saved with ID: " + savedPending.getId());
            
            // Send verification email
            try {
                System.out.println("Attempting to send verification email to: " + email + " with code: " + code);
                emailService.sendVerificationCode(email, code);
                System.out.println("Verification email sent successfully to: " + email);
            } catch (Exception emailError) {
                System.out.println("Failed to send verification email: " + emailError.getMessage());
                emailError.printStackTrace();
                // Don't fail registration if email fails, but log it
                // In production, you might want to handle this differently
            }

            response.put("success", true);
            response.put("message", "Registration successful! Please check your email for verification code.");
            response.put("email", email);
            response.put("requiresVerification", true);
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
            String email = user.getEmail();
            
            // Send account deletion confirmation email BEFORE deleting the account
            try {
                System.out.println("Attempting to send account deletion email to: " + email + " for user: " + username);
                emailService.sendAccountDeletionEmail(email, username);
                System.out.println("✅ Account deletion email sent successfully to: " + email);
            } catch (Exception emailError) {
                System.err.println("❌ Failed to send account deletion email to " + email + ": " + emailError.getMessage());
                emailError.printStackTrace();
                // Don't fail account deletion if email fails, but log it
            }
            
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
    
    // Test endpoint for email functionality
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            String code = generateVerificationCode();
            emailService.sendVerificationCode(email, code);
            
            response.put("success", true);
            response.put("message", "Test email sent successfully");
            response.put("email", email);
            response.put("code", code); // Only for testing - remove in production
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Email test failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Test endpoint for account deletion email
    @PostMapping("/test-deletion-email")
    public ResponseEntity<Map<String, Object>> testDeletionEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String testEmail = request.get("email");
            String testUsername = request.get("username");
            
            if (testEmail == null || testEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (testUsername == null || testUsername.trim().isEmpty()) {
                testUsername = "TestUser";
            }
            
            System.out.println("🧪 Testing account deletion email to: " + testEmail + " for user: " + testUsername);
            
            emailService.sendAccountDeletionEmail(testEmail, testUsername);
            
            response.put("success", true);
            response.put("message", "Account deletion test email sent successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Test deletion email error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Test deletion email failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Debug endpoint to check pending registrations
    @GetMapping("/debug-pending")
    public ResponseEntity<Map<String, Object>> debugPending() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<PendingRegistration> pending = pendingRegistrationRepository.findAll();
            System.out.println("Total pending registrations: " + pending.size());
            
            for (PendingRegistration p : pending) {
                System.out.println("Pending: " + p.getUsername() + ", Email: " + p.getEmail() + ", ID: " + p.getId());
            }
            
            response.put("success", true);
            response.put("message", "Pending registrations retrieved");
            response.put("count", pending.size());
            response.put("pending", pending.stream().map(p -> Map.of(
                "id", p.getId(),
                "username", p.getUsername(),
                "email", p.getEmail(),
                "expiresAt", p.getExpiresAt(),
                "consumed", p.isConsumed()
            )).collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Debug pending error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Debug error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Debug endpoint to test database connection
    @GetMapping("/debug-db")
    public ResponseEntity<Map<String, Object>> debugDatabase() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test user repository
            long userCount = userRepository.count();
            System.out.println("Total users in database: " + userCount);
            
            // Test pending registration repository
            long pendingCount = pendingRegistrationRepository.count();
            System.out.println("Total pending registrations: " + pendingCount);
            
            response.put("success", true);
            response.put("message", "Database connection working");
            response.put("userCount", userCount);
            response.put("pendingCount", pendingCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Database debug error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Database error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Debug endpoint to test email configuration
    @GetMapping("/debug-email-config")
    public ResponseEntity<Map<String, Object>> debugEmailConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test email service configuration
            String testEmail = "test@example.com";
            String testCode = "123456";
            
            System.out.println("Testing email service configuration...");
            emailService.sendVerificationCode(testEmail, testCode);
            
            response.put("success", true);
            response.put("message", "Email service configuration working");
            response.put("testEmail", testEmail);
            response.put("testCode", testCode);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Email config debug error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Email configuration error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Helper methods for email verification
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
} 