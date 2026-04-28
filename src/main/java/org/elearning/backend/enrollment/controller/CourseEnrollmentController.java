package org.elearning.backend.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.service.CourseEnrollmentService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Course Enrollment", description = "Student course enrollment management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CourseEnrollmentController {

    private final CourseEnrollmentService enrollmentService;

    private static final String CREATED = "201";
    private static final String OK = "200";
    private static final String NO_CONTENT = "204";
    private static final String NOT_FOUND = "404";
    private static final String CONFLICT = "409";
    private static final String FORBIDDEN = "403";

    @Operation(summary = "Enroll in a course", description = "Enrolls the authenticated student in the specified course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED, description = "Student successfully enrolled in the course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Student is already enrolled in the course"),
            @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to enroll in the course")
    })
    @PreAuthorize("@accessService.canEnrollInCourse(authentication,#id)")
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentDto> enrollInCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @P("id") @PathVariable UUID courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudentInCourse(userDetails.getUserId(), courseId));
    }

    @Operation(summary = "Unenroll from a course", description = "Unenrolls the authenticated student from the specified course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Student successfully unenrolled from the course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found or student is not enrolled in the course")
    })
    @PreAuthorize("@accessService.canUnenrollFromCourse(authentication,#id)")
    @DeleteMapping("/courses/{courseId}/unenroll")
    public ResponseEntity<Void> unenrollFromCourse(@AuthenticationPrincipal CustomUserDetails userDetails, @P("id") @PathVariable UUID courseId) {
        enrollmentService.unenrollStudentFromCourse(userDetails.getUserId(), courseId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get enrolled courses", description = "Returns a list of courses the authenticated student is currently enrolled in")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Enrolled courses successfully returned")
    })
    @PreAuthorize("@accessService.canViewEnrolledCourses(authentication)")
    @GetMapping("/students/me/courses")
    public ResponseEntity<List<EnrolledCourseDto>> getEnrolledCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(enrollmentService.getEnrolledCoursesForStudent(userDetails.getUserId()));
    }
}
