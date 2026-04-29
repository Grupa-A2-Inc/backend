package org.elearning.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiGenerateRequestDto;
import org.elearning.backend.ai.dto.AiGenerateResponseDto;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiGenerationService {
    private final AiQuestionRequestRepository questionRequestRepository;
    private final LessonRepository lessonRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;
    private final AiAsyncWorker aiAsyncWorker;

    /**
     * Creates an AI question-generation request for the specified lesson and enqueues asynchronous processing.
     *
     * @param lessonId  identifier of the lesson for which questions should be generated
     * @param userId    identifier of the user initiating the request (used for access checks)
     * @param role      role of the user used to enforce access control
     * @param subjectId identifier of the subject to target for question generation
     * @param topicId   identifier of the topic to target for question generation
     * @return          the UUID of the created AI question request
     * @throws WithoutAccessException if the user does not have access to the lesson
     */
    public UUID generateForLesson(UUID lessonId, UUID userId, RoleName role, Integer subjectId, Integer topicId) {
        validateUserAccess(lessonId, userId, role);

        AiQuestionRequest request = new AiQuestionRequest();
        request.setStatus(AiRequestStatus.PENDING);
        request.setLessonId(lessonId);
        request.setSubjectId(subjectId);
        request.setTopicId(topicId);

        request = questionRequestRepository.save(request);
        UUID requestId = request.getId();

        aiAsyncWorker.processAiGenerationInBackground(requestId, lessonId, request);

        return requestId;
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

        AiRequestStatusDto statusDto = new AiRequestStatusDto();
        statusDto.setRequestId(requestId);
        statusDto.setStatus(request.getStatus());

        return statusDto;
    }

    public AiGenerateResponseDto generateTestForLesson(AiGenerateRequestDto requestDto, UUID lessonId, UUID userId, RoleName role) {
        Integer subjectId = requestDto.getSubjectId();
        Integer topicId = requestDto.getTopicId();
        if (subjectId == null || topicId == null) {
            throw new ValidationException("subjectId and topicId are required.");
        }

        UUID requestId = generateForLesson(lessonId, userId, role, subjectId, topicId);

        AiGenerateResponseDto responseDto = new AiGenerateResponseDto();
        responseDto.setRequestId(requestId);
        responseDto.setStatus(AiRequestStatus.PENDING);
        responseDto.setLessonId(lessonId);

        return responseDto;
    }
}
