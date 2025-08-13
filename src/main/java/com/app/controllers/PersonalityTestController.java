package com.app.controllers;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.services.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/personality-test")
@CrossOrigin(origins = "http://localhost:3000")
public class PersonalityTestController {
    
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
            // Get all results from test_results table
            List<TestResultService.TestResultDTO> results = testResultService.getAllResultsForUser(userId);

            response.put("status", "SUCCESS");
            response.put("results", results);
            response.put("count", results.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            // Check in the new test_results table
            Optional<TestResultService.TestResultDTO> result = testResultService.getLatestResultForUser(userId);
            boolean hasTaken = result.isPresent();

            response.put("status", "SUCCESS");
            response.put("hasTakenTest", hasTaken);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
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
            // Simple health check using TestResultService
            response.put("status", "SUCCESS");
            response.put("message", "Personality test service is running");
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Personality test service health check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
