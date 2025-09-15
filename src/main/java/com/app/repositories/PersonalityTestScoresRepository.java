package com.app.repositories;

import com.app.models.PersonalityTestScores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonalityTestScoresRepository extends JpaRepository<PersonalityTestScores, Long> {
    
    /**
     * Find scoring data by test result ID
     */
    Optional<PersonalityTestScores> findByTestResultId(Long testResultId);
    
    /**
     * Find scoring data by session ID
     */
    Optional<PersonalityTestScores> findBySessionId(UUID sessionId);
    
    /**
     * Find scoring data by session ID string
     */
    @Query("SELECT p FROM PersonalityTestScores p WHERE p.sessionId = :sessionId")
    Optional<PersonalityTestScores> findBySessionIdString(@Param("sessionId") String sessionId);
    
    /**
     * Check if scoring data exists for a test result
     */
    boolean existsByTestResultId(Long testResultId);
    
    /**
     * Check if scoring data exists for a session
     */
    boolean existsBySessionId(UUID sessionId);
}
