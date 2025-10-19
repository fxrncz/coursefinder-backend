package com.app.services;

import com.app.dto.EnhancedTestResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiContentValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(AiContentValidationService.class);
    
    @Autowired
    private HuggingFaceApiService huggingFaceService;
    
    /**
     * Validate complete test result data
     */
    public ValidationReport validateTestResult(EnhancedTestResultDTO testResult) {
        log.info("🔍 Starting AI validation for test result: {}", testResult.getId());
        
        ValidationReport report = new ValidationReport();
        report.setTestResultId(testResult.getId());
        report.setTimestamp(java.time.LocalDateTime.now().toString());
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            // Validate course recommendations
            if (testResult.getCoursePath() != null && !testResult.getCoursePath().trim().isEmpty()) {
                ValidationResult courseValidation = validateCourseRecommendations(testResult.getCoursePath());
                if (!courseValidation.isValid()) {
                    issues.add(new ValidationIssue(
                        "COURSE_VALIDATION", 
                        "Course recommendations validation failed", 
                        courseValidation.getMessage(),
                        courseValidation.getConfidence()
                    ));
                }
            }
            
            // Validate career suggestions
            if (testResult.getCareerSuggestions() != null && !testResult.getCareerSuggestions().trim().isEmpty()) {
                ValidationResult careerValidation = validateCareerSuggestions(testResult.getCareerSuggestions());
                if (!careerValidation.isValid()) {
                    issues.add(new ValidationIssue(
                        "CAREER_VALIDATION", 
                        "Career suggestions validation failed", 
                        careerValidation.getMessage(),
                        careerValidation.getConfidence()
                    ));
                }
            }
            
            // Validate MBTI/RIASEC mapping
            ValidationResult personalityValidation = validatePersonalityMapping(
                testResult.getMbtiType(), 
                testResult.getRiasecCode(),
                testResult.getCoursePath(),
                testResult.getCareerSuggestions()
            );
            if (!personalityValidation.isValid()) {
                issues.add(new ValidationIssue(
                    "PERSONALITY_VALIDATION", 
                    "Personality mapping validation failed", 
                    personalityValidation.getMessage(),
                    personalityValidation.getConfidence()
                ));
            }
            
            // Validate development plans
            if (testResult.getCareerDevelopmentPlan() != null || testResult.getCourseDevelopmentPlan() != null) {
                ValidationResult devPlanValidation = validateDevelopmentPlans(testResult);
                if (!devPlanValidation.isValid()) {
                    issues.add(new ValidationIssue(
                        "DEVELOPMENT_PLAN_VALIDATION", 
                        "Development plan validation failed", 
                        devPlanValidation.getMessage(),
                        devPlanValidation.getConfidence()
                    ));
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error during AI validation: {}", e.getMessage(), e);
            issues.add(new ValidationIssue(
                "VALIDATION_ERROR", 
                "Validation process failed", 
                "Error during validation: " + e.getMessage(),
                0.0
            ));
        }
        
        report.setIssues(issues);
        report.setOverallValid(issues.isEmpty());
        report.setValidationScore(calculateValidationScore(issues));
        
        log.info("✅ AI validation completed. Issues found: {}, Score: {}", 
            issues.size(), report.getValidationScore());
        
        return report;
    }
    
    /**
     * Validate course recommendations
     */
    public ValidationResult validateCourseRecommendations(String coursePath) {
        log.debug("🔍 Validating course recommendations: {}", coursePath);
        
        try {
            // Parse courses from the course path string
            List<String> courses = parseCourses(coursePath);
            
            for (String course : courses) {
                String[] parts = course.split(":", 2);
                String courseName = parts[0].trim();
                String courseDescription = parts.length > 1 ? parts[1].trim() : "";
                
                // Validate each course
                HuggingFaceApiService.ValidationResponse response = 
                    huggingFaceService.validateCourseDescription(courseName, courseDescription);
                
                if (!response.isValid()) {
                    return new ValidationResult(false, 
                        String.format("Course validation failed for '%s': %s", courseName, response.getMessage()),
                        response.getConfidence()
                    );
                }
            }
            
            return new ValidationResult(true, "All course recommendations are valid", 0.9);
            
        } catch (Exception e) {
            log.error("❌ Error validating course recommendations: {}", e.getMessage(), e);
            return new ValidationResult(false, "Course validation error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Validate career suggestions
     */
    public ValidationResult validateCareerSuggestions(String careerSuggestions) {
        log.debug("🔍 Validating career suggestions: {}", careerSuggestions);
        
        try {
            // Parse careers from the career suggestions string
            List<String> careers = parseCareers(careerSuggestions);
            
            for (String career : careers) {
                String[] parts = career.split(":", 2);
                String careerName = parts[0].trim();
                String careerDescription = parts.length > 1 ? parts[1].trim() : "";
                
                // For now, we'll use a placeholder salary range
                // In a real implementation, you'd get this from your data
                String salaryRange = "₱20,000 - ₱50,000"; // Placeholder
                
                // Validate each career
                HuggingFaceApiService.ValidationResponse response = 
                    huggingFaceService.validateCareerInfo(careerName, careerDescription, salaryRange);
                
                if (!response.isValid()) {
                    return new ValidationResult(false, 
                        String.format("Career validation failed for '%s': %s", careerName, response.getMessage()),
                        response.getConfidence()
                    );
                }
            }
            
            return new ValidationResult(true, "All career suggestions are valid", 0.9);
            
        } catch (Exception e) {
            log.error("❌ Error validating career suggestions: {}", e.getMessage(), e);
            return new ValidationResult(false, "Career validation error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Validate personality mapping
     */
    public ValidationResult validatePersonalityMapping(String mbtiType, String riasecCode, 
                                                      String courseSuggestions, String careerSuggestions) {
        log.debug("🔍 Validating personality mapping: MBTI={}, RIASEC={}", mbtiType, riasecCode);
        
        try {
            String combinedSuggestions = courseSuggestions + "; " + careerSuggestions;
            
            HuggingFaceApiService.ValidationResponse response = 
                huggingFaceService.validatePersonalityMapping(mbtiType, riasecCode, combinedSuggestions);
            
            return new ValidationResult(
                response.isValid(),
                response.getMessage(),
                response.getConfidence()
            );
            
        } catch (Exception e) {
            log.error("❌ Error validating personality mapping: {}", e.getMessage(), e);
            return new ValidationResult(false, "Personality validation error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Validate development plans
     */
    public ValidationResult validateDevelopmentPlans(EnhancedTestResultDTO testResult) {
        log.debug("🔍 Validating development plans");
        
        try {
            // Basic validation - check if plans exist and are not empty
            boolean hasCareerPlan = testResult.getCareerDevelopmentPlan() != null;
            boolean hasCoursePlan = testResult.getCourseDevelopmentPlan() != null;
            
            if (!hasCareerPlan && !hasCoursePlan) {
                return new ValidationResult(false, "No development plans found", 1.0);
            }
            
            // More sophisticated validation could be added here
            // For now, we'll consider it valid if plans exist
            return new ValidationResult(true, "Development plans are present and valid", 0.8);
            
        } catch (Exception e) {
            log.error("❌ Error validating development plans: {}", e.getMessage(), e);
            return new ValidationResult(false, "Development plan validation error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Parse courses from course path string
     */
    private List<String> parseCourses(String coursePath) {
        List<String> courses = new ArrayList<>();
        
        if (coursePath.contains(";")) {
            String[] parts = coursePath.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    courses.add(trimmed);
                }
            }
        } else {
            String[] parts = coursePath.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    courses.add(trimmed);
                }
            }
        }
        
        return courses;
    }
    
    /**
     * Parse careers from career suggestions string
     */
    private List<String> parseCareers(String careerSuggestions) {
        List<String> careers = new ArrayList<>();
        
        if (careerSuggestions.contains(";")) {
            String[] parts = careerSuggestions.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    careers.add(trimmed);
                }
            }
        } else {
            String[] parts = careerSuggestions.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    careers.add(trimmed);
                }
            }
        }
        
        return careers;
    }
    
    /**
     * Calculate overall validation score
     */
    private double calculateValidationScore(List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return 1.0; // Perfect score
        }
        
        double totalConfidence = 0.0;
        for (ValidationIssue issue : issues) {
            totalConfidence += issue.getConfidence();
        }
        
        double averageConfidence = totalConfidence / issues.size();
        return Math.max(0.0, 1.0 - averageConfidence);
    }
    
    /**
     * Validation Result DTO
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private double confidence;
        
        public ValidationResult(boolean valid, String message, double confidence) {
            this.valid = valid;
            this.message = message;
            this.confidence = confidence;
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    /**
     * Validation Issue DTO
     */
    public static class ValidationIssue {
        private String type;
        private String title;
        private String description;
        private double confidence;
        
        public ValidationIssue(String type, String title, String description, double confidence) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.confidence = confidence;
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    /**
     * Validation Report DTO
     */
    public static class ValidationReport {
        private Long testResultId;
        private String timestamp;
        private boolean overallValid;
        private double validationScore;
        private List<ValidationIssue> issues;
        
        public ValidationReport() {
            this.issues = new ArrayList<>();
        }
        
        // Getters and setters
        public Long getTestResultId() { return testResultId; }
        public void setTestResultId(Long testResultId) { this.testResultId = testResultId; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public boolean isOverallValid() { return overallValid; }
        public void setOverallValid(boolean overallValid) { this.overallValid = overallValid; }
        
        public double getValidationScore() { return validationScore; }
        public void setValidationScore(double validationScore) { this.validationScore = validationScore; }
        
        public List<ValidationIssue> getIssues() { return issues; }
        public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }
    }
}
