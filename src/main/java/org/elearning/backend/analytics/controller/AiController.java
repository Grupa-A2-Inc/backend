package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.InjectRequestDto;
import org.elearning.backend.analytics.dto.InjectionResultDto;
import org.elearning.backend.analytics.service.AiQuestionInjectorService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AiController {
    private final AiQuestionInjectorService aiQuestionInjectorService;

    private static final String OK = "200";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";
    private static final String CONFLICT = "409";
    private static final String UNPROCESSABLE_CONTENT = "422";

    @Operation(summary = "Inject AI-generated questions into a test", description = "Processes the AI request and injects generated questions into the specified test. If no test ID is provided, a new test will be created.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Questions injected successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "AI request or lesson or test not found"),
            @ApiResponse(responseCode = CONFLICT, description = "AI Generation failed or is not completed yet"),
            @ApiResponse(responseCode = UNPROCESSABLE_CONTENT, description = "Validation error in request or error at parsing generated questions")
    })

    @PostMapping("/ai/request/{requestId}/inject")
    public ResponseEntity<InjectionResultDto> injectAiQuestions(@PathVariable UUID requestId,
                                                                @RequestBody(required = false) InjectRequestDto requestBody,
                                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID professorId = currentUser.getUserId();
        UUID testIdOpt = (requestBody != null) ? requestBody.getTestIdOpt() : null;

        return ResponseEntity.ok(aiQuestionInjectorService.injectQuestions(requestId, professorId, testIdOpt));
    }
}
