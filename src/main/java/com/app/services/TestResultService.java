package com.app.services;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.dto.EnhancedTestResultDTO;
import com.app.models.TestResult;
import com.app.models.MbtiRiasecMapping;
import com.app.models.MbtiDetails;
import com.app.models.PersonalityTestScores;
import com.app.repositories.TestResultRepository;
import com.app.repositories.MbtiRiasecMappingRepository;
import com.app.repositories.MbtiDetailsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestResultService {

    private static final Logger logger = LoggerFactory.getLogger(TestResultService.class);

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private MbtiRiasecMappingRepository mappingRepository;

    @Autowired
    private MbtiDetailsRepository mbtiDetailsRepository;

    @Autowired
    private com.app.repositories.CourseDescriptionRepository courseDescriptionRepository;

    @Autowired
    private com.app.repositories.CareerDescriptionRepository careerDescriptionRepository;

    @Autowired
    private EnhancedScoringService enhancedScoringService;


    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Submit test for authenticated user
     */
    public TestResultDTO submitTestForUser(PersonalityTestSubmissionDTO submission) {
        if (submission.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required for user submission");
        }
        
        return processTestSubmission(submission, submission.getUserId(), null);
    }
    
    /**
     * Submit test for guest user
     */
    public TestResultDTO submitTestForGuest(PersonalityTestSubmissionDTO submission, UUID guestToken) {
        if (guestToken == null) {
            guestToken = UUID.randomUUID();
        }
        
        return processTestSubmission(submission, null, guestToken);
    }
    
    /**
     * Process test submission for both users and guests
     */
    private TestResultDTO processTestSubmission(PersonalityTestSubmissionDTO submission, Long userId, UUID guestToken) {
        logger.info("Processing test submission for userId: {}, guestToken: {}", userId, guestToken);
        
        // Check database health before processing
        try {
            long mappingCount = mappingRepository.countTotalMappings();
            logger.debug("Database health check - MbtiRiasecMapping count: {}", mappingCount);
            
            if (mappingCount == 0) {
                logger.warn("No MBTI mappings found in database. Recommendations may be limited.");
            }
            
            // Check description tables
            long courseDescCount = courseDescriptionRepository.count();
            long careerDescCount = careerDescriptionRepository.count();
            logger.debug("Database health check - CourseDescription count: {}, CareerDescription count: {}", courseDescCount, careerDescCount);
            
            if (courseDescCount == 0) {
                logger.warn("No course descriptions found in database. Course descriptions will be generated dynamically.");
            }
            if (careerDescCount == 0) {
                logger.warn("No career descriptions found in database. Career descriptions will be limited.");
            }
        } catch (Exception e) {
            logger.warn("Database health check failed: {}", e.getMessage());
        }
        
        try {
            // Calculate personality scores
            Map<Integer, Integer> answers = submission.getAnswers();
            logger.debug("Processing {} answers", answers.size());
            
            // Calculate enhanced RIASEC scores with percentages
            Map<String, com.app.dto.DetailedScoringDTO.ScoreData> enhancedRiasecScores = enhancedScoringService.calculateEnhancedRIASECScores(answers);
            String riasecTopTwo = enhancedScoringService.determineFinalRIASECCode(enhancedRiasecScores);
            logger.debug("Enhanced RIASEC scores calculated, top two: {}", riasecTopTwo);
            
            // Calculate enhanced MBTI scores with percentages
            Map<String, com.app.dto.DetailedScoringDTO.ScoreData> enhancedMbtiScores = enhancedScoringService.calculateEnhancedMBTIScores(answers);
            String mbtiType = enhancedScoringService.determineFinalMBTIType(enhancedMbtiScores);
            logger.debug("Enhanced MBTI scores calculated, type: {}", mbtiType);
            
            // Use legacy mbti_riasec_mappings table for recommendations
            logger.debug("Using legacy mbti_riasec_mappings for recommendations");

            // Get recommendations from mbti_riasec_mappings table
            List<MbtiRiasecMapping> courseRecommendations = Collections.emptyList();
            String coursePath;
            String careerSuggestions;
            
            try {
                    courseRecommendations = getCourseRecommendations(mbtiType, riasecTopTwo);
                coursePath = generateCoursePathWithDescriptions(courseRecommendations);
                careerSuggestions = generateCareerSuggestions(courseRecommendations);
                
                logger.debug("Generating additional recommendations");
                String learningStyle = generateLearningStyle(courseRecommendations, mbtiType);
                String studyTips = generateStudyTips(courseRecommendations, mbtiType);
                String personalityGrowthTips = generatePersonalityGrowthTips(courseRecommendations, mbtiType);
                String studentGoals = generateStudentGoals(submission.getGoalSettings());
                
                // Create and save test result
                logger.debug("Creating test result entity");
                TestResult testResult = new TestResult();
                testResult.setUserId(userId);
                testResult.setGuestToken(guestToken);
                // sessionId is automatically set in constructor and @PrePersist
                testResult.setMbtiType(mbtiType);
                testResult.setRiasecCode(riasecTopTwo);
                testResult.setCoursePath(coursePath);
                testResult.setCareerSuggestions(careerSuggestions);
                testResult.setLearningStyle(learningStyle);
                testResult.setStudyTips(studyTips);
                testResult.setPersonalityGrowthTips(personalityGrowthTips);
                testResult.setStudentGoals(studentGoals);
                
                // Set new demographic fields
                if (submission.getGoalSettings() != null) {
                    testResult.setAge(submission.getGoalSettings().getAge());
                    testResult.setGender(submission.getGoalSettings().getGender());
                    testResult.setIsFromPLMar(submission.getGoalSettings().getIsFromPLMar());
                    logger.debug("Set demographic info - Age: {}, Gender: {}, IsFromPLMar: {}", 
                        submission.getGoalSettings().getAge(), 
                        submission.getGoalSettings().getGender(), 
                        submission.getGoalSettings().getIsFromPLMar());
                } else {
                    logger.warn("Goal settings is null, setting demographic fields to null");
                    testResult.setAge(null);
                    testResult.setGender(null);
                    testResult.setIsFromPLMar(null);
                }
                
                logger.debug("Saving test result to database");
                logger.debug("TestResult before save - Age: {}, Gender: {}, IsFromPLMar: {}", 
                    testResult.getAge(), testResult.getGender(), testResult.getIsFromPLMar());
                TestResult savedResult = testResultRepository.save(testResult);
                logger.info("Successfully saved test result with ID: {}", savedResult.getId());
                
                // Save detailed scoring data for visualization
                try {
                    logger.info("Attempting to save detailed scoring data for testResultId: {}, sessionId: {}", 
                        savedResult.getId(), savedResult.getSessionId());
                    logger.info("Enhanced RIASEC scores: {}", enhancedRiasecScores.keySet());
                    logger.info("Enhanced MBTI scores: {}", enhancedMbtiScores.keySet());
                    
                    PersonalityTestScores savedScores = enhancedScoringService.saveDetailedScores(
                        savedResult.getId(), 
                        savedResult.getSessionId(),
                        enhancedRiasecScores,
                        enhancedMbtiScores,
                        riasecTopTwo,
                        mbtiType
                    );
                    logger.info("Successfully saved detailed scoring data with ID: {}", savedScores.getId());
                } catch (Exception e) {
                    logger.error("Failed to save detailed scoring data for test result ID {}: {}", savedResult.getId(), e.getMessage(), e);
                    // Don't fail the entire process if scoring data save fails
                }
                
                // Convert to DTO and return (using legacy format for compatibility)
                Map<String, Integer> legacyRiasecScores = new HashMap<>();
                Map<String, Integer> legacyMbtiScores = new HashMap<>();
                
                // Convert enhanced scores to legacy format for DTO
                for (Map.Entry<String, com.app.dto.DetailedScoringDTO.ScoreData> entry : enhancedRiasecScores.entrySet()) {
                    legacyRiasecScores.put(entry.getKey(), entry.getValue().getRaw());
                }
                for (Map.Entry<String, com.app.dto.DetailedScoringDTO.ScoreData> entry : enhancedMbtiScores.entrySet()) {
                    legacyMbtiScores.put(entry.getKey(), entry.getValue().getRaw());
                }
                
                return convertToDTO(savedResult, courseRecommendations, legacyRiasecScores, legacyMbtiScores);
                
            } catch (Exception e) {
                logger.error("Error processing test submission for userId: {}, guestToken: {}", userId, guestToken, e);
                throw new RuntimeException("Failed to process test submission: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("Critical error in test submission processing for userId: {}, guestToken: {}", userId, guestToken, e);
            throw new RuntimeException("Failed to process test submission: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get test result for user
     */
    public Optional<TestResultDTO> getLatestResultForUser(Long userId) {
        try {
        Optional<TestResult> result = testResultRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
        return result.map(this::convertToDTO);
        } catch (Exception e) {
            logger.warn("Error getting latest result for user {}: {}", userId, e.getMessage());
            logger.debug("Latest result error details: ", e);
            return Optional.empty();
        }
    }

    /**
     * Get all test results for user (for history)
     */
    public List<TestResultDTO> getAllResultsForUser(Long userId) {
        List<TestResult> results = testResultRepository.findByUserIdOrderByGeneratedAtDesc(userId);
        return results.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Submit test for a specific user
     */
    public TestResultDTO submitTestForUser(PersonalityTestSubmissionDTO submission, Long userId) {
        return processTestSubmission(submission, userId, null);
    }
    
    /**
     * Get test result for guest
     */
    public Optional<TestResultDTO> getLatestResultForGuest(UUID guestToken) {
        try {
        Optional<TestResult> result = testResultRepository.findTopByGuestTokenOrderByGeneratedAtDesc(guestToken);
        return result.map(this::convertToDTO);
        } catch (Exception e) {
            logger.warn("Error getting latest result for guest {}: {}", guestToken, e.getMessage());
            logger.debug("Latest guest result error details: ", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get detailed MBTI information by type
     */
    public Optional<MbtiDetails> getDetailedMbtiInformation(String mbtiType) {
        try {
        return mbtiDetailsRepository.findByMbtiType(mbtiType);
        } catch (Exception e) {
            logger.warn("Error getting MBTI details for type {}: {}", mbtiType, e.getMessage());
            logger.debug("MBTI details error details: ", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get enhanced test result with detailed MBTI information
     */
    public Optional<EnhancedTestResultDTO> getEnhancedResultForUser(Long userId) {
        try {
        Optional<TestResult> result = testResultRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            Optional<MbtiDetails> mbtiDetails = getDetailedMbtiInformation(testResult.getMbtiType());
            return Optional.of(convertToEnhancedDTO(testResult, mbtiDetails.orElse(null)));
        }
        return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error getting enhanced result for user {}: {}", userId, e.getMessage());
            logger.debug("Enhanced result error details: ", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get enhanced test result for guest with detailed MBTI information
     */
    public Optional<EnhancedTestResultDTO> getEnhancedResultForGuest(UUID guestToken) {
        try {
        Optional<TestResult> result = testResultRepository.findTopByGuestTokenOrderByGeneratedAtDesc(guestToken);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            Optional<MbtiDetails> mbtiDetails = getDetailedMbtiInformation(testResult.getMbtiType());
            return Optional.of(convertToEnhancedDTO(testResult, mbtiDetails.orElse(null)));
        }
        return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error getting enhanced result for guest {}: {}", guestToken, e.getMessage());
            logger.debug("Enhanced guest result error details: ", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get test result by session ID
     */
    public Optional<TestResultDTO> getResultBySessionId(UUID sessionId) {
        try {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        return result.map(this::convertToDTO);
        } catch (Exception e) {
            logger.warn("Error getting result by session ID {}: {}", sessionId, e.getMessage());
            logger.debug("Session ID result error details: ", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get enhanced test result by session ID
     */
    public Optional<EnhancedTestResultDTO> getEnhancedResultBySessionId(UUID sessionId) {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            Optional<MbtiDetails> mbtiDetails = getDetailedMbtiInformation(testResult.getMbtiType());
            return Optional.of(convertToEnhancedDTO(testResult, mbtiDetails.orElse(null)));
        }
        return Optional.empty();
    }

    /**
     * Count total test results
     */
    public long countTotalResults() {
        return testResultRepository.count();
    }
    
    /**
     * Test method to verify description tables are working
     */
    public Map<String, Object> testDescriptionTables() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test course description lookup
            Optional<com.app.models.CourseDescription> testCourse = courseDescriptionRepository.findByCourseName("BS Computer Science");
            result.put("courseDescriptionTest", testCourse.isPresent() ? "SUCCESS" : "FAILED");
            result.put("courseDescriptionCount", courseDescriptionRepository.count());
            
            // Test career description lookup
            Optional<com.app.models.CareerDescription> testCareer = careerDescriptionRepository.findByCareerName("Software Engineer");
            result.put("careerDescriptionTest", testCareer.isPresent() ? "SUCCESS" : "FAILED");
            result.put("careerDescriptionCount", careerDescriptionRepository.count());
            
            // Test sample lookup
            if (testCourse.isPresent()) {
                result.put("sampleCourseDescription", testCourse.get().getDescription().substring(0, Math.min(100, testCourse.get().getDescription().length())) + "...");
            }
            if (testCareer.isPresent()) {
                result.put("sampleCareerDescription", testCareer.get().getDescription().substring(0, Math.min(100, testCareer.get().getDescription().length())) + "...");
            }
            
            } catch (Exception e) {
            result.put("error", e.getMessage());
            logger.error("Error testing description tables: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Get detailed scoring data for visualization
     */
    public com.app.dto.DetailedScoringDTO getDetailedScoringData(String sessionId) {
        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            return enhancedScoringService.getDetailedScoringData(sessionUUID);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid session ID format: {}", sessionId);
            return null;
        } catch (Exception e) {
            logger.error("Error getting detailed scoring data for session {}: {}", sessionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get detailed scoring data by test result ID
     */
    public com.app.dto.DetailedScoringDTO getDetailedScoringDataByTestResultId(Long testResultId) {
        try {
            return enhancedScoringService.getDetailedScoringDataByTestResultId(testResultId);
        } catch (Exception e) {
            logger.error("Error getting detailed scoring data for test result ID {}: {}", testResultId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Regenerate detailed scoring data for existing test results
     */
    public boolean regenerateDetailedScoringData(String sessionId) {
        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            Optional<TestResult> testResultOpt = testResultRepository.findBySessionId(sessionUUID);
            
            if (testResultOpt.isEmpty()) {
                logger.warn("No test result found for session: {}", sessionId);
                return false;
            }
            
            TestResult testResult = testResultOpt.get();
            
            // Check if detailed scoring already exists
            if (enhancedScoringService.getDetailedScoringData(sessionUUID) != null) {
                logger.info("Detailed scoring data already exists for session: {}", sessionId);
                return true;
            }
            
            // Get the original answers from the test result
            // Note: We need to reconstruct the answers from the stored data
            // For now, we'll create a simple fallback based on the final results
            Map<Integer, Integer> reconstructedAnswers = reconstructAnswersFromResult(testResult);
            
            // Calculate enhanced scores
            Map<String, com.app.dto.DetailedScoringDTO.ScoreData> enhancedRiasecScores = 
                enhancedScoringService.calculateEnhancedRIASECScores(reconstructedAnswers);
            Map<String, com.app.dto.DetailedScoringDTO.ScoreData> enhancedMbtiScores = 
                enhancedScoringService.calculateEnhancedMBTIScores(reconstructedAnswers);
            
            String riasecTopTwo = enhancedScoringService.determineFinalRIASECCode(enhancedRiasecScores);
            String mbtiType = enhancedScoringService.determineFinalMBTIType(enhancedMbtiScores);
            
            // Save detailed scoring data
            enhancedScoringService.saveDetailedScores(
                testResult.getId(),
                testResult.getSessionId(),
                enhancedRiasecScores,
                enhancedMbtiScores,
                riasecTopTwo,
                mbtiType
            );
            
            logger.info("Successfully regenerated detailed scoring data for session: {}", sessionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error regenerating detailed scoring data for session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Reconstruct answers from test result (fallback method)
     * This is a simplified reconstruction - in a real scenario, you'd want to store the original answers
     */
    private Map<Integer, Integer> reconstructAnswersFromResult(TestResult testResult) {
        Map<Integer, Integer> answers = new HashMap<>();
        
        // This is a simplified reconstruction based on the final MBTI and RIASEC results
        // In a real implementation, you'd want to store the original answers
        
        // For RIASEC, we'll create answers that would result in the final RIASEC code
        String riasecCode = testResult.getRiasecCode();
        if (riasecCode.length() >= 2) {
            // Create varied scores for the top 2 RIASEC dimensions (higher scores)
            for (int i = 0; i < 2; i++) {
                char dimension = riasecCode.charAt(i);
                int startQuestion = getRIASECStartQuestion(dimension);
                // Create varied scores between 3-5 for preferred dimensions
                for (int q = startQuestion; q < startQuestion + 10; q++) {
                    int baseScore = 3 + (q % 3); // 3, 4, or 5
                    answers.put(q, baseScore);
                }
            }
            
            // Create varied lower scores for other dimensions
            String allDimensions = "RIASEC";
            for (char dim : allDimensions.toCharArray()) {
                if (!riasecCode.contains(String.valueOf(dim))) {
                    int startQuestion = getRIASECStartQuestion(dim);
                    // Create varied scores between 1-3 for non-preferred dimensions
                    for (int q = startQuestion; q < startQuestion + 10; q++) {
                        int baseScore = 1 + (q % 3); // 1, 2, or 3
                        answers.put(q, baseScore);
                    }
                }
            }
        }
        
        // For MBTI, we'll create answers that would result in the final MBTI type
        String mbtiType = testResult.getMbtiType();
        if (mbtiType.length() == 4) {
            // E/I dimension - create varied scores
            if (mbtiType.charAt(0) == 'E') {
                for (int q = 60; q < 65; q++) answers.put(q, 3 + (q % 3)); // E questions: 3, 4, or 5
                for (int q = 65; q < 70; q++) answers.put(q, 1 + (q % 3)); // I questions: 1, 2, or 3
            } else {
                for (int q = 60; q < 65; q++) answers.put(q, 1 + (q % 3)); // E questions: 1, 2, or 3
                for (int q = 65; q < 70; q++) answers.put(q, 3 + (q % 3)); // I questions: 3, 4, or 5
            }
            
            // S/N dimension - create varied scores
            if (mbtiType.charAt(1) == 'S') {
                for (int q = 70; q < 75; q++) answers.put(q, 3 + (q % 3)); // S questions: 3, 4, or 5
                for (int q = 75; q < 80; q++) answers.put(q, 1 + (q % 3)); // N questions: 1, 2, or 3
            } else {
                for (int q = 70; q < 75; q++) answers.put(q, 1 + (q % 3)); // S questions: 1, 2, or 3
                for (int q = 75; q < 80; q++) answers.put(q, 3 + (q % 3)); // N questions: 3, 4, or 5
            }
            
            // T/F dimension - create varied scores
            if (mbtiType.charAt(2) == 'T') {
                for (int q = 80; q < 85; q++) answers.put(q, 3 + (q % 3)); // T questions: 3, 4, or 5
                for (int q = 85; q < 90; q++) answers.put(q, 1 + (q % 3)); // F questions: 1, 2, or 3
            } else {
                for (int q = 80; q < 85; q++) answers.put(q, 1 + (q % 3)); // T questions: 1, 2, or 3
                for (int q = 85; q < 90; q++) answers.put(q, 3 + (q % 3)); // F questions: 3, 4, or 5
            }
            
            // J/P dimension - create varied scores
            if (mbtiType.charAt(3) == 'J') {
                for (int q = 90; q < 95; q++) answers.put(q, 3 + (q % 3)); // J questions: 3, 4, or 5
                for (int q = 95; q < 100; q++) answers.put(q, 1 + (q % 3)); // P questions: 1, 2, or 3
            } else {
                for (int q = 90; q < 95; q++) answers.put(q, 1 + (q % 3)); // J questions: 1, 2, or 3
                for (int q = 95; q < 100; q++) answers.put(q, 3 + (q % 3)); // P questions: 3, 4, or 5
            }
        }
        
        return answers;
    }
    
    private int getRIASECStartQuestion(char dimension) {
        switch (dimension) {
            case 'R': return 0;
            case 'I': return 10;
            case 'A': return 20;
            case 'S': return 30;
            case 'E': return 40;
            case 'C': return 50;
            default: return 0;
        }
    }
    
    /**
     * Regenerate course descriptions for existing test results
     */
    public boolean regenerateCourseDescriptions(UUID sessionId) {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            
            // Use legacy mbti_riasec_mappings for recommendations
            List<MbtiRiasecMapping> courseRecommendations = getCourseRecommendations(
                testResult.getMbtiType(), testResult.getRiasecCode());
            String newCoursePath = generateCoursePathWithDescriptions(courseRecommendations);
            String newCareerSuggestions = generateCareerSuggestions(courseRecommendations);
            
            // Update the test result
            testResult.setCoursePath(newCoursePath);
            testResult.setCareerSuggestions(newCareerSuggestions);
            testResultRepository.save(testResult);
            
            return true;
        }
        return false;
    }

    
    // Helper methods for calculations (reusing logic from PersonalityTestScoringService)
    private Map<String, Integer> calculateRIASECScores(Map<Integer, Integer> answers) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("R", sumAnswers(answers, 0, 9));    // Set 1: Questions 0-9
        scores.put("I", sumAnswers(answers, 10, 19));  // Set 2: Questions 10-19
        scores.put("A", sumAnswers(answers, 20, 29));  // Set 3: Questions 20-29
        scores.put("S", sumAnswers(answers, 30, 39));  // Set 4: Questions 30-39
        scores.put("E", sumAnswers(answers, 40, 49));  // Set 5: Questions 40-49
        scores.put("C", sumAnswers(answers, 50, 59));  // Set 6: Questions 50-59
        return scores;
    }
    
    private Map<String, Integer> calculateMBTIScores(Map<Integer, Integer> answers) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("E", sumAnswers(answers, 60, 64));  // Set 7: Questions 60-64
        scores.put("I", sumAnswers(answers, 65, 69));  // Set 7: Questions 65-69
        scores.put("S", sumAnswers(answers, 70, 74));  // Set 8: Questions 70-74
        scores.put("N", sumAnswers(answers, 75, 79));  // Set 8: Questions 75-79
        scores.put("T", sumAnswers(answers, 80, 84));  // Set 9: Questions 80-84
        scores.put("F", sumAnswers(answers, 85, 89));  // Set 9: Questions 85-89
        scores.put("J", sumAnswers(answers, 90, 94));  // Set 10: Questions 90-94
        scores.put("P", sumAnswers(answers, 95, 99));  // Set 10: Questions 95-99
        return scores;
    }
    
    private int sumAnswers(Map<Integer, Integer> answers, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += answers.getOrDefault(i, 0);
        }
        return sum;
    }
    
    private String getRIASECTopTwo(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining());
    }
    
    private String getMBTIType(Map<String, Integer> scores) {
        String EI = scores.get("E") > scores.get("I") ? "E" : "I";
        String SN = scores.get("S") > scores.get("N") ? "S" : "N";
        String TF = scores.get("T") > scores.get("F") ? "T" : "F";
        String JP = scores.get("J") > scores.get("P") ? "J" : "P";
        return EI + SN + TF + JP;
    }
    
    private List<MbtiRiasecMapping> getCourseRecommendations(String mbtiType, String riasecCode) {
        logger.debug("Getting course recommendations for MBTI: {} and RIASEC: {}", mbtiType, riasecCode);

        try {
        // First try to find exact matches with both MBTI and RIASEC
        List<MbtiRiasecMapping> exactMatches = mappingRepository.findByMbtiTypeAndRiasecCode(mbtiType, riasecCode);
        if (!exactMatches.isEmpty()) {
            logger.debug("Found {} exact matches", exactMatches.size());
            return exactMatches;
        }

        // If no exact matches, try MBTI only (limit for performance)
        List<MbtiRiasecMapping> mbtiMatches = mappingRepository.findByMbtiType(mbtiType);
        if (!mbtiMatches.isEmpty()) {
            logger.debug("Found {} MBTI-only matches", mbtiMatches.size());
            // Limit to first 10 results for performance
            return mbtiMatches.stream().limit(10).collect(Collectors.toList());
        }

        // If still no matches, try RIASEC only (limit for performance)
        List<MbtiRiasecMapping> riasecMatches = mappingRepository.findByRiasecCode(riasecCode);
        if (!riasecMatches.isEmpty()) {
            logger.debug("Found {} RIASEC-only matches", riasecMatches.size());
            // Limit to first 10 results for performance
            return riasecMatches.stream().limit(10).collect(Collectors.toList());
        }

        // Final fallback - try individual RIASEC codes (limit for performance)
        List<String> individualCodes = Arrays.asList(riasecCode.split(""));
        List<MbtiRiasecMapping> individualMatches = mappingRepository.findByRiasecCodesOrderById(individualCodes);
        logger.debug("Found {} matches for individual RIASEC codes", individualMatches.size());
        
        // Limit to first 10 results for performance
        return individualMatches.stream().limit(10).collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error getting course recommendations for MBTI: {} and RIASEC: {}: {}", mbtiType, riasecCode, e.getMessage());
            logger.debug("Course recommendation error details: ", e);
            return Collections.emptyList();
        }
    }
    
    private String generateCoursePath(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        String coursePath = recommendations.stream()
                .limit(1)  // Take the first (best) match
                .map(MbtiRiasecMapping::getSuggestedCourses)
                .filter(Objects::nonNull)
                .filter(courses -> !courses.trim().isEmpty())
                .collect(Collectors.joining("; "));

        if (coursePath.isEmpty()) {
            // Generate default course suggestions based on MBTI and RIASEC
            MbtiRiasecMapping firstRecommendation = recommendations.get(0);
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        return coursePath;
    }

    /**
     * Generate simple course path without complex descriptions (for performance)
     */
    private String generateSimpleCoursePath(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        MbtiRiasecMapping firstRecommendation = recommendations.get(0);
        String suggestedCourses = firstRecommendation.getSuggestedCourses();
        
        if (suggestedCourses == null || suggestedCourses.trim().isEmpty()) {
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        // Return simple course list without complex descriptions
        return suggestedCourses.replace(",", "; ");
    }

    /**
     * Generate simple career suggestions without complex descriptions (for performance)
     */
    private String generateSimpleCareerSuggestions(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Consider exploring careers that align with your personality type and interests.";
        }

        MbtiRiasecMapping firstRecommendation = recommendations.get(0);
        // Use a simple career suggestion based on personality type
        return "Explore careers that match your " + firstRecommendation.getMbtiType() + " personality type and " + 
               firstRecommendation.getRiasecCode() + " interests.";
    }

    /**
     * Generate course descriptions with detailed information for each course
     */
    public String generateCoursePathWithDescriptions(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        // Get the first (best) recommendation
        MbtiRiasecMapping firstRecommendation = recommendations.get(0);
        String suggestedCourses = firstRecommendation.getSuggestedCourses();
        
        if (suggestedCourses == null || suggestedCourses.trim().isEmpty()) {
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        // Split courses and generate descriptions
        String[] courses = suggestedCourses.split(",");
        StringBuilder coursePathWithDescriptions = new StringBuilder();
        
        for (int i = 0; i < courses.length; i++) {
            String course = courses[i].trim();
            if (!course.isEmpty()) {
                String description = generateCourseDescription(course, firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
                coursePathWithDescriptions.append(course).append(": ").append(description);
                
                if (i < courses.length - 1) {
                    coursePathWithDescriptions.append("; ");
                }
            }
        }

        return coursePathWithDescriptions.toString();
    }

    /**
     * Generate a unique description for a specific course based on course name and personality type
     */
    private String generateCourseDescription(String courseName, String mbtiType, String riasecCode) {
        // Prefer DB-backed description; fall back to legacy generator
        try {
            logger.debug("Looking up course description for: '{}'", courseName);
            
            Optional<com.app.models.CourseDescription> courseDesc = courseDescriptionRepository.findByCourseName(courseName);
            if (courseDesc.isPresent()) {
                String description = courseDesc.get().getDescription();
                logger.debug("Found course description for '{}': {}", courseName, description.substring(0, Math.min(100, description.length())) + "...");
                return description;
            } else {
                logger.debug("No course description found for '{}', trying partial match", courseName);
                // Try partial match
                Optional<com.app.models.CourseDescription> partialMatch = courseDescriptionRepository.findByCourseNameContainingIgnoreCase(courseName);
                if (partialMatch.isPresent()) {
                    String description = partialMatch.get().getDescription();
                    logger.debug("Found partial match for '{}': {}", courseName, description.substring(0, Math.min(100, description.length())) + "...");
                    return description;
                } else {
                    logger.debug("No course description found for '{}', using dynamic description", courseName);
                    return generateDynamicCourseDescription(courseName, mbtiType, riasecCode);
                }
            }
        } catch (Exception e) {
            logger.error("Error looking up course description for '{}': {}", courseName, e.getMessage(), e);
            return generateDynamicCourseDescription(courseName, mbtiType, riasecCode);
        }
    }

    /**
     * Legacy static descriptions (to be removed once DB descriptions are fully in use)
     */
    @Deprecated
    @SuppressWarnings("unused")
    private String generateCourseDescriptionLegacy(String courseName, String mbtiType, String riasecCode) {
        String course = courseName.toLowerCase().trim();
        Map<String, String> courseDescriptions = new HashMap<>();
        
        // Technology & Computer Science
        courseDescriptions.put("computer science", "Master programming languages, algorithms, and software development to build innovative digital solutions.");
        courseDescriptions.put("data science", "Learn to extract meaningful insights from large datasets using statistical analysis and machine learning techniques.");
        courseDescriptions.put("software engineering", "Design, develop, and maintain large-scale software systems using engineering principles and methodologies.");
        courseDescriptions.put("web development", "Create interactive websites and web applications using modern frontend and backend technologies.");
        courseDescriptions.put("cybersecurity", "Protect digital systems and networks from threats while developing expertise in information security.");
        courseDescriptions.put("artificial intelligence", "Develop intelligent systems and machine learning algorithms that can perform human-like tasks.");
        courseDescriptions.put("database management", "Design and optimize database systems for efficient data storage, retrieval, and management.");
        courseDescriptions.put("mobile app development", "Build native and cross-platform applications for smartphones and tablets using modern frameworks.");
        courseDescriptions.put("cloud computing", "Deploy, manage, and scale applications using distributed computing resources over the internet.");
        courseDescriptions.put("information technology", "Support and maintain computer systems, networks, and technology infrastructure for organizations.");
        
        // Business & Management
        courseDescriptions.put("business administration", "Develop leadership skills and strategic thinking for managing organizations and driving business success.");
        courseDescriptions.put("marketing", "Learn to promote products and services effectively through digital channels and consumer psychology.");
        courseDescriptions.put("finance", "Analyze financial markets, manage investments, and make strategic financial decisions for organizations.");
        courseDescriptions.put("accounting", "Record, analyze, and report financial transactions while preparing comprehensive financial statements.");
        courseDescriptions.put("economics", "Study how societies allocate scarce resources and understand market dynamics and policy impacts.");
        courseDescriptions.put("project management", "Lead teams and deliver projects successfully within scope, time, and budget constraints.");
        courseDescriptions.put("human resources", "Manage talent acquisition, employee development, and organizational culture to drive business success.");
        courseDescriptions.put("entrepreneurship", "Develop skills to start and grow businesses while identifying market opportunities and managing risks.");
        
        // Hospitality & Tourism Management
        courseDescriptions.put("bs hospitality management", "Master hotel operations, guest services, and hospitality leadership to excel in the global hospitality industry.");
        courseDescriptions.put("hospitality management", "Master hotel operations, guest services, and hospitality leadership to excel in the global hospitality industry.");
        courseDescriptions.put("bs event management", "Learn to plan, organize, and execute successful events while managing budgets, vendors, and client relationships.");
        courseDescriptions.put("event management", "Learn to plan, organize, and execute successful events while managing budgets, vendors, and client relationships.");
        courseDescriptions.put("bs tourism", "Explore destination marketing, sustainable tourism practices, and cultural heritage management for global travel industry.");
        courseDescriptions.put("tourism", "Explore destination marketing, sustainable tourism practices, and cultural heritage management for global travel industry.");
        courseDescriptions.put("bs entrepreneurship", "Develop innovative thinking and business skills to create and scale successful startups and ventures.");
        courseDescriptions.put("bs marketing", "Master digital marketing strategies, consumer behavior analysis, and brand management for competitive markets.");
        courseDescriptions.put("bs international business", "Navigate global markets, cross-cultural management, and international trade regulations for worldwide business success.");
        courseDescriptions.put("international business", "Navigate global markets, cross-cultural management, and international trade regulations for worldwide business success.");
        
        // Healthcare & Medicine
        courseDescriptions.put("medicine", "Train to diagnose, treat, and prevent diseases while maintaining patient health and well-being.");
        courseDescriptions.put("nursing", "Provide direct patient care, health education, and coordinate treatment plans in healthcare settings.");
        courseDescriptions.put("healthcare administration", "Manage healthcare organizations, policies, and operations to improve patient care delivery.");
        courseDescriptions.put("pharmacy", "Study drug interactions, medication management, and pharmaceutical care for patient health outcomes.");
        courseDescriptions.put("public health", "Address population health issues and develop strategies to prevent disease and promote wellness.");
        courseDescriptions.put("physical therapy", "Help patients recover from injuries and improve mobility through therapeutic exercise and treatment.");
        
        // Science & Engineering
        courseDescriptions.put("engineering", "Apply scientific and mathematical principles to design, build, and maintain technological solutions.");
        courseDescriptions.put("mathematics", "Develop advanced problem-solving skills and explore abstract concepts across various disciplines.");
        courseDescriptions.put("physics", "Investigate the fundamental forces, matter, energy, and the laws governing the physical universe.");
        courseDescriptions.put("chemistry", "Study the composition, structure, properties, and reactions of matter at the molecular level.");
        courseDescriptions.put("biology", "Examine living organisms, their life processes, evolution, and interactions with their environment.");
        courseDescriptions.put("environmental science", "Address environmental challenges and develop sustainable solutions for ecological conservation.");
        courseDescriptions.put("biotechnology", "Apply biological processes to develop products and technologies that improve human life.");
        
        // Social Sciences & Humanities
        courseDescriptions.put("psychology", "Investigate human mental processes, behavior patterns, and the factors that influence individual development.");
        courseDescriptions.put("education", "Learn teaching methodologies, curriculum development, and educational psychology for classroom instruction.");
        courseDescriptions.put("social work", "Help individuals and communities overcome challenges while promoting social justice and well-being.");
        courseDescriptions.put("counseling", "Provide therapeutic support and guidance to help people navigate personal and professional challenges.");
        courseDescriptions.put("criminal justice", "Study law enforcement, legal systems, and rehabilitation to maintain social order and justice.");
        courseDescriptions.put("political science", "Analyze government systems, political behavior, and policy development in democratic societies.");
        courseDescriptions.put("sociology", "Examine social relationships, institutions, and cultural patterns that shape human behavior.");
        
        // Arts & Creative Fields
        courseDescriptions.put("graphic design", "Create visual concepts using typography, imagery, and layout to communicate ideas effectively.");
        courseDescriptions.put("digital marketing", "Promote brands and products through online channels, social media, and digital advertising platforms.");
        courseDescriptions.put("creative writing", "Develop storytelling skills and literary techniques to express ideas through written word.");
        courseDescriptions.put("journalism", "Research, write, and report news stories while maintaining ethical standards and accuracy.");
        courseDescriptions.put("communications", "Master effective communication strategies across various media platforms and audiences.");
        courseDescriptions.put("film studies", "Analyze cinematic techniques and storytelling methods while understanding film's cultural impact.");
        courseDescriptions.put("music", "Develop musical skills and understanding of composition, performance, and music theory.");
        
        // Architecture & Design
        courseDescriptions.put("architecture", "Design functional and aesthetic buildings while considering environmental and social factors.");
        courseDescriptions.put("interior design", "Create beautiful and functional interior spaces that enhance human experience and well-being.");
        courseDescriptions.put("urban planning", "Design sustainable cities and communities that promote quality of life and environmental health.");
        courseDescriptions.put("landscape architecture", "Design outdoor spaces that balance natural beauty with functional requirements.");
        
        // Try to find exact match first
        String description = courseDescriptions.get(course);
        if (description != null) {
            return description;
        }
        
        // Try partial matching for variations (case-insensitive)
        for (Map.Entry<String, String> entry : courseDescriptions.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String courseLower = course.toLowerCase();
            if (courseLower.contains(key) || key.contains(courseLower)) {
                return entry.getValue();
            }
        }
        
        // Try matching without "BS" prefix
        if (course.startsWith("bs ")) {
            String courseWithoutBS = course.substring(3).trim();
            description = courseDescriptions.get(courseWithoutBS);
            if (description != null) {
                return description;
            }
        }
        
        // Try matching with "BS" prefix
        if (!course.startsWith("bs ")) {
            String courseWithBS = "bs " + course;
            description = courseDescriptions.get(courseWithBS);
            if (description != null) {
                return description;
            }
        }
        
        // Generate dynamic description based on personality type and field
        return generateDynamicCourseDescription(courseName, mbtiType, riasecCode);
    }

    /**
     * Generate a dynamic description when no specific course description is found
     */
    private String generateDynamicCourseDescription(String courseName, String mbtiType, String riasecCode) {
        // Determine field category based on course name
        String field = determineFieldCategory(courseName);
        
        // Generate description based on personality type and field
        StringBuilder description = new StringBuilder();
        
        // Add field-specific action
        description.append(getFieldAction(field));
        
        // Add personality-specific learning approach
        description.append(" ").append(getPersonalityLearningApproach(mbtiType));
        
        // Add RIASEC-specific career focus
        description.append(" ").append(getRiasecCareerFocus(riasecCode));
        
        return description.toString();
    }

    private String determineFieldCategory(String courseName) {
        String course = courseName.toLowerCase();
        
        if (course.contains("computer") || course.contains("software") || course.contains("data") || 
            course.contains("cyber") || course.contains("ai") || course.contains("web")) {
            return "technology";
        } else if (course.contains("business") || course.contains("marketing") || course.contains("finance") || 
                   course.contains("management") || course.contains("economics")) {
            return "business";
        } else if (course.contains("medicine") || course.contains("nursing") || course.contains("health") || 
                   course.contains("pharmacy") || course.contains("therapy")) {
            return "healthcare";
        } else if (course.contains("engineering") || course.contains("physics") || course.contains("chemistry") || 
                   course.contains("mathematics") || course.contains("biology")) {
            return "science";
        } else if (course.contains("psychology") || course.contains("education") || course.contains("social") || 
                   course.contains("counseling") || course.contains("criminal")) {
            return "social_sciences";
        } else if (course.contains("design") || course.contains("art") || course.contains("creative") || 
                   course.contains("journalism") || course.contains("communication")) {
            return "arts";
        } else if (course.contains("architecture") || course.contains("planning") || course.contains("landscape")) {
            return "design";
        } else {
            return "general";
        }
    }

    private String getFieldAction(String field) {
        switch (field) {
            case "technology": return "Master cutting-edge technologies and programming concepts to build innovative solutions";
            case "business": return "Develop strategic thinking and leadership skills to drive organizational success";
            case "healthcare": return "Learn to provide compassionate care and improve patient health outcomes";
            case "science": return "Explore scientific principles and conduct research to advance human knowledge";
            case "social_sciences": return "Understand human behavior and social systems to create positive change";
            case "arts": return "Develop creative skills and artistic expression to communicate meaningful ideas";
            case "design": return "Create functional and beautiful spaces that enhance human experience";
            default: return "Gain specialized knowledge and skills in your chosen field";
        }
    }

    private String getPersonalityLearningApproach(String mbtiType) {
        if (mbtiType.startsWith("I")) {
            return "through independent study and deep reflection";
        } else {
            return "through collaborative projects and hands-on experience";
        }
    }

    private String getRiasecCareerFocus(String riasecCode) {
        if (riasecCode.contains("R")) {
            return "with practical, hands-on applications";
        } else if (riasecCode.contains("I")) {
            return "with analytical and research-oriented approaches";
        } else if (riasecCode.contains("A")) {
            return "with creative and innovative methodologies";
        } else if (riasecCode.contains("S")) {
            return "with a focus on helping others and community impact";
        } else if (riasecCode.contains("E")) {
            return "with leadership and entrepreneurial opportunities";
        } else if (riasecCode.contains("C")) {
            return "with structured and detail-oriented processes";
        } else {
            return "with comprehensive skill development";
        }
    }

    private String generateCareerSuggestions(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            logger.debug("No recommendations provided for career suggestions");
            return "Career options will vary based on your chosen field of study and personal interests.";
        }

        // Use first mapping's career list, then expand with DB-backed descriptions if needed
        MbtiRiasecMapping first = recommendations.get(0);
        String raw = Optional.ofNullable(first.getCareerSuggestions()).orElse("").trim();
        if (raw.isEmpty()) {
            logger.debug("No career suggestions found in mapping, using default");
            return generateDefaultCareerSuggestions(first.getMbtiType(), first.getRiasecCode());
        }

        logger.debug("Processing career suggestions: {}", raw);
        String[] careers = raw.split(",");
        List<String> withDescriptions = new ArrayList<>();
        
        for (String c : careers) {
            String name = c.trim();
            if (name.isEmpty()) continue;
            
            logger.debug("Looking up career description for: '{}'", name);
            try {
            String desc = careerDescriptionRepository.findByCareerName(name)
                    .map(com.app.models.CareerDescription::getDescription)
                        .or(() -> {
                            logger.debug("Exact match not found for '{}', trying case-insensitive", name);
                            return careerDescriptionRepository.findFirstByCareerNameIgnoreCase(name).map(com.app.models.CareerDescription::getDescription);
                        })
                        .or(() -> {
                            logger.debug("Case-insensitive match not found for '{}', trying partial match", name);
                            return careerDescriptionRepository.findFirstByCareerNameContainingIgnoreCase(name).map(com.app.models.CareerDescription::getDescription);
                        })
                    .orElse("");
                
            if (!desc.isEmpty()) {
                    logger.debug("Found career description for '{}': {}", name, desc.substring(0, Math.min(100, desc.length())) + "...");
                withDescriptions.add(name + ": " + desc);
            } else {
                    logger.debug("No career description found for '{}', using name only", name);
                withDescriptions.add(name);
            }
            } catch (Exception e) {
                logger.error("Error looking up career description for '{}': {}", name, e.getMessage(), e);
                withDescriptions.add(name);
            }
        }
        
        String result = String.join("; ", withDescriptions);
        logger.debug("Final career suggestions result: {}", result.substring(0, Math.min(200, result.length())) + "...");
        return result;
    }
    
    private String generateLearningStyle(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // First try to get learning style from mapping
        if (recommendations != null && !recommendations.isEmpty()) {
            String learningStyle = recommendations.get(0).getLearningStyle();
            if (learningStyle != null && !learningStyle.trim().isEmpty()) {
                return learningStyle;
            }
        }
        
        // If not available from mapping, get from detailed MBTI information
        try {
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getLearningStyleSummary();
            }
        } catch (Exception e) {
            logger.warn("Error getting MBTI details for learning style (MBTI: {}): {}", mbtiType, e.getMessage());
        }
        
        return "Mixed learning approach";
    }
    
    private String generateStudyTips(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // First try to get study tips from mapping
        if (recommendations != null && !recommendations.isEmpty()) {
            String studyTips = recommendations.get(0).getStudyTips();
            if (studyTips != null && !studyTips.trim().isEmpty()) {
                return studyTips;
            }
        }
        
        // If not available from mapping, get from detailed MBTI information
        try {
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getStudyTipsSummary();
            }
        } catch (Exception e) {
            logger.warn("Error getting MBTI details for study tips (MBTI: {}): {}", mbtiType, e.getMessage());
        }
        
        return "Focus on your strengths and preferred learning methods.";
    }
    
    private String generatePersonalityGrowthTips(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // First try to get personality growth tips from mapping
        if (recommendations != null && !recommendations.isEmpty()) {
            String growthTips = recommendations.get(0).getPersonalityGrowthTips();
            if (growthTips != null && !growthTips.trim().isEmpty()) {
                return growthTips;
            }
        }
        
        // If not available from mapping, get general growth challenges from detailed MBTI information
        try {
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getGrowthChallenges();
            }
        } catch (Exception e) {
            logger.warn("Error getting MBTI details for personality growth tips (MBTI: {}): {}", mbtiType, e.getMessage());
        }
        
        return "Continue developing your natural strengths while working on areas for improvement.";
    }
    
    private String generateStudentGoals(PersonalityTestSubmissionDTO.GoalSettingAnswersDTO goalSettings) {
        if (goalSettings == null) {
            logger.warn("Goal settings are null, returning empty JSON");
            return "{}";
        }
        
        try {
            Map<String, Object> goals = new HashMap<>();
            goals.put("priority", goalSettings.getPriority() != null ? goalSettings.getPriority() : "");
            goals.put("environment", goalSettings.getEnvironment() != null ? goalSettings.getEnvironment() : "");
            goals.put("motivation", goalSettings.getMotivation() != null ? goalSettings.getMotivation() : "");
            goals.put("confidence", goalSettings.getConfidence() != null ? goalSettings.getConfidence() : 0);
            goals.put("concern", goalSettings.getConcern() != null ? goalSettings.getConcern() : "");
            goals.put("routine", goalSettings.getRoutine() != null ? goalSettings.getRoutine() : "");
            goals.put("impact", goalSettings.getImpact() != null ? goalSettings.getImpact() : "");
            goals.put("age", goalSettings.getAge() != null ? goalSettings.getAge() : null);
            goals.put("gender", goalSettings.getGender() != null ? goalSettings.getGender() : "");
            goals.put("isFromPLMar", goalSettings.getIsFromPLMar() != null ? goalSettings.getIsFromPLMar() : null);
            
            String result = objectMapper.writeValueAsString(goals);
            logger.debug("Successfully serialized goal settings: {}", result);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize goal settings to JSON: {}", e.getMessage(), e);
            return "{}";
        } catch (Exception e) {
            logger.error("Unexpected error processing goal settings: {}", e.getMessage(), e);
            return "{}";
        }
    }
    
    private TestResultDTO convertToDTO(TestResult entity) {
        return convertToDTO(entity, null, null, null);
    }
    
    private TestResultDTO convertToDTO(TestResult entity, List<MbtiRiasecMapping> recommendations, 
                                     Map<String, Integer> riasecScores, Map<String, Integer> mbtiScores) {
        TestResultDTO dto = new TestResultDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setGuestToken(entity.getGuestToken());
        dto.setSessionId(entity.getSessionId());
        dto.setMbtiType(entity.getMbtiType());
        dto.setRiasecCode(entity.getRiasecCode());
        dto.setCoursePath(entity.getCoursePath());
        dto.setCareerSuggestions(entity.getCareerSuggestions());
        dto.setLearningStyle(entity.getLearningStyle());
        dto.setStudyTips(entity.getStudyTips());
        dto.setPersonalityGrowthTips(entity.getPersonalityGrowthTips());
        dto.setStudentGoals(entity.getStudentGoals());
        dto.setAge(entity.getAge());
        dto.setGender(entity.getGender());
        dto.setIsFromPLMar(entity.getIsFromPLMar());
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setTakenAt(entity.getTakenAt());
        
        // Add course recommendations if available
        if (recommendations != null) {
            dto.setCourseRecommendations(recommendations.stream()
                    .limit(10)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * Convert TestResult entity to EnhancedTestResultDTO with detailed MBTI information
     */
    private EnhancedTestResultDTO convertToEnhancedDTO(TestResult entity, MbtiDetails mbtiDetails) {
        EnhancedTestResultDTO dto = new EnhancedTestResultDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setGuestToken(entity.getGuestToken() != null ? entity.getGuestToken().toString() : null);
        dto.setMbtiType(entity.getMbtiType());
        dto.setRiasecCode(entity.getRiasecCode());
        dto.setCoursePath(entity.getCoursePath());
        dto.setCareerSuggestions(entity.getCareerSuggestions());
        dto.setLearningStyle(entity.getLearningStyle());
        dto.setStudyTips(entity.getStudyTips());
        dto.setPersonalityGrowthTips(entity.getPersonalityGrowthTips());
        dto.setStudentGoals(entity.getStudentGoals());
        dto.setAge(entity.getAge());
        dto.setGender(entity.getGender());
        dto.setIsFromPLMar(entity.getIsFromPLMar());
        dto.setGeneratedAt(entity.getGeneratedAt());
        
        // Add detailed MBTI information if available
        if (mbtiDetails != null) {
            dto.setDetailedMbtiInfo(new EnhancedTestResultDTO.DetailedMbtiInfoDTO(mbtiDetails));
        }
        
        return dto;
    }

    /**
     * Generate default course path based on personality type when database records are empty
     */
    private String generateDefaultCoursePath(String mbtiType, String riasecCode) {
        Map<String, String> mbtiCourses = new HashMap<>();
        mbtiCourses.put("INTJ", "Computer Science, Engineering, Business Administration, Architecture, Medicine");
        mbtiCourses.put("INTP", "Computer Science, Mathematics, Physics, Philosophy, Research");
        mbtiCourses.put("ENTJ", "Business Administration, Law, Engineering Management, Economics, Political Science");
        mbtiCourses.put("ENTP", "Business, Marketing, Communications, Journalism, Entrepreneurship");
        mbtiCourses.put("INFJ", "Psychology, Social Work, Education, Counseling, Writing");
        mbtiCourses.put("INFP", "Psychology, Creative Writing, Art, Social Work, Liberal Arts");
        mbtiCourses.put("ENFJ", "Education, Psychology, Communications, Social Work, Human Resources");
        mbtiCourses.put("ENFP", "Communications, Marketing, Psychology, Journalism, Arts");
        mbtiCourses.put("ISTJ", "Accounting, Business Administration, Engineering, Medicine, Law");
        mbtiCourses.put("ISFJ", "Nursing, Education, Social Work, Psychology, Healthcare");
        mbtiCourses.put("ESTJ", "Business Administration, Management, Law, Engineering, Finance");
        mbtiCourses.put("ESFJ", "Education, Nursing, Social Work, Business, Human Resources");
        mbtiCourses.put("ISTP", "Engineering, Computer Science, Mechanics, Architecture, Technology");
        mbtiCourses.put("ISFP", "Art, Design, Music, Psychology, Healthcare");
        mbtiCourses.put("ESTP", "Business, Sales, Marketing, Sports Management, Emergency Services");
        mbtiCourses.put("ESFP", "Communications, Entertainment, Education, Social Work, Arts");

        String baseCourses = mbtiCourses.getOrDefault(mbtiType, "General Studies, Liberal Arts, Business");

        // Add RIASEC-specific suggestions
        if (riasecCode.contains("R")) baseCourses += ", Engineering Technology, Skilled Trades";
        if (riasecCode.contains("I")) baseCourses += ", Research, Sciences, Analytics";
        if (riasecCode.contains("A")) baseCourses += ", Creative Arts, Design, Media";
        if (riasecCode.contains("S")) baseCourses += ", Education, Healthcare, Social Services";
        if (riasecCode.contains("E")) baseCourses += ", Business, Management, Sales";
        if (riasecCode.contains("C")) baseCourses += ", Administration, Finance, Information Systems";

        return baseCourses;
    }

    /**
     * Generate default career suggestions based on personality type when database records are empty
     */
    private String generateDefaultCareerSuggestions(String mbtiType, String riasecCode) {
        Map<String, String> mbtiCareers = new HashMap<>();
        mbtiCareers.put("INTJ", "Software Architect, Systems Analyst, Project Manager, Research Scientist, Strategic Planner");
        mbtiCareers.put("INTP", "Software Developer, Research Scientist, Data Analyst, Professor, Systems Analyst");
        mbtiCareers.put("ENTJ", "CEO, Management Consultant, Lawyer, Investment Banker, Operations Manager");
        mbtiCareers.put("ENTP", "Entrepreneur, Marketing Manager, Consultant, Sales Manager, Innovation Specialist");
        mbtiCareers.put("INFJ", "Counselor, Psychologist, Teacher, Social Worker, Writer");
        mbtiCareers.put("INFP", "Writer, Counselor, Artist, Social Worker, Therapist");
        mbtiCareers.put("ENFJ", "Teacher, Counselor, HR Manager, Social Worker, Trainer");
        mbtiCareers.put("ENFP", "Marketing Specialist, Journalist, Counselor, Public Relations, Creative Director");
        mbtiCareers.put("ISTJ", "Accountant, Project Manager, Engineer, Doctor, Lawyer");
        mbtiCareers.put("ISFJ", "Nurse, Teacher, Social Worker, Administrator, Healthcare Worker");
        mbtiCareers.put("ESTJ", "Manager, Administrator, Lawyer, Engineer, Financial Analyst");
        mbtiCareers.put("ESFJ", "Teacher, Nurse, Administrator, Social Worker, HR Specialist");
        mbtiCareers.put("ISTP", "Engineer, Mechanic, Technician, Architect, Programmer");
        mbtiCareers.put("ISFP", "Artist, Designer, Musician, Therapist, Healthcare Worker");
        mbtiCareers.put("ESTP", "Sales Representative, Manager, Entrepreneur, Emergency Responder, Athlete");
        mbtiCareers.put("ESFP", "Teacher, Entertainer, Social Worker, Sales Representative, Event Coordinator");

        String baseCareers = mbtiCareers.getOrDefault(mbtiType, "Various career paths available based on interests and skills");

        // Add RIASEC-specific career suggestions
        if (riasecCode.contains("R")) baseCareers += "; Technician, Engineer, Mechanic";
        if (riasecCode.contains("I")) baseCareers += "; Researcher, Analyst, Scientist";
        if (riasecCode.contains("A")) baseCareers += "; Artist, Designer, Creative Professional";
        if (riasecCode.contains("S")) baseCareers += "; Teacher, Counselor, Healthcare Worker";
        if (riasecCode.contains("E")) baseCareers += "; Manager, Entrepreneur, Sales Professional";
        if (riasecCode.contains("C")) baseCareers += "; Administrator, Accountant, Data Specialist";

        return baseCareers;
    }

    // DTO class
    public static class TestResultDTO {
        private Long id;
        private Long userId;
        private UUID guestToken;
        private UUID sessionId;
        private String mbtiType;
        private String riasecCode;
        private String coursePath;
        private String careerSuggestions;
        private String learningStyle;
        private String studyTips;
        private String personalityGrowthTips;
        private String studentGoals;
        private Integer age;
        private String gender;
        private Boolean isFromPLMar;
        private java.time.LocalDateTime generatedAt;
        private java.time.LocalDateTime takenAt;
        private List<MbtiRiasecMapping> courseRecommendations;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public UUID getGuestToken() { return guestToken; }
        public void setGuestToken(UUID guestToken) { this.guestToken = guestToken; }
        
        public UUID getSessionId() { return sessionId; }
        public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
        
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public String getCoursePath() { return coursePath; }
        public void setCoursePath(String coursePath) { this.coursePath = coursePath; }
        
        public String getCareerSuggestions() { return careerSuggestions; }
        public void setCareerSuggestions(String careerSuggestions) { this.careerSuggestions = careerSuggestions; }
        
        public String getLearningStyle() { return learningStyle; }
        public void setLearningStyle(String learningStyle) { this.learningStyle = learningStyle; }
        
        public String getStudyTips() { return studyTips; }
        public void setStudyTips(String studyTips) { this.studyTips = studyTips; }
        
        public String getPersonalityGrowthTips() { return personalityGrowthTips; }
        public void setPersonalityGrowthTips(String personalityGrowthTips) { this.personalityGrowthTips = personalityGrowthTips; }
        
        public String getStudentGoals() { return studentGoals; }
        public void setStudentGoals(String studentGoals) { this.studentGoals = studentGoals; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public Boolean getIsFromPLMar() { return isFromPLMar; }
        public void setIsFromPLMar(Boolean isFromPLMar) { this.isFromPLMar = isFromPLMar; }
        
        public java.time.LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(java.time.LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public java.time.LocalDateTime getTakenAt() { return takenAt; }
        public void setTakenAt(java.time.LocalDateTime takenAt) { this.takenAt = takenAt; }

        public List<MbtiRiasecMapping> getCourseRecommendations() { return courseRecommendations; }
        public void setCourseRecommendations(List<MbtiRiasecMapping> courseRecommendations) { this.courseRecommendations = courseRecommendations; }
    }
    
    /**
     * Get TestResult by session ID
     * This method retrieves a TestResult by its session ID
     */
    public Optional<TestResult> getTestResultBySessionId(String sessionId) {
        try {
            UUID uuid = UUID.fromString(sessionId);
            return testResultRepository.findBySessionId(uuid);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid session ID format: {}", sessionId);
            return Optional.empty();
        }
    }
    
    /**
     * Get all personality test scores for debugging
     */
    public List<com.app.models.PersonalityTestScores> getAllPersonalityTestScores() {
        return enhancedScoringService.getAllPersonalityTestScores();
    }
}
