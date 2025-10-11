package com.app.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mbti_riasec_matching")
public class MbtiRiasecMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "mbti_type", length = 10, nullable = false)
    private String mbtiType;
    
    @Column(name = "riasec_code", length = 10, nullable = false)
    private String riasecCode;
    
    // New columns: courses TEXT[], careers TEXT[], explanation TEXT
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "courses", columnDefinition = "TEXT[]")
    private String[] courses;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "careers", columnDefinition = "TEXT[]")
    private String[] careers;
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    // Constructors
    public MbtiRiasecMapping() {}
    
    public MbtiRiasecMapping(String mbtiType, String riasecCode, String[] courses, String[] careers, String explanation) {
        this.mbtiType = mbtiType;
        this.riasecCode = riasecCode;
        this.courses = courses;
        this.careers = careers;
        this.explanation = explanation;
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
    
    public String[] getCourses() { return courses; }
    public void setCourses(String[] courses) { this.courses = courses; }
    public String[] getCareers() { return careers; }
    public void setCareers(String[] careers) { this.careers = careers; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    
    @Override
    public String toString() {
        return "MbtiRiasecMapping{" +
                "id=" + id +
                ", mbtiType='" + mbtiType + '\'' +
                ", riasecCode='" + riasecCode + '\'' +
                ", courses='" + (courses == null ? null : String.join(", ", courses)) + '\'' +
                ", careers='" + (careers == null ? null : String.join(", ", careers)) + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
