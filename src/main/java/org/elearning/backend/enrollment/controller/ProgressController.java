package org.elearning.backend.enrollment.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.ProgressWithLessonListDto;
import org.elearning.backend.enrollment.service.ProgressService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
