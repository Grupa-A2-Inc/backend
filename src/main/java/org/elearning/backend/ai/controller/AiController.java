package org.elearning.backend.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.service.AiGenerationService;
import org.elearning.backend.ai.service.AiQuestionInjectorService;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "AI Questions", description = "AI-generated questions")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AiController extends GlobalHttpStatusCodes {
    private final AiQuestionInjectorService aiQuestionInjectorService;
    private final AiGenerationService aiService;

    /**
     * Injects AI-generated questions from the specified AI request into a test.
     *
     * @param requestId   the UUID of the AI generation request to process
     * @param requestBody optional payload containing an optional `testIdOpt`; when absent, a new test will be created and questions will be injected into it
     * @return            an InjectionResultDto describing the outcome of the injection operation
     */
    @Operation(summary = "Inject AI-generated questions into a test", description = "Processes the AI request and injects generated questions into the specified test. If no test ID is provided, a new test will be created.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Questions injected successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "AI request or lesson or test not found"),
            @ApiResponse(responseCode = CONFLICT, description = "AI Generation failed or is not completed yet"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in request or error at parsing generated questions")
    })

    @PostMapping("/ai/request/{requestId}/inject")
    @PreAuthorize("@accessService.canInjectAiQuestions(authentication,#id)")
    public ResponseEntity<InjectionResultDto> injectAiQuestions(@P("id") @PathVariable UUID requestId,
                                                                @RequestBody(required = false) InjectRequestDto requestBody,
                                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID professorId = currentUser.getUserId();
        UUID testIdOpt = (requestBody != null) ? requestBody.getTestIdOpt() : null;

        return ResponseEntity.ok(aiQuestionInjectorService.injectQuestions(requestId, professorId, testIdOpt));
    }

    /**
     * Initiates an AI generation request for the specified lesson and returns the created request metadata.
     *
     * @param requestDto request body carrying generation parameters; its `subjectId` and `topicId` are used to scope the generation
     * @return an AiGenerateResponseDto containing the generated `requestId`, `status` set to `AiRequestStatus.PENDING`, and the associated `lessonId`
     */
    // SWAGGER ADDED
    @Operation(summary = "Generate AI test for a lesson", description = "Initiates an AI generation request for the specified lesson and returns the created request metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = ACCEPTED, description = "AI generation request accepted"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid request parameters"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found"),
    })
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')")
    @PostMapping("/lessons/{lessonId}/ai/generate-test")
    public ResponseEntity<AiGenerateResponseDto> generateForLesson(@PathVariable UUID lessonId, @RequestBody AiGenerateRequestDto requestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        RoleName role=userDetails.getRoleName();

        return ResponseEntity.accepted().body(aiService.generateTestForLesson(requestDto, lessonId, userId, role));
    }

    /**
     * Retrieve the current status of an AI generation request by its ID for the authenticated user.
     *
     * @param requestId the UUID of the AI generation request to query
     * @param userDetails the authenticated principal used to identify the requesting user
     * @return the AiRequestStatusDto containing the request's current status and related metadata
     */
    // SWAGGER ADDED
    @Operation(summary = "Get AI generation request status", description = "Retrieves the current status of an AI generation request by its ID for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Status retrieved successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "AI generation request not found"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied")
    })
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')")
    @GetMapping("/ai/requests/{requestId}/status")
    public ResponseEntity<AiRequestStatusDto> getRequestStatus(@PathVariable UUID requestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        RoleName role = userDetails.getRoleName();
        return ResponseEntity.ok(aiService.getRequestStatus(requestId, userId, role));
    }
}
