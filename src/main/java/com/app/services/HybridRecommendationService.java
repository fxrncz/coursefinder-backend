package com.app.services;

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
        return recommendationRepository.findTopCourses(mbtiCode, riasecCode, limit)
            .stream()
            .map(RankedCourseProjection::getName)
            .collect(Collectors.toList());
    }

    public List<String> getTopCareerNames(String mbtiCode, String riasecCode, int limit) {
        return recommendationRepository.findTopCareers(mbtiCode, riasecCode, limit)
            .stream()
            .map(RankedCareerProjection::getName)
            .collect(Collectors.toList());
    }
}


