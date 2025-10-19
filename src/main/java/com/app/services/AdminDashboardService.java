package com.app.services;

import com.app.models.TestResult;
import com.app.repositories.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    @Autowired
    private TestResultRepository testResultRepository;

    /**
     * Get comprehensive dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Get all test results
        List<TestResult> allResults = testResultRepository.findAll();
        
        // Total number of results
        stats.put("totalResults", allResults.size());
        
        // Students from PLMar count
        long plmarCount = allResults.stream()
            .filter(result -> result.getIsFromPLMar() != null && result.getIsFromPLMar())
            .count();
        stats.put("plmarStudents", plmarCount);
        stats.put("nonPlmarStudents", allResults.size() - plmarCount);
        
        // MBTI Type Distribution
        Map<String, Long> mbtiDistribution = allResults.stream()
            .filter(result -> result.getMbtiType() != null && !result.getMbtiType().isEmpty())
            .collect(Collectors.groupingBy(TestResult::getMbtiType, Collectors.counting()));
        stats.put("mbtiDistribution", mbtiDistribution);
        
        // RIASEC Code Distribution
        Map<String, Long> riasecDistribution = allResults.stream()
            .filter(result -> result.getRiasecCode() != null && !result.getRiasecCode().isEmpty())
            .collect(Collectors.groupingBy(TestResult::getRiasecCode, Collectors.counting()));
        stats.put("riasecDistribution", riasecDistribution);
        
        // Top MBTI Types (sorted by count, top 5)
        List<Map<String, Object>> topMbtiTypes = mbtiDistribution.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("type", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        stats.put("topMbtiTypes", topMbtiTypes);
        
        // Top RIASEC Codes (sorted by count, top 5)
        List<Map<String, Object>> topRiasecCodes = riasecDistribution.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("code", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        stats.put("topRiasecCodes", topRiasecCodes);
        
        // Most common courses
        Map<String, Long> courseFrequency = extractItemFrequency(allResults, "course");
        List<Map<String, Object>> topCourses = courseFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        stats.put("topCourses", topCourses);
        
        // Most common careers
        Map<String, Long> careerFrequency = extractItemFrequency(allResults, "career");
        List<Map<String, Object>> topCareers = careerFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        stats.put("topCareers", topCareers);
        
        // Demographics
        Map<String, Object> demographics = new HashMap<>();
        
        // Age distribution
        Map<String, Long> ageDistribution = allResults.stream()
            .filter(result -> result.getAge() != null)
            .collect(Collectors.groupingBy(
                result -> getAgeGroup(result.getAge()),
                Collectors.counting()
            ));
        demographics.put("ageGroups", ageDistribution);
        
        // Gender distribution
        Map<String, Long> genderDistribution = allResults.stream()
            .filter(result -> result.getGender() != null && !result.getGender().isEmpty())
            .collect(Collectors.groupingBy(TestResult::getGender, Collectors.counting()));
        demographics.put("genderDistribution", genderDistribution);
        
        stats.put("demographics", demographics);
        
        // Average age calculation
        List<TestResult> resultsWithAge = allResults.stream()
            .filter(result -> result.getAge() != null)
            .collect(Collectors.toList());
        
        if (!resultsWithAge.isEmpty()) {
            double avgAge = resultsWithAge.stream()
                .mapToInt(TestResult::getAge)
                .average()
                .orElse(0.0);
            stats.put("averageAge", Math.round(avgAge * 10.0) / 10.0);
            stats.put("totalWithAge", resultsWithAge.size());
        } else {
            stats.put("averageAge", 0.0);
            stats.put("totalWithAge", 0);
        }
        
        // Age distribution for chart (sorted data)
        List<Map<String, Object>> ageDistributionChart = new ArrayList<>();
        String[] ageGroups = {"Under 18", "18-22", "23-25", "26-30", "Above 30"};
        for (String group : ageGroups) {
            Map<String, Object> item = new HashMap<>();
            item.put("ageGroup", group);
            item.put("count", ageDistribution.getOrDefault(group, 0L));
            ageDistributionChart.add(item);
        }
        stats.put("ageDistributionChart", ageDistributionChart);
        
        // Gender distribution for chart
        List<Map<String, Object>> genderDistributionChart = genderDistribution.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("gender", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        stats.put("genderDistributionChart", genderDistributionChart);
        
        return stats;
    }
    
    /**
     * Extract frequency of courses or careers from test results
     */
    private Map<String, Long> extractItemFrequency(List<TestResult> results, String type) {
        Map<String, Long> frequency = new HashMap<>();
        
        for (TestResult result : results) {
            String data = type.equals("course") ? result.getCoursePath() : result.getCareerSuggestions();
            
            if (data == null || data.isEmpty()) {
                continue;
            }
            
            // Parse JSON-like data to extract course/career names
            // Assuming format contains "name": "Course Name" or "career": "Career Name"
            List<String> items = extractNames(data, type);
            
            for (String item : items) {
                frequency.put(item, frequency.getOrDefault(item, 0L) + 1);
            }
        }
        
        return frequency;
    }
    
    /**
     * Extract names from course/career string
     * Format: "Name: Description; Name: Description; ..."
     */
    private List<String> extractNames(String data, String type) {
        List<String> names = new ArrayList<>();
        
        try {
            if (data == null || data.trim().isEmpty()) {
                return names;
            }
            
            // Split by semicolon to get individual entries
            String[] entries = data.split(";");
            
            for (String entry : entries) {
                entry = entry.trim();
                if (entry.isEmpty()) {
                    continue;
                }
                
                // Extract the name part (before the colon)
                int colonIndex = entry.indexOf(":");
                if (colonIndex > 0) {
                    String name = entry.substring(0, colonIndex).trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing " + type + " data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return names;
    }
    
    /**
     * Categorize age into groups
     */
    private String getAgeGroup(Integer age) {
        if (age == null) return "Unknown";
        if (age < 18) return "Under 18";
        if (age <= 22) return "18-22";
        if (age <= 25) return "23-25";
        if (age <= 30) return "26-30";
        return "Above 30";
    }
}

