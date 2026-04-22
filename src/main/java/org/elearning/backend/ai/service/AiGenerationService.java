package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.analytics.model.AiQuestionRequest;
import org.elearning.backend.analytics.model.AiRequestStatus;
import org.elearning.backend.analytics.repository.AiQuestionRequestRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiGenerationService {
    private final AiQuestionRequestRepository questionRequestRepository;
    private final LessonRepository lessonRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;

    public UUID generateForLesson(UUID lessonId, UUID userId, RoleName role, Integer subjectId, Integer topicId) {
        validateUserAccess(lessonId, userId, role);

        AiQuestionRequest request = new AiQuestionRequest();
        request.setStatus(AiRequestStatus.PENDING);
        request.setLessonId(lessonId);
        request.setSubjectId(subjectId);
        request.setTopicId(topicId);

        request = questionRequestRepository.save(request);
        UUID requestId = request.getId();

        try {
            AiGenerateResponse response = aiApiClient.generateTest(requestId, lessonId);
            request.setStatus(AiRequestStatus.SUCCESS);
            List<AiQuestionDto> generatedQuestions = response.getQuestions();
            request.setGeneratedQuestions(objectMapper.writeValueAsString(generatedQuestions));
        }
        catch (AiTimeoutException exception) {
            request.setStatus(AiRequestStatus.FALLBACK);
        }
        catch (AiApiException | JsonProcessingException exception) {
            request.setStatus(AiRequestStatus.FAILED);
        }
        questionRequestRepository.save(request);
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
}
