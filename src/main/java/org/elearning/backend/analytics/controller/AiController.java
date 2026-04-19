package org.elearning.backend.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.InjectRequestDto;
import org.elearning.backend.analytics.dto.InjectionResultDto;
import org.elearning.backend.analytics.service.AiQuestionInjectorService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AiController {
    private final AiQuestionInjectorService aiQuestionInjectorService;

    @PostMapping("/ai/request/{requestId}/inject")
    public ResponseEntity<InjectionResultDto> injectAiQuestions(@PathVariable UUID requestId,
                                                                @RequestBody(required = false) InjectRequestDto requestBody,
                                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID professorId = currentUser.getUserId();

        UUID testIdOpt = (requestBody != null) ? requestBody.getTestIdOpt() : null;

        return ResponseEntity.ok(aiQuestionInjectorService.injectQuestions(requestId, professorId, testIdOpt));
    }
}
