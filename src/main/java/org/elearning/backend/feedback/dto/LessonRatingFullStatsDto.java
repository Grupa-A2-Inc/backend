package org.elearning.backend.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class LessonRatingFullStatsDto {
    private UUID lessonId;
    private String title;
    private Double averageRating;
    private Long totalRatings;
}
