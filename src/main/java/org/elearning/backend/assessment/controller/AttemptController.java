package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.*;
import org.elearning.backend.assessment.service.AttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "Start a test attempt", description = "Creates a new attempt for the specified test and returns the questions without correct answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt created successfully"),
            @ApiResponse(responseCode = "400", description = "Test is not PUBLISHED"),
            @ApiResponse(responseCode = "404", description = "Test not found")
    })
    @PostMapping("/api/tests/{testId}/start")
    public ResponseEntity<StartAttemptResponseDto> startAttempt(@PathVariable UUID testId) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        StartAttemptResponseDto response = attemptService.startAttempt(testId, studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit attempt answers", description = "Processes the student's answers, calculates the score, and saves the result.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Attempt not found"),
            @ApiResponse(responseCode = "409", description = "Attempt already submitted"),
            @ApiResponse(responseCode = "410", description = "Timer expired")
    })
    @PostMapping("/api/attempts/{attemptId}/submit")
    public ResponseEntity<TestResultDto> submitAttempt(
            @PathVariable UUID attemptId,
            @RequestBody SubmitRequestDto request) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        TestResultDto result = attemptService.submitAttempt(attemptId, studentId, request);
        return ResponseEntity.ok(result);
    }
}