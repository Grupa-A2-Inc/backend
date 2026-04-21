package org.elearning.backend.analytics.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateDTO;
import org.elearning.backend.analytics.exception.AccessDeniedException;
import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.elearning.backend.analytics.repository.AnalyticsAlertRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.assessment.service.TestResultService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FailureRateService {
    private final TestResultRepository testResultRepository;
    private final TestRepository testRepository;
    private final AnalyticsAlertRepository analyticsAlertRepository;

    public FailureRateDTO getTestFailureRate(UUID testId, UUID professorId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException("Test with id " + testId + " does not exist"));
        if (test.getCreatedBy() != professorId) {
            throw new AccessDeniedException(professorId);
        }

        BigDecimal threshold = analyticsAlertRepository.findByTestId(testId)
                .map(AnalyticsAlert::getFailureThreshold)
                .orElse(new BigDecimal("50.0"));

        double failedCount = 0.0;
        List<TestResult> bestAttempts = testResultRepository.findBestAttemptsByTestId(testId);
        if (bestAttempts.isEmpty()) {
            return new FailureRateDTO(BigDecimal.ZERO, threshold, false);
        }
        for (TestResult testResult : bestAttempts) {
            if (!testResult.isPassed()) {
                failedCount += 1.0;
            }
        }
        double failureRate = (failedCount / bestAttempts.size()) * 100.0;
        boolean alertTriggered = BigDecimal.valueOf(failureRate).compareTo(threshold) > 0;
        return new FailureRateDTO(BigDecimal.valueOf(failureRate), threshold, alertTriggered);
    }

    public FailureRateDTO getLessonFailureRate(UUID lessonId, UUID professorId) {
        Test test = testRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new DoesNotExistException("No test found for lesson with id " + lessonId));
        if (test.getCreatedBy() != professorId) {
            throw new AccessDeniedException(professorId);
        }

        UUID testId = test.getId();

        return getTestFailureRate(testId, professorId);
    }
}
