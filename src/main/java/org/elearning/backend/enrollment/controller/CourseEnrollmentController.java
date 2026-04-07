package org.elearning.backend.enrollment.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.service.CourseEnrollmentService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CourseEnrollmentController {

    private final CourseEnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentDto> enrollInCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudentInCourse(userDetails.getUserId(), courseId));
    }

    @DeleteMapping("/courses/{courseId}/unenroll")
    public ResponseEntity<Void> unenrollFromCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID courseId) {
        enrollmentService.unenrollStudentFromCourse(userDetails.getUserId(), courseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/me/courses")
    public ResponseEntity<List<EnrolledCourseDto>> getEnrolledCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(enrollmentService.getEnrolledCoursesForStudent(userDetails.getUserId()));
    }
}
