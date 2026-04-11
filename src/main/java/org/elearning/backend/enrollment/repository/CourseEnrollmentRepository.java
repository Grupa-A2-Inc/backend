package org.elearning.backend.enrollment.repository;

import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<CourseEnrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<CourseEnrollment> findAllByStudentId(UUID studentId);

    Page<CourseEnrollment> findAllByCourseId(UUID courseId, Pageable pageable);
}
