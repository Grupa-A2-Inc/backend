package org.elearning.backend.analytics;

import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.elearning.backend.analytics.repository.AnalyticsAlertRepository;
import org.elearning.backend.analytics.service.AlertCheckService;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertCheckServiceTest {

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private AnalyticsAlertRepository analyticsAlertRepository;

    @InjectMocks
    private AlertCheckService alertCheckService;

    @Test
    void returnsWhenNoActiveAlertExists() {
        UUID testId = UUID.randomUUID();
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(Optional.empty());

        alertCheckService.checkAlerts(testId);

        verify(analyticsAlertRepository, never()).save(any());
    }

    @Test
    void setsCurrentFailureRateToZeroWhenNoAttemptsExist() {
        UUID testId = UUID.randomUUID();
        AnalyticsAlert alert = activeAlert(60);
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(Optional.of(alert));
        when(testResultRepository.findBestAttemptsByTestId(testId)).thenReturn(List.of());

        alertCheckService.checkAlerts(testId);

        verify(analyticsAlertRepository).save(alert);
        assertThat(alert.getCurrentFailureRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(alert.getTriggeredAt()).isNull();
    }

    @Test
    void triggersAlertWhenFailureRateExceedsThreshold() {
        UUID testId = UUID.randomUUID();
        AnalyticsAlert alert = activeAlert(50);
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(Optional.of(alert));
        when(testResultRepository.findBestAttemptsByTestId(testId)).thenReturn(List.of(result(false), result(false), result(true)));

        alertCheckService.checkAlerts(testId);

        verify(analyticsAlertRepository).save(alert);
        assertThat(alert.getCurrentFailureRate()).isEqualByComparingTo(BigDecimal.valueOf(66.66666666666666));
        assertThat(alert.getTriggeredAt()).isNotNull();
    }

    @Test
    void doesNotRetriggerAlertWhenAlreadyTriggered() {
        UUID testId = UUID.randomUUID();
        AnalyticsAlert alert = activeAlert(10);
        LocalDateTime triggeredAt = LocalDateTime.now().minusDays(1);
        alert.setTriggeredAt(triggeredAt);
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenReturn(Optional.of(alert));
        when(testResultRepository.findBestAttemptsByTestId(testId)).thenReturn(List.of(result(false)));

        alertCheckService.checkAlerts(testId);

        verify(analyticsAlertRepository).save(alert);
        assertThat(alert.getTriggeredAt()).isEqualTo(triggeredAt);
    }

    @Test
    void swallowsRepositoryExceptions() {
        UUID testId = UUID.randomUUID();
        when(analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> alertCheckService.checkAlerts(testId));
    }

    private AnalyticsAlert activeAlert(double threshold) {
        AnalyticsAlert alert = new AnalyticsAlert();
        alert.setFailureThreshold(BigDecimal.valueOf(threshold));
        return alert;
    }

    private TestResult result(boolean passed) {
        TestResult result = new TestResult();
        result.setPassed(passed);
        return result;
    }
}
