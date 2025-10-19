package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "riasec_details")
public class RiasecDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "riasec_type", length = 20, nullable = false, unique = true)
    private String riasecType;
    
    @Column(name = "learning_style_summary", columnDefinition = "TEXT", nullable = false)
    private String learningStyleSummary;
    
    @Column(name = "learning_style_details", columnDefinition = "TEXT", nullable = false)
    private String learningStyleDetails;
    
    @Column(name = "learning_style_environments", columnDefinition = "TEXT", nullable = false)
    private String learningStyleEnvironments;
    
    @Column(name = "learning_style_resources", columnDefinition = "TEXT", nullable = false)
    private String learningStyleResources;
    
    @Column(name = "study_tips_summary", columnDefinition = "TEXT", nullable = false)
    private String studyTipsSummary;
    
    @Column(name = "study_tips_details", columnDefinition = "TEXT", nullable = false)
    private String studyTipsDetails;
    
    @Column(name = "study_tips_dos", columnDefinition = "TEXT", nullable = false)
    private String studyTipsDos;
    
    @Column(name = "study_tips_donts", columnDefinition = "TEXT", nullable = false)
    private String studyTipsDonts;
    
    @Column(name = "study_tips_common_mistakes", columnDefinition = "TEXT", nullable = false)
    private String studyTipsCommonMistakes;
    
    @Column(name = "growth_strengths", columnDefinition = "TEXT", nullable = false)
    private String growthStrengths;
    
    @Column(name = "growth_weaknesses", columnDefinition = "TEXT", nullable = false)
    private String growthWeaknesses;
    
    @Column(name = "growth_opportunities", columnDefinition = "TEXT", nullable = false)
    private String growthOpportunities;
    
    @Column(name = "growth_challenges", columnDefinition = "TEXT", nullable = false)
    private String growthChallenges;
    
    // Constructors
    public RiasecDetails() {}
    
    public RiasecDetails(String riasecType, String learningStyleSummary, String learningStyleDetails,
                        String learningStyleEnvironments, String learningStyleResources,
                        String studyTipsSummary, String studyTipsDetails, String studyTipsDos,
                        String studyTipsDonts, String studyTipsCommonMistakes,
                        String growthStrengths, String growthWeaknesses, String growthOpportunities,
                        String growthChallenges) {
        this.riasecType = riasecType;
        this.learningStyleSummary = learningStyleSummary;
        this.learningStyleDetails = learningStyleDetails;
        this.learningStyleEnvironments = learningStyleEnvironments;
        this.learningStyleResources = learningStyleResources;
        this.studyTipsSummary = studyTipsSummary;
        this.studyTipsDetails = studyTipsDetails;
        this.studyTipsDos = studyTipsDos;
        this.studyTipsDonts = studyTipsDonts;
        this.studyTipsCommonMistakes = studyTipsCommonMistakes;
        this.growthStrengths = growthStrengths;
        this.growthWeaknesses = growthWeaknesses;
        this.growthOpportunities = growthOpportunities;
        this.growthChallenges = growthChallenges;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRiasecType() {
        return riasecType;
    }
    
    public void setRiasecType(String riasecType) {
        this.riasecType = riasecType;
    }
    
    public String getLearningStyleSummary() {
        return learningStyleSummary;
    }
    
    public void setLearningStyleSummary(String learningStyleSummary) {
        this.learningStyleSummary = learningStyleSummary;
    }
    
    public String getLearningStyleDetails() {
        return learningStyleDetails;
    }
    
    public void setLearningStyleDetails(String learningStyleDetails) {
        this.learningStyleDetails = learningStyleDetails;
    }
    
    public String getLearningStyleEnvironments() {
        return learningStyleEnvironments;
    }
    
    public void setLearningStyleEnvironments(String learningStyleEnvironments) {
        this.learningStyleEnvironments = learningStyleEnvironments;
    }
    
    public String getLearningStyleResources() {
        return learningStyleResources;
    }
    
    public void setLearningStyleResources(String learningStyleResources) {
        this.learningStyleResources = learningStyleResources;
    }
    
    public String getStudyTipsSummary() {
        return studyTipsSummary;
    }
    
    public void setStudyTipsSummary(String studyTipsSummary) {
        this.studyTipsSummary = studyTipsSummary;
    }
    
    public String getStudyTipsDetails() {
        return studyTipsDetails;
    }
    
    public void setStudyTipsDetails(String studyTipsDetails) {
        this.studyTipsDetails = studyTipsDetails;
    }
    
    public String getStudyTipsDos() {
        return studyTipsDos;
    }
    
    public void setStudyTipsDos(String studyTipsDos) {
        this.studyTipsDos = studyTipsDos;
    }
    
    public String getStudyTipsDonts() {
        return studyTipsDonts;
    }
    
    public void setStudyTipsDonts(String studyTipsDonts) {
        this.studyTipsDonts = studyTipsDonts;
    }
    
    public String getStudyTipsCommonMistakes() {
        return studyTipsCommonMistakes;
    }
    
    public void setStudyTipsCommonMistakes(String studyTipsCommonMistakes) {
        this.studyTipsCommonMistakes = studyTipsCommonMistakes;
    }
    
    public String getGrowthStrengths() {
        return growthStrengths;
    }
    
    public void setGrowthStrengths(String growthStrengths) {
        this.growthStrengths = growthStrengths;
    }
    
    public String getGrowthWeaknesses() {
        return growthWeaknesses;
    }
    
    public void setGrowthWeaknesses(String growthWeaknesses) {
        this.growthWeaknesses = growthWeaknesses;
    }
    
    public String getGrowthOpportunities() {
        return growthOpportunities;
    }
    
    public void setGrowthOpportunities(String growthOpportunities) {
        this.growthOpportunities = growthOpportunities;
    }
    
    public String getGrowthChallenges() {
        return growthChallenges;
    }
    
    public void setGrowthChallenges(String growthChallenges) {
        this.growthChallenges = growthChallenges;
    }
    
    @Override
    public String toString() {
        return "RiasecDetails{" +
                "id=" + id +
                ", riasecType='" + riasecType + '\'' +
                ", learningStyleSummary='" + learningStyleSummary + '\'' +
                ", studyTipsSummary='" + studyTipsSummary + '\'' +
                '}';
    }
}

