package com.app.dto;

import java.util.List;

public class CareerDevelopmentPlanDTO {
    
    public static class CareerDetails {
        private String careerName;
        private String description;
        
        // Development Plan fields
        private String introduction;
        private String keySkills;
        private String academicsActivities;
        private String softSkills;
        private String growthOpportunities;
        
        // Career Info fields
        private String careerFit;
        private String educationLevel;
        private String workEnvironment;
        private String careerPath;
        
        // Default constructor
        public CareerDetails() {}
        
        // Constructor with all fields
        public CareerDetails(String careerName, String description, String introduction, 
                           String keySkills, String academicsActivities, String softSkills, 
                           String growthOpportunities, String careerFit, String educationLevel, 
                           String workEnvironment, String careerPath) {
            this.careerName = careerName;
            this.description = description;
            this.introduction = introduction;
            this.keySkills = keySkills;
            this.academicsActivities = academicsActivities;
            this.softSkills = softSkills;
            this.growthOpportunities = growthOpportunities;
            this.careerFit = careerFit;
            this.educationLevel = educationLevel;
            this.workEnvironment = workEnvironment;
            this.careerPath = careerPath;
        }
        
        // Getters and Setters
        public String getCareerName() {
            return careerName;
        }
        
        public void setCareerName(String careerName) {
            this.careerName = careerName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getIntroduction() {
            return introduction;
        }
        
        public void setIntroduction(String introduction) {
            this.introduction = introduction;
        }
        
        public String getKeySkills() {
            return keySkills;
        }
        
        public void setKeySkills(String keySkills) {
            this.keySkills = keySkills;
        }
        
        public String getAcademicsActivities() {
            return academicsActivities;
        }
        
        public void setAcademicsActivities(String academicsActivities) {
            this.academicsActivities = academicsActivities;
        }
        
        
        public String getSoftSkills() {
            return softSkills;
        }
        
        public void setSoftSkills(String softSkills) {
            this.softSkills = softSkills;
        }
        
        public String getGrowthOpportunities() {
            return growthOpportunities;
        }
        
        public void setGrowthOpportunities(String growthOpportunities) {
            this.growthOpportunities = growthOpportunities;
        }
        
        public String getCareerFit() {
            return careerFit;
        }
        
        public void setCareerFit(String careerFit) {
            this.careerFit = careerFit;
        }
        
        public String getEducationLevel() {
            return educationLevel;
        }
        
        public void setEducationLevel(String educationLevel) {
            this.educationLevel = educationLevel;
        }
        
        public String getWorkEnvironment() {
            return workEnvironment;
        }
        
        public void setWorkEnvironment(String workEnvironment) {
            this.workEnvironment = workEnvironment;
        }
        
        public String getCareerPath() {
            return careerPath;
        }
        
        public void setCareerPath(String careerPath) {
            this.careerPath = careerPath;
        }
    }
    
    private String mbtiType;
    private String riasecCode;
    private List<CareerDetails> careerDetails;
    
    // Default constructor
    public CareerDevelopmentPlanDTO() {}
    
    // Constructor
    public CareerDevelopmentPlanDTO(String mbtiType, String riasecCode, List<CareerDetails> careerDetails) {
        this.mbtiType = mbtiType;
        this.riasecCode = riasecCode;
        this.careerDetails = careerDetails;
    }
    
    // Getters and Setters
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
    
    public List<CareerDetails> getCareerDetails() {
        return careerDetails;
    }
    
    public void setCareerDetails(List<CareerDetails> careerDetails) {
        this.careerDetails = careerDetails;
    }
}
