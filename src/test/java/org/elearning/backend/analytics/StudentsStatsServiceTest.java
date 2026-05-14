package org.elearning.backend.analytics;

import org.elearning.backend.analytics.dto.statistics.entity.CourseDetailsDto;
import org.elearning.backend.analytics.dto.statistics.entity.CourseStatsDto;
import org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto;
import org.elearning.backend.analytics.dto.statistics.student.MyClassTestAverageDto;
import org.elearning.backend.analytics.dto.statistics.student.MyClassTestBestResultsDto;
import org.elearning.backend.analytics.dto.statistics.student.MyPersonalTestStatsDto;
import org.elearning.backend.analytics.dto.statistics.student.MySummaryDataDto;
import org.elearning.backend.analytics.dto.statistics.student.MyTestStatsDto;
import org.elearning.backend.analytics.exception.StudentNotEnrolledInCourseException;
import org.elearning.backend.analytics.repository.LessonDifficultyByStudentRepository;
import org.elearning.backend.analytics.service.StudentsStatsService;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptDetailsDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentsStatsServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonDifficultyByStudentRepository lessonDifficultyByStudentRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private StudentsStatsService studentsStatsService;

    private UUID studentId;
    private UUID testId;
    private UUID courseId;
    private org.elearning.backend.assessment.model.Test test;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        testId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        Course course = new Course();
        course.setId(courseId);

        Chapter chapter = new Chapter();
        chapter.setCourse(course);

        lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setChapter(chapter);

        test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setLessonId(lesson.getId());
    }

    @Test
    void getMyTestStats_shouldReturnZeroMedianAndPercentileWhenClassHasNoResults() {
        TestResult latestResult = new TestResult();
        latestResult.setScorePercent(BigDecimal.valueOf(72));

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);
        when(testResultRepository.getMyPersonalTestStats(studentId, test)).thenReturn(
                new MyPersonalTestStatsDto(testId, "Midterm", 1, BigDecimal.valueOf(72), BigDecimal.valueOf(72), 72.0)
        );
        when(testResultRepository.findTopByStudentIdAndTestOrderByCompletedAtDesc(studentId, test)).thenReturn(latestResult);
        when(testResultRepository.getMyClassAverageStats(test)).thenReturn(new MyClassTestAverageDto(0.0, 0));
        when(testResultRepository.getAllTestsOrderByBestScoreDesc(test)).thenReturn(List.of());

        MyTestStatsDto result = studentsStatsService.getMyTestStats(studentId, testId);

        assertThat(result.getClassMedian()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getRank()).isEqualTo(1);
        assertThat(result.getPercentile()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getMyTestStats_shouldReturnRankAfterScanningWholeListWhenStudentIsMissing() {
        UUID otherStudentOne = UUID.randomUUID();
        UUID otherStudentTwo = UUID.randomUUID();
        TestResult latestResult = new TestResult();
        latestResult.setScorePercent(BigDecimal.valueOf(55));

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);
        when(testResultRepository.getMyPersonalTestStats(studentId, test)).thenReturn(
                new MyPersonalTestStatsDto(testId, "Midterm", 2, BigDecimal.valueOf(60), BigDecimal.valueOf(50), 55.0)
        );
        when(testResultRepository.findTopByStudentIdAndTestOrderByCompletedAtDesc(studentId, test)).thenReturn(latestResult);
        when(testResultRepository.getMyClassAverageStats(test)).thenReturn(new MyClassTestAverageDto(67.5, 2));
        when(testResultRepository.getAllTestsOrderByBestScoreDesc(test)).thenReturn(List.of(
                new MyClassTestBestResultsDto(otherStudentOne, BigDecimal.valueOf(80)),
                new MyClassTestBestResultsDto(otherStudentTwo, BigDecimal.valueOf(55))
        ));

        MyTestStatsDto result = studentsStatsService.getMyTestStats(studentId, testId);

        assertThat(result.getClassMedian()).isEqualByComparingTo("68");
        assertThat(result.getRank()).isEqualTo(3);
        assertThat(result.getPercentile()).isEqualByComparingTo("-0.0000");
    }

    @Test
    void getMyTestStats_shouldThrowWhenStudentIsNotEnrolledInCourse() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> studentsStatsService.getMyTestStats(studentId, testId))
                .isInstanceOf(StudentNotEnrolledInCourseException.class);
    }

    @Test
    void getMyTestStats_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentsStatsService.getMyTestStats(studentId, testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void getMyTestStats_shouldThrowWhenLessonDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentsStatsService.getMyTestStats(studentId, testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Lesson does not exist");
    }

    @Test
    void getMySummaryData_shouldReturnAggregatedSummaryWhenStudentIsEnrolled() {
        CourseDetailsDto courseDetails = new CourseDetailsDto("Algorithms", 4);
        CourseStatsDto courseStats = new CourseStatsDto(3, 2, BigDecimal.valueOf(95), BigDecimal.valueOf(65), 80.0);
        List<DifficultyLessonDto> difficultLessons = List.of(
                new DifficultyLessonDto(UUID.randomUUID(), "Graphs", BigDecimal.valueOf(55), BigDecimal.valueOf(75), BigDecimal.valueOf(20))
        );
        List<AttemptDetailsDto> attempts = List.of(
                new AttemptDetailsDto(UUID.randomUUID(), testId, "Quiz 1", new BigDecimal("0.8"), new BigDecimal("80"), true, LocalDateTime.now())
        );

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);
        when(testResultRepository.getCourseDetails(courseId)).thenReturn(courseDetails);
        when(testResultRepository.getCourseStats(studentId, courseId)).thenReturn(courseStats);
        when(lessonDifficultyByStudentRepository.getLessonDifficultyList(eq(courseId), eq(studentId), eq(BigDecimal.valueOf(60)), eq(BigDecimal.valueOf(15)), any(Pageable.class)))
                .thenReturn(difficultLessons);
        when(testResultRepository.getLastAttempts(eq(studentId), eq(courseId), any(Pageable.class))).thenReturn(attempts);

        MySummaryDataDto result = studentsStatsService.getMySummaryData(studentId, courseId);

        assertThat(result.getCourseTitle()).isEqualTo("Algorithms");
        assertThat(result.getDifficultyLessons()).isEqualTo(difficultLessons);
        assertThat(result.getLastAttempts()).isEqualTo(attempts);
        verify(testResultRepository).getLastAttempts(eq(studentId), eq(courseId), any(Pageable.class));
    }

    @Test
    void getMySummaryData_shouldThrowWhenCourseDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThatThrownBy(() -> studentsStatsService.getMySummaryData(studentId, courseId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Course does not exist");
    }
}
