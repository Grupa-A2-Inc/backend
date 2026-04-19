package org.elearning.backend.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.AiQuestionDto;
import org.elearning.backend.analytics.dto.InjectionResultDto;
import org.elearning.backend.analytics.exception.AccessDeniedException;
import org.elearning.backend.analytics.exception.ResourceConflictException;
import org.elearning.backend.analytics.exception.ValidationException;
import org.elearning.backend.analytics.model.AiQuestionRequest;
import org.elearning.backend.analytics.model.AiRequestStatus;
import org.elearning.backend.analytics.model.QuestionSource;
import org.elearning.backend.analytics.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service()
@RequiredArgsConstructor
public class AiQuestionInjectorService {
    private final AiQuestionRequestRepository aiQuestionRequestRepository;
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;
    private final TestRepository testRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public InjectionResultDto injectQuestions(UUID requestId, UUID professorId, UUID testIdOpt) {
        AiQuestionRequest aiQuestionRequest = aiQuestionRequestRepository.findById(requestId)
                .orElseThrow(() -> new DoesNotExistException("AI Question Request not found"));

        Lesson lesson = lessonRepository.findById(aiQuestionRequest.getLessonId()).
                orElseThrow(() -> new DoesNotExistException("Lesson not found"));

        UUID courseCreatorId = lesson.getChapter().getCourse().getCreatedBy();
        if(!courseCreatorId.equals(professorId)){
            throw new AccessDeniedException(professorId);
        }

        if(aiQuestionRequest.getStatus() != AiRequestStatus.SUCCESS){
            throw new ResourceConflictException("AI Generation failed or is not completed yet. Create questions manually or retry later.");
        }

        Test test;
        boolean isTestCreated = false;

        if(testIdOpt == null){
            test = new Test();
            test.setLessonId(lesson.getId());
            test.setTitle("Test AI - " + lesson.getTitle());
            test.setCreatedBy(professorId);
            test.setStatus(TestStatus.DRAFT);
            test = testRepository.save(test);
            isTestCreated = true;
        } else {
            test = testRepository.findById(testIdOpt)
                    .orElseThrow(() -> new DoesNotExistException("Test not found"));
        }

        List<AiQuestionDto> aiQuestions;
        try {
            aiQuestions = objectMapper.readValue(
                    aiQuestionRequest.getGeneratedQuestions(),
                    new TypeReference<List<AiQuestionDto>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new ResourceConflictException("Error at parsing generated questions. Create questions manually or retry later.");
        }

        List<Question> questionsToSave = new ArrayList<>();
        int index = 1;

        for (AiQuestionDto dto : aiQuestions) {
            validateAiQuestion(dto, index);

            Question q = new Question();
            q.setTest(test);
            q.setContent(dto.getText());
            q.setQuestionType(dto.getType());

            q.setOptions(dto.getOptions());
            q.setSource(QuestionSource.AI_GENERATED);

            questionsToSave.add(q);
            index++;
        }

        questionRepository.saveAll(questionsToSave);

        aiQuestionRequest.setTestId(test.getId());
        aiQuestionRequestRepository.save(aiQuestionRequest);

        int newTotal = questionRepository.countByTestId(test.getId());

        return new InjectionResultDto(
                test.getId(),
                isTestCreated,
                questionsToSave.size(),
                newTotal
        );
    }

    private void validateAiQuestion(AiQuestionDto dto, int index) {
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new ValidationException(String.format("Question %d: The text can't be empty", index));
        }

        if (dto.getOptions() == null || dto.getOptions().size() < 2) {
            throw new ValidationException(String.format("Question %d: Must have at least 2 answer options.", index));
        }

        List<QuestionOption> correctQuestionOptions = dto.getOptions().stream()
                .filter(QuestionOption::getIsCorrect)
                .toList();

        if (correctQuestionOptions == null || correctQuestionOptions.isEmpty()) {
            throw new ValidationException(String.format("Question %d: Must have at least 1 correct answer defined.", index));
        }

        switch (dto.getType()) {
            case SINGLE_CHOICE:
                if (correctQuestionOptions.size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being SINGLE_CHOICE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case TRUE_FALSE:
                if (dto.getOptions().size() != 2) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 2 options (e.g., True/False).", index));
                }
                if (correctQuestionOptions.size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case MULTIPLE_CHOICE:
                break;

            default:
                throw new ValidationException(String.format("Question %d: Unknown question type.", index));
        }
    }
}
