package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.alerts.AlertDTO;
import org.elearning.backend.analytics.dto.alerts.ThresholdDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.TestFailureRateChartDTO;
import org.elearning.backend.analytics.service.FailureRateService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

// SWAGGER ADDED
@Tag(name = "Failure Rate", description = "Test and lesson failure rate analytics and alerts")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FailureRateController {
    private static final String OK = "200";

    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final FailureRateService failureRateService;

    /**
     * Retrieve failure-rate information for a specific test.
     *
     * The response contains the test's failure rate, the configured alert threshold, and whether the threshold
     * currently triggers an alert for the professor identified by the provided authentication principal.
     *
     * @param testId      the UUID of the test to query
     * @param currentUser the authenticated principal used to determine the professor's UUID
     * @return            a FailureRateDTO with failure rate, threshold, and alert status
     */
    @Operation(summary = "Get the failure rate for a specific test",
            description = "A teacher can get the failure rate for a specific test they created, along with the threshold and whether an alert is triggered.")
    @ApiResponse(responseCode = OK, description = "Failure rate data returned successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the test analytics")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test not found")
    @GetMapping("/tests/{testId}/analytics/failure-rate")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<FailureRateDTO> getTestFailureRate(@P("id") @PathVariable UUID testId, @AuthenticationPrincipal UserDetails currentUser) {
        UUID professorId = extractUserId(currentUser);
        return ResponseEntity.ok(failureRateService.getTestFailureRate(testId, professorId));
    }

    /**
     * Retrieve the failure rate for the specified lesson, including the configured threshold and whether an alert is triggered.
     *
     * @param lessonId    the UUID of the lesson to query
     * @param currentUser the authenticated user requesting the data
     * @return the lesson's failure rate data, threshold, and alert status as a FailureRateDTO
     */
    @Operation(summary = "Get the failure rate for a specific lesson",
            description = "A teacher can get the failure rate for a specific lesson they created, along with the threshold and whether an alert is triggered.")
    @ApiResponse(responseCode = OK, description = "Failure rate data returned successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the lesson analytics")
    @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    @GetMapping("/lessons/{lessonId}/analytics/failure-rate")
    @PreAuthorize("@accessService.canViewLessonContent(authentication,#id)")
    public ResponseEntity<FailureRateDTO> getLessonFailureRate(@P("id") @PathVariable UUID lessonId, @AuthenticationPrincipal UserDetails currentUser) {
        UUID professorId = extractUserId(currentUser);
        return ResponseEntity.ok(failureRateService.getLessonFailureRate(lessonId, professorId));
    }

    /**
     * Create or update an analytics alert for the specified test.
     *
     * Creates or updates an alert that will be triggered when the test's failure rate exceeds the provided threshold.
     *
     * @param testId the UUID of the test to create or update the alert for
     * @param currentUser the authenticated user performing the operation
     * @param thresholdDTO DTO carrying the failure-rate threshold that will trigger the alert
     * @return the created or updated AlertDTO
     */
    @Operation(summary = "Create or update an analytics alert for a specific test",
            description = "A teacher can create or update an analytics alert for a specific test they created by providing a failure rate threshold." +
            " If the failure rate exceeds the threshold, an alert will be triggered.")
    @ApiResponse(responseCode = OK, description = "Analytics alert created or updated successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to create or update the test analytics alert")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test not found")
    @PostMapping("/tests/{testId}/analytics/alerts")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<AlertDTO> postAlert(@P("id") @PathVariable UUID testId, @AuthenticationPrincipal UserDetails currentUser, @RequestBody ThresholdDTO thresholdDTO) {
        UUID professorId = extractUserId(currentUser);
        return ResponseEntity.ok(failureRateService.createOrUpdateAlert(testId, professorId, BigDecimal.valueOf(thresholdDTO.getFailureThreshold())));
    }

    /**
     * Retrieve active analytics alerts for the authenticated professor's tests.
     *
     * @param currentUser the authenticated professor's details (provides professor id and role)
     * @return a list of active AlertDTOs containing test info, failure-threshold, and trigger state for that professor
     */
    @Operation(summary = "Get all active analytics alerts for the authenticated professor",
            description = "A teacher can get a list of all active analytics alerts for the tests they have created," +
                    " including the test information, failure rate threshold, and whether the alert is currently triggered.")
    @ApiResponse(responseCode = OK, description = "Analytics alerts returned successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the analytics alerts")
    @GetMapping("/professors/me/alerts")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AlertDTO>> getAlertsForProfessor(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID professorId = currentUser.getUserId();
        RoleName roleName = currentUser.getRoleName();
        return ResponseEntity.ok(failureRateService.getAlerts(professorId, roleName));
    }

    /**
     * Retrieve failure-rate chart data for every test in the specified course.
     *
     * The returned list contains a TestFailureRateChartDTO for each test, each providing daily failure rates suitable for trend visualization.
     *
     * @param courseId UUID of the course whose tests' failure-rate chart data will be returned
     * @return a list of TestFailureRateChartDTO objects, one per test, containing daily failure rates for that test
     */
    @Operation(summary = "Get failure rate chart data for each test in a specific course",
            description = "A teacher can get the failure rate chart data for all tests associated with a specific course they created. " +
                    "The chart data includes daily failure rates for each test, which can be used to visualize trends over time.")
    @ApiResponse(responseCode = OK, description = "Failure rate chart data returned successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the course analytics")
    @ApiResponse(responseCode = NOT_FOUND, description = "Course not found")
    @GetMapping("/courses/{courseId}/analytics/chart-data")
    @PreAuthorize("@accessService.canViewCourseFullView(authentication,#id)")
    public ResponseEntity<List<TestFailureRateChartDTO>> getFailureRateChartData(@P("id") @PathVariable UUID courseId, @AuthenticationPrincipal UserDetails currentUser) {
        UUID professorId = extractUserId(currentUser);
        return ResponseEntity.ok(failureRateService.getFailureCharts(courseId, professorId));
    }

    /**
     * Obtain the UUID of the authenticated user.
     *
     * If the principal is a CustomUserDetails, returns its userId; otherwise parses the principal's
     * username as a UUID.
     *
     * @param currentUser the authenticated principal provided by Spring Security
     * @return the authenticated user's UUID
     */
    private UUID extractUserId(UserDetails currentUser) {
        if (currentUser instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(currentUser.getUsername());
    }
}
