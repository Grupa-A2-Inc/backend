package org.elearning.backend.assessment;

import org.elearning.backend.assessment.dto.assigment_dto.TestEditDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEntityDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionOptionsDataDto;
import org.elearning.backend.assessment.exception.AlreadyPublishedException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.exception.TestMustBeDraftException;
import org.elearning.backend.assessment.exception.TestNotPublishedException;
import org.elearning.backend.assessment.exception.TestCannotBePublished;
import org.elearning.backend.assessment.exception.TestVersionConflictException;
import org.elearning.backend.assessment.exception.UserHasNoPermissionException;
import org.elearning.backend.assessment.mapper.QuestionOptionMapper;
import org.elearning.backend.assessment.mapper.TestMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionOptionRepository;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.service.TestService;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestMapper testMapper;

    @Mock
    private TestRepository testRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private QuestionOptionMapper questionOptionMapper;

    @InjectMocks
    private TestService testService;

    private UUID lessonId;
    private UUID testId;
    private UUID professorId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
        testId = UUID.randomUUID();
        professorId = UUID.randomUUID();
    }

    @Test
    void createNewTest_shouldThrowWhenLessonDoesNotExist() {
        TestEditDto testEditDto = new TestEditDto();
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> testService.createNewTest(lessonId, testEditDto, professorId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Lesson does not exist");
    }

    @Test
    void getTestFromLesson_shouldThrowWhenLessonDoesNotExist() {
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> testService.getTestFromLesson(lessonId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Lesson does not exist");
    }

    @Test
    void getListOfQuestions_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.existsById(testId)).thenReturn(false);

        assertThatThrownBy(() -> testService.getListOfQuestions(testId, RoleName.TEACHER))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void getListOfQuestions_shouldThrowWhenUserIsNotTeacher() {
        when(testRepository.existsById(testId)).thenReturn(true);

        assertThatThrownBy(() -> testService.getListOfQuestions(testId, RoleName.STUDENT))
                .isInstanceOf(UserHasNoPermissionException.class)
                .hasMessageContaining("teacher");
    }

    @Test
    void getListOfQuestions_shouldReturnTeacherViewWithOptionsAndCorrectAnswers() {
        Question question = new Question();
        question.setId(11);
        question.setQuestionType(org.elearning.backend.assessment.model.QuestionType.SINGLE_CHOICE);
        question.setContent("What is 2 + 2?");

        List<QuestionOption> options = List.of(new QuestionOption(), new QuestionOption());
        List<QuestionOption> correctOptions = List.of(new QuestionOption());

        QuestionOptionsDataDto optionDto = new QuestionOptionsDataDto();
        optionDto.setId(1);
        optionDto.setText("4");

        QuestionOptionsDataDto correctOptionDto = new QuestionOptionsDataDto();
        correctOptionDto.setId(1);
        correctOptionDto.setText("4");

        when(testRepository.existsById(testId)).thenReturn(true);
        when(questionRepository.findByTestIdWithOptions(testId)).thenReturn(List.of(question));
        when(questionOptionRepository.findByQuestionId(11)).thenReturn(options);
        when(questionOptionRepository.findByQuestionIdAndIsCorrectTrue(11)).thenReturn(correctOptions);
        when(questionOptionMapper.toDataForUsersDto(options)).thenReturn(List.of(optionDto));
        when(questionOptionMapper.toDataForUsersDto(correctOptions)).thenReturn(List.of(correctOptionDto));

        List<QuestionDataForUsersDto> result = testService.getListOfQuestions(testId, RoleName.TEACHER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOptions()).containsExactly(optionDto);
        assertThat(result.get(0).getCorrectAnswers()).containsExactly(correctOptionDto);
        verify(questionOptionRepository).findByQuestionId(11);
        verify(questionOptionRepository).findByQuestionIdAndIsCorrectTrue(11);
    }

    @Test
    void createNewTest_shouldInitializeDraftTestAndReturnMappedDto() {
        TestEditDto request = new TestEditDto("Quiz", "desc", 300, true);
        org.elearning.backend.assessment.model.Test entity = new org.elearning.backend.assessment.model.Test();
        org.elearning.backend.assessment.model.Test saved = new org.elearning.backend.assessment.model.Test();
        TestEntityDto response = new TestEntityDto();

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(testRepository.existsByLessonId(lessonId)).thenReturn(false);
        when(testMapper.toEntity(request)).thenReturn(entity);
        when(testRepository.save(entity)).thenReturn(saved);
        when(testMapper.toEntityDto(saved)).thenReturn(response);

        TestEntityDto result = testService.createNewTest(lessonId, request, professorId);

        assertThat(result).isSameAs(response);
        assertThat(entity.getLessonId()).isEqualTo(lessonId);
        assertThat(entity.getCreatedBy()).isEqualTo(professorId);
        assertThat(entity.getVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(TestStatus.DRAFT);
    }

    @Test
    void createNewTest_shouldThrowWhenLessonAlreadyHasTest() {
        TestEditDto request = new TestEditDto();

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(testRepository.existsByLessonId(lessonId)).thenReturn(true);

        assertThatThrownBy(() -> testService.createNewTest(lessonId, request, professorId))
                .isInstanceOf(org.elearning.backend.assessment.exception.LessonAlreadyHasTestException.class)
                .hasMessageContaining("Lesson already has a test");
    }

    @Test
    void ensureEditableDraft_shouldReturnExistingDraft() {
        org.elearning.backend.assessment.model.Test draft = new org.elearning.backend.assessment.model.Test();
        draft.setId(testId);
        draft.setStatus(TestStatus.DRAFT);
        TestEntityDto response = new TestEntityDto();

        when(testRepository.findById(testId)).thenReturn(Optional.of(draft));
        when(testMapper.toEntityDto(draft)).thenReturn(response);

        TestEntityDto result = testService.ensureEditableDraft(testId);

        assertThat(result).isSameAs(response);
    }

    @Test
    void ensureEditableDraft_shouldThrowWhenSourceTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.ensureEditableDraft(testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void ensureEditableDraft_shouldReturnExistingLessonDraftForPublishedSource() {
        org.elearning.backend.assessment.model.Test published = new org.elearning.backend.assessment.model.Test();
        published.setId(testId);
        published.setLessonId(lessonId);
        published.setStatus(TestStatus.PUBLISHED);

        org.elearning.backend.assessment.model.Test existingDraft = new org.elearning.backend.assessment.model.Test();
        existingDraft.setLessonId(lessonId);
        existingDraft.setStatus(TestStatus.DRAFT);

        TestEntityDto response = new TestEntityDto();

        when(testRepository.findById(testId)).thenReturn(Optional.of(published));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.of(existingDraft));
        when(testMapper.toEntityDto(existingDraft)).thenReturn(response);

        TestEntityDto result = testService.ensureEditableDraft(testId);

        assertThat(result).isSameAs(response);
    }

    @Test
    void ensureEditableDraft_shouldClonePublishedSourceWithoutQuestionsWhenNoVersionsExist() {
        UUID sourceId = UUID.randomUUID();
        org.elearning.backend.assessment.model.Test published = new org.elearning.backend.assessment.model.Test();
        published.setId(sourceId);
        published.setLessonId(lessonId);
        published.setCreatedBy(professorId);
        published.setTitle("Published");
        published.setDescription("desc");
        published.setTimeLimitSec(1200);
        published.setAiEnabled(true);
        published.setStatus(TestStatus.PUBLISHED);

        TestEntityDto response = new TestEntityDto();
        org.elearning.backend.assessment.model.Test[] savedDraftRef = new org.elearning.backend.assessment.model.Test[1];

        when(testRepository.findById(sourceId)).thenReturn(Optional.of(published));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(testRepository.findMaxVersionByLessonId(lessonId)).thenReturn(null);
        when(testRepository.save(org.mockito.ArgumentMatchers.any(org.elearning.backend.assessment.model.Test.class))).thenAnswer(invocation -> {
            org.elearning.backend.assessment.model.Test savedDraft = invocation.getArgument(0);
            savedDraft.setId(UUID.randomUUID());
            savedDraftRef[0] = savedDraft;
            return savedDraft;
        });
        when(questionRepository.findByTestIdWithOptions(sourceId)).thenReturn(List.of());
        when(testMapper.toEntityDto(org.mockito.ArgumentMatchers.any(org.elearning.backend.assessment.model.Test.class))).thenReturn(response);

        TestEntityDto result = testService.ensureEditableDraft(sourceId);

        assertThat(result).isSameAs(response);
        assertThat(savedDraftRef[0].getVersion()).isEqualTo(1);
        assertThat(savedDraftRef[0].getPreviousVersionId()).isEqualTo(sourceId);
        assertThat(savedDraftRef[0].getStatus()).isEqualTo(TestStatus.DRAFT);
    }

    @Test
    void ensureEditableDraft_shouldClonePublishedSourceQuestionsAndOptions() {
        UUID sourceId = UUID.randomUUID();
        UUID savedDraftId = UUID.randomUUID();
        org.elearning.backend.assessment.model.Test published = new org.elearning.backend.assessment.model.Test();
        published.setId(sourceId);
        published.setLessonId(lessonId);
        published.setCreatedBy(professorId);
        published.setTitle("Published");
        published.setDescription("desc");
        published.setTimeLimitSec(900);
        published.setAiEnabled(false);
        published.setStatus(TestStatus.PUBLISHED);

        Question sourceQuestion = new Question();
        sourceQuestion.setSubjectId(10);
        sourceQuestion.setTopicId(20);
        sourceQuestion.setQuestionType(QuestionType.MULTI_CHOICE);
        sourceQuestion.setContent("Pick all valid answers");
        sourceQuestion.setDifficulty(java.math.BigDecimal.valueOf(0.7));
        sourceQuestion.setIsActive(true);
        sourceQuestion.setSource(QuestionSource.AI_GENERATED);

        QuestionOption sourceOption = new QuestionOption();
        sourceOption.setText("A");
        sourceOption.setDisplayOrder(1);
        sourceOption.setIsCorrect(true);
        sourceOption.setQuestion(sourceQuestion);
        sourceQuestion.setOptions(List.of(sourceOption));

        TestEntityDto response = new TestEntityDto();
        org.elearning.backend.assessment.model.Test[] savedDraftRef = new org.elearning.backend.assessment.model.Test[1];

        when(testRepository.findById(sourceId)).thenReturn(Optional.of(published));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(testRepository.findMaxVersionByLessonId(lessonId)).thenReturn(4);
        when(testRepository.save(org.mockito.ArgumentMatchers.any(org.elearning.backend.assessment.model.Test.class))).thenAnswer(invocation -> {
            org.elearning.backend.assessment.model.Test savedDraft = invocation.getArgument(0);
            savedDraft.setId(savedDraftId);
            savedDraftRef[0] = savedDraft;
            return savedDraft;
        });
        when(questionRepository.findByTestIdWithOptions(sourceId)).thenReturn(List.of(sourceQuestion));
        when(questionRepository.saveAll(org.mockito.ArgumentMatchers.anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(testMapper.toEntityDto(org.mockito.ArgumentMatchers.any(org.elearning.backend.assessment.model.Test.class))).thenReturn(response);

        TestEntityDto result = testService.ensureEditableDraft(sourceId);

        assertThat(result).isSameAs(response);
        assertThat(savedDraftRef[0].getVersion()).isEqualTo(5);
        org.mockito.ArgumentCaptor<List<Question>> questionsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(questionRepository).saveAll(questionsCaptor.capture());
        List<Question> clonedQuestions = questionsCaptor.getValue();
        assertThat(clonedQuestions).hasSize(1);
        Question clonedQuestion = clonedQuestions.get(0);
        assertThat(clonedQuestion.getTest()).isSameAs(savedDraftRef[0]);
        assertThat(clonedQuestion.getQuestionType()).isEqualTo(QuestionType.MULTI_CHOICE);
        assertThat(clonedQuestion.getContent()).isEqualTo("Pick all valid answers");
        assertThat(clonedQuestion.getOptions()).hasSize(1);
        assertThat(clonedQuestion.getOptions().get(0).getQuestion()).isSameAs(clonedQuestion);
        assertThat(clonedQuestion.getOptions().get(0).getText()).isEqualTo("A");
        assertThat(clonedQuestion.getOptions().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(clonedQuestion.getOptions().get(0).getIsCorrect()).isTrue();
    }

    @Test
    void ensureEditableDraft_shouldRejectSupersededSourceWithoutDraft() {
        org.elearning.backend.assessment.model.Test superseded = new org.elearning.backend.assessment.model.Test();
        superseded.setId(testId);
        superseded.setLessonId(lessonId);
        superseded.setStatus(TestStatus.SUPERSEDED);

        when(testRepository.findById(testId)).thenReturn(Optional.of(superseded));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.ensureEditableDraft(testId))
                .isInstanceOf(TestVersionConflictException.class)
                .hasMessageContaining("published tests");
    }

    @Test
    void publishTest_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.publishTest(testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void publishTest_shouldThrowWhenTestHasNoQuestions() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.DRAFT);
        test.setQuestions(List.of());

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.publishTest(testId))
                .isInstanceOf(TestCannotBePublished.class)
                .hasMessageContaining("cannot be published");
    }

    @Test
    void publishTest_shouldThrowWhenTestAlreadyPublished() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.PUBLISHED);
        test.setQuestions(List.of(new Question()));

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.publishTest(testId))
                .isInstanceOf(AlreadyPublishedException.class)
                .hasMessageContaining("already published");
    }

    @Test
    void publishTest_shouldThrowWhenTestIsNotDraftAndNotPublished() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.SUPERSEDED);
        test.setQuestions(List.of(new Question()));

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.publishTest(testId))
                .isInstanceOf(TestMustBeDraftException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void getTestDetails_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.getTestDetails(testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void getTestFromLesson_shouldReturnPublishedTestForStudent() {
        org.elearning.backend.assessment.model.Test published = new org.elearning.backend.assessment.model.Test();
        TestEntityDto response = new TestEntityDto();

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.PUBLISHED))
                .thenReturn(Optional.of(published));
        when(testMapper.toEntityDto(published)).thenReturn(response);

        TestEntityDto result = testService.getTestFromLesson(lessonId, RoleName.STUDENT);

        assertThat(result).isSameAs(response);
    }

    @Test
    void getTestFromLesson_shouldThrowForStudentWhenNoPublishedTestExists() {
        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.getTestFromLesson(lessonId, RoleName.STUDENT))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void getTestFromLesson_shouldUseTeacherOverload() {
        org.elearning.backend.assessment.model.Test draft = new org.elearning.backend.assessment.model.Test();
        TestEntityDto response = new TestEntityDto();

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.of(draft));
        when(testMapper.toEntityDto(draft)).thenReturn(response);

        TestEntityDto result = testService.getTestFromLesson(lessonId);

        assertThat(result).isSameAs(response);
    }

    @Test
    void updateTest_shouldThrowWhenTestDoesNotExist() {
        TestEditDto testEditDto = new TestEditDto();
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.updateTest(testEditDto, testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void updateTest_shouldThrowWhenTestIsNotDraft() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.PUBLISHED);
        TestEditDto editDto = new TestEditDto();

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.updateTest(editDto, testId))
                .isInstanceOf(TestMustBeDraftException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void updateTest_shouldApplyAllEditableFields() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.DRAFT);
        TestEditDto editDto = new TestEditDto("New title", "New desc", 450, false);
        TestEntityDto response = new TestEntityDto();

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(testRepository.save(test)).thenReturn(test);
        when(testMapper.toEntityDto(test)).thenReturn(response);

        TestEntityDto result = testService.updateTest(editDto, testId);

        assertThat(result).isSameAs(response);
        assertThat(test.getTitle()).isEqualTo("New title");
        assertThat(test.getDescription()).isEqualTo("New desc");
        assertThat(test.getTimeLimitSec()).isEqualTo(450);
        assertThat(test.getAiEnabled()).isFalse();
    }

    @Test
    void deleteTest_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.deleteTest(testId))
                .isInstanceOf(TestNotPublishedException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void deleteTest_shouldThrowWhenTestIsNotDraft() {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setStatus(TestStatus.PUBLISHED);

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.deleteTest(testId))
                .isInstanceOf(TestMustBeDraftException.class)
                .hasMessageContaining("draft");
    }
}
