package com.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ModelTestingService {
    
    private static final Logger log = LoggerFactory.getLogger(ModelTestingService.class);
    
    @Autowired
    private HuggingFaceApiService huggingFaceService;
    
    @Value("${huggingface.model.validation}")
    private String currentValidationModel;
    
    @Value("${huggingface.model.education}")
    private String currentEducationModel;
    
    /**
     * Test different models with sample validation tasks
     */
    public ModelComparisonResult compareModels() {
        log.info("🧪 Starting model comparison test");
        
        ModelComparisonResult result = new ModelComparisonResult();
        result.setTimestamp(java.time.LocalDateTime.now().toString());
        
        // Define test cases
        List<TestCase> testCases = createTestCases();
        
        // Define models to test (optimized for paid plan)
        List<String> modelsToTest = Arrays.asList(
            "microsoft/DialoGPT-large",     // High quality, good for validation
            "EleutherAI/gpt-neo-2.7B",     // Premium model, excellent reasoning
            "google/t5-base",               // Good for text-to-text tasks
            "microsoft/DialoGPT-medium",    // Fast fallback
            "facebook/blenderbot-400M-distill" // Fastest option
        );
        
        Map<String, ModelTestResult> results = new HashMap<>();
        
        for (String model : modelsToTest) {
            log.info("🧪 Testing model: {}", model);
            ModelTestResult modelResult = testModel(model, testCases);
            results.put(model, modelResult);
        }
        
        result.setModelResults(results);
        result.setRecommendations(generateRecommendations(results));
        
        log.info("✅ Model comparison completed");
        return result;
    }
    
    /**
     * Test a specific model with all test cases
     */
    private ModelTestResult testModel(String model, List<TestCase> testCases) {
        ModelTestResult result = new ModelTestResult();
        result.setModelName(model);
        result.setTestStartTime(System.currentTimeMillis());
        
        int passedTests = 0;
        int totalTests = testCases.size();
        double totalConfidence = 0.0;
        long totalResponseTime = 0;
        
        for (TestCase testCase : testCases) {
            try {
                long startTime = System.currentTimeMillis();
                
                HuggingFaceApiService.ValidationResponse response = 
                    huggingFaceService.callModel(model, testCase.getPrompt());
                
                long responseTime = System.currentTimeMillis() - startTime;
                totalResponseTime += responseTime;
                
                if (response.isValid()) {
                    passedTests++;
                }
                
                totalConfidence += response.getConfidence();
                
                log.debug("✅ Test case '{}' for model {}: {}", 
                    testCase.getName(), model, response.isValid() ? "PASSED" : "FAILED");
                
            } catch (Exception e) {
                log.warn("❌ Test case '{}' failed for model {}: {}", 
                    testCase.getName(), model, e.getMessage());
            }
        }
        
        result.setTestEndTime(System.currentTimeMillis());
        result.setPassedTests(passedTests);
        result.setTotalTests(totalTests);
        result.setSuccessRate((double) passedTests / totalTests);
        result.setAverageConfidence(totalConfidence / totalTests);
        result.setAverageResponseTime(totalResponseTime / totalTests);
        
        return result;
    }
    
    /**
     * Create test cases for model comparison
     */
    private List<TestCase> createTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        
        // Test Case 1: Course Description Validation
        testCases.add(new TestCase(
            "course_validation",
            "Validate this course description: 'BS Computer Science: A program that teaches students how to use Microsoft Word and Excel.'",
            "Course validation"
        ));
        
        // Test Case 2: Career Information Validation
        testCases.add(new TestCase(
            "career_validation",
            "Validate this career info: 'Software Engineer: A person who fixes computers and installs software.'",
            "Career validation"
        ));
        
        // Test Case 3: Personality Mapping Validation
        testCases.add(new TestCase(
            "personality_validation",
            "Validate this personality mapping: 'INTJ (Introverted, Intuitive, Thinking, Judging) should be recommended for careers in sales and marketing.'",
            "Personality validation"
        ));
        
        // Test Case 4: Educational Content Validation
        testCases.add(new TestCase(
            "education_validation",
            "Validate this educational statement: 'Psychology is the study of rocks and minerals.'",
            "Educational content validation"
        ));
        
        return testCases;
    }
    
    /**
     * Generate recommendations based on test results
     */
    private List<String> generateRecommendations(Map<String, ModelTestResult> results) {
        List<String> recommendations = new ArrayList<>();
        
        // Find best performing models
        String bestAccuracy = results.entrySet().stream()
            .max(Comparator.comparing(entry -> entry.getValue().getSuccessRate()))
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        String fastest = results.entrySet().stream()
            .min(Comparator.comparing(entry -> entry.getValue().getAverageResponseTime()))
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        String bestConfidence = results.entrySet().stream()
            .max(Comparator.comparing(entry -> entry.getValue().getAverageConfidence()))
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        recommendations.add("Best Accuracy: " + bestAccuracy + 
            " (" + String.format("%.1f%%", results.get(bestAccuracy).getSuccessRate() * 100) + ")");
        recommendations.add("Fastest Response: " + fastest + 
            " (" + results.get(fastest).getAverageResponseTime() + "ms avg)");
        recommendations.add("Highest Confidence: " + bestConfidence + 
            " (" + String.format("%.2f", results.get(bestConfidence).getAverageConfidence()) + ")");
        
        // Generate specific recommendations
        if (results.get("microsoft/DialoGPT-large").getSuccessRate() > 0.8) {
            recommendations.add("Recommend DialoGPT-large for production use (high accuracy)");
        } else if (results.get("google/t5-base").getSuccessRate() > 0.7) {
            recommendations.add("Recommend T5-base for balanced performance");
        } else {
            recommendations.add("Consider using DialoGPT-medium for cost-effective validation");
        }
        
        return recommendations;
    }
    
    /**
     * Test current configuration
     */
    public ConfigurationTestResult testCurrentConfiguration() {
        log.info("🧪 Testing current model configuration");
        
        ConfigurationTestResult result = new ConfigurationTestResult();
        result.setValidationModel(currentValidationModel);
        result.setEducationModel(currentEducationModel);
        result.setTimestamp(java.time.LocalDateTime.now().toString());
        
        try {
            // Test validation model
            String testPrompt = "Validate this course: 'BS Computer Science: A comprehensive program in computing'";
            String validationResponse = 
                huggingFaceService.generateText(currentValidationModel, testPrompt);
            result.setValidationModelWorking(validationResponse != null && !validationResponse.trim().isEmpty());
            
            // Test education model
            String educationResponse = 
                huggingFaceService.generateText(currentEducationModel, testPrompt);
            result.setEducationModelWorking(educationResponse != null && !educationResponse.trim().isEmpty());
            
            result.setOverallWorking(result.isValidationModelWorking() && result.isEducationModelWorking());
            
        } catch (Exception e) {
            log.error("❌ Configuration test failed: {}", e.getMessage(), e);
            result.setOverallWorking(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * DTOs for test results
     */
    public static class TestCase {
        private String name;
        private String prompt;
        private String description;
        
        public TestCase(String name, String prompt, String description) {
            this.name = name;
            this.prompt = prompt;
            this.description = description;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class ModelTestResult {
        private String modelName;
        private long testStartTime;
        private long testEndTime;
        private int passedTests;
        private int totalTests;
        private double successRate;
        private double averageConfidence;
        private long averageResponseTime;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public long getTestStartTime() { return testStartTime; }
        public void setTestStartTime(long testStartTime) { this.testStartTime = testStartTime; }
        
        public long getTestEndTime() { return testEndTime; }
        public void setTestEndTime(long testEndTime) { this.testEndTime = testEndTime; }
        
        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
        
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }
        
        public long getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(long averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    }
    
    public static class ModelComparisonResult {
        private String timestamp;
        private Map<String, ModelTestResult> modelResults;
        private List<String> recommendations;
        
        // Getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public Map<String, ModelTestResult> getModelResults() { return modelResults; }
        public void setModelResults(Map<String, ModelTestResult> modelResults) { this.modelResults = modelResults; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    public static class ConfigurationTestResult {
        private String timestamp;
        private String validationModel;
        private String educationModel;
        private boolean validationModelWorking;
        private boolean educationModelWorking;
        private boolean overallWorking;
        private String errorMessage;
        
        // Getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getValidationModel() { return validationModel; }
        public void setValidationModel(String validationModel) { this.validationModel = validationModel; }
        
        public String getEducationModel() { return educationModel; }
        public void setEducationModel(String educationModel) { this.educationModel = educationModel; }
        
        public boolean isValidationModelWorking() { return validationModelWorking; }
        public void setValidationModelWorking(boolean validationModelWorking) { this.validationModelWorking = validationModelWorking; }
        
        public boolean isEducationModelWorking() { return educationModelWorking; }
        public void setEducationModelWorking(boolean educationModelWorking) { this.educationModelWorking = educationModelWorking; }
        
        public boolean isOverallWorking() { return overallWorking; }
        public void setOverallWorking(boolean overallWorking) { this.overallWorking = overallWorking; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
