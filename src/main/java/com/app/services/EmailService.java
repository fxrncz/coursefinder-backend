package com.app.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    // Toggle provider: sendgrid or resend
    @Value("${email.provider:sendgrid}")
    private String emailProvider;

    // SendGrid
    @Value("${sendgrid.api.key:}")
    private String sendgridApiKey;

    @Value("${sendgrid.from:}")
    private String sendgridFrom;

    private static final String SENDGRID_SEND_ENDPOINT = "https://api.sendgrid.com/v3/mail/send";

    // Resend (kept for later)
    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from:}") 
    private String resendFrom;

    // Frontend URL configuration
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationCode(String toEmail, String code) {
        // Check if email service is configured
        if (!isEmailConfigured()) {
            System.out.println("⚠️ Email service not configured. Skipping email send.");
            return;
        }
        
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendWithSendGrid(toEmail, code);
        } else {
            sendWithResend(toEmail, code);
        }
    }
    
    private boolean isEmailConfigured() {
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            return sendgridApiKey != null && !sendgridApiKey.trim().isEmpty();
        } else {
            return resendApiKey != null && !resendApiKey.trim().isEmpty();
        }
    }

    public void sendPasswordResetLink(String toEmail, String resetUrl) {
        // Check if email service is configured
        if (!isEmailConfigured()) {
            System.out.println("⚠️ Email service not configured. Skipping password reset email.");
            return;
        }
        
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendResetWithSendGrid(toEmail, resetUrl);
        } else {
            sendResetWithResend(toEmail, resetUrl);
        }
    }

    public void sendVerificationSuccessEmail(String toEmail, String username) {
        // Check if email service is configured
        if (!isEmailConfigured()) {
            System.out.println("⚠️ Email service not configured. Skipping verification success email.");
            return;
        }
        
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendSuccessWithSendGrid(toEmail, username);
        } else {
            sendSuccessWithResend(toEmail, username);
        }
    }

    public void sendAccountDeletionEmail(String toEmail, String username) {
        System.out.println("📧 EmailService: Starting account deletion email process");
        System.out.println("📧 EmailService: Provider = " + emailProvider);
        System.out.println("📧 EmailService: To = " + toEmail + ", Username = " + username);
        System.out.println("📧 EmailService: Frontend URL = " + frontendUrl);
        
        // Check if email service is configured
        if (!isEmailConfigured()) {
            System.out.println("⚠️ Email service not configured. Skipping account deletion email.");
            return;
        }
        
        try {
            if ("sendgrid".equalsIgnoreCase(emailProvider)) {
                System.out.println("📧 EmailService: Using SendGrid provider");
                sendDeletionWithSendGrid(toEmail, username);
            } else {
                System.out.println("📧 EmailService: Using Resend provider");
                sendDeletionWithResend(toEmail, username);
            }
            System.out.println("📧 EmailService: Account deletion email sent successfully");
        } catch (Exception e) {
            System.err.println("📧 EmailService: Error sending account deletion email: " + e.getMessage());
            throw e; // Re-throw to be caught by the controller
        }
    }

    /**
     * Generic method to send custom HTML emails
     * Used for test results reports and other automated emails
     */
    public void sendCustomEmail(String toEmail, String subject, String htmlContent) {
        // Check if email service is configured
        if (!isEmailConfigured()) {
            System.out.println("⚠️ Email service not configured. Skipping custom email.");
            return;
        }
        
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendCustomWithSendGrid(toEmail, subject, htmlContent);
        } else {
            sendCustomWithResend(toEmail, subject, htmlContent);
        }
    }

    private void sendWithSendGrid(String toEmail, String code) {
        if (sendgridApiKey == null || sendgridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        }
        if (sendgridFrom == null || sendgridFrom.isEmpty()) {
            throw new IllegalStateException("sendgrid.from is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("reply_to", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("personalizations", new Object[]{ Map.of(
                "to", new Object[]{ Map.of("email", toEmail) },
                "headers", Map.of(
                        "X-Priority", "1",
                        "Priority", "urgent",
                        "X-MSMail-Priority", "High"
                )
        ) });
        body.put("subject", "Your CourseFinder verification code");
        body.put("categories", new String[]{"transactional", "email_verification"});
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Verify your email</h2>" +
                "<p>Use the following code to complete your registration. It expires in 10 minutes.</p>" +
                "<div style='font-size:28px;letter-spacing:4px;font-weight:700;color:#A75F00;margin:16px 0'>" + code + "</div>" +
                "<p style='font-size:12px;color:#666'>If you didn't request this, you can safely ignore this email.</p>" +
                "</div>";
        String text = "Your verification code: " + code + "\nThis code expires in 10 minutes.";
        body.put("content", new Object[]{
                Map.of("type", "text/plain", "value", text),
                Map.of("type", "text/html", "value", html)
        });
        body.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", false),
                "open_tracking", Map.of("enable", false)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(SENDGRID_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendWithResend(String toEmail, String code) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        String fromAddress = (resendFrom != null && !resendFrom.isEmpty()) ? resendFrom : "onboarding@resend.dev";
        body.put("from", fromAddress);
        body.put("to", new String[]{ toEmail });
        body.put("subject", "Your CourseFinder verification code");
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Verify your email</h2>" +
                "<p>Use the following code to complete your registration. It expires in 10 minutes.</p>" +
                "<div style='font-size:28px;letter-spacing:4px;font-weight:700;color:#A75F00;margin:16px 0'>" + code + "</div>" +
                "<p style='font-size:12px;color:#666'>If you didn't request this, you can safely ignore this email.</p>" +
                "</div>";
        body.put("html", html);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendResetWithSendGrid(String toEmail, String resetUrl) {
        if (sendgridApiKey == null || sendgridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        }
        if (sendgridFrom == null || sendgridFrom.isEmpty()) {
            throw new IllegalStateException("sendgrid.from is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("personalizations", new Object[]{ Map.of(
                "to", new Object[]{ Map.of("email", toEmail) },
                "headers", Map.of(
                        "X-Priority", "1",
                        "Priority", "urgent",
                        "X-MSMail-Priority", "High"
                )
        ) });
        body.put("subject", "Reset your CourseFinder password");
        body.put("categories", new String[]{"transactional", "password_reset"});
        String safeUrl = resetUrl.replace("&", "&amp;");
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Reset Password</h2>" +
                "<p>Click the button below to set a new password. This link expires in 10 minutes.</p>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + safeUrl + "' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Set New Password</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>If the button is not clickable, copy and paste this URL into your browser:<br/><span>" + safeUrl + "</span></p>" +
                "<p style='font-size:12px;color:#666'>If you didn't request this, you can safely ignore this email.</p>" +
                "</div>";
        String text = "Reset your password: " + resetUrl + "\nThis link expires in 10 minutes.";
        body.put("content", new Object[]{
                Map.of("type", "text/plain", "value", text),
                Map.of("type", "text/html", "value", html)
        });
        body.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", false),
                "open_tracking", Map.of("enable", false)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(SENDGRID_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendResetWithResend(String toEmail, String resetUrl) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        String fromAddress = (resendFrom != null && !resendFrom.isEmpty()) ? resendFrom : "onboarding@resend.dev";
        body.put("from", fromAddress);
        body.put("to", new String[]{ toEmail });
        body.put("subject", "Reset your CourseFinder password");
        String safeUrl = resetUrl.replace("&", "&amp;");
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Reset Password</h2>" +
                "<p>Click the button below to set a new password. This link expires in 10 minutes.</p>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + safeUrl + "' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Set New Password</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>If the button is not clickable, copy and paste this URL into your browser:<br/><span>" + safeUrl + "</span></p>" +
                "<p style='font-size:12px;color:#666'>If you didn't request this, you can safely ignore this email.</p>" +
                "</div>";
        body.put("html", html);
        body.put("text", "Reset your password: " + resetUrl + "\nThis link expires in 10 minutes.");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private String extractEmail(String from) {
        int lt = from.indexOf('<');
        int gt = from.indexOf('>');
        if (lt >= 0 && gt > lt) {
            return from.substring(lt + 1, gt).trim();
        }
        return from.trim();
    }

    private String extractName(String from) {
        int lt = from.indexOf('<');
        if (lt > 0) {
            return from.substring(0, lt).trim();
        }
        return "";
    }

    private void sendSuccessWithSendGrid(String toEmail, String username) {
        if (sendgridApiKey == null || sendgridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        }
        if (sendgridFrom == null || sendgridFrom.isEmpty()) {
            throw new IllegalStateException("sendgrid.from is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("reply_to", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("personalizations", new Object[]{ Map.of(
                "to", new Object[]{ Map.of("email", toEmail) },
                "headers", Map.of(
                        "X-Priority", "1",
                        "Priority", "urgent",
                        "X-MSMail-Priority", "High"
                )
        ) });
        body.put("subject", "Welcome to CourseFinder - Account Verified!");
        body.put("categories", new String[]{"transactional", "account_verified"});
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>🎉 Welcome to CourseFinder!</h2>" +
                "<p>Hi " + username + ",</p>" +
                "<p>Your email has been successfully verified and your account is now ready to use!</p>" +
                "<p>You can now:</p>" +
                "<ul style='margin:16px 0;padding-left:20px'>" +
                "<li>Take personality and career assessments</li>" +
                "<li>Get personalized course recommendations</li>" +
                "<li>Explore career paths that match your interests</li>" +
                "</ul>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "/userpage' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Go to your account</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>Thank you for joining CourseFinder!</p>" +
                "</div>";
        String text = "Welcome to CourseFinder!\n\nHi " + username + ",\n\nYour email has been successfully verified and your account is now ready to use!\n\nYou can now take assessments, get course recommendations, and explore career paths.\n\nVisit: " + frontendUrl + "/userpage\n\nThank you for joining CourseFinder!";
        body.put("content", new Object[]{
                Map.of("type", "text/plain", "value", text),
                Map.of("type", "text/html", "value", html)
        });
        body.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", false),
                "open_tracking", Map.of("enable", false)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(SENDGRID_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendSuccessWithResend(String toEmail, String username) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        String fromAddress = (resendFrom != null && !resendFrom.isEmpty()) ? resendFrom : "onboarding@resend.dev";
        body.put("from", fromAddress);
        body.put("to", new String[]{ toEmail });
        body.put("subject", "Welcome to CourseFinder - Account Verified!");
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>🎉 Welcome to CourseFinder!</h2>" +
                "<p>Hi " + username + ",</p>" +
                "<p>Your email has been successfully verified and your account is now ready to use!</p>" +
                "<p>You can now:</p>" +
                "<ul style='margin:16px 0;padding-left:20px'>" +
                "<li>Take personality and career assessments</li>" +
                "<li>Get personalized course recommendations</li>" +
                "<li>Explore career paths that match your interests</li>" +
                "</ul>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "/userpage' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Go to your account</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>Thank you for joining CourseFinder!</p>" +
                "</div>";
        body.put("html", html);
        body.put("text", "Welcome to CourseFinder!\n\nHi " + username + ",\n\nYour email has been successfully verified and your account is now ready to use!\n\nYou can now take assessments, get course recommendations, and explore career paths.\n\nVisit: " + frontendUrl + "/userpage\n\nThank you for joining CourseFinder!");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendDeletionWithSendGrid(String toEmail, String username) {
        if (sendgridApiKey == null || sendgridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        }
        if (sendgridFrom == null || sendgridFrom.isEmpty()) {
            throw new IllegalStateException("sendgrid.from is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("reply_to", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("personalizations", new Object[]{ Map.of(
                "to", new Object[]{ Map.of("email", toEmail) },
                "headers", Map.of(
                        "X-Priority", "1",
                        "Priority", "urgent",
                        "X-MSMail-Priority", "High"
                )
        ) });
        body.put("subject", "Account Deleted - CourseFinder");
        body.put("categories", new String[]{"transactional", "account_deletion"});
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Account Successfully Deleted</h2>" +
                "<p>Hi " + username + ",</p>" +
                "<p>Your CourseFinder account has been successfully deleted as requested.</p>" +
                "<p>We're sorry to see you go! Here's what happened:</p>" +
                "<ul style='margin:16px 0;padding-left:20px'>" +
                "<li>Your account and all personal data have been permanently removed</li>" +
                "<li>All your test results and recommendations have been deleted</li>" +
                "<li>You can create a new account anytime in the future</li>" +
                "</ul>" +
                "<p>If you didn't request this deletion or have any questions, please contact our support team immediately.</p>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Visit CourseFinder</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>Thank you for using CourseFinder!</p>" +
                "</div>";
        String text = "Account Deleted - CourseFinder\n\nHi " + username + ",\n\nYour CourseFinder account has been successfully deleted as requested.\n\nWe're sorry to see you go! Your account and all personal data have been permanently removed.\n\nIf you didn't request this deletion or have any questions, please contact our support team immediately.\n\nVisit: " + frontendUrl + "\n\nThank you for using CourseFinder!";
        body.put("content", new Object[]{
                Map.of("type", "text/plain", "value", text),
                Map.of("type", "text/html", "value", html)
        });
        body.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", false),
                "open_tracking", Map.of("enable", false)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(SENDGRID_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    private void sendDeletionWithResend(String toEmail, String username) {
        System.out.println("📧 Resend: Starting account deletion email");
        
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            System.err.println("📧 Resend: API key is null or empty");
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }
        
        System.out.println("📧 Resend: API key configured, length: " + resendApiKey.length());
        System.out.println("📧 Resend: API key starts with: " + resendApiKey.substring(0, Math.min(10, resendApiKey.length())) + "...");

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        String fromAddress = (resendFrom != null && !resendFrom.isEmpty()) ? resendFrom : "onboarding@resend.dev";
        System.out.println("📧 Resend: From address = " + fromAddress);
        System.out.println("📧 Resend: To address = " + toEmail);
        
        body.put("from", fromAddress);
        body.put("to", new String[]{ toEmail });
        body.put("subject", "Account Deleted - CourseFinder");
        String html = "<div style='font-family:Arial,sans-serif;font-size:16px;color:#333'>" +
                "<h2 style='color:#A75F00;margin-bottom:8px'>Account Successfully Deleted</h2>" +
                "<p>Hi " + username + ",</p>" +
                "<p>Your CourseFinder account has been successfully deleted as requested.</p>" +
                "<p>We're sorry to see you go! Here's what happened:</p>" +
                "<ul style='margin:16px 0;padding-left:20px'>" +
                "<li>Your account and all personal data have been permanently removed</li>" +
                "<li>All your test results and recommendations have been deleted</li>" +
                "<li>You can create a new account anytime in the future</li>" +
                "</ul>" +
                "<p>If you didn't request this deletion or have any questions, please contact our support team immediately.</p>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Visit CourseFinder</a>" +
                "</p>" +
                "<p style='font-size:12px;color:#666'>Thank you for using CourseFinder!</p>" +
                "</div>";
        
        System.out.println("📧 Resend: HTML template length: " + html.length());
        body.put("html", html);
        body.put("text", "Account Deleted - CourseFinder\n\nHi " + username + ",\n\nYour CourseFinder account has been successfully deleted as requested.\n\nWe're sorry to see you go! Your account and all personal data have been permanently removed.\n\nIf you didn't request this deletion or have any questions, please contact our support team immediately.\n\nVisit: " + frontendUrl + "\n\nThank you for using CourseFinder!");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        System.out.println("📧 Resend: Sending HTTP request to Resend API");
        System.out.println("📧 Resend: Request body size: " + body.size() + " fields");
        
        try {
            var response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
            System.out.println("📧 Resend: Response status: " + response.getStatusCode());
            System.out.println("📧 Resend: Response body: " + response.getBody());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("📧 Resend: Request failed with status: " + response.getStatusCode().value());
                throw new RuntimeException("Resend send failed: " + response.getStatusCode().value() + " - " + response.getBody());
            }
            
            System.out.println("📧 Resend: Email sent successfully!");
        } catch (Exception e) {
            System.err.println("📧 Resend: Exception during HTTP request: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Send custom HTML email via SendGrid
     */
    private void sendCustomWithSendGrid(String toEmail, String subject, String htmlContent) {
        if (sendgridApiKey == null || sendgridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        }
        if (sendgridFrom == null || sendgridFrom.isEmpty()) {
            throw new IllegalStateException("sendgrid.from is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("reply_to", Map.of("email", extractEmail(sendgridFrom), "name", extractName(sendgridFrom)));
        body.put("personalizations", new Object[]{ Map.of(
                "to", new Object[]{ Map.of("email", toEmail) }
        ) });
        body.put("subject", subject);
        body.put("categories", new String[]{"automated", "test_results"});
        
        // Generate plain text version from HTML (simple approach)
        String plainText = htmlContent.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        
        body.put("content", new Object[]{
                Map.of("type", "text/plain", "value", plainText),
                Map.of("type", "text/html", "value", htmlContent)
        });
        body.put("tracking_settings", Map.of(
                "click_tracking", Map.of("enable", true),
                "open_tracking", Map.of("enable", true)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(SENDGRID_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("SendGrid send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }

    /**
     * Send custom HTML email via Resend
     */
    private void sendCustomWithResend(String toEmail, String subject, String htmlContent) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        String fromAddress = (resendFrom != null && !resendFrom.isEmpty()) ? resendFrom : "onboarding@resend.dev";
        body.put("from", fromAddress);
        body.put("to", new String[]{ toEmail });
        body.put("subject", subject);
        body.put("html", htmlContent);
        
        // Generate plain text version
        String plainText = htmlContent.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        body.put("text", plainText);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend send failed: " + response.getStatusCode().value() + " - " + response.getBody());
        }
    }
}


