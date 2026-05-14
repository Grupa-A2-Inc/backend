package org.elearning.backend.analytics;

import org.elearning.backend.analytics.dto.alerts.AlertDTO;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.analytics.repository.AnalyticsAlertRepository;
import org.elearning.backend.analytics.service.FailureRateService;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailureRateServiceTest {

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private AnalyticsAlertRepository analyticsAlertRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    private FailureRateService service;
    private UUID professorId;
    private UUID otherProfessorId;
    private UUID testId;
    private UUID lessonId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        service = new FailureRateService(
                testResultRepository,
                testRepository,
                analyticsAlertRepository,
                courseRepository,
                lessonRepository
        );
        professorId = UUID.randomUUID();
        otherProfessorId = UUID.randomUUID();
        testId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        courseId = UUID.randomUUID();
    }

    @Test
    void getTestFailureRateThrowsWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTestFailureRate(testId, professorId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessage("Test with id " + testId + " does not exist");
    }

    @Test
    void getTestFailureRateThrowsWhenProfessorDoesNotOwnTest() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(testOwnedBy(otherProfessorId)));

        assertThatThrownBy(() -> service.getTestFailureRate(testId, professorId))
                .isInstanceOf(WithoutAccessException.class)
                .hasMessage("User " + professorId + " has no access to this field");
    }

    @Test
    void getLessonFailureRateThrowsWhenProfessorDoesNotOwnLessonTest() {
        when(testRepository.findByLessonId(lessonId)).thenReturn(Optional.of(testOwnedBy(otherProfessorId)));

        assertThatThrownBy(() -> service.getLessonFailureRate(lessonId, professorId))
                .isInstanceOf(WithoutAccessException.class)
                .hasMessage("User " + professorId + " has no access to this field");
    }

    @Test
    void createOrUpdateAlertThrowsWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrUpdateAlert(testId, professorId, BigDecimal.TEN))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessage("Test with id " + testId + " does not exist");
    }

    @Test
    void createOrUpdateAlertThrowsWhenProfessorDoesNotOwnTest() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(testOwnedBy(otherProfessorId)));

        assertThatThrownBy(() -> service.createOrUpdateAlert(testId, professorId, BigDecimal.TEN))
                .isInstanceOf(WithoutAccessException.class)
                .hasMessage("User " + professorId + " has no access to this field");
    }

    @Test
    void createOrUpdateAlertReturnsNullWhenUpsertDoesNotExposeActiveAlert() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(testOwnedBy(professorId)));
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(Optional.empty());

        AlertDTO result = service.createOrUpdateAlert(testId, professorId, BigDecimal.valueOf(70));

        assertThat(result).isNull();
        verify(analyticsAlertRepository).upsertAlertThreshold(testId, professorId, BigDecimal.valueOf(70));
    }

    @Test
    void getAlertsThrowsWhenRoleIsNotTeacher() {
        assertThatThrownBy(() -> service.getAlerts(professorId, RoleName.STUDENT))
                .isInstanceOf(WithoutAccessException.class)
                .hasMessage("User " + professorId + " has no access to this field");

        verifyNoInteractions(analyticsAlertRepository);
    }

    @Test
    void getFailureChartsThrowsWhenCourseDoesNotExist() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFailureCharts(courseId, professorId))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found with ID: " + courseId);
    }

    @Test
    void getFailureChartsThrowsWhenProfessorDoesNotOwnCourse() {
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(otherProfessorId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.getFailureCharts(courseId, professorId))
                .isInstanceOf(WithoutAccessException.class)
                .hasMessage("User " + professorId + " has no access to this field");
    }

    @Test
    void getFailureChartsReturnsEmptyListWhenCourseHasNoLessons() {
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(professorId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.findAllLessonIdsByCourseId(courseId)).thenReturn(List.of());

        assertThat(service.getFailureCharts(courseId, professorId)).isEmpty();
    }

    private org.elearning.backend.assessment.model.Test testOwnedBy(UUID ownerId) {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setLessonId(lessonId);
        test.setCreatedBy(ownerId);
        return test;
    }
}
