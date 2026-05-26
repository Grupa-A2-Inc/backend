package org.elearning.backend.enrollment;

import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.exception.StudentAccessForbiddenException;
import org.elearning.backend.enrollment.mapper.EnrollmentMapper;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.service.CourseEnrollmentService;
import org.elearning.backend.enrollment.service.ProgressCalculatorService;
import org.elearning.backend.content.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private ClassroomCourseRepository classroomCourseRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private ProgressCalculatorService progressCalculatorService;

    @InjectMocks
    private CourseEnrollmentService service;

    private UUID studentId;
    private UUID enrollmentId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        enrollmentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
    }

    @Test
    void getEnrolledCoursesForStudentReturnsMappedDtosWithCourseDataAndProgress() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);

        EnrolledCourseDto dto = new EnrolledCourseDto();
        dto.setUnrollmentId(enrollmentId);
        dto.setCourseId(courseId);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Algorithms");
        course.setCategory("CS");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<CourseEnrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        when(courseEnrollmentRepository.findAllByStudentIdAndCourseStatus(studentId, CourseStatus.PUBLISHED, pageable)).thenReturn(page);
        when(courseRepository.findAllById(List.of(courseId))).thenReturn(List.of(course));
        when(enrollmentMapper.toEnrolledCourseDto(enrollment)).thenReturn(dto);
        when(progressCalculatorService.calculateProgressPercent(enrollmentId)).thenReturn(75.0);

        Page<EnrolledCourseDto> result = service.getEnrolledCoursesForStudent(studentId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseTitle()).isEqualTo("Algorithms");
        assertThat(result.getContent().get(0).getCourseCategory()).isEqualTo("CS");
        assertThat(result.getContent().get(0).getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(75.0));
    }

    @Test
    void getEnrolledCoursesForStudentThrowsWhenMappedCourseCannotBeFound() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        EnrolledCourseDto dto = new EnrolledCourseDto();
        dto.setCourseId(courseId);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<CourseEnrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        when(courseEnrollmentRepository.findAllByStudentIdAndCourseStatus(studentId, CourseStatus.PUBLISHED, pageable)).thenReturn(page);
        when(courseRepository.findAllById(List.of(courseId))).thenReturn(List.of());
        when(enrollmentMapper.toEnrolledCourseDto(enrollment)).thenReturn(dto);
        when(progressCalculatorService.calculateProgressPercent(dto.getUnrollmentId())).thenReturn(0.0);

        Page<EnrolledCourseDto> result = service.getEnrolledCoursesForStudent(studentId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseTitle()).isNull();
        assertThat(result.getContent().get(0).getCourseCategory()).isNull();
    }

    @Test
    void getEnrolledCoursesForStudentPageLeavesCourseFieldsNullWhenCourseMissingFromBulkLookup() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setEnrolledAt(LocalDateTime.now());

        EnrolledCourseDto dto = new EnrolledCourseDto();
        dto.setUnrollmentId(enrollmentId);
        dto.setCourseId(courseId);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<CourseEnrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        when(courseEnrollmentRepository.findAllByStudentIdAndCourseStatus(studentId, CourseStatus.PUBLISHED, pageable)).thenReturn(page);
        when(courseRepository.findAllById(List.of(courseId))).thenReturn(List.of());
        when(enrollmentMapper.toEnrolledCourseDto(enrollment)).thenReturn(dto);
        when(progressCalculatorService.calculateProgressPercent(enrollmentId)).thenReturn(12.5);

        Page<EnrolledCourseDto> result = service.getEnrolledCoursesForStudent(studentId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseTitle()).isNull();
        assertThat(result.getContent().get(0).getCourseCategory()).isNull();
        assertThat(result.getContent().get(0).getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(12.5));
        verify(enrollmentMapper).toEnrolledCourseDto(enrollment);
    }

    @Test
    void getEnrolledCoursesForStudentUsesOnlyPublishedCourses() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(courseEnrollmentRepository.findAllByStudentIdAndCourseStatus(studentId, CourseStatus.PUBLISHED, pageable))
                .thenReturn(Page.empty(pageable));

        Page<EnrolledCourseDto> result = service.getEnrolledCoursesForStudent(studentId, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(courseEnrollmentRepository).findAllByStudentIdAndCourseStatus(studentId, CourseStatus.PUBLISHED, pageable);
    }

    @Test
    void unenrollStudentFromCourseDeletesEnrollmentWhenCourseIsNotAssignedThroughClassroom() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);

        when(courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(java.util.Optional.of(enrollment));
        when(classroomCourseRepository.existsCourseAssignedToUserThroughAnyClassroom(
                studentId,
                MembershipType.STUDENT,
                courseId
        )).thenReturn(false);

        service.unenrollStudentFromCourse(studentId, courseId);

        verify(courseEnrollmentRepository).delete(enrollment);
    }

    @Test
    void unenrollStudentFromCourseThrowsWhenCourseIsAssignedThroughClassroom() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);

        when(courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(java.util.Optional.of(enrollment));
        when(classroomCourseRepository.existsCourseAssignedToUserThroughAnyClassroom(
                studentId,
                MembershipType.STUDENT,
                courseId
        )).thenReturn(true);

        StudentAccessForbiddenException exception = assertThrows(
                StudentAccessForbiddenException.class,
                () -> service.unenrollStudentFromCourse(studentId, courseId)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Students cannot unenroll themselves from courses assigned through a classroom");
        verify(courseEnrollmentRepository, never()).delete(enrollment);
    }
}
