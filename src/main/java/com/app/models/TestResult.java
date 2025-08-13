package com.app.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_results")
public class TestResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "guest_token")
    private UUID guestToken;
    
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    
    @Column(name = "mbti_type", length = 4)
    private String mbtiType;
    
    @Column(name = "riasec_code", length = 4)
    private String riasecCode;
    
    @Column(name = "course_path", columnDefinition = "TEXT")
    private String coursePath;
    
    @Column(name = "career_suggestions", columnDefinition = "TEXT")
    private String careerSuggestions;
    
    @Column(name = "learning_style", columnDefinition = "TEXT")
    private String learningStyle;
    
    @Column(name = "study_tips", columnDefinition = "TEXT")
    private String studyTips;
    
    @Column(name = "personality_growth_tips", columnDefinition = "TEXT")
    private String personalityGrowthTips;
    
    @Column(name = "student_goals", columnDefinition = "TEXT")
    private String studentGoals;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    // Constructors
    public TestResult() {
        this.sessionId = UUID.randomUUID();
        this.generatedAt = LocalDateTime.now();
        this.takenAt = LocalDateTime.now();
    }
    
    public TestResult(Long userId) {
        this();
        this.userId = userId;
    }
    
    public TestResult(UUID guestToken) {
        this();
        this.guestToken = guestToken;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public UUID getGuestToken() {
        return guestToken;
    }
    
    public void setGuestToken(UUID guestToken) {
        this.guestToken = guestToken;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
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
    
    public String getCoursePath() {
        return coursePath;
    }
    
    public void setCoursePath(String coursePath) {
        this.coursePath = coursePath;
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
    
    public String getStudentGoals() {
        return studentGoals;
    }
    
    public void setStudentGoals(String studentGoals) {
        this.studentGoals = studentGoals;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID();
        }
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        return "TestResult{" +
                "id=" + id +
                ", userId=" + userId +
                ", guestToken=" + guestToken +
                ", sessionId=" + sessionId +
                ", mbtiType='" + mbtiType + '\'' +
                ", riasecCode='" + riasecCode + '\'' +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
