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
import org.springdoc.core.annotations.ParameterObject;
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
    @Operation(
            summary = "Inject AI-generated questions into a test",
            description = """
                    Injects the generated questions from a completed AI request into a test.
                    
                    Important for clients:
                    - this endpoint must be called only after polling /api/v1/ai/requests/{requestId}/status returns DONE
                    - the endpoint path is singular: /api/v1/ai/request/{requestId}/inject
                    - if testIdOpt is missing or null, the backend creates a new DRAFT test automatically
                    - if testIdOpt is provided, the generated questions are injected into that existing test
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Questions injected successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "AI request or lesson or test not found"),
            @ApiResponse(responseCode = CONFLICT, description = "AI generation is not DONE yet or has failed"),
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
    @Operation(
            summary = "Start AI question generation for a lesson",
            description = """
                    Starts the asynchronous AI generation flow for a lesson and immediately returns a local requestId.
                    
                    Frontend flow:
                    1. call POST /api/v1/lessons/{lessonId}/ai/generate-test
                    2. poll GET /api/v1/ai/requests/{requestId}/status every 3-5 seconds
                    3. when status becomes DONE, call POST /api/v1/ai/request/{requestId}/inject
                    
                    Notes:
                    - the frontend should call the backend only, not the AI microservice directly
                    - this endpoint returns 202 Accepted and does not wait for the AI generation to finish
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = ACCEPTED, description = "AI generation request accepted; use the returned requestId for polling"),
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
    @Operation(
            summary = "Poll AI generation request status",
            description = """
                    Returns the current status of an AI generation request identified by the local requestId.
                    
                    Recommended client behavior:
                    - poll every 3-5 seconds
                    - stop when status becomes DONE or FAILED
                    - apply a client-side timeout, for example 5 minutes
                    
                    Returned statuses:
                    - PENDING: request exists but remote generation is not yet running
                    - RUNNING: AI generation is in progress
                    - DONE: generated questions are stored locally and can be injected into a test
                    - FAILED: generation failed; inspect the optional error field
                    """
    )
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

    @Operation(
            summary = "Get curriculum catalog",
            description = "Returns curriculum catalog filtered by optional query parameters: grade, subjectId and topicId."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Catalog returned successfully"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid query parameters"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Unauthorized"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied")
    })
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/ai/catalog/curriculum")
    public ResponseEntity<CurriculumCatalogResponseDto> getCurriculumCatalog(@ParameterObject CurriculumCatalogRequestDto requestDto) {
        return ResponseEntity.ok(aiService.getCurriculumCatalog(requestDto));
    }
}
