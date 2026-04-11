package org.elearning.backend.enrollment;

import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.exception.CourseEnrollmentNotFoundException;
import org.elearning.backend.enrollment.exception.CourseHasNotBeenFinalizedException;
import org.elearning.backend.enrollment.exception.CourseMustBePublicException;
import org.elearning.backend.enrollment.exception.StudentAccessForbiddenException;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.service.CertificateGeneratorService;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateGeneratorServiceTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;

    private CertificateGeneratorService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new CertificateGeneratorService(
                courseEnrollmentRepository, courseRepository, userRepository);
    }

    @Test
    void generateCertificatePdf_enrollmentNotFound_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThrows(CourseEnrollmentNotFoundException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_wrongStudent_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(UUID.randomUUID()); // alt student
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(StudentAccessForbiddenException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_courseNotCompleted_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(null);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(CourseHasNotBeenFinalizedException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_privateCourse_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(LocalDateTime.now());
        when(enrollment.getCourseId()).thenReturn(courseId);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        Course course = mock(Course.class);
        when(course.getVisibility()).thenReturn(CourseVisibility.PRIVATE);
        when(course.getId()).thenReturn(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(CourseMustBePublicException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_success_returnsByteArray() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(LocalDateTime.now());
        when(enrollment.getCourseId()).thenReturn(courseId);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        Course course = mock(Course.class);
        when(course.getVisibility()).thenReturn(CourseVisibility.PUBLIC);
        when(course.getTitle()).thenReturn("Spring Boot Masterclass");
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        User student = mock(User.class);
        when(student.getFirstName()).thenReturn("Ion");
        when(student.getLastName()).thenReturn("Popescu");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        byte[] result = service.generateCertificatePdf(enrollmentId, studentId);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }
}