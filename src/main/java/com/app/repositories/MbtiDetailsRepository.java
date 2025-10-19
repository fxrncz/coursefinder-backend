package com.app.repositories;

import com.app.models.MbtiDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MbtiDetailsRepository extends JpaRepository<MbtiDetails, Long> {
    
    /**
     * Find detailed MBTI information by MBTI type
     */
    Optional<MbtiDetails> findByMbtiType(String mbtiType);
    
    /**
     * Check if MBTI details exist for a given type
     */
    boolean existsByMbtiType(String mbtiType);
}
