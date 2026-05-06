package org.elearning.backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.feedback.dto.LessonRatingSummaryDto;
import org.elearning.backend.feedback.dto.RateLessonDto;
import org.elearning.backend.feedback.dto.RateLessonResponseDto;
import org.elearning.backend.feedback.service.LessonRatingService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Lesson Ratings", description = "Lesson rating and feedback management")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonRatingController {

    private final LessonRatingService lessonRatingService;

    private static final String OK = "200";
    private static final String BAD_REQUEST = "400";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    @Operation(summary = "Rate a lesson", description = "Submits or updates a rating and optional comment for the specified lesson by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Rating submitted successfully"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid rating data"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @PostMapping("/lessons/{lessonId}/ratings")
    public ResponseEntity<RateLessonResponseDto> rateLesson(@RequestBody @Valid RateLessonDto requestDto,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                            @PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonRatingService.rateLesson(lessonId, customUserDetails.getUserId(), requestDto.getRating(), requestDto.getComment()));
    }

    @Operation(summary = "Get lesson rating summary", description = "Returns the aggregated rating summary for the specified lesson, including average rating and rating distribution.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Rating summary retrieved successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @GetMapping("/lessons/{lessonId}/ratings/summary")
    public ResponseEntity<LessonRatingSummaryDto> getRatingSummary(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ResponseEntity.ok(
                lessonRatingService.getLessonSummary(lessonId, customUserDetails.getUserId(), customUserDetails.getRoleName())
        );
    }
}
