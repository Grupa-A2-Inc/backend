package org.elearning.backend.enrollment.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.service.CourseEnrollmentService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
