package com.app.dto;

import com.app.models.MbtiDetails;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

public class EnhancedTestResultDTO {
    
    private Long id;
    private Long userId;
    private String guestToken;
    private String mbtiType;
    private String riasecCode;
    private String coursePath;
    private String careerSuggestions;
    private String learningStyle;
    private String studyTips;
    private String personalityGrowthTips;
    private String studentGoals;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;
    
    // RIASEC and MBTI scores for visualization
    private Map<String, Integer> riasecScores;
    private Map<String, Integer> mbtiScores;
    
    // Enhanced MBTI details
    private DetailedMbtiInfoDTO detailedMbtiInfo;
    
    // Static inner class for detailed MBTI information
    public static class DetailedMbtiInfoDTO {
        private String learningStyleSummary;
        private String learningStyleDetails;
        private String learningStyleEnvironments;
        private String learningStyleResources;
        private String studyTipsSummary;
        private String studyTipsDetails;
        private String studyTipsDos;
        private String studyTipsDonts;
        private String studyTipsCommonMistakes;
        private String growthStrengths;
        private String growthWeaknesses;
        private String growthOpportunities;
        private String growthChallenges;
        
        // Constructor
        public DetailedMbtiInfoDTO() {}
        
        public DetailedMbtiInfoDTO(MbtiDetails mbtiDetails) {
            if (mbtiDetails != null) {
                this.learningStyleSummary = mbtiDetails.getLearningStyleSummary();
                this.learningStyleDetails = mbtiDetails.getLearningStyleDetails();
                this.learningStyleEnvironments = mbtiDetails.getLearningStyleEnvironments();
                this.learningStyleResources = mbtiDetails.getLearningStyleResources();
                this.studyTipsSummary = mbtiDetails.getStudyTipsSummary();
                this.studyTipsDetails = mbtiDetails.getStudyTipsDetails();
                this.studyTipsDos = mbtiDetails.getStudyTipsDos();
                this.studyTipsDonts = mbtiDetails.getStudyTipsDonts();
                this.studyTipsCommonMistakes = mbtiDetails.getStudyTipsCommonMistakes();
                this.growthStrengths = mbtiDetails.getGrowthStrengths();
                this.growthWeaknesses = mbtiDetails.getGrowthWeaknesses();
                this.growthOpportunities = mbtiDetails.getGrowthOpportunities();
                this.growthChallenges = mbtiDetails.getGrowthChallenges();
            }
        }
        
        // Getters and Setters
        public String getLearningStyleSummary() { return learningStyleSummary; }
        public void setLearningStyleSummary(String learningStyleSummary) { this.learningStyleSummary = learningStyleSummary; }
        
        public String getLearningStyleDetails() { return learningStyleDetails; }
        public void setLearningStyleDetails(String learningStyleDetails) { this.learningStyleDetails = learningStyleDetails; }
        
        public String getLearningStyleEnvironments() { return learningStyleEnvironments; }
        public void setLearningStyleEnvironments(String learningStyleEnvironments) { this.learningStyleEnvironments = learningStyleEnvironments; }
        
        public String getLearningStyleResources() { return learningStyleResources; }
        public void setLearningStyleResources(String learningStyleResources) { this.learningStyleResources = learningStyleResources; }
        
        public String getStudyTipsSummary() { return studyTipsSummary; }
        public void setStudyTipsSummary(String studyTipsSummary) { this.studyTipsSummary = studyTipsSummary; }
        
        public String getStudyTipsDetails() { return studyTipsDetails; }
        public void setStudyTipsDetails(String studyTipsDetails) { this.studyTipsDetails = studyTipsDetails; }
        
        public String getStudyTipsDos() { return studyTipsDos; }
        public void setStudyTipsDos(String studyTipsDos) { this.studyTipsDos = studyTipsDos; }
        
        public String getStudyTipsDonts() { return studyTipsDonts; }
        public void setStudyTipsDonts(String studyTipsDonts) { this.studyTipsDonts = studyTipsDonts; }
        
        public String getStudyTipsCommonMistakes() { return studyTipsCommonMistakes; }
        public void setStudyTipsCommonMistakes(String studyTipsCommonMistakes) { this.studyTipsCommonMistakes = studyTipsCommonMistakes; }
        
        public String getGrowthStrengths() { return growthStrengths; }
        public void setGrowthStrengths(String growthStrengths) { this.growthStrengths = growthStrengths; }
        
        public String getGrowthWeaknesses() { return growthWeaknesses; }
        public void setGrowthWeaknesses(String growthWeaknesses) { this.growthWeaknesses = growthWeaknesses; }
        
        public String getGrowthOpportunities() { return growthOpportunities; }
        public void setGrowthOpportunities(String growthOpportunities) { this.growthOpportunities = growthOpportunities; }
        
        public String getGrowthChallenges() { return growthChallenges; }
        public void setGrowthChallenges(String growthChallenges) { this.growthChallenges = growthChallenges; }
    }
    
    // Constructors
    public EnhancedTestResultDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getGuestToken() { return guestToken; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
    
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
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public Map<String, Integer> getRiasecScores() { return riasecScores; }
    public void setRiasecScores(Map<String, Integer> riasecScores) { this.riasecScores = riasecScores; }
    
    public Map<String, Integer> getMbtiScores() { return mbtiScores; }
    public void setMbtiScores(Map<String, Integer> mbtiScores) { this.mbtiScores = mbtiScores; }
    
    public DetailedMbtiInfoDTO getDetailedMbtiInfo() { return detailedMbtiInfo; }
    public void setDetailedMbtiInfo(DetailedMbtiInfoDTO detailedMbtiInfo) { this.detailedMbtiInfo = detailedMbtiInfo; }
}
