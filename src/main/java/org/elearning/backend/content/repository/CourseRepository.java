package org.elearning.backend.content.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.elearning.backend.content.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.elearning.backend.content.model.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

// JpaRepository da automat metodele: save(), findAll(), deleteById(), etc.
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByCreatedBy(UUID createdBy);
    List<Course> findByCreatedByIn(Collection<UUID> createdBy);
    List<Course> findByStatusAndVisibility(CourseStatus status, CourseVisibility visibility);

    Page<Course> findByStatusAndVisibility(CourseStatus status, CourseVisibility visibility, Pageable pageable);

    Page<Course> findByCreatedBy(UUID createdBy, Pageable pageable);
    /**
     * Retrieves a Course entity along with its associated chapters based on the provided course ID.
     * @param courseId The UUID of the course to retrieve.
     * @return An Optional containing the Course entity with its chapters if found, or an empty Optional if not found.
     */
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.chapters WHERE c.id = :courseId")
    Optional<Course> findCourseWithChapters(@Param("courseId") UUID courseId);
}
