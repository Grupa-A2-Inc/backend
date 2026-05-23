package org.elearning.backend.assessment.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestResultService {
    private final TestAttemptRepository testAttemptRepository;
    private final TestResultRepository testResultRepository;
    private final TestRepository testRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuestionOptionRepository optionRepository;
    private final AttemptMapper attemptMapper;
    private final AttemptReportMapper attemptReportMapper;

    @Transactional
    public AttemptReportDTO getTestResult(UUID attemptId, UUID studentId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt with id " + attemptId + "does not exist"));
        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            throw new TimerExpiredException("The attempt expired before being submitted");
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new AttemptInProgressException("The attempt is still in progress");
        }



        TestResult result = testResultRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt with id " + attemptId + "does not have results"));
        TestResultDto resultDTO = attemptMapper.toTestResultDTO(result);

        List<QuestionForAttemptReportDTO> questionResults = buildQuestionResults(attemptId);

        return attemptReportMapper.toAttemptReportDTO(resultDTO, questionResults);
    }

    /**
     * Constructs per-question report entries for the specified test attempt.
     *
     * @param attemptId the UUID of the test attempt
     * @return a list of QuestionForAttemptReportDTO objects, each containing the question id, question type,
     *         question content, selected/correct option ids, and the full option list with text and review flags
     */
    private List<QuestionForAttemptReportDTO> buildQuestionResults(UUID attemptId) {
        List<AttemptAnswer> answers = answerRepository.findByAttemptId(attemptId);

        return answers.stream()
                .map(answer -> {
                    int questionId = answer.getQuestion().getId();
                    List<Integer> selectedOptionIds = answer.getSelectedOptionIds();
                    Set<Integer> selectedOptionIdSet = Set.copyOf(selectedOptionIds);

                    List<QuestionOption> options = optionRepository.findByQuestionId(questionId).stream()
                            .sorted(Comparator
                                    .comparing(QuestionOption::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(QuestionOption::getId))
                            .toList();
                    List<Integer> correctOptionIds = options.stream()
                            .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                            .map(QuestionOption::getId)
                            .toList();
                    Set<Integer> correctOptionIdSet = Set.copyOf(correctOptionIds);

                    List<QuestionForAttemptReportDTO.OptionForAttemptReportDTO> optionDtos = options.stream()
                            .map(option -> QuestionForAttemptReportDTO.OptionForAttemptReportDTO.builder()
                                    .optionId(option.getId())
                                    .text(option.getText())
                                    .displayOrder(option.getDisplayOrder())
                                    .selected(selectedOptionIdSet.contains(option.getId()))
                                    .correct(correctOptionIdSet.contains(option.getId()))
                                    .build())
                            .toList();

                    return QuestionForAttemptReportDTO.builder()
                            .questionId(questionId)
                            .questionType(answer.getQuestion().getQuestionType())
                            .content(answer.getQuestion().getContent())
                            .selectedOptionIds(selectedOptionIds)
                            .correctOptionIds(correctOptionIds)
                            .options(optionDtos)
                            .build();
                })
                .toList();
    }

    /**
     * Retrieve attempt status summaries for a student on a specific test.
     *
     * @param testId    the identifier of the test
     * @param studentId the identifier of the student
     * @return a list of AttemptStatusDTO for the student's attempts on the test, ordered by attempt start time descending
     */
    @Transactional
    public List<AttemptStatusDTO> getTestAttempts(UUID testId, UUID studentId) {
        List<TestResult> results = testResultRepository.findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(studentId, testId);
        return results.stream().map(attemptReportMapper::toAttemptStatusDTO).toList();
    }

    @Transactional
    public List<AttemptStatusDTO> getLessonAttempts(UUID lessonId, UUID studentId) {
        List<TestResult> results = testResultRepository.findByStudentIdAndLessonIdOrderByAttemptStartedAtDesc(studentId, lessonId);
        return results.stream().map(attemptReportMapper::toAttemptStatusDTO).toList();
    }

    @Transactional
    public AttemptStatusDTO getBestTestAttempt(UUID testId, UUID studentId) {
        TestResult testResult = testResultRepository.findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(studentId, testId, AttemptStatus.DONE)
                .orElseThrow(() -> new IllegalArgumentException("No finished attempts found for student " + studentId + " on test " + testId));
        return attemptReportMapper.toAttemptStatusDTO(testResult);
    }
}
