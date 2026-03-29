package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.dto.CourseDto;
import org.elearning.backend.content.dto.CourseFullViewDto;
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
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {

        Course savedCourse = courseService.createCourse(course);

        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CourseDto>> getAllCourses(@RequestParam String role, @RequestParam UUID userId) {
        return ResponseEntity.ok(courseService.getCourses(role, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> updateCourse(@PathVariable UUID id, @RequestBody Course courseDetails) {
        return ResponseEntity.ok(courseService.updateCourse(id, courseDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/courses/{courseId}/full-view Entrypoint
     * @PathVariable - extracts dynamic value directly from the URI
     * Retrieves a Course entity along with its associated chapters, lessons, and resources based on the provided course ID.
     * Returns HTTP 200 OK with the Course entity in the response body if found.
     * If no course with the given ID exists, returns HTTP 404 Not Found.
     */
    @Operation(summary = "Get full view of a course",
            description = "Retrieves a Course entity along with its associated chapters, lessons, and resources based on the provided course ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{courseId}/full-view")
    public ResponseEntity<CourseFullViewDto> getCourseFullView(@PathVariable UUID courseId) {
        CourseFullViewDto courseServiceCourseFullView = courseService.getCourseFullView(courseId);

        return ResponseEntity.ok(courseServiceCourseFullView);
    }
}