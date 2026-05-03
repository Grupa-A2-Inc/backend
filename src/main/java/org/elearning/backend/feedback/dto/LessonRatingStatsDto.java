package org.elearning.backend.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LessonRatingStatsDto {
    private Double rating;
    private Long count;
}
