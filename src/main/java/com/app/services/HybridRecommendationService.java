package com.app.services;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.app.repositories.RecommendationRepository;
import com.app.repositories.RankedCourseProjection;
import com.app.repositories.RankedCareerProjection;

@Service
public class HybridRecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepository;

    public List<String> getTopCourseNames(String mbtiCode, String riasecCode, int limit) {
        try {
            return recommendationRepository.findTopCourses(mbtiCode, riasecCode, limit)
                .stream()
                .map(RankedCourseProjection::getName)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error and return empty list to allow fallback to legacy system
            System.err.println("Error getting top course names from hybrid service: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> getTopCareerNames(String mbtiCode, String riasecCode, int limit) {
        try {
            return recommendationRepository.findTopCareers(mbtiCode, riasecCode, limit)
                .stream()
                .map(RankedCareerProjection::getName)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error and return empty list to allow fallback to legacy system
            System.err.println("Error getting top career names from hybrid service: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}


