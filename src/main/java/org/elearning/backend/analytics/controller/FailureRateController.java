package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.alerts.AlertDTO;
import org.elearning.backend.analytics.dto.alerts.ThresholdDTO;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateDTO;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FailureRateController {
    private static final String OK = "200";

    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final FailureRateService failureRateService;

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

    @Operation(summary = "Get all active analytics alerts for the authenticated professor",
            description = "A teacher can get a list of all active analytics alerts for the tests they have created," +
                    " including the test information, failure rate threshold, and whether the alert is currently triggered.")
    @ApiResponse(responseCode = OK, description = "Analytics alerts returned successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the analytics alerts")
    @GetMapping("/professors/me/alerts")
    //Nu stiu ce sa pun aici la PreAuthorize
    public ResponseEntity<List<AlertDTO>> getAlertsForProfessor(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID professorId = currentUser.getUserId();
        RoleName roleName = currentUser.getRoleName();
        return ResponseEntity.ok(failureRateService.getAlerts(professorId, roleName));
    }



    private UUID extractUserId(UserDetails currentUser) {
        if (currentUser instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(currentUser.getUsername());
    }
}
