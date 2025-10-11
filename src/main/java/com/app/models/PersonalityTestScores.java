package com.app.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personality_test_scores")
public class PersonalityTestScores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_result_id", nullable = false)
    private Long testResultId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    // RIASEC Scores (raw and percentage)
    @Column(name = "riasec_r_raw", nullable = false)
    private Integer riasecRRaw;

    @Column(name = "riasec_r_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecRPercentage;

    @Column(name = "riasec_i_raw", nullable = false)
    private Integer riasecIRaw;

    @Column(name = "riasec_i_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecIPercentage;

    @Column(name = "riasec_a_raw", nullable = false)
    private Integer riasecARaw;

    @Column(name = "riasec_a_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecAPercentage;

    @Column(name = "riasec_s_raw", nullable = false)
    private Integer riasecSRaw;

    @Column(name = "riasec_s_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecSPercentage;

    @Column(name = "riasec_e_raw", nullable = false)
    private Integer riasecERaw;

    @Column(name = "riasec_e_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecEPercentage;

    @Column(name = "riasec_c_raw", nullable = false)
    private Integer riasecCRaw;

    @Column(name = "riasec_c_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riasecCPercentage;

    // MBTI Scores (raw and percentage)
    @Column(name = "mbti_e_raw", nullable = false)
    private Integer mbtiERaw;

    @Column(name = "mbti_e_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiEPercentage;

    @Column(name = "mbti_i_raw", nullable = false)
    private Integer mbtiIRaw;

    @Column(name = "mbti_i_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiIPercentage;

    @Column(name = "mbti_s_raw", nullable = false)
    private Integer mbtiSRaw;

    @Column(name = "mbti_s_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiSPercentage;

    @Column(name = "mbti_n_raw", nullable = false)
    private Integer mbtiNRaw;

    @Column(name = "mbti_n_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiNPercentage;

    @Column(name = "mbti_t_raw", nullable = false)
    private Integer mbtiTRaw;

    @Column(name = "mbti_t_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiTPercentage;

    @Column(name = "mbti_f_raw", nullable = false)
    private Integer mbtiFRaw;

    @Column(name = "mbti_f_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiFPercentage;

    @Column(name = "mbti_j_raw", nullable = false)
    private Integer mbtiJRaw;

    @Column(name = "mbti_j_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiJPercentage;

    @Column(name = "mbti_p_raw", nullable = false)
    private Integer mbtiPRaw;

    @Column(name = "mbti_p_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mbtiPPercentage;

    // Final Results
    @Column(name = "final_riasec_code", nullable = false)
    private String finalRiasecCode;

    @Column(name = "final_mbti_type", nullable = false)
    private String finalMbtiType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public PersonalityTestScores() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestResultId() {
        return testResultId;
    }

    public void setTestResultId(Long testResultId) {
        this.testResultId = testResultId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    // RIASEC Getters and Setters
    public Integer getRiasecRRaw() {
        return riasecRRaw;
    }

    public void setRiasecRRaw(Integer riasecRRaw) {
        this.riasecRRaw = riasecRRaw;
    }

    public BigDecimal getRiasecRPercentage() {
        return riasecRPercentage;
    }

    public void setRiasecRPercentage(BigDecimal riasecRPercentage) {
        this.riasecRPercentage = riasecRPercentage;
    }

    public Integer getRiasecIRaw() {
        return riasecIRaw;
    }

    public void setRiasecIRaw(Integer riasecIRaw) {
        this.riasecIRaw = riasecIRaw;
    }

    public BigDecimal getRiasecIPercentage() {
        return riasecIPercentage;
    }

    public void setRiasecIPercentage(BigDecimal riasecIPercentage) {
        this.riasecIPercentage = riasecIPercentage;
    }

    public Integer getRiasecARaw() {
        return riasecARaw;
    }

    public void setRiasecARaw(Integer riasecARaw) {
        this.riasecARaw = riasecARaw;
    }

    public BigDecimal getRiasecAPercentage() {
        return riasecAPercentage;
    }

    public void setRiasecAPercentage(BigDecimal riasecAPercentage) {
        this.riasecAPercentage = riasecAPercentage;
    }

    public Integer getRiasecSRaw() {
        return riasecSRaw;
    }

    public void setRiasecSRaw(Integer riasecSRaw) {
        this.riasecSRaw = riasecSRaw;
    }

    public BigDecimal getRiasecSPercentage() {
        return riasecSPercentage;
    }

    public void setRiasecSPercentage(BigDecimal riasecSPercentage) {
        this.riasecSPercentage = riasecSPercentage;
    }

    public Integer getRiasecERaw() {
        return riasecERaw;
    }

    public void setRiasecERaw(Integer riasecERaw) {
        this.riasecERaw = riasecERaw;
    }

    public BigDecimal getRiasecEPercentage() {
        return riasecEPercentage;
    }

    public void setRiasecEPercentage(BigDecimal riasecEPercentage) {
        this.riasecEPercentage = riasecEPercentage;
    }

    public Integer getRiasecCRaw() {
        return riasecCRaw;
    }

    public void setRiasecCRaw(Integer riasecCRaw) {
        this.riasecCRaw = riasecCRaw;
    }

    public BigDecimal getRiasecCPercentage() {
        return riasecCPercentage;
    }

    public void setRiasecCPercentage(BigDecimal riasecCPercentage) {
        this.riasecCPercentage = riasecCPercentage;
    }

    // MBTI Getters and Setters
    public Integer getMbtiERaw() {
        return mbtiERaw;
    }

    public void setMbtiERaw(Integer mbtiERaw) {
        this.mbtiERaw = mbtiERaw;
    }

    public BigDecimal getMbtiEPercentage() {
        return mbtiEPercentage;
    }

    public void setMbtiEPercentage(BigDecimal mbtiEPercentage) {
        this.mbtiEPercentage = mbtiEPercentage;
    }

    public Integer getMbtiIRaw() {
        return mbtiIRaw;
    }

    public void setMbtiIRaw(Integer mbtiIRaw) {
        this.mbtiIRaw = mbtiIRaw;
    }

    public BigDecimal getMbtiIPercentage() {
        return mbtiIPercentage;
    }

    public void setMbtiIPercentage(BigDecimal mbtiIPercentage) {
        this.mbtiIPercentage = mbtiIPercentage;
    }

    public Integer getMbtiSRaw() {
        return mbtiSRaw;
    }

    public void setMbtiSRaw(Integer mbtiSRaw) {
        this.mbtiSRaw = mbtiSRaw;
    }

    public BigDecimal getMbtiSPercentage() {
        return mbtiSPercentage;
    }

    public void setMbtiSPercentage(BigDecimal mbtiSPercentage) {
        this.mbtiSPercentage = mbtiSPercentage;
    }

    public Integer getMbtiNRaw() {
        return mbtiNRaw;
    }

    public void setMbtiNRaw(Integer mbtiNRaw) {
        this.mbtiNRaw = mbtiNRaw;
    }

    public BigDecimal getMbtiNPercentage() {
        return mbtiNPercentage;
    }

    public void setMbtiNPercentage(BigDecimal mbtiNPercentage) {
        this.mbtiNPercentage = mbtiNPercentage;
    }

    public Integer getMbtiTRaw() {
        return mbtiTRaw;
    }

    public void setMbtiTRaw(Integer mbtiTRaw) {
        this.mbtiTRaw = mbtiTRaw;
    }

    public BigDecimal getMbtiTPercentage() {
        return mbtiTPercentage;
    }

    public void setMbtiTPercentage(BigDecimal mbtiTPercentage) {
        this.mbtiTPercentage = mbtiTPercentage;
    }

    public Integer getMbtiFRaw() {
        return mbtiFRaw;
    }

    public void setMbtiFRaw(Integer mbtiFRaw) {
        this.mbtiFRaw = mbtiFRaw;
    }

    public BigDecimal getMbtiFPercentage() {
        return mbtiFPercentage;
    }

    public void setMbtiFPercentage(BigDecimal mbtiFPercentage) {
        this.mbtiFPercentage = mbtiFPercentage;
    }

    public Integer getMbtiJRaw() {
        return mbtiJRaw;
    }

    public void setMbtiJRaw(Integer mbtiJRaw) {
        this.mbtiJRaw = mbtiJRaw;
    }

    public BigDecimal getMbtiJPercentage() {
        return mbtiJPercentage;
    }

    public void setMbtiJPercentage(BigDecimal mbtiJPercentage) {
        this.mbtiJPercentage = mbtiJPercentage;
    }

    public Integer getMbtiPRaw() {
        return mbtiPRaw;
    }

    public void setMbtiPRaw(Integer mbtiPRaw) {
        this.mbtiPRaw = mbtiPRaw;
    }

    public BigDecimal getMbtiPPercentage() {
        return mbtiPPercentage;
    }

    public void setMbtiPPercentage(BigDecimal mbtiPPercentage) {
        this.mbtiPPercentage = mbtiPPercentage;
    }

    // Final Results Getters and Setters
    public String getFinalRiasecCode() {
        return finalRiasecCode;
    }

    public void setFinalRiasecCode(String finalRiasecCode) {
        this.finalRiasecCode = finalRiasecCode;
    }

    public String getFinalMbtiType() {
        return finalMbtiType;
    }

    public void setFinalMbtiType(String finalMbtiType) {
        this.finalMbtiType = finalMbtiType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
