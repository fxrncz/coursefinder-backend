package com.app.controllers;

import com.app.models.User;
import com.app.repositories.PasswordResetRepository;
import com.app.repositories.UserRepository;
import com.app.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

            String hashed = PasswordUtil.hashPassword(newPassword);
            user.setPassword(hashed);
            userRepository.save(user);

            res.put("success", true);
            res.put("message", "Password updated successfully");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}


