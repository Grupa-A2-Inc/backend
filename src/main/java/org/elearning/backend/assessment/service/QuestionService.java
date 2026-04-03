package org.elearning.backend.assessment.service;

import jakarta.validation.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.OptionRequestDto;
import org.elearning.backend.assessment.dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.QuestionResponseDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.mapper.QuestionMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Transactional
    public QuestionResponseDto createQuestion(UUID testId, QuestionRequestDto dto, UUID professorId) {
        Test test = getValidTestForModification(testId, professorId);
        validateQuestionOptions(dto);
        Question question = questionMapper.toEntity(dto);
        question.setTest(test);
        if (question.getOptions() != null) {
            question.getOptions().forEach(option -> option.setQuestion(question));
        }
        Question savedQuestion = questionRepository.save(question);
        return questionMapper.toResponseDto(savedQuestion);
    }
    private void validateQuestionOptions(QuestionRequestDto dto) {
        List<OptionRequestDto> options = dto.getOptions();

        if (options == null || options.isEmpty()) {
            throw new ValidationException("The question should have at least one option.");
        }

        // cate optiuni corecte a trimis profesorul
        long correctCount = options.stream()
                .filter(OptionRequestDto::getIsCorrect)
                .count();

        switch (dto.getQuestionType()) {
            case SINGLE_CHOICE:
                if (options.size() < 2) {
                    throw new ValidationException("Single choice requires at least 2 options.");
                }
                if (correctCount != 1) {
                    throw new ValidationException("Single choice should have exactly one correct option.");
                }
                break;

            case MULTIPLE_CHOICE:
                if (options.size() < 2) {
                    throw new ValidationException("Multiple choice requires at least 2 options.");
                }
                if (correctCount < 2) {
                    throw new ValidationException("Multiple choice should have AT LEAST 2 correct options.");
                }
                break;

            case TRUE_FALSE:
                if (options.size() != 2) {
                    throw new ValidationException("True/False requires exactly 2 options.");
                }
                if (correctCount != 1) {
                    throw new ValidationException("True/False should have exactly one correct option.");
                }

                // verificam ca textele sa fie exact "true" si "false"
                boolean hasTrue = options.stream().anyMatch(o -> o.getText().equalsIgnoreCase("True"));
                boolean hasFalse = options.stream().anyMatch(o -> o.getText().equalsIgnoreCase("False"));
                if (!hasTrue || !hasFalse) {
                    throw new ValidationException("Options for True and False should be exactly 'True' and 'False'.");
                }
                break;

            default:
                throw new ValidationException("Question not supported.");
        }
    }

    public List<QuestionResponseDto> getAllQuestionsForTest(UUID testId) {
        List<Question> questions = questionRepository.findByTestId(testId);

        return questions.stream()
                .map(questionMapper::toResponseDto)
                .toList();
    }

    public QuestionResponseDto getQuestionById(UUID testId, Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new DoesNotExistException("Question not found."));

        // intrebarea sa apartina testului specificat
        if (!question.getTest().getId().equals(testId)) {
            throw new ValidationException("This question does not belong to the specified test.");
        }

        return questionMapper.toResponseDto(question);
    }

    @Transactional
    public QuestionResponseDto updateQuestion(UUID testId, Integer questionId, QuestionRequestDto dto, UUID professorId) {
        Test test = getValidTestForModification(testId, professorId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new DoesNotExistException("Question not found."));
        if (!question.getTest().getId().equals(test.getId())) {
            throw new ValidationException("This question does not belong to the specified test.");
        }
        validateQuestionOptions(dto);
        question.setContent(dto.getContent());
        question.setQuestionType(dto.getQuestionType());
        question.setDifficulty(dto.getDifficulty());
        question.getOptions().clear();
        Question mappedQuestion = questionMapper.toEntity(dto);
        if (mappedQuestion.getOptions() != null) {
            mappedQuestion.getOptions().forEach(opt -> {
                opt.setQuestion(question); // refacem legatura parinte-copil
                question.getOptions().add(opt); // le adaugam pe cele noi
            });
        }
        return questionMapper.toResponseDto(questionRepository.save(question));
    }
    @Transactional
    public void deleteQuestion(UUID testId, Integer questionId, UUID professorId) {
        Test test = getValidTestForModification(testId, professorId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new DoesNotExistException("Question not found."));
        if (!question.getTest().getId().equals(test.getId())) {
            throw new ValidationException("This question does not belong to the specified test.");
        }
        questionRepository.delete(question);
    }

    private Test getValidTestForModification(UUID testId, UUID professorId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException("Test not found."));

        if (!test.getCreatedBy().equals(professorId)) {
            throw new AccessDeniedException("You are not the owner of this test.");
        }

        if (!"DRAFT".equals(test.getStatus().name())) {
            throw new ValidationException("Test is not in DRAFT state and cannot be modified.");
        }
        return test;
    }
}
