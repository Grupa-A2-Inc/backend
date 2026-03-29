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

    //logica pentru POST
    public Course createCourse(Course course) {
        // il salvez in baza de date (dar si returnez obiectul Course)
        return courseRepository.save(course);
    }

    public List<CourseDto> getCourses(String role, UUID userId) {
        List<Course> courses;
        if ("INSTRUCTOR".equals(role)) {
            courses = courseRepository.findByCreatedBy(userId);
        } else {
            courses = courseRepository.findByStatusAndVisibility(
                    CourseStatus.PUBLISHED,
                    CourseVisibility.PUBLIC
            );
        }
        return courses.stream()
                .map(this::convertToDto)
                .toList();
    }

    private CourseDto convertToDto(Course course) {
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setCategory(course.getCategory());
        dto.setStatus(course.getStatus());
        dto.setVisibility(course.getVisibility());
        dto.setCreatedBy(course.getCreatedBy());
        return dto;
    }

    @Transactional
    public CourseDto updateCourse(UUID id, Course courseDetails) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursul nu a fost gasit."));

        course.setTitle(courseDetails.getTitle());
        course.setCategory(courseDetails.getCategory());
        course.setStatus(courseDetails.getStatus());
        course.setVisibility(courseDetails.getVisibility());

        Course updatedCourse = courseRepository.save(course);

        return convertToDto(updatedCourse);
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