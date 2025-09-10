package com.app.repositories;

import com.app.models.CourseDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDescriptionRepository extends JpaRepository<CourseDescription, Long> {
    Optional<CourseDescription> findByCourseName(String courseName);
}


