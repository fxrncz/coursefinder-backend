package com.app.repositories;

import com.app.models.CareerDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerDescriptionRepository extends JpaRepository<CareerDescription, Long> {
    Optional<CareerDescription> findByCareerName(String careerName);

    // Fallbacks for name mismatches
    Optional<CareerDescription> findFirstByCareerNameIgnoreCase(String careerName);

    Optional<CareerDescription> findFirstByCareerNameContainingIgnoreCase(String keyword);
}


