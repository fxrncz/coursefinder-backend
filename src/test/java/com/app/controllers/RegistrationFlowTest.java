package com.app.controllers;

import com.app.models.PendingRegistration;
import com.app.repositories.PendingRegistrationRepository;
import com.app.repositories.UserRepository;
import com.app.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RegistrationFlowTest {

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testExpiredRegistrationCleanup() {
        // Create an expired pending registration
        PendingRegistration expiredPending = new PendingRegistration(
            "testuser",
            "test@example.com",
            "hashedpassword",
            "codehash",
            LocalDateTime.now().minusMinutes(10) // Expired 10 minutes ago
        );
        
        pendingRegistrationRepository.save(expiredPending);
        
        // Verify it's expired
        assertTrue(expiredPending.isExpired());
        
        // Clean up expired registrations
        pendingRegistrationRepository.deleteExpiredRegistrations(LocalDateTime.now());
        
        // Verify it's been deleted
        assertFalse(pendingRegistrationRepository.findByEmail("test@example.com").isPresent());
    }

    @Test
    public void testActiveRegistrationNotDeleted() {
        // Create an active (non-expired) pending registration
        PendingRegistration activePending = new PendingRegistration(
            "testuser2",
            "test2@example.com",
            "hashedpassword",
            "codehash",
            LocalDateTime.now().plusMinutes(5) // Expires in 5 minutes
        );
        
        pendingRegistrationRepository.save(activePending);
        
        // Verify it's not expired
        assertFalse(activePending.isExpired());
        
        // Clean up expired registrations
        pendingRegistrationRepository.deleteExpiredRegistrations(LocalDateTime.now());
        
        // Verify it's still there
        assertTrue(pendingRegistrationRepository.findByEmail("test2@example.com").isPresent());
    }
}
