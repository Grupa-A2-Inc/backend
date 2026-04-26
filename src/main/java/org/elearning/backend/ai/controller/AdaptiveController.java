package org.elearning.backend.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AdaptiveResultDto;
import org.elearning.backend.ai.dto.AdaptiveStartDto;
import org.elearning.backend.ai.dto.AdaptiveStartRequestDto;
import org.elearning.backend.ai.dto.AdaptiveSubmitRequestDto;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.ai.service.AdaptiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdaptiveController {
    private final AdaptiveSessionService adaptiveSessionService;

    private static final String OK = "200";
    private static final String NOT_FOUND = "404";
    private static final String CONFLICT = "409";
    private static final String UNPROCESSABLE_CONTENT = "422";

    /**
     * Submit a student's answers for an adaptive session and return the computed results.
     *
     * Processes the submitted answers, calculates the total score, and returns per-question details
     * along with AI feedback status.
     *
     * @param sessionId the UUID of the adaptive session to submit
     * @param body the submitted answers and related submission data
     * @param currentUser the authenticated user's details submitting the session
     * @return an AdaptiveResultDto containing the total score, detailed results for each question, and AI feedback status
     */
    @Operation(summary = "Submit an adaptive session", description = "Processes the student's answers for an adaptive session, calculates the total score, and returns detailed results for each question along with AI feedback status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Session submitted successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Session not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Session is not active or has expired"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in submitted answers")
    })

    @PostMapping("/adaptive/sessions/{sessionId}/submit")
    @PreAuthorize("@accessService.canSubmitAdaptiveSession(authentication,#id)")
    public ResponseEntity<AdaptiveResultDto> submitAdaptiveSession(
            @P("id") @PathVariable UUID sessionId,
            @RequestBody AdaptiveSubmitRequestDto body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(adaptiveSessionService.submitSession(sessionId, currentUser.getUserId(), body));
    }

    /**
     * Starts a new adaptive session for the authenticated student using the specified subject, topic, and question count.
     *
     * @param request     contains the subjectId, topicId, and desired question count for the new session
     * @param userDetails authentication principal for the current user; the student's UUID is taken from this object
     * @return an AdaptiveStartDto describing the created adaptive session
     */
    @PostMapping("/adaptive/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AdaptiveStartDto> startAdaptiveSession(@RequestBody AdaptiveStartRequestDto request, @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        UUID studentId = userDetails.getUserId();
        return ResponseEntity.ok(adaptiveSessionService.startSession(studentId, request.getSubjectId(), request.getTopicId(), request.getCount()));
    }
}
