package com.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Advanced Analytics Service
 * Combines human personality metrics with AI comparison metrics to generate
 * comprehensive match analysis for courses and careers
 */
@Service
public class AdvancedAnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(AdvancedAnalyticsService.class);
    
    @Autowired
    private HuggingFaceApiService huggingFaceService;
    
    @Value("${huggingface.validation.enabled:false}")
    private boolean aiEnabled;
    
    @Value("${huggingface.model.validation}")
    private String validationModel;
    
    /**
     * Generate comprehensive analytics combining human metrics and AI analysis
     */
    public AdvancedAnalyticsResult generateAdvancedAnalytics(
        String mbtiType,
        String riasecCode,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages,
        List<CourseWithAiScore> coursesWithAiScores,
        List<CareerWithAiScore> careersWithAiScores
    ) {
        log.info("🔬 Generating advanced analytics for {} / {}", mbtiType, riasecCode);
        
        try {
            AdvancedAnalyticsResult result = new AdvancedAnalyticsResult();
            result.setMbtiType(mbtiType);
            result.setRiasecCode(riasecCode);
            result.setGeneratedAt(new Date());
            
            // Extract personality trait percentages
            result.setPersonalityMetrics(extractPersonalityMetrics(mbtiType, riasecCode, mbtiPercentages, riasecPercentages));
            
            // Calculate combined match scores for courses
            List<CourseMatchAnalysis> courseAnalyses = calculateCourseMatches(
                coursesWithAiScores, 
                mbtiPercentages, 
                riasecPercentages,
                mbtiType,
                riasecCode
            );
            result.setCourseMatches(courseAnalyses);
            
            // Calculate combined match scores for careers
            List<CareerMatchAnalysis> careerAnalyses = calculateCareerMatches(
                careersWithAiScores,
                mbtiPercentages,
                riasecPercentages,
                mbtiType,
                riasecCode
            );
            result.setCareerMatches(careerAnalyses);
            
            // Generate AI-powered overall synthesis
            if (aiEnabled) {
                String overallSynthesis = generateAiSynthesis(
                    mbtiType, 
                    riasecCode, 
                    mbtiPercentages, 
                    riasecPercentages,
                    courseAnalyses,
                    careerAnalyses
                );
                result.setOverallSynthesis(overallSynthesis);
            } else {
                result.setOverallSynthesis(generateFallbackSynthesis(mbtiType, riasecCode, courseAnalyses, careerAnalyses));
            }
            
            // Calculate overall statistics
            result.setOverallStatistics(calculateOverallStatistics(courseAnalyses, careerAnalyses));
            
            log.info("✅ Advanced analytics generated successfully");
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to generate advanced analytics: {}", e.getMessage(), e);
            return createFallbackAnalytics(mbtiType, riasecCode);
        }
    }
    
    /**
     * Extract personality metrics from MBTI and RIASEC percentages
     */
    private PersonalityMetrics extractPersonalityMetrics(
        String mbtiType,
        String riasecCode,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages
    ) {
        PersonalityMetrics metrics = new PersonalityMetrics();
        
        // Extract dominant MBTI traits
        List<TraitScore> mbtiTraits = new ArrayList<>();
        for (char trait : mbtiType.toCharArray()) {
            String traitStr = String.valueOf(trait);
            Double percentage = mbtiPercentages.getOrDefault(traitStr, 0.0);
            mbtiTraits.add(new TraitScore(traitStr, percentage, getTraitLabel(traitStr)));
        }
        metrics.setMbtiTraits(mbtiTraits);
        
        // Extract top RIASEC interests
        List<TraitScore> riasecInterests = new ArrayList<>();
        for (char interest : riasecCode.toCharArray()) {
            String interestStr = String.valueOf(interest);
            Double percentage = riasecPercentages.getOrDefault(interestStr, 0.0);
            riasecInterests.add(new TraitScore(interestStr, percentage, getRiasecLabel(interestStr)));
        }
        metrics.setRiasecInterests(riasecInterests);
        
        // Calculate average trait strength
        double avgMbti = mbtiTraits.stream().mapToDouble(TraitScore::getPercentage).average().orElse(0.0);
        double avgRiasec = riasecInterests.stream().mapToDouble(TraitScore::getPercentage).average().orElse(0.0);
        metrics.setAverageTraitStrength((avgMbti + avgRiasec) / 2.0);
        
        return metrics;
    }
    
    /**
     * Calculate combined match scores for courses
     */
    private List<CourseMatchAnalysis> calculateCourseMatches(
        List<CourseWithAiScore> courses,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages,
        String mbtiType,
        String riasecCode
    ) {
        List<CourseMatchAnalysis> analyses = new ArrayList<>();
        
        for (CourseWithAiScore course : courses) {
            CourseMatchAnalysis analysis = new CourseMatchAnalysis();
            analysis.setCourseName(course.getCourseName());
            analysis.setCourseDescription(course.getCourseDescription());
            
            // Human metrics (personality alignment)
            double humanScore = calculateHumanCourseScore(course.getCourseName(), mbtiPercentages, riasecPercentages);
            analysis.setHumanMetricScore(humanScore);
            
            // AI metrics (from AI comparison)
            double aiScore = course.getAiScore();
            analysis.setAiMetricScore(aiScore);
            
            // Combined score (weighted average: 40% human, 60% AI)
            double combinedScore = (humanScore * 0.4) + (aiScore * 0.6);
            analysis.setCombinedMatchScore(combinedScore);
            
            // Generate match explanation
            analysis.setMatchExplanation(generateCourseMatchExplanation(
                course.getCourseName(), 
                humanScore, 
                aiScore, 
                combinedScore,
                mbtiType,
                riasecCode
            ));
            
            // Determine confidence level
            analysis.setConfidenceLevel(determineConfidenceLevel(humanScore, aiScore));
            
            analyses.add(analysis);
        }
        
        // Sort by combined score (descending)
        analyses.sort((a, b) -> Double.compare(b.getCombinedMatchScore(), a.getCombinedMatchScore()));
        
        return analyses;
    }
    
    /**
     * Calculate combined match scores for careers
     */
    private List<CareerMatchAnalysis> calculateCareerMatches(
        List<CareerWithAiScore> careers,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages,
        String mbtiType,
        String riasecCode
    ) {
        List<CareerMatchAnalysis> analyses = new ArrayList<>();
        
        for (CareerWithAiScore career : careers) {
            CareerMatchAnalysis analysis = new CareerMatchAnalysis();
            analysis.setCareerName(career.getCareerName());
            analysis.setCareerDescription(career.getCareerDescription());
            
            // Human metrics (personality alignment)
            double humanScore = calculateHumanCareerScore(career.getCareerName(), mbtiPercentages, riasecPercentages);
            analysis.setHumanMetricScore(humanScore);
            
            // AI metrics (from AI comparison)
            double aiScore = career.getAiScore();
            analysis.setAiMetricScore(aiScore);
            
            // Combined score (weighted average: 40% human, 60% AI)
            double combinedScore = (humanScore * 0.4) + (aiScore * 0.6);
            analysis.setCombinedMatchScore(combinedScore);
            
            // Generate match explanation
            analysis.setMatchExplanation(generateCareerMatchExplanation(
                career.getCareerName(),
                humanScore,
                aiScore,
                combinedScore,
                mbtiType,
                riasecCode
            ));
            
            // Determine confidence level
            analysis.setConfidenceLevel(determineConfidenceLevel(humanScore, aiScore));
            
            analyses.add(analysis);
        }
        
        // Sort by combined score (descending)
        analyses.sort((a, b) -> Double.compare(b.getCombinedMatchScore(), a.getCombinedMatchScore()));
        
        return analyses;
    }
    
    /**
     * Calculate human-based course score from personality traits
     */
    private double calculateHumanCourseScore(
        String courseName,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages
    ) {
        double score = 70.0; // Base score
        String lowerCourseName = courseName.toLowerCase();
        
        // MBTI-based adjustments
        if (lowerCourseName.contains("computer") || lowerCourseName.contains("technology")) {
            score += mbtiPercentages.getOrDefault("I", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("N", 0.0) * 0.10;
            score += mbtiPercentages.getOrDefault("T", 0.0) * 0.15;
        }
        if (lowerCourseName.contains("business") || lowerCourseName.contains("management")) {
            score += mbtiPercentages.getOrDefault("E", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("J", 0.0) * 0.10;
        }
        if (lowerCourseName.contains("psychology") || lowerCourseName.contains("counseling")) {
            score += mbtiPercentages.getOrDefault("F", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("N", 0.0) * 0.10;
        }
        if (lowerCourseName.contains("engineering")) {
            score += mbtiPercentages.getOrDefault("T", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("S", 0.0) * 0.10;
        }
        if (lowerCourseName.contains("art") || lowerCourseName.contains("design")) {
            score += mbtiPercentages.getOrDefault("N", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("P", 0.0) * 0.10;
        }
        
        // RIASEC-based adjustments
        if (lowerCourseName.contains("engineering") || lowerCourseName.contains("mechanical")) {
            score += riasecPercentages.getOrDefault("R", 0.0) * 0.20;
        }
        if (lowerCourseName.contains("science") || lowerCourseName.contains("research")) {
            score += riasecPercentages.getOrDefault("I", 0.0) * 0.20;
        }
        if (lowerCourseName.contains("art") || lowerCourseName.contains("creative")) {
            score += riasecPercentages.getOrDefault("A", 0.0) * 0.20;
        }
        if (lowerCourseName.contains("education") || lowerCourseName.contains("social")) {
            score += riasecPercentages.getOrDefault("S", 0.0) * 0.20;
        }
        if (lowerCourseName.contains("business") || lowerCourseName.contains("entrepreneurship")) {
            score += riasecPercentages.getOrDefault("E", 0.0) * 0.20;
        }
        if (lowerCourseName.contains("accounting") || lowerCourseName.contains("administration")) {
            score += riasecPercentages.getOrDefault("C", 0.0) * 0.20;
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }
    
    /**
     * Calculate human-based career score from personality traits
     */
    private double calculateHumanCareerScore(
        String careerName,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages
    ) {
        double score = 70.0; // Base score
        String lowerCareerName = careerName.toLowerCase();
        
        // MBTI-based adjustments
        if (lowerCareerName.contains("developer") || lowerCareerName.contains("programmer")) {
            score += mbtiPercentages.getOrDefault("I", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("T", 0.0) * 0.15;
        }
        if (lowerCareerName.contains("manager") || lowerCareerName.contains("director")) {
            score += mbtiPercentages.getOrDefault("E", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("J", 0.0) * 0.10;
        }
        if (lowerCareerName.contains("counselor") || lowerCareerName.contains("therapist")) {
            score += mbtiPercentages.getOrDefault("F", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("N", 0.0) * 0.10;
        }
        if (lowerCareerName.contains("engineer")) {
            score += mbtiPercentages.getOrDefault("T", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("S", 0.0) * 0.10;
        }
        if (lowerCareerName.contains("designer") || lowerCareerName.contains("artist")) {
            score += mbtiPercentages.getOrDefault("N", 0.0) * 0.15;
            score += mbtiPercentages.getOrDefault("P", 0.0) * 0.10;
        }
        
        // RIASEC-based adjustments
        if (lowerCareerName.contains("mechanic") || lowerCareerName.contains("technician")) {
            score += riasecPercentages.getOrDefault("R", 0.0) * 0.20;
        }
        if (lowerCareerName.contains("researcher") || lowerCareerName.contains("scientist")) {
            score += riasecPercentages.getOrDefault("I", 0.0) * 0.20;
        }
        if (lowerCareerName.contains("artist") || lowerCareerName.contains("writer")) {
            score += riasecPercentages.getOrDefault("A", 0.0) * 0.20;
        }
        if (lowerCareerName.contains("teacher") || lowerCareerName.contains("counselor")) {
            score += riasecPercentages.getOrDefault("S", 0.0) * 0.20;
        }
        if (lowerCareerName.contains("entrepreneur") || lowerCareerName.contains("sales")) {
            score += riasecPercentages.getOrDefault("E", 0.0) * 0.20;
        }
        if (lowerCareerName.contains("accountant") || lowerCareerName.contains("clerk")) {
            score += riasecPercentages.getOrDefault("C", 0.0) * 0.20;
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }
    
    /**
     * Generate AI-powered overall synthesis
     */
    private String generateAiSynthesis(
        String mbtiType,
        String riasecCode,
        Map<String, Double> mbtiPercentages,
        Map<String, Double> riasecPercentages,
        List<CourseMatchAnalysis> courseAnalyses,
        List<CareerMatchAnalysis> careerAnalyses
    ) {
        try {
            // Build comprehensive prompt for AI
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an expert career counselor analyzing personality-career alignment.\n\n");
            prompt.append(String.format("Student Profile: MBTI %s, RIASEC %s\n\n", mbtiType, riasecCode));
            
            prompt.append("Personality Trait Strengths:\n");
            for (char trait : mbtiType.toCharArray()) {
                String traitStr = String.valueOf(trait);
                Double percentage = mbtiPercentages.getOrDefault(traitStr, 0.0);
                prompt.append(String.format("- %s (%s): %.0f%%\n", traitStr, getTraitLabel(traitStr), percentage));
            }
            prompt.append("\n");
            
            prompt.append("Interest Strengths:\n");
            for (char interest : riasecCode.toCharArray()) {
                String interestStr = String.valueOf(interest);
                Double percentage = riasecPercentages.getOrDefault(interestStr, 0.0);
                prompt.append(String.format("- %s (%s): %.0f%%\n", interestStr, getRiasecLabel(interestStr), percentage));
            }
            prompt.append("\n");
            
            prompt.append("Top 3 Course Matches:\n");
            for (int i = 0; i < Math.min(3, courseAnalyses.size()); i++) {
                CourseMatchAnalysis course = courseAnalyses.get(i);
                prompt.append(String.format("%d. %s - Combined Match: %.0f%% (Human: %.0f%%, AI: %.0f%%)\n",
                    i + 1, course.getCourseName(), course.getCombinedMatchScore(),
                    course.getHumanMetricScore(), course.getAiMetricScore()));
            }
            prompt.append("\n");
            
            prompt.append("Top 3 Career Matches:\n");
            for (int i = 0; i < Math.min(3, careerAnalyses.size()); i++) {
                CareerMatchAnalysis career = careerAnalyses.get(i);
                prompt.append(String.format("%d. %s - Combined Match: %.0f%% (Human: %.0f%%, AI: %.0f%%)\n",
                    i + 1, career.getCareerName(), career.getCombinedMatchScore(),
                    career.getHumanMetricScore(), career.getAiMetricScore()));
            }
            prompt.append("\n");
            
            prompt.append("Task: Provide a comprehensive 3-4 paragraph synthesis that:\n");
            prompt.append("1. Analyzes how the personality traits align with the recommended paths\n");
            prompt.append("2. Explains the synergy between human metrics and AI analysis\n");
            prompt.append("3. Highlights key strengths and potential challenges\n");
            prompt.append("4. Provides actionable insights for career development\n\n");
            prompt.append("Write in a professional, encouraging, and insightful tone.\n");
            
            String aiResponse = huggingFaceService.generateText(validationModel, prompt.toString());
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                log.info("✅ AI synthesis generated successfully");
                return aiResponse.trim();
            } else {
                log.warn("⚠️ AI returned empty synthesis, using fallback");
                return generateFallbackSynthesis(mbtiType, riasecCode, courseAnalyses, careerAnalyses);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to generate AI synthesis: {}", e.getMessage(), e);
            return generateFallbackSynthesis(mbtiType, riasecCode, courseAnalyses, careerAnalyses);
        }
    }
    
    /**
     * Generate fallback synthesis when AI is unavailable
     */
    private String generateFallbackSynthesis(
        String mbtiType,
        String riasecCode,
        List<CourseMatchAnalysis> courseAnalyses,
        List<CareerMatchAnalysis> careerAnalyses
    ) {
        StringBuilder synthesis = new StringBuilder();
        
        synthesis.append(String.format("Your %s personality type combined with %s interests creates a unique profile ", 
            mbtiType, riasecCode));
        synthesis.append("that aligns well with the recommended educational and career paths. ");
        
        synthesis.append("The advanced analytics system has evaluated your personality traits against both ");
        synthesis.append("traditional career matching algorithms and cutting-edge AI analysis to provide ");
        synthesis.append("the most accurate recommendations possible.\n\n");
        
        if (!courseAnalyses.isEmpty()) {
            CourseMatchAnalysis topCourse = courseAnalyses.get(0);
            synthesis.append(String.format("Your top course recommendation, %s, shows a %.0f%% combined match score, ",
                topCourse.getCourseName(), topCourse.getCombinedMatchScore()));
            synthesis.append("indicating strong alignment between your personality traits and the program requirements. ");
        }
        
        if (!careerAnalyses.isEmpty()) {
            CareerMatchAnalysis topCareer = careerAnalyses.get(0);
            synthesis.append(String.format("Similarly, %s emerges as your top career match with a %.0f%% score, ",
                topCareer.getCareerName(), topCareer.getCombinedMatchScore()));
            synthesis.append("suggesting excellent potential for success and satisfaction in this field.\n\n");
        }
        
        synthesis.append("The convergence of human personality metrics and AI-powered analysis provides ");
        synthesis.append("a comprehensive view of your career potential. Focus on developing skills that ");
        synthesis.append("leverage your natural strengths while remaining open to growth opportunities ");
        synthesis.append("in areas that may challenge you. Your unique combination of traits positions you ");
        synthesis.append("well for success in your chosen path.");
        
        return synthesis.toString();
    }
    
    /**
     * Calculate overall statistics
     */
    private OverallStatistics calculateOverallStatistics(
        List<CourseMatchAnalysis> courseAnalyses,
        List<CareerMatchAnalysis> careerAnalyses
    ) {
        OverallStatistics stats = new OverallStatistics();
        
        // Course statistics
        if (!courseAnalyses.isEmpty()) {
            double avgCourseMatch = courseAnalyses.stream()
                .mapToDouble(CourseMatchAnalysis::getCombinedMatchScore)
                .average()
                .orElse(0.0);
            stats.setAverageCourseMatch(avgCourseMatch);
            
            double topCourseMatch = courseAnalyses.get(0).getCombinedMatchScore();
            stats.setTopCourseMatch(topCourseMatch);
        }
        
        // Career statistics
        if (!careerAnalyses.isEmpty()) {
            double avgCareerMatch = careerAnalyses.stream()
                .mapToDouble(CareerMatchAnalysis::getCombinedMatchScore)
                .average()
                .orElse(0.0);
            stats.setAverageCareerMatch(avgCareerMatch);
            
            double topCareerMatch = careerAnalyses.get(0).getCombinedMatchScore();
            stats.setTopCareerMatch(topCareerMatch);
        }
        
        // Overall confidence
        double overallConfidence = (stats.getAverageCourseMatch() + stats.getAverageCareerMatch()) / 2.0;
        stats.setOverallConfidence(overallConfidence);
        
        // Determine recommendation strength
        if (overallConfidence >= 85) {
            stats.setRecommendationStrength("EXCELLENT");
        } else if (overallConfidence >= 75) {
            stats.setRecommendationStrength("STRONG");
        } else if (overallConfidence >= 65) {
            stats.setRecommendationStrength("GOOD");
        } else {
            stats.setRecommendationStrength("MODERATE");
        }
        
        return stats;
    }
    
    /**
     * Generate course match explanation
     */
    private String generateCourseMatchExplanation(
        String courseName,
        double humanScore,
        double aiScore,
        double combinedScore,
        String mbtiType,
        String riasecCode
    ) {
        StringBuilder explanation = new StringBuilder();
        
        if (combinedScore >= 85) {
            explanation.append("Excellent match! ");
        } else if (combinedScore >= 75) {
            explanation.append("Strong match! ");
        } else if (combinedScore >= 65) {
            explanation.append("Good match! ");
        } else {
            explanation.append("Moderate match. ");
        }
        
        explanation.append(String.format("Your %s personality and %s interests ", mbtiType, riasecCode));
        
        double scoreDiff = Math.abs(humanScore - aiScore);
        if (scoreDiff <= 10) {
            explanation.append("show strong alignment with this course, confirmed by both personality analysis and AI evaluation. ");
        } else if (humanScore > aiScore) {
            explanation.append("align well with this course based on personality traits, though AI analysis suggests exploring additional options. ");
        } else {
            explanation.append("are recognized by AI as a great fit, with personality traits providing solid foundation for success. ");
        }
        
        return explanation.toString();
    }
    
    /**
     * Generate career match explanation
     */
    private String generateCareerMatchExplanation(
        String careerName,
        double humanScore,
        double aiScore,
        double combinedScore,
        String mbtiType,
        String riasecCode
    ) {
        StringBuilder explanation = new StringBuilder();
        
        if (combinedScore >= 85) {
            explanation.append("Outstanding career fit! ");
        } else if (combinedScore >= 75) {
            explanation.append("Strong career alignment! ");
        } else if (combinedScore >= 65) {
            explanation.append("Promising career option! ");
        } else {
            explanation.append("Viable career path. ");
        }
        
        explanation.append(String.format("Your %s traits and %s interests ", mbtiType, riasecCode));
        
        double scoreDiff = Math.abs(humanScore - aiScore);
        if (scoreDiff <= 10) {
            explanation.append("are perfectly suited for this career, validated by both traditional assessment and AI insights. ");
        } else if (humanScore > aiScore) {
            explanation.append("show natural affinity for this career, with opportunities to develop AI-identified growth areas. ");
        } else {
            explanation.append("are recognized by AI as highly compatible, building on your personality strengths. ");
        }
        
        return explanation.toString();
    }
    
    /**
     * Determine confidence level based on score agreement
     */
    private String determineConfidenceLevel(double humanScore, double aiScore) {
        double scoreDiff = Math.abs(humanScore - aiScore);
        double avgScore = (humanScore + aiScore) / 2.0;
        
        if (scoreDiff <= 10 && avgScore >= 80) {
            return "VERY_HIGH";
        } else if (scoreDiff <= 15 && avgScore >= 70) {
            return "HIGH";
        } else if (scoreDiff <= 20 && avgScore >= 60) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Get trait label
     */
    private String getTraitLabel(String trait) {
        Map<String, String> labels = new HashMap<>();
        labels.put("E", "Extraversion");
        labels.put("I", "Introversion");
        labels.put("S", "Sensing");
        labels.put("N", "Intuition");
        labels.put("T", "Thinking");
        labels.put("F", "Feeling");
        labels.put("J", "Judging");
        labels.put("P", "Perceiving");
        return labels.getOrDefault(trait, trait);
    }
    
    /**
     * Get RIASEC label
     */
    private String getRiasecLabel(String code) {
        Map<String, String> labels = new HashMap<>();
        labels.put("R", "Realistic");
        labels.put("I", "Investigative");
        labels.put("A", "Artistic");
        labels.put("S", "Social");
        labels.put("E", "Enterprising");
        labels.put("C", "Conventional");
        return labels.getOrDefault(code, code);
    }
    
    /**
     * Create fallback analytics when generation fails
     */
    private AdvancedAnalyticsResult createFallbackAnalytics(String mbtiType, String riasecCode) {
        AdvancedAnalyticsResult result = new AdvancedAnalyticsResult();
        result.setMbtiType(mbtiType);
        result.setRiasecCode(riasecCode);
        result.setGeneratedAt(new Date());
        result.setOverallSynthesis("Advanced analytics are currently being processed. Please check back shortly for detailed insights.");
        return result;
    }
    
    // ==================== DTOs ====================
    
    public static class AdvancedAnalyticsResult {
        private String mbtiType;
        private String riasecCode;
        private Date generatedAt;
        private PersonalityMetrics personalityMetrics;
        private List<CourseMatchAnalysis> courseMatches;
        private List<CareerMatchAnalysis> careerMatches;
        private String overallSynthesis;
        private OverallStatistics overallStatistics;
        
        // Getters and setters
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public Date getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
        
        public PersonalityMetrics getPersonalityMetrics() { return personalityMetrics; }
        public void setPersonalityMetrics(PersonalityMetrics personalityMetrics) { this.personalityMetrics = personalityMetrics; }
        
        public List<CourseMatchAnalysis> getCourseMatches() { return courseMatches; }
        public void setCourseMatches(List<CourseMatchAnalysis> courseMatches) { this.courseMatches = courseMatches; }
        
        public List<CareerMatchAnalysis> getCareerMatches() { return careerMatches; }
        public void setCareerMatches(List<CareerMatchAnalysis> careerMatches) { this.careerMatches = careerMatches; }
        
        public String getOverallSynthesis() { return overallSynthesis; }
        public void setOverallSynthesis(String overallSynthesis) { this.overallSynthesis = overallSynthesis; }
        
        public OverallStatistics getOverallStatistics() { return overallStatistics; }
        public void setOverallStatistics(OverallStatistics overallStatistics) { this.overallStatistics = overallStatistics; }
    }
    
    public static class PersonalityMetrics {
        private List<TraitScore> mbtiTraits;
        private List<TraitScore> riasecInterests;
        private double averageTraitStrength;
        
        // Getters and setters
        public List<TraitScore> getMbtiTraits() { return mbtiTraits; }
        public void setMbtiTraits(List<TraitScore> mbtiTraits) { this.mbtiTraits = mbtiTraits; }
        
        public List<TraitScore> getRiasecInterests() { return riasecInterests; }
        public void setRiasecInterests(List<TraitScore> riasecInterests) { this.riasecInterests = riasecInterests; }
        
        public double getAverageTraitStrength() { return averageTraitStrength; }
        public void setAverageTraitStrength(double averageTraitStrength) { this.averageTraitStrength = averageTraitStrength; }
    }
    
    public static class TraitScore {
        private String trait;
        private double percentage;
        private String label;
        
        public TraitScore(String trait, double percentage, String label) {
            this.trait = trait;
            this.percentage = percentage;
            this.label = label;
        }
        
        // Getters and setters
        public String getTrait() { return trait; }
        public void setTrait(String trait) { this.trait = trait; }
        
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
    
    public static class CourseMatchAnalysis {
        private String courseName;
        private String courseDescription;
        private double humanMetricScore;
        private double aiMetricScore;
        private double combinedMatchScore;
        private String matchExplanation;
        private String confidenceLevel;
        
        // Getters and setters
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getCourseDescription() { return courseDescription; }
        public void setCourseDescription(String courseDescription) { this.courseDescription = courseDescription; }
        
        public double getHumanMetricScore() { return humanMetricScore; }
        public void setHumanMetricScore(double humanMetricScore) { this.humanMetricScore = humanMetricScore; }
        
        public double getAiMetricScore() { return aiMetricScore; }
        public void setAiMetricScore(double aiMetricScore) { this.aiMetricScore = aiMetricScore; }
        
        public double getCombinedMatchScore() { return combinedMatchScore; }
        public void setCombinedMatchScore(double combinedMatchScore) { this.combinedMatchScore = combinedMatchScore; }
        
        public String getMatchExplanation() { return matchExplanation; }
        public void setMatchExplanation(String matchExplanation) { this.matchExplanation = matchExplanation; }
        
        public String getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    }
    
    public static class CareerMatchAnalysis {
        private String careerName;
        private String careerDescription;
        private double humanMetricScore;
        private double aiMetricScore;
        private double combinedMatchScore;
        private String matchExplanation;
        private String confidenceLevel;
        
        // Getters and setters
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public String getCareerDescription() { return careerDescription; }
        public void setCareerDescription(String careerDescription) { this.careerDescription = careerDescription; }
        
        public double getHumanMetricScore() { return humanMetricScore; }
        public void setHumanMetricScore(double humanMetricScore) { this.humanMetricScore = humanMetricScore; }
        
        public double getAiMetricScore() { return aiMetricScore; }
        public void setAiMetricScore(double aiMetricScore) { this.aiMetricScore = aiMetricScore; }
        
        public double getCombinedMatchScore() { return combinedMatchScore; }
        public void setCombinedMatchScore(double combinedMatchScore) { this.combinedMatchScore = combinedMatchScore; }
        
        public String getMatchExplanation() { return matchExplanation; }
        public void setMatchExplanation(String matchExplanation) { this.matchExplanation = matchExplanation; }
        
        public String getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    }
    
    public static class OverallStatistics {
        private double averageCourseMatch;
        private double topCourseMatch;
        private double averageCareerMatch;
        private double topCareerMatch;
        private double overallConfidence;
        private String recommendationStrength;
        
        // Getters and setters
        public double getAverageCourseMatch() { return averageCourseMatch; }
        public void setAverageCourseMatch(double averageCourseMatch) { this.averageCourseMatch = averageCourseMatch; }
        
        public double getTopCourseMatch() { return topCourseMatch; }
        public void setTopCourseMatch(double topCourseMatch) { this.topCourseMatch = topCourseMatch; }
        
        public double getAverageCareerMatch() { return averageCareerMatch; }
        public void setAverageCareerMatch(double averageCareerMatch) { this.averageCareerMatch = averageCareerMatch; }
        
        public double getTopCareerMatch() { return topCareerMatch; }
        public void setTopCareerMatch(double topCareerMatch) { this.topCareerMatch = topCareerMatch; }
        
        public double getOverallConfidence() { return overallConfidence; }
        public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }
        
        public String getRecommendationStrength() { return recommendationStrength; }
        public void setRecommendationStrength(String recommendationStrength) { this.recommendationStrength = recommendationStrength; }
    }
    
    public static class CourseWithAiScore {
        private String courseName;
        private String courseDescription;
        private double aiScore;
        
        public CourseWithAiScore(String courseName, String courseDescription, double aiScore) {
            this.courseName = courseName;
            this.courseDescription = courseDescription;
            this.aiScore = aiScore;
        }
        
        // Getters and setters
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getCourseDescription() { return courseDescription; }
        public void setCourseDescription(String courseDescription) { this.courseDescription = courseDescription; }
        
        public double getAiScore() { return aiScore; }
        public void setAiScore(double aiScore) { this.aiScore = aiScore; }
    }
    
    public static class CareerWithAiScore {
        private String careerName;
        private String careerDescription;
        private double aiScore;
        
        public CareerWithAiScore(String careerName, String careerDescription, double aiScore) {
            this.careerName = careerName;
            this.careerDescription = careerDescription;
            this.aiScore = aiScore;
        }
        
        // Getters and setters
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public String getCareerDescription() { return careerDescription; }
        public void setCareerDescription(String careerDescription) { this.careerDescription = careerDescription; }
        
        public double getAiScore() { return aiScore; }
        public void setAiScore(double aiScore) { this.aiScore = aiScore; }
    }
}

