package com.app.repositories;

import com.app.models.CareerDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerDescriptionRepository extends JpaRepository<CareerDescription, Long> {
    Optional<CareerDescription> findByCareerName(String careerName);

    // Fallbacks for name mismatches
    Optional<CareerDescription> findFirstByCareerNameIgnoreCase(String careerName);

    Optional<CareerDescription> findFirstByCareerNameContainingIgnoreCase(String keyword);

    // Native lookups for either possible column names (career_name vs career)
    @Query(value = "SELECT description FROM updated_career_description WHERE lower(career_name) = lower(:name) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCareerNameExact(@Param("name") String name);

    @Query(value = "SELECT description FROM updated_career_description WHERE lower(career) = lower(:name) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCareerExact(@Param("name") String name);

    @Query(value = "SELECT description FROM updated_career_description WHERE lower(career_name) LIKE lower(concat('%', :keyword, '%')) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCareerNamePartial(@Param("keyword") String keyword);

    @Query(value = "SELECT description FROM updated_career_description WHERE lower(career) LIKE lower(concat('%', :keyword, '%')) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCareerPartial(@Param("keyword") String keyword);
}


