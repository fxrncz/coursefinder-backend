package com.app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class HuggingFaceApiService {
    
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceApiService.class);
    
    @Value("${huggingface.api.key}")
    private String apiKey;
    
    @Value("${huggingface.api.url}")
    private String apiUrl;
    
    @Value("${huggingface.model.validation}")
    private String validationModel;
    
    @Value("${huggingface.model.education}")
    private String educationModel;
    
    @Value("${huggingface.timeout}")
    private int timeout;
    
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    public HuggingFaceApiService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Validate course description using AI
     */
    public ValidationResponse validateCourseDescription(String courseName, String description) {
        String prompt = buildCourseValidationPrompt(courseName, description);
        return callHuggingFaceApi(educationModel, prompt, "course_validation");
    }
    
    /**
     * Validate career information using AI
     */
    public ValidationResponse validateCareerInfo(String careerName, String description, String salaryRange) {
        String prompt = buildCareerValidationPrompt(careerName, description, salaryRange);
        return callHuggingFaceApi(validationModel, prompt, "career_validation");
    }
    
    /**
     * Validate MBTI/RIASEC mapping using AI
     */
    public ValidationResponse validatePersonalityMapping(String mbtiType, String riasecCode, String courseSuggestions) {
        String prompt = buildPersonalityValidationPrompt(mbtiType, riasecCode, courseSuggestions);
        return callHuggingFaceApi(educationModel, prompt, "personality_validation");
    }
    
    /**
     * General text generation using Hugging Face API
     */
    public String generateText(String model, String prompt) {
        try {
            ValidationResponse response = callHuggingFaceApi(model, prompt, "text_generation");
            return response.getMessage();
        } catch (Exception e) {
            log.error("Failed to generate text with model {}: {}", model, e.getMessage());
            return null;
        }
    }
    
    /**
     * Call a specific model (for testing purposes)
     */
    public ValidationResponse callModel(String model, String prompt) {
        return callHuggingFaceApi(model, prompt, "model_testing");
    }
    
    /**
     * Call Hugging Face Inference API
     */
    private ValidationResponse callHuggingFaceApi(String model, String input, String taskType) {
        try {
            String url = apiUrl.replace("/models", "/models/" + model);
            
            // Build request body with enhanced parameters for paid plan
            ApiRequest apiRequest = new ApiRequest(input);
            apiRequest.setParameters(new ApiParameters());
            String requestBody = objectMapper.writeValueAsString(apiRequest);
            
            RequestBody body = RequestBody.create(
                requestBody,
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            log.info("🤖 Calling Hugging Face API: {} for task: {}", model, taskType);
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("❌ Hugging Face API call failed: {} {} - Body: {}", 
                        response.code(), response.message(), errorBody);
                    
                    // Check if model is loading
                    if (response.code() == 503 || errorBody.contains("loading")) {
                        return new ValidationResponse(false, 
                            "Model is loading. This may take 30-60 seconds on first use. Please try again in a moment.", 0.0);
                    }
                    
                    // Check for rate limiting
                    if (response.code() == 429) {
                        return new ValidationResponse(false, 
                            "API rate limit exceeded. Please try again later.", 0.0);
                    }
                    
                    // Check for authentication errors
                    if (response.code() == 401 || response.code() == 403) {
                        return new ValidationResponse(false, 
                            "Authentication failed. Please check your API key.", 0.0);
                    }
                    
                    return new ValidationResponse(false, 
                        "API call failed: " + response.message() + " (Status: " + response.code() + ")", 0.0);
                }
                
                String responseBody = response.body().string();
                log.debug("📥 Received response from Hugging Face: {}", responseBody);
                
                // Check if response indicates model is loading
                if (responseBody.contains("\"error\"") && responseBody.contains("loading")) {
                    return new ValidationResponse(false, 
                        "Model is currently loading. Please try again in 30-60 seconds.", 0.0);
                }
                
                return parseValidationResponse(responseBody, taskType);
            }
            
        } catch (IOException e) {
            log.error("❌ Error calling Hugging Face API: {}", e.getMessage(), e);
            return new ValidationResponse(false, "Network error: " + e.getMessage(), 0.0);
        } catch (Exception e) {
            log.error("❌ Unexpected error in Hugging Face API call: {}", e.getMessage(), e);
            return new ValidationResponse(false, "Unexpected error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Parse Hugging Face API response
     */
    private ValidationResponse parseValidationResponse(String responseBody, String taskType) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Handle different response formats
            if (rootNode.isArray()) {
                // Standard inference response format
                JsonNode firstResult = rootNode.get(0);
                if (firstResult.has("generated_text")) {
                    String generatedText = firstResult.get("generated_text").asText();
                    return analyzeGeneratedText(generatedText, taskType);
                }
            } else if (rootNode.has("generated_text")) {
                String generatedText = rootNode.get("generated_text").asText();
                return analyzeGeneratedText(generatedText, taskType);
            }
            
            log.warn("⚠️ Unexpected response format from Hugging Face: {}", responseBody);
            return new ValidationResponse(false, "Unexpected response format", 0.0);
            
        } catch (Exception e) {
            log.error("❌ Error parsing Hugging Face response: {}", e.getMessage(), e);
            return new ValidationResponse(false, "Response parsing error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Analyze generated text to determine validation result
     */
    private ValidationResponse analyzeGeneratedText(String generatedText, String taskType) {
        String lowerText = generatedText.toLowerCase();
        
        // Simple keyword-based analysis (can be enhanced with more sophisticated NLP)
        boolean isValid = true;
        double confidence = 0.8; // Default confidence
        
        // Look for validation indicators
        if (lowerText.contains("inaccurate") || lowerText.contains("incorrect") || 
            lowerText.contains("invalid") || lowerText.contains("error")) {
            isValid = false;
            confidence = 0.9;
        } else if (lowerText.contains("accurate") || lowerText.contains("correct") || 
                   lowerText.contains("valid") || lowerText.contains("good")) {
            isValid = true;
            confidence = 0.9;
        } else if (lowerText.contains("maybe") || lowerText.contains("possibly") || 
                   lowerText.contains("uncertain")) {
            confidence = 0.5;
        }
        
        return new ValidationResponse(isValid, generatedText, confidence);
    }
    
    /**
     * Build course validation prompt
     */
    private String buildCourseValidationPrompt(String courseName, String description) {
        return String.format(
            "Please validate this course description for accuracy and completeness:\n\n" +
            "Course: %s\n" +
            "Description: %s\n\n" +
            "Check if the description accurately represents what students would learn in this course. " +
            "Consider: curriculum relevance, career applicability, and educational standards. " +
            "Respond with 'VALID' if accurate, 'INVALID' if inaccurate, followed by your reasoning.",
            courseName, description
        );
    }
    
    /**
     * Build career validation prompt
     */
    private String buildCareerValidationPrompt(String careerName, String description, String salaryRange) {
        return String.format(
            "Please validate this career information for accuracy:\n\n" +
            "Career: %s\n" +
            "Description: %s\n" +
            "Salary Range: %s\n\n" +
            "Check if the description accurately represents this career, its responsibilities, and if the salary range " +
            "is reasonable for the Philippines job market. Respond with 'VALID' if accurate, 'INVALID' if inaccurate, " +
            "followed by your reasoning.",
            careerName, description, salaryRange
        );
    }
    
    /**
     * Build personality validation prompt
     */
    private String buildPersonalityValidationPrompt(String mbtiType, String riasecCode, String courseSuggestions) {
        return String.format(
            "Please validate this personality-based course recommendation:\n\n" +
            "MBTI Type: %s\n" +
            "RIASEC Code: %s\n" +
            "Suggested Courses: %s\n\n" +
            "Check if the course suggestions align with the personality type and career interests. " +
            "Consider psychological research on MBTI and RIASEC correlations with career success. " +
            "Respond with 'VALID' if the recommendations are appropriate, 'INVALID' if not, followed by your reasoning.",
            mbtiType, riasecCode, courseSuggestions
        );
    }
    
    /**
     * Request DTO for Hugging Face API
     */
    public static class ApiRequest {
        private String inputs;
        private ApiParameters parameters;
        
        public ApiRequest(String inputs) {
            this.inputs = inputs;
        }
        
        public String getInputs() { return inputs; }
        public void setInputs(String inputs) { this.inputs = inputs; }
        
        public ApiParameters getParameters() { return parameters; }
        public void setParameters(ApiParameters parameters) { this.parameters = parameters; }
    }
    
    /**
     * API Parameters for enhanced control (paid plan features)
     */
    public static class ApiParameters {
        private boolean returnFullText = false;  // Don't return full text, just the generated part
        private boolean useCache = true;         // Use caching for better performance
        private boolean waitForModel = true;     // Wait for model to load if needed
        private int maxNewTokens = 512;          // Limit response length for faster processing
        
        public boolean isReturnFullText() { return returnFullText; }
        public void setReturnFullText(boolean returnFullText) { this.returnFullText = returnFullText; }
        
        public boolean isUseCache() { return useCache; }
        public void setUseCache(boolean useCache) { this.useCache = useCache; }
        
        public boolean isWaitForModel() { return waitForModel; }
        public void setWaitForModel(boolean waitForModel) { this.waitForModel = waitForModel; }
        
        public int getMaxNewTokens() { return maxNewTokens; }
        public void setMaxNewTokens(int maxNewTokens) { this.maxNewTokens = maxNewTokens; }
    }
    
    /**
     * Validation response DTO
     */
    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private double confidence;
        private String timestamp;
        
        public ValidationResponse(boolean valid, String message, double confidence) {
            this.valid = valid;
            this.message = message;
            this.confidence = confidence;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
