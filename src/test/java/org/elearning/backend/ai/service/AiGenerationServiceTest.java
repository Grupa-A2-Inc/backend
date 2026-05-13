package org.elearning.backend.ai.service;

import org.elearning.backend.ai.dto.AiGenerateRequestDto;
import org.elearning.backend.ai.dto.AiGenerateResponseDto;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.ai.dto.CurriculumCatalogRequestDto;
import org.elearning.backend.ai.dto.CurriculumCatalogResponseDto;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private AiAsyncWorker aiAsyncWorker;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    @Test
    void generateForLesson_shouldSaveRequestAndStartAsync_whenTeacherHasAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();

        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(savedId);
            return request;
        });

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5);

        assertThat(requestId).isEqualTo(savedId);
        ArgumentCaptor<AiQuestionRequest> requestCaptor = ArgumentCaptor.forClass(AiQuestionRequest.class);
        verify(questionRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(AiRequestStatus.PENDING);
        assertThat(requestCaptor.getValue().getLessonId()).isEqualTo(lessonId);
        verify(aiAsyncWorker).processAiGenerationInBackground(5, lessonId, requestCaptor.getValue());
    }

    @Test
    void generateForLesson_shouldRejectTeacherWithoutAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(false);

        assertThatThrownBy(() -> aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5))
                .isInstanceOf(WithoutAccessException.class);

        verify(questionRequestRepository, never()).save(any());
        verify(aiAsyncWorker, never()).processAiGenerationInBackground(any(Integer.class), any(UUID.class), any());
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

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.STUDENT, 3);

        assertThat(requestId).isEqualTo(savedId);
        verify(lessonRepository).isStudentEnrolledInLessonCourse(lessonId, userId);
        verify(aiAsyncWorker).processAiGenerationInBackground(eq(3), eq(lessonId), any(AiQuestionRequest.class));
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
                .status(AiRequestStatus.SUCCESS)
                .build();

        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);

        AiRequestStatusDto statusDto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.TEACHER);

        assertThat(statusDto.getRequestId()).isEqualTo(requestId);
        assertThat(statusDto.getStatus()).isEqualTo(AiRequestStatus.SUCCESS);
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

        AiGenerateResponseDto responseDto = aiGenerationService.generateTestForLesson(requestDto, lessonId, userId, RoleName.ADMIN);

        assertThat(responseDto.getRequestId()).isEqualTo(requestId);
        assertThat(responseDto.getLessonId()).isEqualTo(lessonId);
        assertThat(responseDto.getStatus()).isEqualTo(AiRequestStatus.PENDING);
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
}
