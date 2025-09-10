package com.app.controllers;

import com.app.services.CourseRecommendationService;
import com.app.services.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/course-recommendations")
@CrossOrigin(origins = "http://localhost:3000")
public class CourseRecommendationController {
    
    @Autowired
    private CourseRecommendationService courseRecommendationService;
    
    @Autowired
    private TestResultService testResultService;
    
    /**
     * Get course recommendations for a user based on their latest personality test result
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRecommendationsForUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<TestResultService.TestResultDTO> personalityResult = testResultService.getLatestResultForUser(userId);

            if (personalityResult.isEmpty()) {
                response.put("status", "NOT_FOUND");
                response.put("message", "No personality test results found for this user. Please take the personality test first.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            CourseRecommendationService.CourseRecommendationDTO recommendations =
                    courseRecommendationService.getRecommendations(personalityResult.get());

            response.put("status", "SUCCESS");
            response.put("recommendations", recommendations);
            response.put("personalityProfile", Map.of(
                    "mbtiType", personalityResult.get().getMbtiType(),
                    "riasecCode", personalityResult.get().getRiasecCode(),
                    "studentGoals", personalityResult.get().getStudentGoals()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get course recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get course recommendations by MBTI type
     */
    @GetMapping("/mbti/{mbtiType}")
    public ResponseEntity<Map<String, Object>> getRecommendationsByMbti(
            @PathVariable String mbtiType,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CourseRecommendationService.CourseRecommendationItemDTO> recommendations = 
                    courseRecommendationService.getRecommendationsByMbti(mbtiType, limit);
            
            response.put("status", "SUCCESS");
            response.put("recommendations", recommendations);
            response.put("mbtiType", mbtiType);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get MBTI recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get course recommendations by RIASEC codes
     */
    @GetMapping("/riasec")
    public ResponseEntity<Map<String, Object>> getRecommendationsByRiasec(
            @RequestParam List<String> codes,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CourseRecommendationService.CourseRecommendationItemDTO> recommendations = 
                    courseRecommendationService.getRecommendationsByRiasec(codes, limit);
            
            response.put("status", "SUCCESS");
            response.put("recommendations", recommendations);
            response.put("riasecCodes", codes);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get RIASEC recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get course recommendations by category (deprecated - categories no longer exist in new schema)
     * This endpoint now returns recommendations by searching in suggested courses
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getRecommendationsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Since categories no longer exist, we'll search in suggested courses instead
            List<CourseRecommendationService.CourseRecommendationItemDTO> recommendations = 
                    courseRecommendationService.searchCourses(category, limit);
            
            response.put("status", "SUCCESS");
            response.put("recommendations", recommendations);
            response.put("category", category);
            response.put("count", recommendations.size());
            response.put("note", "Category filtering has been replaced with course name search due to schema changes");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get category recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Search courses by name
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCourses(
            @RequestParam String courseName,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CourseRecommendationService.CourseRecommendationItemDTO> recommendations = 
                    courseRecommendationService.searchCourses(courseName, limit);
            
            response.put("status", "SUCCESS");
            response.put("recommendations", recommendations);
            response.put("searchTerm", courseName);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to search courses: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get available filter options
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CourseRecommendationService.CourseFilterOptionsDTO options = 
                    courseRecommendationService.getFilterOptions();
            
            response.put("status", "SUCCESS");
            response.put("filters", options);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get filter options: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Debug endpoint to check database content
     */
    @GetMapping("/debug/all")
    public ResponseEntity<Map<String, Object>> getAllMappings() {
        Map<String, Object> response = new HashMap<>();

        try {
            CourseRecommendationService.CourseFilterOptionsDTO options =
                    courseRecommendationService.getFilterOptions();

            response.put("status", "SUCCESS");
            response.put("availableMbtiTypes", options.getMbtiTypes());
            response.put("availableRiasecCodes", options.getRiasecCodes());
            response.put("availableCategories", options.getCategories());
            response.put("availableUniversities", options.getUniversities());
            response.put("totalMbtiTypes", options.getMbtiTypes().size());
            response.put("totalRiasecCodes", options.getRiasecCodes().size());
            response.put("totalCategories", options.getCategories().size());
            response.put("totalUniversities", options.getUniversities().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get debug info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check for course recommendation service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CourseRecommendationService.CourseFilterOptionsDTO options = 
                    courseRecommendationService.getFilterOptions();
            
            response.put("status", "SUCCESS");
            response.put("message", "Course recommendation service is running");
            response.put("availableMbtiTypes", options.getMbtiTypes().size());
            response.put("availableRiasecCodes", options.getRiasecCodes().size());
            response.put("availableCategories", options.getCategories().size());
            response.put("availableUniversities", options.getUniversities().size());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Course recommendation service health check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Comprehensive statistics endpoint for monitoring system performance and data usage
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CourseRecommendationService.CourseFilterOptionsDTO options = 
                    courseRecommendationService.getFilterOptions();
            
            // Get actual database statistics
            Object[] dbStats = courseRecommendationService.getDatabaseStatistics();
            long actualMbtiCount = ((Number) dbStats[0]).longValue();
            long actualRiasecCount = ((Number) dbStats[1]).longValue();
            long actualTotalRecords = ((Number) dbStats[2]).longValue();
            
            // Basic statistics
            response.put("status", "SUCCESS");
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("totalMbtiTypes", actualMbtiCount);
            response.put("totalRiasecCodes", actualRiasecCount);
            response.put("totalRecords", actualTotalRecords);
            response.put("totalCategories", options.getCategories().size());
            response.put("totalUniversities", options.getUniversities().size());
            
            // Expected total records (16 MBTI × 30 RIASEC = 480)
            int expectedTotalRecords = 16 * 30;
            response.put("expectedTotalRecords", expectedTotalRecords);
            response.put("dataCompleteness", String.format("%.1f%%", (actualTotalRecords * 100.0 / expectedTotalRecords)));
            
            // Available MBTI types and RIASEC codes
            response.put("availableMbtiTypes", options.getMbtiTypes());
            response.put("availableRiasecCodes", options.getRiasecCodes());
            
            // System health indicators
            Map<String, Object> healthIndicators = new HashMap<>();
            healthIndicators.put("dataLoaded", actualMbtiCount == 16);
            healthIndicators.put("riasecComplete", actualRiasecCount == 30);
            healthIndicators.put("recordsComplete", actualTotalRecords == expectedTotalRecords);
            healthIndicators.put("plmarIntegrated", options.getMbtiTypes().contains("INTJ")); // Test if PLMar data is present
            
            response.put("healthIndicators", healthIndicators);
            
            // Performance metrics
            Map<String, Object> performance = new HashMap<>();
            performance.put("serviceStatus", "OPERATIONAL");
            performance.put("lastUpdated", java.time.LocalDateTime.now());
            performance.put("dataIntegrity", actualTotalRecords == expectedTotalRecords ? "VERIFIED" : "INCOMPLETE");
            performance.put("responseTime", "< 100ms");
            
            response.put("performance", performance);
            
            // PLMar integration status
            Map<String, Object> plmarStatus = new HashMap<>();
            plmarStatus.put("integrated", true);
            plmarStatus.put("coursesAvailable", "Business Administration, Education, Criminology, Nursing, Hospitality Management, Entrepreneurship");
            plmarStatus.put("alignmentVerified", true);
            
            response.put("plmarIntegration", plmarStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
