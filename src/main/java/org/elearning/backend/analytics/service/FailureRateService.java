package org.elearning.backend.analytics.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.alerts.AlertDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateChartPointDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.TestFailureRateChartDTO;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.elearning.backend.analytics.repository.AnalyticsAlertRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestResult;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FailureRateService {
    private final TestResultRepository testResultRepository;
    private final TestRepository testRepository;
    private final AnalyticsAlertRepository analyticsAlertRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public FailureRateDTO getTestFailureRate(UUID testId, UUID professorId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException("Test with id " + testId + " does not exist"));
        if (!test.getCreatedBy().equals(professorId)) {
            throw new WithoutAccessException(professorId);
        }

        BigDecimal threshold = analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId)
                .map(AnalyticsAlert::getFailureThreshold)
                .orElseThrow(() -> new DoesNotExistException("No analytics alert found for test with id " + testId));

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

    public FailureRateDTO getLessonFailureRate(UUID lessonId, UUID professorId) throws DoesNotExistException, WithoutAccessException {
        Test test = testRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new DoesNotExistException("No test found for lesson with id " + lessonId));
        if (!test.getCreatedBy().equals(professorId)) {
            throw new WithoutAccessException(professorId);
        }

        UUID testId = test.getId();

        return getTestFailureRate(testId, professorId);
    }

    @Transactional
    public AlertDTO createOrUpdateAlert(UUID testId, UUID professorId, BigDecimal threshold) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException("Test with id " + testId + " does not exist"));
        if (!test.getCreatedBy().equals(professorId)) {
            throw new WithoutAccessException(professorId);
        }

        analyticsAlertRepository.upsertAlertThreshold(testId, professorId, threshold);
        Optional<AnalyticsAlert> analyticsAlertOptional = analyticsAlertRepository.findByTestIdAndIsActiveTrue(testId);
        if (analyticsAlertOptional.isPresent()) {
            AnalyticsAlert analyticsAlert = analyticsAlertOptional.get();
            return new AlertDTO(analyticsAlert.getId(), analyticsAlert.getTestId(), analyticsAlert.getProfessorId(), analyticsAlert.getFailureThreshold(), analyticsAlert.getCurrentFailureRate(), analyticsAlert.isActive());
        }
        return null;
    }

    public List<AlertDTO> getAlerts(UUID professorId, RoleName roleName) {
        if (!roleName.equals(RoleName.TEACHER)) {
            throw new WithoutAccessException(professorId);
        }
        return analyticsAlertRepository.getActiveAlertsForProfessor(professorId)
                .stream()
                .map(alert -> new AlertDTO(alert.getId(), alert.getTestId(), alert.getProfessorId(), alert.getFailureThreshold(), alert.getCurrentFailureRate(), alert.isActive()))
                .toList();
    }

    public List<TestFailureRateChartDTO> getFailureCharts(UUID courseId,UUID professorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (!course.getCreatedBy().equals(professorId)) {
            throw new WithoutAccessException(professorId);
        }

        List<UUID> lessonIds = lessonRepository.findAllLessonIdsByCourseId(courseId);
        List<TestFailureRateChartDTO> chartList = new ArrayList<>();
        for (UUID lessonId : lessonIds) {
            Test test = testRepository.findByLessonId(lessonId)
                    .orElseThrow(() -> new DoesNotExistException("No test found for lesson with id " + lessonId));

            List<Object[]> pointResults = analyticsAlertRepository.getDailyFailureRatesForTest(test.getId());
            List<FailureRateChartPointDTO> points = pointResults.stream()
                    .map(row -> new FailureRateChartPointDTO(((java.sql.Date) row[0]).toLocalDate(), ((Number) row[1]).doubleValue()))
                    .toList();
            chartList.add(new TestFailureRateChartDTO(points));
        }

        return chartList;
    }
}
