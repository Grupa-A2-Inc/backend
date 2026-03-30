package org.elearning.backend.content.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.dto.CourseDto;
import org.elearning.backend.content.dto.CourseDtoGet;
import org.elearning.backend.content.dto.CourseFullViewDto;
import org.elearning.backend.content.dto.CreateCourseDTO;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CoursesController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseFullViewDto> createCourse(@RequestBody CreateCourseDTO courseDto) {
        CourseFullViewDto savedCourse = courseService.createCourse(courseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
    }

    @GetMapping
    public ResponseEntity<List<CourseDtoGet>> getAllCourses(@RequestParam String role, @RequestParam UUID userId) {
        List<Course> courses = courseService.getCourses(role, userId);
        List<CourseDtoGet> responseList = courses.stream()
                .map(this::mapToDtoGet)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDtoGet> updateCourse(@PathVariable UUID id, @RequestBody CourseDto courseDto) {
        Course updatedCourse = courseService.updateCourse(id, courseDto);
        return ResponseEntity.ok(mapToDtoGet(updatedCourse));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseDtoGet> patchCourse(@PathVariable UUID id, @RequestBody CourseDto courseDto) {
        Course patchedCourse = courseService.patchCourse(id, courseDto);
        return ResponseEntity.ok(mapToDtoGet(patchedCourse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/full-view")
    public ResponseEntity<CourseFullViewDto> getCourseFullView(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseFullView(courseId));
    }

    private CourseDtoGet mapToDtoGet(Course c) {
        CourseDtoGet dto = new CourseDtoGet();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCategory(c.getCategory());
        dto.setStatus(c.getStatus());
        dto.setVisibility(c.getVisibility());
        dto.setCreatedBy(c.getCreatedBy());
        return dto;
    }
}