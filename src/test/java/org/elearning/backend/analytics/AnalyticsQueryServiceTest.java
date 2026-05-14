package org.elearning.backend.analytics;

import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.analytics.service.AnalyticsQueryService;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private TestRepository testRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TestResultRepository testResultRepository;

    @InjectMocks
    private AnalyticsQueryService service;

    private UUID professorId;

    @BeforeEach
    void setUp() {
        professorId = UUID.randomUUID();
    }

    @Test
    void getClassAverageThrowsWhenProfessorDoesNotOwnTest() {
        UUID testId = UUID.randomUUID();
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setCreatedBy(UUID.randomUUID());

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> service.getClassAverage(testId, professorId))
                .isInstanceOf(WithoutAccessException.class);

        verifyNoInteractions(testResultRepository);
    }

    @Test
    void getClassAverageThrowsWhenTestMissing() {
        UUID testId = UUID.randomUUID();
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClassAverage(testId, professorId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessage("Test does not exist");
    }

    @Test
    void getStudentAveragesThrowsWhenProfessorDoesNotOwnCourse() {
        UUID courseId = UUID.randomUUID();
        var pageRequest = PageRequest.of(0, 5);
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(UUID.randomUUID());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.getStudentAverages(courseId, professorId, pageRequest))
                .isInstanceOf(WithoutAccessException.class);

        verifyNoInteractions(testResultRepository);
    }

    @Test
    void getStudentAveragesThrowsWhenCourseMissing() {
        UUID courseId = UUID.randomUUID();
        var pageRequest = PageRequest.of(0, 5);
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentAverages(courseId, professorId, pageRequest))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessage("Course does not exist");
    }
}
