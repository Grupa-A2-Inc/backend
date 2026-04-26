package org.elearning.backend.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.analytics.dto.AiQuestionDto;
import org.elearning.backend.analytics.dto.InjectionResultDto;
import org.elearning.backend.analytics.exception.WithoutAccessException;
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

    /**
     * Injects AI-generated questions from a completed AI question generation request into a test.
     *
     * @param requestId   the ID of the AI question generation request containing the generated questions
     * @param professorId the ID of the professor performing the injection; used to verify course ownership for access control
     * @param testIdOpt   an optional existing test ID to inject questions into; if null a new DRAFT test will be created for the lesson
     * @return an InjectionResultDto containing the test ID, whether a new test was created, the number of questions injected, the updated total number of questions in the test, and the lesson ID
     * @throws DoesNotExistException     if the AI question request, the associated lesson, or the specified test (when provided) does not exist
     * @throws WithoutAccessException   if the professorId does not match the course creator for the lesson
     * @throws ResourceConflictException if the AI request status is not SUCCESS
     * @throws ValidationException      if generated questions cannot be parsed or any parsed question fails validation (e.g., empty text, insufficient options, incorrect correct-answer definitions)
     */
    @Transactional
    public InjectionResultDto injectQuestions(UUID requestId, UUID professorId, UUID testIdOpt) {
        AiQuestionRequest aiQuestionRequest = aiQuestionRequestRepository.findById(requestId)
                .orElseThrow(() -> new DoesNotExistException("AI Question Request not found"));

        Lesson lesson = lessonRepository.findById(aiQuestionRequest.getLessonId()).
                orElseThrow(() -> new DoesNotExistException("Lesson not found"));

        UUID courseCreatorId = lesson.getChapter().getCourse().getCreatedBy();
        if(!courseCreatorId.equals(professorId)){
            throw new WithoutAccessException(professorId);
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
            test.setTimeLimitSec(1800);
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
            throw new ValidationException("Error at parsing generated questions. Create questions manually or retry later.");
        }

        List<Question> questionsToSave = new ArrayList<>();
        int index = 1;

        for (AiQuestionDto dto : aiQuestions) {
            validateAiQuestion(dto, index);

            Question q = new Question();
            q.setTest(test);
            q.setContent(dto.getText());
            q.setQuestionType(dto.getType());
            q.setIsActive(true);
            q.setSource(QuestionSource.AI_GENERATED);

            List<QuestionOption> mappedOptions = dto.getOptions().stream()
                    .map(opt -> {
                        QuestionOption qo = new QuestionOption();
                        qo.setText(opt.getText());
                        qo.setDisplayOrder(opt.getDisplayOrder());
                        qo.setIsCorrect(opt.getIsCorrect());
                        qo.setQuestion(q);
                        return qo;
                    })
                    .toList();
            q.setOptions(mappedOptions);

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
                newTotal,
                lesson.getId()
        );
    }

    /**
     * Validate an AI-generated question DTO and throw a ValidationException if it violates required constraints.
     *
     * <p>Checks performed:
     * <ul>
     *   <li>Question text must be non-empty.</li>
     *   <li>At least two answer options must be present.</li>
     *   <li>At least one option must be marked correct.</li>
     *   <li>Question type must be provided.</li>
     *   <li>Type-specific rules:
     *     <ul>
     *       <li>SINGLE_CHOICE: exactly one correct option.</li>
     *       <li>TRUE_FALSE: exactly two options and exactly one correct option.</li>
     *       <li>MULTIPLE_CHOICE: no additional constraints beyond the shared rules.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param dto the AI question DTO to validate
     * @param index the 1-based index of the question in the input list, used to compose error messages
     * @throws ValidationException if any validation rule is violated (message includes the question index)
     */
    private void validateAiQuestion(AiQuestionDto dto, int index) {
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new ValidationException(String.format("Question %d: The text can't be empty", index));
        }

        if (dto.getOptions() == null || dto.getOptions().size() < 2) {
            throw new ValidationException(String.format("Question %d: Must have at least 2 answer options.", index));
        }

        List<AiQuestionDto.OptionDto> correctOptions = dto.getOptions().stream()
                .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                .toList();

        if (correctOptions.isEmpty()) {
            throw new ValidationException(String.format("Question %d: Must have at least 1 correct answer defined.", index));
        }

        if (dto.getType() == null) {
            throw new ValidationException(String.format("Question %d: Unknown question type.", index));
        }

        switch (dto.getType()) {
            case SINGLE_CHOICE:
                if (correctOptions.size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being SINGLE_CHOICE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case TRUE_FALSE:
                if (dto.getOptions().size() != 2) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 2 options (e.g., True/False).", index));
                }
                if (correctOptions.size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case MULTIPLE_CHOICE:
                break;
        }
    }
}
