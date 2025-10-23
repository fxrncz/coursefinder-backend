package com.app.dto;

import java.util.Date;
import java.util.List;

/**
 * DTO for Advanced Analytics Result
 * Contains combined human + AI metrics analysis
 */
public class AdvancedAnalyticsDTO {
    private String mbtiType;
    private String riasecCode;
    private Date generatedAt;
    private PersonalityMetricsDTO personalityMetrics;
    private List<CourseMatchAnalysisDTO> courseMatches;
    private List<CareerMatchAnalysisDTO> careerMatches;
    private String overallSynthesis;
    private OverallStatisticsDTO overallStatistics;
    
    // Getters and setters
    public String getMbtiType() { return mbtiType; }
    public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
    
    public String getRiasecCode() { return riasecCode; }
    public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
    
    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
    
    public PersonalityMetricsDTO getPersonalityMetrics() { return personalityMetrics; }
    public void setPersonalityMetrics(PersonalityMetricsDTO personalityMetrics) { this.personalityMetrics = personalityMetrics; }
    
    public List<CourseMatchAnalysisDTO> getCourseMatches() { return courseMatches; }
    public void setCourseMatches(List<CourseMatchAnalysisDTO> courseMatches) { this.courseMatches = courseMatches; }
    
    public List<CareerMatchAnalysisDTO> getCareerMatches() { return careerMatches; }
    public void setCareerMatches(List<CareerMatchAnalysisDTO> careerMatches) { this.careerMatches = careerMatches; }
    
    public String getOverallSynthesis() { return overallSynthesis; }
    public void setOverallSynthesis(String overallSynthesis) { this.overallSynthesis = overallSynthesis; }
    
    public OverallStatisticsDTO getOverallStatistics() { return overallStatistics; }
    public void setOverallStatistics(OverallStatisticsDTO overallStatistics) { this.overallStatistics = overallStatistics; }
    
    // Nested DTOs
    public static class PersonalityMetricsDTO {
        private List<TraitScoreDTO> mbtiTraits;
        private List<TraitScoreDTO> riasecInterests;
        private double averageTraitStrength;
        
        public List<TraitScoreDTO> getMbtiTraits() { return mbtiTraits; }
        public void setMbtiTraits(List<TraitScoreDTO> mbtiTraits) { this.mbtiTraits = mbtiTraits; }
        
        public List<TraitScoreDTO> getRiasecInterests() { return riasecInterests; }
        public void setRiasecInterests(List<TraitScoreDTO> riasecInterests) { this.riasecInterests = riasecInterests; }
        
        public double getAverageTraitStrength() { return averageTraitStrength; }
        public void setAverageTraitStrength(double averageTraitStrength) { this.averageTraitStrength = averageTraitStrength; }
    }
    
    public static class TraitScoreDTO {
        private String trait;
        private double percentage;
        private String label;
        
        public String getTrait() { return trait; }
        public void setTrait(String trait) { this.trait = trait; }
        
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
    
    public static class CourseMatchAnalysisDTO {
        private String courseName;
        private String courseDescription;
        private double humanMetricScore;
        private double aiMetricScore;
        private double combinedMatchScore;
        private String matchExplanation;
        private String confidenceLevel;
        
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
    
    public static class CareerMatchAnalysisDTO {
        private String careerName;
        private String careerDescription;
        private double humanMetricScore;
        private double aiMetricScore;
        private double combinedMatchScore;
        private String matchExplanation;
        private String confidenceLevel;
        
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
    
    public static class OverallStatisticsDTO {
        private double averageCourseMatch;
        private double topCourseMatch;
        private double averageCareerMatch;
        private double topCareerMatch;
        private double overallConfidence;
        private String recommendationStrength;
        
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
}

