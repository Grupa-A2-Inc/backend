package org.elearning.backend.content;

import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.dto.ResponseCourseDto;
import org.elearning.backend.content.dto.ResponseCourseFullViewDto;
import org.elearning.backend.content.dto.UpdateCourseDto;
import org.elearning.backend.content.exception.CourseNotFoundException;
import org.elearning.backend.content.mapper.CourseFullViewMapper;
import org.elearning.backend.content.mapper.CourseMapper;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseFullViewMapper courseFullViewMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private TestRepository testRepository;

    private CourseService courseService;
    private UUID courseId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                chapterRepository,
                lessonRepository,
                courseFullViewMapper,
                courseMapper,
                testRepository
        );
        courseId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getPublicCoursesMapsRepositoryResults() {
        Course course = new Course();
        ResponseCourseDto dto = new ResponseCourseDto();
        when(courseRepository.findByStatusAndVisibility(CourseStatus.PUBLISHED, CourseVisibility.PUBLIC))
                .thenReturn(List.of(course));
        when(courseMapper.toCourseDtoGetList(List.of(course))).thenReturn(List.of(dto));

        assertThat(courseService.getPublicCourses()).containsExactly(dto);
    }

    @Test
    void getMyCoursesMapsRepositoryResults() {
        Course course = new Course();
        ResponseCourseDto dto = new ResponseCourseDto();
        when(courseRepository.findByCreatedBy(userId)).thenReturn(List.of(course));
        when(courseMapper.toCourseDtoGetList(List.of(course))).thenReturn(List.of(dto));

        assertThat(courseService.getMyCourses(userId)).containsExactly(dto);
    }

    @Test
    void updateCourseThrowsWhenCourseMissing() {
        UpdateCourseDto updateCourseDto = new UpdateCourseDto();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(courseId, updateCourseDto))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found with id: " + courseId);
    }

    @Test
    void patchCourseThrowsWhenCourseMissing() {
        UpdateCourseDto updateCourseDto = new UpdateCourseDto();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.patchCourse(courseId, updateCourseDto))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found with id: " + courseId);
    }

    @Test
    void deleteCourseThrowsWhenCourseMissing() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThatThrownBy(() -> courseService.deleteCourse(courseId))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found with id: " + courseId);
    }

    @Test
    void getCourseFullViewThrowsWhenCourseMissing() {
        when(courseRepository.findCourseWithChapters(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseFullView(courseId))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found with id: " + courseId);
    }

    @Test
    void getCourseFullViewReturnsMappedDtoWhenCourseHasNoLessons() {
        Course course = new Course();
        course.setId(courseId);
        course.setChapters(List.of());
        ResponseCourseFullViewDto dto = new ResponseCourseFullViewDto();
        when(courseRepository.findCourseWithChapters(courseId)).thenReturn(Optional.of(course));
        when(courseFullViewMapper.toCourseFullViewDTO(eq(course), any())).thenReturn(dto);

        ResponseCourseFullViewDto result = courseService.getCourseFullView(courseId);

        assertThat(result).isSameAs(dto);
        verify(chapterRepository).findChaptersWithLessonsByCourseId(courseId);
        verify(lessonRepository).findLessonsWithResourcesByCourseId(courseId);
        verify(testRepository, never()).findTestIdsByLessonIds(any());

        ArgumentCaptor<Map<UUID, UUID>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(courseFullViewMapper).toCourseFullViewDTO(eq(course), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).isEmpty();
    }

    @Test
    void getCourseFullViewBuildsLessonToTestMap() {
        UUID lessonId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        Chapter chapter = new Chapter();
        chapter.setLessons(List.of(lesson));
        Course course = new Course();
        course.setId(courseId);
        course.setChapters(List.of(chapter));
        ResponseCourseFullViewDto dto = new ResponseCourseFullViewDto();

        when(courseRepository.findCourseWithChapters(courseId)).thenReturn(Optional.of(course));
        when(testRepository.findTestIdsByLessonIds(List.of(lessonId)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{lessonId, testId}));
        when(courseFullViewMapper.toCourseFullViewDTO(eq(course), any())).thenReturn(dto);

        ResponseCourseFullViewDto result = courseService.getCourseFullView(courseId);

        assertThat(result).isSameAs(dto);
        ArgumentCaptor<Map<UUID, UUID>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(courseFullViewMapper).toCourseFullViewDTO(eq(course), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry(lessonId, testId);
    }
}
