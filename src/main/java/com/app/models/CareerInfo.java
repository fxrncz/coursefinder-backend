package com.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "career_info")
public class CareerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "career", nullable = false, unique = true)
    private String career;

    @Column(name = "career_fit", nullable = false)
    private String careerFit;

    @Column(name = "education_level", nullable = false)
    private String educationLevel;

    @Column(name = "work_environment", nullable = false)
    private String workEnvironment;

    @Column(name = "career_path", nullable = false)
    private String careerPath;

    // Default constructor
    public CareerInfo() {}

    // Constructor with all fields
    public CareerInfo(String career, String careerFit, String educationLevel, 
                     String workEnvironment, String careerPath) {
        this.career = career;
        this.careerFit = careerFit;
        this.educationLevel = educationLevel;
        this.workEnvironment = workEnvironment;
        this.careerPath = careerPath;
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

    @Override
    public String toString() {
        return "CareerInfo{" +
                "id=" + id +
                ", career='" + career + '\'' +
                ", careerFit='" + careerFit + '\'' +
                ", educationLevel='" + educationLevel + '\'' +
                ", workEnvironment='" + workEnvironment + '\'' +
                ", careerPath='" + careerPath + '\'' +
                '}';
    }
}
