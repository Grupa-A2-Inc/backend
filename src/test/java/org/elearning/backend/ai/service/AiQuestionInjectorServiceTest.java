package org.elearning.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiQuestionDto;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AiQuestionRequest;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AiQuestionRequestRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
}
