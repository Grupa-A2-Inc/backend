package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAsyncWorker {
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;
    private final AiQuestionRequestRepository repository;

    @Async
    public void processAiGenerationInBackground(UUID requestId, UUID lessonId, AiQuestionRequest request) {
        try {
            AiGenerateResponse response = aiApiClient.generateTest(requestId, lessonId);
            request.setStatus(AiRequestStatus.SUCCESS);
            List<AiQuestionDto> generatedQuestions = response.getQuestions();
            request.setGeneratedQuestions(objectMapper.writeValueAsString(generatedQuestions));
        }
        catch (AiTimeoutException exception) {
            request.setStatus(AiRequestStatus.FALLBACK);
        }
        catch (AiApiException | JsonProcessingException exception) {
            request.setStatus(AiRequestStatus.FAILED);
        }
        repository.save(request);
    }
}
