package org.elearning.backend.assessment;

import org.elearning.backend.assessment.dto.assigment_dto.TestEditDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEntityDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionOptionsDataDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.exception.TestNotPublishedException;
import org.elearning.backend.assessment.exception.UserHasNoPermissionException;
import org.elearning.backend.assessment.mapper.QuestionOptionMapper;
import org.elearning.backend.assessment.mapper.TestMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
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
        when(testRepository.lessonHasTest(lessonId)).thenReturn(0);
        when(testMapper.toEntity(request)).thenReturn(entity);
        when(testRepository.save(entity)).thenReturn(saved);
        when(testMapper.toEntityDto(saved)).thenReturn(response);

        TestEntityDto result = testService.createNewTest(lessonId, request, professorId);

        assertThat(result).isSameAs(response);
        assertThat(entity.getLessonId()).isEqualTo(lessonId);
        assertThat(entity.getCreatedBy()).isEqualTo(professorId);
        assertThat(entity.getStatus()).isEqualTo(TestStatus.DRAFT);
    }

    @Test
    void publishTest_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.publishTest(testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
    }

    @Test
    void getTestDetails_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.getTestDetails(testId))
                .isInstanceOf(DoesNotExistException.class)
                .hasMessageContaining("Test does not exist");
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
    void deleteTest_shouldThrowWhenTestDoesNotExist() {
        when(testRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.deleteTest(testId))
                .isInstanceOf(TestNotPublishedException.class)
                .hasMessageContaining("Test does not exist");
    }
}
