package com.app.repositories;

import com.app.models.CareerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CareerInfoRepository extends JpaRepository<CareerInfo, Long> {

    // Find by exact career name (case-insensitive)
    @Query(value = "SELECT * FROM career_info WHERE lower(career) = lower(:careerName) LIMIT 1", nativeQuery = true)
    Optional<CareerInfo> findByCareerNameExact(@Param("careerName") String careerName);

    // Find by partial career name match (case-insensitive)
    @Query(value = "SELECT * FROM career_info WHERE lower(career) LIKE lower(concat('%', :careerName, '%')) LIMIT 1", nativeQuery = true)
    Optional<CareerInfo> findByCareerNamePartial(@Param("careerName") String careerName);

    // Find multiple career infos by career names
    @Query(value = "SELECT * FROM career_info WHERE lower(career) IN :careerNames", nativeQuery = true)
    List<CareerInfo> findByCareerNamesExact(@Param("careerNames") List<String> careerNames);

    // Find career infos with partial matches for multiple career names
    @Query(value = "SELECT DISTINCT ci.* FROM career_info ci WHERE EXISTS (SELECT 1 FROM unnest(ARRAY[:careerNames]) AS career_name WHERE lower(ci.career) LIKE lower(concat('%', career_name, '%')))", nativeQuery = true)
    List<CareerInfo> findByCareerNamesPartial(@Param("careerNames") List<String> careerNames);
}
