package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.elearning.backend.ai.dto.AiGenerateRequestDto;
import org.elearning.backend.ai.dto.AiGenerateJobResponse;
import org.elearning.backend.ai.dto.AiGenerateJobStatusResponse;
import org.elearning.backend.ai.dto.AiGenerateResponseDto;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.ai.dto.CurriculumCatalogRequestDto;
import org.elearning.backend.ai.dto.CurriculumCatalogResponseDto;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private AiQuestionRequestRepository questionRequestRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private AiApiClient aiApiClient;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    @Test
    void generateForLesson_shouldSaveRequestAndStartRemoteJob_whenTeacherHasAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();
        AiGenerateJobResponse remoteResponse = new AiGenerateJobResponse();
        remoteResponse.setJobId("job-123");
        remoteResponse.setStatus(AiRequestStatus.PENDING);

        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(savedId);
            return request;
        });
        when(aiApiClient.startGenerateJob(lessonId, 5)).thenReturn(remoteResponse);

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5);

        assertThat(requestId).isEqualTo(savedId);
        ArgumentCaptor<AiQuestionRequest> requestCaptor = ArgumentCaptor.forClass(AiQuestionRequest.class);
        verify(questionRequestRepository, org.mockito.Mockito.times(2)).save(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).getStatus()).isEqualTo(AiRequestStatus.PENDING);
        assertThat(requestCaptor.getAllValues().get(0).getLessonId()).isEqualTo(lessonId);
        assertThat(requestCaptor.getAllValues().get(1).getAiJobId()).isEqualTo("job-123");
        assertThat(requestCaptor.getAllValues().get(1).getStatus()).isEqualTo(AiRequestStatus.PENDING);
    }

    @Test
    void generateForLesson_shouldRejectTeacherWithoutAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(false);

        assertThatThrownBy(() -> aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5))
                .isInstanceOf(WithoutAccessException.class);

        verify(questionRequestRepository, never()).save(any());
    }

    @Test
    void generateForLesson_shouldSaveRequestWhenStudentHasAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();

        when(lessonRepository.isStudentEnrolledInLessonCourse(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(savedId);
            return request;
        });
        AiGenerateJobResponse remoteResponse = new AiGenerateJobResponse();
        remoteResponse.setJobId("job-456");
        remoteResponse.setStatus(AiRequestStatus.RUNNING);
        when(aiApiClient.startGenerateJob(lessonId, 3)).thenReturn(remoteResponse);

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.STUDENT, 3);

        assertThat(requestId).isEqualTo(savedId);
        verify(lessonRepository).isStudentEnrolledInLessonCourse(lessonId, userId);
        verify(aiApiClient).startGenerateJob(lessonId, 3);
    }

    @Test
    void generateForLesson_shouldRejectStudentWithoutAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(lessonRepository.isStudentEnrolledInLessonCourse(lessonId, userId)).thenReturn(false);

        assertThatThrownBy(() -> aiGenerationService.generateForLesson(lessonId, userId, RoleName.STUDENT, 3))
                .isInstanceOf(WithoutAccessException.class);

        verify(questionRequestRepository, never()).save(any());
    }

    @Test
    void generateForLesson_shouldSkipAccessCheckForAdmin() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();

        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(savedId);
            return request;
        });
        AiGenerateJobResponse remoteResponse = new AiGenerateJobResponse();
        remoteResponse.setJobId("job-789");
        remoteResponse.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startGenerateJob(lessonId, 2)).thenReturn(remoteResponse);

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.ADMIN, 2);

        assertThat(requestId).isEqualTo(savedId);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getRequestStatus_shouldReturnStatus_whenRequestExists() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.PENDING)
                .aiJobId("job-100")
                .build();
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-100");
        remoteStatus.setStatus(AiRequestStatus.RUNNING);

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-100")).thenReturn(remoteStatus);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getRequestId()).isEqualTo(requestId);
        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.RUNNING);
    }

    @Test
    void getRequestStatus_shouldThrowWhenRequestMissing() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiGenerationService.getRequestStatus(requestId, userId, RoleName.ADMIN))
                .isInstanceOf(DoesNotExistException.class);
    }

    @Test
    void generateTestForLesson_shouldRejectMissingCount() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AiGenerateRequestDto requestDto = new AiGenerateRequestDto();
        ReflectionTestUtils.setField(requestDto, "count", null);

        assertThatThrownBy(() -> aiGenerationService.generateTestForLesson(requestDto, lessonId, userId, RoleName.ADMIN))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("count is required");
    }

    @Test
    void generateTestForLesson_shouldReturnPendingResponse() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        AiGenerateRequestDto requestDto = new AiGenerateRequestDto();
        ReflectionTestUtils.setField(requestDto, "count", 7);

        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(requestId);
            return request;
        });
        AiGenerateJobResponse remoteResponse = new AiGenerateJobResponse();
        remoteResponse.setJobId("job-200");
        remoteResponse.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startGenerateJob(lessonId, 7)).thenReturn(remoteResponse);

        AiGenerateResponseDto responseDto = aiGenerationService.generateTestForLesson(requestDto, lessonId, userId, RoleName.ADMIN);

        assertThat(responseDto.getRequestId()).isEqualTo(requestId);
        assertThat(responseDto.getLessonId()).isEqualTo(lessonId);
        assertThat(responseDto.getStatus()).isEqualTo(AiRequestStatus.PENDING);
    }

    @Test
    void getRequestStatus_shouldPersistQuestionsWhenRemoteJobIsDone() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .aiJobId("job-300")
                .status(AiRequestStatus.RUNNING)
                .build();
        AiQuestionDto question = new AiQuestionDto();
        question.setText("Q1");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setAnswers(java.util.List.of("A", "B"));
        question.setCorrectAnswers(java.util.List.of("A"));
        question.setDifficulty(0.5);
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-300");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setQuestions(java.util.List.of(question));

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-300")).thenReturn(remoteStatus);
        when(objectMapper.writeValueAsString(remoteStatus.getQuestions())).thenReturn("[{\"text\":\"Q1\"}]");

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.DONE);
        assertThat(request.getGeneratedQuestions()).isEqualTo("[{\"text\":\"Q1\"}]");
        assertThat(request.getResolvedAt()).isNotNull();
    }

    @Test
    void generateForLesson_shouldMarkRequestFailedWhenStartJobFails() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();

        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(savedId);
            return request;
        });
        when(aiApiClient.startGenerateJob(lessonId, 5)).thenThrow(new AiApiException("start failed"));

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5);

        assertThat(requestId).isEqualTo(savedId);
        ArgumentCaptor<AiQuestionRequest> requestCaptor = ArgumentCaptor.forClass(AiQuestionRequest.class);
        verify(questionRequestRepository, org.mockito.Mockito.times(2)).save(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(1).getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(requestCaptor.getAllValues().get(1).getErrorMessage()).contains("start failed");
    }

    @Test
    void getCurriculumCatalog_shouldDelegateToClient() {
        CurriculumCatalogRequestDto requestDto = new CurriculumCatalogRequestDto(9, 2, 3);
        CurriculumCatalogResponseDto expected = new CurriculumCatalogResponseDto();
        when(aiApiClient.getCurriculumCatalog(requestDto)).thenReturn(expected);

        CurriculumCatalogResponseDto actual = aiGenerationService.getCurriculumCatalog(requestDto);

        assertThat(actual).isSameAs(expected);
        verify(aiApiClient).getCurriculumCatalog(requestDto);
    }

    @Test
    void getRequestStatus_shouldNotSyncWhenStatusIsTerminal() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.DONE)
                .aiJobId("job-terminal")
                .build();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.DONE);
        verify(aiApiClient, never()).getGenerateJobStatus(any());
    }

    @Test
    void getRequestStatus_shouldNotSyncWhenStatusIsFailed() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.FAILED)
                .aiJobId("job-failed-terminal")
                .errorMessage("failed")
                .build();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        verify(aiApiClient, never()).getGenerateJobStatus(any());
    }

    @Test
    void getRequestStatus_shouldNotSyncWhenJobIdIsMissing() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId(null)
                .build();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.RUNNING);
        verify(aiApiClient, never()).getGenerateJobStatus(any());
    }

    @Test
    void getRequestStatus_shouldMarkFailedWhenDoneResponseHasNoQuestions() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId("job-empty")
                .build();
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-empty");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setQuestions(null);

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-empty")).thenReturn(remoteStatus);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(statusDto.getError()).contains("invalid");
    }

    @Test
    void getRequestStatus_shouldMarkFailedWhenQuestionSerializationFails() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId("job-json")
                .build();
        AiQuestionDto question = new AiQuestionDto();
        question.setText("Q1");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setAnswers(java.util.List.of("A", "B"));
        question.setCorrectAnswers(java.util.List.of("A"));
        question.setDifficulty(0.2);
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-json");
        remoteStatus.setStatus(AiRequestStatus.DONE);
        remoteStatus.setQuestions(java.util.List.of(question));

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-json")).thenReturn(remoteStatus);
        when(objectMapper.writeValueAsString(remoteStatus.getQuestions())).thenThrow(new JsonProcessingException("boom") { });

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(statusDto.getError()).contains("invalid");
    }

    @Test
    void getRequestStatus_shouldMarkFailedWhenRemoteStatusReturnsBlankError() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId("job-failed")
                .build();
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-failed");
        remoteStatus.setStatus(AiRequestStatus.FAILED);
        remoteStatus.setError(" ");

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-failed")).thenReturn(remoteStatus);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(statusDto.getError()).contains("invalid");
    }

    @Test
    void getRequestStatus_shouldMarkFailedWhenRemoteStatusReturnsNullError() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId("job-failed-null")
                .build();
        AiGenerateJobStatusResponse remoteStatus = new AiGenerateJobStatusResponse();
        remoteStatus.setJobId("job-failed-null");
        remoteStatus.setStatus(AiRequestStatus.FAILED);
        remoteStatus.setError(null);

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-failed-null")).thenReturn(remoteStatus);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(statusDto.getError()).contains("invalid");
    }

    @Test
    void getRequestStatus_shouldMarkFailedWhenRemoteStatusThrowsTimeout() {
        UUID requestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.RUNNING)
                .aiJobId("job-timeout")
                .build();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(aiApiClient.getGenerateJobStatus("job-timeout")).thenThrow(new AiTimeoutException("timeout"));

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(statusDto.getError()).contains("timeout");
    }
}
