package com.app.dto;

import java.util.List;

public class CourseDevelopmentPlanDTO {
    
    public static class CourseDetails {
        private String courseName;
        private String description;  // Course description from updated_course_description table
        
        // Development Plan fields - 7 sections
        private String courseOverview;      // Course Overview (Introduction equivalent)
        private String coreCompetencies;    // Core Competencies (Key Skills equivalent)
        private String acadsExtra;          // Academic & Extracurricular Activities
        private String subjMaster;          // Subjects to Master (Certifications equivalent)
        private String softSkills;          // Soft Skills & Habits to Develop
        private String careerReadiness;     // Career Readiness (alternative naming)
        private String growth;              // Growth Opportunities & Career Pathways
        
        // Default constructor
        public CourseDetails() {}
        
        // Constructor with all fields
        public CourseDetails(String courseName, String description, String courseOverview, 
                           String coreCompetencies, String acadsExtra, String subjMaster, 
                           String softSkills, String careerReadiness, String growth) {
            this.courseName = courseName;
            this.description = description;
            this.courseOverview = courseOverview;
            this.coreCompetencies = coreCompetencies;
            this.acadsExtra = acadsExtra;
            this.subjMaster = subjMaster;
            this.softSkills = softSkills;
            this.careerReadiness = careerReadiness;
            this.growth = growth;
        }
        
        // Getters and Setters
        public String getCourseName() {
            return courseName;
        }
        
        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getCourseOverview() {
            return courseOverview;
        }
        
        public void setCourseOverview(String courseOverview) {
            this.courseOverview = courseOverview;
        }
        
        public String getCoreCompetencies() {
            return coreCompetencies;
        }
        
        public void setCoreCompetencies(String coreCompetencies) {
            this.coreCompetencies = coreCompetencies;
        }
        
        public String getAcadsExtra() {
            return acadsExtra;
        }
        
        public void setAcadsExtra(String acadsExtra) {
            this.acadsExtra = acadsExtra;
        }
        
        public String getSubjMaster() {
            return subjMaster;
        }
        
        public void setSubjMaster(String subjMaster) {
            this.subjMaster = subjMaster;
        }
        
        public String getSoftSkills() {
            return softSkills;
        }
        
        public void setSoftSkills(String softSkills) {
            this.softSkills = softSkills;
        }
        
        public String getCareerReadiness() {
            return careerReadiness;
        }
        
        public void setCareerReadiness(String careerReadiness) {
            this.careerReadiness = careerReadiness;
        }
        
        public String getGrowth() {
            return growth;
        }
        
        public void setGrowth(String growth) {
            this.growth = growth;
        }
    }
    
    private String mbtiType;
    private String riasecCode;
    private List<CourseDetails> courseDetails;
    
    // Default constructor
    public CourseDevelopmentPlanDTO() {}
    
    // Constructor
    public CourseDevelopmentPlanDTO(String mbtiType, String riasecCode, List<CourseDetails> courseDetails) {
        this.mbtiType = mbtiType;
        this.riasecCode = riasecCode;
        this.courseDetails = courseDetails;
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
    
    public List<CourseDetails> getCourseDetails() {
        return courseDetails;
    }
    
    public void setCourseDetails(List<CourseDetails> courseDetails) {
        this.courseDetails = courseDetails;
    }
}

