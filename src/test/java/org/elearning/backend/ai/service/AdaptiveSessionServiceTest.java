package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AdaptiveServiceUnavailableException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AdaptiveExerciseJob;
import org.elearning.backend.ai.model.AdaptiveSession;
import org.elearning.backend.ai.model.AdaptiveSessionExercise;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AdaptiveExerciseJobRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveSessionServiceTest {

    @Mock private AdaptiveExerciseJobRepository adaptiveExerciseJobRepository;
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

    @Test
    void createAdaptiveJob_shouldPersistRemoteJobIdAndStatus() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> {
            AdaptiveExerciseJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobId);
            }
            return job;
        });

        AiAdaptiveJobResponse remoteResponse = new AiAdaptiveJobResponse();
        remoteResponse.setJobId("adaptive-job-1");
        remoteResponse.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startAdaptiveJob(studentId, 1, 2, 4)).thenReturn(remoteResponse);

        AdaptiveJobResponseDto response = adaptiveSessionService.createAdaptiveJob(studentId, 1, 2, 4);

        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.PENDING);
        verify(adaptiveExerciseJobRepository, times(2)).save(any(AdaptiveExerciseJob.class));
    }

    @Test
    void getAdaptiveJobStatus_shouldMaterializeSessionWhenRemoteJobIsDone() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .questionCount(3)
                .aiJobId("adaptive-job-2")
                .status(AiRequestStatus.RUNNING)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAdaptiveExerciseDto exercise = new AiAdaptiveExerciseDto();
        exercise.setExerciseId("ex-1");
        exercise.setText("Question");
        exercise.setType(QuestionType.SINGLE_CHOICE);
        exercise.setAnswers(List.of("A", "B"));
        exercise.setCorrectAnswers(List.of("A"));
        exercise.setDifficulty(0.6);

        AiAdaptiveJobStatusResponse remoteStatus = new AiAdaptiveJobStatusResponse();
        remoteStatus.setJobId("adaptive-job-2");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setExercises(List.of(exercise));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-2")).thenReturn(remoteStatus);

        when(adaptiveSessionRepository.save(any(AdaptiveSession.class))).thenAnswer(invocation -> {
            AdaptiveSession session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });
        when(objectMapper.writeValueAsString(anyList())).thenReturn("[\"A\"]");
        when(adaptiveSessionRepository.findById(sessionId)).thenReturn(Optional.of(
                AdaptiveSession.builder()
                        .id(sessionId)
                        .studentId(studentId)
                        .subjectId(1)
                        .topicId(2)
                        .expiresAt(java.time.LocalDateTime.now().plusMinutes(30))
                        .build()
        ));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(
                AdaptiveSessionExercise.builder()
                        .sessionId(sessionId)
                        .mlExerciseId("ex-1")
                        .exerciseText("Question")
                        .exerciseType("SINGLE_CHOICE")
                        .answersRaw("[\"A\",\"B\"]")
                        .correctAnswersRaw("[\"A\"]")
                        .build()
        ));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A", "B"));

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.DONE);
        assertThat(status.getSession()).isNotNull();
        assertThat(status.getSession().getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void getAdaptiveJobStatus_shouldReuseExistingSessionWhenAlreadyMaterialized() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .questionCount(3)
                .aiJobId("adaptive-job-3")
                .status(AiRequestStatus.DONE)
                .sessionId(sessionId)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveSessionRepository.findById(sessionId)).thenReturn(Optional.of(
                AdaptiveSession.builder()
                        .id(sessionId)
                        .studentId(studentId)
                        .subjectId(1)
                        .topicId(2)
                        .expiresAt(java.time.LocalDateTime.now().plusMinutes(30))
                        .build()
        ));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of());

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.DONE);
        assertThat(status.getSession().getSessionId()).isEqualTo(sessionId);
        verify(aiApiClient, org.mockito.Mockito.never()).getAdaptiveJobStatus(any());
    }
}
