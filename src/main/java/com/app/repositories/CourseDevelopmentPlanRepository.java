package com.app.repositories;

import com.app.models.CourseDevelopmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CourseDevelopmentPlanRepository extends JpaRepository<CourseDevelopmentPlan, Long> {

    // Find by exact course name (case-insensitive)
    @Query(value = "SELECT * FROM course_development_plan WHERE lower(course) = lower(:courseName) LIMIT 1", nativeQuery = true)
    Optional<CourseDevelopmentPlan> findByCourseNameExact(@Param("courseName") String courseName);

    // Find by partial course name match (case-insensitive)
    @Query(value = "SELECT * FROM course_development_plan WHERE lower(course) LIKE lower(concat('%', :courseName, '%')) LIMIT 1", nativeQuery = true)
    Optional<CourseDevelopmentPlan> findByCourseNamePartial(@Param("courseName") String courseName);

    // Find multiple development plans by course names
    @Query(value = "SELECT * FROM course_development_plan WHERE lower(course) IN :courseNames", nativeQuery = true)
    List<CourseDevelopmentPlan> findByCourseNamesExact(@Param("courseNames") List<String> courseNames);

    // Find development plans with partial matches for multiple course names
    @Query(value = "SELECT DISTINCT cdp.* FROM course_development_plan cdp WHERE EXISTS (SELECT 1 FROM unnest(ARRAY[:courseNames]) AS course_name WHERE lower(cdp.course) LIKE lower(concat('%', course_name, '%')))", nativeQuery = true)
    List<CourseDevelopmentPlan> findByCourseNamesPartial(@Param("courseNames") List<String> courseNames);
}

