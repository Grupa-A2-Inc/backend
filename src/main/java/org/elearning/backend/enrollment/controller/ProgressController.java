package org.elearning.backend.enrollment.controller;

// SWAGGER ADDED
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.CompletedCourseDto;
import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.ProgressWithLessonListDto;
import org.elearning.backend.enrollment.dto.StudentProgressDto;
import org.elearning.backend.enrollment.service.ProgressService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Progress", description = "Student course progress tracking")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService courseProgressService;

    // SWAGGER ADDED
    @Operation(summary = "Get my course progress", description = "Returns the authenticated student's progress for the specified course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/courses/{courseId}/my-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ProgressWithLessonListDto> getMyProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProgressWithLessonListDto progress = courseProgressService.getMyCourseProgress(courseId, userDetails.getUserId());
        return ResponseEntity.ok(progress);
    }

    // SWAGGER ADDED
    @Operation(summary = "Get all students' progress for a course", description = "Returns a paginated list of student progress for the specified course, accessible by the authenticated teacher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress data returned successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/courses/{courseId}/students-progress")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<StudentProgressDto>> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {

        UUID professorId=userDetails.getUserId();
        return ResponseEntity.ok(courseProgressService.getCourseProgressForProfessor(courseId, professorId, pageable));
    }

    // SWAGGER ADDED
    @Operation(summary = "Get a student's courses progress", description = "Returns the list of enrolled courses and their progress for the specified student")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress data returned successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/students/{studentId}/courses-progress")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'ADMIN') or (hasRole('STUDENT') and #studentId == principal.id)")
    public ResponseEntity<List<EnrolledCourseDto>> getStudentProgress(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(courseProgressService.getStudentCoursesProgress(studentId));
    }

    // SWAGGER ADDED
    @Operation(summary = "Get my completed courses", description = "Returns a list of courses the authenticated student has completed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Completed courses returned successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/students/me/completed-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CompletedCourseDto>> getMyCompletedCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID studentId = userDetails.getUserId();
        return ResponseEntity.ok(courseProgressService.getMyCompletedCourses(studentId));
    }
}
