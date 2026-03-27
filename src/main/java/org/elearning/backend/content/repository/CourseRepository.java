package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// JpaRepository da automat metodele: save(), findAll(), deleteById(), etc.
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByStatus(CourseStatus status);
    List<Course> findByCreatedBy(UUID createdBy);
}