package org.elearning.backend.feedback.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.feedback.dto.LessonRatingFullStatsDto;
import org.elearning.backend.feedback.service.ProfessorRatingService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/professors")
@RequiredArgsConstructor
public class ProfessorRatingController extends GlobalHttpStatusCodes {
    private final ProfessorRatingService professorRatingService;

    @GetMapping("/me/lessons/ratings")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<LessonRatingFullStatsDto>> getAverageRatingsForAllLessons(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(professorRatingService.getAverageRatingsForAllLessons(userDetails.getUserId()));
    }
}
