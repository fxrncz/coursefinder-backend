package com.app.services;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.dto.EnhancedTestResultDTO;
import com.app.models.TestResult;
import com.app.models.MbtiRiasecMapping;
import com.app.models.MbtiDetails;
import com.app.models.RiasecDetails;
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
import org.springframework.jdbc.core.JdbcTemplate;

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
    private com.app.repositories.RiasecDetailsRepository riasecDetailsRepository;

    @Autowired
    private com.app.repositories.CourseDescriptionRepository courseDescriptionRepository;

    @Autowired
    private com.app.repositories.CareerDescriptionRepository careerDescriptionRepository;

    @Autowired
    private com.app.repositories.DevelopmentPlanRepository developmentPlanRepository;

    @Autowired
    private com.app.repositories.CourseDevelopmentPlanRepository courseDevelopmentPlanRepository;

    @Autowired
    private com.app.repositories.CareerInfoRepository careerInfoRepository;

    @Autowired
    private EnhancedScoringService enhancedScoringService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


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
        
        // Removed database health checks to avoid pre-insert failures in transaction
        
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
            
            // Build recommendations from mbti_riasec_matching
            List<MbtiRiasecMapping> courseRecommendations = Collections.emptyList();
            String coursePath;
            String careerSuggestions;
            
            try {
                // Try exact MBTI+RIASEC from matching; fallback to MBTI-only, then RIASEC-only
                try {
                    List<Object[]> exact = mappingRepository.fetchArraysForExact(mbtiType, riasecTopTwo);
                    if (!exact.isEmpty()) {
                        Object[] row = exact.get(0);
                        List<String> courseList = extractTextArray(row[0]);
                        List<String> careerList = extractTextArray(row[1]);
                        String explanation = row.length > 2 && row[2] != null ? String.valueOf(row[2]) : null;
                        coursePath = buildNameDescriptionPairs(courseList, mbtiType, riasecTopTwo, true);
                        careerSuggestions = buildNameDescriptionPairs(careerList, mbtiType, riasecTopTwo, false);
                        // Attach explanation to student goals blob for frontend overview insights
                        if (explanation != null && !explanation.isEmpty()) {
                            submission.getGoalSettings();
                        }
                    } else {
                        List<Object[]> byMbti = mappingRepository.fetchAllByMbti(mbtiType);
                        if (!byMbti.isEmpty()) {
                            Object[] row = byMbti.get(0);
                            List<String> courseList = extractTextArray(row[0]);
                            List<String> careerList = extractTextArray(row[1]);
                            coursePath = buildNameDescriptionPairs(courseList, mbtiType, riasecTopTwo, true);
                            careerSuggestions = buildNameDescriptionPairs(careerList, mbtiType, riasecTopTwo, false);
                        } else {
                            List<Object[]> byRiasec = mappingRepository.fetchAllByRiasec(riasecTopTwo);
                            if (!byRiasec.isEmpty()) {
                                Object[] row = byRiasec.get(0);
                                List<String> courseList = extractTextArray(row[0]);
                                List<String> careerList = extractTextArray(row[1]);
                                coursePath = buildNameDescriptionPairs(courseList, mbtiType, riasecTopTwo, true);
                                careerSuggestions = buildNameDescriptionPairs(careerList, mbtiType, riasecTopTwo, false);
                            } else {
                                coursePath = generateDefaultCoursePath(mbtiType, riasecTopTwo);
                                careerSuggestions = generateDefaultCareerSuggestions(mbtiType, riasecTopTwo);
                            }
                        }
                    }
                } catch (Exception arrayErr) {
                    // Fallback to entity hydration if array casting fails
                    courseRecommendations = getCourseRecommendations(mbtiType, riasecTopTwo);
                coursePath = generateCoursePathWithDescriptions(courseRecommendations);
                careerSuggestions = generateCareerSuggestions(courseRecommendations);
                }
                
                logger.debug("Generating additional recommendations");
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
     * Safely convert a Postgres text[] column value to a List<String> regardless of JDBC driver return type.
     */
    private List<String> extractTextArray(Object columnValue) {
        try {
            if (columnValue == null) return Collections.emptyList();
            if (columnValue instanceof String[]) {
                return Arrays.asList((String[]) columnValue);
            }
            if (columnValue instanceof java.sql.Array) {
                Object arr = ((java.sql.Array) columnValue).getArray();
                if (arr instanceof Object[]) {
                    Object[] objs = (Object[]) arr;
                    List<String> out = new ArrayList<>(objs.length);
                    for (Object o : objs) {
                        if (o != null) out.add(String.valueOf(o));
                    }
                    return out;
                }
            }
            // As a last resort, parse a string like {a,b,c}
            String s = String.valueOf(columnValue).trim();
            if (s.startsWith("{") && s.endsWith("}")) {
                s = s.substring(1, s.length() - 1);
            }
            if (s.isEmpty()) return Collections.emptyList();
            String[] parts = s.split(",");
            List<String> out = new ArrayList<>(parts.length);
            for (String p : parts) {
                String v = p.trim();
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1);
                }
                if (!v.isEmpty()) out.add(v);
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // Removed merging of explanation into studentGoals to keep fields separate
    
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
            EnhancedTestResultDTO dto = convertToEnhancedDTO(testResult, mbtiDetails.orElse(null));
            // Attach detailed explanation from matching table (do not persist)
            try {
                List<Object[]> exp = mappingRepository.fetchExplanationExact(testResult.getMbtiType(), testResult.getRiasecCode());
                if (!exp.isEmpty() && exp.get(0)[0] != null) {
                    String explanation = String.valueOf(exp.get(0)[0]);
                    logger.debug("Fetched explanation for {} + {}: length={}, content={}", 
                        testResult.getMbtiType(), testResult.getRiasecCode(), 
                        explanation.length(), explanation.substring(0, Math.min(100, explanation.length())));
                    dto.setDetailedExplanation(explanation);
                } else {
                    logger.debug("No explanation found for {} + {}", testResult.getMbtiType(), testResult.getRiasecCode());
                }
            } catch (Exception e) {
                logger.warn("Error fetching explanation for {} + {}: {}", 
                    testResult.getMbtiType(), testResult.getRiasecCode(), e.getMessage());
            }
            return Optional.of(dto);
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
            EnhancedTestResultDTO dto = convertToEnhancedDTO(testResult, mbtiDetails.orElse(null));
            try {
                List<Object[]> exp = mappingRepository.fetchExplanationExact(testResult.getMbtiType(), testResult.getRiasecCode());
                if (!exp.isEmpty() && exp.get(0)[0] != null) {
                    String explanation = String.valueOf(exp.get(0)[0]);
                    logger.debug("Fetched explanation for guest {} + {}: length={}, content={}", 
                        testResult.getMbtiType(), testResult.getRiasecCode(), 
                        explanation.length(), explanation.substring(0, Math.min(100, explanation.length())));
                    dto.setDetailedExplanation(explanation);
                } else {
                    logger.debug("No explanation found for guest {} + {}", testResult.getMbtiType(), testResult.getRiasecCode());
                }
            } catch (Exception e) {
                logger.warn("Error fetching explanation for guest {} + {}: {}", 
                    testResult.getMbtiType(), testResult.getRiasecCode(), e.getMessage());
            }
            return Optional.of(dto);
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
     * Get test result entity by session ID (for authorization checks)
     */
    public Optional<TestResult> getTestResultBySessionId(UUID sessionId) {
        return testResultRepository.findBySessionId(sessionId);
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
     * Debug method to check explanation field content and length
     */
    public Map<String, Object> debugExplanationField(String mbtiType, String riasecCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Debugging explanation field for {} + {}", mbtiType, riasecCode);
            
            // 1. Check using the native query (what we actually use)
            List<Object[]> nativeResult = mappingRepository.fetchExplanationExact(mbtiType, riasecCode);
            if (!nativeResult.isEmpty() && nativeResult.get(0)[0] != null) {
                String explanation = String.valueOf(nativeResult.get(0)[0]);
                result.put("nativeQueryFound", true);
                result.put("nativeQueryLength", explanation.length());
                result.put("nativeQueryFirst100Chars", explanation.substring(0, Math.min(100, explanation.length())));
                result.put("nativeQueryLast100Chars", explanation.length() > 100 ? 
                    explanation.substring(Math.max(0, explanation.length() - 100)) : explanation);
                result.put("nativeQueryFullContent", explanation);
                
                // Count lines and paragraphs
                String[] lines = explanation.split("\n");
                result.put("nativeQueryLineCount", lines.length);
                String[] paragraphs = explanation.split("\n\s*\n");
                result.put("nativeQueryParagraphCount", paragraphs.length);
            } else {
                result.put("nativeQueryFound", false);
            }
            
            // 2. Check using JPA entity method for comparison
            List<MbtiRiasecMapping> entityResults = mappingRepository.findByMbtiTypeAndRiasecCode(mbtiType, riasecCode);
            if (!entityResults.isEmpty()) {
                MbtiRiasecMapping mapping = entityResults.get(0);
                String entityExplanation = mapping.getExplanation();
                if (entityExplanation != null) {
                    result.put("entityQueryFound", true);
                    result.put("entityQueryLength", entityExplanation.length());
                    result.put("entityQueryFirst100Chars", entityExplanation.substring(0, Math.min(100, entityExplanation.length())));
                    result.put("entityQueryFullContent", entityExplanation);
                    
                    // Check if native and entity results match
                    result.put("nativeAndEntityMatch", nativeResult.isEmpty() ? false : 
                        entityExplanation.equals(String.valueOf(nativeResult.get(0)[0])));
                } else {
                    result.put("entityQueryFound", false);
                }
            } else {
                result.put("entityQueryFound", false);
            }
            
            // 3. Check what gets set in DTO
            try {
                EnhancedTestResultDTO testDto = new EnhancedTestResultDTO();
                if (!nativeResult.isEmpty() && nativeResult.get(0)[0] != null) {
                    String explanation = String.valueOf(nativeResult.get(0)[0]);
                    testDto.setDetailedExplanation(explanation);
                    String dtoExplanation = testDto.getDetailedExplanation();
                    result.put("dtoLength", dtoExplanation != null ? dtoExplanation.length() : 0);
                    result.put("dtoFirst100Chars", dtoExplanation != null && dtoExplanation.length() > 0 ? 
                        dtoExplanation.substring(0, Math.min(100, dtoExplanation.length())) : "");
                }
            } catch (Exception e) {
                result.put("dtoError", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error in debugExplanationField: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
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
    @Deprecated
    @SuppressWarnings("unused")
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
    
    @Deprecated
    @SuppressWarnings("unused")
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
    
    @Deprecated
    @SuppressWarnings("unused")
    private String getRIASECTopTwo(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining());
    }
    
    @Deprecated
    @SuppressWarnings("unused")
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
    
    @Deprecated
    @SuppressWarnings("unused")
    private String generateCoursePath(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        String coursePath = recommendations.stream()
                .limit(1)
                .map(m -> m.getCourses())
                .filter(Objects::nonNull)
                .filter(arr -> arr.length > 0)
                .map(arr -> String.join(", ", arr))
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
    @Deprecated
    @SuppressWarnings("unused")
    private String generateSimpleCoursePath(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        MbtiRiasecMapping firstRecommendation = recommendations.get(0);
        String[] suggestedCoursesArr = firstRecommendation.getCourses();
        
        if (suggestedCoursesArr == null || suggestedCoursesArr.length == 0) {
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        // Return simple course list without complex descriptions
        return String.join("; ", suggestedCoursesArr);
    }

    /**
     * Generate simple career suggestions without complex descriptions (for performance)
     */
    @Deprecated
    @SuppressWarnings("unused")
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
        String[] suggestedCourses = firstRecommendation.getCourses();
        
        if (suggestedCourses == null || suggestedCourses.length == 0) {
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        // Split courses and generate descriptions
        String[] courses = suggestedCourses;
        StringBuilder coursePathWithDescriptions = new StringBuilder();
        
        for (int i = 0; i < courses.length; i++) {
            String course = courses[i].trim();
            if (!course.isEmpty()) {
                String description = lookupCourseDescriptionFlexible(course)
                        .orElseGet(() -> generateCourseDescription(course, firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode()));
                coursePathWithDescriptions.append(course).append(": ").append(description);
                if (i < courses.length - 1) {
                    coursePathWithDescriptions.append("; ");
                }
            }
        }

        return coursePathWithDescriptions.toString();
    }

    private String buildNameDescriptionPairs(List<String> names, String mbtiType, String riasecCode, boolean isCourse) {
        if (names == null || names.isEmpty()) return "";
        List<String> out = new ArrayList<>();
        for (String raw : names) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;
            Optional<String> desc = isCourse ? lookupCourseDescriptionFlexible(name) : lookupCareerDescriptionFlexible(name);
            String value = desc.filter(d -> !d.isEmpty()).orElseGet(() ->
                    isCourse ? generateCourseDescription(name, mbtiType, riasecCode) : name
            );
            out.add(isCourse ? (name + ": " + value) : (value.equals(name) ? name : (name + ": " + value)));
        }
        return String.join("; ", out);
    }

    /**
     * Generate a unique description for a specific course based on course name and personality type
     */
    private String generateCourseDescription(String courseName, String mbtiType, String riasecCode) {
        // Prefer DB-backed description via flexible native queries; fall back to dynamic generator
        try {
            logger.debug("Looking up course description (flexible) for: '{}'", courseName);
            Optional<String> exact = courseDescriptionRepository.findDescriptionByCourseNameExact(courseName)
                    .or(() -> courseDescriptionRepository.findDescriptionByCourseExact(courseName));
            if (exact.isPresent()) {
                String d = exact.get();
                logger.debug("Found exact course description for '{}': {}", courseName, d.substring(0, Math.min(100, d.length())) + "...");
                return d;
            }
            Optional<String> partial = courseDescriptionRepository.findDescriptionByCourseNamePartial(courseName)
                    .or(() -> courseDescriptionRepository.findDescriptionByCoursePartial(courseName));
            if (partial.isPresent()) {
                String d = partial.get();
                logger.debug("Found partial course description for '{}': {}", courseName, d.substring(0, Math.min(100, d.length())) + "...");
                return d;
            }
        } catch (Exception e) {
            logger.warn("Flexible course description lookup failed for '{}': {}", courseName, e.getMessage());
        }
        logger.debug("No DB description for '{}', using dynamic description", courseName);
        return generateDynamicCourseDescription(courseName, mbtiType, riasecCode);
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
        String[] careersArr = Optional.ofNullable(first.getCareers()).orElse(new String[]{});
        if (careersArr.length == 0) {
            logger.debug("No career suggestions found in mapping, using default");
            return generateDefaultCareerSuggestions(first.getMbtiType(), first.getRiasecCode());
        }

        logger.debug("Processing career suggestions array, count: {}", careersArr.length);
        String[] careers = careersArr;
        List<String> withDescriptions = new ArrayList<>();
        
        for (String c : careers) {
            String name = c.trim();
            if (name.isEmpty()) continue;
            
            logger.debug("Looking up career description for: '{}'", name);
            try {
            String desc = lookupCareerDescriptionFlexible(name).orElse("");
                
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

    private Optional<String> lookupCourseDescriptionFlexible(String courseName) {
        try {
            return safeFindCourseDescription(courseName);
        } catch (Exception e) {
            logger.warn("Course description lookup failed for '{}': {}", courseName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> lookupCareerDescriptionFlexible(String careerName) {
        try {
            return safeFindCareerDescription(careerName);
        } catch (Exception e) {
            logger.warn("Career description lookup failed for '{}': {}", careerName, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName.toLowerCase(), columnName.toLowerCase());
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.warn("Failed to check column existence for {}.{}: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    private Optional<String> safeFindCourseDescription(String name) {
        String table = "updated_course_description";
        boolean hasCourseName = columnExists(table, "course_name");
        boolean hasCourse = columnExists(table, "course");
        if (!hasCourseName && !hasCourse) {
            return Optional.empty();
        }
        try {
            String column = hasCourseName ? "course_name" : "course";
            String sql = "SELECT description FROM " + table + " WHERE lower(" + column + ") = lower(?) LIMIT 1";
            String desc = jdbcTemplate.query(sql, ps -> ps.setString(1, name), rs -> rs.next() ? rs.getString(1) : null);
            if (desc != null && !desc.isEmpty()) return Optional.of(desc);
            // Partial fallback
            String likeSql = "SELECT description FROM " + table + " WHERE lower(" + column + ") LIKE lower(?) LIMIT 1";
            String descLike = jdbcTemplate.query(likeSql, ps -> ps.setString(1, "%" + name + "%"), rs -> rs.next() ? rs.getString(1) : null);
            return Optional.ofNullable(descLike);
        } catch (Exception e) {
            logger.warn("safeFindCourseDescription failed for '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> safeFindCareerDescription(String name) {
        String table = "updated_career_description";
        boolean hasCareerName = columnExists(table, "career_name");
        boolean hasCareer = columnExists(table, "career");
        if (!hasCareerName && !hasCareer) {
            return Optional.empty();
        }
        try {
            String column = hasCareerName ? "career_name" : "career";
            String sql = "SELECT description FROM " + table + " WHERE lower(" + column + ") = lower(?) LIMIT 1";
            String desc = jdbcTemplate.query(sql, ps -> ps.setString(1, name), rs -> rs.next() ? rs.getString(1) : null);
            if (desc != null && !desc.isEmpty()) return Optional.of(desc);
            // Partial fallback
            String likeSql = "SELECT description FROM " + table + " WHERE lower(" + column + ") LIKE lower(?) LIMIT 1";
            String descLike = jdbcTemplate.query(likeSql, ps -> ps.setString(1, "%" + name + "%"), rs -> rs.next() ? rs.getString(1) : null);
            return Optional.ofNullable(descLike);
        } catch (Exception e) {
            logger.warn("safeFindCareerDescription failed for '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }
    
    // Legacy helper removed
    @Deprecated
    @SuppressWarnings("unused")
    private String generateLearningStyle(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // Get from detailed MBTI information
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
    
    // Legacy helper removed
    @Deprecated
    @SuppressWarnings("unused")
    private String generateStudyTips(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // Get from detailed MBTI information
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
    
    // Legacy helper removed
    @Deprecated
    @SuppressWarnings("unused")
    private String generatePersonalityGrowthTips(List<MbtiRiasecMapping> recommendations, String mbtiType) {
        // Try explanation from mapping
        if (recommendations != null && !recommendations.isEmpty()) {
            String explanation = recommendations.get(0).getExplanation();
            if (explanation != null && !explanation.trim().isEmpty()) {
                return explanation;
            }
        }
        
        // Otherwise get general growth challenges from detailed MBTI information
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
        // Removed learningStyle, studyTips, personalityGrowthTips from persisted results
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
     * Map RIASEC letter code to full name
     * R -> Realistic, I -> Investigative, A -> Artistic, 
     * S -> Social, E -> Enterprising, C -> Conventional
     */
    private String mapRiasecLetterToFullName(String letter) {
        switch (letter.toUpperCase()) {
            case "R":
                return "Realistic";
            case "I":
                return "Investigative";
            case "A":
                return "Artistic";
            case "S":
                return "Social";
            case "E":
                return "Enterprising";
            case "C":
                return "Conventional";
            default:
                logger.warn("Unknown RIASEC letter code: {}", letter);
                return letter; // Return as-is if unknown
        }
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
        // Removed learningStyle, studyTips, personalityGrowthTips from persisted results
        dto.setStudentGoals(entity.getStudentGoals());
        dto.setAge(entity.getAge());
        dto.setGender(entity.getGender());
        dto.setIsFromPLMar(entity.getIsFromPLMar());
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setTakenAt(entity.getTakenAt());
        
        // Add detailed MBTI information if available
        if (mbtiDetails != null) {
            dto.setDetailedMbtiInfo(new EnhancedTestResultDTO.DetailedMbtiInfoDTO(mbtiDetails));
        }
        
        // Add detailed RIASEC information for TOP 1 RIASEC code
        try {
            String riasecCode = entity.getRiasecCode();
            if (riasecCode != null && !riasecCode.isEmpty()) {
                // Extract first character (TOP 1 RIASEC code)
                String topRiasecLetter = riasecCode.substring(0, 1);
                
                // Map letter to full name
                String topRiasecType = mapRiasecLetterToFullName(topRiasecLetter);
                logger.debug("Fetching RIASEC details for top code: {} ({})", topRiasecLetter, topRiasecType);
                
                Optional<RiasecDetails> riasecDetails = riasecDetailsRepository.findByRiasecType(topRiasecType);
                if (riasecDetails.isPresent()) {
                    dto.setDetailedRiasecInfo(new EnhancedTestResultDTO.DetailedRiasecInfoDTO(riasecDetails.get()));
                    logger.debug("Added RIASEC details for type: {} ({})", topRiasecLetter, topRiasecType);
                } else {
                    logger.warn("No RIASEC details found for type: {} ({})", topRiasecLetter, topRiasecType);
                }
            }
        } catch (Exception e) {
            logger.warn("Error fetching RIASEC details: {}", e.getMessage());
        }
        
        // Generate and add career development plan
        try {
            com.app.dto.CareerDevelopmentPlanDTO careerDevelopmentPlan = 
                generateCareerDevelopmentPlan(entity.getMbtiType(), entity.getRiasecCode());
            dto.setCareerDevelopmentPlan(careerDevelopmentPlan);
            logger.debug("Added career development plan to enhanced result for MBTI: {}, RIASEC: {}", 
                entity.getMbtiType(), entity.getRiasecCode());
        } catch (Exception e) {
            logger.warn("Error generating career development plan for enhanced result: {}", e.getMessage());
            // Set empty plan on error
            dto.setCareerDevelopmentPlan(new com.app.dto.CareerDevelopmentPlanDTO(
                entity.getMbtiType(), entity.getRiasecCode(), new ArrayList<>()));
        }
        
        // Generate and add course development plan
        try {
            com.app.dto.CourseDevelopmentPlanDTO courseDevelopmentPlan = 
                generateCourseDevelopmentPlan(entity.getMbtiType(), entity.getRiasecCode());
            dto.setCourseDevelopmentPlan(courseDevelopmentPlan);
            logger.debug("Added course development plan to enhanced result for MBTI: {}, RIASEC: {}", 
                entity.getMbtiType(), entity.getRiasecCode());
        } catch (Exception e) {
            logger.warn("Error generating course development plan for enhanced result: {}", e.getMessage());
            // Set empty plan on error
            dto.setCourseDevelopmentPlan(new com.app.dto.CourseDevelopmentPlanDTO(
                entity.getMbtiType(), entity.getRiasecCode(), new ArrayList<>()));
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
        private String studentGoals;
        private Integer age;
        private String gender;
        private Boolean isFromPLMar;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private java.time.LocalDateTime generatedAt;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
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
     * Generate career development plan based on MBTI type and RIASEC code
     */
    private com.app.dto.CareerDevelopmentPlanDTO generateCareerDevelopmentPlan(String mbtiType, String riasecCode) {
        try {
            logger.debug("Generating career development plan for MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // First, get careers from mbti_riasec_matching
            List<String> careerNames = new ArrayList<>();
            
            // Try exact MBTI + RIASEC match first
            try {
                List<Object[]> exactMatches = mappingRepository.fetchArraysForExact(mbtiType, riasecCode);
                if (!exactMatches.isEmpty()) {
                    Object[] row = exactMatches.get(0);
                    List<String> careerList = extractTextArray(row[1]); // careers column
                    careerNames.addAll(careerList);
                    logger.debug("Found {} careers from exact match", careerList.size());
                }
            } catch (Exception e) {
                logger.warn("Error getting exact match careers: {}", e.getMessage());
            }
            
            // If no exact match, try MBTI only
            if (careerNames.isEmpty()) {
                try {
                    List<Object[]> mbtiMatches = mappingRepository.fetchAllByMbti(mbtiType);
                    if (!mbtiMatches.isEmpty()) {
                        Object[] row = mbtiMatches.get(0);
                        List<String> careerList = extractTextArray(row[1]); // careers column
                        careerNames.addAll(careerList);
                        logger.debug("Found {} careers from MBTI match", careerList.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error getting MBTI match careers: {}", e.getMessage());
                }
            }
            
            // If still no match, try RIASEC only
            if (careerNames.isEmpty()) {
                try {
                    List<Object[]> riasecMatches = mappingRepository.fetchAllByRiasec(riasecCode);
                    if (!riasecMatches.isEmpty()) {
                        Object[] row = riasecMatches.get(0);
                        List<String> careerList = extractTextArray(row[1]); // careers column
                        careerNames.addAll(careerList);
                        logger.debug("Found {} careers from RIASEC match", careerList.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error getting RIASEC match careers: {}", e.getMessage());
                }
            }
            
            // Limit to top 6 careers for development plan
            List<String> topCareers = careerNames.stream()
                .distinct()
                .limit(6)
                .collect(Collectors.toList());
            
            logger.debug("Processing {} top careers for development plan", topCareers.size());
            
            // Build career details list
            List<com.app.dto.CareerDevelopmentPlanDTO.CareerDetails> careerDetailsList = new ArrayList<>();
            
            for (String careerName : topCareers) {
                try {
                    com.app.dto.CareerDevelopmentPlanDTO.CareerDetails careerDetails = 
                        new com.app.dto.CareerDevelopmentPlanDTO.CareerDetails();
                    
                    careerDetails.setCareerName(careerName);
                    
                    // Get description from career_description table
                    Optional<String> description = lookupCareerDescriptionFlexible(careerName);
                    careerDetails.setDescription(description.orElse("No description available"));
                    
                    // Get development plan data
                    Optional<com.app.models.DevelopmentPlan> developmentPlan = 
                        developmentPlanRepository.findByCareerNameExact(careerName);
                    
                    if (!developmentPlan.isPresent()) {
                        developmentPlan = developmentPlanRepository.findByCareerNamePartial(careerName);
                    }
                    
                    if (developmentPlan.isPresent()) {
                        com.app.models.DevelopmentPlan dp = developmentPlan.get();
                        careerDetails.setIntroduction(dp.getIntroduction());
                        careerDetails.setKeySkills(dp.getKeySkills());
                        careerDetails.setAcademicsActivities(dp.getAcademicsActivities());
                        careerDetails.setSoftSkills(dp.getSoftSkills());
                        careerDetails.setGrowthOpportunities(dp.getGrowthOpportunities());
                    } else {
                        // Default values if no development plan found
                        careerDetails.setIntroduction("Career development information coming soon.");
                        careerDetails.setKeySkills("Skills information will be available soon.");
                        careerDetails.setAcademicsActivities("Academic activities information coming soon.");
                        careerDetails.setSoftSkills("Soft skills information coming soon.");
                        careerDetails.setGrowthOpportunities("Growth opportunities information coming soon.");
                    }
                    
                    // Get career info data
                    Optional<com.app.models.CareerInfo> careerInfo = 
                        careerInfoRepository.findByCareerNameExact(careerName);
                    
                    if (!careerInfo.isPresent()) {
                        careerInfo = careerInfoRepository.findByCareerNamePartial(careerName);
                    }
                    
                    if (careerInfo.isPresent()) {
                        com.app.models.CareerInfo ci = careerInfo.get();
                        careerDetails.setCareerFit(ci.getCareerFit());
                        careerDetails.setEducationLevel(ci.getEducationLevel());
                        careerDetails.setWorkEnvironment(ci.getWorkEnvironment());
                        careerDetails.setCareerPath(ci.getCareerPath());
                    } else {
                        // Default values if no career info found
                        careerDetails.setCareerFit("Career fit information coming soon.");
                        careerDetails.setEducationLevel("Education level information coming soon.");
                        careerDetails.setWorkEnvironment("Work environment information coming soon.");
                        careerDetails.setCareerPath("Career path information coming soon.");
                    }
                    
                    careerDetailsList.add(careerDetails);
                    logger.debug("Added career details for: {}", careerName);
                    
                } catch (Exception e) {
                    logger.warn("Error processing career details for '{}': {}", careerName, e.getMessage());
                }
            }
            
            // Create and return the development plan DTO
            com.app.dto.CareerDevelopmentPlanDTO developmentPlanDTO = 
                new com.app.dto.CareerDevelopmentPlanDTO(mbtiType, riasecCode, careerDetailsList);
            
            logger.debug("Successfully generated career development plan with {} careers", careerDetailsList.size());
            return developmentPlanDTO;
            
        } catch (Exception e) {
            logger.error("Error generating career development plan for MBTI: {}, RIASEC: {}", mbtiType, riasecCode, e);
            // Return empty development plan on error
            return new com.app.dto.CareerDevelopmentPlanDTO(mbtiType, riasecCode, new ArrayList<>());
        }
    }

    /**
     * Generate course development plan based on MBTI type and RIASEC code
     */
    private com.app.dto.CourseDevelopmentPlanDTO generateCourseDevelopmentPlan(String mbtiType, String riasecCode) {
        try {
            logger.debug("Generating course development plan for MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // First, get courses from mbti_riasec_matching
            List<String> courseNames = new ArrayList<>();
            
            // Try exact MBTI + RIASEC match first
            try {
                List<Object[]> exactMatches = mappingRepository.fetchArraysForExact(mbtiType, riasecCode);
                if (!exactMatches.isEmpty()) {
                    Object[] row = exactMatches.get(0);
                    List<String> courseList = extractTextArray(row[0]); // courses column (index 0)
                    courseNames.addAll(courseList);
                    logger.debug("Found {} courses from exact match", courseList.size());
                }
            } catch (Exception e) {
                logger.warn("Error getting exact match courses: {}", e.getMessage());
            }
            
            // If no exact match, try MBTI only
            if (courseNames.isEmpty()) {
                try {
                    List<Object[]> mbtiMatches = mappingRepository.fetchAllByMbti(mbtiType);
                    if (!mbtiMatches.isEmpty()) {
                        Object[] row = mbtiMatches.get(0);
                        List<String> courseList = extractTextArray(row[0]); // courses column
                        courseNames.addAll(courseList);
                        logger.debug("Found {} courses from MBTI match", courseList.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error getting MBTI match courses: {}", e.getMessage());
                }
            }
            
            // If still no match, try RIASEC only
            if (courseNames.isEmpty()) {
                try {
                    List<Object[]> riasecMatches = mappingRepository.fetchAllByRiasec(riasecCode);
                    if (!riasecMatches.isEmpty()) {
                        Object[] row = riasecMatches.get(0);
                        List<String> courseList = extractTextArray(row[0]); // courses column
                        courseNames.addAll(courseList);
                        logger.debug("Found {} courses from RIASEC match", courseList.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error getting RIASEC match courses: {}", e.getMessage());
                }
            }
            
            // Limit to top 6 courses for development plan
            List<String> topCourses = courseNames.stream()
                .distinct()
                .limit(6)
                .collect(Collectors.toList());
            
            logger.debug("Processing {} top courses for development plan", topCourses.size());
            
            // Build course details list
            List<com.app.dto.CourseDevelopmentPlanDTO.CourseDetails> courseDetailsList = new ArrayList<>();
            
            for (String courseName : topCourses) {
                try {
                    com.app.dto.CourseDevelopmentPlanDTO.CourseDetails courseDetails = 
                        new com.app.dto.CourseDevelopmentPlanDTO.CourseDetails();
                    
                    courseDetails.setCourseName(courseName);
                    
                    // Get description from updated_course_description table
                    Optional<String> description = lookupCourseDescriptionFlexible(courseName);
                    courseDetails.setDescription(description.orElse("No description available"));
                    
                    // Get development plan data from course_development_plan table
                    Optional<com.app.models.CourseDevelopmentPlan> developmentPlan = 
                        courseDevelopmentPlanRepository.findByCourseNameExact(courseName);
                    
                    if (!developmentPlan.isPresent()) {
                        developmentPlan = courseDevelopmentPlanRepository.findByCourseNamePartial(courseName);
                    }
                    
                    if (developmentPlan.isPresent()) {
                        com.app.models.CourseDevelopmentPlan cdp = developmentPlan.get();
                        courseDetails.setCourseOverview(cdp.getCourseOverview());
                        courseDetails.setCoreCompetencies(cdp.getCoreCompetencies());
                        courseDetails.setAcadsExtra(cdp.getAcadsExtra());
                        courseDetails.setSubjMaster(cdp.getSubjMaster());
                        courseDetails.setSoftSkills(cdp.getSoftSkills());
                        courseDetails.setCareerReadiness(cdp.getCareerReadiness());
                        courseDetails.setGrowth(cdp.getGrowth());
                    } else {
                        // Default values if no development plan found
                        courseDetails.setCourseOverview("Course overview information coming soon.");
                        courseDetails.setCoreCompetencies("Core competencies information will be available soon.");
                        courseDetails.setAcadsExtra("Academic and extracurricular activities information coming soon.");
                        courseDetails.setSubjMaster("Subject mastery information coming soon.");
                        courseDetails.setSoftSkills("Soft skills information coming soon.");
                        courseDetails.setCareerReadiness("Career readiness information coming soon.");
                        courseDetails.setGrowth("Growth opportunities information coming soon.");
                    }
                    
                    courseDetailsList.add(courseDetails);
                    logger.debug("Added course details for: {}", courseName);
                    
                } catch (Exception e) {
                    logger.warn("Error processing course details for '{}': {}", courseName, e.getMessage());
                }
            }
            
            // Create and return the development plan DTO
            com.app.dto.CourseDevelopmentPlanDTO developmentPlanDTO = 
                new com.app.dto.CourseDevelopmentPlanDTO(mbtiType, riasecCode, courseDetailsList);
            
            logger.debug("Successfully generated course development plan with {} courses", courseDetailsList.size());
            return developmentPlanDTO;
            
        } catch (Exception e) {
            logger.error("Error generating course development plan for MBTI: {}, RIASEC: {}", mbtiType, riasecCode, e);
            // Return empty development plan on error
            return new com.app.dto.CourseDevelopmentPlanDTO(mbtiType, riasecCode, new ArrayList<>());
        }
    }


    /**
     * Get all personality test scores for debugging
     */
    public List<com.app.models.PersonalityTestScores> getAllPersonalityTestScores() {
        return enhancedScoringService.getAllPersonalityTestScores();
    }
}
