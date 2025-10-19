package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "development_plan")
public class DevelopmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "career", nullable = false, unique = true)
    private String career;

    @Column(name = "introduction", nullable = false, columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "key_skills", nullable = false, columnDefinition = "TEXT")
    private String keySkills;

    @Column(name = "academics_activities", nullable = false, columnDefinition = "TEXT")
    private String academicsActivities;


    @Column(name = "soft_skills", nullable = false, columnDefinition = "TEXT")
    private String softSkills;

    @Column(name = "growth_opportunities", nullable = false, columnDefinition = "TEXT")
    private String growthOpportunities;

    // Default constructor
    public DevelopmentPlan() {}

    // Constructor with all fields
    public DevelopmentPlan(String career, String introduction, String keySkills, 
                          String academicsActivities, String softSkills, String growthOpportunities) {
        this.career = career;
        this.introduction = introduction;
        this.keySkills = keySkills;
        this.academicsActivities = academicsActivities;
        this.softSkills = softSkills;
        this.growthOpportunities = growthOpportunities;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
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

    @Override
    public String toString() {
        return "DevelopmentPlan{" +
                "id=" + id +
                ", career='" + career + '\'' +
                ", introduction='" + introduction + '\'' +
                ", keySkills='" + keySkills + '\'' +
                ", academicsActivities='" + academicsActivities + '\'' +
                ", softSkills='" + softSkills + '\'' +
                ", growthOpportunities='" + growthOpportunities + '\'' +
                '}';
    }
}
