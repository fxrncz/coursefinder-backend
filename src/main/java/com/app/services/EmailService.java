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
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendWithSendGrid(toEmail, code);
        } else {
            sendWithResend(toEmail, code);
        }
    }

    public void sendPasswordResetLink(String toEmail, String resetUrl) {
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendResetWithSendGrid(toEmail, resetUrl);
        } else {
            sendResetWithResend(toEmail, resetUrl);
        }
    }

    public void sendVerificationSuccessEmail(String toEmail, String username) {
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendSuccessWithSendGrid(toEmail, username);
        } else {
            sendSuccessWithResend(toEmail, username);
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
                "<li>Access your personalized dashboard</li>" +
                "</ul>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "/userpage' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Go to Dashboard</a>" +
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
                "<li>Access your personalized dashboard</li>" +
                "</ul>" +
                "<p style='margin:16px 0'>" +
                "<a href='" + frontendUrl + "/userpage' style='display:inline-block;background:#A75F00;color:#fff;padding:10px 16px;border-radius:6px;text-decoration:none;font-weight:600'>Go to Dashboard</a>" +
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
}


