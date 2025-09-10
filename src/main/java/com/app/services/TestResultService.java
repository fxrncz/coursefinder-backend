package com.app.services;

import com.app.dto.PersonalityTestSubmissionDTO;
import com.app.dto.EnhancedTestResultDTO;
import com.app.models.TestResult;
import com.app.models.MbtiRiasecMapping;
import com.app.models.MbtiDetails;
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
    private HybridRecommendationService hybridRecommendationService;

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
        
        // Try hybrid engine first (new normalized tables)
        List<String> topCourses = Collections.emptyList();
        List<String> topCareers = Collections.emptyList();
        try {
            topCourses = hybridRecommendationService.getTopCourseNames(mbtiType, riasecTopTwo, 10);
            topCareers = hybridRecommendationService.getTopCareerNames(mbtiType, riasecTopTwo, 10);
        } catch (Exception e) {
            // Fallback will handle if hybrid not available yet
        }

        // Legacy fallback (existing 480-table) if hybrid has no data yet
        List<MbtiRiasecMapping> courseRecommendations = Collections.emptyList();
        String coursePath;
        String careerSuggestions;
        if (topCourses != null && !topCourses.isEmpty()) {
            coursePath = buildCoursePathFromNames(topCourses, mbtiType, riasecTopTwo);
        } else {
            courseRecommendations = getCourseRecommendations(mbtiType, riasecTopTwo);
            coursePath = generateCoursePathWithDescriptions(courseRecommendations);
        }

        if (topCareers != null && !topCareers.isEmpty()) {
            careerSuggestions = buildCareerSuggestionsFromNames(topCareers);
        } else {
            if (courseRecommendations.isEmpty()) {
                courseRecommendations = getCourseRecommendations(mbtiType, riasecTopTwo);
            }
            careerSuggestions = generateCareerSuggestions(courseRecommendations);
        }
        String learningStyle = generateLearningStyle(courseRecommendations, mbtiType);
        String studyTips = generateStudyTips(courseRecommendations, mbtiType);
        String personalityGrowthTips = generatePersonalityGrowthTips(courseRecommendations, mbtiType);
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
     * Get detailed MBTI information by type
     */
    public Optional<MbtiDetails> getDetailedMbtiInformation(String mbtiType) {
        return mbtiDetailsRepository.findByMbtiType(mbtiType);
    }
    
    /**
     * Get enhanced test result with detailed MBTI information
     */
    public Optional<EnhancedTestResultDTO> getEnhancedResultForUser(Long userId) {
        Optional<TestResult> result = testResultRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            Optional<MbtiDetails> mbtiDetails = getDetailedMbtiInformation(testResult.getMbtiType());
            return Optional.of(convertToEnhancedDTO(testResult, mbtiDetails.orElse(null)));
        }
        return Optional.empty();
    }
    
    /**
     * Get enhanced test result for guest with detailed MBTI information
     */
    public Optional<EnhancedTestResultDTO> getEnhancedResultForGuest(UUID guestToken) {
        Optional<TestResult> result = testResultRepository.findTopByGuestTokenOrderByGeneratedAtDesc(guestToken);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            Optional<MbtiDetails> mbtiDetails = getDetailedMbtiInformation(testResult.getMbtiType());
            return Optional.of(convertToEnhancedDTO(testResult, mbtiDetails.orElse(null)));
        }
        return Optional.empty();
    }
    
    /**
     * Get test result by session ID
     */
    public Optional<TestResultDTO> getResultBySessionId(UUID sessionId) {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        return result.map(this::convertToDTO);
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
     * Regenerate course descriptions for existing test results
     */
    public boolean regenerateCourseDescriptions(UUID sessionId) {
        Optional<TestResult> result = testResultRepository.findBySessionId(sessionId);
        if (result.isPresent()) {
            TestResult testResult = result.get();
            
            // Prefer hybrid engine; fallback to legacy mappings
            List<String> topCourses = Collections.emptyList();
            List<String> topCareers = Collections.emptyList();
            try {
                topCourses = hybridRecommendationService.getTopCourseNames(testResult.getMbtiType(), testResult.getRiasecCode(), 10);
                topCareers = hybridRecommendationService.getTopCareerNames(testResult.getMbtiType(), testResult.getRiasecCode(), 10);
            } catch (Exception e) {
                // ignore
            }

            String newCoursePath;
            String newCareerSuggestions;
            if (topCourses != null && !topCourses.isEmpty()) {
                newCoursePath = buildCoursePathFromNames(topCourses, testResult.getMbtiType(), testResult.getRiasecCode());
            } else {
                List<MbtiRiasecMapping> courseRecommendations = getCourseRecommendations(
                    testResult.getMbtiType(), testResult.getRiasecCode());
                newCoursePath = generateCoursePathWithDescriptions(courseRecommendations);
            }

            if (topCareers != null && !topCareers.isEmpty()) {
                newCareerSuggestions = buildCareerSuggestionsFromNames(topCareers);
            } else {
                List<MbtiRiasecMapping> courseRecommendations = getCourseRecommendations(
                    testResult.getMbtiType(), testResult.getRiasecCode());
                newCareerSuggestions = generateCareerSuggestions(courseRecommendations);
            }
            
            // Update the test result
            testResult.setCoursePath(newCoursePath);
            testResult.setCareerSuggestions(newCareerSuggestions);
            testResultRepository.save(testResult);
            
            return true;
        }
        return false;
    }

    // Build course path string from ranked course names using dynamic descriptions
    private String buildCoursePathFromNames(List<String> courseNames, String mbtiType, String riasecCode) {
        if (courseNames == null || courseNames.isEmpty()) {
            return generateDefaultCoursePath(mbtiType, riasecCode);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < courseNames.size(); i++) {
            String course = Optional.ofNullable(courseNames.get(i)).orElse("").trim();
            if (course.isEmpty()) continue;
            String description = generateCourseDescription(course, mbtiType, riasecCode);
            sb.append(course).append(": ").append(description);
            if (i < courseNames.size() - 1) {
                sb.append("; ");
            }
        }
        return sb.length() > 0 ? sb.toString() : generateDefaultCoursePath(mbtiType, riasecCode);
    }

    // Build career suggestions string from ranked career names, enriching with descriptions if present
    private String buildCareerSuggestionsFromNames(List<String> careerNames) {
        if (careerNames == null || careerNames.isEmpty()) {
            return "";
        }
        List<String> withDescriptions = new ArrayList<>();
        for (String raw : careerNames) {
            String name = Optional.ofNullable(raw).orElse("").trim();
            if (name.isEmpty()) continue;
            String desc = careerDescriptionRepository.findByCareerName(name)
                    .map(com.app.models.CareerDescription::getDescription)
                    .or(() -> careerDescriptionRepository.findFirstByCareerNameIgnoreCase(name).map(com.app.models.CareerDescription::getDescription))
                    .or(() -> careerDescriptionRepository.findFirstByCareerNameContainingIgnoreCase(name).map(com.app.models.CareerDescription::getDescription))
                    .orElse("");
            if (!desc.isEmpty()) {
                withDescriptions.add(name + ": " + desc);
            } else {
                withDescriptions.add(name);
            }
        }
        return String.join("; ", withDescriptions);
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
        List<MbtiRiasecMapping> mbtiMatches = mappingRepository.findByMbtiType(mbtiType);
        logger.info("Found {} MBTI-only matches for {}", mbtiMatches.size(), mbtiType);

        if (!mbtiMatches.isEmpty()) {
            return mbtiMatches;
        }

        // If still no matches, try RIASEC only
        List<MbtiRiasecMapping> riasecMatches = mappingRepository.findByRiasecCode(riasecCode);
        logger.info("Found {} RIASEC-only matches for {}", riasecMatches.size(), riasecCode);

        if (!riasecMatches.isEmpty()) {
            return riasecMatches;
        }

        // If still no matches, try individual RIASEC codes
        List<String> individualCodes = Arrays.asList(riasecCode.split(""));
        logger.info("Trying individual RIASEC codes: {}", individualCodes);
        List<MbtiRiasecMapping> individualMatches = mappingRepository.findByRiasecCodesOrderById(individualCodes);
        logger.info("Found {} matches for individual RIASEC codes", individualMatches.size());

        return individualMatches;
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
            return courseDescriptionRepository.findByCourseName(courseName)
                .map(com.app.models.CourseDescription::getDescription)
                .orElseGet(() -> generateDynamicCourseDescription(courseName, mbtiType, riasecCode));
        } catch (Exception e) {
            // If repository not available or any error, use dynamic fallback
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
            return "Career options will vary based on your chosen field of study and personal interests.";
        }

        // Use first mapping's career list, then expand with DB-backed descriptions if needed
        MbtiRiasecMapping first = recommendations.get(0);
        String raw = Optional.ofNullable(first.getCareerSuggestions()).orElse("").trim();
        if (raw.isEmpty()) {
            return generateDefaultCareerSuggestions(first.getMbtiType(), first.getRiasecCode());
        }

        String[] careers = raw.split(",");
        List<String> withDescriptions = new ArrayList<>();
        for (String c : careers) {
            String name = c.trim();
            if (name.isEmpty()) continue;
            String desc = careerDescriptionRepository.findByCareerName(name)
                    .map(com.app.models.CareerDescription::getDescription)
                    .or(() -> careerDescriptionRepository.findFirstByCareerNameIgnoreCase(name).map(com.app.models.CareerDescription::getDescription))
                    .or(() -> careerDescriptionRepository.findFirstByCareerNameContainingIgnoreCase(name).map(com.app.models.CareerDescription::getDescription))
                    .orElse("");
            if (!desc.isEmpty()) {
                withDescriptions.add(name + ": " + desc);
            } else {
                withDescriptions.add(name);
            }
        }
        return String.join("; ", withDescriptions);
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
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getLearningStyleSummary();
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
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getStudyTipsSummary();
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
        Optional<MbtiDetails> mbtiDetails = mbtiDetailsRepository.findByMbtiType(mbtiType);
        if (mbtiDetails.isPresent()) {
            return mbtiDetails.get().getGrowthChallenges();
        }
        
        return "Continue developing your natural strengths while working on areas for improvement.";
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
