package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.dto.InjectionResultDto;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.ai.exception.ResourceConflictException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service()
@RequiredArgsConstructor
public class AiQuestionInjectorService {
    private final AiQuestionRequestRepository aiQuestionRequestRepository;
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;
    private final TestRepository testRepository;
    private final ObjectMapper objectMapper;

    /**
     * This method injects AI-generated questions into a test based on a completed AI question generation request. It performs the following steps:
     * 1. Validates that the AI question request exists and is successful.
     * 2. Validates that the associated lesson exists and that the professor has permission to modify it.
     * 3. If a test ID is provided, it validates that the test exists; otherwise, it creates a new test in DRAFT status.
     * 4. Parses the generated questions from the AI request and validates each question's content, options, and correct answers.
     * 5. Saves the valid questions to the database, associating them with the test.
     * 6. Updates the AI question request with the test ID and returns an InjectionResultDto containing details of the injection outcome.
     *
     * @param requestId The ID of the AI question generation request containing the generated questions.
     * @param professorId The ID of the professor attempting to inject the questions (used for access control).
     * @param testIdOpt An optional ID of an existing test to inject questions into; if null, a new test will be created.
     * @return An InjectionResultDto containing the ID of the test, whether a new test was created, the number of questions injected, and the new total number of questions in the test.
     * @throws DoesNotExistException if the AI question request, lesson, or specified test does not exist.
     * @throws WithoutAccessException if the professor does not have permission to modify the lesson or test.
     * @throws ResourceConflictException if the AI generation is not successful or if there is an error parsing generated questions.
     * @throws ValidationException if any of the generated questions fail validation checks (e.g., empty text, insufficient options, incorrect answer definitions).
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
        } catch (JsonProcessingException exception) {
            throw new ValidationException("Error at parsing generated questions. Create questions manually or retry later.");
        }

        List<Question> questionsToSave = new ArrayList<>();
        int index = 1;

        for (AiQuestionDto dto : aiQuestions) {
            validateAiQuestion(dto, index);

            Question question = new Question();
            question.setTest(test);
            question.setContent(dto.getText());
            question.setQuestionType(dto.getType());
            question.setIsActive(true);
            question.setSource(QuestionSource.AI_GENERATED);
            question.setDifficulty(BigDecimal.valueOf(dto.getDifficulty()));
            AtomicInteger displayOrder = new AtomicInteger(1);

            List<QuestionOption> mappedOptions = dto.getAnswers().stream()
                    .map(questionOptionString -> {
                        QuestionOption questionOption = new QuestionOption();
                        questionOption.setText(questionOptionString);
                        questionOption.setDisplayOrder(displayOrder.getAndIncrement());
                        questionOption.setIsCorrect(dto.getCorrectAnswers().contains(questionOptionString));
                        questionOption.setQuestion(question);
                        return questionOption;
                    })
                    .toList();
            question.setOptions(mappedOptions);

            questionsToSave.add(question);

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
     * Validates the AI-generated question DTO to ensure it meets the necessary criteria for injection.
     * This includes checks for non-empty text, a minimum number of options, and correct answer definitions based on question type.
     *
     * @param dto The AI question DTO to validate.
     * @param index The index of the question in the list (used for error messages).
     * @throws ValidationException if any validation rule is violated.
     */
    private void validateAiQuestion(AiQuestionDto dto, int index) {
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new ValidationException(String.format("Question %d: The text can't be empty", index));
        }

        if (dto.getAnswers() == null || dto.getAnswers().size() < 2) {
            throw new ValidationException(String.format("Question %d: Must have at least 2 answer options.", index));
        }

        if (dto.getCorrectAnswers().isEmpty()) {
            throw new ValidationException(String.format("Question %d: Must have at least 1 correct answer defined.", index));
        }

        List<String> correctOptions = dto.getAnswers().stream().filter(dto.getCorrectAnswers()::contains).toList();
        if (correctOptions.isEmpty()) {
            throw new ValidationException(String.format("Question %d: Correct answer(s) must be among the provided answer options.", index));
        }
        if(correctOptions.size() != dto.getCorrectAnswers().size()) {
            throw new ValidationException(String.format("Question %d: Some correct answers are not among the provided answer options.", index));
        }

        if (dto.getType() == null) {
            throw new ValidationException(String.format("Question %d: Unknown question type.", index));
        }

        switch (dto.getType()) {
            case SINGLE_CHOICE:
                if (dto.getCorrectAnswers().size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being SINGLE_CHOICE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case TRUE_FALSE:
                if (dto.getAnswers().size() != 2) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 2 options (e.g., True/False).", index));
                }
                if (dto.getCorrectAnswers().size() != 1) {
                    throw new ValidationException(String.format("Question %d: Being TRUE_FALSE type, it must have exactly 1 correct answer.", index));
                }
                break;

            case MULTI_CHOICE:
                break;
        }
    }
}
