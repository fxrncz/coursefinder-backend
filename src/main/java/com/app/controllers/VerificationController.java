package com.app.controllers;

import com.app.models.EmailVerification;
import com.app.models.User;
import com.app.models.PendingRegistration;
import com.app.repositories.EmailVerificationRepository;
import com.app.repositories.PasswordResetRepository;
import com.app.models.PasswordReset;
import com.app.repositories.UserRepository;
import com.app.repositories.PendingRegistrationRepository;
import com.app.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @PostMapping("/send-code")
    @Transactional
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email = request.get("email").toString();
            String purpose = request.getOrDefault("purpose", "register").toString();
            // If a real user exists already, we treat it as account verification or other flows.
            // For registration flow, a PendingRegistration must exist.
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
            Optional<User> userOpt = userRepository.findByEmail(email);
            Long userId = userOpt.map(User::getId).orElse(null);

            if (userId == null && pendingOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "No pending registration found for this email");
                return ResponseEntity.badRequest().body(res);
            }

            String code = generateCode();
            String codeHash = sha256(code);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

            if ("reset".equalsIgnoreCase(purpose)) {
                // Password reset flow via tokenized link
                if (userId == null) {
                    res.put("success", true); // Do not leak existence
                    res.put("message", "If the email exists, a reset link was sent");
                    return ResponseEntity.ok(res);
                }
                PasswordReset pr = new PasswordReset(userId, email, codeHash, expiresAt);
                passwordResetRepository.save(pr);
                String resetUrl = request.getOrDefault("resetBaseUrl", "http://localhost:3000/reset-password").toString()
                        + "?token=" + code + "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                // emailService.sendPasswordResetLink(email, resetUrl); // Temporarily disabled per request
            } else {
                // Registration flow
                EmailVerification ev = new EmailVerification(userId, email, codeHash, expiresAt);
                emailVerificationRepository.save(ev);
                pendingOpt.ifPresent(p -> {
                    p.setCodeHash(codeHash);
                    p.setExpiresAt(expiresAt);
                    pendingRegistrationRepository.save(p);
                });
                // emailService.sendVerificationCode(email, code); // Temporarily disabled per request
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
                pr.setConsumed(true);
                passwordResetRepository.save(pr);
                // Indicate that reset can proceed (frontend should now prompt for new password)
                res.put("success", true);
                res.put("message", "Code verified. You may reset your password.");
                return ResponseEntity.ok(res);
            }

            Optional<EmailVerification> evOpt = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email);
            if (evOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "No verification request found");
                return ResponseEntity.badRequest().body(res);
            }

            EmailVerification ev = evOpt.get();
            if (ev.isConsumed()) {
                res.put("success", false);
                res.put("message", "Code already used");
                return ResponseEntity.badRequest().body(res);
            }
            if (LocalDateTime.now().isAfter(ev.getExpiresAt())) {
                res.put("success", false);
                res.put("message", "Code expired");
                return ResponseEntity.badRequest().body(res);
            }
            if (!sha256(code).equals(ev.getCodeHash())) {
                ev.setAttempts(ev.getAttempts() + 1);
                emailVerificationRepository.save(ev);
                res.put("success", false);
                res.put("message", "Invalid code");
                return ResponseEntity.badRequest().body(res);
            }

            ev.setConsumed(true);
            emailVerificationRepository.save(ev);

            // If pending registration exists, create real user now and consume pending
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
            if (pendingOpt.isPresent()) {
                PendingRegistration p = pendingOpt.get();
                if (!p.isConsumed() && p.getCodeHash() != null && p.getCodeHash().equals(ev.getCodeHash())) {
                    User newUser = new User(p.getUsername(), p.getEmail(), p.getPasswordHash());
                    User savedUser = userRepository.save(newUser);
                    p.setConsumed(true);
                    pendingRegistrationRepository.save(p);
                    res.put("user", Map.of(
                            "id", savedUser.getId(),
                            "username", savedUser.getUsername(),
                            "email", savedUser.getEmail(),
                            "createdAt", savedUser.getCreatedAt()
                    ));
                }
            }

            res.put("success", true);
            res.put("message", "Email verified successfully");
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


