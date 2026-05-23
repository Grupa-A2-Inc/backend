package org.elearning.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.dto.InjectionResultDto;
import org.elearning.backend.ai.exception.ResourceConflictException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiQuestionInjectorServiceTest {

    @Mock private AiQuestionRequestRepository aiQuestionRequestRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private TestRepository testRepository;

    private AiQuestionInjectorService aiQuestionInjectorService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiQuestionInjectorService = new AiQuestionInjectorService(
                aiQuestionRequestRepository,
                questionRepository,
                lessonRepository,
                testRepository,
                objectMapper
        );
    }

    @Test
    void injectQuestions_shouldThrowWhenSomeCorrectAnswersAreNotAmongOptions() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        AiQuestionDto aiQuestion = new AiQuestionDto();
        aiQuestion.setText("Question");
        aiQuestion.setType(QuestionType.MULTI_CHOICE);
        aiQuestion.setAnswers(List.of("A", "B"));
        aiQuestion.setCorrectAnswers(List.of("A", "C"));
        aiQuestion.setDifficulty(1.0);

        AiQuestionRequest request = AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.DONE)
                .generatedQuestions(objectMapper.writeValueAsString(List.of(aiQuestion)))
                .build();

        Course course = new Course();
        course.setCreatedBy(professorId);
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Lesson");
        lesson.setChapter(chapter);

        when(aiQuestionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThatThrownBy(() -> aiQuestionInjectorService.injectQuestions(requestId, professorId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Some correct answers are not among the provided answer options.");
    }

    @Test
    void injectQuestions_shouldReuseExistingDraftWhenNoTestIdIsProvided() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        AiQuestionDto aiQuestion = validQuestion();
        AiQuestionRequest request = request(requestId, lessonId, objectMapper.writeValueAsString(List.of(aiQuestion)));
        Lesson lesson = lesson(lessonId, professorId);
        org.elearning.backend.assessment.model.Test draft = new org.elearning.backend.assessment.model.Test();
        draft.setId(testId);
        draft.setLessonId(lessonId);
        draft.setStatus(TestStatus.DRAFT);

        when(aiQuestionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.of(draft));
        when(questionRepository.countByTestId(testId)).thenReturn(1);

        InjectionResultDto result = aiQuestionInjectorService.injectQuestions(requestId, professorId, null);

        assertThat(result.getTestId()).isEqualTo(testId);
        assertThat(result.isTestCreated()).isFalse();
        assertThat(result.getInjectedCount()).isEqualTo(1);
        verify(questionRepository).saveAll(any());
    }

    @Test
    void injectQuestions_shouldThrowWhenLessonAlreadyHasPublishedTestAndNoDraft() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        AiQuestionRequest request = request(requestId, lessonId, objectMapper.writeValueAsString(List.of(validQuestion())));

        when(aiQuestionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson(lessonId, professorId)));
        when(testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(testRepository.existsByLessonId(lessonId)).thenReturn(true);

        assertThatThrownBy(() -> aiQuestionInjectorService.injectQuestions(requestId, professorId, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("published test already exists");
    }

    @Test
    void injectQuestions_shouldThrowWhenProvidedTestIsNotDraft() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        AiQuestionRequest request = request(requestId, lessonId, objectMapper.writeValueAsString(List.of(validQuestion())));
        org.elearning.backend.assessment.model.Test published = new org.elearning.backend.assessment.model.Test();
        published.setId(testId);
        published.setStatus(TestStatus.PUBLISHED);

        when(aiQuestionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson(lessonId, professorId)));
        when(testRepository.findById(testId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> aiQuestionInjectorService.injectQuestions(requestId, professorId, testId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("only into a draft test");
    }

    private AiQuestionDto validQuestion() {
        AiQuestionDto aiQuestion = new AiQuestionDto();
        aiQuestion.setText("Question");
        aiQuestion.setType(QuestionType.SINGLE_CHOICE);
        aiQuestion.setAnswers(List.of("A", "B"));
        aiQuestion.setCorrectAnswers(List.of("A"));
        aiQuestion.setDifficulty(1.0);
        return aiQuestion;
    }

    private AiQuestionRequest request(UUID requestId, UUID lessonId, String generatedQuestions) {
        return AiQuestionRequest.builder()
                .id(requestId)
                .lessonId(lessonId)
                .status(AiRequestStatus.DONE)
                .generatedQuestions(generatedQuestions)
                .build();
    }

    private Lesson lesson(UUID lessonId, UUID professorId) {
        Course course = new Course();
        course.setCreatedBy(professorId);
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Lesson");
        lesson.setChapter(chapter);
        return lesson;
    }
}
