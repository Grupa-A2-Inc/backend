package org.elearning.backend.analytics.controller;

import org.elearning.backend.analytics.dto.ClassAverageDto;
import org.elearning.backend.analytics.dto.MyTestStatsDto;
import org.elearning.backend.analytics.dto.StudentAverageDto;
import org.elearning.backend.analytics.service.AnalyticsQueryService;
import org.elearning.backend.analytics.service.StudentsStatsService;

import org.elearning.backend.security.auth.CustomUserDetails;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.parameters.P;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsAndStatsController {

    private final AnalyticsQueryService analyticsQueryService;
    private final StudentsStatsService studentsStatsService;

    public AnalyticsAndStatsController(AnalyticsQueryService analyticsQueryService, StudentsStatsService studentsStatsService) {
        this.analyticsQueryService = analyticsQueryService;
        this.studentsStatsService = studentsStatsService;
    }

    @GetMapping("/tests/{testId}/analytics/class-average")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<ClassAverageDto> getClassAverage(
            @P("id") @PathVariable UUID testId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = currentUser.getUserId();

        return ResponseEntity.ok().body(analyticsQueryService.getClassAverage(testId, userId));
    }

    @GetMapping("/courses/{courseId}/analytics/student-averages")
    @PreAuthorize("@accessService.canViewCourseFullView(authentication,#id)")
    public ResponseEntity<Page<StudentAverageDto>> getStudentAverages(
            @P("id") @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @ParameterObject Pageable pageable) {
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok().body(analyticsQueryService.getStudentAverages(courseId, userId, pageable));
    }

    @GetMapping("/students/me/tests/{testId}/stats")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<MyTestStatsDto> getMyTestStats(
            @P("id") @PathVariable UUID testId,
            @AuthenticationPrincipal CustomUserDetails currentUser){
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok().body(studentsStatsService.getMyTestStats(userId, testId));
    }


}
