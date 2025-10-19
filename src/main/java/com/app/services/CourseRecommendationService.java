package com.app.services;

import com.app.models.MbtiRiasecMapping;
import com.app.repositories.MbtiRiasecMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseRecommendationService {
    
    @Autowired
    private MbtiRiasecMappingRepository mappingRepository;
    
    /**
     * Get course recommendations based on personality test results
     */
    public CourseRecommendationDTO getRecommendations(TestResultService.TestResultDTO personalityResult) {
        String mbtiType = personalityResult.getMbtiType();
        List<String> riasecTopTwo = Arrays.asList(personalityResult.getRiasecCode().split(""));
        List<String> goalTags = parseGoalTags(personalityResult.getStudentGoals());
        
        // Get recommendations based on MBTI and RIASEC combination
        List<MbtiRiasecMapping> exactMatches = mappingRepository.findByMbtiTypeAndRiasecCodesOrderById(
                mbtiType, riasecTopTwo);
        
        // Get additional recommendations based on MBTI only
        List<MbtiRiasecMapping> mbtiMatches = mappingRepository.findByMbtiType(mbtiType);
        
        // Get additional recommendations based on RIASEC only
        List<MbtiRiasecMapping> riasecMatches = mappingRepository.findByRiasecCodesOrderById(riasecTopTwo);
        
        // Build recommendation response
        CourseRecommendationDTO recommendation = new CourseRecommendationDTO();
        recommendation.setMbtiType(mbtiType);
        recommendation.setRiasecTopTwo(riasecTopTwo);
        recommendation.setGoalTags(goalTags);
        
        // Categorize recommendations
        recommendation.setExactMatches(limitAndConvert(exactMatches, 10));
        recommendation.setMbtiMatches(limitAndConvert(filterOut(mbtiMatches, exactMatches), 5));
        recommendation.setRiasecMatches(limitAndConvert(filterOut(riasecMatches, exactMatches), 5));
        
        // Get top recommendations (best overall matches)
        List<MbtiRiasecMapping> topRecommendations = getTopRecommendations(exactMatches, mbtiMatches, riasecMatches);
        recommendation.setTopRecommendations(limitAndConvert(topRecommendations, 5));
        
        // Add statistics
        recommendation.setTotalExactMatches(exactMatches.size());
        recommendation.setTotalMbtiMatches(mbtiMatches.size());
        recommendation.setTotalRiasecMatches(riasecMatches.size());
        
        return recommendation;
    }
    
    /**
     * Get course recommendations by specific criteria
     */
    public List<CourseRecommendationItemDTO> getRecommendationsByMbti(String mbtiType, int limit) {
        List<MbtiRiasecMapping> mappings = mappingRepository.findByMbtiType(mbtiType);
        return limitAndConvert(mappings, limit);
    }
    
    public List<CourseRecommendationItemDTO> getRecommendationsByRiasec(List<String> riasecCodes, int limit) {
        List<MbtiRiasecMapping> mappings = mappingRepository.findByRiasecCodesOrderById(riasecCodes);
        return limitAndConvert(mappings, limit);
    }
    
    /**
     * Search courses by name (searching in suggested courses)
     */
    public List<CourseRecommendationItemDTO> searchCourses(String courseName, int limit) {
        List<MbtiRiasecMapping> mappings = mappingRepository.findBySuggestedCoursesContainingIgnoreCase(courseName);
        return limitAndConvert(mappings, limit);
    }
    
    /**
     * Get available filter options
     */
    public CourseFilterOptionsDTO getFilterOptions() {
        CourseFilterOptionsDTO options = new CourseFilterOptionsDTO();
        options.setMbtiTypes(mappingRepository.findAllUniqueMbtiTypes());
        options.setRiasecCodes(mappingRepository.findAllUniqueRiasecCodes());
        // Remove categories and universities as they no longer exist in the new schema
        options.setCategories(new ArrayList<>());
        options.setUniversities(new ArrayList<>());
        return options;
    }
    
    /**
     * Get detailed database statistics
     */
    public Object[] getDatabaseStatistics() {
        return mappingRepository.getDetailedStatistics();
    }
    
    // Helper methods
    private List<MbtiRiasecMapping> getTopRecommendations(List<MbtiRiasecMapping> exact, 
                                                          List<MbtiRiasecMapping> mbti, 
                                                          List<MbtiRiasecMapping> riasec) {
        Set<Long> seenIds = new HashSet<>();
        List<MbtiRiasecMapping> combined = new ArrayList<>();
        
        // Add exact matches first
        for (MbtiRiasecMapping mapping : exact) {
            if (!seenIds.contains(mapping.getId())) {
                combined.add(mapping);
                seenIds.add(mapping.getId());
            }
        }
        
        // Add best MBTI matches
        for (MbtiRiasecMapping mapping : mbti) {
            if (!seenIds.contains(mapping.getId()) && combined.size() < 10) {
                combined.add(mapping);
                seenIds.add(mapping.getId());
            }
        }
        
        // Add best RIASEC matches
        for (MbtiRiasecMapping mapping : riasec) {
            if (!seenIds.contains(mapping.getId()) && combined.size() < 10) {
                combined.add(mapping);
                seenIds.add(mapping.getId());
            }
        }
        
        return combined;
    }
    
    private List<MbtiRiasecMapping> filterOut(List<MbtiRiasecMapping> source, List<MbtiRiasecMapping> toRemove) {
        Set<Long> removeIds = toRemove.stream().map(MbtiRiasecMapping::getId).collect(Collectors.toSet());
        return source.stream()
                .filter(mapping -> !removeIds.contains(mapping.getId()))
                .collect(Collectors.toList());
    }
    
    private List<CourseRecommendationItemDTO> limitAndConvert(List<MbtiRiasecMapping> mappings, int limit) {
        return mappings.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private CourseRecommendationItemDTO convertToDTO(MbtiRiasecMapping mapping) {
        CourseRecommendationItemDTO dto = new CourseRecommendationItemDTO();
        dto.setId(mapping.getId());
        dto.setMbtiType(mapping.getMbtiType());
        dto.setRiasecCode(mapping.getRiasecCode());
        // Join arrays for backward-compatibility fields
        String joinedCourses = mapping.getCourses() != null ? String.join(", ", mapping.getCourses()) : "";
        String joinedCareers = mapping.getCareers() != null ? String.join(", ", mapping.getCareers()) : "";
        dto.setSuggestedCourses(joinedCourses);
        dto.setCareerSuggestions(joinedCareers);
        // Removed: learning style, study tips, and growth tips from recommendation item
        
        // Set default values for fields that no longer exist
        dto.setCourseName("Multiple Options Available");
        dto.setCourseDescription(joinedCourses);
        dto.setCareerOptions(joinedCareers);
        dto.setUniversity("Various Universities");
        dto.setProgramType("Bachelor's Degree");
        dto.setDuration("4 years");
        dto.setRequirements("Standard admission requirements");
        dto.setSalaryRange("Varies by field");
        dto.setJobOutlook("Good");
        dto.setSkillsNeeded("");
        dto.setWorkEnvironment("");
        dto.setMatchScore(1.0);  // Default match score since it no longer exists
        dto.setCategory("General");
        
        return dto;
    }
    
    // DTOs
    public static class CourseRecommendationDTO {
        private String mbtiType;
        private List<String> riasecTopTwo;
        private List<String> goalTags;
        private List<CourseRecommendationItemDTO> exactMatches;
        private List<CourseRecommendationItemDTO> mbtiMatches;
        private List<CourseRecommendationItemDTO> riasecMatches;
        private List<CourseRecommendationItemDTO> topRecommendations;
        private int totalExactMatches;
        private int totalMbtiMatches;
        private int totalRiasecMatches;
        
        // Getters and Setters
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public List<String> getRiasecTopTwo() { return riasecTopTwo; }
        public void setRiasecTopTwo(List<String> riasecTopTwo) { this.riasecTopTwo = riasecTopTwo; }
        
        public List<String> getGoalTags() { return goalTags; }
        public void setGoalTags(List<String> goalTags) { this.goalTags = goalTags; }
        
        public List<CourseRecommendationItemDTO> getExactMatches() { return exactMatches; }
        public void setExactMatches(List<CourseRecommendationItemDTO> exactMatches) { this.exactMatches = exactMatches; }
        
        public List<CourseRecommendationItemDTO> getMbtiMatches() { return mbtiMatches; }
        public void setMbtiMatches(List<CourseRecommendationItemDTO> mbtiMatches) { this.mbtiMatches = mbtiMatches; }
        
        public List<CourseRecommendationItemDTO> getRiasecMatches() { return riasecMatches; }
        public void setRiasecMatches(List<CourseRecommendationItemDTO> riasecMatches) { this.riasecMatches = riasecMatches; }
        
        public List<CourseRecommendationItemDTO> getTopRecommendations() { return topRecommendations; }
        public void setTopRecommendations(List<CourseRecommendationItemDTO> topRecommendations) { this.topRecommendations = topRecommendations; }
        
        public int getTotalExactMatches() { return totalExactMatches; }
        public void setTotalExactMatches(int totalExactMatches) { this.totalExactMatches = totalExactMatches; }
        
        public int getTotalMbtiMatches() { return totalMbtiMatches; }
        public void setTotalMbtiMatches(int totalMbtiMatches) { this.totalMbtiMatches = totalMbtiMatches; }
        
        public int getTotalRiasecMatches() { return totalRiasecMatches; }
        public void setTotalRiasecMatches(int totalRiasecMatches) { this.totalRiasecMatches = totalRiasecMatches; }
    }
    
    public static class CourseRecommendationItemDTO {
        private Long id;
        private String mbtiType;
        private String riasecCode;
        private String suggestedCourses;
        private String careerSuggestions;
        private String learningStyle;
        private String studyTips;
        private String personalityGrowthTips;
        
        // Legacy fields for backward compatibility
        private String courseName;
        private String courseDescription;
        private String careerOptions;
        private String university;
        private String programType;
        private String duration;
        private String requirements;
        private String salaryRange;
        private String jobOutlook;
        private String skillsNeeded;
        private String workEnvironment;
        private Double matchScore;
        private String category;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getMbtiType() { return mbtiType; }
        public void setMbtiType(String mbtiType) { this.mbtiType = mbtiType; }
        
        public String getRiasecCode() { return riasecCode; }
        public void setRiasecCode(String riasecCode) { this.riasecCode = riasecCode; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getCourseDescription() { return courseDescription; }
        public void setCourseDescription(String courseDescription) { this.courseDescription = courseDescription; }
        
        public String getCareerOptions() { return careerOptions; }
        public void setCareerOptions(String careerOptions) { this.careerOptions = careerOptions; }
        
        public String getUniversity() { return university; }
        public void setUniversity(String university) { this.university = university; }
        
        public String getProgramType() { return programType; }
        public void setProgramType(String programType) { this.programType = programType; }
        
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        
        public String getRequirements() { return requirements; }
        public void setRequirements(String requirements) { this.requirements = requirements; }
        
        public String getSalaryRange() { return salaryRange; }
        public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
        
        public String getJobOutlook() { return jobOutlook; }
        public void setJobOutlook(String jobOutlook) { this.jobOutlook = jobOutlook; }
        
        public String getSkillsNeeded() { return skillsNeeded; }
        public void setSkillsNeeded(String skillsNeeded) { this.skillsNeeded = skillsNeeded; }
        
        public String getWorkEnvironment() { return workEnvironment; }
        public void setWorkEnvironment(String workEnvironment) { this.workEnvironment = workEnvironment; }
        
        public Double getMatchScore() { return matchScore; }
        public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        // New field getters and setters
        public String getSuggestedCourses() { return suggestedCourses; }
        public void setSuggestedCourses(String suggestedCourses) { this.suggestedCourses = suggestedCourses; }
        
        public String getCareerSuggestions() { return careerSuggestions; }
        public void setCareerSuggestions(String careerSuggestions) { this.careerSuggestions = careerSuggestions; }
        
        public String getLearningStyle() { return learningStyle; }
        public void setLearningStyle(String learningStyle) { this.learningStyle = learningStyle; }
        
        public String getStudyTips() { return studyTips; }
        public void setStudyTips(String studyTips) { this.studyTips = studyTips; }
        
        public String getPersonalityGrowthTips() { return personalityGrowthTips; }
        public void setPersonalityGrowthTips(String personalityGrowthTips) { this.personalityGrowthTips = personalityGrowthTips; }
    }
    
    public static class CourseFilterOptionsDTO {
        private List<String> mbtiTypes;
        private List<String> riasecCodes;
        private List<String> categories;
        private List<String> universities;
        
        // Getters and Setters
        public List<String> getMbtiTypes() { return mbtiTypes; }
        public void setMbtiTypes(List<String> mbtiTypes) { this.mbtiTypes = mbtiTypes; }
        
        public List<String> getRiasecCodes() { return riasecCodes; }
        public void setRiasecCodes(List<String> riasecCodes) { this.riasecCodes = riasecCodes; }
        
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        
        public List<String> getUniversities() { return universities; }
        public void setUniversities(List<String> universities) { this.universities = universities; }
    }

    /**
     * Parse goal tags from student goals string
     */
    private List<String> parseGoalTags(String studentGoals) {
        if (studentGoals == null || studentGoals.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Simple parsing - split by common delimiters
        return Arrays.asList(studentGoals.split("[,;\\n]"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
