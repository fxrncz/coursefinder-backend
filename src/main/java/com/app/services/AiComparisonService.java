package com.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered comparison and ranking service for courses and careers
 * Uses Hugging Face PRO account to intelligently compare and rank recommendations
 */
@Service
public class AiComparisonService {
    
    private static final Logger log = LoggerFactory.getLogger(AiComparisonService.class);
    
    @Autowired
    private HuggingFaceApiService huggingFaceService;
    
    @Value("${huggingface.validation.enabled:false}")
    private boolean aiEnabled;
    
    @Value("${huggingface.model.validation}")
    private String validationModel;
    
    /**
     * AI-powered course comparison and ranking
     * Compares all courses and returns them ranked by AI-calculated fit
     */
    public List<CourseRanking> rankCoursesByPersonality(String mbtiType, String riasecCode, String coursePath) {
        if (!aiEnabled) {
            log.info("ℹ️ AI ranking disabled - using default ranking");
            return getDefaultCourseRanking(coursePath);
        }
        
        try {
            log.info("🤖 AI: Ranking courses for MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // Parse courses from coursePath
            List<String> courses = parseCourses(coursePath);
            
            if (courses.isEmpty()) {
                log.warn("⚠️ No courses to rank");
                return new ArrayList<>();
            }
            
            // Build AI prompt for course comparison
            String prompt = buildCourseComparisonPrompt(mbtiType, riasecCode, courses);
            
            // Call AI to analyze and rank courses
            String aiResponse = huggingFaceService.generateText(validationModel, prompt);
            
            // Parse AI response to get rankings
            List<CourseRanking> rankings = parseAiCourseRankings(aiResponse, courses, mbtiType, riasecCode);
            
            log.info("✅ AI ranked {} courses successfully", rankings.size());
            return rankings;
            
        } catch (Exception e) {
            log.error("❌ AI ranking failed: {}", e.getMessage(), e);
            return getDefaultCourseRanking(coursePath);
        }
    }
    
    /**
     * AI-powered career comparison and ranking
     * Compares all careers and returns them ranked by AI-calculated fit
     */
    public List<CareerRanking> rankCareersByPersonality(String mbtiType, String riasecCode, String careerSuggestions) {
        if (!aiEnabled) {
            log.info("ℹ️ AI ranking disabled - using default ranking");
            return getDefaultCareerRanking(careerSuggestions);
        }
        
        try {
            log.info("🤖 AI: Ranking careers for MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // Parse careers from careerSuggestions
            List<String> careers = parseCareers(careerSuggestions);
            
            if (careers.isEmpty()) {
                log.warn("⚠️ No careers to rank");
                return new ArrayList<>();
            }
            
            // Build AI prompt for career comparison
            String prompt = buildCareerComparisonPrompt(mbtiType, riasecCode, careers);
            
            // Call AI to analyze and rank careers
            String aiResponse = huggingFaceService.generateText(validationModel, prompt);
            
            // Parse AI response to get rankings
            List<CareerRanking> rankings = parseAiCareerRankings(aiResponse, careers, mbtiType, riasecCode);
            
            log.info("✅ AI ranked {} careers successfully", rankings.size());
            return rankings;
            
        } catch (Exception e) {
            log.error("❌ AI ranking failed: {}", e.getMessage(), e);
            return getDefaultCareerRanking(careerSuggestions);
        }
    }
    
    /**
     * Build AI prompt for course comparison
     */
    private String buildCourseComparisonPrompt(String mbtiType, String riasecCode, List<String> courses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert career counselor and educational advisor.\n\n");
        prompt.append(String.format("Personality Profile: MBTI Type %s, RIASEC Code %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Analyze and rank these courses from BEST to WORST fit for this personality:\n\n");
        
        for (int i = 0; i < courses.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, courses.get(i)));
        }
        
        prompt.append("\nFor each course, provide:\n");
        prompt.append("1. Rank (1 = best fit, higher = worse fit)\n");
        prompt.append("2. Match Score (0-100%)\n");
        prompt.append("3. Brief reason WHY it's a good/bad fit\n\n");
        prompt.append("Format: RANK|COURSE_NAME|SCORE|REASON\n");
        prompt.append("Example: 1|BS Computer Science|95|Excellent for analytical INTJ minds\n");
        
        return prompt.toString();
    }
    
    /**
     * Build AI prompt for career comparison
     */
    private String buildCareerComparisonPrompt(String mbtiType, String riasecCode, List<String> careers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert career counselor specializing in personality-career matching.\n\n");
        prompt.append(String.format("Personality Profile: MBTI Type %s, RIASEC Code %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Analyze and rank these careers from BEST to WORST fit for this personality:\n\n");
        
        for (int i = 0; i < careers.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, careers.get(i)));
        }
        
        prompt.append("\nFor each career, provide:\n");
        prompt.append("1. Rank (1 = best fit, higher = worse fit)\n");
        prompt.append("2. Match Score (0-100%)\n");
        prompt.append("3. Brief reason WHY it's a good/bad fit\n\n");
        prompt.append("Format: RANK|CAREER_NAME|SCORE|REASON\n");
        prompt.append("Example: 1|Software Engineer|92|Perfect for logical problem-solvers\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse courses from coursePath string
     */
    private List<String> parseCourses(String coursePath) {
        if (coursePath == null || coursePath.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Split by semicolon or comma
        String[] parts = coursePath.contains(";") ? 
            coursePath.split(";") : coursePath.split(",");
        
        return Arrays.stream(parts)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.split(":")[0].trim()) // Extract course name only
            .collect(Collectors.toList());
    }
    
    /**
     * Parse careers from careerSuggestions string
     */
    private List<String> parseCareers(String careerSuggestions) {
        if (careerSuggestions == null || careerSuggestions.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Split by semicolon or comma
        String[] parts = careerSuggestions.contains(";") ? 
            careerSuggestions.split(";") : careerSuggestions.split(",");
        
        return Arrays.stream(parts)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.split(":")[0].trim()) // Extract career name only
            .collect(Collectors.toList());
    }
    
    /**
     * Parse AI response for course rankings
     */
    private List<CourseRanking> parseAiCourseRankings(String aiResponse, List<String> courses, String mbtiType, String riasecCode) {
        List<CourseRanking> rankings = new ArrayList<>();
        
        try {
            // Try to parse structured AI response
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        try {
                            int rank = Integer.parseInt(parts[0].trim());
                            String courseName = parts[1].trim();
                            double score = Double.parseDouble(parts[2].trim().replace("%", ""));
                            String reason = parts[3].trim();
                            
                            rankings.add(new CourseRanking(rank, courseName, score, reason, mbtiType, riasecCode));
                        } catch (NumberFormatException e) {
                            log.debug("Could not parse AI ranking line: {}", line);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing AI course rankings: {}", e.getMessage());
        }
        
        // If parsing failed or no rankings, use intelligent default
        if (rankings.isEmpty()) {
            rankings = generateIntelligentCourseRankings(courses, mbtiType, riasecCode);
        }
        
        // Sort by rank
        rankings.sort(Comparator.comparingInt(CourseRanking::getRank));
        
        return rankings;
    }
    
    /**
     * Parse AI response for career rankings
     */
    private List<CareerRanking> parseAiCareerRankings(String aiResponse, List<String> careers, String mbtiType, String riasecCode) {
        List<CareerRanking> rankings = new ArrayList<>();
        
        try {
            // Try to parse structured AI response
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        try {
                            int rank = Integer.parseInt(parts[0].trim());
                            String careerName = parts[1].trim();
                            double score = Double.parseDouble(parts[2].trim().replace("%", ""));
                            String reason = parts[3].trim();
                            
                            rankings.add(new CareerRanking(rank, careerName, score, reason, mbtiType, riasecCode));
                        } catch (NumberFormatException e) {
                            log.debug("Could not parse AI ranking line: {}", line);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing AI career rankings: {}", e.getMessage());
        }
        
        // If parsing failed or no rankings, use intelligent default
        if (rankings.isEmpty()) {
            rankings = generateIntelligentCareerRankings(careers, mbtiType, riasecCode);
        }
        
        // Sort by rank
        rankings.sort(Comparator.comparingInt(CareerRanking::getRank));
        
        return rankings;
    }
    
    /**
     * Generate intelligent course rankings using personality matching algorithms
     */
    private List<CourseRanking> generateIntelligentCourseRankings(List<String> courses, String mbtiType, String riasecCode) {
        List<CourseRanking> rankings = new ArrayList<>();
        
        for (int i = 0; i < courses.size(); i++) {
            String course = courses.get(i);
            double score = calculateCourseMatchScore(course, mbtiType, riasecCode);
            String reason = generateCourseMatchReason(course, mbtiType, riasecCode, score);
            
            rankings.add(new CourseRanking(i + 1, course, score, reason, mbtiType, riasecCode));
        }
        
        // Sort by score (descending) and reassign ranks
        rankings.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }
        
        return rankings;
    }
    
    /**
     * Generate intelligent career rankings using personality matching algorithms
     */
    private List<CareerRanking> generateIntelligentCareerRankings(List<String> careers, String mbtiType, String riasecCode) {
        List<CareerRanking> rankings = new ArrayList<>();
        
        for (int i = 0; i < careers.size(); i++) {
            String career = careers.get(i);
            double score = calculateCareerMatchScore(career, mbtiType, riasecCode);
            String reason = generateCareerMatchReason(career, mbtiType, riasecCode, score);
            
            rankings.add(new CareerRanking(i + 1, career, score, reason, mbtiType, riasecCode));
        }
        
        // Sort by score (descending) and reassign ranks
        rankings.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }
        
        return rankings;
    }
    
    /**
     * Calculate course match score based on personality
     */
    private double calculateCourseMatchScore(String course, String mbtiType, String riasecCode) {
        double baseScore = 75.0; // Base score
        
        // Adjust based on MBTI preferences
        if (mbtiType.contains("I")) baseScore += course.toLowerCase().contains("research") ? 5 : 0;
        if (mbtiType.contains("E")) baseScore += course.toLowerCase().contains("business") ? 5 : 0;
        if (mbtiType.contains("N")) baseScore += course.toLowerCase().contains("science") ? 5 : 0;
        if (mbtiType.contains("S")) baseScore += course.toLowerCase().contains("engineering") ? 5 : 0;
        if (mbtiType.contains("T")) baseScore += course.toLowerCase().contains("computer") ? 5 : 0;
        if (mbtiType.contains("F")) baseScore += course.toLowerCase().contains("psychology") ? 5 : 0;
        
        // Adjust based on RIASEC code
        char primaryRiasec = riasecCode.charAt(0);
        switch (primaryRiasec) {
            case 'R': baseScore += course.toLowerCase().contains("engineering") ? 10 : 0; break;
            case 'I': baseScore += course.toLowerCase().contains("science") ? 10 : 0; break;
            case 'A': baseScore += course.toLowerCase().contains("art") ? 10 : 0; break;
            case 'S': baseScore += course.toLowerCase().contains("social") ? 10 : 0; break;
            case 'E': baseScore += course.toLowerCase().contains("business") ? 10 : 0; break;
            case 'C': baseScore += course.toLowerCase().contains("accounting") ? 10 : 0; break;
        }
        
        return Math.min(100.0, baseScore);
    }
    
    /**
     * Calculate career match score based on personality
     */
    private double calculateCareerMatchScore(String career, String mbtiType, String riasecCode) {
        double baseScore = 75.0; // Base score
        
        // Adjust based on MBTI preferences
        if (mbtiType.contains("I")) baseScore += career.toLowerCase().contains("analyst") ? 5 : 0;
        if (mbtiType.contains("E")) baseScore += career.toLowerCase().contains("manager") ? 5 : 0;
        if (mbtiType.contains("N")) baseScore += career.toLowerCase().contains("consultant") ? 5 : 0;
        if (mbtiType.contains("S")) baseScore += career.toLowerCase().contains("engineer") ? 5 : 0;
        if (mbtiType.contains("T")) baseScore += career.toLowerCase().contains("developer") ? 5 : 0;
        if (mbtiType.contains("F")) baseScore += career.toLowerCase().contains("counselor") ? 5 : 0;
        
        // Adjust based on RIASEC code
        char primaryRiasec = riasecCode.charAt(0);
        switch (primaryRiasec) {
            case 'R': baseScore += career.toLowerCase().contains("engineer") ? 10 : 0; break;
            case 'I': baseScore += career.toLowerCase().contains("researcher") ? 10 : 0; break;
            case 'A': baseScore += career.toLowerCase().contains("designer") ? 10 : 0; break;
            case 'S': baseScore += career.toLowerCase().contains("teacher") ? 10 : 0; break;
            case 'E': baseScore += career.toLowerCase().contains("entrepreneur") ? 10 : 0; break;
            case 'C': baseScore += career.toLowerCase().contains("accountant") ? 10 : 0; break;
        }
        
        return Math.min(100.0, baseScore);
    }
    
    /**
     * Generate match reason for course
     */
    private String generateCourseMatchReason(String course, String mbtiType, String riasecCode, double score) {
        if (score >= 90) {
            return String.format("Excellent fit for %s personality with %s interests", mbtiType, riasecCode);
        } else if (score >= 80) {
            return String.format("Strong alignment with %s traits and %s preferences", mbtiType, riasecCode);
        } else if (score >= 70) {
            return String.format("Good match for %s personality type", mbtiType);
        } else {
            return String.format("Moderate fit for %s with %s code", mbtiType, riasecCode);
        }
    }
    
    /**
     * Generate match reason for career
     */
    private String generateCareerMatchReason(String career, String mbtiType, String riasecCode, double score) {
        if (score >= 90) {
            return String.format("Ideal career for %s personality with %s interests", mbtiType, riasecCode);
        } else if (score >= 80) {
            return String.format("Strong career match for %s traits", mbtiType);
        } else if (score >= 70) {
            return String.format("Good career option for %s type", mbtiType);
        } else {
            return String.format("Suitable for %s with %s interests", mbtiType, riasecCode);
        }
    }
    
    /**
     * Get default course ranking (fallback)
     */
    private List<CourseRanking> getDefaultCourseRanking(String coursePath) {
        List<String> courses = parseCourses(coursePath);
        List<CourseRanking> rankings = new ArrayList<>();
        
        for (int i = 0; i < courses.size(); i++) {
            rankings.add(new CourseRanking(
                i + 1,
                courses.get(i),
                75.0,
                "Good match based on personality profile",
                "N/A",
                "N/A"
            ));
        }
        
        return rankings;
    }
    
    /**
     * Get default career ranking (fallback)
     */
    private List<CareerRanking> getDefaultCareerRanking(String careerSuggestions) {
        List<String> careers = parseCareers(careerSuggestions);
        List<CareerRanking> rankings = new ArrayList<>();
        
        for (int i = 0; i < careers.size(); i++) {
            rankings.add(new CareerRanking(
                i + 1,
                careers.get(i),
                75.0,
                "Good match based on personality profile",
                "N/A",
                "N/A"
            ));
        }
        
        return rankings;
    }
    
    /**
     * Course Ranking DTO
     */
    public static class CourseRanking {
        private int rank;
        private String courseName;
        private double matchScore;
        private String matchReason;
        private String mbtiType;
        private String riasecCode;
        private boolean aiRecommended;
        
        public CourseRanking(int rank, String courseName, double matchScore, String matchReason, 
                           String mbtiType, String riasecCode) {
            this.rank = rank;
            this.courseName = courseName;
            this.matchScore = matchScore;
            this.matchReason = matchReason;
            this.mbtiType = mbtiType;
            this.riasecCode = riasecCode;
            this.aiRecommended = rank == 1; // Top rank is AI recommended
        }
        
        // Getters and setters
        public int getRank() { return rank; }
        public void setRank(int rank) { 
            this.rank = rank;
            this.aiRecommended = (rank == 1);
        }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public double getMatchScore() { return matchScore; }
        public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
        
        public String getMatchReason() { return matchReason; }
        public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
        
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public boolean isAiRecommended() { return aiRecommended; }
        public void setAiRecommended(boolean aiRecommended) { this.aiRecommended = aiRecommended; }
    }
    
    /**
     * Career Ranking DTO
     */
    public static class CareerRanking {
        private int rank;
        private String careerName;
        private double matchScore;
        private String matchReason;
        private String mbtiType;
        private String riasecCode;
        private boolean aiRecommended;
        
        public CareerRanking(int rank, String careerName, double matchScore, String matchReason,
                           String mbtiType, String riasecCode) {
            this.rank = rank;
            this.careerName = careerName;
            this.matchScore = matchScore;
            this.matchReason = matchReason;
            this.mbtiType = mbtiType;
            this.riasecCode = riasecCode;
            this.aiRecommended = rank == 1; // Top rank is AI recommended
        }
        
        // Getters and setters
        public int getRank() { return rank; }
        public void setRank(int rank) { 
            this.rank = rank;
            this.aiRecommended = (rank == 1);
        }
        
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public double getMatchScore() { return matchScore; }
        public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
        
        public String getMatchReason() { return matchReason; }
        public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
        
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public boolean isAiRecommended() { return aiRecommended; }
        public void setAiRecommended(boolean aiRecommended) { this.aiRecommended = aiRecommended; }
    }
}
