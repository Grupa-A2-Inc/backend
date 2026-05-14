package org.elearning.backend.content;

import org.elearning.backend.content.dto.LessonDtoEntity;
import org.elearning.backend.content.dto.LessonDtoMetadata;
import org.elearning.backend.content.dto.LessonDtoPost;
import org.elearning.backend.content.exception.ChapterNotFoundException;
import org.elearning.backend.content.exception.InvalidOrderIndexException;
import org.elearning.backend.content.exception.LessonNotFoundException;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.service.LessonService;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.repository.LessonProgressRepository;
import org.elearning.backend.enrollment.service.ProgressCalculatorService;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private ProgressCalculatorService progressCalculatorService;

    @InjectMocks
    private LessonService service;

    private UUID lessonId;
    private UUID chapterId;
    private UUID courseId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
        chapterId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getAllLessonsFromChapterThrowsWhenChapterMissing() {
        when(chapterRepository.existsById(chapterId)).thenReturn(false);

        assertThatThrownBy(() -> service.getAllLessonsFromChapter(chapterId))
                .isInstanceOf(ChapterNotFoundException.class);
    }

    @Test
    void getLessonContentThrowsWhenLessonMissing() {
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> service.getLessonContent(lessonId))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void createNewLessonThrowsWhenChapterMissing() {
        LessonDtoPost request = new LessonDtoPost("Title", "Body");
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createNewLesson(request, chapterId))
                .isInstanceOf(ChapterNotFoundException.class);
    }

    @Test
    void updateLessonMarkdownContentThrowsWhenLessonMissingBeforeUpdate() {
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateLessonMarkdownContent(lessonId, "md"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void updateLessonMarkdownContentThrowsWhenLessonMissingAfterUpdateLookup() {
        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLessonMarkdownContent(lessonId, "md"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void updateLessonMetadataThrowsWhenLessonMissing() {
        LessonDtoMetadata metadata = new LessonDtoMetadata();
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateLessonMetadata(lessonId, metadata))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void updateLessonMetadataThrowsWhenUpdatedLessonCannotBeReloaded() {
        LessonDtoMetadata metadata = new LessonDtoMetadata("New title", null);

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLessonMetadata(lessonId, metadata))
                .isInstanceOf(LessonNotFoundException.class);

        verify(lessonRepository).updateLessonTitle(lessonId, "New title");
        verify(lessonRepository).flush();
    }

    @Test
    void updateLessonMetadataThrowsWhenReorderingCannotFindChapter() {
        LessonDtoMetadata metadata = new LessonDtoMetadata(null, 2);

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLessonMetadata(lessonId, metadata))
                .isInstanceOf(ChapterNotFoundException.class);
    }

    @Test
    void updateLessonMetadataThrowsWhenReorderingCannotFindPreviousOrderIndex() {
        LessonDtoMetadata metadata = new LessonDtoMetadata(null, 2);

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(lessonRepository.findLastOrderIndex(chapterId)).thenReturn(Optional.of(4));
        when(lessonRepository.findOrderIndexFromID(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLessonMetadata(lessonId, metadata))
                .isInstanceOf(InvalidOrderIndexException.class);
    }

    @Test
    void deleteLessonThrowsWhenLessonMissing() {
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteLesson(lessonId))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void deleteLessonThrowsWhenChapterLookupFails() {
        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLesson(lessonId))
                .isInstanceOf(ChapterNotFoundException.class);
    }

    @Test
    void deleteLessonThrowsWhenOrderIndexLookupFails() {
        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(lessonRepository.findOrderIndexFromID(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLesson(lessonId))
                .isInstanceOf(InvalidOrderIndexException.class);
    }

    @Test
    void getLessonByIdReturnsLessonWithoutProgressForNonStudent() {
        Lesson lesson = lesson(lessonId, chapterId, "Lesson");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        LessonDtoEntity result = service.getLessonById(userId, RoleName.TEACHER, lessonId);

        assertThat(result.getId()).isEqualTo(lessonId);
        verify(courseEnrollmentRepository, never()).findByStudentIdAndCourseId(any(), any());
    }

    @Test
    void getLessonByIdThrowsWhenLessonMissing() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLessonById(userId, RoleName.TEACHER, lessonId))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void getLessonByIdThrowsWhenStudentChapterMissing() {
        Lesson lesson = lesson(lessonId, chapterId, "Lesson");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLessonById(userId, RoleName.STUDENT, lessonId))
                .isInstanceOf(ChapterNotFoundException.class);
    }

    @Test
    void getLessonByIdThrowsWhenStudentCourseMissing() {
        Lesson lesson = lesson(lessonId, chapterId, "Lesson");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLessonById(userId, RoleName.STUDENT, lessonId))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void getLessonByIdReturnsLessonWhenStudentIsNotEnrolled() {
        Lesson lesson = lesson(lessonId, chapterId, "Lesson");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.of(courseId));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());

        LessonDtoEntity result = service.getLessonById(userId, RoleName.STUDENT, lessonId);

        assertThat(result.getId()).isEqualTo(lessonId);
        verify(lessonProgressRepository, never()).insertProgressIdempotent(any(), any(), any());
    }

    @Test
    void getLessonByIdHandlesAsyncProgressCalculationFailure() {
        UUID enrollmentId = UUID.randomUUID();
        Lesson lesson = lesson(lessonId, chapterId, "Lesson");
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(enrollmentId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findChapterIdFromID(lessonId)).thenReturn(Optional.of(chapterId));
        when(chapterRepository.findCourseIdFromId(chapterId)).thenReturn(Optional.of(courseId));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.of(enrollment));
        doThrow(new RuntimeException("boom")).when(progressCalculatorService).checkAndMarkCompletion(enrollmentId);

        LessonDtoEntity result = service.getLessonById(userId, RoleName.STUDENT, lessonId);

        assertThat(result.getId()).isEqualTo(lessonId);
        verify(lessonProgressRepository).insertProgressIdempotent(lessonId, userId, enrollmentId);
        verify(progressCalculatorService, timeout(1000)).checkAndMarkCompletion(enrollmentId);
    }

    private Lesson lesson(UUID id, UUID chapterId, String title) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);

        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setChapter(chapter);
        lesson.setTitle(title);
        lesson.setContentMarkdown("content");
        lesson.setOrderIndex(1);
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setUpdatedAt(LocalDateTime.now());
        return lesson;
    }
}
