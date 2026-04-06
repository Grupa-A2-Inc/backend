package org.elearning.backend.enrollment.repository;

import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
