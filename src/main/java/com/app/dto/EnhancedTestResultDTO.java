package com.app.dto;

import com.app.models.MbtiDetails;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    
    // AI Validation Status
    private AiValidationStatusDTO aiValidationStatus;
    
    // AI-powered course and career rankings
    private List<AiCourseRanking> aiCourseRankings;
    private List<AiCareerRanking> aiCareerRankings;
    
    // AI Model Comparison Results
    private AiModelComparison aiModelComparison;
    
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
    
    public AiValidationStatusDTO getAiValidationStatus() { return aiValidationStatus; }
    public void setAiValidationStatus(AiValidationStatusDTO aiValidationStatus) { this.aiValidationStatus = aiValidationStatus; }
    
    public List<AiCourseRanking> getAiCourseRankings() { return aiCourseRankings; }
    public void setAiCourseRankings(List<AiCourseRanking> aiCourseRankings) { this.aiCourseRankings = aiCourseRankings; }
    
    public List<AiCareerRanking> getAiCareerRankings() { return aiCareerRankings; }
    public void setAiCareerRankings(List<AiCareerRanking> aiCareerRankings) { this.aiCareerRankings = aiCareerRankings; }
    
    public AiModelComparison getAiModelComparison() { return aiModelComparison; }
    public void setAiModelComparison(AiModelComparison aiModelComparison) { this.aiModelComparison = aiModelComparison; }
    
    // AI Validation Status DTO
    public static class AiValidationStatusDTO {
        private boolean validated;
        private String validationStatus;
        private double validationScore;
        private String validationMessage;
        private String validatedAt;
        private List<String> validationIssues;
        
        public AiValidationStatusDTO() {
            this.validationIssues = new ArrayList<>();
        }
        
        // Getters and setters
        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }
        
        public String getValidationStatus() { return validationStatus; }
        public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
        
        public double getValidationScore() { return validationScore; }
        public void setValidationScore(double validationScore) { this.validationScore = validationScore; }
        
        public String getValidationMessage() { return validationMessage; }
        public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
        
        public String getValidatedAt() { return validatedAt; }
        public void setValidatedAt(String validatedAt) { this.validatedAt = validatedAt; }
        
        public List<String> getValidationIssues() { return validationIssues; }
        public void setValidationIssues(List<String> validationIssues) { this.validationIssues = validationIssues; }
    }
    
    // AI Course Ranking DTO
    public static class AiCourseRanking {
        private int rank;
        private String courseName;
        private double matchScore;
        private String matchReason;
        private boolean aiRecommended;
        
        public AiCourseRanking() {}
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public double getMatchScore() { return matchScore; }
        public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
        
        public String getMatchReason() { return matchReason; }
        public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
        
        public boolean isAiRecommended() { return aiRecommended; }
        public void setAiRecommended(boolean aiRecommended) { this.aiRecommended = aiRecommended; }
    }
    
    // AI Career Ranking DTO
    public static class AiCareerRanking {
        private int rank;
        private String careerName;
        private double matchScore;
        private String matchReason;
        private boolean aiRecommended;
        
        public AiCareerRanking() {}
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public double getMatchScore() { return matchScore; }
        public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
        
        public String getMatchReason() { return matchReason; }
        public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
        
        public boolean isAiRecommended() { return aiRecommended; }
        public void setAiRecommended(boolean aiRecommended) { this.aiRecommended = aiRecommended; }
    }
    
    // AI Model Comparison DTO
    public static class AiModelComparison {
        private String mbtiType;
        private String riasecCode;
        private String model1Name;
        private String model2Name;
        private List<CourseComparison> courseComparisons;
        private List<CareerComparison> careerComparisons;
        
        public AiModelComparison() {}
        
        // Getters and setters
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public String getModel1Name() { return model1Name; }
        public void setModel1Name(String model1Name) { this.model1Name = model1Name; }
        
        public String getModel2Name() { return model2Name; }
        public void setModel2Name(String model2Name) { this.model2Name = model2Name; }
        
        public List<CourseComparison> getCourseComparisons() { return courseComparisons; }
        public void setCourseComparisons(List<CourseComparison> courseComparisons) { this.courseComparisons = courseComparisons; }
        
        public List<CareerComparison> getCareerComparisons() { return careerComparisons; }
        public void setCareerComparisons(List<CareerComparison> careerComparisons) { this.careerComparisons = careerComparisons; }
    }
    
    public static class CourseComparison {
        private String courseName;
        private double model1Score;
        private double model2Score;
        private String model1Analysis;
        private String model2Analysis;
        private String agreement;
        
        public CourseComparison() {}
        
        // Getters and setters
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public double getModel1Score() { return model1Score; }
        public void setModel1Score(double model1Score) { this.model1Score = model1Score; }
        
        public double getModel2Score() { return model2Score; }
        public void setModel2Score(double model2Score) { this.model2Score = model2Score; }
        
        public String getModel1Analysis() { return model1Analysis; }
        public void setModel1Analysis(String model1Analysis) { this.model1Analysis = model1Analysis; }
        
        public String getModel2Analysis() { return model2Analysis; }
        public void setModel2Analysis(String model2Analysis) { this.model2Analysis = model2Analysis; }
        
        public String getAgreement() { return agreement; }
        public void setAgreement(String agreement) { this.agreement = agreement; }
    }
    
    public static class CareerComparison {
        private String careerName;
        private double model1Score;
        private double model2Score;
        private String model1Analysis;
        private String model2Analysis;
        private String agreement;
        
        public CareerComparison() {}
        
        // Getters and setters
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public double getModel1Score() { return model1Score; }
        public void setModel1Score(double model1Score) { this.model1Score = model1Score; }
        
        public double getModel2Score() { return model2Score; }
        public void setModel2Score(double model2Score) { this.model2Score = model2Score; }
        
        public String getModel1Analysis() { return model1Analysis; }
        public void setModel1Analysis(String model1Analysis) { this.model1Analysis = model1Analysis; }
        
        public String getModel2Analysis() { return model2Analysis; }
        public void setModel2Analysis(String model2Analysis) { this.model2Analysis = model2Analysis; }
        
        public String getAgreement() { return agreement; }
        public void setAgreement(String agreement) { this.agreement = agreement; }
    }
}
