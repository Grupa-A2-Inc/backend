package org.elearning.backend.content.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.exception.CourseNotFoundException;
import org.elearning.backend.content.mapper.CourseFullViewMapper;
import org.elearning.backend.content.mapper.CourseMapper;
import org.elearning.backend.content.model.*;
import org.elearning.backend.content.dto.*;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseFullViewMapper courseFullViewMapper;
    private final CourseMapper courseMapper;

    @Transactional
    public ResponseCourseFullViewDto createCourse(CreateCourseDto dto) {
        setDefaultCourseProperties(dto);
        Course course = courseMapper.toCourse(dto);
        linkChaptersToCourse(course);
        course = courseRepository.saveAndFlush(course);
        return courseFullViewMapper.toCourseFullViewDTO(course);
    }

    private void setDefaultCourseProperties(CreateCourseDto dto) {
        if (dto.getVisibility() == null) {
            dto.setVisibility(CourseVisibility.PRIVATE);
        }
        if (dto.getStatus() == null) {
            dto.setStatus(CourseStatus.DRAFT);
        }
    }

    private void linkChaptersToCourse(Course course) {
        if (course.getChapters() == null) return;
        for (Chapter chapter : course.getChapters()) {
            chapter.setCourse(course);
            linkLessonsToChapter(chapter);
        }
    }

    private void linkLessonsToChapter(Chapter chapter) {
        if (chapter.getLessons() == null) return;
        for (Lesson lesson : chapter.getLessons()) {
            lesson.setChapter(chapter);
            linkResourcesToLesson(lesson);
        }
    }

    private void linkResourcesToLesson(Lesson lesson) {
        if (lesson.getLessonResources() == null) return;
        for (LessonResource resource : lesson.getLessonResources()) {
            resource.setLesson(lesson);
        }
    }

    public List<Course> getCourses(String role, UUID userId) {
        if ("INSTRUCTOR".equals(role)) {
            return courseRepository.findByCreatedBy(userId);
        } else {
            return courseRepository.findByStatusAndVisibility(
                    CourseStatus.PUBLISHED,
                    CourseVisibility.PUBLIC
            );
        }
    }

    @Transactional
    public Course updateCourse(UUID id, CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCategory(dto.getCategory());
        if (dto.getStatus() != null) course.setStatus(dto.getStatus());
        return courseRepository.save(course);
    }

    @Transactional
    public Course patchCourse(UUID id, CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        if (dto.getTitle() != null) course.setTitle(dto.getTitle());
        if (dto.getDescription() != null) course.setDescription(dto.getDescription());
        if (dto.getCategory() != null) course.setCategory(dto.getCategory());
        if (dto.getStatus() != null) course.setStatus(dto.getStatus());
        if (dto.getCreatedBy() != null) course.setCreatedBy(dto.getCreatedBy());
        return courseRepository.save(course);
    }

    public void deleteCourse(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ResponseCourseFullViewDto getCourseFullView(UUID courseId) {
        Course course = courseRepository.findCourseWithChapters(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        chapterRepository.findChaptersWithLessonsByCourseId(courseId);
        lessonRepository.findLessonsWithResourcesByCourseId(courseId);
        return courseFullViewMapper.toCourseFullViewDTO(course);
    }
}