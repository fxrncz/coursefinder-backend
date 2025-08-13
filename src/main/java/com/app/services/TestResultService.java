package com.app.services;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.models.TestResult;
import com.app.models.MbtiRiasecMapping;
import com.app.repositories.TestResultRepository;
import com.app.repositories.MbtiRiasecMappingRepository;
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
        // Calculate personality scores
        Map<Integer, Integer> answers = submission.getAnswers();
        
        // Calculate RIASEC scores
        Map<String, Integer> riasecScores = calculateRIASECScores(answers);
        String riasecTopTwo = getRIASECTopTwo(riasecScores);
        
        // Calculate MBTI scores
        Map<String, Integer> mbtiScores = calculateMBTIScores(answers);
        String mbtiType = getMBTIType(mbtiScores);
        
        // Get course recommendations
        List<MbtiRiasecMapping> courseRecommendations = getCourseRecommendations(mbtiType, riasecTopTwo);
        
        // Generate content for the test result
        String coursePath = generateCoursePath(courseRecommendations);
        String careerSuggestions = generateCareerSuggestions(courseRecommendations);
        String learningStyle = generateLearningStyle(submission.getGoalSettings());
        String studyTips = generateStudyTips(mbtiType, riasecTopTwo);
        String personalityGrowthTips = generatePersonalityGrowthTips(mbtiType);
        String studentGoals = generateStudentGoals(submission.getGoalSettings());
        
        // Create and save test result
        TestResult testResult = new TestResult();
        testResult.setUserId(userId);
        testResult.setGuestToken(guestToken);
        testResult.setMbtiType(mbtiType);
        testResult.setRiasecCode(riasecTopTwo);
        testResult.setCoursePath(coursePath);
        testResult.setCareerSuggestions(careerSuggestions);
        testResult.setLearningStyle(learningStyle);
        testResult.setStudyTips(studyTips);
        testResult.setPersonalityGrowthTips(personalityGrowthTips);
        testResult.setStudentGoals(studentGoals);
        
        TestResult savedResult = testResultRepository.save(testResult);
        
        // Convert to DTO and return
        return convertToDTO(savedResult, courseRecommendations, riasecScores, mbtiScores);
    }
    
    /**
     * Get test result for user
     */
    public Optional<TestResultDTO> getLatestResultForUser(Long userId) {
        Optional<TestResult> result = testResultRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
        return result.map(this::convertToDTO);
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
        Optional<TestResult> result = testResultRepository.findTopByGuestTokenOrderByGeneratedAtDesc(guestToken);
        return result.map(this::convertToDTO);
    }
    
    /**
     * Get test result by session ID
     */
    public Optional<TestResultDTO> getResultBySessionId(UUID sessionId) {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        return result.map(this::convertToDTO);
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
        logger.info("Getting course recommendations for MBTI: {} and RIASEC: {}", mbtiType, riasecCode);

        // First try to find exact matches with both MBTI and RIASEC
        List<MbtiRiasecMapping> exactMatches = mappingRepository.findByMbtiTypeAndRiasecCode(mbtiType, riasecCode);
        logger.info("Found {} exact matches for MBTI {} and RIASEC {}", exactMatches.size(), mbtiType, riasecCode);

        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }

        // If no exact matches, try MBTI only
        List<MbtiRiasecMapping> mbtiMatches = mappingRepository.findByMbtiTypeOrderByMatchScoreDesc(mbtiType);
        logger.info("Found {} MBTI-only matches for {}", mbtiMatches.size(), mbtiType);

        if (!mbtiMatches.isEmpty()) {
            return mbtiMatches;
        }

        // If still no matches, try RIASEC only
        List<MbtiRiasecMapping> riasecMatches = mappingRepository.findByRiasecCodeOrderByMatchScoreDesc(riasecCode);
        logger.info("Found {} RIASEC-only matches for {}", riasecMatches.size(), riasecCode);

        if (!riasecMatches.isEmpty()) {
            return riasecMatches;
        }

        // If still no matches, try individual RIASEC codes
        List<String> individualCodes = Arrays.asList(riasecCode.split(""));
        logger.info("Trying individual RIASEC codes: {}", individualCodes);
        List<MbtiRiasecMapping> individualMatches = mappingRepository.findByRiasecCodesOrderByMatchScore(individualCodes);
        logger.info("Found {} matches for individual RIASEC codes", individualMatches.size());

        return individualMatches;
    }
    
    private String generateCoursePath(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No specific course recommendations found. Consider exploring general programs that align with your interests.";
        }

        String coursePath = recommendations.stream()
                .limit(5)
                .map(MbtiRiasecMapping::getCourseName)
                .filter(Objects::nonNull)
                .filter(name -> !name.trim().isEmpty())
                .collect(Collectors.joining(", "));

        if (coursePath.isEmpty()) {
            // Generate default course suggestions based on MBTI and RIASEC
            MbtiRiasecMapping firstRecommendation = recommendations.get(0);
            return generateDefaultCoursePath(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        return coursePath;
    }

    private String generateCareerSuggestions(List<MbtiRiasecMapping> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Career options will vary based on your chosen field of study and personal interests.";
        }

        String careerSuggestions = recommendations.stream()
                .limit(3)
                .map(MbtiRiasecMapping::getCareerOptions)
                .filter(Objects::nonNull)
                .filter(career -> !career.trim().isEmpty())
                .collect(Collectors.joining("; "));

        if (careerSuggestions.isEmpty()) {
            // Generate default career suggestions based on MBTI and RIASEC
            MbtiRiasecMapping firstRecommendation = recommendations.get(0);
            return generateDefaultCareerSuggestions(firstRecommendation.getMbtiType(), firstRecommendation.getRiasecCode());
        }

        return careerSuggestions;
    }
    
    private String generateLearningStyle(PersonalityTestSubmissionDTO.GoalSettingAnswersDTO goalSettings) {
        if (goalSettings == null || goalSettings.getLearningStyle() == null) {
            return "Mixed learning approach";
        }
        return String.join(", ", goalSettings.getLearningStyle());
    }
    
    private String generateStudyTips(String mbtiType, String riasecCode) {
        // Generate study tips based on personality type
        return "Study tips for " + mbtiType + " with " + riasecCode + " interests: Focus on your strengths and preferred learning methods.";
    }
    
    private String generatePersonalityGrowthTips(String mbtiType) {
        return "Growth tips for " + mbtiType + ": Continue developing your natural strengths while working on areas for improvement.";
    }
    
    private String generateStudentGoals(PersonalityTestSubmissionDTO.GoalSettingAnswersDTO goalSettings) {
        if (goalSettings == null) {
            return "{}";
        }
        
        try {
            Map<String, Object> goals = new HashMap<>();
            goals.put("priority", goalSettings.getPriority());
            goals.put("environment", goalSettings.getEnvironment());
            goals.put("motivation", goalSettings.getMotivation());
            goals.put("confidence", goalSettings.getConfidence());
            return objectMapper.writeValueAsString(goals);
        } catch (JsonProcessingException e) {
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
        
        public java.time.LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(java.time.LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public java.time.LocalDateTime getTakenAt() { return takenAt; }
        public void setTakenAt(java.time.LocalDateTime takenAt) { this.takenAt = takenAt; }

        public List<MbtiRiasecMapping> getCourseRecommendations() { return courseRecommendations; }
        public void setCourseRecommendations(List<MbtiRiasecMapping> courseRecommendations) { this.courseRecommendations = courseRecommendations; }
    }
}
