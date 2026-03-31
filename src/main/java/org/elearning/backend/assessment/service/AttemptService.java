package org.elearning.backend.assessment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.*;
import org.elearning.backend.assessment.exception.AttemptAlreadySubmittedException;
import org.elearning.backend.assessment.exception.TestNotPublishedException;
import org.elearning.backend.assessment.exception.TimerExpiredException;
import org.elearning.backend.assessment.mapper.AttemptMapper;
import org.elearning.backend.assessment.mapper.QuestionMapper;
import org.elearning.backend.assessment.model.*;
import org.elearning.backend.assessment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final TestResultRepository resultRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    private final AttemptMapper attemptMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public StartAttemptResponseDTO startAttempt(UUID testId, UUID studentId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The test with id " + testId + " does not exist"));

        if (test.getStatus() != TestStatus.PUBLISHED) {
            throw new TestNotPublishedException(
                    "The test with id " + testId + " is not published yet");
        }

        int attemptNumber = attemptRepository.countByTestIdAndStudentId(testId, studentId) + 1;

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setStudentId(studentId);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        TestAttempt saved = attemptRepository.save(attempt);

        List<Question> questionsFromDb = questionRepository.findByTestIdWithOptions(testId);

        List<QuestionForStudentDTO> questions = questionMapper.toQuestionForStudentDTOList(questionsFromDb);

        return attemptMapper.toStartAttemptResponseDTO(saved, test, questions);
    }

    @Transactional
    public TestResultDTO submitAttempt(UUID attemptId, UUID studentId,
                                       SubmitRequestDTO request) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attempt with id " + attemptId + " does not exist"));

        if (attempt.getStatus() == AttemptStatus.DONE) {
            throw new AttemptAlreadySubmittedException("The attempt has already been submitted");
        }
        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            throw new TimerExpiredException("The attempt has expired and cannot be submitted");
        }
        /*if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException(
                    "Nu ai permisiunea să submit-ezi acest attempt");
        }*/

        Test test = attempt.getTest();
        /*testRepository.findById(attempt.getTest().getId())
                .orElseThrow(() -> new IllegalArgumentException("The test with id " + attempt.getTest().getId() + " does not exist"));
*/
        LocalDateTime expireTime = attempt.getStartedAt().plusSeconds(test.getTimeLimitSec());
        if (LocalDateTime.now().isAfter(expireTime)) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setEndedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
            throw new TimerExpiredException("The time limit for this attempt has expired");
        }

        int correctCount = 0;
        int totalCount = request.getAnswers().size();

        for (SubmitAnswerDTO answerDTO : request.getAnswers()) {
            Question question = questionRepository.findById(answerDTO.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Question with id " + answerDTO.getQuestionId() + " does not exist"));

            Set<Long> correctOptionIds = optionRepository
                    .findByQuestionIdAndIsCorrectTrue(answerDTO.getQuestionId())
                    .stream()
                    .map(QuestionOption::getId)
                    .collect(Collectors.toSet());

            Set<Long> selectedIds = new HashSet<>(answerDTO.getSelectedOptionIds());
            boolean isCorrect = selectedIds.equals(correctOptionIds);
            if (isCorrect) {
                correctCount++;
            }

            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOptionIds(answerDTO.getSelectedOptionIds());
            answer.setCorrect(isCorrect);
            answer.setTimeSpent(answerDTO.getTimeSpent());
            answer.setAnsweredAt(LocalDateTime.now());

            answerRepository.save(answer);
        }

        BigDecimal score = totalCount > 0
                ? BigDecimal.valueOf((double) correctCount / totalCount)
                .setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal scorePercent = score
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        boolean passed = scorePercent.compareTo(BigDecimal.valueOf(60)) >= 0;

        TestResult result = new TestResult();
        result.setAttempt(attempt);
        result.setStudentId(studentId);
        result.setTest(test);
        result.setScore(score);
        result.setScorePercent(scorePercent);
        result.setPassed(passed);
        result.setCompletedAt(LocalDateTime.now());
        resultRepository.save(result);

        attempt.setStatus(AttemptStatus.DONE);
        attempt.setEndedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        if (test.getAiEnabled()) {
            // TODO sprint 3: sendToAiAsync(attempt, result);
        }

        return attemptMapper.toTestResultDTO(result);
    }
}
