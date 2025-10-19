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
     * Find course recommendations by RIASEC codes (for top 2 RIASEC results)
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.riasecCode IN :riasecCodes ORDER BY m.id")
    List<MbtiRiasecMapping> findByRiasecCodesOrderById(@Param("riasecCodes") List<String> riasecCodes);
    
    /**
     * Find best matches by MBTI type and any of the RIASEC codes
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.mbtiType = :mbtiType AND m.riasecCode IN :riasecCodes ORDER BY m.id")
    List<MbtiRiasecMapping> findByMbtiTypeAndRiasecCodesOrderById(
            @Param("mbtiType") String mbtiType, 
            @Param("riasecCodes") List<String> riasecCodes);
    
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
     * Find top N recommendations for a specific MBTI and RIASEC combination
     */
    @Query("SELECT m FROM MbtiRiasecMapping m WHERE m.mbtiType = :mbtiType AND m.riasecCode IN :riasecCodes ORDER BY m.id LIMIT :limit")
    List<MbtiRiasecMapping> findTopRecommendations(
            @Param("mbtiType") String mbtiType, 
            @Param("riasecCodes") List<String> riasecCodes, 
            @Param("limit") int limit);
    
    /**
     * Search courses inside courses array (case-insensitive). Uses Postgres UNNEST.
     */
    @Query(value = "SELECT * FROM mbti_riasec_matching m WHERE EXISTS (SELECT 1 FROM unnest(m.courses) c WHERE lower(c) LIKE lower(concat('%', :courseName, '%')))", nativeQuery = true)
    List<MbtiRiasecMapping> findBySuggestedCoursesContainingIgnoreCase(@Param("courseName") String courseName);

    // Native queries to fetch arrays without hydrating the whole entity (avoids ARRAY mapping issues)
    @Query(value = "SELECT courses, careers, explanation FROM mbti_riasec_matching WHERE mbti_type = :mbti AND riasec_code = :riasec ORDER BY id LIMIT 1", nativeQuery = true)
    List<Object[]> fetchArraysForExact(@Param("mbti") String mbti, @Param("riasec") String riasec);

    @Query(value = "SELECT courses, careers, explanation FROM mbti_riasec_matching WHERE mbti_type = :mbti ORDER BY id LIMIT 1", nativeQuery = true)
    List<Object[]> fetchAllByMbti(@Param("mbti") String mbti);

    @Query(value = "SELECT courses, careers, explanation FROM mbti_riasec_matching WHERE riasec_code = :riasec ORDER BY id LIMIT 1", nativeQuery = true)
    List<Object[]> fetchAllByRiasec(@Param("riasec") String riasec);

    @Query(value = "SELECT explanation FROM mbti_riasec_matching WHERE mbti_type = :mbti AND riasec_code = :riasec ORDER BY id LIMIT 1", nativeQuery = true)
    List<Object[]> fetchExplanationExact(@Param("mbti") String mbti, @Param("riasec") String riasec);
    
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
    
    /**
     * Get detailed statistics about the mappings
     */
    @Query("SELECT COUNT(DISTINCT m.mbtiType) as mbtiCount, COUNT(DISTINCT m.riasecCode) as riasecCount, COUNT(m) as totalRecords FROM MbtiRiasecMapping m")
    Object[] getDetailedStatistics();
}
