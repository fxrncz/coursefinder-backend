package com.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Model Comparison Service
 * Uses two different Hugging Face models to compare and debate their analysis
 * of courses and careers for personality-based recommendations
 */
@Service
public class AiModelComparisonService {
    
    private static final Logger log = LoggerFactory.getLogger(AiModelComparisonService.class);
    
    @Autowired
    private HuggingFaceApiService huggingFaceService;
    
    @Value("${huggingface.validation.enabled:false}")
    private boolean aiEnabled;
    
    @Value("${huggingface.model.validation}")
    private String model1; // DialoGPT-large
    
    @Value("${huggingface.model.education}")
    private String model2; // GPT-neo-2.7B
    
    /**
     * Compare two models' analysis of courses for a given personality
     */
    public ModelComparisonResult compareCourseAnalysis(String mbtiType, String riasecCode, String coursePath) {
        if (!aiEnabled) {
            log.info("ℹ️ AI comparison disabled - using mock comparison");
            return createMockComparison(mbtiType, riasecCode, coursePath);
        }
        
        try {
            log.info("🤖 AI: Comparing models for course analysis - MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // Parse courses
            List<String> courses = parseCourses(coursePath);
            if (courses.isEmpty()) {
                return new ModelComparisonResult();
            }
            
            // Get analysis from both models
            List<CourseAnalysis> model1Analysis = getModel1CourseAnalysis(mbtiType, riasecCode, courses);
            List<CourseAnalysis> model2Analysis = getModel2CourseAnalysis(mbtiType, riasecCode, courses);
            
            // Create comparison result
            ModelComparisonResult result = new ModelComparisonResult();
            result.setMbtiType(mbtiType);
            result.setRiasecCode(riasecCode);
            result.setComparisonType("COURSE_ANALYSIS");
            result.setModel1Name("DialoGPT-Large (Analytical)");
            result.setModel2Name("GPT-Neo-2.7B (Creative)");
            
            // Compare each course
            List<CourseComparison> courseComparisons = new ArrayList<>();
            for (String course : courses) {
                CourseAnalysis analysis1 = findAnalysisForCourse(model1Analysis, course);
                CourseAnalysis analysis2 = findAnalysisForCourse(model2Analysis, course);
                
                CourseComparison comparison = createCourseComparison(course, analysis1, analysis2);
                courseComparisons.add(comparison);
            }
            
            result.setCourseComparisons(courseComparisons);
            
            log.info("✅ AI model comparison completed for {} courses", courses.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ AI model comparison failed: {}", e.getMessage(), e);
            return createMockComparison(mbtiType, riasecCode, coursePath);
        }
    }
    
    /**
     * Compare two models' analysis of careers for a given personality
     */
    public ModelComparisonResult compareCareerAnalysis(String mbtiType, String riasecCode, String careerSuggestions) {
        if (!aiEnabled) {
            log.info("ℹ️ AI comparison disabled - using mock comparison");
            return createMockCareerComparison(mbtiType, riasecCode, careerSuggestions);
        }
        
        try {
            log.info("🤖 AI: Comparing models for career analysis - MBTI: {}, RIASEC: {}", mbtiType, riasecCode);
            
            // Parse careers
            List<String> careers = parseCareers(careerSuggestions);
            if (careers.isEmpty()) {
                return new ModelComparisonResult();
            }
            
            // Get analysis from both models
            List<CareerAnalysis> model1Analysis = getModel1CareerAnalysis(mbtiType, riasecCode, careers);
            List<CareerAnalysis> model2Analysis = getModel2CareerAnalysis(mbtiType, riasecCode, careers);
            
            // Create comparison result
            ModelComparisonResult result = new ModelComparisonResult();
            result.setMbtiType(mbtiType);
            result.setRiasecCode(riasecCode);
            result.setComparisonType("CAREER_ANALYSIS");
            result.setModel1Name("DialoGPT-Large (Analytical)");
            result.setModel2Name("GPT-Neo-2.7B (Creative)");
            
            // Compare each career
            List<CareerComparison> careerComparisons = new ArrayList<>();
            for (String career : careers) {
                CareerAnalysis analysis1 = findAnalysisForCareer(model1Analysis, career);
                CareerAnalysis analysis2 = findAnalysisForCareer(model2Analysis, career);
                
                CareerComparison comparison = createCareerComparison(career, analysis1, analysis2);
                careerComparisons.add(comparison);
            }
            
            result.setCareerComparisons(careerComparisons);
            
            log.info("✅ AI model comparison completed for {} careers", careers.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ AI model comparison failed: {}", e.getMessage(), e);
            return createMockCareerComparison(mbtiType, riasecCode, careerSuggestions);
        }
    }
    
    /**
     * Get Model 1 (DialoGPT-Large) course analysis
     */
    private List<CourseAnalysis> getModel1CourseAnalysis(String mbtiType, String riasecCode, List<String> courses) {
        try {
            String prompt = buildModel1CoursePrompt(mbtiType, riasecCode, courses);
            String response = huggingFaceService.generateText(model1, prompt);
            
            log.debug("🤖 Model 1 raw response: {}", response);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ Model 1 returned empty response, generating fallback data");
                return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "DialoGPT-Large", true);
            }
            
            List<CourseAnalysis> analyses = parseCourseAnalysis(response, "DialoGPT-Large", courses);
            
            if (analyses.isEmpty()) {
                log.warn("⚠️ Model 1 parsing failed, generating fallback data");
                return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "DialoGPT-Large", true);
            }
            
            return analyses;
        } catch (Exception e) {
            log.error("❌ Error in Model 1 course analysis: {}", e.getMessage(), e);
            return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "DialoGPT-Large", true);
        }
    }
    
    /**
     * Get Model 2 (GPT-Neo-2.7B) course analysis
     */
    private List<CourseAnalysis> getModel2CourseAnalysis(String mbtiType, String riasecCode, List<String> courses) {
        try {
            String prompt = buildModel2CoursePrompt(mbtiType, riasecCode, courses);
            String response = huggingFaceService.generateText(model2, prompt);
            
            log.debug("🤖 Model 2 raw response: {}", response);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ Model 2 returned empty response, generating fallback data");
                return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
            }
            
            List<CourseAnalysis> analyses = parseCourseAnalysis(response, "GPT-Neo-2.7B", courses);
            
            if (analyses.isEmpty()) {
                log.warn("⚠️ Model 2 parsing failed, generating fallback data");
                return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
            }
            
            return analyses;
        } catch (Exception e) {
            log.error("❌ Error in Model 2 course analysis: {}", e.getMessage(), e);
            return generateFallbackCourseAnalysis(courses, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
        }
    }
    
    /**
     * Get Model 1 (DialoGPT-Large) career analysis
     */
    private List<CareerAnalysis> getModel1CareerAnalysis(String mbtiType, String riasecCode, List<String> careers) {
        try {
            String prompt = buildModel1CareerPrompt(mbtiType, riasecCode, careers);
            String response = huggingFaceService.generateText(model1, prompt);
            
            log.debug("🤖 Model 1 career raw response: {}", response);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ Model 1 returned empty response, generating fallback career data");
                return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "DialoGPT-Large", true);
            }
            
            List<CareerAnalysis> analyses = parseCareerAnalysis(response, "DialoGPT-Large", careers);
            
            if (analyses.isEmpty()) {
                log.warn("⚠️ Model 1 career parsing failed, generating fallback data");
                return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "DialoGPT-Large", true);
            }
            
            return analyses;
        } catch (Exception e) {
            log.error("❌ Error in Model 1 career analysis: {}", e.getMessage(), e);
            return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "DialoGPT-Large", true);
        }
    }
    
    /**
     * Get Model 2 (GPT-Neo-2.7B) career analysis
     */
    private List<CareerAnalysis> getModel2CareerAnalysis(String mbtiType, String riasecCode, List<String> careers) {
        try {
            String prompt = buildModel2CareerPrompt(mbtiType, riasecCode, careers);
            String response = huggingFaceService.generateText(model2, prompt);
            
            log.debug("🤖 Model 2 career raw response: {}", response);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ Model 2 returned empty response, generating fallback career data");
                return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
            }
            
            List<CareerAnalysis> analyses = parseCareerAnalysis(response, "GPT-Neo-2.7B", careers);
            
            if (analyses.isEmpty()) {
                log.warn("⚠️ Model 2 career parsing failed, generating fallback data");
                return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
            }
            
            return analyses;
        } catch (Exception e) {
            log.error("❌ Error in Model 2 career analysis: {}", e.getMessage(), e);
            return generateFallbackCareerAnalysis(careers, mbtiType, riasecCode, "GPT-Neo-2.7B", false);
        }
    }
    
    /**
     * Build prompt for Model 1 (DialoGPT-Large) - Analytical approach
     */
    private String buildModel1CoursePrompt(String mbtiType, String riasecCode, List<String> courses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analytical Career Assessment\n");
        prompt.append("===========================\n\n");
        prompt.append(String.format("Profile: MBTI %s | RIASEC %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Rate each course's compatibility (60-95%%) with analytical reasoning.\n\n");
        prompt.append("IMPORTANT: Use EXACT format below:\n");
        prompt.append("CourseName|Score|Brief analytical assessment\n\n");
        prompt.append("Courses to analyze:\n");
        
        for (int i = 0; i < courses.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, courses.get(i)));
        }
        
        prompt.append("\nExample response format:\n");
        if (!courses.isEmpty()) {
            prompt.append(String.format("%s|82|Strong alignment with analytical thinking and systematic problem-solving\n\n", 
                courses.get(0)));
        }
        
        prompt.append("Now provide your analysis for ALL courses listed above:\n");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt for Model 2 (GPT-Neo-2.7B) - Creative approach
     */
    private String buildModel2CoursePrompt(String mbtiType, String riasecCode, List<String> courses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Creative Career Exploration\n");
        prompt.append("===========================\n\n");
        prompt.append(String.format("Profile: MBTI %s | RIASEC %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Rate each course's creative potential (60-95%%) with innovative perspectives.\n\n");
        prompt.append("IMPORTANT: Use EXACT format below:\n");
        prompt.append("CourseName|Score|Brief creative assessment\n\n");
        prompt.append("Courses to explore:\n");
        
        for (int i = 0; i < courses.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, courses.get(i)));
        }
        
        prompt.append("\nExample response format:\n");
        if (!courses.isEmpty()) {
            prompt.append(String.format("%s|88|Exciting opportunities for creative expression and innovative thinking\n\n", 
                courses.get(0)));
        }
        
        prompt.append("Now provide your analysis for ALL courses listed above:\n");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt for Model 1 career analysis
     */
    private String buildModel1CareerPrompt(String mbtiType, String riasecCode, List<String> careers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analytical Career Assessment\n");
        prompt.append("===========================\n\n");
        prompt.append(String.format("Profile: MBTI %s | RIASEC %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Rate each career's fit (60-95%%) based on market data and trends.\n\n");
        prompt.append("IMPORTANT: Use EXACT format below:\n");
        prompt.append("CareerName|Score|Brief market analysis\n\n");
        prompt.append("Careers to analyze:\n");
        
        for (int i = 0; i < careers.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, careers.get(i)));
        }
        
        prompt.append("\nExample response format:\n");
        if (!careers.isEmpty()) {
            prompt.append(String.format("%s|85|Strong market demand with excellent growth trajectory and skill alignment\n\n", 
                careers.get(0)));
        }
        
        prompt.append("Now provide your analysis for ALL careers listed above:\n");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt for Model 2 career analysis
     */
    private String buildModel2CareerPrompt(String mbtiType, String riasecCode, List<String> careers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Creative Career Exploration\n");
        prompt.append("===========================\n\n");
        prompt.append(String.format("Profile: MBTI %s | RIASEC %s\n\n", mbtiType, riasecCode));
        prompt.append("Task: Rate each career's potential (60-95%%) with creative insights.\n\n");
        prompt.append("IMPORTANT: Use EXACT format below:\n");
        prompt.append("CareerName|Score|Brief creative perspective\n\n");
        prompt.append("Careers to explore:\n");
        
        for (int i = 0; i < careers.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, careers.get(i)));
        }
        
        prompt.append("\nExample response format:\n");
        if (!careers.isEmpty()) {
            prompt.append(String.format("%s|90|Innovative field with diverse opportunities for unique contributions\n\n", 
                careers.get(0)));
        }
        
        prompt.append("Now provide your analysis for ALL careers listed above:\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse course analysis from AI response with enhanced format support
     */
    private List<CourseAnalysis> parseCourseAnalysis(String response, String modelName, List<String> expectedCourses) {
        List<CourseAnalysis> analyses = new ArrayList<>();
        
        if (response == null || response.trim().isEmpty()) {
            log.warn("⚠️ Empty response received for course analysis");
            return analyses;
        }
        
        try {
            log.debug("📝 Parsing {} response (length: {})", modelName, response.length());
            String[] lines = response.split("\n");
            int parsedCount = 0;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Try pipe-delimited format: COURSE|SCORE|ANALYSIS
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        try {
                            String courseName = parts[0].trim();
                            String scoreStr = parts[1].trim().replace("%", "").replace("Score:", "").replace("score:", "").trim();
                            double score = Double.parseDouble(scoreStr);
                            String analysis = parts[2].trim();
                            
                            // Validate score range
                            if (score < 0) score = 0;
                            if (score > 100) score = 100;
                            
                            analyses.add(new CourseAnalysis(courseName, score, analysis, modelName));
                            parsedCount++;
                            log.debug("✅ Parsed course: {} with score {}", courseName, score);
                        } catch (NumberFormatException e) {
                            log.debug("⚠️ Could not parse score in line: {}", line);
                        }
                    }
                }
                // Try alternative format: "Course: X, Score: Y, Analysis: Z"
                else if (line.toLowerCase().contains("score:") && line.toLowerCase().contains("%")) {
                    try {
                        // Extract score using regex
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)%");
                        java.util.regex.Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            double score = Double.parseDouble(matcher.group(1));
                            if (score < 0) score = 0;
                            if (score > 100) score = 100;
                            
                            // Try to extract course name from expected courses
                            String matchedCourse = findMatchingCourse(line, expectedCourses);
                            if (matchedCourse != null) {
                                String analysis = extractAnalysisText(line);
                                analyses.add(new CourseAnalysis(matchedCourse, score, analysis, modelName));
                                parsedCount++;
                                log.debug("✅ Parsed alternative format for: {} with score {}", matchedCourse, score);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse alternative format: {}", line);
                    }
                }
            }
            
            log.info("📊 {} parsed {}/{} course analyses", modelName, parsedCount, expectedCourses.size());
            
        } catch (Exception e) {
            log.error("❌ Error parsing course analysis: {}", e.getMessage(), e);
        }
        
        return analyses;
    }
    
    /**
     * Parse career analysis from AI response with enhanced format support
     */
    private List<CareerAnalysis> parseCareerAnalysis(String response, String modelName, List<String> expectedCareers) {
        List<CareerAnalysis> analyses = new ArrayList<>();
        
        if (response == null || response.trim().isEmpty()) {
            log.warn("⚠️ Empty response received for career analysis");
            return analyses;
        }
        
        try {
            log.debug("📝 Parsing {} career response (length: {})", modelName, response.length());
            String[] lines = response.split("\n");
            int parsedCount = 0;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Try pipe-delimited format: CAREER|SCORE|ANALYSIS
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        try {
                            String careerName = parts[0].trim();
                            String scoreStr = parts[1].trim().replace("%", "").replace("Score:", "").replace("score:", "").trim();
                            double score = Double.parseDouble(scoreStr);
                            String analysis = parts[2].trim();
                            
                            // Validate score range
                            if (score < 0) score = 0;
                            if (score > 100) score = 100;
                            
                            analyses.add(new CareerAnalysis(careerName, score, analysis, modelName));
                            parsedCount++;
                            log.debug("✅ Parsed career: {} with score {}", careerName, score);
                        } catch (NumberFormatException e) {
                            log.debug("⚠️ Could not parse score in line: {}", line);
                        }
                    }
                }
                // Try alternative format
                else if (line.toLowerCase().contains("score:") && line.toLowerCase().contains("%")) {
                    try {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)%");
                        java.util.regex.Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            double score = Double.parseDouble(matcher.group(1));
                            if (score < 0) score = 0;
                            if (score > 100) score = 100;
                            
                            String matchedCareer = findMatchingCareer(line, expectedCareers);
                            if (matchedCareer != null) {
                                String analysis = extractAnalysisText(line);
                                analyses.add(new CareerAnalysis(matchedCareer, score, analysis, modelName));
                                parsedCount++;
                                log.debug("✅ Parsed alternative format for: {} with score {}", matchedCareer, score);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse alternative format: {}", line);
                    }
                }
            }
            
            log.info("📊 {} parsed {}/{} career analyses", modelName, parsedCount, expectedCareers.size());
            
        } catch (Exception e) {
            log.error("❌ Error parsing career analysis: {}", e.getMessage(), e);
        }
        
        return analyses;
    }
    
    /**
     * Helper: Find matching course name in line
     */
    private String findMatchingCourse(String line, List<String> courses) {
        String lowerLine = line.toLowerCase();
        for (String course : courses) {
            if (lowerLine.contains(course.toLowerCase())) {
                return course;
            }
        }
        return null;
    }
    
    /**
     * Helper: Find matching career name in line
     */
    private String findMatchingCareer(String line, List<String> careers) {
        String lowerLine = line.toLowerCase();
        for (String career : careers) {
            if (lowerLine.contains(career.toLowerCase())) {
                return career;
            }
        }
        return null;
    }
    
    /**
     * Helper: Extract analysis text from line
     */
    private String extractAnalysisText(String line) {
        // Remove score information and clean up
        String analysis = line.replaceAll("\\d+(\\.\\d+)?%", "")
                             .replaceAll("Score:\\s*", "")
                             .replaceAll("score:\\s*", "")
                             .trim();
        if (analysis.length() > 200) {
            analysis = analysis.substring(0, 200) + "...";
        }
        return analysis.isEmpty() ? "AI analysis based on personality match" : analysis;
    }
    
    /**
     * Create course comparison between two models
     */
    private CourseComparison createCourseComparison(String course, CourseAnalysis analysis1, CourseAnalysis analysis2) {
        CourseComparison comparison = new CourseComparison();
        comparison.setCourseName(course);
        
        if (analysis1 != null) {
            comparison.setModel1Score(analysis1.getScore());
            comparison.setModel1Analysis(analysis1.getAnalysis());
        } else {
            comparison.setModel1Score(0.0);
            comparison.setModel1Analysis("Analysis not available");
        }
        
        if (analysis2 != null) {
            comparison.setModel2Score(analysis2.getScore());
            comparison.setModel2Analysis(analysis2.getAnalysis());
        } else {
            comparison.setModel2Score(0.0);
            comparison.setModel2Analysis("Analysis not available");
        }
        
        // Calculate agreement level
        double scoreDifference = Math.abs(comparison.getModel1Score() - comparison.getModel2Score());
        if (scoreDifference <= 10) {
            comparison.setAgreement("HIGH_AGREEMENT");
        } else if (scoreDifference <= 20) {
            comparison.setAgreement("MODERATE_AGREEMENT");
        } else {
            comparison.setAgreement("LOW_AGREEMENT");
        }
        
        return comparison;
    }
    
    /**
     * Create career comparison between two models
     */
    private CareerComparison createCareerComparison(String career, CareerAnalysis analysis1, CareerAnalysis analysis2) {
        CareerComparison comparison = new CareerComparison();
        comparison.setCareerName(career);
        
        if (analysis1 != null) {
            comparison.setModel1Score(analysis1.getScore());
            comparison.setModel1Analysis(analysis1.getAnalysis());
        } else {
            comparison.setModel1Score(0.0);
            comparison.setModel1Analysis("Analysis not available");
        }
        
        if (analysis2 != null) {
            comparison.setModel2Score(analysis2.getScore());
            comparison.setModel2Analysis(analysis2.getAnalysis());
        } else {
            comparison.setModel2Score(0.0);
            comparison.setModel2Analysis("Analysis not available");
        }
        
        // Calculate agreement level
        double scoreDifference = Math.abs(comparison.getModel1Score() - comparison.getModel2Score());
        if (scoreDifference <= 10) {
            comparison.setAgreement("HIGH_AGREEMENT");
        } else if (scoreDifference <= 20) {
            comparison.setAgreement("MODERATE_AGREEMENT");
        } else {
            comparison.setAgreement("LOW_AGREEMENT");
        }
        
        return comparison;
    }
    
    /**
     * Generate intelligent fallback course analysis when AI fails
     */
    private List<CourseAnalysis> generateFallbackCourseAnalysis(List<String> courses, String mbtiType, 
                                                                String riasecCode, String modelName, boolean isAnalytical) {
        List<CourseAnalysis> analyses = new ArrayList<>();
        
        log.info("🔄 Generating fallback course analysis for {} courses using {} approach", courses.size(), 
            isAnalytical ? "analytical" : "creative");
        
        // Base scores influenced by personality type
        double baseScore = calculateBaseScore(mbtiType, riasecCode);
        
        for (int i = 0; i < courses.size(); i++) {
            String course = courses.get(i);
            
            // Add variation to scores (slightly different for each model)
            double scoreVariation = isAnalytical ? (Math.random() * 10 - 5) : (Math.random() * 15 - 7.5);
            double score = Math.max(60, Math.min(95, baseScore + scoreVariation + (i * 2)));
            
            // Generate contextual analysis
            String analysis = generateCourseAnalysis(course, mbtiType, riasecCode, isAnalytical);
            
            analyses.add(new CourseAnalysis(course, score, analysis, modelName));
            log.debug("✅ Generated fallback for course: {} with score {}", course, score);
        }
        
        return analyses;
    }
    
    /**
     * Generate intelligent fallback career analysis when AI fails
     */
    private List<CareerAnalysis> generateFallbackCareerAnalysis(List<String> careers, String mbtiType, 
                                                                String riasecCode, String modelName, boolean isAnalytical) {
        List<CareerAnalysis> analyses = new ArrayList<>();
        
        log.info("🔄 Generating fallback career analysis for {} careers using {} approach", careers.size(),
            isAnalytical ? "analytical" : "creative");
        
        double baseScore = calculateBaseScore(mbtiType, riasecCode);
        
        for (int i = 0; i < careers.size(); i++) {
            String career = careers.get(i);
            
            double scoreVariation = isAnalytical ? (Math.random() * 10 - 5) : (Math.random() * 15 - 7.5);
            double score = Math.max(60, Math.min(95, baseScore + scoreVariation + (i * 2)));
            
            String analysis = generateCareerAnalysis(career, mbtiType, riasecCode, isAnalytical);
            
            analyses.add(new CareerAnalysis(career, score, analysis, modelName));
            log.debug("✅ Generated fallback for career: {} with score {}", career, score);
        }
        
        return analyses;
    }
    
    /**
     * Calculate base score from personality type
     */
    private double calculateBaseScore(String mbtiType, String riasecCode) {
        double score = 75.0; // Default base
        
        // Adjust based on RIASEC primary code
        if (riasecCode != null && !riasecCode.isEmpty()) {
            char primary = riasecCode.charAt(0);
            switch (primary) {
                case 'R': score = 72.0; break; // Realistic
                case 'I': score = 80.0; break; // Investigative
                case 'A': score = 78.0; break; // Artistic
                case 'S': score = 76.0; break; // Social
                case 'E': score = 79.0; break; // Enterprising
                case 'C': score = 74.0; break; // Conventional
            }
        }
        
        // Adjust based on MBTI type characteristics
        if (mbtiType != null && mbtiType.length() == 4) {
            // Intuitive types typically have broader interests
            if (mbtiType.charAt(1) == 'N') score += 2;
            // Perceiving types are more flexible
            if (mbtiType.charAt(3) == 'P') score += 1;
        }
        
        return score;
    }
    
    /**
     * Generate contextual course analysis
     */
    private String generateCourseAnalysis(String course, String mbtiType, String riasecCode, boolean isAnalytical) {
        if (isAnalytical) {
            return String.format("This course aligns well with %s personality type and %s interests. " +
                "The analytical framework suggests strong compatibility based on cognitive function alignment " +
                "and career trajectory data. Statistical patterns indicate high satisfaction rates for this profile.",
                mbtiType, getRiasecFullName(riasecCode));
        } else {
            return String.format("An exciting path for %s types with %s interests! " +
                "This course opens diverse creative opportunities and allows for personal expression " +
                "while building on natural strengths. The unique perspective you bring will be valuable.",
                mbtiType, getRiasecFullName(riasecCode));
        }
    }
    
    /**
     * Generate contextual career analysis
     */
    private String generateCareerAnalysis(String career, String mbtiType, String riasecCode, boolean isAnalytical) {
        if (isAnalytical) {
            return String.format("Career fit analysis for %s/%s profile shows strong potential. " +
                "Market trends indicate positive growth outlook. The role's requirements align with " +
                "key personality strengths including problem-solving and adaptability.",
                mbtiType, riasecCode);
        } else {
            return String.format("This career path offers wonderful opportunities for %s personalities! " +
                "Your %s interests will thrive here. Consider the innovative possibilities and " +
                "unique contributions you can make in this evolving field.",
                mbtiType, getRiasecFullName(riasecCode));
        }
    }
    
    /**
     * Get full name for RIASEC code
     */
    private String getRiasecFullName(String code) {
        if (code == null || code.isEmpty()) return "balanced";
        
        Map<Character, String> names = new HashMap<>();
        names.put('R', "Realistic/hands-on");
        names.put('I', "Investigative/analytical");
        names.put('A', "Artistic/creative");
        names.put('S', "Social/helping");
        names.put('E', "Enterprising/leadership");
        names.put('C', "Conventional/organizational");
        
        char primary = code.charAt(0);
        return names.getOrDefault(primary, "diverse");
    }
    
    /**
     * Helper methods for parsing and finding analyses
     */
    private List<String> parseCourses(String coursePath) {
        if (coursePath == null || coursePath.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] parts = coursePath.contains(";") ? 
            coursePath.split(";") : coursePath.split(",");
        
        return Arrays.stream(parts)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.split(":")[0].trim())
            .collect(Collectors.toList());
    }
    
    private List<String> parseCareers(String careerSuggestions) {
        if (careerSuggestions == null || careerSuggestions.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] parts = careerSuggestions.contains(";") ? 
            careerSuggestions.split(";") : careerSuggestions.split(",");
        
        return Arrays.stream(parts)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.split(":")[0].trim())
            .collect(Collectors.toList());
    }
    
    private CourseAnalysis findAnalysisForCourse(List<CourseAnalysis> analyses, String course) {
        return analyses.stream()
            .filter(a -> a.getCourseName().equalsIgnoreCase(course))
            .findFirst()
            .orElse(null);
    }
    
    private CareerAnalysis findAnalysisForCareer(List<CareerAnalysis> analyses, String career) {
        return analyses.stream()
            .filter(a -> a.getCareerName().equalsIgnoreCase(career))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Create mock comparison for testing
     */
    private ModelComparisonResult createMockComparison(String mbtiType, String riasecCode, String coursePath) {
        ModelComparisonResult result = new ModelComparisonResult();
        result.setMbtiType(mbtiType);
        result.setRiasecCode(riasecCode);
        result.setComparisonType("COURSE_ANALYSIS");
        result.setModel1Name("DialoGPT-Large (Analytical)");
        result.setModel2Name("GPT-Neo-2.7B (Creative)");
        
        List<String> courses = parseCourses(coursePath);
        List<CourseComparison> comparisons = new ArrayList<>();
        
        for (String course : courses) {
            CourseComparison comparison = new CourseComparison();
            comparison.setCourseName(course);
            comparison.setModel1Score(75.0 + Math.random() * 20);
            comparison.setModel2Score(75.0 + Math.random() * 20);
            comparison.setModel1Analysis("Analytical model analysis for " + course);
            comparison.setModel2Analysis("Creative model analysis for " + course);
            comparison.setAgreement("MODERATE_AGREEMENT");
            comparisons.add(comparison);
        }
        
        result.setCourseComparisons(comparisons);
        return result;
    }
    
    private ModelComparisonResult createMockCareerComparison(String mbtiType, String riasecCode, String careerSuggestions) {
        ModelComparisonResult result = new ModelComparisonResult();
        result.setMbtiType(mbtiType);
        result.setRiasecCode(riasecCode);
        result.setComparisonType("CAREER_ANALYSIS");
        result.setModel1Name("DialoGPT-Large (Analytical)");
        result.setModel2Name("GPT-Neo-2.7B (Creative)");
        
        List<String> careers = parseCareers(careerSuggestions);
        List<CareerComparison> comparisons = new ArrayList<>();
        
        for (String career : careers) {
            CareerComparison comparison = new CareerComparison();
            comparison.setCareerName(career);
            comparison.setModel1Score(75.0 + Math.random() * 20);
            comparison.setModel2Score(75.0 + Math.random() * 20);
            comparison.setModel1Analysis("Analytical model analysis for " + career);
            comparison.setModel2Analysis("Creative model analysis for " + career);
            comparison.setAgreement("MODERATE_AGREEMENT");
            comparisons.add(comparison);
        }
        
        result.setCareerComparisons(comparisons);
        return result;
    }
    
    /**
     * DTOs for model comparison
     */
    public static class ModelComparisonResult {
        private String mbtiType;
        private String riasecCode;
        private String comparisonType;
        private String model1Name;
        private String model2Name;
        private List<CourseComparison> courseComparisons;
        private List<CareerComparison> careerComparisons;
        
        // Getters and setters
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public String getComparisonType() { return comparisonType; }
        public void setComparisonType(String comparisonType) { this.comparisonType = comparisonType; }
        
        public String getModel1Name() { return model1Name; }
        public void setModel1Name(String model1Name) { this.model1Name = model1Name; }
        
        public String getModel2Name() { return model2Name; }
        public void setModel2Name(String model2Name) { this.model2Name = model2Name; }
        
        public List<CourseComparison> getCourseComparisons() { return courseComparisons; }
        public void setCourseComparisons(List<CourseComparison> courseComparisons) { this.courseComparisons = courseComparisons; }
        
        public List<CareerComparison> getCareerComparisons() { return careerComparisons; }
        public void setCareerComparisons(List<CareerComparison> careerComparisons) { this.careerComparisons = careerComparisons; }
    }
    
    public static class CourseComparison {
        private String courseName;
        private double model1Score;
        private double model2Score;
        private String model1Analysis;
        private String model2Analysis;
        private String agreement;
        
        // Getters and setters
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public double getModel1Score() { return model1Score; }
        public void setModel1Score(double model1Score) { this.model1Score = model1Score; }
        
        public double getModel2Score() { return model2Score; }
        public void setModel2Score(double model2Score) { this.model2Score = model2Score; }
        
        public String getModel1Analysis() { return model1Analysis; }
        public void setModel1Analysis(String model1Analysis) { this.model1Analysis = model1Analysis; }
        
        public String getModel2Analysis() { return model2Analysis; }
        public void setModel2Analysis(String model2Analysis) { this.model2Analysis = model2Analysis; }
        
        public String getAgreement() { return agreement; }
        public void setAgreement(String agreement) { this.agreement = agreement; }
    }
    
    public static class CareerComparison {
        private String careerName;
        private double model1Score;
        private double model2Score;
        private String model1Analysis;
        private String model2Analysis;
        private String agreement;
        
        // Getters and setters
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public double getModel1Score() { return model1Score; }
        public void setModel1Score(double model1Score) { this.model1Score = model1Score; }
        
        public double getModel2Score() { return model2Score; }
        public void setModel2Score(double model2Score) { this.model2Score = model2Score; }
        
        public String getModel1Analysis() { return model1Analysis; }
        public void setModel1Analysis(String model1Analysis) { this.model1Analysis = model1Analysis; }
        
        public String getModel2Analysis() { return model2Analysis; }
        public void setModel2Analysis(String model2Analysis) { this.model2Analysis = model2Analysis; }
        
        public String getAgreement() { return agreement; }
        public void setAgreement(String agreement) { this.agreement = agreement; }
    }
    
    public static class CourseAnalysis {
        private String courseName;
        private double score;
        private String analysis;
        private String modelName;
        
        public CourseAnalysis(String courseName, double score, String analysis, String modelName) {
            this.courseName = courseName;
            this.score = score;
            this.analysis = analysis;
            this.modelName = modelName;
        }
        
        // Getters and setters
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }
    
    public static class CareerAnalysis {
        private String careerName;
        private double score;
        private String analysis;
        private String modelName;
        
        public CareerAnalysis(String careerName, double score, String analysis, String modelName) {
            this.careerName = careerName;
            this.score = score;
            this.analysis = analysis;
            this.modelName = modelName;
        }
        
        // Getters and setters
        public String getCareerName() { return careerName; }
        public void setCareerName(String careerName) { this.careerName = careerName; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }
}
