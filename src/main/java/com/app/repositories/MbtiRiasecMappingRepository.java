package com.app.repositories;

import com.app.models.MbtiRiasecMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MbtiRiasecMappingRepository extends JpaRepository<MbtiRiasecMapping, Long> {
    
    /**
     * Find course recommendations by MBTI type
     */
    List<MbtiRiasecMapping> findByMbtiType(String mbtiType);
    
    /**
     * Find course recommendations by RIASEC code
     */
    List<MbtiRiasecMapping> findByRiasecCode(String riasecCode);
    
    /**
     * Find course recommendations by both MBTI type and RIASEC code
     */
    List<MbtiRiasecMapping> findByMbtiTypeAndRiasecCode(String mbtiType, String riasecCode);
    
    /**
     * Find course recommendations by MBTI type and RIASEC code, ordered by match score
     */
    List<MbtiRiasecMapping> findByMbtiTypeAndRiasecCodeOrderByMatchScoreDesc(String mbtiType, String riasecCode);
    
    /**
     * Find course recommendations by MBTI type, ordered by match score
     */
    List<MbtiRiasecMapping> findByMbtiTypeOrderByMatchScoreDesc(String mbtiType);
    
    /**
     * Find course recommendations by RIASEC code, ordered by match score
     */
    List<MbtiRiasecMapping> findByRiasecCodeOrderByMatchScoreDesc(String riasecCode);
    
    /**
     * Find course recommendations by RIASEC codes (for top 2 RIASEC results)
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.riasecCode IN :riasecCodes ORDER BY m.matchScore DESC")
    List<MbtiRiasecMapping> findByRiasecCodesOrderByMatchScore(@Param("riasecCodes") List<String> riasecCodes);
    
    /**
     * Find best matches by MBTI type and any of the RIASEC codes
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.mbtiType = :mbtiType AND m.riasecCode IN :riasecCodes ORDER BY m.matchScore DESC")
    List<MbtiRiasecMapping> findByMbtiTypeAndRiasecCodesOrderByMatchScore(
            @Param("mbtiType") String mbtiType, 
            @Param("riasecCodes") List<String> riasecCodes);
    
    /**
     * Find recommendations by category
     */
    List<MbtiRiasecMapping> findByCategory(String category);
    
    /**
     * Find recommendations by university
     */
    List<MbtiRiasecMapping> findByUniversity(String university);
    
    /**
     * Find recommendations by program type
     */
    List<MbtiRiasecMapping> findByProgramType(String programType);
    
    /**
     * Get all unique MBTI types in the database
     */
    @Query("SELECT DISTINCT m.mbtiType FROM MbtiRiasecMapping m ORDER BY m.mbtiType")
    List<String> findAllUniqueMbtiTypes();
    
    /**
     * Get all unique RIASEC codes in the database
     */
    @Query("SELECT DISTINCT m.riasecCode FROM MbtiRiasecMapping m ORDER BY m.riasecCode")
    List<String> findAllUniqueRiasecCodes();
    
    /**
     * Get all unique categories
     */
    @Query("SELECT DISTINCT m.category FROM MbtiRiasecMapping m WHERE m.category IS NOT NULL ORDER BY m.category")
    List<String> findAllUniqueCategories();
    
    /**
     * Get all unique universities
     */
    @Query("SELECT DISTINCT m.university FROM MbtiRiasecMapping m WHERE m.university IS NOT NULL ORDER BY m.university")
    List<String> findAllUniqueUniversities();
    
    /**
     * Find top N recommendations by match score for a specific MBTI and RIASEC combination
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.mbtiType = :mbtiType AND m.riasecCode IN :riasecCodes ORDER BY m.matchScore DESC LIMIT :limit")
    List<MbtiRiasecMapping> findTopRecommendations(
            @Param("mbtiType") String mbtiType, 
            @Param("riasecCodes") List<String> riasecCodes, 
            @Param("limit") int limit);
    
    /**
     * Search courses by name (case-insensitive)
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE LOWER(m.courseName) LIKE LOWER(CONCAT('%', :courseName, '%'))")
    List<MbtiRiasecMapping> findByCourseNameContainingIgnoreCase(@Param("courseName") String courseName);
    
    /**
     * Get statistics about mappings
     */
    @Query("SELECT COUNT(m) FROM MbtiRiasecMapping m")
    long countTotalMappings();
    
    /**
     * Get count by MBTI type
     */
    @Query("SELECT m.mbtiType, COUNT(m) FROM MbtiRiasecMapping m GROUP BY m.mbtiType ORDER BY COUNT(m) DESC")
    List<Object[]> countByMbtiType();
    
    /**
     * Get count by RIASEC code
     */
    @Query("SELECT m.riasecCode, COUNT(m) FROM MbtiRiasecMapping m GROUP BY m.riasecCode ORDER BY COUNT(m) DESC")
    List<Object[]> countByRiasecCode();
}
