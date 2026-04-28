package org.elearning.backend.ai.controller;

// SWAGGER ADDED
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiGenerateRequestDto;
import org.elearning.backend.ai.dto.AiGenerateResponseDto;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.ai.service.AiGenerationService;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "AI Generation", description = "AI test generation requests and status")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiGenerationController {
    private final AiGenerationService aiService;

    /**
     * Initiates an AI generation request for the specified lesson and returns the created request metadata.
     *
     * @param requestDto request body carrying generation parameters; its `subjectId` and `topicId` are used to scope the generation
     * @return an AiGenerateResponseDto containing the generated `requestId`, `status` set to `AiRequestStatus.PENDING`, and the associated `lessonId`
     */
    // SWAGGER ADDED
    @Operation(summary = "Generate AI test for a lesson", description = "Initiates an AI generation request for the specified lesson and returns the created request metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "AI generation request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Lesson not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during generation")
    })
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')")
    @PostMapping("/lessons/{lessonId}/ai/generate-test")
    public ResponseEntity<AiGenerateResponseDto> generateForLesson(@PathVariable UUID lessonId, @RequestBody AiGenerateRequestDto requestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        RoleName role=userDetails.getRoleName();
        Integer subjectId=requestDto.getSubjectId();
        Integer topicId=requestDto.getTopicId();

        UUID requestId = aiService.generateForLesson(lessonId, userId, role, subjectId, topicId);

        AiGenerateResponseDto responseDto = new AiGenerateResponseDto();
        responseDto.setRequestId(requestId);
        responseDto.setStatus(AiRequestStatus.PENDING);
        responseDto.setLessonId(lessonId);

        return ResponseEntity.accepted().body(responseDto);
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
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "AI generation request not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')")
    @GetMapping("/ai/requests/{requestId}/status")
    public ResponseEntity<AiRequestStatusDto> getRequestStatus(@PathVariable UUID requestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        RoleName role = userDetails.getRoleName();
        return ResponseEntity.ok(aiService.getRequestStatus(requestId, userId, role));
    }
}
