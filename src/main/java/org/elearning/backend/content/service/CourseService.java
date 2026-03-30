package org.elearning.backend.content.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.mapper.CourseFullViewMapper;
import org.elearning.backend.content.model.*;
import org.elearning.backend.content.dto.*;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseFullViewMapper courseFullViewMapper;


    public Course createCourse(CourseDto dto) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCategory(dto.getCategory());
        course.setCreatedBy(dto.getCreatedBy());
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }

        if (dto.getVisibility() != null) {
            course.setVisibility(dto.getVisibility());
        }

        return courseRepository.save(course);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursul nu a fost gasit."));

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCategory(dto.getCategory());

        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }

        if (dto.getVisibility() != null) {
            course.setVisibility(dto.getVisibility());
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course patchCourse(UUID id, CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // PATCH = Modificare partiala. Setam doar ce nu e NULL in DTO
        if (dto.getTitle() != null) course.setTitle(dto.getTitle());
        if (dto.getDescription() != null) course.setDescription(dto.getDescription());
        if (dto.getCategory() != null) course.setCategory(dto.getCategory());
        if (dto.getStatus() != null) course.setStatus(dto.getStatus());
        if (dto.getVisibility() != null) course.setVisibility(dto.getVisibility());
        if (dto.getCreatedBy() != null) course.setCreatedBy(dto.getCreatedBy());

        return courseRepository.save(course);
    }
    public void deleteCourse(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursul cu acest ID nu a fost gasit!");
        }
        courseRepository.deleteById(id);
    }

    /**
     * Retrieves a Course entity along with its associated chapters, lessons, and resources based on the provided course ID.
     * This method performs three separate queries to fetch the course, its chapters, and their lessons with resources.
     * @param courseId The UUID of the course to retrieve.
     * @return The Course entity with its chapters, lessons, and resources fully loaded.
     * @throws ResponseStatusException NOT_FOUND if no course is found with the provided ID.
     */
    @Transactional(readOnly = true)
    public CourseFullViewDto getCourseFullView(UUID courseId) {
        // Query 1 — cursul cu capitole
        Course course = courseRepository.findCourseWithChapters(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course with ID " + courseId + " not found."));

        // Query 2 si 3 trebuie sa fie in ACEEASI tranzactie cu Query 1
        // ca Hibernate sa poata asocia rezultatele in primul nivel cache
        chapterRepository.findChaptersWithLessonsByCourseId(courseId);
        lessonRepository.findLessonsWithResourcesByCourseId(courseId);

        return courseFullViewMapper.toCourseFullViewDTO(course);
    }
}