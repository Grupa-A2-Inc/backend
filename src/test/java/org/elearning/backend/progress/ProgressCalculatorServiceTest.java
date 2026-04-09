package org.elearning.backend.progress;

import org.elearning.backend.assessment.model.*;
import org.elearning.backend.content.model.*;
import org.elearning.backend.enrollment.model.*;
import org.elearning.backend.enrollment.dto.ProgressDto;
import org.elearning.backend.enrollment.service.ProgressCalculatorService;
import org.elearning.backend.content.repository.*;
import org.elearning.backend.enrollment.repository.*;
import org.elearning.backend.assessment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProgressCalculatorServiceTest {

    @Autowired private ProgressCalculatorService progressCalculatorService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private CourseEnrollmentRepository enrollmentRepository;
    @Autowired private LessonProgressRepository lessonProgressRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private TestResultRepository testResultRepository;

    private UUID studentId;
    private Course course;
    private Chapter chapter;
    private Lesson lessonNoTest;
    private Lesson lessonWithTest;
    private CourseEnrollment enrollment;
    private org.elearning.backend.assessment.model.Test test;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        course = new Course();
        course.setTitle("Test Course");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setVisibility(CourseVisibility.PUBLIC);
        course.setCreatedBy(UUID.randomUUID());
        courseRepository.save(course);

        chapter = new Chapter();
        chapter.setCourse(course);
        chapter.setTitle("Chapter 1");
        chapter.setOrderIndex(0);
        chapterRepository.save(chapter);

        lessonNoTest = new Lesson();
        lessonNoTest.setChapter(chapter);
        lessonNoTest.setTitle("Lesson No Test");
        lessonNoTest.setOrderIndex(0);
        lessonRepository.save(lessonNoTest);

        lessonWithTest = new Lesson();
        lessonWithTest.setChapter(chapter);
        lessonWithTest.setTitle("Lesson With Test");
        lessonWithTest.setOrderIndex(1);
        lessonRepository.save(lessonWithTest);

        test = new org.elearning.backend.assessment.model.Test();
        test.setLessonId(lessonWithTest.getId());
        test.setCreatedBy(UUID.randomUUID());
        test.setTitle("Test pentru lectie");
        test.setStatus(TestStatus.PUBLISHED);
        test.setTimeLimitSec(300);
        testRepository.save(test);

        enrollment = new CourseEnrollment();
        enrollment.setCourseId(course.getId());
        enrollment.setStudentId(studentId);
        enrollmentRepository.save(enrollment);
    }

    @Test
    void shouldReturn0_whenStudentHasNotVisitedAnything() {
        ProgressDto progress = progressCalculatorService.calculateCourseProgress(
                course.getId(), studentId, enrollment.getId()
        );
        assertThat(progress.getPercentageDisplay()).isEqualTo(0.0);
        assertThat(progress.getCompletedLessons()).isEqualTo(0);
        assertThat(progress.getTotalLessons()).isEqualTo(2);
        assertThat(progress.isCompleted()).isFalse();
    }

    @Test
    void shouldReturn50_whenStudentVisitsLessonWithoutTest() {
        markVisited(lessonNoTest);
        ProgressDto progress = progressCalculatorService.calculateCourseProgress(
                course.getId(), studentId, enrollment.getId()
        );
        assertThat(progress.getPercentageDisplay()).isEqualTo(50.0);
        assertThat(progress.getCompletedLessons()).isEqualTo(1);
        assertThat(progress.isCompleted()).isFalse();
    }

    @Test
    void shouldStayAt50_whenStudentVisitsLessonWithTestButNoAttempt() {
        markVisited(lessonNoTest);
        markVisited(lessonWithTest);
        ProgressDto progress = progressCalculatorService.calculateCourseProgress(
                course.getId(), studentId, enrollment.getId()
        );
        assertThat(progress.getPercentageDisplay()).isEqualTo(50.0);
        assertThat(progress.getCompletedLessons()).isEqualTo(1);
    }

    @Test
    void shouldReturn100_whenStudentVisitsBothAndPassesTest() {
        markVisited(lessonNoTest);
        markVisited(lessonWithTest);
        passTest(test);
        ProgressDto progress = progressCalculatorService.calculateCourseProgress(
                course.getId(), studentId, enrollment.getId()
        );
        assertThat(progress.getPercentageDisplay()).isEqualTo(100.0);
        assertThat(progress.getCompletedLessons()).isEqualTo(2);
        assertThat(progress.isCompleted()).isTrue();
    }

    @Test
    void shouldReturn0_whenCourseHasNoLessons() {
        Course emptyCourse = new Course();
        emptyCourse.setTitle("Empty Course");
        emptyCourse.setStatus(CourseStatus.PUBLISHED);
        emptyCourse.setVisibility(CourseVisibility.PUBLIC);
        emptyCourse.setCreatedBy(UUID.randomUUID());
        courseRepository.save(emptyCourse);

        CourseEnrollment emptyEnrollment = new CourseEnrollment();
        emptyEnrollment.setCourseId(emptyCourse.getId());
        emptyEnrollment.setStudentId(studentId);
        enrollmentRepository.save(emptyEnrollment);

        ProgressDto progress = progressCalculatorService.calculateCourseProgress(
                emptyCourse.getId(), studentId, emptyEnrollment.getId()
        );
        assertThat(progress.getPercentageDisplay()).isEqualTo(0.0);
        assertThat(progress.getTotalLessons()).isEqualTo(0);
        assertThat(progress.isCompleted()).isFalse();
    }

    private void markVisited(Lesson lesson) {
        LessonProgress lp = new LessonProgress();
        lp.setLessonId(lesson.getId());
        lp.setStudentId(studentId);
        lp.setCourseEnrollment(enrollment);
        lessonProgressRepository.save(lp);
    }

    private void passTest(org.elearning.backend.assessment.model.Test t) {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(t);
        attempt.setStudentId(studentId);
        attempt.setAttemptNumber(1);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setEndedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.DONE);
        testAttemptRepository.save(attempt);

        TestResult result = new TestResult();
        result.setAttempt(attempt);
        result.setStudentId(studentId);
        result.setTest(t);
        result.setScore(new BigDecimal("0.8500"));
        result.setScorePercent(new BigDecimal("85.00"));
        result.setPassed(true);
        result.setCompletedAt(LocalDateTime.now());
        testResultRepository.save(result);
    }
    // -------------------------------------------------------
