package org.elearning.backend.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Enroll in a course", description = "Enrolls the authenticated student in the specified course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student successfully enrolled in the course"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "409", description = "Student is already enrolled in the course"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to enroll in the course")
    })
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentDto> enrollInCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudentInCourse(userDetails.getUserId(), courseId));
    }

    @Operation(summary = "Unenroll from a course", description = "Unenrolls the authenticated student from the specified course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Student successfully unenrolled from the course"),
            @ApiResponse(responseCode = "404", description = "Course not found or student is not enrolled in the course")
    })
    @DeleteMapping("/courses/{courseId}/unenroll")
    public ResponseEntity<Void> unenrollFromCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID courseId) {
        enrollmentService.unenrollStudentFromCourse(userDetails.getUserId(), courseId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get enrolled courses", description = "Returns a list of courses the authenticated student is currently enrolled in")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enrolled courses successfully returned")
    })
    @GetMapping("/students/me/courses")
    public ResponseEntity<List<EnrolledCourseDto>> getEnrolledCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(enrollmentService.getEnrolledCoursesForStudent(userDetails.getUserId()));
    }
}
