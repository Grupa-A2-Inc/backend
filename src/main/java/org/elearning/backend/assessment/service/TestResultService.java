package org.elearning.backend.assessment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.QuestionForAttemptReportDTO;
import org.elearning.backend.assessment.dto.TestResultDto;
import org.elearning.backend.assessment.exception.AttemptInProgressException;
import org.elearning.backend.assessment.exception.TimerExpiredException;
import org.elearning.backend.assessment.mapper.AttemptMapper;
import org.elearning.backend.assessment.mapper.AttemptReportMapper;
import org.elearning.backend.assessment.model.*;
import org.elearning.backend.assessment.repository.AttemptAnswerRepository;
import org.elearning.backend.assessment.repository.QuestionOptionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestResultService {
    private final TestAttemptRepository testAttemptRepository;
    private final TestResultRepository testResultRepository;
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

    private List<QuestionForAttemptReportDTO> buildQuestionResults(UUID attemptId) {
        List<AttemptAnswer> answers = answerRepository.findByAttemptId(attemptId);

        return answers.stream()
                .map(answer -> {
                    Long questionId = answer.getQuestion().getId();

                    // Get correct option IDs for this question (where isCorrect = true)
                    List<Long> correctOptionIds = optionRepository
                            .findByQuestionIdAndIsCorrectTrue(questionId)
                            .stream()
                            .map(QuestionOption::getId)
                            .collect(Collectors.toList());

                    // Build the DTO
                    return QuestionForAttemptReportDTO.builder()
                            .questionId(questionId)
                            .questionType(answer.getQuestion().getQuestionType())
                            .content(answer.getQuestion().getContent())
                            .selectedOptionIds(answer.getSelectedOptionIds())  // Already a List<Long>
                            .correctOptionIds(correctOptionIds)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
