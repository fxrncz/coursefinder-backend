package com.app.controllers;

import com.app.models.User;
import com.app.models.PendingRegistration;
import com.app.repositories.PasswordResetRepository;
import com.app.models.PasswordReset;
import com.app.repositories.UserRepository;
import com.app.repositories.PendingRegistrationRepository;
import com.app.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/verify")
@CrossOrigin(origins = "http://localhost:3000")
public class VerificationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    // Frontend URL configuration
    @Value("${frontend.url:https://coursefinder-sti.vercel.app}")
    private String frontendUrl;

    @PostMapping("/send-code")
    @Transactional
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email = request.get("email").toString();
            String purpose = request.getOrDefault("purpose", "register").toString();
            
            // Clean up expired registrations first
            pendingRegistrationRepository.deleteExpiredRegistrations(LocalDateTime.now());
            
            // For registration flow, a PendingRegistration must exist
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
            Optional<User> userOpt = userRepository.findByEmail(email);
            Long userId = userOpt.map(User::getId).orElse(null);

            // Check if there's an expired pending registration and clean it up
            if (pendingOpt.isPresent() && pendingOpt.get().isExpired()) {
                pendingRegistrationRepository.delete(pendingOpt.get());
                System.out.println("Deleted expired pending registration for: " + email + " during send-code");
                pendingOpt = Optional.empty(); // Clear the optional
            }

            if (userId == null && pendingOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "No pending registration found for this email");
                return ResponseEntity.badRequest().body(res);
            }

            String code = generateCode();
            String codeHash = sha256(code);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minutes expiration

            if ("reset".equalsIgnoreCase(purpose)) {
                // Password reset flow via tokenized link
                if (userId == null) {
                    res.put("success", true); // Do not leak existence
                    res.put("message", "If the email exists, a reset link was sent");
                    return ResponseEntity.ok(res);
                }
                
                // Clean up expired password resets first
                passwordResetRepository.deleteExpiredPasswordResets(LocalDateTime.now());
                
                // Check for existing active password reset
                Optional<PasswordReset> existingReset = passwordResetRepository.findActiveByEmail(email, LocalDateTime.now());
                
                PasswordReset pr;
                if (existingReset.isPresent()) {
                    // Update existing password reset with new code
                    pr = existingReset.get();
                    pr.setCodeHash(codeHash);
                    pr.setExpiresAt(expiresAt);
                    pr.setAttempts(0); // Reset attempts for new code
                    System.out.println("🔄 Updated existing password reset with new code for: " + email);
                } else {
                    // Create new password reset
                    pr = new PasswordReset(userId, email, codeHash, expiresAt);
                    System.out.println("🆕 Created new password reset for: " + email);
                }
                passwordResetRepository.save(pr);
                
                // Generate reset link
                String resetBaseUrl = request.getOrDefault("resetBaseUrl", frontendUrl + "/reset-password").toString();
                String resetLink = resetBaseUrl + "?email=" + email + "&token=" + code;
                
                System.out.println("🔗 Password Reset: Frontend URL = " + frontendUrl);
                System.out.println("🔗 Password Reset: Reset Base URL = " + resetBaseUrl);
                System.out.println("🔗 Password Reset: Generated Reset Link = " + resetLink);
                
                // Send reset link email
                try {
                    emailService.sendPasswordResetLink(email, resetLink);
                    System.out.println("Password reset link sent to: " + email);
                } catch (Exception emailError) {
                    System.out.println("Failed to send reset link: " + emailError.getMessage());
                    // Don't fail the request if email fails
                }
            } else {
                // Registration flow - update existing pending registration with new code
                if (pendingOpt.isPresent()) {
                    PendingRegistration pending = pendingOpt.get();
                    pending.setCodeHash(codeHash);
                    pending.setExpiresAt(expiresAt);
                    pending.setAttempts(0); // Reset attempts for new code
                    pendingRegistrationRepository.save(pending);
                    System.out.println("Updated pending registration with new code for: " + email);
                }
                emailService.sendVerificationCode(email, code);
            }

            res.put("success", true);
            res.put("message", "Verification code sent");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email = request.get("email").toString();
            String code = request.get("code").toString();
            String purpose = request.getOrDefault("purpose", "register").toString();

            if ("reset".equalsIgnoreCase(purpose)) {
                Optional<PasswordReset> prOpt = passwordResetRepository.findTopByEmailOrderByCreatedAtDesc(email);
            if (prOpt.isEmpty()) {
                    res.put("success", false);
                    res.put("message", "No reset request found");
                    return ResponseEntity.badRequest().body(res);
                }
                PasswordReset pr = prOpt.get();
                if (pr.isConsumed()) {
                    res.put("success", false);
                    res.put("message", "Code already used");
                    return ResponseEntity.badRequest().body(res);
                }
                if (LocalDateTime.now().isAfter(pr.getExpiresAt())) {
                    res.put("success", false);
                    res.put("message", "Code expired");
                    return ResponseEntity.badRequest().body(res);
                }
                if (!sha256(code).equals(pr.getCodeHash())) {
                    pr.setAttempts(pr.getAttempts() + 1);
                    passwordResetRepository.save(pr);
                    res.put("success", false);
                    res.put("message", "Invalid code");
                    return ResponseEntity.badRequest().body(res);
                }
                // Don't consume the token yet - let the password reset endpoint handle it
                // Just verify the token is valid and not expired
                res.put("success", true);
                res.put("message", "Code verified. You may reset your password.");
                return ResponseEntity.ok(res);
            }

            // Clean up expired registrations first
            pendingRegistrationRepository.deleteExpiredRegistrations(LocalDateTime.now());

            // Find the pending registration
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
            if (pendingOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "No pending registration found for this email");
                return ResponseEntity.badRequest().body(res);
            }

            PendingRegistration pending = pendingOpt.get();
            
            // Check if registration can be verified
            if (!pending.canAttemptVerification()) {
                if (pending.isConsumed()) {
                    res.put("success", false);
                    res.put("message", "Registration already completed");
                } else if (pending.isExpired()) {
                    res.put("success", false);
                    res.put("message", "Verification code expired. Please request a new one.");
                } else {
                    res.put("success", false);
                    res.put("message", "Too many attempts. Please request a new code");
                }
                return ResponseEntity.badRequest().body(res);
            }

            // Verify the code
            if (!sha256(code).equals(pending.getCodeHash())) {
                pending.incrementAttempts();
                pendingRegistrationRepository.save(pending);
                res.put("success", false);
                res.put("message", "Invalid code");
                return ResponseEntity.badRequest().body(res);
            }

            // Create user account
            User newUser = new User(pending.getUsername(), pending.getEmail(), pending.getPasswordHash());
            User savedUser = userRepository.save(newUser);
            
            // Mark pending as consumed and delete it
            pending.markAsConsumed();
            pendingRegistrationRepository.save(pending);
            pendingRegistrationRepository.delete(pending); // Remove from database
            
            // Prepare response data
            Map<String, Object> userData = Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "createdAt", savedUser.getCreatedAt()
            );
            res.put("user", userData);
            
            // Send success email asynchronously (don't wait for it)
            try {
                emailService.sendVerificationSuccessEmail(email, savedUser.getUsername());
                System.out.println("Success email sent to: " + email);
            } catch (Exception emailError) {
                System.out.println("Failed to send success email: " + emailError.getMessage());
                // Don't fail verification if success email fails
            }

            res.put("success", true);
            res.put("message", "Email verified successfully! Welcome to CourseFinder!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    private String generateCode() {
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


