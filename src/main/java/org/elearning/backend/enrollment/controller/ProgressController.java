package org.elearning.backend.enrollment.controller;

import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService courseProgressService;

    @GetMapping("/courses/{courseId}/my-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ProgressWithLessonListDto> getMyProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProgressWithLessonListDto progress = courseProgressService.getMyCourseProgress(courseId, userDetails.getUserId());
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/courses/{courseId}/students-progress")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Page<StudentProgressDto>> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {

        UUID professorId=userDetails.getUserId();
        return ResponseEntity.ok(courseProgressService.getCourseProgressForProfessor(courseId, professorId, pageable));
    }

    @GetMapping("/students/{studentId}/courses-progress")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'PARENT', 'ADMIN') or (hasRole('STUDENT') and #studentId == principal.id)")
    public ResponseEntity<List<EnrolledCourseDto>> getStudentProgress(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(courseProgressService.getStudentCoursesProgress(studentId));
    }
}
