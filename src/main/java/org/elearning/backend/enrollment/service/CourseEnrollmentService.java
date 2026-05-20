package org.elearning.backend.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.exception.CourseEnrollmentNotFoundException;
import org.elearning.backend.enrollment.exception.CourseIsPrivateException;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.enrollment.exception.StudentAlreadyEnrolledInCourseException;
import org.elearning.backend.enrollment.mapper.EnrollmentMapper;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;

    private final ProgressCalculatorService progressCalculatorService;

    /**
     * Enrolls a student in a course if the course is public and published, and the student is not already enrolled.
     *
     * @param studentId the ID of the student to enroll
     * @param courseId  the ID of the course to enroll in
     * @return an EnrollmentDto containing details of the enrollment
     * @throws CourseNotFoundException if the course does not exist or is not published
     * @throws CourseIsPrivateException if the course is private
     * @throws StudentAlreadyEnrolledInCourseException if the student is already enrolled in the course
     */
    @Transactional
    public EnrollmentDto enrollStudentInCourse(UUID studentId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if(course.getVisibility() == CourseVisibility.PRIVATE) {
            throw new CourseIsPrivateException(courseId);
        }
        if(course.getStatus() != CourseStatus.PUBLISHED) {
            throw new CourseNotFoundException(courseId);
        }

        if(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new StudentAlreadyEnrolledInCourseException(studentId, courseId);
        }

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment = courseEnrollmentRepository.save(enrollment);

        EnrollmentDto enrollmentDto = enrollmentMapper.toEnrollmentDto(enrollment);
        enrollmentDto.setProgressPercent(BigDecimal.valueOf(0));

        return enrollmentDto;
    }

    /**
     * Unenrolls a student from a course if the enrollment exists.
     *
     * @param studentId the ID of the student to unenroll
     * @param courseId  the ID of the course to unenroll from
     * @throws CourseEnrollmentNotFoundException if the enrollment does not exist
     */
    @Transactional
    public void unenrollStudentFromCourse(UUID studentId, UUID courseId) {
        CourseEnrollment enrollment = courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new CourseEnrollmentNotFoundException(courseId));

        courseEnrollmentRepository.delete(enrollment);
    }

    /**
     * Retrieves a list of courses that a student is currently enrolled in, along with their progress.
     *
     * @param studentId the ID of the student
     * @return a list of EnrolledCourseDto containing details of the enrolled courses and progress
     */

    public Page<EnrolledCourseDto> getEnrolledCoursesForStudent(UUID studentId, Pageable pageable) {
        Page<CourseEnrollment> enrollments = courseEnrollmentRepository.findAllByStudentIdAndCourseStatus(
                studentId,
                CourseStatus.PUBLISHED,
                pageable
        );

        List<UUID> courseIds = enrollments.getContent().stream()
                .map(CourseEnrollment::getCourseId)
                .toList();

        Map<UUID, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(Course::getId, c -> c));

        return enrollments.map(enrollment -> {
            EnrolledCourseDto dto = enrollmentMapper.toEnrolledCourseDto(enrollment);

            Course course = courseMap.get(dto.getCourseId());
            if (course != null) {
                dto.setCourseTitle(course.getTitle());
                dto.setCourseCategory(course.getCategory());
            }

            dto.setProgressPercent(BigDecimal.valueOf(
                    progressCalculatorService.calculateProgressPercent(dto.getUnrollmentId())
            ));

            return dto;
        });
    }
}
