package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.model.QuestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAsyncWorkerTest {

    @Mock
    private AiApiClient aiApiClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AiQuestionRequestRepository repository;

    @InjectMocks
    private AiAsyncWorker aiAsyncWorker;

    @Test
    void processAiGenerationInBackground_shouldPersistSuccessPayload() throws Exception {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .status(AiRequestStatus.PENDING)
                .build();

        AiQuestionDto questionDto = new AiQuestionDto();
        questionDto.setText("Question");
        questionDto.setType(QuestionType.MULTI_CHOICE);
        questionDto.setAnswers(List.of("A", "B"));
        questionDto.setCorrectAnswers(List.of("A"));
        questionDto.setDifficulty(0.5);

        AiGenerateResponse response = new AiGenerateResponse();
        response.setQuestions(List.of(questionDto));

        when(aiApiClient.generateTest(lessonId, 4)).thenReturn(response);
        when(objectMapper.writeValueAsString(response.getQuestions())).thenReturn("[{\"text\":\"Question\"}]");

        aiAsyncWorker.processAiGenerationInBackground(4, lessonId, request);

        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(request.getGeneratedQuestions()).isEqualTo("[{\"text\":\"Question\"}]");
        verify(repository).save(request);
    }

    @Test
    void processAiGenerationInBackground_shouldMarkFallbackOnTimeout() {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .status(AiRequestStatus.PENDING)
                .build();

        when(aiApiClient.generateTest(lessonId, 4)).thenThrow(new AiTimeoutException("Timeout"));

        aiAsyncWorker.processAiGenerationInBackground(4, lessonId, request);

        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FALLBACK);
        assertThat(request.getGeneratedQuestions()).isNull();
        verify(repository).save(request);
    }

    @Test
    void processAiGenerationInBackground_shouldMarkFailedOnApiException() {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .status(AiRequestStatus.PENDING)
                .build();

        when(aiApiClient.generateTest(lessonId, 4)).thenThrow(new AiApiException("AI error"));

        aiAsyncWorker.processAiGenerationInBackground(4, lessonId, request);

        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        verify(repository).save(request);
    }

    @Test
    void processAiGenerationInBackground_shouldMarkFailedWhenSerializationFails() throws Exception {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .status(AiRequestStatus.PENDING)
                .build();

        AiQuestionDto questionDto = new AiQuestionDto();
        questionDto.setText("Question");
        questionDto.setType(QuestionType.MULTI_CHOICE);
        questionDto.setAnswers(List.of("A", "B"));
        questionDto.setCorrectAnswers(List.of("A"));
        questionDto.setDifficulty(0.5);

        AiGenerateResponse response = new AiGenerateResponse();
        response.setQuestions(List.of(questionDto));

        when(aiApiClient.generateTest(lessonId, 4)).thenReturn(response);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        aiAsyncWorker.processAiGenerationInBackground(4, lessonId, request);

        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        verify(repository).save(request);
    }
}
