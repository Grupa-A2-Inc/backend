package org.elearning.backend.assessment;

import org.elearning.backend.analytics.service.AlertCheckService;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.exception.InvalidAttemptUserException;
import org.elearning.backend.assessment.mapper.AttemptMapper;
import org.elearning.backend.assessment.mapper.QuestionMapper;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.*;
import org.elearning.backend.assessment.service.AttemptService;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.repository.LessonProgressRepository;
import org.elearning.backend.enrollment.service.ProgressCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock private TestAttemptRepository attemptRepository;
    @Mock private AttemptAnswerRepository answerRepository;
    @Mock private TestResultRepository resultRepository;
    @Mock private TestRepository testRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository optionRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private ProgressCalculatorService progressCalculatorService;
    @Mock private AttemptMapper attemptMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private AlertCheckService alertCheckService;

    @InjectMocks
    private AttemptService attemptService;

    @Test
    void startAttempt_shouldThrowWhenTestDoesNotExist() {
        UUID testId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attemptService.startAttempt(testId, studentId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("test");
    }

    @Test
    void submitAttempt_shouldThrowWhenAttemptBelongsToDifferentStudent() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setTimeLimitSec(300);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStudentId(UUID.randomUUID());
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTest(test);

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(java.util.List.of());

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> attemptService.submitAttempt(attemptId, studentId, request))
                .isInstanceOf(InvalidAttemptUserException.class)
                .hasMessageContaining(studentId.toString());
    }

    @Test
    void submitAttempt_shouldThrowWhenAttemptDoesNotExist() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(java.util.List.of());

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attemptService.submitAttempt(attemptId, studentId, request))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("attempt")
                .hasMessageContaining(attemptId.toString());
    }

    @Test
    void submitAttempt_shouldContinueWhenAlertCheckFails() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setStatus(TestStatus.PUBLISHED);
        test.setTimeLimitSec(300);
        test.setAiEnabled(false);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(LocalDateTime.now().minusSeconds(30));
        attempt.setTest(test);

        Question question = new Question();
        question.setId(11);
        question.setContent("Q");
        question.setQuestionType(QuestionType.MULTI_CHOICE);

        SubmitRequestDto request = new SubmitRequestDto();
        org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto answer =
                new org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto();
        answer.setQuestionId(11);
        answer.setSelectedOptionIds(java.util.List.of(101));
        answer.setTimeSpent(java.math.BigDecimal.ONE);
        request.setAnswers(java.util.List.of(answer));

        org.elearning.backend.assessment.model.QuestionOption correctOption =
                new org.elearning.backend.assessment.model.QuestionOption();
        correctOption.setId(101);

        org.elearning.backend.assessment.dto.assigment_dto.TestResultDto mappedDto =
                new org.elearning.backend.assessment.dto.assigment_dto.TestResultDto();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(java.util.List.of(question));
        when(questionRepository.findById(11)).thenReturn(Optional.of(question));
        when(optionRepository.findByQuestionIdAndIsCorrectTrue(11)).thenReturn(java.util.List.of(correctOption));
        doThrow(new RuntimeException("alert failure")).when(alertCheckService).checkAlerts(testId);
        when(attemptMapper.toTestResultDTO(any(TestResult.class))).thenReturn(mappedDto);

        var result = attemptService.submitAttempt(attemptId, studentId, request);

        assertSame(mappedDto, result);
        verify(resultRepository).save(any(TestResult.class));
        verify(attemptRepository).save(attempt);
    }

    @Test
    void submitAttempt_shouldIgnoreExpirationWhenTimeLimitIsZero() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setStatus(TestStatus.PUBLISHED);
        test.setTimeLimitSec(0);
        test.setAiEnabled(false);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(LocalDateTime.now().minusHours(2));
        attempt.setTest(test);

        Question question = new Question();
        question.setId(11);
        question.setContent("Q");
        question.setQuestionType(QuestionType.MULTI_CHOICE);

        SubmitRequestDto request = new SubmitRequestDto();
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(11);
        answer.setSelectedOptionIds(java.util.List.of(101));
        answer.setTimeSpent(BigDecimal.ONE);
        request.setAnswers(java.util.List.of(answer));

        org.elearning.backend.assessment.model.QuestionOption correctOption =
                new org.elearning.backend.assessment.model.QuestionOption();
        correctOption.setId(101);

        org.elearning.backend.assessment.dto.assigment_dto.TestResultDto mappedDto =
                new org.elearning.backend.assessment.dto.assigment_dto.TestResultDto();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(java.util.List.of(question));
        when(questionRepository.findById(11)).thenReturn(Optional.of(question));
        when(optionRepository.findByQuestionIdAndIsCorrectTrue(11)).thenReturn(java.util.List.of(correctOption));
        when(attemptMapper.toTestResultDTO(any(TestResult.class))).thenReturn(mappedDto);

        assertSame(mappedDto, attemptService.submitAttempt(attemptId, studentId, request));
        verify(attemptRepository, never()).saveAndFlush(attempt);
        verify(resultRepository).save(any(TestResult.class));
    }

    @Test
    void submitAttempt_shouldIgnoreExpirationWhenTimeLimitIsNull() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setStatus(TestStatus.PUBLISHED);
        test.setTimeLimitSec(null);
        test.setAiEnabled(false);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(LocalDateTime.now().minusHours(2));
        attempt.setTest(test);

        Question question = new Question();
        question.setId(11);
        question.setContent("Q");
        question.setQuestionType(QuestionType.MULTI_CHOICE);

        SubmitRequestDto request = new SubmitRequestDto();
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(11);
        answer.setSelectedOptionIds(java.util.List.of(101));
        answer.setTimeSpent(BigDecimal.ONE);
        request.setAnswers(java.util.List.of(answer));

        org.elearning.backend.assessment.model.QuestionOption correctOption =
                new org.elearning.backend.assessment.model.QuestionOption();
        correctOption.setId(101);

        org.elearning.backend.assessment.dto.assigment_dto.TestResultDto mappedDto =
                new org.elearning.backend.assessment.dto.assigment_dto.TestResultDto();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(java.util.List.of(question));
        when(questionRepository.findById(11)).thenReturn(Optional.of(question));
        when(optionRepository.findByQuestionIdAndIsCorrectTrue(11)).thenReturn(java.util.List.of(correctOption));
        when(attemptMapper.toTestResultDTO(any(TestResult.class))).thenReturn(mappedDto);

        assertSame(mappedDto, attemptService.submitAttempt(attemptId, studentId, request));
        verify(attemptRepository, never()).saveAndFlush(attempt);
        verify(resultRepository).save(any(TestResult.class));
    }

    @Test
    void markLessonAndCheckCourseCompletion_shouldStopWhenCourseIdIsMissing() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setLessonId(lessonId);

        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.empty());

        invokeMarkLesson(studentId, test);

        verify(courseEnrollmentRepository, never()).findByStudentIdAndCourseId(any(), any());
        verify(lessonProgressRepository, never()).insertProgressIdempotent(any(), any(), any());
    }

    @Test
    void markLessonAndCheckCourseCompletion_shouldRunAsyncCompletionCheck() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setLessonId(lessonId);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(enrollmentId);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(progressCalculatorService).checkAndMarkCompletion(enrollmentId);

        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.of(courseId));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.of(enrollment));

        invokeMarkLesson(studentId, test);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(lessonProgressRepository).insertProgressIdempotent(lessonId, studentId, enrollmentId);
        verify(progressCalculatorService).checkAndMarkCompletion(enrollmentId);
    }

    @Test
    void markLessonAndCheckCourseCompletion_shouldCatchAsyncProgressFailure() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setLessonId(lessonId);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(enrollmentId);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            throw new RuntimeException("boom");
        }).when(progressCalculatorService).checkAndMarkCompletion(enrollmentId);

        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.of(courseId));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.of(enrollment));

        invokeMarkLesson(studentId, test);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(lessonProgressRepository).insertProgressIdempotent(lessonId, studentId, enrollmentId);
        verify(progressCalculatorService).checkAndMarkCompletion(enrollmentId);
    }

    private void invokeMarkLesson(UUID studentId, org.elearning.backend.assessment.model.Test test) throws Exception {
        Method method = AttemptService.class.getDeclaredMethod("markLessonAndCheckCourseCompletion", UUID.class, org.elearning.backend.assessment.model.Test.class);
        method.setAccessible(true);
        method.invoke(attemptService, studentId, test);
    }
}
