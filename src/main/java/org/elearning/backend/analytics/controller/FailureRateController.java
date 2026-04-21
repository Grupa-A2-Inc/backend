package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.statistics.teacher.FailureRateDTO;
import org.elearning.backend.analytics.service.FailureRateService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private UUID extractUserId(UserDetails currentUser) {
        if (currentUser instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(currentUser.getUsername());
    }
}
