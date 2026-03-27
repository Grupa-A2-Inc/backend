package org.elearning.backend.content.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.dto.CourseDto;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    //logica pentru POST
    public Course createCourse(Course course) {
        // il salvez in baza de date (dar si returnez obiectul Course)
        return courseRepository.save(course);
    }

    public List<CourseDto> getCourses(String role, UUID userId) {
        List<Course> courses;
        if ("TEACHER".equals(role)) {
            courses = courseRepository.findByCreatedBy(userId);
        }
        else
        {
            courses = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        }
        return courses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
}