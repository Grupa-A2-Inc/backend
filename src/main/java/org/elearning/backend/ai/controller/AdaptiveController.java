package org.elearning.backend.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AdaptiveResultDto;
import org.elearning.backend.ai.dto.AdaptiveJobResponseDto;
import org.elearning.backend.ai.dto.AdaptiveJobStatusDto;
import org.elearning.backend.ai.dto.AdaptiveStartDto;
import org.elearning.backend.ai.dto.AdaptiveStartRequestDto;
import org.elearning.backend.ai.dto.AdaptiveSubmitRequestDto;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.ai.service.AdaptiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Adaptive Sessions", description = "Adaptive learning session management")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdaptiveController extends GlobalHttpStatusCodes {
    private final AdaptiveSessionService adaptiveSessionService;

    /**
     * Submit a student's answers for an adaptive session and return the computed results.
     * Processes the submitted answers, calculates the total score, and returns per-question details
     * along with AI feedback status.
     *
     * @param sessionId the UUID of the adaptive session to submit
     * @param body the submitted answers and related submission data
     * @param currentUser the authenticated user's details submitting the session
     * @return an AdaptiveResultDto containing the total score, detailed results for each question, and AI feedback status
     */
    @Operation(
            summary = "Submit an adaptive session",
            description = "Submits the student's answers for a previously created adaptive session. " +
                    "The session must still be ACTIVE and not expired. " +
                    "The response contains the total score, per-exercise grading details and whether AI feedback sync succeeded."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = OK,
                    description = "Session submitted successfully",
                    content = @Content(
                            schema = @Schema(implementation = AdaptiveResultDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                                      "totalScore": 8.5,
                                      "clientResults": [
                                        {
                                          "mlExerciseId": "ex-1",
                                          "correct": true,
                                          "score": 1.0,
                                          "correctAnswers": ["Paris"],
                                          "givenAnswers": ["Paris"]
                                        }
                                      ],
                                      "feedbackSent": true
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = NOT_FOUND, description = "Session not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Session is not active or has expired"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in submitted answers")
    })
    @PostMapping("/adaptive/sessions/{sessionId}/submit")
    @PreAuthorize("@accessService.canSubmitAdaptiveSession(authentication,#id)")
    public ResponseEntity<AdaptiveResultDto> submitAdaptiveSession(
            @Parameter(description = "Adaptive session identifier returned by the start endpoint or the adaptive job status endpoint.", required = true)
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
    @Operation(
            summary = "Start an adaptive session synchronously",
            description = "Legacy synchronous endpoint. " +
                    "It creates the adaptive session and returns all exercises in the same HTTP response. " +
                    "Use `/api/v1/adaptive/jobs` for the recommended timeout-safe async flow."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = OK,
                    description = "Adaptive session started successfully",
                    content = @Content(
                            schema = @Schema(implementation = AdaptiveStartDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                                      "expiresAt": "2026-05-20T10:45:00",
                                      "exercises": [
                                        {
                                          "exerciseId": "ex-1",
                                          "text": "What is the capital of France?",
                                          "type": "SINGLE_CHOICE",
                                          "answers": ["Paris", "Lyon", "Marseille", "Bordeaux"]
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = NOT_FOUND, description = "Subject or topic not found"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in request parameters")
    })
    @PostMapping("/adaptive/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AdaptiveStartDto> startAdaptiveSession(@RequestBody AdaptiveStartRequestDto request, @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        UUID studentId = userDetails.getUserId();
        return ResponseEntity.ok(adaptiveSessionService.startSession(studentId, request.getSubjectId(), request.getTopicId(), request.getCount()));
    }

    @Operation(
            summary = "Create an adaptive exercise generation job",
            description = "Recommended async endpoint for adaptive generation. " +
                    "Creates a backend job immediately and returns a local `jobId`. " +
                    "Poll `GET /api/v1/adaptive/jobs/{jobId}` until the status becomes `DONE`. " +
                    "When that happens, the polling response contains the full adaptive session payload."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = ACCEPTED,
                    description = "Adaptive job accepted",
                    content = @Content(
                            schema = @Schema(implementation = AdaptiveJobResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "jobId": "550e8400-e29b-41d4-a716-446655440000",
                                      "status": "PENDING"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in request parameters")
    })
    @PostMapping("/adaptive/jobs")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AdaptiveJobResponseDto> createAdaptiveJob(
            @RequestBody AdaptiveStartRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID studentId = userDetails.getUserId();
        return ResponseEntity.accepted()
                .body(adaptiveSessionService.createAdaptiveJob(studentId, request.getSubjectId(), request.getTopicId(), request.getCount()));
    }

    @Operation(
            summary = "Get adaptive exercise generation job status",
            description = "Polling endpoint for adaptive generation. " +
                    "Returns `PENDING` or `RUNNING` while the AI pipeline is still generating the remaining exercises. " +
                    "Returns `DONE` together with the full adaptive session once the session has been materialized locally. " +
                    "Returns `FAILED` and an error message if the generation pipeline failed."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = OK,
                    description = "Adaptive job status returned",
                    content = @Content(
                            schema = @Schema(implementation = AdaptiveJobStatusDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Running",
                                            value = """
                                                    {
                                                      "jobId": "550e8400-e29b-41d4-a716-446655440000",
                                                      "status": "RUNNING",
                                                      "error": null,
                                                      "session": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Done",
                                            value = """
                                                    {
                                                      "jobId": "550e8400-e29b-41d4-a716-446655440000",
                                                      "status": "DONE",
                                                      "error": null,
                                                      "session": {
                                                        "sessionId": "9a1d6a26-a837-4d4f-bfef-9d2f2ebf7c3b",
                                                        "expiresAt": "2026-05-20T10:45:00",
                                                        "exercises": [
                                                          {
                                                            "exerciseId": "ex-1",
                                                            "text": "What is the capital of France?",
                                                            "type": "SINGLE_CHOICE",
                                                            "answers": ["Paris", "Lyon", "Marseille", "Bordeaux"]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Failed",
                                            value = """
                                                    {
                                                      "jobId": "550e8400-e29b-41d4-a716-446655440000",
                                                      "status": "FAILED",
                                                      "error": "Adaptive AI returned an invalid response.",
                                                      "session": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = NOT_FOUND, description = "Adaptive job not found")
    })
    @GetMapping("/adaptive/jobs/{jobId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AdaptiveJobStatusDto> getAdaptiveJobStatus(
            @Parameter(description = "Local backend adaptive job identifier returned by `POST /api/v1/adaptive/jobs`.", required = true)
            @PathVariable UUID jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adaptiveSessionService.getAdaptiveJobStatus(jobId, userDetails.getUserId()));
    }
}
