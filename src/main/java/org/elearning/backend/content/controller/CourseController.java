package org.elearning.backend.content.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {

        Course savedCourse = courseService.createCourse(course);

        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }
}