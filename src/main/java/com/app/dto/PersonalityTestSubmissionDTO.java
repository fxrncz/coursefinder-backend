package com.app.dto;

import java.util.List;
import java.util.Map;

public class PersonalityTestSubmissionDTO {
    
    private Long userId;
    private Map<Integer, Integer> answers; // questionIndex -> answer (1-5)
    private GoalSettingAnswersDTO goalSettings;
    
    // Constructors
    public PersonalityTestSubmissionDTO() {}
    
    public PersonalityTestSubmissionDTO(Long userId, Map<Integer, Integer> answers, GoalSettingAnswersDTO goalSettings) {
        this.userId = userId;
        this.answers = answers;
        this.goalSettings = goalSettings;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Map<Integer, Integer> getAnswers() {
        return answers;
    }
    
    public void setAnswers(Map<Integer, Integer> answers) {
        this.answers = answers;
    }
    
    public GoalSettingAnswersDTO getGoalSettings() {
        return goalSettings;
    }
    
    public void setGoalSettings(GoalSettingAnswersDTO goalSettings) {
        this.goalSettings = goalSettings;
    }
    
    // Inner class for Goal Setting Answers
    public static class GoalSettingAnswersDTO {
        private String priority;
        private List<String> learningStyle;
        private String environment;
        private String motivation;
        private String concern;
        private Integer confidence;
        private String routine;
        private String impact;
        private Integer age;
        private String gender;
        private Boolean isFromPLMar;
        
        // Constructors
        public GoalSettingAnswersDTO() {}
        
        // Getters and Setters
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public List<String> getLearningStyle() {
            return learningStyle;
        }
        
        public void setLearningStyle(List<String> learningStyle) {
            this.learningStyle = learningStyle;
        }
        
        public String getEnvironment() {
            return environment;
        }
        
        public void setEnvironment(String environment) {
            this.environment = environment;
        }
        
        public String getMotivation() {
            return motivation;
        }
        
        public void setMotivation(String motivation) {
            this.motivation = motivation;
        }
        
        public String getConcern() {
            return concern;
        }
        
        public void setConcern(String concern) {
            this.concern = concern;
        }
        
        public Integer getConfidence() {
            return confidence;
        }
        
        public void setConfidence(Integer confidence) {
            this.confidence = confidence;
        }
        
        public String getRoutine() {
            return routine;
        }
        
        public void setRoutine(String routine) {
            this.routine = routine;
        }
        
        public String getImpact() {
            return impact;
        }
        
        public void setImpact(String impact) {
            this.impact = impact;
        }
        
        public Integer getAge() {
            return age;
        }
        
        public void setAge(Integer age) {
            this.age = age;
        }
        
        public String getGender() {
            return gender;
        }
        
        public void setGender(String gender) {
            this.gender = gender;
        }
        
        public Boolean getIsFromPLMar() {
            return isFromPLMar;
        }
        
        public void setIsFromPLMar(Boolean isFromPLMar) {
            this.isFromPLMar = isFromPLMar;
        }
        
        @Override
        public String toString() {
            return "GoalSettingAnswersDTO{" +
                    "priority='" + priority + '\'' +
                    ", learningStyle=" + learningStyle +
                    ", environment='" + environment + '\'' +
                    ", motivation='" + motivation + '\'' +
                    ", concern='" + concern + '\'' +
                    ", confidence=" + confidence +
                    ", routine='" + routine + '\'' +
                    ", impact='" + impact + '\'' +
                    ", age=" + age +
                    ", gender='" + gender + '\'' +
                    ", isFromPLMar=" + isFromPLMar +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "PersonalityTestSubmissionDTO{" +
                "userId=" + userId +
                ", answers=" + answers +
                ", goalSettings=" + goalSettings +
                '}';
    }
}
