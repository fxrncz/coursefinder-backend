package com.app.services;

import com.app.repositories.PendingRegistrationRepository;
import com.app.repositories.PasswordResetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CleanupService {

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    // Run cleanup every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    @Transactional
    public void cleanupExpiredRegistrations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Delete expired registrations
            int expiredCount = pendingRegistrationRepository.findExpiredRegistrations(now).size();
            pendingRegistrationRepository.deleteExpiredRegistrations(now);
            
            // Delete consumed registrations (should be cleaned up immediately, but just in case)
            int consumedCount = pendingRegistrationRepository.findConsumedRegistrations().size();
            pendingRegistrationRepository.deleteConsumedRegistrations();
            
            if (expiredCount > 0 || consumedCount > 0) {
                System.out.println("Cleanup completed: " + expiredCount + " expired, " + consumedCount + " consumed registrations removed");
            }
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    // Run cleanup every 5 minutes for password resets
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    @Transactional
    public void cleanupExpiredPasswordResets() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Delete expired password resets
            int expiredCount = passwordResetRepository.deleteExpiredPasswordResets(now);
            
            // Delete consumed password resets
            int consumedCount = passwordResetRepository.deleteConsumedPasswordResets();
            
            if (expiredCount > 0 || consumedCount > 0) {
                System.out.println("Password reset cleanup completed: " + expiredCount + " expired, " + consumedCount + " consumed resets removed");
            }
        } catch (Exception e) {
            System.err.println("Error during password reset cleanup: " + e.getMessage());
        }
    }

    // Manual cleanup method that can be called on demand
    @Transactional
    public void manualCleanup() {
        cleanupExpiredRegistrations();
        cleanupExpiredPasswordResets();
    }
}
