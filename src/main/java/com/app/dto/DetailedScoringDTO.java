package com.app.dto;

import java.util.Map;

public class DetailedScoringDTO {
    
    // RIASEC Scores
    private Map<String, ScoreData> riasecScores;
    
    // MBTI Scores
    private Map<String, ScoreData> mbtiScores;
    
    // Final Results
    private String finalRiasecCode;
    private String finalMbtiType;
    
    // Graph Data
    private Map<String, Object> riasecGraphData;
    private Map<String, Object> mbtiGraphData;
    
    public static class ScoreData {
        private Integer raw;
        private Double percentage;
        private String label;
        private String description;
        
        public ScoreData() {}
        
        public ScoreData(Integer raw, Double percentage, String label, String description) {
            this.raw = raw;
            this.percentage = percentage;
            this.label = label;
            this.description = description;
        }
        
        // Getters and Setters
        public Integer getRaw() {
            return raw;
        }
        
        public void setRaw(Integer raw) {
            this.raw = raw;
        }
        
        public Double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    // Constructors
    public DetailedScoringDTO() {}
    
    // Getters and Setters
    public Map<String, ScoreData> getRiasecScores() {
        return riasecScores;
    }
    
    public void setRiasecScores(Map<String, ScoreData> riasecScores) {
        this.riasecScores = riasecScores;
    }
    
    public Map<String, ScoreData> getMbtiScores() {
        return mbtiScores;
    }
    
    public void setMbtiScores(Map<String, ScoreData> mbtiScores) {
        this.mbtiScores = mbtiScores;
    }
    
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
    
    public Map<String, Object> getRiasecGraphData() {
        return riasecGraphData;
    }
    
    public void setRiasecGraphData(Map<String, Object> riasecGraphData) {
        this.riasecGraphData = riasecGraphData;
    }
    
    public Map<String, Object> getMbtiGraphData() {
        return mbtiGraphData;
    }
    
    public void setMbtiGraphData(Map<String, Object> mbtiGraphData) {
        this.mbtiGraphData = mbtiGraphData;
    }
}
