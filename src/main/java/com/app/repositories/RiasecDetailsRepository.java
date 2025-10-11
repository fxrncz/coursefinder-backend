package com.app.repositories;

import com.app.models.RiasecDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiasecDetailsRepository extends JpaRepository<RiasecDetails, Long> {
    
    /**
     * Find detailed RIASEC information by RIASEC type
     */
    Optional<RiasecDetails> findByRiasecType(String riasecType);
    
    /**
     * Check if RIASEC details exist for a given type
     */
    boolean existsByRiasecType(String riasecType);
}

