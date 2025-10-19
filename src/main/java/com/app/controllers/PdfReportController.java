package com.app.controllers;

import com.app.dto.DetailedScoringDTO;
import com.app.dto.EnhancedTestResultDTO;
import com.app.models.User;
import com.app.repositories.UserRepository;
import com.app.services.PdfReportService;
import com.app.services.TestResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf-report")
@CrossOrigin(origins = "http://localhost:3000")
public class PdfReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfReportController.class);
    
    @Autowired
    private PdfReportService pdfReportService;
    
    @Autowired
    private TestResultService testResultService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Download PDF report for a specific session
     */
    @GetMapping("/download/{sessionId}")
    public ResponseEntity<?> downloadPdfReport(@PathVariable String sessionId) {
        try {
            logger.info("📄 Generating PDF report for session: {}", sessionId);
            
            UUID sessionUUID = UUID.fromString(sessionId);
            
            // Get enhanced test result
            Optional<EnhancedTestResultDTO> enhancedResultOpt = 
                testResultService.getEnhancedResultBySessionId(sessionUUID);
            
            if (!enhancedResultOpt.isPresent()) {
                logger.warn("⚠️ No enhanced result found for session: {}", sessionId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Test result not found");
                error.put("message", "No test result found for the provided session ID");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            EnhancedTestResultDTO enhancedResult = enhancedResultOpt.get();
            
            // Get user information
            User user = null;
            if (enhancedResult.getUserId() != null) {
                user = userRepository.findById(enhancedResult.getUserId()).orElse(null);
            }
            
            if (user == null) {
                logger.warn("⚠️ User not found for session: {}", sessionId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                error.put("message", "No user found for this test result");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Get detailed scoring data
            DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
            
            // Generate PDF
            byte[] pdfBytes = pdfReportService.generatePdfReport(user, enhancedResult, scoringData);
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                String.format("personality-test-results-%s-%s.pdf", 
                    enhancedResult.getMbtiType(), enhancedResult.getRiasecCode()));
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            logger.info("✅ Successfully generated PDF report for user: {} ({} bytes)", 
                user.getUsername(), pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid session ID format: {}", sessionId);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid session ID");
            error.put("message", "The provided session ID is not valid");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            
        } catch (Exception e) {
            logger.error("❌ Failed to generate PDF report for session {}: {}", sessionId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "PDF generation failed");
            error.put("message", "Failed to generate PDF report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Download PDF report for a specific user's latest test result
     */
    @GetMapping("/download/user/{userId}")
    public ResponseEntity<?> downloadPdfReportForUser(@PathVariable Long userId) {
        try {
            logger.info("📄 Generating PDF report for user ID: {}", userId);
            
            // Get user
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                logger.warn("⚠️ User not found: {}", userId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                error.put("message", "No user found with the provided ID");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            User user = userOpt.get();
            
            // Get latest test result for user
            Optional<TestResultService.TestResultDTO> latestResultOpt = 
                testResultService.getLatestResultForUser(userId);
            
            if (!latestResultOpt.isPresent()) {
                logger.warn("⚠️ No test result found for user: {}", userId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "No test result found");
                error.put("message", "No personality test results found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            TestResultService.TestResultDTO latestResult = latestResultOpt.get();
            
            // Get enhanced test result
            Optional<EnhancedTestResultDTO> enhancedResultOpt = 
                testResultService.getEnhancedResultBySessionId(latestResult.getSessionId());
            
            if (!enhancedResultOpt.isPresent()) {
                logger.warn("⚠️ No enhanced result found for user: {}", userId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Enhanced result not found");
                error.put("message", "No enhanced test result found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            EnhancedTestResultDTO enhancedResult = enhancedResultOpt.get();
            
            // Get detailed scoring data
            DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(latestResult.getSessionId().toString());
            
            // Generate PDF
            byte[] pdfBytes = pdfReportService.generatePdfReport(user, enhancedResult, scoringData);
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                String.format("personality-test-results-%s-%s.pdf", 
                    enhancedResult.getMbtiType(), enhancedResult.getRiasecCode()));
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            logger.info("✅ Successfully generated PDF report for user: {} ({} bytes)", 
                user.getUsername(), pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("❌ Failed to generate PDF report for user {}: {}", userId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "PDF generation failed");
            error.put("message", "Failed to generate PDF report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
