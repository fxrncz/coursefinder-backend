package com.app.repositories;

import com.app.models.CourseDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDescriptionRepository extends JpaRepository<CourseDescription, Long> {
    Optional<CourseDescription> findByCourseName(String courseName);
    
    // Fallbacks for name mismatches
    Optional<CourseDescription> findFirstByCourseNameIgnoreCase(String courseName);
    Optional<CourseDescription> findByCourseNameContainingIgnoreCase(String keyword);

    // Native lookups for either possible column names (course_name vs course)
    @Query(value = "SELECT description FROM updated_course_description WHERE lower(course_name) = lower(:name) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCourseNameExact(@Param("name") String name);

    @Query(value = "SELECT description FROM updated_course_description WHERE lower(course) = lower(:name) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCourseExact(@Param("name") String name);

    @Query(value = "SELECT description FROM updated_course_description WHERE lower(course_name) LIKE lower(concat('%', :keyword, '%')) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCourseNamePartial(@Param("keyword") String keyword);

    @Query(value = "SELECT description FROM updated_course_description WHERE lower(course) LIKE lower(concat('%', :keyword, '%')) ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<String> findDescriptionByCoursePartial(@Param("keyword") String keyword);
}


