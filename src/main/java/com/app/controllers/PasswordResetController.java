package com.app.controllers;

import com.app.models.User;
import com.app.models.PasswordReset;
import com.app.repositories.UserRepository;
import com.app.repositories.PasswordResetRepository;
import com.app.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email = request.get("email").toString();
            String newPassword = request.get("newPassword").toString();
            String token = request.get("token").toString();

            // First verify the reset token
            Optional<PasswordReset> prOpt = passwordResetRepository.findActiveByEmail(email, LocalDateTime.now());
            if (prOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "No valid reset request found");
                return ResponseEntity.badRequest().body(res);
            }

            PasswordReset pr = prOpt.get();
            
            // Check if reset can be attempted
            if (!pr.canAttemptReset()) {
                if (pr.isConsumed()) {
                    res.put("success", false);
                    res.put("message", "Reset link already used");
                } else if (pr.isExpired()) {
                    res.put("success", false);
                    res.put("message", "Reset link expired");
                } else {
                    res.put("success", false);
                    res.put("message", "Too many failed attempts");
                }
                return ResponseEntity.badRequest().body(res);
            }
            
            // Verify the token
            if (!sha256(token).equals(pr.getCodeHash())) {
                pr.incrementAttempts();
                passwordResetRepository.save(pr);
                res.put("success", false);
                res.put("message", "Invalid reset link");
                return ResponseEntity.badRequest().body(res);
            }

            // Token is valid, now update the password
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "User not found");
                return ResponseEntity.badRequest().body(res);
            }

            User user = userOpt.get();
            // Prevent same password reuse (handles hashed and legacy plaintext)
            boolean sameAsCurrent;
            String stored = user.getPassword();
            boolean storedIsHashed = stored != null && stored.contains(":");
            if (storedIsHashed) {
                try {
                    sameAsCurrent = PasswordUtil.verifyPassword(newPassword, stored);
                } catch (Exception ex) {
                    sameAsCurrent = false;
                }
            } else {
                sameAsCurrent = newPassword.equals(stored);
            }
            if (sameAsCurrent) {
                res.put("success", false);
                res.put("message", "Use different password");
                return ResponseEntity.badRequest().body(res);
            }

            // Update password and consume the reset token
            String hashed = PasswordUtil.hashPassword(newPassword);
            user.setPassword(hashed);
            userRepository.save(user);
            
            // Mark the reset token as consumed and delete it
            pr.markAsConsumed();
            passwordResetRepository.save(pr);
            passwordResetRepository.delete(pr); // Remove consumed reset from database

            res.put("success", true);
            res.put("message", "Password updated successfully");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
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


