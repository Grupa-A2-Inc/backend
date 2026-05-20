package org.elearning.backend.enrollment.repository;

import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<CourseEnrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<CourseEnrollment> findAllByStudentId(UUID studentId);
    Page<CourseEnrollment> findAllByStudentId(UUID studentId, Pageable pageable);
    Page<CourseEnrollment> findAllByCourseId(UUID courseId, Pageable pageable);

    @Query(
            value = """
                    SELECT ce
                    FROM CourseEnrollment ce
                    JOIN Course c ON c.id = ce.courseId
                    WHERE ce.studentId = :studentId
                      AND c.status = :status
                    """,
            countQuery = """
                    SELECT COUNT(ce)
                    FROM CourseEnrollment ce
                    JOIN Course c ON c.id = ce.courseId
                    WHERE ce.studentId = :studentId
                      AND c.status = :status
                    """
    )
    Page<CourseEnrollment> findAllByStudentIdAndCourseStatus(
            @Param("studentId") UUID studentId,
            @Param("status") CourseStatus status,
            Pageable pageable
    );

    List<CourseEnrollment> findAllByStudentIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(UUID studentId);
}
