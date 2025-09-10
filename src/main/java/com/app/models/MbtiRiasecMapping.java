package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "mbti_riasec_mappings")
public class MbtiRiasecMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "mbti_type", length = 10, nullable = false)
    private String mbtiType;
    
    @Column(name = "riasec_code", length = 10, nullable = false)
    private String riasecCode;
    
    @Column(name = "suggested_courses", columnDefinition = "TEXT", nullable = false)
    private String suggestedCourses;
    
    @Column(name = "career_suggestions", columnDefinition = "TEXT", nullable = false)
    private String careerSuggestions;
    
    @Column(name = "learning_style", columnDefinition = "TEXT", nullable = false)
    private String learningStyle;
    
    @Column(name = "study_tips", columnDefinition = "TEXT", nullable = false)
    private String studyTips;
    
    @Column(name = "personality_growth_tips", columnDefinition = "TEXT", nullable = false)
    private String personalityGrowthTips;
    
    // Constructors
    public MbtiRiasecMapping() {}
    
    public MbtiRiasecMapping(String mbtiType, String riasecCode, String suggestedCourses, 
                            String careerSuggestions, String learningStyle, String studyTips, 
                            String personalityGrowthTips) {
        this.mbtiType = mbtiType;
        this.riasecCode = riasecCode;
        this.suggestedCourses = suggestedCourses;
        this.careerSuggestions = careerSuggestions;
        this.learningStyle = learningStyle;
        this.studyTips = studyTips;
        this.personalityGrowthTips = personalityGrowthTips;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMbtiType() {
        return mbtiType;
    }
    
    public void setMbtiType(String mbtiType) {
        this.mbtiType = mbtiType;
    }
    
    public String getRiasecCode() {
        return riasecCode;
    }
    
    public void setRiasecCode(String riasecCode) {
        this.riasecCode = riasecCode;
    }
    
    public String getSuggestedCourses() {
        return suggestedCourses;
    }
    
    public void setSuggestedCourses(String suggestedCourses) {
        this.suggestedCourses = suggestedCourses;
    }
    
    public String getCareerSuggestions() {
        return careerSuggestions;
    }
    
    public void setCareerSuggestions(String careerSuggestions) {
        this.careerSuggestions = careerSuggestions;
    }
    
    public String getLearningStyle() {
        return learningStyle;
    }
    
    public void setLearningStyle(String learningStyle) {
        this.learningStyle = learningStyle;
    }
    
    public String getStudyTips() {
        return studyTips;
    }
    
    public void setStudyTips(String studyTips) {
        this.studyTips = studyTips;
    }
    
    public String getPersonalityGrowthTips() {
        return personalityGrowthTips;
    }
    
    public void setPersonalityGrowthTips(String personalityGrowthTips) {
        this.personalityGrowthTips = personalityGrowthTips;
    }
    
    @Override
    public String toString() {
        return "MbtiRiasecMapping{" +
                "id=" + id +
                ", mbtiType='" + mbtiType + '\'' +
                ", riasecCode='" + riasecCode + '\'' +
                ", suggestedCourses='" + suggestedCourses + '\'' +
                ", careerSuggestions='" + careerSuggestions + '\'' +
                ", learningStyle='" + learningStyle + '\'' +
                '}';
    }
}
