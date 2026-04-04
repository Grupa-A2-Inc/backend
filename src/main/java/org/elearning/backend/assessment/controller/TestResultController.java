package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.AttemptStatusDTO;
import org.elearning.backend.assessment.service.TestResultService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AttemptReportDTO> getResult(@PathVariable UUID attemptId) {
        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        AttemptReportDTO response = testResultService.getTestResult(attemptId, studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get the list of attempts for a specific test, including score and status for each attempt")
    @ApiResponse(responseCode = "200", description = "Attempts retrieved successfully")
    @GetMapping("/api/v1/tests/{testId}/my-attempts")
    public ResponseEntity<List<AttemptStatusDTO>> getAttempts(@PathVariable UUID testId) {
        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        List<AttemptStatusDTO> response = testResultService.getTestAttempts(testId, studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get the best finished attempt for a specific test by score percentage")
    @ApiResponse(responseCode = "200", description = "Best attempt retrieved successfully")
    @ApiResponse(responseCode = "404", description = "No finished attempts found for the test")
    @GetMapping("/api/v1/tests/{testId}/my-best")
    public ResponseEntity<AttemptStatusDTO> getBestAttempt(@PathVariable UUID testId) {
        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        AttemptStatusDTO response = testResultService.getBestTestAttempt(testId, studentId);
        return ResponseEntity.ok(response);
    }
}
