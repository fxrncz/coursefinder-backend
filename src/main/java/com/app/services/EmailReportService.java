package com.app.services;

import com.app.models.TestResult;
import com.app.models.User;
import com.app.dto.EnhancedTestResultDTO;
import com.app.dto.DetailedScoringDTO;
import com.app.dto.CareerDevelopmentPlanDTO;
import com.app.dto.CourseDevelopmentPlanDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmailReportService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailReportService.class);
    
    @Autowired
    private EmailService emailService;
    
    @Value("${backend.url:https://coursefinder-backend-production.up.railway.app}")
    private String backendUrl;
    
    /**
     * Send automated test results email with ACTUAL data from results page
     * Triggered asynchronously after test submission
     * 
     * @param testResult The test result entity
     * @param user The user entity
     * @param enhancedResult The enhanced result DTO (contains all display data)
     * @param scoringData The detailed scoring data (contains percentages)
     */
    @Async
    public void sendTestResultsEmail(
        TestResult testResult, 
        User user,
        EnhancedTestResultDTO enhancedResult,
        DetailedScoringDTO scoringData
    ) {
        try {
            log.info("📧 Starting email report generation for user: {} ({})", 
                user.getUsername(), user.getEmail());
            
            // Validate user has email
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                log.warn("⚠️ User {} has no email address, skipping email", user.getId());
                return;
            }
            
            // Validate we have the required data
            if (enhancedResult == null) {
                log.error("❌ Enhanced result is null for user {}", user.getId());
                return;
            }
            
            // Build HTML email content using ACTUAL data
            String htmlContent = buildHtmlEmailContent(user, enhancedResult, scoringData, testResult.getSessionId());
            
            // Send email
            String subject = String.format(
                "🎯 Your Personality Test Results - %s (%s)", 
                enhancedResult.getMbtiType(),
                enhancedResult.getRiasecCode()
            );
            
            emailService.sendCustomEmail(user.getEmail(), subject, htmlContent);
            
            log.info("✅ Successfully sent test results email to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("❌ Failed to send test results email to user {}: {}", 
                user.getId(), e.getMessage(), e);
            // Don't rethrow - we don't want to break the test submission
        }
    }
    
    /**
     * Build complete HTML email content using ACTUAL data from enhanced result
     */
    private String buildHtmlEmailContent(
        User user, 
        EnhancedTestResultDTO result,
        DetailedScoringDTO scoringData,
        UUID sessionId
    ) {
        StringBuilder html = new StringBuilder();
        
        // Email structure
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Your Personality Test Results</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f5f5f5;">
                    <tr>
                        <td align="center" style="padding: 20px 10px;">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" 
                                   style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
            """);
        
        // Header
        html.append(buildEmailHeader(user));
        
        // Section 1: Overview (MBTI + RIASEC)
        html.append(buildOverviewSection(result));
        
        // Section 2: MBTI Percentages
        if (scoringData != null && scoringData.getMbtiScores() != null) {
            html.append(buildMBTIPercentagesSection(scoringData.getMbtiScores()));
        }
        
        // Section 3: RIASEC Percentages
        if (scoringData != null && scoringData.getRiasecScores() != null) {
            html.append(buildRIASECPercentagesSection(scoringData.getRiasecScores()));
        }
        
        // Section 4: Course Recommendations (6 courses)
        html.append(buildCourseSuggestionsSection(result.getCoursePath()));
        
        // Section 5: Career Recommendations (6 careers)
        html.append(buildCareerSuggestionsSection(result.getCareerSuggestions()));
        
        // Section 6: Development Plan (ACTUAL from database)
        if (result.getCareerDevelopmentPlan() != null || result.getCourseDevelopmentPlan() != null) {
            html.append(buildDevelopmentPlanSection(
                result.getCareerDevelopmentPlan(), 
                result.getCourseDevelopmentPlan()
            ));
        }
        
        // Section 7: Personality Details (MBTI + RIASEC details)
        if (result.getDetailedMbtiInfo() != null) {
            html.append(buildPersonalityDetailsSection(result.getDetailedMbtiInfo()));
        }
        
        // Link to online version
        html.append(buildViewOnlineSection(sessionId));
        
        // Footer
        html.append(buildEmailFooter());
        
        // Close tags
        html.append("""
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """);
        
        return html.toString();
    }
    
    // ========== EMAIL SECTION BUILDERS ==========
    
    private String buildEmailHeader(User user) {
        return String.format("""
            <tr>
                <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center;">
                    <h1 style="color: #ffffff; margin: 0 0 10px 0; font-size: 28px; font-weight: 700;">
                        🎓 CourseFinder - Your Test Results
                    </h1>
                    <p style="color: #ffffff; margin: 0; font-size: 16px; opacity: 0.95;">
                        Personalized Career & Course Recommendations
                    </p>
                </td>
            </tr>
            <tr>
                <td style="padding: 30px; border-bottom: 3px solid #f0f0f0;">
                    <h2 style="color: #333; margin: 0 0 10px 0; font-size: 24px;">
                        Hi %s! 👋
                    </h2>
                    <p style="color: #666; margin: 0; font-size: 16px; line-height: 1.6;">
                        Here are your complete personality test results. This email contains everything 
                        shown on your results page for easy reference.
                    </p>
                </td>
            </tr>
            """, 
            user.getUsername()
        );
    }
    
    private String buildOverviewSection(EnhancedTestResultDTO result) {
        String mbtiDescription = result.getDetailedMbtiInfo() != null && 
            result.getDetailedMbtiInfo().getLearningStyleSummary() != null ?
            result.getDetailedMbtiInfo().getLearningStyleSummary() :
            "Your personality type";
        
        return String.format("""
            <tr>
                <td style="padding: 30px; background-color: #f8f9fa;">
                    <h2 style="color: #667eea; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        📊 Overview
                    </h2>
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td width="50%%" style="padding: 10px;">
                                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 20px; border-radius: 8px; text-align: center;">
                                    <p style="margin: 0; color: #ffffff; font-size: 13px; opacity: 0.9;">MBTI Personality Type</p>
                                    <p style="margin: 8px 0 0 0; color: #ffffff; font-size: 32px; font-weight: 700;">%s</p>
                                </div>
                            </td>
                            <td width="50%%" style="padding: 10px;">
                                <div style="background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); padding: 20px; border-radius: 8px; text-align: center;">
                                    <p style="margin: 0; color: #ffffff; font-size: 13px; opacity: 0.9;">RIASEC Interest Code</p>
                                    <p style="margin: 8px 0 0 0; color: #ffffff; font-size: 32px; font-weight: 700;">%s</p>
                                </div>
                            </td>
                        </tr>
                    </table>
                    <div style="margin-top: 20px; padding: 15px; background-color: #ffffff; border-radius: 8px; border-left: 4px solid #667eea;">
                        <p style="color: #555; margin: 0; font-size: 14px; line-height: 1.7;">
                            %s
                        </p>
                    </div>
                </td>
            </tr>
            """,
            result.getMbtiType(),
            result.getRiasecCode(),
            mbtiDescription
        );
    }
    
    private String buildMBTIPercentagesSection(Map<String, DetailedScoringDTO.ScoreData> mbtiScores) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px;">
                    <h2 style="color: #667eea; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        📈 MBTI Type Breakdown
                    </h2>
            """);
        
        // E vs I
        html.append(buildDimensionBar("E", "I", mbtiScores));
        
        // S vs N
        html.append(buildDimensionBar("S", "N", mbtiScores));
        
        // T vs F
        html.append(buildDimensionBar("T", "F", mbtiScores));
        
        // J vs P
        html.append(buildDimensionBar("J", "P", mbtiScores));
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String buildDimensionBar(String left, String right, Map<String, DetailedScoringDTO.ScoreData> scores) {
        DetailedScoringDTO.ScoreData leftScore = scores.get(left);
        DetailedScoringDTO.ScoreData rightScore = scores.get(right);
        
        if (leftScore == null || rightScore == null) {
            return "";
        }
        
        double leftPercent = leftScore.getPercentage();
        double rightPercent = rightScore.getPercentage();
        
        return String.format("""
            <div style="margin-bottom: 20px;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                    <span style="font-size: 13px; color: #666; font-weight: 600;">%s: %s</span>
                    <span style="font-size: 13px; color: #666; font-weight: 600;">%s: %s</span>
                </div>
                <div style="display: flex; height: 30px; background-color: #e0e0e0; border-radius: 15px; overflow: hidden;">
                    <div style="width: %.1f%%; background: linear-gradient(90deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: flex-start; padding-left: 10px;">
                        <span style="color: #ffffff; font-weight: 700; font-size: 12px;">%.0f%%</span>
                    </div>
                    <div style="width: %.1f%%; background: linear-gradient(90deg, #f093fb, #f5576c); display: flex; align-items: center; justify-content: flex-end; padding-right: 10px;">
                        <span style="color: #ffffff; font-weight: 700; font-size: 12px;">%.0f%%</span>
                    </div>
                </div>
                <div style="display: flex; justify-content: space-between; margin-top: 5px;">
                    <span style="font-size: 11px; color: #888;">%s</span>
                    <span style="font-size: 11px; color: #888;">%s</span>
                </div>
            </div>
            """,
            leftScore.getLabel(), leftScore.getLabel(),
            rightScore.getLabel(), rightScore.getLabel(),
            leftPercent, leftPercent,
            rightPercent, rightPercent,
            leftScore.getDescription(),
            rightScore.getDescription()
        );
    }
    
    private String buildRIASECPercentagesSection(Map<String, DetailedScoringDTO.ScoreData> riasecScores) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px; background-color: #f9f9f9;">
                    <h2 style="color: #f5576c; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        🎯 RIASEC Interest Breakdown
                    </h2>
            """);
        
        // Sort by percentage (highest first)
        List<Map.Entry<String, DetailedScoringDTO.ScoreData>> sortedScores = riasecScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().getPercentage(), a.getValue().getPercentage()))
            .collect(Collectors.toList());
        
        for (Map.Entry<String, DetailedScoringDTO.ScoreData> entry : sortedScores) {
            DetailedScoringDTO.ScoreData score = entry.getValue();
            String color = getRIASECColor(entry.getKey());
            
            html.append(String.format("""
                <div style="margin-bottom: 15px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                        <span style="font-size: 14px; color: #333; font-weight: 600;">%s - %s</span>
                        <span style="font-size: 14px; color: #666; font-weight: 600;">%.0f%%</span>
                    </div>
                    <div style="background-color: #e0e0e0; border-radius: 10px; overflow: hidden; height: 24px;">
                        <div style="width: %.1f%%; background-color: %s; height: 100%%; border-radius: 10px; transition: width 0.3s ease;"></div>
                    </div>
                    <p style="font-size: 11px; color: #888; margin: 3px 0 0 0;">%s</p>
                </div>
                """,
                entry.getKey(), score.getLabel(),
                score.getPercentage(),
                score.getPercentage(), color,
                score.getDescription()
            ));
        }
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String getRIASECColor(String code) {
        switch (code) {
            case "R": return "#ff6b6b";
            case "I": return "#4ecdc4";
            case "A": return "#ffe66d";
            case "S": return "#95e1d3";
            case "E": return "#f38181";
            case "C": return "#aa96da";
            default: return "#667eea";
        }
    }
    
    private String buildCourseSuggestionsSection(String coursePath) {
        if (coursePath == null || coursePath.trim().isEmpty()) {
            return "";
        }
        
        // Parse courses using the same logic as frontend
        // Frontend expects format: "Course 1: Description; Course 2: Description; ..." or comma-separated
        List<String> courses = new ArrayList<>();
        
        // Check if it uses semicolon separator (preferred format)
        if (coursePath.contains(";")) {
            courses = Arrays.stream(coursePath.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(6)
                .collect(Collectors.toList());
        } else {
            // Fallback to comma separator
            courses = Arrays.stream(coursePath.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(6)
                .collect(Collectors.toList());
        }
        
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px;">
                    <h2 style="color: #007bff; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        📚 Your Top 6 Course Recommendations
                    </h2>
            """);
        
        for (int i = 0; i < courses.size(); i++) {
            String course = courses.get(i);
            String[] parts = course.split(":", 2);
            String courseName = parts[0].trim();
            String courseDesc = parts.length > 1 ? parts[1].trim() : getFallbackCourseDescription(courseName);
            
            html.append(String.format("""
                <div style="background-color: #f8f9fa; padding: 18px; margin-bottom: 12px; border-radius: 8px; border-left: 4px solid #007bff;">
                    <h4 style="color: #333; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">
                        %d. %s
                    </h4>
                    <p style="color: #666; margin: 0; font-size: 13px; line-height: 1.6;">
                        %s
                    </p>
                </div>
                """,
                i + 1,
                courseName,
                courseDesc
            ));
        }
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String buildCareerSuggestionsSection(String careerSuggestions) {
        if (careerSuggestions == null || careerSuggestions.trim().isEmpty()) {
            return "";
        }
        
        // Parse careers using the same logic as frontend
        // Frontend expects format: "Career 1: Description; Career 2: Description; ..." or comma-separated
        List<String> careers = new ArrayList<>();
        
        // Check if it uses semicolon separator (preferred format)
        if (careerSuggestions.contains(";")) {
            careers = Arrays.stream(careerSuggestions.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(6)
                .collect(Collectors.toList());
        } else {
            // Fallback to comma separator
            careers = Arrays.stream(careerSuggestions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(6)
                .collect(Collectors.toList());
        }
        
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px; background-color: #f9f9f9;">
                    <h2 style="color: #28a745; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        💼 Your Top 6 Career Recommendations
                    </h2>
            """);
        
        for (int i = 0; i < careers.size(); i++) {
            String career = careers.get(i);
            String[] parts = career.split(":", 2);
            String careerName = parts[0].trim();
            String careerDesc = parts.length > 1 ? parts[1].trim() : getFallbackCareerDescription(careerName);
            
            html.append(String.format("""
                <div style="background-color: #ffffff; padding: 18px; margin-bottom: 12px; border-radius: 8px; border-left: 4px solid #28a745;">
                    <h4 style="color: #333; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">
                        %d. %s
                    </h4>
                    <p style="color: #666; margin: 0; font-size: 13px; line-height: 1.6;">
                        %s
                    </p>
                </div>
                """,
                i + 1,
                careerName,
                careerDesc
            ));
        }
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String buildDevelopmentPlanSection(
        CareerDevelopmentPlanDTO careerPlan,
        CourseDevelopmentPlanDTO coursePlan
    ) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px;">
                    <h2 style="color: #ffc107; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        🚀 Development Plan
                    </h2>
            """);
        
        // Career Development Plan
        if (careerPlan != null && careerPlan.getCareerDetails() != null && !careerPlan.getCareerDetails().isEmpty()) {
            html.append("""
                <h3 style="color: #333; margin: 0 0 15px 0; font-size: 18px; font-weight: 600;">
                    Career Development
                </h3>
                """);
            
            for (CareerDevelopmentPlanDTO.CareerDetails career : careerPlan.getCareerDetails()) {
                html.append(String.format("""
                    <div style="background-color: #f8f9fa; padding: 20px; margin-bottom: 15px; border-radius: 8px;">
                        <h4 style="color: #28a745; margin: 0 0 10px 0; font-size: 16px; font-weight: 600;">
                            %s
                        </h4>
                    """,
                    career.getCareerName()
                ));
                
                if (career.getIntroduction() != null && !career.getIntroduction().isEmpty()) {
                    html.append(String.format("""
                        <p style="color: #666; margin: 0 0 12px 0; font-size: 13px; line-height: 1.6;">
                            %s
                        </p>
                        """,
                        career.getIntroduction()
                    ));
                }
                
                if (career.getKeySkills() != null && !career.getKeySkills().isEmpty()) {
                    html.append(String.format("""
                        <div style="margin-bottom: 10px;">
                            <p style="margin: 0 0 5px 0; color: #333; font-weight: 600; font-size: 13px;">Key Skills:</p>
                            <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                        </div>
                        """,
                        career.getKeySkills()
                    ));
                }
                
                if (career.getCareerPath() != null && !career.getCareerPath().isEmpty()) {
                    html.append(String.format("""
                        <div style="margin-bottom: 10px;">
                            <p style="margin: 0 0 5px 0; color: #333; font-weight: 600; font-size: 13px;">Career Pathway:</p>
                            <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                        </div>
                        """,
                        career.getCareerPath()
                    ));
                }
                
                html.append("</div>");
            }
        }
        
        // Course Development Plan
        if (coursePlan != null && coursePlan.getCourseDetails() != null && !coursePlan.getCourseDetails().isEmpty()) {
            html.append("""
                <h3 style="color: #333; margin: 20px 0 15px 0; font-size: 18px; font-weight: 600;">
                    Course Development
                </h3>
                """);
            
            for (CourseDevelopmentPlanDTO.CourseDetails course : coursePlan.getCourseDetails()) {
                html.append(String.format("""
                    <div style="background-color: #f8f9fa; padding: 20px; margin-bottom: 15px; border-radius: 8px;">
                        <h4 style="color: #007bff; margin: 0 0 10px 0; font-size: 16px; font-weight: 600;">
                            %s
                        </h4>
                    """,
                    course.getCourseName()
                ));
                
                if (course.getCourseOverview() != null && !course.getCourseOverview().isEmpty()) {
                    html.append(String.format("""
                        <p style="color: #666; margin: 0 0 12px 0; font-size: 13px; line-height: 1.6;">
                            %s
                        </p>
                        """,
                        course.getCourseOverview()
                    ));
                }
                
                if (course.getCoreCompetencies() != null && !course.getCoreCompetencies().isEmpty()) {
                    html.append(String.format("""
                        <div style="margin-bottom: 10px;">
                            <p style="margin: 0 0 5px 0; color: #333; font-weight: 600; font-size: 13px;">Core Competencies:</p>
                            <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                        </div>
                        """,
                        course.getCoreCompetencies()
                    ));
                }
                
                if (course.getGrowth() != null && !course.getGrowth().isEmpty()) {
                    html.append(String.format("""
                        <div style="margin-bottom: 10px;">
                            <p style="margin: 0 0 5px 0; color: #333; font-weight: 600; font-size: 13px;">Growth Opportunities:</p>
                            <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                        </div>
                        """,
                        course.getGrowth()
                    ));
                }
                
                html.append("</div>");
            }
        }
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String buildPersonalityDetailsSection(EnhancedTestResultDTO.DetailedMbtiInfoDTO mbtiInfo) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <tr>
                <td style="padding: 30px; background-color: #f0f8ff;">
                    <h2 style="color: #667eea; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;">
                        💡 Personality Details
                    </h2>
            """);
        
        // Learning Style
        if (mbtiInfo.getLearningStyleDetails() != null) {
            html.append(String.format("""
                <div style="background-color: #ffffff; padding: 20px; margin-bottom: 15px; border-radius: 8px;">
                    <h4 style="color: #333; margin: 0 0 10px 0; font-size: 16px; font-weight: 600;">
                        📖 How You Learn Best
                    </h4>
                    <p style="color: #666; margin: 0; font-size: 13px; line-height: 1.7;">
                        %s
                    </p>
                </div>
                """,
                mbtiInfo.getLearningStyleDetails()
            ));
        }
        
        // Study Tips
        if (mbtiInfo.getStudyTipsDetails() != null) {
            html.append(String.format("""
                <div style="background-color: #ffffff; padding: 20px; margin-bottom: 15px; border-radius: 8px;">
                    <h4 style="color: #333; margin: 0 0 10px 0; font-size: 16px; font-weight: 600;">
                        ✅ Study Tips
                    </h4>
                    <p style="color: #666; margin: 0 0 10px 0; font-size: 13px; line-height: 1.7;">
                        %s
                    </p>
                """,
                mbtiInfo.getStudyTipsDetails()
            ));
            
            // Do's
            if (mbtiInfo.getStudyTipsDos() != null) {
                html.append("""
                    <div style="margin-top: 12px;">
                        <p style="margin: 0 0 5px 0; color: #28a745; font-weight: 600; font-size: 13px;">✓ Do's:</p>
                    """);
                
                String[] dos = mbtiInfo.getStudyTipsDos().split("\n");
                for (String tip : dos) {
                    String cleanTip = tip.replace("•", "").trim();
                    if (!cleanTip.isEmpty()) {
                        html.append(String.format("""
                            <p style="margin: 0 0 4px 0; padding-left: 15px; color: #666; font-size: 12px;">• %s</p>
                            """,
                            cleanTip
                        ));
                    }
                }
                
                html.append("</div>");
            }
            
            // Don'ts
            if (mbtiInfo.getStudyTipsDonts() != null) {
                html.append("""
                    <div style="margin-top: 12px;">
                        <p style="margin: 0 0 5px 0; color: #dc3545; font-weight: 600; font-size: 13px;">✗ Don'ts:</p>
                    """);
                
                String[] donts = mbtiInfo.getStudyTipsDonts().split("\n");
                for (String tip : donts) {
                    String cleanTip = tip.replace("•", "").trim();
                    if (!cleanTip.isEmpty()) {
                        html.append(String.format("""
                            <p style="margin: 0 0 4px 0; padding-left: 15px; color: #666; font-size: 12px;">• %s</p>
                            """,
                            cleanTip
                        ));
                    }
                }
                
                html.append("</div>");
            }
            
            html.append("</div>");
        }
        
        // Growth Analysis
        if (mbtiInfo.getGrowthStrengths() != null || mbtiInfo.getGrowthWeaknesses() != null) {
            html.append("""
                <div style="background-color: #ffffff; padding: 20px; border-radius: 8px;">
                    <h4 style="color: #333; margin: 0 0 15px 0; font-size: 16px; font-weight: 600;">
                        🌱 Growth & Development
                    </h4>
                """);
            
            if (mbtiInfo.getGrowthStrengths() != null) {
                html.append(String.format("""
                    <div style="margin-bottom: 12px;">
                        <p style="margin: 0 0 5px 0; color: #28a745; font-weight: 600; font-size: 13px;">💪 Strengths:</p>
                        <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                    </div>
                    """,
                    mbtiInfo.getGrowthStrengths()
                ));
            }
            
            if (mbtiInfo.getGrowthWeaknesses() != null) {
                html.append(String.format("""
                    <div style="margin-bottom: 12px;">
                        <p style="margin: 0 0 5px 0; color: #ffc107; font-weight: 600; font-size: 13px;">⚠️ Areas for Growth:</p>
                        <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                    </div>
                    """,
                    mbtiInfo.getGrowthWeaknesses()
                ));
            }
            
            if (mbtiInfo.getGrowthOpportunities() != null) {
                html.append(String.format("""
                    <div>
                        <p style="margin: 0 0 5px 0; color: #007bff; font-weight: 600; font-size: 13px;">🎯 Opportunities:</p>
                        <p style="margin: 0; color: #666; font-size: 13px; line-height: 1.6;">%s</p>
                    </div>
                    """,
                    mbtiInfo.getGrowthOpportunities()
                ));
            }
            
            html.append("</div>");
        }
        
        html.append("</td></tr>");
        
        return html.toString();
    }
    
    private String buildViewOnlineSection(UUID sessionId) {
        String pdfDownloadUrl = String.format(
            "%s/api/pdf-report/download/%s",
            backendUrl,
            sessionId.toString()
        );
        
        return String.format("""
            <tr>
                <td style="padding: 30px; background-color: #f8f9fa; text-align: center;">
                    <h3 style="color: #333; margin: 0 0 15px 0; font-size: 20px; font-weight: 600;">
                        📄 Download Your Results
                    </h3>
                    
                    <p style="color: #666; margin: 0 0 20px 0; font-size: 14px; line-height: 1.6;">
                        Get a comprehensive PDF report of your personality test results. 
                        Perfect for keeping a record or sharing with career counselors.
                    </p>
                    
                    <div style="margin: 25px 0;">
                        <a href="%s" 
                           style="display: inline-block; padding: 16px 40px; background-color: #dc3545; 
                                  color: #ffffff !important; text-decoration: none; border-radius: 6px; 
                                  font-weight: 600; font-size: 15px; box-shadow: 0 4px 6px rgba(220,53,69,0.3);">
                            📥 Download PDF Result
                        </a>
                    </div>
                    
                    <p style="color: #888; font-size: 12px; margin: 15px 0 0 0;">
                        Includes all your results, recommendations, and development plan
                    </p>
                </td>
            </tr>
            """,
            pdfDownloadUrl
        );
    }
    
    private String buildEmailFooter() {
        return """
            <tr>
                <td style="padding: 30px; background-color: #2c3e50; text-align: center;">
                    <p style="color: #ecf0f1; margin: 0 0 8px 0; font-size: 18px; font-weight: 600;">
                        CourseFinder
                    </p>
                    <p style="color: #95a5a6; margin: 0 0 15px 0; font-size: 13px;">
                        Guiding Filipino Students to Their Perfect Career Path
                    </p>
                    <p style="color: #7f8c8d; margin: 0; font-size: 11px; line-height: 1.6;">
                        © 2024 CourseFinder. All rights reserved.<br>
                        This email was sent because you completed a personality test on our platform.
                    </p>
                </td>
            </tr>
            """;
    }
    
    /**
     * Get fallback career description when none is provided
     * Uses the same logic as the frontend
     */
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
    
    /**
     * Get fallback course description when none is provided
     */
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
}
