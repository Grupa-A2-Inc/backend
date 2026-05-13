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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void marksRequestSuccessAndStoresSerializedQuestions() throws Exception {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = new AiQuestionRequest();
        AiGenerateResponse response = new AiGenerateResponse();
        response.setQuestions(List.of(new AiQuestionDto()));
        when(aiApiClient.generateTest(lessonId, 3)).thenReturn(response);
        when(objectMapper.writeValueAsString(response.getQuestions())).thenReturn("[{}]");

        aiAsyncWorker.processAiGenerationInBackground(3, lessonId, request);

        ArgumentCaptor<AiQuestionRequest> captor = ArgumentCaptor.forClass(AiQuestionRequest.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(captor.getValue().getGeneratedQuestions()).isEqualTo("[{}]");
    }

    @Test
    void marksRequestFallbackOnTimeout() {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = new AiQuestionRequest();
        when(aiApiClient.generateTest(lessonId, 2)).thenThrow(new AiTimeoutException("timeout"));

        aiAsyncWorker.processAiGenerationInBackground(2, lessonId, request);

        verify(repository).save(request);
        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FALLBACK);
    }

    @Test
    void marksRequestFailedOnAiApiException() {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = new AiQuestionRequest();
        when(aiApiClient.generateTest(lessonId, 2)).thenThrow(new AiApiException("failed"));

        aiAsyncWorker.processAiGenerationInBackground(2, lessonId, request);

        verify(repository).save(request);
        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FAILED);
    }

    @Test
    void marksRequestFailedOnSerializationException() throws Exception {
        UUID lessonId = UUID.randomUUID();
        AiQuestionRequest request = new AiQuestionRequest();
        AiGenerateResponse response = new AiGenerateResponse();
        response.setQuestions(List.of(new AiQuestionDto()));
        when(aiApiClient.generateTest(lessonId, 2)).thenReturn(response);
        when(objectMapper.writeValueAsString(response.getQuestions()))
                .thenThrow(new JsonProcessingException("bad json") {});

        aiAsyncWorker.processAiGenerationInBackground(2, lessonId, request);

        verify(repository).save(request);
        assertThat(request.getStatus()).isEqualTo(AiRequestStatus.FAILED);
    }
}
