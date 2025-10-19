package com.app.controllers;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.dto.EnhancedTestResultDTO;
import com.app.models.TestResult;
import com.app.services.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/personality-test")
@CrossOrigin(origins = "http://localhost:3000")
public class PersonalityTestController {

    private static final Logger logger = LoggerFactory.getLogger(PersonalityTestController.class);

    @Autowired
    private TestResultService testResultService;
    
    /**
     * Submit personality test for guest users
     */
    @PostMapping("/submit/guest")
    public ResponseEntity<Map<String, Object>> submitTestForGuest(@RequestBody PersonalityTestSubmissionDTO submission,
                                                                  @RequestParam(required = false) String guestToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate submission
            if (submission.getAnswers() == null || submission.getAnswers().size() != 100) {
                response.put("status", "ERROR");
                response.put("message", "All 100 questions must be answered");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate goal settings and PLMar requirement
            if (submission.getGoalSettings() != null) {
                // PLMar status is required - check if it's explicitly set to true or false
                Boolean isFromPLMar = submission.getGoalSettings().getIsFromPLMar();
                if (isFromPLMar == null) {
                    response.put("status", "ERROR");
                    response.put("message", "PLMar status is required. Please answer 'Are you from PLMar or Not?'");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            UUID guestUUID = null;
            if (guestToken != null && !guestToken.isEmpty()) {
                try {
                    guestUUID = UUID.fromString(guestToken);
                } catch (IllegalArgumentException e) {
                    // Generate new UUID if invalid
                    guestUUID = UUID.randomUUID();
                }
            } else {
                guestUUID = UUID.randomUUID();
            }

            // Submit test for guest
            TestResultService.TestResultDTO result = testResultService.submitTestForGuest(submission, guestUUID);

            response.put("status", "SUCCESS");
            response.put("message", "Personality test submitted successfully");
            response.put("result", result);
            response.put("guestToken", result.getGuestToken().toString());
            response.put("sessionId", result.getSessionId().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in guest submission: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Failed to process personality test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Submit personality test for authenticated users
     */
    @PostMapping("/submit/user")
    public ResponseEntity<Map<String, Object>> submitTestForUser(@RequestBody PersonalityTestSubmissionDTO submission) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate submission
            if (submission.getUserId() == null) {
                response.put("status", "ERROR");
                response.put("message", "User ID is required for user submission");
                return ResponseEntity.badRequest().body(response);
            }

            if (submission.getAnswers() == null || submission.getAnswers().size() != 100) {
                response.put("status", "ERROR");
                response.put("message", "All 100 questions must be answered");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate goal settings and PLMar requirement
            if (submission.getGoalSettings() != null) {
                // PLMar status is required - check if it's explicitly set to true or false
                Boolean isFromPLMar = submission.getGoalSettings().getIsFromPLMar();
                if (isFromPLMar == null) {
                    response.put("status", "ERROR");
                    response.put("message", "PLMar status is required. Please answer 'Are you from PLMar or Not?'");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Submit test for user
            TestResultService.TestResultDTO result = testResultService.submitTestForUser(submission);

            response.put("status", "SUCCESS");
            response.put("message", "Personality test submitted successfully");
            response.put("result", result);
            response.put("sessionId", result.getSessionId().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to process personality test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    
    /**
     * Get test result by session ID (for both guests and users)
     */
    @GetMapping("/result/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getResultBySessionId(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            Optional<TestResultService.TestResultDTO> result = testResultService.getResultBySessionId(sessionUUID);

            if (result.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", result.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this session");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("message", "Invalid session ID format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get test result for guest by guest token
     */
    @GetMapping("/result/guest/{guestToken}")
    public ResponseEntity<Map<String, Object>> getLatestResultForGuest(@PathVariable String guestToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID guestUUID = UUID.fromString(guestToken);
            Optional<TestResultService.TestResultDTO> result = testResultService.getLatestResultForGuest(guestUUID);

            if (result.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", result.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this guest");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("message", "Invalid guest token format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get the latest personality test result for a user (using new test_results table)
     */
    @GetMapping("/result/user/{userId}")
    public ResponseEntity<Map<String, Object>> getLatestResult(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get result from new test_results table
            Optional<TestResultService.TestResultDTO> result = testResultService.getLatestResultForUser(userId);

            if (result.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", result.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No personality test results found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve personality test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all personality test results for a user (for history display)
     */
    @GetMapping("/results/user/{userId}")
    public ResponseEntity<Map<String, Object>> getAllResultsForUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== GET ALL RESULTS FOR USER ===");
            System.out.println("Getting all results for userId: " + userId);
            
            // Get all results from test_results table
            List<TestResultService.TestResultDTO> results = testResultService.getAllResultsForUser(userId);
            
            System.out.println("Found " + results.size() + " results");
            for (int i = 0; i < results.size(); i++) {
                TestResultService.TestResultDTO result = results.get(i);
                System.out.println("Result " + i + ": ID=" + result.getId() + ", MBTI=" + result.getMbtiType() + 
                    ", Age=" + result.getAge() + ", Gender=" + result.getGender() + ", IsFromPLMar=" + result.getIsFromPLMar());
            }

            response.put("status", "SUCCESS");
            response.put("results", results);
            response.put("count", results.size());
            
            System.out.println("Returning response with " + results.size() + " results");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error retrieving test results for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error retrieving test results: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check if a user has taken the personality test (using new test_results table)
     */
    @GetMapping("/check/user/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserHasTakenTest(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== CHECK USER TEST STATUS ===");
            System.out.println("Checking test status for userId: " + userId);
            
            // Check in the new test_results table
            Optional<TestResultService.TestResultDTO> result = testResultService.getLatestResultForUser(userId);
            boolean hasTaken = result.isPresent();
            
            System.out.println("Found result: " + result.isPresent());
            if (result.isPresent()) {
                System.out.println("Result ID: " + result.get().getId());
                System.out.println("Result MBTI: " + result.get().getMbtiType());
            }

            response.put("status", "SUCCESS");
            response.put("hasTakenTest", hasTaken);

            System.out.println("Returning response: " + response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error checking test status for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Failed to check test status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    


    

    


    /**
     * Submit personality test for a specific user (for testing purposes)
     */
    @PostMapping("/submit/user/{userId}")
    public ResponseEntity<Map<String, Object>> submitTestForUser(
            @PathVariable Long userId,
            @RequestBody PersonalityTestSubmissionDTO submission) {
        Map<String, Object> response = new HashMap<>();

        try {
            TestResultService.TestResultDTO result = testResultService.submitTestForUser(submission, userId);

            response.put("status", "SUCCESS");
            response.put("message", "Personality test submitted successfully");
            response.put("result", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to submit personality test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Test endpoint to check scoring with sample data (always creates new result)
     */
    @PostMapping("/test-scoring")
    public ResponseEntity<Map<String, Object>> testScoring() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Create sample test data
            PersonalityTestSubmissionDTO submission = new PersonalityTestSubmissionDTO();

            // Create sample answers (all 3s for neutral, with some variations)
            Map<Integer, Integer> answers = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                answers.put(i, 3); // Neutral answers
            }

            // Make some answers higher for testing
            for (int i = 0; i < 10; i++) {
                answers.put(i, 5); // High Realistic scores
            }
            for (int i = 10; i < 20; i++) {
                answers.put(i, 4); // Medium-high Investigative scores
            }

            submission.setAnswers(answers);

            // Create sample goal settings
            PersonalityTestSubmissionDTO.GoalSettingAnswersDTO goalSettings =
                new PersonalityTestSubmissionDTO.GoalSettingAnswersDTO();
            goalSettings.setPriority("High-paying job after graduation");
            goalSettings.setEnvironment("Laboratory or tech workspace");
            goalSettings.setMotivation("Discovering how things work");
            goalSettings.setConfidence(4);

            submission.setGoalSettings(goalSettings);

            // Test the scoring with a new UUID each time
            UUID newGuestToken = UUID.randomUUID();
            TestResultService.TestResultDTO result = testResultService.submitTestForGuest(submission, newGuestToken);

            response.put("status", "SUCCESS");
            response.put("message", "Test scoring completed");
            response.put("result", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Test scoring failed: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Health check endpoint for personality test service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test database connectivity
            long resultCount = testResultService.countTotalResults();
            
            response.put("status", "SUCCESS");
            response.put("message", "Personality test service is running");
            response.put("database", "Connected");
            response.put("totalResults", resultCount);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Personality test service health check failed: " + e.getMessage());
            response.put("database", "Disconnected");
            response.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get enhanced test result with detailed MBTI information for user
     */
    @GetMapping("/result/enhanced/user/{userId}")
    public ResponseEntity<Map<String, Object>> getEnhancedResultForUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<EnhancedTestResultDTO> result = testResultService.getEnhancedResultForUser(userId);

            if (result.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", result.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve enhanced test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get enhanced test result by session ID (for both users and guests)
     * Now includes authorization validation
     */
    @GetMapping("/result/enhanced/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getEnhancedResultBySessionId(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            
            // First, get the test result to check ownership
            Optional<TestResult> testResult = testResultService.getTestResultBySessionId(sessionUUID);
            
            if (!testResult.isPresent()) {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this session");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            TestResult result = testResult.get();
            
            // Authorization Check: Validate ownership
            boolean isAuthorized = false;
            boolean isUserResult = result.getUserId() != null;
            boolean isGuestResult = result.getGuestToken() != null;
            
            if (isUserResult) {
                // This is a registered user's result
                // ONLY the owner (matching userId) can access it
                // No guest tokens or other users can access
                if (userId != null && userId.equals(result.getUserId())) {
                    isAuthorized = true;
                    logger.debug("User {} accessing their own result (session: {})", userId, sessionId);
                } else {
                    isAuthorized = false;
                    logger.warn("Unauthorized: User {} attempted to access user result owned by {} (session: {})", 
                        userId, result.getUserId(), sessionId);
                }
            } else if (isGuestResult) {
                // This is a guest result
                // ONLY the matching guestToken can access it
                // No logged-in users or other guests can access
                if (guestToken != null) {
                    try {
                        UUID requestGuestToken = UUID.fromString(guestToken);
                        if (requestGuestToken.equals(result.getGuestToken())) {
                            isAuthorized = true;
                            logger.debug("Guest {} accessing their own result (session: {})", guestToken, sessionId);
                        } else {
                            isAuthorized = false;
                            logger.warn("Unauthorized: Guest {} attempted to access guest result owned by {} (session: {})", 
                                guestToken, result.getGuestToken(), sessionId);
                        }
                    } catch (IllegalArgumentException e) {
                        isAuthorized = false;
                        logger.warn("Unauthorized: Invalid guestToken format for session: {}", sessionId);
                    }
                } else {
                    // No guestToken provided - deny access even if userId is provided
                    // This prevents logged-in users from accessing guest results
                    isAuthorized = false;
                    logger.warn("Unauthorized: Attempted to access guest result without guestToken (session: {}, userId: {})", 
                        sessionId, userId);
                }
            } else {
                // Result has neither userId nor guestToken - this should never happen
                isAuthorized = false;
                logger.error("Invalid result state: No userId or guestToken for session: {}", sessionId);
            }
            
            if (!isAuthorized) {
                response.put("status", "UNAUTHORIZED");
                response.put("message", "You don't have permission to view these test results");
                response.put("isPrivate", isUserResult);
                logger.warn("Unauthorized access attempt to session: {} (User result: {}, Request userId: {}, Request guestToken: {})",
                    sessionId, isUserResult, userId, guestToken != null ? "provided" : "not provided");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // If authorized, proceed with fetching the enhanced result
            Optional<EnhancedTestResultDTO> enhancedResult = testResultService.getEnhancedResultBySessionId(sessionUUID);

            if (enhancedResult.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", enhancedResult.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this session");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("message", "Invalid session ID format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve enhanced test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Regenerate course descriptions for existing test results
     */
    @PostMapping("/result/regenerate-descriptions/{sessionId}")
    public ResponseEntity<Map<String, Object>> regenerateCourseDescriptions(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            boolean success = testResultService.regenerateCourseDescriptions(sessionUUID);

            if (success) {
                response.put("status", "SUCCESS");
                response.put("message", "Course descriptions regenerated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this session");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("message", "Invalid session ID format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to regenerate course descriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get enhanced test result with detailed MBTI information for guest
     */
    @GetMapping("/result/enhanced/guest/{guestToken}")
    public ResponseEntity<Map<String, Object>> getEnhancedResultForGuest(@PathVariable String guestToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID guestUUID = UUID.fromString(guestToken);
            Optional<EnhancedTestResultDTO> result = testResultService.getEnhancedResultForGuest(guestUUID);

            if (result.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("result", result.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "No test results found for this guest");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("message", "Invalid guest token format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve enhanced test result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test description tables connectivity
     */
    @GetMapping("/test-descriptions")
    public ResponseEntity<Map<String, Object>> testDescriptionTables() {
        try {
            Map<String, Object> result = testResultService.testDescriptionTables();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Debug endpoint to check explanation field content and length
     */
    @GetMapping("/debug-explanation/{mbtiType}/{riasecCode}")
    public ResponseEntity<Map<String, Object>> debugExplanation(
            @PathVariable String mbtiType, @PathVariable String riasecCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> debugInfo = testResultService.debugExplanationField(mbtiType, riasecCode);
            response.put("status", "SUCCESS");
            response.putAll(debugInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error debugging explanation for {} + {}: {}", mbtiType, riasecCode, e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Failed to debug explanation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get detailed scoring data for visualization/graphs
     */
    @GetMapping("/scoring-data/{sessionId}")
    public ResponseEntity<Map<String, Object>> getDetailedScoringData(@PathVariable String sessionId) {
        try {
            logger.info("Fetching detailed scoring data for session: {}", sessionId);
            com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
            if (scoringData != null) {
                logger.info("Found detailed scoring data for session: {}", sessionId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", scoringData);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("No scoring data found for session: {}", sessionId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No scoring data found for session: " + sessionId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching detailed scoring data for session {}: {}", sessionId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Debug endpoint to check if detailed scoring data exists for a session
     */
    @GetMapping("/debug-scoring/{sessionId}")
    public ResponseEntity<Map<String, Object>> debugScoringData(@PathVariable String sessionId) {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("sessionId", sessionId);
            
            // Check if test result exists
            Optional<TestResult> testResult = testResultService.getTestResultBySessionId(sessionId);
            debugInfo.put("testResultExists", testResult.isPresent());
            if (testResult.isPresent()) {
                debugInfo.put("testResultId", testResult.get().getId());
                debugInfo.put("mbtiType", testResult.get().getMbtiType());
                debugInfo.put("riasecCode", testResult.get().getRiasecCode());
            }
            
            // Check if detailed scoring exists
            com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
            debugInfo.put("scoringDataExists", scoringData != null);
            if (scoringData != null) {
                debugInfo.put("riasecScores", scoringData.getRiasecScores());
                debugInfo.put("mbtiScores", scoringData.getMbtiScores());
            }
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Regenerate detailed scoring data for existing test results
     */
    @PostMapping("/regenerate-scoring/{sessionId}")
    public ResponseEntity<Map<String, Object>> regenerateDetailedScoringData(@PathVariable String sessionId) {
        try {
            boolean success = testResultService.regenerateDetailedScoringData(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            if (success) {
                response.put("message", "Detailed scoring data regenerated successfully");
            } else {
                response.put("message", "Failed to regenerate detailed scoring data");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Debug endpoint to check all personality_test_scores data
     */
    @GetMapping("/debug-all-scores")
    public ResponseEntity<Map<String, Object>> debugAllScores() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get all personality test scores
            List<com.app.models.PersonalityTestScores> allScores = testResultService.getAllPersonalityTestScores();
            response.put("totalScores", allScores.size());
            response.put("scores", allScores.stream().map(score -> {
                Map<String, Object> scoreData = new HashMap<>();
                scoreData.put("id", score.getId());
                scoreData.put("testResultId", score.getTestResultId());
                scoreData.put("sessionId", score.getSessionId());
                scoreData.put("riasecRPercentage", score.getRiasecRPercentage());
                scoreData.put("riasecIPercentage", score.getRiasecIPercentage());
                scoreData.put("riasecAPercentage", score.getRiasecAPercentage());
                scoreData.put("riasecSPercentage", score.getRiasecSPercentage());
                scoreData.put("riasecEPercentage", score.getRiasecEPercentage());
                scoreData.put("riasecCPercentage", score.getRiasecCPercentage());
                scoreData.put("mbtiEPercentage", score.getMbtiEPercentage());
                scoreData.put("mbtiIPercentage", score.getMbtiIPercentage());
                scoreData.put("mbtiSPercentage", score.getMbtiSPercentage());
                scoreData.put("mbtiNPercentage", score.getMbtiNPercentage());
                scoreData.put("mbtiTPercentage", score.getMbtiTPercentage());
                scoreData.put("mbtiFPercentage", score.getMbtiFPercentage());
                scoreData.put("mbtiJPercentage", score.getMbtiJPercentage());
                scoreData.put("mbtiPPercentage", score.getMbtiPPercentage());
                scoreData.put("finalRiasecCode", score.getFinalRiasecCode());
                scoreData.put("finalMbtiType", score.getFinalMbtiType());
                scoreData.put("createdAt", score.getCreatedAt());
                return scoreData;
            }).collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test endpoint to verify data flow
     */
    @GetMapping("/test-data-flow/{sessionId}")
    public ResponseEntity<Map<String, Object>> testDataFlow(@PathVariable String sessionId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            
            // Step 1: Check if test result exists
            Optional<TestResult> testResult = testResultService.getTestResultBySessionId(sessionId);
            response.put("testResultExists", testResult.isPresent());
            if (testResult.isPresent()) {
                response.put("testResultId", testResult.get().getId());
                response.put("testResultSessionId", testResult.get().getSessionId());
                response.put("mbtiType", testResult.get().getMbtiType());
                response.put("riasecCode", testResult.get().getRiasecCode());
            }
            
            // Step 2: Check if detailed scoring exists
            com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
            response.put("scoringDataExists", scoringData != null);
            if (scoringData != null) {
                response.put("riasecScores", scoringData.getRiasecScores());
                response.put("mbtiScores", scoringData.getMbtiScores());
                response.put("finalRiasecCode", scoringData.getFinalRiasecCode());
                response.put("finalMbtiType", scoringData.getFinalMbtiType());
            }
            
            // Step 3: Check database directly
            List<com.app.models.PersonalityTestScores> allScores = testResultService.getAllPersonalityTestScores();
            response.put("totalScoresInDB", allScores.size());
            response.put("scoresForSession", allScores.stream()
                .filter(score -> score.getSessionId().toString().equals(sessionId))
                .map(score -> {
                    Map<String, Object> scoreData = new HashMap<>();
                    scoreData.put("id", score.getId());
                    scoreData.put("sessionId", score.getSessionId());
                    scoreData.put("riasecRPercentage", score.getRiasecRPercentage());
                    scoreData.put("mbtiEPercentage", score.getMbtiEPercentage());
                    return scoreData;
                })
                .collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Check if personality_test_scores table has data by session ID (legacy)
     */
    @GetMapping("/check-scores/{sessionId}")
    public ResponseEntity<Map<String, Object>> checkScoresData(@PathVariable String sessionId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            
            // Add debugging
            logger.info("Checking scores for session ID: {}", sessionId);
            
            // Check if detailed scoring exists
            com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
            response.put("scoringDataExists", scoringData != null);
            
            if (scoringData != null) {
                logger.info("Found scoring data for session: {}", sessionId);
                response.put("riasecScores", scoringData.getRiasecScores());
                response.put("mbtiScores", scoringData.getMbtiScores());
                response.put("finalRiasecCode", scoringData.getFinalRiasecCode());
                response.put("finalMbtiType", scoringData.getFinalMbtiType());
            } else {
                logger.warn("No scoring data found for session: {}", sessionId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking scores for session {}: {}", sessionId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get personality test scores by test result ID (preferred method)
     */
    @GetMapping("/scores/{testResultId}")
    public ResponseEntity<Map<String, Object>> getScoresByTestResultId(@PathVariable Long testResultId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("testResultId", testResultId);
            
            // Add debugging
            logger.info("Getting scores for test result ID: {}", testResultId);
            
            // Get detailed scoring data
            com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringDataByTestResultId(testResultId);
            response.put("scoringDataExists", scoringData != null);
            
            if (scoringData != null) {
                logger.info("Found scoring data for test result ID: {}", testResultId);
                response.put("riasecScores", scoringData.getRiasecScores());
                response.put("mbtiScores", scoringData.getMbtiScores());
                response.put("finalRiasecCode", scoringData.getFinalRiasecCode());
                response.put("finalMbtiType", scoringData.getFinalMbtiType());
                response.put("status", "SUCCESS");
            } else {
                logger.warn("No scoring data found for test result ID: {}", testResultId);
                response.put("status", "NOT_FOUND");
                response.put("message", "No detailed scoring data found for this test result");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting scores for test result ID {}: {}", testResultId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Force regenerate scoring data for a session (useful for fixing missing data)
     */
    @PostMapping("/force-regenerate-scoring/{sessionId}")
    public ResponseEntity<Map<String, Object>> forceRegenerateScoring(@PathVariable String sessionId) {
        try {
            logger.info("Force regenerating scoring data for session: {}", sessionId);
            boolean success = testResultService.regenerateDetailedScoringData(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            if (success) {
                response.put("message", "Scoring data regenerated successfully");
                // Also return the regenerated data
                com.app.dto.DetailedScoringDTO scoringData = testResultService.getDetailedScoringData(sessionId);
                if (scoringData != null) {
                    response.put("data", scoringData);
                }
            } else {
                response.put("message", "Failed to regenerate scoring data - check if session exists");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error force regenerating scoring data for session {}: {}", sessionId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test endpoint to create sample detailed scoring data
     */
    @PostMapping("/test-scoring/{sessionId}")
    public ResponseEntity<Map<String, Object>> createTestScoringData(@PathVariable String sessionId) {
        try {
            // Get the test result
            Optional<TestResult> testResultOpt = testResultService.getTestResultBySessionId(sessionId);
            if (testResultOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No test result found for session: " + sessionId);
                return ResponseEntity.notFound().build();
            }
            
            TestResult testResult = testResultOpt.get();
            
            // Create sample detailed scoring data with realistic percentages
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("riasecScores", Map.of(
                "R", Map.of("raw", 35, "percentage", 70.0, "label", "Realistic", "description", "Practical, hands-on, mechanical"),
                "I", Map.of("raw", 28, "percentage", 56.0, "label", "Investigative", "description", "Analytical, scientific, intellectual"),
                "A", Map.of("raw", 22, "percentage", 44.0, "label", "Artistic", "description", "Creative, expressive, original"),
                "S", Map.of("raw", 40, "percentage", 80.0, "label", "Social", "description", "Helpful, cooperative, caring"),
                "E", Map.of("raw", 32, "percentage", 64.0, "label", "Enterprising", "description", "Leadership, persuasive, ambitious"),
                "C", Map.of("raw", 18, "percentage", 36.0, "label", "Conventional", "description", "Organized, detail-oriented, systematic")
            ));
            
            sampleData.put("mbtiScores", Map.of(
                "E", Map.of("raw", 18, "percentage", 72.0, "label", "Extraversion", "description", "Outgoing, social, energetic"),
                "I", Map.of("raw", 7, "percentage", 28.0, "label", "Introversion", "description", "Reflective, reserved, focused"),
                "S", Map.of("raw", 20, "percentage", 80.0, "label", "Sensing", "description", "Practical, concrete, detail-oriented"),
                "N", Map.of("raw", 5, "percentage", 20.0, "label", "Intuition", "description", "Abstract, theoretical, future-focused"),
                "T", Map.of("raw", 15, "percentage", 60.0, "label", "Thinking", "description", "Logical, objective, analytical"),
                "F", Map.of("raw", 10, "percentage", 40.0, "label", "Feeling", "description", "Values-based, empathetic, personal"),
                "J", Map.of("raw", 22, "percentage", 88.0, "label", "Judging", "description", "Structured, decisive, organized"),
                "P", Map.of("raw", 3, "percentage", 12.0, "label", "Perceiving", "description", "Flexible, adaptable, spontaneous")
            ));
            
            sampleData.put("finalRiasecCode", testResult.getRiasecCode());
            sampleData.put("finalMbtiType", testResult.getMbtiType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", sampleData);
            response.put("message", "Sample detailed scoring data created");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
