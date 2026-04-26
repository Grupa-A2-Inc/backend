package org.elearning.backend.ai.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiGenerateRequestDto;
import org.elearning.backend.ai.dto.AiGenerateResponseDto;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.ai.service.AiGenerationService;
import org.elearning.backend.analytics.model.AiRequestStatus;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')")
    @GetMapping("/ai/requests/{requestId}/status")
    public ResponseEntity<AiRequestStatusDto> getRequestStatus(@PathVariable UUID requestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        RoleName role = userDetails.getRoleName();
        return ResponseEntity.ok(aiService.getRequestStatus(requestId, userId, role));
    }
}
