package com.app.services;

import com.app.dto.EnhancedTestResultDTO;
import com.app.dto.DetailedScoringDTO;
import com.app.models.User;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PdfReportService {
    
    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);
    
    /**
     * Generate PDF report for test results
     */
    public byte[] generatePdfReport(User user, EnhancedTestResultDTO result, DetailedScoringDTO scoringData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Set up fonts
            PdfFont titleFont = PdfFontFactory.createFont();
            PdfFont headerFont = PdfFontFactory.createFont();
            PdfFont bodyFont = PdfFontFactory.createFont();
            
            // Title
            Paragraph title = new Paragraph("Personality Test Results")
                .setFont(titleFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);
            
            // User info
            Paragraph userInfo = new Paragraph()
                .add(new Text("Name: ").setBold())
                .add(user.getUsername())
                .add(new Text("\nEmail: ").setBold())
                .add(user.getEmail())
                .setFont(bodyFont)
                .setFontSize(12)
                .setMarginBottom(20);
            document.add(userInfo);
            
            // Overview section
            addOverviewSection(document, result, scoringData, bodyFont, headerFont);
            
            // Course recommendations
            addCourseRecommendationsSection(document, result, bodyFont, headerFont);
            
            // Career recommendations
            addCareerRecommendationsSection(document, result, bodyFont, headerFont);
            
            // MBTI Details
            if (result.getDetailedMbtiInfo() != null) {
                addMbtiDetailsSection(document, result.getDetailedMbtiInfo(), bodyFont, headerFont);
            }
            
            // Development Plan
            if (result.getCareerDevelopmentPlan() != null || result.getCourseDevelopmentPlan() != null) {
                addDevelopmentPlanSection(document, result, bodyFont, headerFont);
            }
            
            document.close();
            
            log.info("✅ Successfully generated PDF report for user: {}", user.getUsername());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("❌ Failed to generate PDF report for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
    
    private void addOverviewSection(Document document, EnhancedTestResultDTO result, DetailedScoringDTO scoringData, 
                                  PdfFont bodyFont, PdfFont headerFont) throws IOException {
        // MBTI Type
        Paragraph mbtiHeader = new Paragraph("MBTI Type")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(mbtiHeader);
        
        Paragraph mbtiType = new Paragraph(result.getMbtiType())
            .setFont(bodyFont)
            .setFontSize(14)
            .setMarginBottom(15);
        document.add(mbtiType);
        
        // MBTI Percentages
        if (scoringData != null && scoringData.getMbtiScores() != null) {
            Paragraph mbtiPercentages = new Paragraph("MBTI Type Percentages:")
                .setFont(bodyFont)
                .setBold()
                .setMarginBottom(5);
            document.add(mbtiPercentages);
            
            for (Map.Entry<String, DetailedScoringDTO.ScoreData> entry : scoringData.getMbtiScores().entrySet()) {
                String percentage = String.format("%.1f%%", entry.getValue().getPercentage());
                Paragraph mbtiScore = new Paragraph(String.format("%s: %s", entry.getKey(), percentage))
                    .setFont(bodyFont)
                    .setFontSize(12)
                    .setMarginLeft(20);
                document.add(mbtiScore);
            }
        }
        
        // RIASEC Code
        Paragraph riasecHeader = new Paragraph("RIASEC Code")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(riasecHeader);
        
        Paragraph riasecType = new Paragraph(result.getRiasecCode())
            .setFont(bodyFont)
            .setFontSize(14)
            .setMarginBottom(15);
        document.add(riasecType);
        
        // RIASEC Percentages
        if (scoringData != null && scoringData.getRiasecScores() != null) {
            Paragraph riasecPercentages = new Paragraph("RIASEC Code Percentages:")
                .setFont(bodyFont)
                .setBold()
                .setMarginBottom(5);
            document.add(riasecPercentages);
            
            for (Map.Entry<String, DetailedScoringDTO.ScoreData> entry : scoringData.getRiasecScores().entrySet()) {
                String percentage = String.format("%.1f%%", entry.getValue().getPercentage());
                Paragraph riasecScore = new Paragraph(String.format("%s: %s", entry.getKey(), percentage))
                    .setFont(bodyFont)
                    .setFontSize(12)
                    .setMarginLeft(20);
                document.add(riasecScore);
            }
        }
    }
    
    private void addCourseRecommendationsSection(Document document, EnhancedTestResultDTO result, 
                                               PdfFont bodyFont, PdfFont headerFont) throws IOException {
        Paragraph courseHeader = new Paragraph("Course Recommendations")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(courseHeader);
        
        if (result.getCoursePath() != null && !result.getCoursePath().trim().isEmpty()) {
            List<String> courses = parseCourses(result.getCoursePath());
            
            for (int i = 0; i < Math.min(courses.size(), 6); i++) {
                String course = courses.get(i);
                String[] parts = course.split(":", 2);
                String courseName = parts[0].trim();
                String courseDesc = parts.length > 1 ? parts[1].trim() : getFallbackCourseDescription(courseName);
                
                Paragraph courseItem = new Paragraph()
                    .add(new Text(String.format("%d. %s\n", i + 1, courseName)).setBold())
                    .add(courseDesc)
                    .setFont(bodyFont)
                    .setFontSize(12)
                    .setMarginBottom(10);
                document.add(courseItem);
            }
        }
    }
    
    private void addCareerRecommendationsSection(Document document, EnhancedTestResultDTO result, 
                                               PdfFont bodyFont, PdfFont headerFont) throws IOException {
        Paragraph careerHeader = new Paragraph("Career Recommendations")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(careerHeader);
        
        if (result.getCareerSuggestions() != null && !result.getCareerSuggestions().trim().isEmpty()) {
            List<String> careers = parseCareers(result.getCareerSuggestions());
            
            for (int i = 0; i < Math.min(careers.size(), 6); i++) {
                String career = careers.get(i);
                String[] parts = career.split(":", 2);
                String careerName = parts[0].trim();
                String careerDesc = parts.length > 1 ? parts[1].trim() : getFallbackCareerDescription(careerName);
                
                Paragraph careerItem = new Paragraph()
                    .add(new Text(String.format("%d. %s\n", i + 1, careerName)).setBold())
                    .add(careerDesc)
                    .setFont(bodyFont)
                    .setFontSize(12)
                    .setMarginBottom(10);
                document.add(careerItem);
            }
        }
    }
    
    private void addMbtiDetailsSection(Document document, EnhancedTestResultDTO.DetailedMbtiInfoDTO mbtiInfo, 
                                     PdfFont bodyFont, PdfFont headerFont) throws IOException {
        Paragraph mbtiDetailsHeader = new Paragraph("Personality Details")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(mbtiDetailsHeader);
        
        if (mbtiInfo.getLearningStyleSummary() != null && !mbtiInfo.getLearningStyleSummary().trim().isEmpty()) {
            Paragraph learningStyle = new Paragraph()
                .add(new Text("Learning Style:\n").setBold())
                .add(mbtiInfo.getLearningStyleSummary())
                .setFont(bodyFont)
                .setFontSize(12)
                .setMarginBottom(10);
            document.add(learningStyle);
        }
        
        if (mbtiInfo.getStudyTipsSummary() != null && !mbtiInfo.getStudyTipsSummary().trim().isEmpty()) {
            Paragraph studyTips = new Paragraph()
                .add(new Text("Study Tips:\n").setBold())
                .add(mbtiInfo.getStudyTipsSummary())
                .setFont(bodyFont)
                .setFontSize(12)
                .setMarginBottom(10);
            document.add(studyTips);
        }
    }
    
    private void addDevelopmentPlanSection(Document document, EnhancedTestResultDTO result, 
                                         PdfFont bodyFont, PdfFont headerFont) throws IOException {
        Paragraph devPlanHeader = new Paragraph("Development Plan")
            .setFont(headerFont)
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(devPlanHeader);
        
        if (result.getCareerDevelopmentPlan() != null) {
            Paragraph careerPlan = new Paragraph()
                .add(new Text("Career Development:\n").setBold())
                .add("Focus on developing skills that align with your personality type and career interests.")
                .setFont(bodyFont)
                .setFontSize(12)
                .setMarginBottom(10);
            document.add(careerPlan);
        }
        
        if (result.getCourseDevelopmentPlan() != null) {
            Paragraph coursePlan = new Paragraph()
                .add(new Text("Course Development:\n").setBold())
                .add("Consider courses that match your learning style and career goals.")
                .setFont(bodyFont)
                .setFontSize(12)
                .setMarginBottom(10);
            document.add(coursePlan);
        }
    }
    
    private List<String> parseCourses(String coursePath) {
        List<String> courses = new java.util.ArrayList<>();
        
        if (coursePath.contains(";")) {
            courses = Arrays.stream(coursePath.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        } else {
            courses = Arrays.stream(coursePath.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
        
        return courses;
    }
    
    private List<String> parseCareers(String careerSuggestions) {
        List<String> careers = new java.util.ArrayList<>();
        
        if (careerSuggestions.contains(";")) {
            careers = Arrays.stream(careerSuggestions.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        } else {
            careers = Arrays.stream(careerSuggestions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
        
        return careers;
    }
    
    private String getFallbackCourseDescription(String courseName) {
        String name = courseName.toLowerCase();
        if (name.contains("computer science") || name.contains("information technology")) {
            return "A program focused on computing systems, software development, and digital technologies.";
        }
        if (name.contains("psychology")) {
            return "A program studying human behavior, mental processes, and psychological research methods.";
        }
        if (name.contains("business") || name.contains("management")) {
            return "A program preparing students for leadership roles in business and organizational management.";
        }
        if (name.contains("engineering")) {
            return "A program combining mathematics, science, and technology to solve real-world problems.";
        }
        if (name.contains("medicine") || name.contains("medical")) {
            return "A program preparing students for careers in healthcare and medical practice.";
        }
        if (name.contains("education") || name.contains("teaching")) {
            return "A program preparing students to become educators and educational professionals.";
        }
        return "A program aligned with your interests and career aspirations.";
    }
    
    private String getFallbackCareerDescription(String careerName) {
        String name = careerName.toLowerCase();
        if (name.contains("teacher")) return "Educates and mentors learners through structured instruction and assessment.";
        if (name.contains("manager")) return "Leads teams, improves processes, and aligns outcomes with organizational goals.";
        if (name.contains("specialist")) return "Provides focused expertise and delivers quality outcomes in a defined domain.";
        if (name.contains("director")) return "Oversees creative or operational vision, strategy, and execution across initiatives.";
        if (name.contains("writer") || name.contains("editor")) return "Creates and refines content to communicate ideas clearly for target audiences.";
        if (name.contains("designer") || name.contains("artist")) return "Transforms concepts into compelling visuals and experiences.";
        return "A role aligned with your strengths, offering meaningful impact and growth opportunities.";
    }
}
