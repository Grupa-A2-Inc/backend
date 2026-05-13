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
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
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

    private UUID lessonId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void generateForLessonPersistsPendingRequestAndStartsWorkerForTeacher() {
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        UUID requestId = aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5);

        ArgumentCaptor<AiQuestionRequest> captor = ArgumentCaptor.forClass(AiQuestionRequest.class);
        verify(questionRequestRepository).save(captor.capture());
        verify(aiAsyncWorker).processAiGenerationInBackground(eq(5), eq(lessonId), eq(captor.getValue()));
        assertThat(captor.getValue().getStatus()).isEqualTo(AiRequestStatus.PENDING);
        assertThat(captor.getValue().getLessonId()).isEqualTo(lessonId);
        assertThat(requestId).isNotNull();
    }

    @Test
    void generateForLessonThrowsWhenTeacherCannotAccessLesson() {
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(false);

        assertThatThrownBy(() -> aiGenerationService.generateForLesson(lessonId, userId, RoleName.TEACHER, 5))
                .isInstanceOf(WithoutAccessException.class);
    }

    @Test
    void generateForLessonThrowsWhenStudentIsNotEnrolled() {
        when(lessonRepository.isStudentEnrolledInLessonCourse(lessonId, userId)).thenReturn(false);

        assertThatThrownBy(() -> aiGenerationService.generateForLesson(lessonId, userId, RoleName.STUDENT, 5))
                .isInstanceOf(WithoutAccessException.class);
    }

    @Test
    void getRequestStatusReturnsStatusDto() {
        UUID requestId = UUID.randomUUID();
        AiQuestionRequest request = new AiQuestionRequest();
        request.setLessonId(lessonId);
        request.setStatus(AiRequestStatus.SUCCESS);
        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.isStudentEnrolledInLessonCourse(lessonId, userId)).thenReturn(true);

        AiRequestStatusDto dto = aiGenerationService.getRequestStatus(requestId, userId, RoleName.STUDENT);

        assertThat(dto.getRequestId()).isEqualTo(requestId);
        assertThat(dto.getStatus()).isEqualTo(AiRequestStatus.SUCCESS);
    }

    @Test
    void getRequestStatusThrowsWhenRequestMissing() {
        UUID requestId = UUID.randomUUID();
        when(questionRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiGenerationService.getRequestStatus(requestId, userId, RoleName.STUDENT))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessage("Request not found");
    }

    @Test
    void generateTestForLessonThrowsWhenCountMissing() {
        AiGenerateRequestDto requestDto = new AiGenerateRequestDto();
        ReflectionTestUtils.setField(requestDto, "count", null);

        assertThatThrownBy(() -> aiGenerationService.generateTestForLesson(requestDto, lessonId, userId, RoleName.TEACHER))
                .isInstanceOf(ValidationException.class)
                .hasMessage(" count is required.");
    }

    @Test
    void generateTestForLessonBuildsAcceptedResponse() {
        AiGenerateRequestDto requestDto = new AiGenerateRequestDto();
        ReflectionTestUtils.setField(requestDto, "count", 4);
        when(lessonRepository.isLessonOwnedByProfessor(lessonId, userId)).thenReturn(true);
        when(questionRequestRepository.save(any(AiQuestionRequest.class))).thenAnswer(invocation -> {
            AiQuestionRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        AiGenerateResponseDto response = aiGenerationService.generateTestForLesson(requestDto, lessonId, userId, RoleName.TEACHER);

        assertThat(response.getLessonId()).isEqualTo(lessonId);
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.PENDING);
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    void getCurriculumCatalogDelegatesToApiClient() {
        CurriculumCatalogRequestDto requestDto = new CurriculumCatalogRequestDto(8, 2, 4);
        CurriculumCatalogResponseDto expected = new CurriculumCatalogResponseDto();
        when(aiApiClient.getCurriculumCatalog(requestDto)).thenReturn(expected);

        CurriculumCatalogResponseDto actual = aiGenerationService.getCurriculumCatalog(requestDto);

        assertThat(actual).isSameAs(expected);
    }
}
