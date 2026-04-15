package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.service.TestResultService;
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

@RestController
@RequiredArgsConstructor
public class TestResultController {
    private final TestResultService testResultService;

    @Operation(summary = "Get the result of a test attempt, including score and question details")
    @ApiResponse(responseCode = "200", description = "Result retrieved successfully")
    @ApiResponse(responseCode = "403", description = "The attempt is still in progress")
    @ApiResponse(responseCode = "404", description = "Attempt not found or no results available")
    @GetMapping("/api/v1/attempts/{attemptId}/result")
    @PreAuthorize("@accessService.canViewAttemptResult(authentication,#id)")
    public ResponseEntity<AttemptReportDTO> getResult(@P("id") @PathVariable UUID attemptId,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);
        AttemptReportDTO response = testResultService.getTestResult(attemptId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get the list of attempts for a specific test, including score and status for each attempt")
    @ApiResponse(responseCode = "200", description = "Attempts retrieved successfully")
    @GetMapping("/api/v1/tests/{testId}/my-attempts")
    @PreAuthorize("@accessService.canViewMyTestAttempts(authentication,#id)")
    public ResponseEntity<List<AttemptStatusDTO>> getAttempts(@P("id") @PathVariable UUID testId ,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);

        List<AttemptStatusDTO> response = testResultService.getTestAttempts(testId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get the best finished attempt for a specific test by score percentage")
    @ApiResponse(responseCode = "200", description = "Best attempt retrieved successfully")
    @ApiResponse(responseCode = "404", description = "No finished attempts found for the test")
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
