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
    private String detailedExplanation;
    // Removed: learningStyle, studyTips, personalityGrowthTips from persisted results
    private String studentGoals;
    private Integer age;
    private String gender;
    private Boolean isFromPLMar;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime generatedAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime takenAt;
    
    // RIASEC and MBTI scores for visualization
    private Map<String, Integer> riasecScores;
    private Map<String, Integer> mbtiScores;
    
    // Enhanced MBTI details
    private DetailedMbtiInfoDTO detailedMbtiInfo;
    
    // Enhanced RIASEC details
    private DetailedRiasecInfoDTO detailedRiasecInfo;
    
    // Career Development Plan
    private CareerDevelopmentPlanDTO careerDevelopmentPlan;
    
    // Course Development Plan
    private CourseDevelopmentPlanDTO courseDevelopmentPlan;
    
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
    
    // Static inner class for detailed RIASEC information
    public static class DetailedRiasecInfoDTO {
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
        public DetailedRiasecInfoDTO() {}
        
        public DetailedRiasecInfoDTO(com.app.models.RiasecDetails riasecDetails) {
            if (riasecDetails != null) {
                this.learningStyleSummary = riasecDetails.getLearningStyleSummary();
                this.learningStyleDetails = riasecDetails.getLearningStyleDetails();
                this.learningStyleEnvironments = riasecDetails.getLearningStyleEnvironments();
                this.learningStyleResources = riasecDetails.getLearningStyleResources();
                this.studyTipsSummary = riasecDetails.getStudyTipsSummary();
                this.studyTipsDetails = riasecDetails.getStudyTipsDetails();
                this.studyTipsDos = riasecDetails.getStudyTipsDos();
                this.studyTipsDonts = riasecDetails.getStudyTipsDonts();
                this.studyTipsCommonMistakes = riasecDetails.getStudyTipsCommonMistakes();
                this.growthStrengths = riasecDetails.getGrowthStrengths();
                this.growthWeaknesses = riasecDetails.getGrowthWeaknesses();
                this.growthOpportunities = riasecDetails.getGrowthOpportunities();
                this.growthChallenges = riasecDetails.getGrowthChallenges();
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
    
    public String getDetailedExplanation() { return detailedExplanation; }
    public void setDetailedExplanation(String detailedExplanation) { this.detailedExplanation = detailedExplanation; }
    
    // Removed getters/setters for the removed fields
    
    public String getStudentGoals() { return studentGoals; }
    public void setStudentGoals(String studentGoals) { this.studentGoals = studentGoals; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public Boolean getIsFromPLMar() { return isFromPLMar; }
    public void setIsFromPLMar(Boolean isFromPLMar) { this.isFromPLMar = isFromPLMar; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }
    
    public Map<String, Integer> getRiasecScores() { return riasecScores; }
    public void setRiasecScores(Map<String, Integer> riasecScores) { this.riasecScores = riasecScores; }
    
    public Map<String, Integer> getMbtiScores() { return mbtiScores; }
    public void setMbtiScores(Map<String, Integer> mbtiScores) { this.mbtiScores = mbtiScores; }
    
    public DetailedMbtiInfoDTO getDetailedMbtiInfo() { return detailedMbtiInfo; }
    public void setDetailedMbtiInfo(DetailedMbtiInfoDTO detailedMbtiInfo) { this.detailedMbtiInfo = detailedMbtiInfo; }
    
    public DetailedRiasecInfoDTO getDetailedRiasecInfo() { return detailedRiasecInfo; }
    public void setDetailedRiasecInfo(DetailedRiasecInfoDTO detailedRiasecInfo) { this.detailedRiasecInfo = detailedRiasecInfo; }
    
    public CareerDevelopmentPlanDTO getCareerDevelopmentPlan() { return careerDevelopmentPlan; }
    public void setCareerDevelopmentPlan(CareerDevelopmentPlanDTO careerDevelopmentPlan) { this.careerDevelopmentPlan = careerDevelopmentPlan; }
    
    public CourseDevelopmentPlanDTO getCourseDevelopmentPlan() { return courseDevelopmentPlan; }
    public void setCourseDevelopmentPlan(CourseDevelopmentPlanDTO courseDevelopmentPlan) { this.courseDevelopmentPlan = courseDevelopmentPlan; }
}
