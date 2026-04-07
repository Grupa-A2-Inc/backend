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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;

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

    public void unenrollStudentFromCourse(UUID studentId, UUID courseId) {
        CourseEnrollment enrollment = courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new CourseEnrollmentNotFoundException(courseId));

        courseEnrollmentRepository.delete(enrollment);
    }

    public List<EnrolledCourseDto> getEnrolledCoursesForStudent(UUID studentId) {
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findAllByStudentId(studentId);
        List<EnrolledCourseDto> enrolledCourseDtos = enrollmentMapper.toEnrolledCourseDtos(enrollments);

        for(EnrolledCourseDto dto : enrolledCourseDtos) {
            Course course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new CourseNotFoundException(dto.getCourseId()));
            dto.setCourseTitle(course.getTitle());
            dto.setCourseCategory(course.getCategory());

            //Aici trebuie inlocuit cu functia de calculare a progresului
            dto.setProgressPercent(BigDecimal.valueOf(0));
        }

        return enrolledCourseDtos;
    }
}
