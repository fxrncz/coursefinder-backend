package com.app.controllers;

import com.app.dto.EnhancedTestResultDTO;
import com.app.services.AiContentValidationService;
import com.app.services.TestResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-validation")
@CrossOrigin(origins = "http://localhost:3000")
public class AiValidationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiValidationController.class);
    
    @Autowired
    private AiContentValidationService validationService;
    
    @Autowired
    private TestResultService testResultService;
    
    @Autowired
    private com.app.services.ModelTestingService modelTestingService;
    
    /**
     * Validate test result by session ID
     */
    @PostMapping("/validate/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> validateTestResultBySession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🤖 Starting AI validation for session: {}", sessionId);
            
            UUID sessionUUID = UUID.fromString(sessionId);
            
            // Get enhanced test result
            Optional<EnhancedTestResultDTO> enhancedResultOpt = 
                testResultService.getEnhancedResultBySessionId(sessionUUID);
            
            if (!enhancedResultOpt.isPresent()) {
                response.put("status", "NOT_FOUND");
                response.put("message", "Test result not found for session ID: " + sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            EnhancedTestResultDTO enhancedResult = enhancedResultOpt.get();
            
                            // Perform AI validation
            AiContentValidationService.ValidationReport validationReport = 
                validationService.validateTestResult(enhancedResult);
            
            response.put("status", "SUCCESS");
            response.put("validationReport", validationReport);
            response.put("sessionId", sessionId);
            
            logger.info("✅ AI validation completed for session: {}. Score: {}", 
                sessionId, validationReport.getValidationScore());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid session ID format: {}", sessionId);
            response.put("status", "BAD_REQUEST");
            response.put("message", "Invalid session ID format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            logger.error("❌ Error during AI validation for session {}: {}", sessionId, e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Validate course recommendations
     */
    @PostMapping("/validate/courses")
    public ResponseEntity<Map<String, Object>> validateCourseRecommendations(
            @RequestBody CourseValidationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔍 Validating course recommendations: {}", request.getCoursePath());
            
            AiContentValidationService.ValidationResult result = 
                validationService.validateCourseRecommendations(request.getCoursePath());
            
            response.put("status", "SUCCESS");
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("confidence", result.getConfidence());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error validating course recommendations: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Validate career suggestions
     */
    @PostMapping("/validate/careers")
    public ResponseEntity<Map<String, Object>> validateCareerSuggestions(
            @RequestBody CareerValidationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔍 Validating career suggestions: {}", request.getCareerSuggestions());
            
            AiContentValidationService.ValidationResult result = 
                validationService.validateCareerSuggestions(request.getCareerSuggestions());
            
            response.put("status", "SUCCESS");
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("confidence", result.getConfidence());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error validating career suggestions: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Validate personality mapping
     */
    @PostMapping("/validate/personality")
    public ResponseEntity<Map<String, Object>> validatePersonalityMapping(
            @RequestBody PersonalityValidationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔍 Validating personality mapping: MBTI={}, RIASEC={}", 
                request.getMbtiType(), request.getRiasecCode());
            
            AiContentValidationService.ValidationResult result = 
                validationService.validatePersonalityMapping(
                    request.getMbtiType(),
                    request.getRiasecCode(),
                    request.getCourseSuggestions(),
                    request.getCareerSuggestions()
                );
            
            response.put("status", "SUCCESS");
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("confidence", result.getConfidence());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error validating personality mapping: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get validation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getValidationStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // This would typically come from a database or analytics service
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalValidations", 0);
            stats.put("successfulValidations", 0);
            stats.put("failedValidations", 0);
            stats.put("averageConfidence", 0.0);
            
            response.put("status", "SUCCESS");
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error getting validation stats: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Failed to get validation stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test current model configuration
     */
    @GetMapping("/test/configuration")
    public ResponseEntity<Map<String, Object>> testCurrentConfiguration() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🧪 Testing current AI model configuration");
            
            com.app.services.ModelTestingService.ConfigurationTestResult result = 
                modelTestingService.testCurrentConfiguration();
            
            response.put("status", "SUCCESS");
            response.put("configurationTest", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error testing configuration: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Configuration test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Compare different models
     */
    @PostMapping("/test/compare-models")
    public ResponseEntity<Map<String, Object>> compareModels() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🧪 Starting model comparison test");
            
            com.app.services.ModelTestingService.ModelComparisonResult result = 
                modelTestingService.compareModels();
            
            response.put("status", "SUCCESS");
            response.put("comparisonResult", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error comparing models: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Model comparison failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Simple AI test - validates sample content
     */
    @GetMapping("/test/simple")
    public ResponseEntity<Map<String, Object>> simpleAiTest() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🧪 Running simple AI validation test");
            
            // Test course validation
            AiContentValidationService.ValidationResult courseResult = 
                validationService.validateCourseRecommendations(
                    "BS Computer Science: A program that teaches students how to use Microsoft Word and Excel"
                );
            
            // Test career validation
            AiContentValidationService.ValidationResult careerResult = 
                validationService.validateCareerSuggestions(
                    "Software Engineer: A person who fixes computers and installs software"
                );
            
            Map<String, Object> testResults = new HashMap<>();
            testResults.put("courseValidation", Map.of(
                "valid", courseResult.isValid(),
                "message", courseResult.getMessage(),
                "confidence", courseResult.getConfidence()
            ));
            
            testResults.put("careerValidation", Map.of(
                "valid", careerResult.isValid(),
                "message", careerResult.getMessage(),
                "confidence", careerResult.getConfidence()
            ));
            
            response.put("status", "SUCCESS");
            response.put("message", "AI validation is working!");
            response.put("testResults", testResults);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Simple AI test failed: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "AI test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Request DTOs
     */
    public static class CourseValidationRequest {
        private String coursePath;
        
        public String getCoursePath() { return coursePath; }
        public void setCoursePath(String coursePath) { this.coursePath = coursePath; }
    }
    
    public static class CareerValidationRequest {
        private String careerSuggestions;
        
        public String getCareerSuggestions() { return careerSuggestions; }
        public void setCareerSuggestions(String careerSuggestions) { this.careerSuggestions = careerSuggestions; }
    }
    
    public static class PersonalityValidationRequest {
        private String mbtiType;
        private String riasecCode;
        private String courseSuggestions;
        private String careerSuggestions;
        
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public String getCourseSuggestions() { return courseSuggestions; }
        public void setCourseSuggestions(String courseSuggestions) { this.courseSuggestions = courseSuggestions; }
        
        public String getCareerSuggestions() { return careerSuggestions; }
        public void setCareerSuggestions(String careerSuggestions) { this.careerSuggestions = careerSuggestions; }
    }
}
