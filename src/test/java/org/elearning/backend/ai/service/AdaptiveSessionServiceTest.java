package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AdaptiveServiceUnavailableException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AdaptiveSession;
import org.elearning.backend.ai.repository.AdaptiveSessionAnswerRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionExerciseRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionRepository;
import org.elearning.backend.assessment.model.QuestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveSessionServiceTest {

    @Mock private AdaptiveSessionRepository adaptiveSessionRepository;
    @Mock private AiApiClient aiApiClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private AdaptiveSessionExerciseRepository exerciseRepository;
    @Mock private AdaptiveSessionAnswerRepository adaptiveSessionAnswerRepository;

    @InjectMocks
    private AdaptiveSessionService adaptiveSessionService;

    @Test
    void startSession_shouldThrowWhenAiReturnsNullExercises() {
        UUID studentId = UUID.randomUUID();
        AiAdaptiveResponse response = new AiAdaptiveResponse();
        response.setExercises(null);
        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(response);

        assertThatThrownBy(() -> adaptiveSessionService.startSession(studentId, 1, 2, 3))
                .isInstanceOf(AdaptiveServiceUnavailableException.class)
                .hasMessageContaining("No exercises available");
    }

    @Test
    void startSession_shouldThrowWhenAiReturnsEmptyExercises() {
        UUID studentId = UUID.randomUUID();
        AiAdaptiveResponse response = new AiAdaptiveResponse();
        response.setExercises(List.of());
        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(response);

        assertThatThrownBy(() -> adaptiveSessionService.startSession(studentId, 1, 2, 3))
                .isInstanceOf(AdaptiveServiceUnavailableException.class)
                .hasMessageContaining("No exercises available");
    }

    @Test
    void startSession_shouldThrowWhenExerciseSerializationFails() throws Exception {
        UUID studentId = UUID.randomUUID();
        AiAdaptiveExerciseDto exercise = new AiAdaptiveExerciseDto();
        exercise.setExerciseId("ex-1");
        exercise.setText("Question");
        exercise.setType(QuestionType.SINGLE_CHOICE);
        exercise.setAnswers(List.of("A", "B"));
        exercise.setCorrectAnswers(List.of("A"));
        exercise.setDifficulty(1.0);

        AiAdaptiveResponse response = new AiAdaptiveResponse();
        response.setExercises(List.of(exercise));

        AdaptiveSession savedSession = AdaptiveSession.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .build();

        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(response);
        when(adaptiveSessionRepository.save(any(AdaptiveSession.class))).thenReturn(savedSession);
        when(objectMapper.writeValueAsString(anyList())).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> adaptiveSessionService.startSession(studentId, 1, 2, 3))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to process exercise data.");
    }
}
