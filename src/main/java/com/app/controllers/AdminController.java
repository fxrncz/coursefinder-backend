package com.app.controllers;

import com.app.models.Admin;
import com.app.repositories.AdminRepository;
import com.app.security.PasswordUtil;
import com.app.services.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "https://coursefinder-sti.vercel.app"})
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private AdminDashboardService dashboardService;

    /**
     * Admin login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");
            
            System.out.println("Admin login attempt for email: " + email);
            
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
            
            // Find admin by email
            Optional<Admin> adminOptional = adminRepository.findByEmail(email);
            
            if (adminOptional.isEmpty()) {
                System.out.println("Admin not found for email: " + email);
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response);
            }
            
            Admin admin = adminOptional.get();
            
            // Check if admin is active
            if (!admin.getIsActive()) {
                System.out.println("Admin account is inactive: " + email);
                response.put("success", false);
                response.put("message", "This admin account has been deactivated");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("Admin found: " + admin.getUsername() + ", ID: " + admin.getId());
            
            // Verify password (supports both hashed and legacy plaintext)
            boolean passwordOk;
            if (admin.getPassword() != null && admin.getPassword().contains(":")) {
                // Hashed password
                passwordOk = PasswordUtil.verifyPassword(password, admin.getPassword());
            } else {
                // Legacy plaintext password - compare directly and upgrade to hashed
                passwordOk = password.equals(admin.getPassword());
                if (passwordOk) {
                    String upgradedHash = PasswordUtil.hashPassword(password);
                    admin.setPassword(upgradedHash);
                    adminRepository.save(admin);
                    System.out.println("Upgraded admin password to hashed format");
                }
            }

            if (!passwordOk) {
                System.out.println("Password mismatch for admin: " + admin.getUsername());
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("Admin login successful: " + admin.getUsername());
            response.put("success", true);
            response.put("message", "Admin login successful");
            
            // Create admin response map (exclude password)
            Map<String, Object> adminResponse = new HashMap<>();
            adminResponse.put("id", admin.getId());
            adminResponse.put("username", admin.getUsername());
            adminResponse.put("email", admin.getEmail());
            adminResponse.put("fullName", admin.getFullName());
            
            response.put("admin", adminResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Admin login error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Check if admin is logged in (verify admin data)
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAdmin(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long adminId = Long.parseLong(request.get("id").toString());
            
            Optional<Admin> adminOptional = adminRepository.findById(adminId);
            
            if (adminOptional.isEmpty() || !adminOptional.get().getIsActive()) {
                response.put("success", false);
                response.put("message", "Admin not found or inactive");
                return ResponseEntity.badRequest().body(response);
            }
            
            Admin admin = adminOptional.get();
            
            Map<String, Object> adminResponse = new HashMap<>();
            adminResponse.put("id", admin.getId());
            adminResponse.put("username", admin.getUsername());
            adminResponse.put("email", admin.getEmail());
            adminResponse.put("fullName", admin.getFullName());
            
            response.put("success", true);
            response.put("admin", adminResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Verification failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Admin logout endpoint (for future cleanup if needed)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // For now, just return success
            // Frontend will clear localStorage
            response.put("success", true);
            response.put("message", "Logout successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get admin profile
     */
    @GetMapping("/profile/{adminId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long adminId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Admin> adminOptional = adminRepository.findById(adminId);
            
            if (adminOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Admin admin = adminOptional.get();
            
            Map<String, Object> adminResponse = new HashMap<>();
            adminResponse.put("id", admin.getId());
            adminResponse.put("username", admin.getUsername());
            adminResponse.put("email", admin.getEmail());
            adminResponse.put("fullName", admin.getFullName());
            
            response.put("success", true);
            response.put("admin", adminResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get dashboard statistics and analytics
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> statistics = dashboardService.getDashboardStatistics();
            
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Dashboard statistics error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Failed to retrieve dashboard statistics: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

