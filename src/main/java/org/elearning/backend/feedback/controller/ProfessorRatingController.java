package org.elearning.backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Professor Ratings", description = "Professor lesson rating statistics")
@RestController
@RequestMapping("/api/v1/professors")
@RequiredArgsConstructor
public class ProfessorRatingController extends GlobalHttpStatusCodes {
    private final ProfessorRatingService professorRatingService;

    private static final String OK = "200";
    private static final String FORBIDDEN = "403";

    @Operation(summary = "Get average ratings for all lessons", description = "Returns the average rating and full rating statistics for all lessons owned by the authenticated professor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Ratings retrieved successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied — requires TEACHER role")
    })
    @GetMapping("/me/lessons/ratings")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<LessonRatingFullStatsDto>> getAverageRatingsForAllLessons(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(professorRatingService.getAverageRatingsForAllLessons(userDetails.getUserId()));
    }
}
