package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiGenerationService {
    private final AiQuestionRequestRepository questionRequestRepository;
    private final LessonRepository lessonRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates an AI question-generation request for the specified lesson and enqueues asynchronous processing.
     *
     * @param lessonId  identifier of the lesson for which questions should be generated
     * @param userId    identifier of the user initiating the request (used for access checks)
     * @param role      role of the user used to enforce access control
     * @return          the UUID of the created AI question request
     * @throws WithoutAccessException if the user does not have access to the lesson
     */
    public UUID generateForLesson(UUID lessonId, UUID userId, RoleName role, int count) {
        validateUserAccess(lessonId, userId, role);

        AiQuestionRequest request = new AiQuestionRequest();
        request.setStatus(AiRequestStatus.PENDING);
        request.setLessonId(lessonId);

        request = questionRequestRepository.save(request);
        try {
            AiGenerateJobResponse response = aiApiClient.startGenerateJob(lessonId, count);
            request.setAiJobId(response.getJobId());
            request.setStatus(response.getStatus());
        } catch (AiApiException | AiTimeoutException exception) {
            markAsFailed(request, exception.getMessage());
        }
        questionRequestRepository.save(request);

        return request.getId();
    }

    /**
     * Enforces that the specified user has permission to access the given lesson according to their role.
     *
     * @param lessonId the lesson identifier to validate access for
     * @param userId   the user identifier whose access is being validated
     * @param role     the role of the user which determines the required access check
     * @throws WithoutAccessException if the user does not have the required access to the lesson
     */
    private void validateUserAccess(UUID lessonId, UUID userId, RoleName role)
    {
        if (role==RoleName.TEACHER)
        {
            if (!lessonRepository.isLessonOwnedByProfessor(lessonId, userId))
            {
                throw new WithoutAccessException(userId);
            }
        }
        else if (role==RoleName.STUDENT && !lessonRepository.isStudentEnrolledInLessonCourse(lessonId, userId))
        {
            throw new WithoutAccessException(userId);

        }
    }

    /**
     * Retrieves the status of an AI question generation request and validates that the given user has access to the associated lesson.
     *
     * @param requestId the identifier of the AI generation request to fetch
     * @param userId    the identifier of the user requesting the status
     * @param role      the role of the user used for access validation
     * @return          an AiRequestStatusDto containing the requestId and the request's current status
     * @throws DoesNotExistException        if no request exists for the provided requestId
     * @throws WithoutAccessException if the user is not authorized to access the lesson associated with the request
     */
    public AiRequestStatusDto getRequestStatus(UUID requestId, UUID userId, RoleName role) {
        AiQuestionRequest request = questionRequestRepository.findById(requestId)
                .orElseThrow(() -> new DoesNotExistException("Request not found"));
        UUID lessonId = request.getLessonId();
        validateUserAccess(lessonId, userId, role);

        if (!isTerminalStatus(request.getStatus()) && request.getAiJobId() != null) {
            syncRequestWithAi(request);
            questionRequestRepository.save(request);
        }

        AiRequestStatusDto statusDto = new AiRequestStatusDto();
        statusDto.setRequestId(requestId);
        statusDto.setStatus(request.getStatus());
        statusDto.setError(request.getErrorMessage());

        return statusDto;
    }

    public AiGenerateResponseDto generateTestForLesson(AiGenerateRequestDto requestDto, UUID lessonId, UUID userId, RoleName role) {
        Integer count = requestDto.getCount();
        if (count == null) {
            throw new ValidationException(" count is required.");
        }

        UUID requestId = generateForLesson(lessonId, userId, role, count);

        AiGenerateResponseDto responseDto = new AiGenerateResponseDto();
        responseDto.setRequestId(requestId);
        responseDto.setStatus(AiRequestStatus.PENDING);
        responseDto.setLessonId(lessonId);

        return responseDto;
    }

    public CurriculumCatalogResponseDto getCurriculumCatalog(CurriculumCatalogRequestDto requestDto) {
        return aiApiClient.getCurriculumCatalog(requestDto);
    }

    private boolean isTerminalStatus(AiRequestStatus status) {
        return status == AiRequestStatus.DONE || status == AiRequestStatus.FAILED;
    }

    private void syncRequestWithAi(AiQuestionRequest request) {
        try {
            AiGenerateJobStatusResponse response = aiApiClient.getGenerateJobStatus(request.getAiJobId());
            request.setStatus(response.getStatus());

            if (response.getStatus() == AiRequestStatus.DONE) {
                if (response.getQuestions() == null) {
                    markAsFailed(request, "LLM-ul a returnat un raspuns invalid.");
                    return;
                }
                request.setGeneratedQuestions(objectMapper.writeValueAsString(response.getQuestions()));
                request.setErrorMessage(null);
                request.setResolvedAt(LocalDateTime.now());
                return;
            }

            if (response.getStatus() == AiRequestStatus.FAILED) {
                markAsFailed(request, response.getError());
            }
        } catch (AiApiException | AiTimeoutException exception) {
            markAsFailed(request, exception.getMessage());
        } catch (JsonProcessingException exception) {
            markAsFailed(request, "LLM-ul a returnat un raspuns invalid.");
        }
    }

    private void markAsFailed(AiQuestionRequest request, String errorMessage) {
        request.setStatus(AiRequestStatus.FAILED);
        request.setErrorMessage(errorMessage == null || errorMessage.isBlank()
                ? "LLM-ul a returnat un raspuns invalid."
                : errorMessage);
        request.setResolvedAt(LocalDateTime.now());
    }
}
