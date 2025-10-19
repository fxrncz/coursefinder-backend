package com.app.repositories;

import com.app.models.DevelopmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DevelopmentPlanRepository extends JpaRepository<DevelopmentPlan, Long> {

    // Find by exact career name (case-insensitive)
    @Query(value = "SELECT * FROM development_plan WHERE lower(career) = lower(:careerName) LIMIT 1", nativeQuery = true)
    Optional<DevelopmentPlan> findByCareerNameExact(@Param("careerName") String careerName);

    // Find by partial career name match (case-insensitive)
    @Query(value = "SELECT * FROM development_plan WHERE lower(career) LIKE lower(concat('%', :careerName, '%')) LIMIT 1", nativeQuery = true)
    Optional<DevelopmentPlan> findByCareerNamePartial(@Param("careerName") String careerName);

    // Find multiple development plans by career names
    @Query(value = "SELECT * FROM development_plan WHERE lower(career) IN :careerNames", nativeQuery = true)
    List<DevelopmentPlan> findByCareerNamesExact(@Param("careerNames") List<String> careerNames);

    // Find development plans with partial matches for multiple career names
    @Query(value = "SELECT DISTINCT dp.* FROM development_plan dp WHERE EXISTS (SELECT 1 FROM unnest(ARRAY[:careerNames]) AS career_name WHERE lower(dp.career) LIKE lower(concat('%', career_name, '%')))", nativeQuery = true)
    List<DevelopmentPlan> findByCareerNamesPartial(@Param("careerNames") List<String> careerNames);
}
