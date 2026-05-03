package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.assigment_dto.TestResultDto;
import org.elearning.backend.assessment.dto.test_dto.StartAttemptResponseDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.assessment.service.AttemptService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Attempts", description = "Test attempt lifecycle management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AttemptController {

    private final AttemptService attemptService;

    private static final String OK = "200";
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
    @PreAuthorize("@accessService.canStartTest(authentication,#id)")
    public ResponseEntity<StartAttemptResponseDto> startAttempt(@AuthenticationPrincipal CustomUserDetails userDetails, @P("id") @PathVariable UUID testId) {
        return ResponseEntity.ok(attemptService.startAttempt(testId, userDetails.getUserId()));
    }

    @Operation(summary = "Submit attempt answers", description = "Processes the student's answers, calculates the score, and saves the result.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Attempt submitted successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Attempt not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Attempt already submitted"),
            @ApiResponse(responseCode = GONE, description = "Timer expired")
    })
    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("@accessService.canSubmitAttempt(authentication,#id)")
    public ResponseEntity<TestResultDto> submitAttempt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @P("id") @PathVariable UUID attemptId,
            @RequestBody SubmitRequestDto request) {
        return ResponseEntity.ok(attemptService.submitAttempt(attemptId, userDetails.getUserId(), request));
    }
}