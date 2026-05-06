package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.service.TestResultService;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Test Results", description = "Test attempt results and history")
@RestController
@RequiredArgsConstructor
public class TestResultController extends GlobalHttpStatusCodes {
    private final TestResultService testResultService;

    /**
     * Retrieve the result of a specific test attempt for the authenticated user.
     *
     * @param attemptId  UUID of the attempt to fetch results for
     * @param currentUser  the authenticated principal used to determine the requesting user's id
     * @return  an AttemptReportDTO containing the attempt score and per-question details
     */
    @Operation(summary = "Get the result of a test attempt, including score and question details")
    @ApiResponse(responseCode = OK, description = "Result retrieved successfully")
    @ApiResponse(responseCode = FORBIDDEN, description = "The attempt is still in progress")
    @ApiResponse(responseCode = NOT_FOUND, description = "Attempt not found or no results available")
    @GetMapping("/api/v1/attempts/{attemptId}/result")
    @PreAuthorize("@accessService.canViewAttemptResult(authentication,#id)")
    public ResponseEntity<AttemptReportDTO> getResult(@P("id") @PathVariable UUID attemptId,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);
        AttemptReportDTO response = testResultService.getTestResult(attemptId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the authenticated user's attempts for a given test, including score and status for each attempt.
     *
     * @param testId the UUID of the test whose attempts to retrieve
     * @return a list of AttemptStatusDTO containing score and status for each attempt
     */
    @Operation(summary = "Get the list of attempts for a specific test, including score and status for each attempt")
    @ApiResponse(responseCode = OK, description = "Attempts retrieved successfully")
    @GetMapping("/api/v1/tests/{testId}/my-attempts")
    @PreAuthorize("@accessService.canViewMyTestAttempts(authentication,#id)")
    public ResponseEntity<List<AttemptStatusDTO>> getAttempts(@P("id") @PathVariable UUID testId ,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);

        List<AttemptStatusDTO> response = testResultService.getTestAttempts(testId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the authenticated user's best finished attempt for the specified test by score percentage.
     *
     * @param testId the UUID of the test to query
     * @return the best finished AttemptStatusDTO for the authenticated user and specified test; if no finished attempts exist for the test a 404 response is produced
     */
    @Operation(summary = "Get the best finished attempt for a specific test by score percentage")
    @ApiResponse(responseCode = OK, description = "Best attempt retrieved successfully")
    @ApiResponse(responseCode = NOT_FOUND, description = "No finished attempts found for the test")
    @GetMapping("/api/v1/tests/{testId}/my-best")
    @PreAuthorize("@accessService.canViewMyBestTestResult(authentication,#id)")
    public ResponseEntity<AttemptStatusDTO> getBestAttempt(@P("id") @PathVariable UUID testId,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);


        AttemptStatusDTO response = testResultService.getBestTestAttempt(testId, userId);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(UserDetails currentUser) {
        if (currentUser instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(currentUser.getUsername());
    }
}
