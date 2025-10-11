package com.app.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AccountDeletionEmailTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testAccountDeletionEmailMethodExists() {
        // Test that the method exists and can be called
        assertDoesNotThrow(() -> {
            // This would normally send an email, but we're just testing the method exists
            // In a real test environment, you'd mock the email service
            emailService.getClass().getMethod("sendAccountDeletionEmail", String.class, String.class);
        });
    }

    @Test
    public void testAccountDeletionEmailParameters() {
        // Test that the method signature is correct
        try {
            var method = emailService.getClass().getMethod("sendAccountDeletionEmail", String.class, String.class);
            assertEquals(2, method.getParameterCount());
            assertEquals(String.class, method.getParameterTypes()[0]); // email
            assertEquals(String.class, method.getParameterTypes()[1]); // username
        } catch (NoSuchMethodException e) {
            fail("sendAccountDeletionEmail method not found");
        }
    }
}
