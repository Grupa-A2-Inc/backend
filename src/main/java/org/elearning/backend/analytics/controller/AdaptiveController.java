package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.AdaptiveResultDto;
import org.elearning.backend.analytics.dto.AdaptiveSubmitRequestDto;
import org.elearning.backend.analytics.service.AdaptiveSubmitService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdaptiveController {
    private final AdaptiveSubmitService adaptiveSubmitService;

    private static final String OK = "200";
    private static final String NOT_FOUND = "404";
    private static final String CONFLICT = "409";
    private static final String UNPROCESSABLE_CONTENT = "422";

    @Operation(summary = "Submit an adaptive session", description = "Processes the student's answers for an adaptive session, calculates the total score, and returns detailed results for each question along with AI feedback status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Session submitted successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Session not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Session is not active or has expired"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in submitted answers")
    })

    @PostMapping("/adaptive/sessions/{sessionId}/submit")
    public ResponseEntity<AdaptiveResultDto> submitAdaptiveSession(
            @PathVariable UUID sessionId,
            @RequestBody AdaptiveSubmitRequestDto body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(adaptiveSubmitService.submitSession(sessionId, currentUser.getUserId(), body));
    }
}
