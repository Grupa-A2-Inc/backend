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

    /**
     * Computes the failure rate (percentage) for the best attempts of a test and indicates whether it exceeds the active alert threshold.
     *
     * @param testId      the identifier of the test to evaluate
     * @param professorId the identifier of the professor; must match the test's creator for access
     * @return a FailureRateDTO containing the failure rate percentage, the configured threshold, and whether the alert is triggered
     * @throws DoesNotExistException if the test does not exist or no active alert threshold is configured for the test
     * @throws WithoutAccessException if the given professorId does not match the test's creator
     */
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

    /**
     * Resolves the test for the given lesson and returns its failure rate and alert status.
     *
     * @param lessonId    the lesson's id whose associated test will be evaluated
     * @param professorId the professor's id used to authorize access to the lesson's test
     * @return a FailureRateDTO containing the test's failure rate percentage, the configured threshold, and whether the alert is triggered
     * @throws DoesNotExistException  if no test is found for the given lesson id
     * @throws WithoutAccessException if the professorId does not match the test's creator
     */
    public FailureRateDTO getLessonFailureRate(UUID lessonId, UUID professorId) throws DoesNotExistException, WithoutAccessException {
        Test test = testRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new DoesNotExistException("No test found for lesson with id " + lessonId));
        if (!test.getCreatedBy().equals(professorId)) {
            throw new WithoutAccessException(professorId);
        }

        UUID testId = test.getId();

        return getTestFailureRate(testId, professorId);
    }

    /**
     * Creates or updates the active failure-rate alert threshold for a test and returns the active alert.
     *
     * @param testId      the identifier of the test to configure
     * @param professorId the identifier of the professor performing the operation; must be the test's creator
     * @param threshold   the failure-rate threshold to set (as a percentage)
     * @return            an AlertDTO representing the active alert after upsert, or `null` if no active alert is found
     * @throws DoesNotExistException if no test exists with the given `testId`
     * @throws WithoutAccessException if the `professorId` is not the creator of the test
     */
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

    /**
     * Retrieves active failure-rate alerts for the given professor.
     *
     * @param professorId the UUID of the professor whose active alerts are requested
     * @param roleName the role of the caller; must be {@link RoleName#TEACHER}
     * @return a list of active {@code AlertDTO} objects for the professor
     * @throws WithoutAccessException if {@code roleName} is not {@code TEACHER}
     */
    public List<AlertDTO> getAlerts(UUID professorId, RoleName roleName) {
        if (!roleName.equals(RoleName.TEACHER)) {
            throw new WithoutAccessException(professorId);
        }
        return analyticsAlertRepository.getActiveAlertsForProfessor(professorId)
                .stream()
                .map(alert -> new AlertDTO(alert.getId(), alert.getTestId(), alert.getProfessorId(), alert.getFailureThreshold(), alert.getCurrentFailureRate(), alert.isActive()))
                .toList();
    }

    /**
     * Builds per-lesson failure-rate charts for every test in the specified course owned by the professor.
     *
     * @param courseId    the course identifier whose lessons' tests will be queried
     * @param professorId the professor identifier used to verify course ownership
     * @return a list of TestFailureRateChartDTO objects; each element contains the daily failure-rate points for a lesson's test
     * @throws CourseNotFoundException if no course exists with the given `courseId`
     * @throws WithoutAccessException if the course is not owned by the given `professorId`
     * @throws DoesNotExistException   if a lesson in the course has no associated test
     */
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
