package org.elearning.backend.assessment;

import org.elearning.backend.assessment.dto.attempt_dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.dto.question_dto.QuestionForAttemptReportDTO;
import org.elearning.backend.assessment.dto.assigment_dto.TestResultDto;
import org.elearning.backend.assessment.exception.AttemptInProgressException;
import org.elearning.backend.assessment.exception.TimerExpiredException;
import org.elearning.backend.assessment.mapper.AttemptMapper;
import org.elearning.backend.assessment.mapper.AttemptReportMapper;
import org.elearning.backend.assessment.model.*;
import org.elearning.backend.assessment.repository.*;
import org.elearning.backend.assessment.service.TestResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestResultServiceTest {

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @SuppressWarnings("unused")
    @Mock
    private TestRepository testRepository; // Needed for @InjectMocks constructor injection

    @Mock
    private AttemptAnswerRepository answerRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private AttemptMapper attemptMapper;

    @Mock
    private AttemptReportMapper attemptReportMapper;

    @InjectMocks
    private TestResultService testResultService;

    // Eliminates the "Unchecked assignment" SonarQube bug
    @Captor
    private ArgumentCaptor<List<QuestionForAttemptReportDTO>> questionListCaptor;

    private UUID attemptId;
    private UUID studentId;
    private UUID testId;

    @BeforeEach
    void setUp() {
        attemptId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        testId = UUID.randomUUID();
    }

    // --- TESTE PENTRU getTestResult ---

    @Test
    void getTestResult_attemptNotFound_throwsException() {
        when(testAttemptRepository.findById(attemptId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testResultService.getTestResult(attemptId, studentId));

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void getTestResult_attemptExpired_throwsException() {
        TestAttempt attempt = new TestAttempt();
        attempt.setStatus(AttemptStatus.EXPIRED);

        when(testAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThrows(TimerExpiredException.class,
                () -> testResultService.getTestResult(attemptId, studentId));
    }

    @Test
    void getTestResult_attemptInProgress_throwsException() {
        TestAttempt attempt = new TestAttempt();
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        when(testAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThrows(AttemptInProgressException.class,
                () -> testResultService.getTestResult(attemptId, studentId));
    }

    @Test
    void getTestResult_resultNotFound_throwsException() {
        TestAttempt attempt = new TestAttempt();
        attempt.setStatus(AttemptStatus.DONE);

        when(testAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(testResultRepository.findById(attemptId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testResultService.getTestResult(attemptId, studentId));

        assertTrue(exception.getMessage().contains("does not have results"));
    }

    @Test
    void getTestResult_success_returnsAttemptReportDTO() {
        // Mock the Attempt
        TestAttempt attempt = new TestAttempt();
        attempt.setStatus(AttemptStatus.DONE);

        // Mock the TestResult
        TestResult result = new TestResult();

        // Mock DTOs to bypass constructor visibility issues
        TestResultDto resultDTO = mock(TestResultDto.class);
        AttemptReportDTO expectedReport = mock(AttemptReportDTO.class);

        // Mock Question & Answer
        AttemptAnswer answer = mock(AttemptAnswer.class);
        Question question = mock(Question.class);
        when(answer.getQuestion()).thenReturn(question);
        when(question.getId()).thenReturn(1);
        when(question.getContent()).thenReturn("What is Java?");
        when(answer.getSelectedOptionIds()).thenReturn(List.of(10, 11));

        // Mock Correct Options
        QuestionOption correctOption = mock(QuestionOption.class);
        when(correctOption.getId()).thenReturn(10);

        // Setting up behaviors
        when(testAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(testResultRepository.findById(attemptId)).thenReturn(Optional.of(result));
        when(attemptMapper.toTestResultDTO(result)).thenReturn(resultDTO);

        when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(answer));
        when(optionRepository.findByQuestionIdAndIsCorrectTrue(1)).thenReturn(List.of(correctOption));

        when(attemptReportMapper.toAttemptReportDTO(eq(resultDTO), anyList())).thenReturn(expectedReport);

        // Executare
        AttemptReportDTO actualReport = testResultService.getTestResult(attemptId, studentId);

        // Asertii
        assertNotNull(actualReport);
        assertEquals(expectedReport, actualReport);

        // Verify with class-level @Captor to avoid generic type warnings
        verify(attemptReportMapper).toAttemptReportDTO(eq(resultDTO), questionListCaptor.capture());

        List<QuestionForAttemptReportDTO> builtQuestions = questionListCaptor.getValue();
        assertEquals(1, builtQuestions.size());
        assertEquals(1, builtQuestions.get(0).getQuestionId());
        assertEquals("What is Java?", builtQuestions.get(0).getContent());
        assertEquals(List.of(10, 11), builtQuestions.get(0).getSelectedOptionIds());
        assertEquals(List.of(10), builtQuestions.get(0).getCorrectOptionIds());
    }

    // --- TESTE PENTRU getTestAttempts ---

    @Test
    void getTestAttempts_returnsListOfAttemptStatusDTO() {
        TestResult testResult = new TestResult();
        AttemptStatusDTO attemptStatusDTO = mock(AttemptStatusDTO.class);

        when(testResultRepository.findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(studentId, testId))
                .thenReturn(List.of(testResult));
        when(attemptReportMapper.toAttemptStatusDTO(testResult)).thenReturn(attemptStatusDTO);

        List<AttemptStatusDTO> results = testResultService.getTestAttempts(testId, studentId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(attemptStatusDTO, results.get(0));
    }

    @Test
    void getTestAttempts_noAttempts_returnsEmptyList() {
        when(testResultRepository.findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(studentId, testId))
                .thenReturn(Collections.emptyList());

        List<AttemptStatusDTO> results = testResultService.getTestAttempts(testId, studentId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // --- TESTE PENTRU getBestTestAttempt ---

    @Test
    void getBestTestAttempt_noFinishedAttempts_throwsException() {
        when(testResultRepository.findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(studentId, testId, AttemptStatus.DONE))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testResultService.getBestTestAttempt(testId, studentId));

        assertTrue(exception.getMessage().contains("No finished attempts found"));
    }

    @Test
    void getBestTestAttempt_success_returnsAttemptStatusDTO() {
        TestResult testResult = new TestResult();
        AttemptStatusDTO attemptStatusDTO = mock(AttemptStatusDTO.class);

        when(testResultRepository.findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(studentId, testId, AttemptStatus.DONE))
                .thenReturn(Optional.of(testResult));
        when(attemptReportMapper.toAttemptStatusDTO(testResult)).thenReturn(attemptStatusDTO);

        AttemptStatusDTO result = testResultService.getBestTestAttempt(testId, studentId);

        assertNotNull(result);
        assertEquals(attemptStatusDTO, result);
    }
}