// calculateProgressPercent
// -------------------------------------------------------

    @Test
    void calculateProgressPercent_shouldReturn0_whenNothingVisited() {
        double percent = progressCalculatorService.calculateProgressPercent(enrollment.getId());
        assertThat(percent).isEqualTo(0.0);
    }

    @Test
    void calculateProgressPercent_shouldReturn50_whenOneOfTwoCompleted() {
        markVisited(lessonNoTest);
        double percent = progressCalculatorService.calculateProgressPercent(enrollment.getId());
        assertThat(percent).isEqualTo(50.0);
    }

    @Test
    void calculateProgressPercent_shouldReturn100_whenAllCompleted() {
        markVisited(lessonNoTest);
        markVisited(lessonWithTest);
        passTest(test);
        double percent = progressCalculatorService.calculateProgressPercent(enrollment.getId());
        assertThat(percent).isEqualTo(100.0);
    }

// -------------------------------------------------------
// checkAndMarkCompletion
// -------------------------------------------------------

    @Test
    void checkAndMarkCompletion_shouldReturnFalse_whenNotComplete() {
        markVisited(lessonNoTest);
        boolean marked = progressCalculatorService.checkAndMarkCompletion(enrollment.getId());
        assertThat(marked).isFalse();

        CourseEnrollment refreshed = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(refreshed.getCompletedAt()).isNull();
    }

    @Test
    void checkAndMarkCompletion_shouldReturnTrue_andSetCompletedAt_whenAllDone() {
        markVisited(lessonNoTest);
        markVisited(lessonWithTest);
        passTest(test);

        boolean marked = progressCalculatorService.checkAndMarkCompletion(enrollment.getId());
        assertThat(marked).isTrue();

        CourseEnrollment refreshed = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(refreshed.getCompletedAt()).isNotNull();
    }

    @Test
    void checkAndMarkCompletion_shouldReturnFalse_whenAlreadyMarked() {
        markVisited(lessonNoTest);
        markVisited(lessonWithTest);
        passTest(test);

        progressCalculatorService.checkAndMarkCompletion(enrollment.getId());

        // al doilea apel — deja marcat
        boolean markedAgain = progressCalculatorService.checkAndMarkCompletion(enrollment.getId());
        assertThat(markedAgain).isFalse();
    }
}