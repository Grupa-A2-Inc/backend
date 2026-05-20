package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.AdaptiveServiceUnavailableException;
import org.elearning.backend.ai.exception.ResourceConflictException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AdaptiveSessionAnswer;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

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

    @Test
    void getAdaptiveJobStatus_shouldNotSyncWhenStatusIsFailed() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .status(AiRequestStatus.FAILED)
                .aiJobId("adaptive-job-terminal-failed")
                .errorMessage("failed")
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        verify(aiApiClient, never()).getAdaptiveJobStatus(any());
    }

    @Test
    void getAdaptiveJobStatus_shouldNotSyncWhenAiJobIdIsMissing() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId(null)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.RUNNING);
        verify(aiApiClient, never()).getAdaptiveJobStatus(any());
    }

    @Test
    void getAdaptiveJobStatus_shouldReturnDoneWithoutSessionWhenSessionIdIsNull() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .status(AiRequestStatus.DONE)
                .sessionId(null)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.DONE);
        assertThat(status.getSession()).isNull();
        verify(aiApiClient, never()).getAdaptiveJobStatus(any());
    }

    @Test
    void createAdaptiveJob_shouldMarkFailedWhenRemoteStartThrows() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> {
            AdaptiveExerciseJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobId);
            }
            return job;
        });
        when(aiApiClient.startAdaptiveJob(studentId, 1, 2, 4)).thenThrow(new AiApiException("remote failed"));

        AdaptiveJobResponseDto response = adaptiveSessionService.createAdaptiveJob(studentId, 1, 2, 4);

        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        verify(adaptiveExerciseJobRepository, times(2)).save(any(AdaptiveExerciseJob.class));
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenJobMissing() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId))
                .isInstanceOf(org.elearning.backend.assessment.exception.DoesNotExistException.class)
                .hasMessageContaining("Adaptive job not found");
    }

    @Test
    void getAdaptiveJobStatus_shouldMarkFailedWhenDoneResponseHasNoExercises() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .aiJobId("adaptive-job-empty")
                .status(AiRequestStatus.RUNNING)
                .build();

        AiAdaptiveJobStatusResponse remoteStatus = new AiAdaptiveJobStatusResponse();
        remoteStatus.setJobId("adaptive-job-empty");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setExercises(List.of());

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-empty")).thenReturn(remoteStatus);

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(status.getError()).contains("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldMarkFailedWhenDoneResponseHasNullExercises() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .aiJobId("adaptive-job-null-exercises")
                .status(AiRequestStatus.RUNNING)
                .build();

        AiAdaptiveJobStatusResponse remoteStatus = new AiAdaptiveJobStatusResponse();
        remoteStatus.setJobId("adaptive-job-null-exercises");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setExercises(null);

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-null-exercises")).thenReturn(remoteStatus);

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(status.getError()).contains("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldMarkFailedWhenRemoteStatusThrowsTimeout() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .aiJobId("adaptive-job-timeout")
                .status(AiRequestStatus.RUNNING)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-timeout")).thenThrow(new AiTimeoutException("timeout"));

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(status.getError()).contains("timeout");
    }

    @Test
    void getAdaptiveJobStatus_shouldMarkFailedWhenRemoteStatusReturnsBlankError() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .aiJobId("adaptive-job-failed-blank")
                .status(AiRequestStatus.RUNNING)
                .build();

        AiAdaptiveJobStatusResponse remoteStatus = new AiAdaptiveJobStatusResponse();
        remoteStatus.setJobId("adaptive-job-failed-blank");
        remoteStatus.setStatus(AiRequestStatus.FAILED);
        remoteStatus.setError(" ");

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-failed-blank")).thenReturn(remoteStatus);

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(status.getError()).contains("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldMarkFailedWhenRemoteStatusReturnsNullError() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .aiJobId("adaptive-job-failed-null")
                .status(AiRequestStatus.RUNNING)
                .build();

        AiAdaptiveJobStatusResponse remoteStatus = new AiAdaptiveJobStatusResponse();
        remoteStatus.setJobId("adaptive-job-failed-null");
        remoteStatus.setStatus(AiRequestStatus.FAILED);
        remoteStatus.setError(null);

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveExerciseJobRepository.save(any(AdaptiveExerciseJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-failed-null")).thenReturn(remoteStatus);

        AdaptiveJobStatusDto status = adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId);

        assertThat(status.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(status.getError()).contains("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenDoneSessionReferenceIsMissing() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .status(AiRequestStatus.DONE)
                .sessionId(sessionId)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId))
                .isInstanceOf(org.elearning.backend.assessment.exception.DoesNotExistException.class)
                .hasMessageContaining("Adaptive session not found");
    }

    @Test
    void startSession_shouldWrapAiApiException() {
        UUID studentId = UUID.randomUUID();
        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new AiApiException("ml down"));

        assertThatThrownBy(() -> adaptiveSessionService.startSession(studentId, 1, 2, 3))
                .isInstanceOf(AdaptiveServiceUnavailableException.class)
                .hasMessageContaining("currently unavailable");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenStoredSessionExerciseCannotBeDeserialized() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .id(jobId)
                .studentId(studentId)
                .status(AiRequestStatus.DONE)
                .sessionId(sessionId)
                .build();

        when(adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)).thenReturn(Optional.of(job));
        when(adaptiveSessionRepository.findById(sessionId)).thenReturn(Optional.of(
                AdaptiveSession.builder()
                        .id(sessionId)
                        .studentId(studentId)
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .build()
        ));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(
                AdaptiveSessionExercise.builder()
                        .sessionId(sessionId)
                        .mlExerciseId("ex-1")
                        .exerciseText("Question")
                        .exerciseType("SINGLE_CHOICE")
                        .answersRaw("bad json")
                        .correctAnswersRaw("[\"A\"]")
                        .build()
        ));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenThrow(new JsonProcessingException("bad json") { });

        assertThatThrownBy(() -> adaptiveSessionService.getAdaptiveJobStatus(jobId, studentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to read exercise data");
    }

    @Test
    void submitSession_shouldHandleSingleChoiceScoringAndFeedbackSent() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("SINGLE_CHOICE")
                .correctAnswersRaw("[\"A\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("A"));
        answer.setTimeSpent(12);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"A\"]");

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(1.0);
        assertThat(result.isFeedbackSent()).isTrue();
        verify(adaptiveSessionAnswerRepository).save(any(AdaptiveSessionAnswer.class));
    }

    @Test
    void submitSession_shouldRejectExpiredSession() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of());

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> adaptiveSessionService.submitSession(sessionId, studentId, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void submitSession_shouldRejectInactiveSession() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .status("COMPLETED")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of());

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> adaptiveSessionService.submitSession(sessionId, studentId, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void submitSession_shouldReturnPartialScoreForMultipleChoiceSubsetAndFalseFeedbackFlagWhenSendFails() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("MULTIPLE_CHOICE")
                .correctAnswersRaw("[\"A\",\"B\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("A"));
        answer.setTimeSpent(8);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A", "B"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"A\"]");
        doThrow(new RuntimeException("feedback failed")).when(aiApiClient).sendAdaptiveFeedback(any());

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(0.5);
        assertThat(result.isFeedbackSent()).isFalse();
    }

    @Test
    void submitSession_shouldUseZeroScoreWhenGivenAnswersAreNull() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("TRUE_FALSE")
                .correctAnswersRaw("[\"true\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(null);
        answer.setTimeSpent(5);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("true"));

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(0.0);
    }

    @Test
    void submitSession_shouldThrowWhenCorrectAnswersCannotBeParsed() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("SINGLE_CHOICE")
                .correctAnswersRaw("bad json")
                .build();

        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of());

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenThrow(new JsonProcessingException("bad json") { });

        assertThatThrownBy(() -> adaptiveSessionService.submitSession(sessionId, studentId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Error parsing correct answers");
    }

    @Test
    void submitSession_shouldThrowWhenGivenAnswersCannotBeSerialized() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("UNKNOWN")
                .correctAnswersRaw("[\"A\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("A"));
        answer.setTimeSpent(3);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A"));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialize fail") { });

        assertThatThrownBy(() -> adaptiveSessionService.submitSession(sessionId, studentId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unable to serialize object to JSON");
    }

    @Test
    void submitSession_shouldReturnZeroScoreForMultipleChoiceWithDisjointAnswers() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("MULTIPLE_CHOICE")
                .correctAnswersRaw("[\"A\",\"B\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("C"));
        answer.setTimeSpent(4);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A", "B"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"C\"]");

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(0.0);
    }

    @Test
    void submitSession_shouldReturnZeroScoreForUnknownType() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("UNKNOWN")
                .correctAnswersRaw("[\"A\"]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("A"));
        answer.setTimeSpent(4);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of("A"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"A\"]");

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(0.0);
    }

    @Test
    void submitSession_shouldReturnZeroScoreWhenCorrectAnswersAreEmpty() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        AdaptiveSession session = AdaptiveSession.builder()
                .id(sessionId)
                .studentId(studentId)
                .subjectId(1)
                .topicId(2)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        AdaptiveSessionExercise exercise = AdaptiveSessionExercise.builder()
                .id(exerciseId)
                .sessionId(sessionId)
                .mlExerciseId("ex-1")
                .exerciseType("SINGLE_CHOICE")
                .correctAnswersRaw("[]")
                .build();

        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId("ex-1");
        answer.setGivenAnswers(List.of("A"));
        answer.setTimeSpent(2);
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)).thenReturn(Optional.of(session));
        when(exerciseRepository.findAllBySessionId(sessionId)).thenReturn(List.of(exercise));
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<String>>>any()))
                .thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"A\"]");

        AdaptiveResultDto result = adaptiveSessionService.submitSession(sessionId, studentId, request);

        assertThat(result.getTotalScore()).isEqualTo(0.0);
    }
}
