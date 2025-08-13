package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "mbti_riasec_mappings")
public class MbtiRiasecMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "mbti_type", length = 4)
    private String mbtiType;
    
    @Column(name = "riasec_code", length = 10)
    private String riasecCode;
    
    @Column(name = "course_name", length = 255)
    private String courseName;
    
    @Column(name = "course_description", columnDefinition = "TEXT")
    private String courseDescription;
    
    @Column(name = "career_options", columnDefinition = "TEXT")
    private String careerOptions;
    
    @Column(name = "university", length = 255)
    private String university;
    
    @Column(name = "program_type", length = 100)
    private String programType;
    
    @Column(name = "duration", length = 50)
    private String duration;
    
    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;
    
    @Column(name = "salary_range", length = 100)
    private String salaryRange;
    
    @Column(name = "job_outlook", length = 100)
    private String jobOutlook;
    
    @Column(name = "skills_needed", columnDefinition = "TEXT")
    private String skillsNeeded;
    
    @Column(name = "work_environment", columnDefinition = "TEXT")
    private String workEnvironment;
    
    @Column(name = "match_score", columnDefinition = "DECIMAL(3,2)")
    private Double matchScore;
    
    @Column(name = "category", length = 100)
    private String category;
    
    // Constructors
    public MbtiRiasecMapping() {}
    
    public MbtiRiasecMapping(String mbtiType, String riasecCode, String courseName) {
        this.mbtiType = mbtiType;
        this.riasecCode = riasecCode;
        this.courseName = courseName;
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
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getCourseDescription() {
        return courseDescription;
    }
    
    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }
    
    public String getCareerOptions() {
        return careerOptions;
    }
    
    public void setCareerOptions(String careerOptions) {
        this.careerOptions = careerOptions;
    }
    
    public String getUniversity() {
        return university;
    }
    
    public void setUniversity(String university) {
        this.university = university;
    }
    
    public String getProgramType() {
        return programType;
    }
    
    public void setProgramType(String programType) {
        this.programType = programType;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public String getRequirements() {
        return requirements;
    }
    
    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
    
    public String getSalaryRange() {
        return salaryRange;
    }
    
    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }
    
    public String getJobOutlook() {
        return jobOutlook;
    }
    
    public void setJobOutlook(String jobOutlook) {
        this.jobOutlook = jobOutlook;
    }
    
    public String getSkillsNeeded() {
        return skillsNeeded;
    }
    
    public void setSkillsNeeded(String skillsNeeded) {
        this.skillsNeeded = skillsNeeded;
    }
    
    public String getWorkEnvironment() {
        return workEnvironment;
    }
    
    public void setWorkEnvironment(String workEnvironment) {
        this.workEnvironment = workEnvironment;
    }
    
    public Double getMatchScore() {
        return matchScore;
    }
    
    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "MbtiRiasecMapping{" +
                "id=" + id +
                ", mbtiType='" + mbtiType + '\'' +
                ", riasecCode='" + riasecCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", university='" + university + '\'' +
                ", matchScore=" + matchScore +
                '}';
    }
}
