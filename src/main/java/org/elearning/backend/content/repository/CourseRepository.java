package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.elearning.backend.content.model.*;
import java.util.List;

// JpaRepository da automat metodele: save(), findAll(), deleteById(), etc.
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByCreatedBy(UUID createdBy);
    List<Course> findByStatusAndVisibility(CourseStatus status, CourseVisibility visibility);
}