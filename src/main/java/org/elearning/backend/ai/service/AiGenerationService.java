package org.elearning.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiRequestStatusDto;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
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

    public AiRequestStatusDto getRequestStatus(UUID requestId, UUID userId, RoleName role) {
        AiQuestionRequest request = questionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        UUID lessonId = request.getLessonId();
        validateUserAccess(lessonId, userId, role);

        AiRequestStatusDto statusDto = new AiRequestStatusDto();
        statusDto.setRequestId(requestId);
        statusDto.setStatus(request.getStatus());

        return statusDto;
    }
}
