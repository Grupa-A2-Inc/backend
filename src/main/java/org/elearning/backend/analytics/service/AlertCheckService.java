package org.elearning.backend.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.elearning.backend.analytics.repository.AnalyticsAlertRepository;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckService {
    private final TestResultRepository testResultRepository;
    private final AnalyticsAlertRepository analyticsAlertRepository;

    /**
     * Evaluates the active analytics alert for the given test and updates its state based on recent best attempts.
     *
     * If an active alert exists, computes the current failure rate from the repository's best attempts, sets
     * the alert's `currentFailureRate` (zero if no attempts), and, if the computed rate is greater than the
     * alert's `failureThreshold` and the alert has not been previously triggered, sets `triggeredAt` to now.
     * The updated alert is persisted.
     *
     * @param testId the UUID of the test whose alerts should be checked and updated
     */
    public void checkAlerts(UUID testId) {
        try {
            Optional<AnalyticsAlert> alertOptional = analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId);
            if (alertOptional.isEmpty()) {
                return;
            }
            AnalyticsAlert alert = alertOptional.get();

            BigDecimal threshold = alert.getFailureThreshold();

            List<TestResult> bestAttempts = testResultRepository.findBestAttemptsByTestId(testId);
            if (bestAttempts.isEmpty()) {
                alert.setCurrentFailureRate(BigDecimal.ZERO);
            }
            else {
                double failedCount = 0.0;
                for (TestResult testResult : bestAttempts) {
                    if (!testResult.isPassed()) {
                        failedCount += 1.0;
                    }
                }
                double failureRate = (failedCount / bestAttempts.size()) * 100.0;
                alert.setCurrentFailureRate(BigDecimal.valueOf(failureRate));
                if (BigDecimal.valueOf(failureRate).compareTo(threshold) > 0 && alert.getTriggeredAt() == null) {
                    alert.setTriggeredAt(LocalDateTime.now());
                }
            }
            analyticsAlertRepository.save(alert);

        } catch (Exception exception) {
            log.error("Failed to check alerts for testId: {}", testId, exception);
        }
    }
}
