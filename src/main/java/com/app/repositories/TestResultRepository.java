package com.app.repositories;

import com.app.models.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    
    /**
     * Find test results by user ID
     */
    List<TestResult> findByUserIdOrderByGeneratedAtDesc(Long userId);
    
    /**
     * Find the most recent test result for a user
     */
    Optional<TestResult> findTopByUserIdOrderByGeneratedAtDesc(Long userId);
    
    /**
     * Find test results by guest token
     */
    List<TestResult> findByGuestTokenOrderByGeneratedAtDesc(UUID guestToken);
    
    /**
     * Find the most recent test result for a guest
     */
    Optional<TestResult> findTopByGuestTokenOrderByGeneratedAtDesc(UUID guestToken);
    
    /**
     * Find test result by session ID
     */
    Optional<TestResult> findBySessionId(UUID sessionId);
    
    /**
     * Check if user has taken the test
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Check if guest has taken the test
     */
    boolean existsByGuestToken(UUID guestToken);
    
    /**
     * Find test results by MBTI type
     */
    List<TestResult> findByMbtiType(String mbtiType);
    
    /**
     * Find test results by RIASEC code
     */
    List<TestResult> findByRiasecCode(String riasecCode);
    
    /**
     * Get statistics for MBTI types
     */
    @Query("SELECT t.mbtiType, COUNT(t) FROM TestResult t WHERE t.mbtiType IS NOT NULL GROUP BY t.mbtiType ORDER BY COUNT(t) DESC")
    List<Object[]> getMbtiTypeStatistics();
    
    /**
     * Get statistics for RIASEC codes
     */
    @Query("SELECT t.riasecCode, COUNT(t) FROM TestResult t WHERE t.riasecCode IS NOT NULL GROUP BY t.riasecCode ORDER BY COUNT(t) DESC")
    List<Object[]> getRiasecCodeStatistics();
    
    /**
     * Count total test results
     */
    @Query("SELECT COUNT(t) FROM TestResult t")
    long countTotalResults();
    
    /**
     * Count test results by users (not guests)
     */
    @Query("SELECT COUNT(t) FROM TestResult t WHERE t.userId IS NOT NULL")
    long countUserResults();
    
    /**
     * Count test results by guests
     */
    @Query("SELECT COUNT(t) FROM TestResult t WHERE t.guestToken IS NOT NULL")
    long countGuestResults();
    
    /**
     * Find recent test results (last 30 days)
     */
    @Query("SELECT t FROM TestResult t WHERE t.generatedAt >= :cutoffDate ORDER BY t.generatedAt DESC")
    List<TestResult> findRecentResults(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * Delete old guest results (older than specified days)
     */
    @Modifying
    @Query("DELETE FROM TestResult t WHERE t.guestToken IS NOT NULL AND t.generatedAt < :cutoffDate")
    void deleteOldGuestResults(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

  /**
   * Delete all test results for a specific user
   */
  @Modifying
  @Query("DELETE FROM TestResult t WHERE t.userId = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
