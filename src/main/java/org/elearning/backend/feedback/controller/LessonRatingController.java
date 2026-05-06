package org.elearning.backend.feedback.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.feedback.dto.LessonRatingSummaryDto;
import org.elearning.backend.feedback.dto.RateLessonDto;
import org.elearning.backend.feedback.dto.RateLessonResponseDto;
import org.elearning.backend.feedback.service.LessonRatingService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonRatingController extends GlobalHttpStatusCodes {

    private final LessonRatingService lessonRatingService;

    @PostMapping("/lessons/{lessonId}/ratings")
    public ResponseEntity<RateLessonResponseDto> rateLesson(@RequestBody @Valid RateLessonDto requestDto,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                            @PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonRatingService.rateLesson(lessonId, customUserDetails.getUserId(), requestDto.getRating(), requestDto.getComment()));
    }

    @GetMapping("/lessons/{lessonId}/ratings/summary")
    public ResponseEntity<LessonRatingSummaryDto> getRatingSummary(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ResponseEntity.ok(
                lessonRatingService.getLessonSummary(lessonId, customUserDetails.getUserId(), customUserDetails.getRoleName())
        );
    }
}
