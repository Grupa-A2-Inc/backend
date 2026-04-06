package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.assigment_dto.TestResultDto;
import org.elearning.backend.assessment.dto.test_dto.StartAttemptResponseDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.assessment.service.AttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AttemptController {

    private final AttemptService attemptService;
    private static String hardcodedId = "00000000-0000-0000-0000-000000000001";

    private static final String OK = "201";
    private static final String NOT_FOUND = "404";
    private static final String CONFLICT = "409";
    private static final String BAD_REQUEST = "400";
    private static final String GONE = "410";

    @Operation(summary = "Start a test attempt", description = "Creates a new attempt for the specified test and returns the questions without correct answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Attempt created successfully"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Test is not PUBLISHED"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Test not found")
    })
    @PostMapping("/tests/{testId}/start")
    public ResponseEntity<StartAttemptResponseDto> startAttempt(@PathVariable UUID testId) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString(hardcodedId);

        StartAttemptResponseDto response = attemptService.startAttempt(testId, studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit attempt answers", description = "Processes the student's answers, calculates the score, and saves the result.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Attempt submitted successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Attempt not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Attempt already submitted"),
            @ApiResponse(responseCode = GONE, description = "Timer expired")
    })
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<TestResultDto> submitAttempt(
            @PathVariable UUID attemptId,
            @RequestBody SubmitRequestDto request) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString(hardcodedId);

        TestResultDto result = attemptService.submitAttempt(attemptId, studentId, request);
        return ResponseEntity.ok(result);
    }
}