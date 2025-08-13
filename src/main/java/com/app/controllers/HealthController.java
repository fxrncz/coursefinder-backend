package com.app.controllers;

import com.app.models.User;
import com.app.repositories.UserRepository;
import com.app.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "http://localhost:3000")
public class HealthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> res = new HashMap<>();
        try {
            long userCount = userRepository.count();
            res.put("success", true);
            res.put("database", "up");
            res.put("userCount", userCount);
            res.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("database", "down");
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    @PostMapping("/db-write")
    public ResponseEntity<Map<String, Object>> databaseWriteTest() {
        Map<String, Object> res = new HashMap<>();
        Long createdId = null;
        try {
            String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String username = "healthcheck_" + unique;
            String email = "healthcheck+" + unique + "@local.test";
            String hashed = PasswordUtil.hashPassword("TempPass-123!" + unique);

            User user = new User(username, email, hashed);
            User saved = userRepository.save(user);
            createdId = saved.getId();

            // Clean up immediately after verifying write works
            userRepository.deleteById(createdId);

            res.put("success", true);
            res.put("database", "up");
            res.put("writeTest", "ok");
            res.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("database", "down");
            res.put("writeTest", "failed");
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}


