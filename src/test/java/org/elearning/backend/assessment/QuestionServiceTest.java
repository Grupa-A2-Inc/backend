package org.elearning.backend.assessment;
import jakarta.validation.ValidationException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.elearning.backend.assessment.dto.question_dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionResponseDto;
import org.elearning.backend.assessment.dto.question_option_dto.OptionRequestDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.mapper.QuestionMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

// org.elearning.backend.assessment.model.Test is referenced fully-qualified everywhere
// to avoid import collision with org.junit.jupiter.api.Test

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private TestRepository testRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;

    private UUID teacherId;
    private UUID testId;
    private org.elearning.backend.assessment.model.Test draftTest;

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        testId    = UUID.randomUUID();

        draftTest = new org.elearning.backend.assessment.model.Test();
        draftTest.setId(testId);
        draftTest.setCreatedBy(teacherId);
        draftTest.setStatus(TestStatus.DRAFT);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private OptionRequestDto option(String text, boolean correct) {
        OptionRequestDto o = new OptionRequestDto();
        o.setText(text);
        o.setIsCorrect(correct);
        return o;
    }

    private List<OptionRequestDto> singleChoiceOptions() {
        return List.of(option("A", true), option("B", false), option("C", false));
    }

    private List<OptionRequestDto> multipleChoiceOptions() {
        return List.of(option("X", true), option("Y", true), option("Z", false));
    }

    private List<OptionRequestDto> trueFalseOptions(boolean trueIsCorrect) {
        return List.of(option("True", trueIsCorrect), option("False", !trueIsCorrect));
    }

    private QuestionRequestDto requestDto(QuestionType type, List<OptionRequestDto> options) {
        QuestionRequestDto dto = new QuestionRequestDto();
        dto.setContent("Sample question");
        dto.setQuestionType(type);
        dto.setDifficulty(BigDecimal.valueOf(2));
        dto.setOptions(options);
        return dto;
    }

    private Question questionEntity(Integer id, org.elearning.backend.assessment.model.Test test) {
        Question q = new Question();
        q.setId(id);
        q.setTest(test);
        q.setContent("Sample question");
        q.setOptions(new ArrayList<>());
        return q;
    }

    private QuestionResponseDto responseDto(Integer id) {
        return QuestionResponseDto.builder()
                .questionId(id.longValue())
                .content("Sample question")
                .questionType(QuestionType.SINGLE_CHOICE)
                .difficulty(BigDecimal.valueOf(2))
                .options(List.of())
                .build();
    }

    /** Constructs a Test entity that does NOT belong to the current testId. */
    private org.elearning.backend.assessment.model.Test otherTest() {
        org.elearning.backend.assessment.model.Test t =
                new org.elearning.backend.assessment.model.Test();
        t.setId(UUID.randomUUID());
        return t;
    }

    // =========================================================================
    // createQuestion
    // =========================================================================

    @Nested
    class CreateQuestion {

        @Test
        void shouldCreateQuestion_whenSingleChoiceIsValid() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question entity = questionEntity(1, draftTest);
            Question saved  = questionEntity(1, draftTest);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionMapper.toEntity(dto)).thenReturn(entity);
            when(questionRepository.save(entity)).thenReturn(saved);
            when(questionMapper.toResponseDto(saved)).thenReturn(responseDto(1));

            QuestionResponseDto result = questionService.createQuestion(testId, dto, teacherId);

            assertThat(result.getQuestionId()).isEqualTo(1L);
            verify(questionRepository).save(entity);
        }

        @Test
        void shouldCreateQuestion_whenMultipleChoiceIsValid() {
            QuestionRequestDto dto = requestDto(QuestionType.MULTI_CHOICE, multipleChoiceOptions());
            Question entity = questionEntity(2, draftTest);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionMapper.toEntity(dto)).thenReturn(entity);
            when(questionRepository.save(entity)).thenReturn(entity);
            when(questionMapper.toResponseDto(entity)).thenReturn(responseDto(2));

            assertThat(questionService.createQuestion(testId, dto, teacherId)).isNotNull();
            verify(questionRepository).save(entity);
        }

        @Test
        void shouldCreateQuestion_whenTrueFalseIsValid() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE, trueFalseOptions(true));
            Question entity = questionEntity(3, draftTest);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionMapper.toEntity(dto)).thenReturn(entity);
            when(questionRepository.save(entity)).thenReturn(entity);
            when(questionMapper.toResponseDto(entity)).thenReturn(responseDto(3));

            assertThat(questionService.createQuestion(testId, dto, teacherId)).isNotNull();
        }

        @Test
        void shouldSetQuestionOnOptions_whenEntityHasOptions() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question entity = questionEntity(1, draftTest);
            QuestionOption opt = new QuestionOption();
            entity.getOptions().add(opt);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionMapper.toEntity(dto)).thenReturn(entity);
            when(questionRepository.save(entity)).thenReturn(entity);
            when(questionMapper.toResponseDto(entity)).thenReturn(responseDto(1));

            questionService.createQuestion(testId, dto, teacherId);

            assertThat(opt.getQuestion()).isSameAs(entity);
        }

        @Test
        void shouldCreateQuestion_whenMappedEntityHasNoOptions() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question entity = questionEntity(4, draftTest);
            entity.setOptions(null);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionMapper.toEntity(dto)).thenReturn(entity);
            when(questionRepository.save(entity)).thenReturn(entity);
            when(questionMapper.toResponseDto(entity)).thenReturn(responseDto(4));

            QuestionResponseDto result = questionService.createQuestion(testId, dto, teacherId);

            assertThat(result.getQuestionId()).isEqualTo(4L);
            verify(questionRepository).save(entity);
        }

        @Test
        void shouldThrow_whenTestNotFound() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            when(testRepository.findById(testId)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
            verifyNoInteractions(questionRepository);
        }

        @Test
        void shouldThrowAccessDenied_whenTeacherIsNotOwner() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            UUID otherId = UUID.randomUUID();
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, otherId);

            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldThrowValidation_whenTestIsNotDraft() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            draftTest.setStatus(TestStatus.PUBLISHED);
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        void shouldThrowValidation_whenOptionsListIsNull() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, null);
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least one option");
        }

        @Test
        void shouldThrowValidation_whenOptionsListIsEmpty() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, List.of());
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call).isInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrowValidation_whenSingleChoiceHasOnlyOneOption() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, List.of(option("A", true)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 2 options");
        }

        @Test
        void shouldThrowValidation_whenSingleChoiceHasNoCorrectOption() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE,
                    List.of(option("A", false), option("B", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("exactly one correct");
        }

        @Test
        void shouldThrowValidation_whenSingleChoiceHasTwoCorrectOptions() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE,
                    List.of(option("A", true), option("B", true)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("exactly one correct");
        }

        @Test
        void shouldThrowValidation_whenMultipleChoiceHasOnlyOneOption() {
            QuestionRequestDto dto = requestDto(QuestionType.MULTI_CHOICE, List.of(option("A", true)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 2 options");
        }

        @Test
        void shouldThrowValidation_whenMultipleChoiceHasOnlyOneCorrectOption() {
            QuestionRequestDto dto = requestDto(QuestionType.MULTI_CHOICE,
                    List.of(option("A", true), option("B", false), option("C", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("AT LEAST 2 correct");
        }

        @Test
        void shouldThrowValidation_whenTrueFalseHasThreeOptions() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE,
                    List.of(option("True", true), option("False", false), option("Maybe", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("exactly 2 options");
        }

        @Test
        void shouldThrowValidation_whenTrueFalseHasNoCorrectOption() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE,
                    List.of(option("True", false), option("False", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("exactly one correct");
        }

        @Test
        void shouldThrowValidation_whenTrueFalseOptionsHaveWrongText() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE,
                    List.of(option("Yes", true), option("No", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("'True' and 'False'");
        }

        @Test
        void shouldThrowValidation_whenQuestionTypeIsUnsupported() {
            QuestionRequestDto dto = requestDto(null, trueFalseOptions(true));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not supported");
        }

        @Test
        void shouldThrowValidation_whenTrueFalseHasOnlyFalseKeyword() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE,
                    List.of(option("False", true), option("Maybe", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("'True' and 'False'");
        }

        @Test
        void shouldThrowValidation_whenTrueFalseHasOnlyTrueKeyword() {
            QuestionRequestDto dto = requestDto(QuestionType.TRUE_FALSE,
                    List.of(option("True", true), option("Maybe", false)));
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.createQuestion(testId, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("'True' and 'False'");
        }
    }

    // =========================================================================
    // getAllQuestionsForTest
    // =========================================================================

    @Test
    void shouldReturnAllQuestionsForTest() {
        Question q1 = questionEntity(1, draftTest);
        Question q2 = questionEntity(2, draftTest);
        when(questionRepository.findByTestId(testId)).thenReturn(List.of(q1, q2));
        when(questionMapper.toResponseDto(q1)).thenReturn(responseDto(1));
        when(questionMapper.toResponseDto(q2)).thenReturn(responseDto(2));

        List<QuestionResponseDto> result = questionService.getAllQuestionsForTest(testId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(QuestionResponseDto::getQuestionId).containsExactly(1L, 2L);
    }

    // =========================================================================
    // getQuestionById
    // =========================================================================

    @Nested
    class GetQuestionById {

        @Test
        void shouldReturnQuestion_whenItBelongsToTest() {
            Question question = questionEntity(10, draftTest);
            when(questionRepository.findById(10)).thenReturn(Optional.of(question));
            when(questionMapper.toResponseDto(question)).thenReturn(responseDto(10));

            QuestionResponseDto result = questionService.getQuestionById(testId, 10);

            assertThat(result.getQuestionId()).isEqualTo(10L);
        }

        @Test
        void shouldThrow_whenQuestionNotFound() {
            when(questionRepository.findById(999)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.getQuestionById(testId, 999);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
        }

        @Test
        void shouldThrowValidation_whenQuestionBelongsToDifferentTest() {
            Question question = questionEntity(10, otherTest());
            when(questionRepository.findById(10)).thenReturn(Optional.of(question));

            ThrowingCallable call = () -> questionService.getQuestionById(testId, 10);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // =========================================================================
    // getFilteredAndSortedQuestions
    // =========================================================================

    @Nested
    class GetFilteredAndSortedQuestions {

        @Test
        void shouldReturnAllQuestions_whenNoFiltersApplied() {
            Question q1 = questionEntity(1, draftTest);
            Question q2 = questionEntity(2, draftTest);
            when(questionRepository.findFilteredQuestions(eq(testId), isNull(), isNull(), any(Sort.class)))
                    .thenReturn(List.of(q1, q2));
            when(questionMapper.toResponseDto(q1)).thenReturn(responseDto(1));
            when(questionMapper.toResponseDto(q2)).thenReturn(responseDto(2));

            List<QuestionResponseDto> result =
                    questionService.getFilteredAndSortedQuestions(testId, null, null, null, "asc");

            assertThat(result).hasSize(2);
        }

        @Test
        void shouldFilterByType_whenQuestionTypeProvided() {
            Question q1 = questionEntity(1, draftTest);
            when(questionRepository.findFilteredQuestions(
                    eq(testId), eq(QuestionType.SINGLE_CHOICE), isNull(), any()))
                    .thenReturn(List.of(q1));
            when(questionMapper.toResponseDto(q1)).thenReturn(responseDto(1));

            List<QuestionResponseDto> result = questionService.getFilteredAndSortedQuestions(
                    testId, QuestionType.SINGLE_CHOICE, null, "displayOrder", "asc");

            assertThat(result).hasSize(1);
        }

        @Test
        void shouldFilterByDifficulty_whenDifficultyProvided() {
            Question q1 = questionEntity(1, draftTest);
            when(questionRepository.findFilteredQuestions(
                    eq(testId), isNull(), eq(BigDecimal.ONE), any()))
                    .thenReturn(List.of(q1));
            when(questionMapper.toResponseDto(q1)).thenReturn(responseDto(1));

            List<QuestionResponseDto> result = questionService.getFilteredAndSortedQuestions(
                    testId, null, BigDecimal.ONE, "displayOrder", "asc");

            assertThat(result).hasSize(1);
        }

        @Test
        void shouldSortDescending_whenSortDirIsDesc() {
            Sort expectedSort = Sort.by(Sort.Direction.DESC, "displayOrder");
            when(questionRepository.findFilteredQuestions(eq(testId), isNull(), isNull(), eq(expectedSort)))
                    .thenReturn(List.of());

            questionService.getFilteredAndSortedQuestions(testId, null, null, "displayOrder", "desc");

            verify(questionRepository).findFilteredQuestions(testId, null, null, expectedSort);
        }

        @Test
        void shouldDefaultSortByDisplayOrder_whenSortByIsNull() {
            Sort expectedSort = Sort.by(Sort.Direction.ASC, "displayOrder");
            when(questionRepository.findFilteredQuestions(eq(testId), isNull(), isNull(), eq(expectedSort)))
                    .thenReturn(List.of());

            questionService.getFilteredAndSortedQuestions(testId, null, null, null, "asc");

            verify(questionRepository).findFilteredQuestions(testId, null, null, expectedSort);
        }

        @Test
        void shouldDefaultSortByDisplayOrder_whenSortByIsEmpty() {
            Sort expectedSort = Sort.by(Sort.Direction.ASC, "displayOrder");
            when(questionRepository.findFilteredQuestions(eq(testId), isNull(), isNull(), eq(expectedSort)))
                    .thenReturn(List.of());

            questionService.getFilteredAndSortedQuestions(testId, null, null, "", "asc");

            verify(questionRepository).findFilteredQuestions(testId, null, null, expectedSort);
        }

        @Test
        void shouldReturnEmptyList_whenNoQuestionsMatch() {
            when(questionRepository.findFilteredQuestions(any(), any(), any(), any()))
                    .thenReturn(List.of());

            List<QuestionResponseDto> result = questionService.getFilteredAndSortedQuestions(
                    testId, QuestionType.TRUE_FALSE, null, "displayOrder", "asc");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // updateQuestion
    // =========================================================================

    @Nested
    class UpdateQuestion {

        @Test
        void shouldUpdateQuestion_whenRequestIsValid() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question existing = questionEntity(1, draftTest);
            Question mapped   = questionEntity(1, draftTest);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(existing));
            when(questionMapper.toEntity(dto)).thenReturn(mapped);
            when(questionRepository.save(existing)).thenReturn(existing);
            when(questionMapper.toResponseDto(existing)).thenReturn(responseDto(1));

            QuestionResponseDto result = questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThat(result).isNotNull();
            verify(questionRepository).save(existing);
        }

        @Test
        void shouldClearAndReAddOptions_onUpdate() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());

            QuestionOption oldOpt = new QuestionOption();
            oldOpt.setText("OLD");
            Question existing = questionEntity(1, draftTest);
            existing.getOptions().add(oldOpt);

            QuestionOption newOpt = new QuestionOption();
            newOpt.setText("NEW");
            Question mapped = new Question();
            mapped.setOptions(new ArrayList<>(List.of(newOpt)));

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(existing));
            when(questionMapper.toEntity(dto)).thenReturn(mapped);
            when(questionRepository.save(existing)).thenReturn(existing);
            when(questionMapper.toResponseDto(existing)).thenReturn(responseDto(1));

            questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThat(existing.getOptions()).doesNotContain(oldOpt);
            assertThat(existing.getOptions()).contains(newOpt);
            assertThat(newOpt.getQuestion()).isSameAs(existing);
        }

        @Test
        void shouldUpdateQuestion_whenMappedQuestionHasNoOptions() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question existing = questionEntity(1, draftTest);
            existing.getOptions().add(new QuestionOption());
            Question mapped = new Question();
            mapped.setOptions(null);

            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(existing));
            when(questionMapper.toEntity(dto)).thenReturn(mapped);
            when(questionRepository.save(existing)).thenReturn(existing);
            when(questionMapper.toResponseDto(existing)).thenReturn(responseDto(1));

            QuestionResponseDto result = questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThat(result).isNotNull();
            assertThat(existing.getOptions()).isEmpty();
        }

        @Test
        void shouldThrow_whenTestNotFound() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            when(testRepository.findById(testId)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
        }

        @Test
        void shouldThrow_whenQuestionNotFound() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(999)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.updateQuestion(testId, 999, dto, teacherId);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
        }

        @Test
        void shouldThrowValidation_whenQuestionBelongsToDifferentTest() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            Question question = questionEntity(1, otherTest());
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(question));

            ThrowingCallable call = () -> questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void shouldThrowValidation_whenOptionsAreInvalid() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE,
                    List.of(option("A", false), option("B", false)));
            Question existing = questionEntity(1, draftTest);
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(existing));

            ThrowingCallable call = () -> questionService.updateQuestion(testId, 1, dto, teacherId);

            assertThatThrownBy(call).isInstanceOf(ValidationException.class);
            verify(questionRepository, never()).save(any());
        }

        @Test
        void shouldThrowAccessDenied_whenTeacherIsNotOwner() {
            QuestionRequestDto dto = requestDto(QuestionType.SINGLE_CHOICE, singleChoiceOptions());
            UUID otherId = UUID.randomUUID();
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.updateQuestion(testId, 1, dto, otherId);

            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }
    }

    // =========================================================================
    // deleteQuestion
    // =========================================================================

    @Nested
    class DeleteQuestion {

        @Test
        void shouldDeleteQuestion_whenRequestIsValid() {
            Question question = questionEntity(1, draftTest);
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(question));

            questionService.deleteQuestion(testId, 1, teacherId);

            verify(questionRepository).delete(question);
        }

        @Test
        void shouldThrow_whenTestNotFound() {
            when(testRepository.findById(testId)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.deleteQuestion(testId, 1, teacherId);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
            verify(questionRepository, never()).delete(any());
        }

        @Test
        void shouldThrow_whenQuestionNotFound() {
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(999)).thenReturn(Optional.empty());

            ThrowingCallable call = () -> questionService.deleteQuestion(testId, 999, teacherId);

            assertThatThrownBy(call).isInstanceOf(DoesNotExistException.class);
        }

        @Test
        void shouldThrowValidation_whenQuestionBelongsToDifferentTest() {
            Question question = questionEntity(1, otherTest());
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));
            when(questionRepository.findById(1)).thenReturn(Optional.of(question));

            ThrowingCallable call = () -> questionService.deleteQuestion(testId, 1, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not belong");
            verify(questionRepository, never()).delete(any());
        }

        @Test
        void shouldThrowAccessDenied_whenTeacherIsNotOwner() {
            UUID otherId = UUID.randomUUID();
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.deleteQuestion(testId, 1, otherId);

            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldThrowValidation_whenTestIsNotDraft() {
            draftTest.setStatus(TestStatus.PUBLISHED);
            when(testRepository.findById(testId)).thenReturn(Optional.of(draftTest));

            ThrowingCallable call = () -> questionService.deleteQuestion(testId, 1, teacherId);

            assertThatThrownBy(call)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT");
        }
    }
}
