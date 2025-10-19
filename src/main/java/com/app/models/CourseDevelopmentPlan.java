package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "course_development_plan")
public class CourseDevelopmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course", nullable = false, unique = true)
    private String course;

    @Column(name = "course_overview", nullable = false, columnDefinition = "TEXT")
    private String courseOverview;

    @Column(name = "core_competencies", nullable = false, columnDefinition = "TEXT")
    private String coreCompetencies;

    @Column(name = "acads_extra", nullable = false, columnDefinition = "TEXT")
    private String acadsExtra;

    @Column(name = "subj_master", nullable = false, columnDefinition = "TEXT")
    private String subjMaster;

    @Column(name = "soft_skills", nullable = false, columnDefinition = "TEXT")
    private String softSkills;

    @Column(name = "career_readiness", nullable = false, columnDefinition = "TEXT")
    private String careerReadiness;

    @Column(name = "growth", nullable = false, columnDefinition = "TEXT")
    private String growth;

    // Default constructor
    public CourseDevelopmentPlan() {}

    // Constructor with all fields
    public CourseDevelopmentPlan(String course, String courseOverview, String coreCompetencies, 
                          String acadsExtra, String subjMaster, 
                          String softSkills, String careerReadiness, String growth) {
        this.course = course;
        this.courseOverview = courseOverview;
        this.coreCompetencies = coreCompetencies;
        this.acadsExtra = acadsExtra;
        this.subjMaster = subjMaster;
        this.softSkills = softSkills;
        this.careerReadiness = careerReadiness;
        this.growth = growth;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
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

    @Override
    public String toString() {
        return "CourseDevelopmentPlan{" +
                "id=" + id +
                ", course='" + course + '\'' +
                ", courseOverview='" + courseOverview + '\'' +
                ", coreCompetencies='" + coreCompetencies + '\'' +
                ", acadsExtra='" + acadsExtra + '\'' +
                ", subjMaster='" + subjMaster + '\'' +
                ", softSkills='" + softSkills + '\'' +
                ", careerReadiness='" + careerReadiness + '\'' +
                ", growth='" + growth + '\'' +
                '}';
    }
}

